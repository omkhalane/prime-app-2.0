package com.example.vpn.dns

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

sealed class DnsFilterResult {
    data class Blocked(
        val responsePayload: ByteArray,
        val domain: String,
        val category: String,
        val reason: String
    ) : DnsFilterResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Blocked
            return domain == other.domain && category == other.category
        }
        override fun hashCode(): Int = domain.hashCode() * 31 + category.hashCode()
    }

    data class Allowed(
        val responsePayload: ByteArray,
        val domain: String
    ) : DnsFilterResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Allowed
            return domain == other.domain
        }
        override fun hashCode(): Int = domain.hashCode()
    }

    object Ignored : DnsFilterResult()
}

class DnsFilterEngine(
    private val socketProtector: (DatagramSocket) -> Boolean = { true }
) {
    companion object {
        private const val TAG = "DnsFilterEngine"
        private const val UPSTREAM_TIMEOUT_MS = 2500
        val DEFAULT_UPSTREAM_SERVERS = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9", "1.0.0.1")
    }

    // Active rules held in memory for sub-millisecond evaluation
    private val blockedDomainRules = ConcurrentHashMap<String, String>() // domain -> category
    private val allowlistRules = ConcurrentHashMap.newKeySet<String>()

    var blockingStrategy: BlockingStrategy = BlockingStrategy.NXDOMAIN
    var onBlockedAttempt: ((domain: String, category: String, reason: String) -> Unit)? = null

    // Lightweight in-memory DNS cache: (domain + qtype) -> (timestamp, responseBytes)
    private data class CacheEntry(val expiresAt: Long, val responseBytes: ByteArray)
    private val dnsCache = ConcurrentHashMap<String, CacheEntry>()

    fun updateRules(
        blockedDomainsWithCategory: Map<String, String>,
        allowlist: Set<String>
    ) {
        blockedDomainRules.clear()
        blockedDomainsWithCategory.forEach { (domain, category) ->
            blockedDomainRules[DomainMatcher.normalize(domain)] = category
        }
        allowlistRules.clear()
        allowlist.forEach { allowlistRules.add(DomainMatcher.normalize(it)) }
        dnsCache.clear()
    }

    fun isDomainBlocked(domain: String): Pair<Boolean, String> {
        val norm = DomainMatcher.normalize(domain)
        if (norm.isEmpty()) return Pair(false, "")

        // 1. Allowlist check
        if (allowlistRules.any { DomainMatcher.matchesRule(norm, it) }) {
            return Pair(false, "")
        }

        // 2. Blocked rules check
        for ((rule, category) in blockedDomainRules) {
            if (DomainMatcher.matchesRule(norm, rule)) {
                return Pair(true, category)
            }
        }
        return Pair(false, "")
    }

    /**
     * Filters a raw DNS payload. If blocked, returns a synthetic blocked response.
     * If permitted, forwards to upstream DNS servers with caching.
     */
    fun processDnsPayload(dnsPayload: ByteArray): DnsFilterResult {
        val query = DnsPacketParser.parseQuery(dnsPayload) ?: return DnsFilterResult.Ignored

        if (query.isResponse) {
            return DnsFilterResult.Ignored // Ignore incoming responses on tunnel
        }

        val domain = query.domain

        // Canary domain for Chromium & Firefox: Returning NXDOMAIN forces browsers to disable DoH and use system DNS
        if (domain.equals("use-application-dns.net", ignoreCase = true)) {
            val responseBytes = DnsPacketBuilder.buildHeaderWithRCode(query, DnsPacketBuilder.RCODE_NXDOMAIN)
            return DnsFilterResult.Blocked(
                responsePayload = responseBytes,
                domain = domain,
                category = "System",
                reason = "DoH Canary Handled"
            )
        }

        val (blocked, category) = isDomainBlocked(domain)

        if (blocked) {
            val responseBytes = DnsPacketBuilder.buildBlockedResponse(query, blockingStrategy)
            onBlockedAttempt?.invoke(domain, category, "PRIME Rule Matched ($category)")
            return DnsFilterResult.Blocked(
                responsePayload = responseBytes,
                domain = domain,
                category = category,
                reason = "PRIME Rule Matched ($category)"
            )
        }

        // Check DNS Cache
        val cacheKey = "${domain}_${query.qtype}_${query.qclass}"
        val now = System.currentTimeMillis()
        val cached = dnsCache[cacheKey]
        if (cached != null && cached.expiresAt > now) {
            // Rewrite transaction ID to match current query
            val rewritten = cached.responseBytes.clone()
            if (rewritten.size >= 2) {
                rewritten[0] = (query.transactionId shr 8).toByte()
                rewritten[1] = (query.transactionId and 0xFF).toByte()
            }
            return DnsFilterResult.Allowed(rewritten, domain)
        }

        // Forward to upstream DNS resolver
        val upstreamResponse = forwardToUpstream(dnsPayload)
        if (upstreamResponse != null) {
            val ttlSeconds = DnsPacketParser.minimumAnswerTtlSeconds(upstreamResponse)
                ?.coerceIn(1L, 3600L) ?: 0L
            if (ttlSeconds > 0L) {
                dnsCache[cacheKey] = CacheEntry(System.currentTimeMillis() + ttlSeconds * 1000L, upstreamResponse)
            }
            return DnsFilterResult.Allowed(upstreamResponse, domain)
        }

        // If upstream timed out, return empty SERVFAIL or fail-open
        val servfail = DnsPacketBuilder.buildHeaderWithRCode(query, DnsPacketBuilder.RCODE_SERVFAIL)
        return DnsFilterResult.Allowed(servfail, domain)
    }

    private fun forwardToUpstream(queryPayload: ByteArray): ByteArray? {
        for (serverIp in DEFAULT_UPSTREAM_SERVERS) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.soTimeout = UPSTREAM_TIMEOUT_MS

                // CRITICAL: Protect the socket so packets bypass the local VPN interface
                val protected = socketProtector(socket)
                if (!protected) {
                    Log.w(TAG, "Socket could not be protected from VPN routing loop")
                }

                val serverAddress = InetAddress.getByName(serverIp)
                val outPacket = DatagramPacket(queryPayload, queryPayload.size, serverAddress, 53)
                socket.send(outPacket)

                val responseBuffer = ByteArray(1500)
                val inPacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(inPacket)

                val responseData = ByteArray(inPacket.length)
                System.arraycopy(responseBuffer, 0, responseData, 0, inPacket.length)
                return responseData
            } catch (_: Exception) {
                // Try next upstream server
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
        return null
    }

    fun clearCache() {
        dnsCache.clear()
    }
}
