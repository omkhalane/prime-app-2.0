package com.example.vpn.dns

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

sealed class DnsFilterResult {
    data class Blocked(val responsePayload: ByteArray, val domain: String, val category: String, val reason: String) : DnsFilterResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Blocked
            return domain == other.domain && category == other.category
        }
        override fun hashCode(): Int = domain.hashCode() * 31 + category.hashCode()
    }
    data class Allowed(val responsePayload: ByteArray, val domain: String) : DnsFilterResult() {
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

class DnsFilterEngine(private val socketProtector: (DatagramSocket) -> Boolean = { true }) {
    companion object {
        private const val TAG = "DnsFilterEngine"
        private const val UPSTREAM_TIMEOUT_MS = 2500
        private const val MAX_DNS_CACHE_ENTRIES = 2048
        val DEFAULT_UPSTREAM_SERVERS = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9", "1.0.0.1")
    }
    private val blockedDomainRules = ConcurrentHashMap<String, String>()
    private val allowlistRules = ConcurrentHashMap.newKeySet<String>()
    var blockingStrategy: BlockingStrategy = BlockingStrategy.NXDOMAIN
    var onBlockedAttempt: ((domain: String, category: String, reason: String) -> Unit)? = null
    private data class CacheEntry(val expiresAt: Long, val responseBytes: ByteArray)
    private val dnsCache = ConcurrentHashMap<String, CacheEntry>()

    fun updateRules(blockedDomainsWithCategory: Map<String, String>, allowlist: Set<String>) {
        blockedDomainRules.clear()
        blockedDomainsWithCategory.forEach { (domain, category) ->
            DomainMatcher.normalize(domain).takeIf { it.isNotEmpty() }?.let { blockedDomainRules[it] = category }
        }
        allowlistRules.clear()
        allowlist.forEach { DomainMatcher.normalize(it).takeIf(String::isNotEmpty)?.let(allowlistRules::add) }
        dnsCache.clear()
    }

    fun isDomainBlocked(domain: String): Pair<Boolean, String> {
        val norm = DomainMatcher.normalize(domain)
        if (norm.isEmpty()) return false to ""
        if (allowlistRules.any { DomainMatcher.matchesRule(norm, it) }) return false to ""
        for ((rule, category) in blockedDomainRules) {
            if (DomainMatcher.matchesRule(norm, rule)) return true to category
        }
        return false to ""
    }

    fun processDnsPayload(dnsPayload: ByteArray): DnsFilterResult {
        val query = DnsPacketParser.parseQuery(dnsPayload) ?: return DnsFilterResult.Ignored
        if (query.isResponse) return DnsFilterResult.Ignored
        val domain = query.domain
        if (domain.equals("use-application-dns.net", ignoreCase = true)) {
            return DnsFilterResult.Blocked(DnsPacketBuilder.buildHeaderWithRCode(query, DnsPacketBuilder.RCODE_NXDOMAIN),
                domain, "System", "DoH Canary Handled")
        }
        val (blocked, category) = isDomainBlocked(domain)
        if (blocked) {
            val reason = "PRIME Rule Matched ($category)"
            onBlockedAttempt?.invoke(domain, category, reason)
            return DnsFilterResult.Blocked(DnsPacketBuilder.buildBlockedResponse(query, blockingStrategy), domain, category, reason)
        }

        val cacheKey = buildCacheKey(dnsPayload)
        val now = System.currentTimeMillis()
        val cached = dnsCache[cacheKey]
        if (cached != null && cached.expiresAt > now) {
            val rewritten = cached.responseBytes.clone()
            if (rewritten.size >= 2) {
                rewritten[0] = (query.transactionId shr 8).toByte()
                rewritten[1] = (query.transactionId and 0xFF).toByte()
            }
            return DnsFilterResult.Allowed(rewritten, domain)
        }
        if (cached != null) dnsCache.remove(cacheKey, cached)

        val response = forwardToUpstream(dnsPayload, query)
        if (response != null) {
            val ttl = DnsPacketParser.minimumAnswerTtlSeconds(response)?.coerceIn(1L, 3600L) ?: 0L
            if (ttl > 0L) cacheResponse(cacheKey, response, ttl)
            return DnsFilterResult.Allowed(response, domain)
        }
        return DnsFilterResult.Allowed(DnsPacketBuilder.buildHeaderWithRCode(query, DnsPacketBuilder.RCODE_SERVFAIL), domain)
    }

    /**
     * Cache only equivalent wire queries. Domain/type/class alone is insufficient:
     * clients may vary case randomization, recursion/CD flags, or EDNS options.
     * Transaction IDs are the sole field rewritten on cache hits.
     */
    private fun buildCacheKey(queryPayload: ByteArray): String {
        val normalized = queryPayload.clone()
        if (normalized.size >= 2) {
            normalized[0] = 0
            normalized[1] = 0
        }
        return normalized.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }
    }

    private fun cacheResponse(key: String, response: ByteArray, ttlSeconds: Long) {
        val now = System.currentTimeMillis()
        if (dnsCache.size >= MAX_DNS_CACHE_ENTRIES) {
            dnsCache.entries.removeIf { it.value.expiresAt <= now }
            if (dnsCache.size >= MAX_DNS_CACHE_ENTRIES) {
                val oldestAvailableKey = dnsCache.keys.iterator().let { if (it.hasNext()) it.next() else null }
                if (oldestAvailableKey != null) dnsCache.remove(oldestAvailableKey)
            }
        }
        dnsCache[key] = CacheEntry(now + ttlSeconds * 1000L, response.clone())
    }

    private fun forwardToUpstream(queryPayload: ByteArray, query: DnsQuery): ByteArray? {
        if (queryPayload.size > 4096) return null
        for (serverIp in DEFAULT_UPSTREAM_SERVERS) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.soTimeout = UPSTREAM_TIMEOUT_MS
                // Never forward through the TUN itself: that creates a recursive DNS loop.
                if (!socketProtector(socket)) {
                    Log.w(TAG, "Socket protection failed; skipping upstream $serverIp")
                    continue
                }
                socket.connect(InetAddress.getByName(serverIp), 53)
                socket.send(DatagramPacket(queryPayload, queryPayload.size))
                val responseBuffer = ByteArray(4096)
                val incoming = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(incoming)
                val responseBytes = responseBuffer.copyOf(incoming.length)
                val response = DnsPacketParser.parseQuery(responseBytes) ?: continue
                if (!response.isResponse || response.transactionId != query.transactionId ||
                    response.domain != query.domain || response.qtype != query.qtype || response.qclass != query.qclass) {
                    Log.w(TAG, "Ignoring mismatched DNS response from $serverIp")
                    continue
                }
                return responseBytes
            } catch (_: Exception) {
                // Try the next resolver.
            } finally {
                try { socket?.close() } catch (_: Exception) { }
            }
        }
        return null
    }

    fun clearCache() { dnsCache.clear() }
}