package com.example.vpn.dns

import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer

enum class BlockingStrategy {
    NXDOMAIN,   // RCODE 3: Name Error (Standard for browser website blocking)
    REFUSED,    // RCODE 5: Query Refused
    SINKHOLE_IP // Return 0.0.0.0 / ::
}

object DnsPacketBuilder {

    const val RCODE_NOERROR = 0
    const val RCODE_FORMERR = 1
    const val RCODE_SERVFAIL = 2
    const val RCODE_NXDOMAIN = 3
    const val RCODE_REFUSED = 5

    /**
     * Builds an RFC 1035 compliant DNS response packet for a blocked query.
     */
    fun buildBlockedResponse(
        query: DnsQuery,
        strategy: BlockingStrategy = BlockingStrategy.NXDOMAIN
    ): ByteArray {
        return when (strategy) {
            BlockingStrategy.NXDOMAIN -> buildHeaderWithRCode(query, RCODE_NXDOMAIN)
            BlockingStrategy.REFUSED -> buildHeaderWithRCode(query, RCODE_REFUSED)
            BlockingStrategy.SINKHOLE_IP -> {
                if (query.qtype == DnsPacketParser.QTYPE_A) {
                    buildSinkholeAResponse(query, "0.0.0.0")
                } else {
                    // For AAAA or HTTPS records, return empty nodata response (NOERROR with ANCOUNT=0)
                    buildHeaderWithRCode(query, RCODE_NOERROR)
                }
            }
        }
    }

    /**
     * Builds a DNS response header with specified RCODE (NXDOMAIN / REFUSED), copying question bytes.
     */
    fun buildHeaderWithRCode(query: DnsQuery, rcode: Int): ByteArray {
        val out = ByteArrayOutputStream(12 + query.questionBytes.size)

        val header = ByteBuffer.allocate(12)
        // 1. Transaction ID (2 bytes)
        header.putShort(query.transactionId.toShort())

        // 2. Flags (2 bytes): QR=1 (response), AA=1, RD=query.RD, RA=1, RCODE
        var flags = 0x8180 // QR=1, RD=1, RA=1
        if (!query.recursionDesired) {
            flags = flags and 0xFEFF.toInt() // clear RD bit
        }
        flags = (flags and 0xFFF0) or (rcode and 0x000F)
        header.putShort(flags.toShort())

        // 3. Counts: QDCOUNT=1, ANCOUNT=0, NSCOUNT=0, ARCOUNT=0
        header.putShort(1.toShort()) // QDCOUNT
        header.putShort(0.toShort()) // ANCOUNT
        header.putShort(0.toShort()) // NSCOUNT
        header.putShort(0.toShort()) // ARCOUNT

        out.write(header.array())
        out.write(query.questionBytes)

        return out.toByteArray()
    }

    /**
     * Builds a DNS response pointing to a sinkhole IP address (0.0.0.0).
     */
    private fun buildSinkholeAResponse(query: DnsQuery, ipString: String): ByteArray {
        val out = ByteArrayOutputStream(12 + query.questionBytes.size + 16)

        val header = ByteBuffer.allocate(12)
        header.putShort(query.transactionId.toShort())

        var flags = 0x8180 // QR=1, RD=1, RA=1, NOERROR
        if (!query.recursionDesired) {
            flags = flags and 0xFEFF.toInt()
        }
        header.putShort(flags.toShort())

        header.putShort(1.toShort()) // QDCOUNT = 1
        header.putShort(1.toShort()) // ANCOUNT = 1
        header.putShort(0.toShort()) // NSCOUNT = 0
        header.putShort(0.toShort()) // ARCOUNT = 0

        out.write(header.array())
        out.write(query.questionBytes)

        // Answer Record: Pointer to QNAME at byte 12 (0xC0, 0x0C)
        val answer = ByteBuffer.allocate(16)
        answer.put(0xC0.toByte())
        answer.put(0x0C.toByte())
        answer.putShort(1.toShort()) // TYPE = A (1)
        answer.putShort(1.toShort()) // CLASS = IN (1)
        answer.putInt(60)            // TTL = 60s
        answer.putShort(4.toShort()) // RDLENGTH = 4 bytes

        val ipBytes = try {
            InetAddress.getByName(ipString).address
        } catch (_: Exception) {
            byteArrayOf(0, 0, 0, 0)
        }
        answer.put(ipBytes)

        out.write(answer.array())
        return out.toByteArray()
    }
}
