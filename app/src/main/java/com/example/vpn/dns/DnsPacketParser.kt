package com.example.vpn.dns

import java.nio.ByteBuffer
import java.util.Locale

data class DnsQuery(
    val transactionId: Int,
    val isResponse: Boolean,
    val opcode: Int,
    val recursionDesired: Boolean,
    val questionCount: Int,
    val domain: String,
    val qtype: Int,
    val qclass: Int,
    val questionBytes: ByteArray,
    val rawPacket: ByteArray,
    val isValid: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DnsQuery
        return transactionId == other.transactionId && domain == other.domain && qtype == other.qtype
    }

    override fun hashCode(): Int {
        var result = transactionId
        result = 31 * result + domain.hashCode()
        result = 31 * result + qtype
        return result
    }


object DnsPacketParser {

    const val QTYPE_A = 1
    const val QTYPE_NS = 2
    const val QTYPE_CNAME = 5
    const val QTYPE_SOA = 6
    const val QTYPE_PTR = 12
    const val QTYPE_MX = 15
    const val QTYPE_TXT = 16
    const val QTYPE_AAAA = 28
    const val QTYPE_HTTPS = 65
    const val QTYPE_ANY = 255

    /**
     * Parses a raw byte array representing a DNS packet.
     * Returns a valid DnsQuery if parsing succeeds, or null if the packet is malformed.
     */
    fun parseQuery(data: ByteArray): DnsQuery? {
        if (data.size < 12) return null // Minimum DNS header is 12 bytes

        try {
            val buffer = ByteBuffer.wrap(data)

            // Header (12 bytes)
            val transactionId = buffer.short.toInt() and 0xFFFF
            val flags = buffer.short.toInt() and 0xFFFF
            val isResponse = (flags and 0x8000) != 0
            val opcode = (flags and 0x7800) shr 11
            val recursionDesired = (flags and 0x0100) != 0

            val qdcount = buffer.short.toInt() and 0xFFFF
            buffer.short // ancount
            buffer.short // nscount
            buffer.short // arcount

            if (qdcount < 1) return null // Must have at least 1 question

            // Parse Question section
            val questionStart = buffer.position()
            val domainBuilder = StringBuilder()
            var jumped = false
            var currentPos = buffer.position()

            // Read QNAME (sequence of length-prefixed labels ending with 0)
            while (currentPos < data.size) {
                val len = data[currentPos].toInt() and 0xFF
                if (len == 0) {
                    currentPos++
                    break
                }

                // Check for DNS compression pointer (top 2 bits set: 0xC0)
                if ((len and 0xC0) == 0xC0) {
                    if (currentPos + 1 >= data.size) return null
                    val offset = ((len and 0x3F) shl 8) or (data[currentPos + 1].toInt() and 0xFF)
                    if (!jumped) {
                        buffer.position(currentPos + 2)
                        jumped = true
                    }
                    currentPos = offset
                    continue
                }

                currentPos++
                if (currentPos + len > data.size) return null

                val labelBytes = ByteArray(len)
                System.arraycopy(data, currentPos, labelBytes, 0, len)
                currentPos += len

                if (domainBuilder.isNotEmpty()) {
                    domainBuilder.append('.')
                }
                domainBuilder.append(String(labelBytes, Charsets.US_ASCII))
            }

            if (!jumped) {
                buffer.position(currentPos)
            }

            if (buffer.remaining() < 4) return null
            val qtype = buffer.short.toInt() and 0xFFFF
            val qclass = buffer.short.toInt() and 0xFFFF
            val questionEnd = buffer.position()

            val questionLength = questionEnd - questionStart
            val questionBytes = ByteArray(questionLength)
            System.arraycopy(data, questionStart, questionBytes, 0, questionLength)

            val domain = domainBuilder.toString().lowercase(Locale.ROOT)

            return DnsQuery(
                transactionId = transactionId,
                isResponse = isResponse,
                opcode = opcode,
                recursionDesired = recursionDesired,
                questionCount = qdcount,
                domain = domain,
                qtype = qtype,
                qclass = qclass,
                questionBytes = questionBytes,
                rawPacket = data,
                isValid = true
            )
        } catch (_: Exception) {
            return null
        }
    }
    /** Minimum answer-section TTL in seconds; null means malformed or no positive answer. */
    fun minimumAnswerTtlSeconds(response: ByteArray): Long? {
        if (response.size < 12) return null
        fun u16(offset: Int): Int {
            if (offset < 0 || offset + 2 > response.size) throw IndexOutOfBoundsException()
            return ((response[offset].toInt() and 0xFF) shl 8) or (response[offset + 1].toInt() and 0xFF)
        }
        fun skipName(start: Int): Int? {
            var pos = start
            while (pos < response.size) {
                val length = response[pos].toInt() and 0xFF
                if (length == 0) return pos + 1
                if ((length and 0xC0) == 0xC0) return if (pos + 1 < response.size) pos + 2 else null
                if ((length and 0xC0) != 0 || pos + 1 + length > response.size) return null
                pos += 1 + length
            }
            return null
        }
        return try {
            val questions = u16(4)
            val answers = u16(6)
            val authorities = u16(8)
            val additional = u16(10)
            if (answers == 0) return null
            var offset = 12
            repeat(questions) {
                offset = (skipName(offset) ?: return null) + 4
                if (offset > response.size) return null
            }
            var minTtl: Long? = null
            repeat(answers + authorities + additional) { index ->
                val nameEnd = skipName(offset) ?: return null
                val ttlOffset = nameEnd + 4
                val lengthOffset = nameEnd + 8
                if (lengthOffset + 2 > response.size || ttlOffset + 4 > response.size) return null
                val ttl = ((response[ttlOffset].toLong() and 0xFF) shl 24) or
                    ((response[ttlOffset + 1].toLong() and 0xFF) shl 16) or
                    ((response[ttlOffset + 2].toLong() and 0xFF) shl 8) or
                    (response[ttlOffset + 3].toLong() and 0xFF)
                val recordLength = u16(lengthOffset)
                val end = lengthOffset + 2 + recordLength
                if (end > response.size) return null
                if (index < answers) minTtl = minOf(minTtl ?: ttl, ttl)
                offset = end
            }
            minTtl
        } catch (_: Exception) { null }
    }

}
