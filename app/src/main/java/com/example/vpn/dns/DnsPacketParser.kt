package com.example.vpn.dns

import java.nio.ByteBuffer
import java.util.Locale

data class DnsQuery(
    val transactionId: Int, val isResponse: Boolean, val opcode: Int,
    val recursionDesired: Boolean, val questionCount: Int, val domain: String,
    val qtype: Int, val qclass: Int, val questionBytes: ByteArray,
    val rawPacket: ByteArray, val isValid: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DnsQuery
        return transactionId == other.transactionId && domain == other.domain && qtype == other.qtype
    }
    override fun hashCode(): Int = 31 * (31 * transactionId + domain.hashCode()) + qtype
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

    /** Parses exactly one standard DNS question and rejects malformed compression. */
    fun parseQuery(data: ByteArray): DnsQuery? {
        if (data.size < 17 || data.size > 4096) return null
        return try {
            val buffer = ByteBuffer.wrap(data)
            val transactionId = buffer.short.toInt() and 0xFFFF
            val flags = buffer.short.toInt() and 0xFFFF
            val isResponse = (flags and 0x8000) != 0
            val opcode = (flags and 0x7800) ushr 11
            val recursionDesired = (flags and 0x0100) != 0
            val qdcount = buffer.short.toInt() and 0xFFFF
            buffer.short
            buffer.short
            buffer.short
            if (qdcount != 1 || opcode != 0) return null

            val questionStart = buffer.position()
            val labels = StringBuilder()
            val visitedPointers = HashSet<Int>()
            var currentPos = questionStart
            var jumped = false
            var pointerHops = 0
            var expandedNameLength = 0
            while (true) {
                if (currentPos !in data.indices) return null
                val length = data[currentPos].toInt() and 0xFF
                if (length == 0) {
                    currentPos++
                    break
                }
                when (length and 0xC0) {
                    0xC0 -> {
                        if (currentPos + 1 >= data.size) return null
                        val pointer = ((length and 0x3F) shl 8) or (data[currentPos + 1].toInt() and 0xFF)
                        if (pointer !in 12 until data.size || !visitedPointers.add(pointer)) return null
                        if (++pointerHops > 16) return null
                        if (!jumped) {
                            buffer.position(currentPos + 2)
                            jumped = true
                        }
                        currentPos = pointer
                    }
                    0x00 -> {
                        if (length > 63 || currentPos + 1 + length > data.size) return null
                        currentPos++
                        if (labels.isNotEmpty()) labels.append('.')
                        val label = String(data, currentPos, length, Charsets.US_ASCII)
                        if (label.any { it.code !in 0x21..0x7E || it == '.' }) return null
                        labels.append(label)
                        expandedNameLength += length + 1
                        if (expandedNameLength > 255) return null
                        currentPos += length
                    }
                    else -> return null
                }
            }
            if (!jumped) buffer.position(currentPos)
            if (buffer.remaining() < 4) return null
            val qtype = buffer.short.toInt() and 0xFFFF
            val qclass = buffer.short.toInt() and 0xFFFF
            val questionBytes = data.copyOfRange(questionStart, buffer.position())
            val domain = labels.toString().lowercase(Locale.ROOT)
            if (domain.isBlank() || domain.length > 253) return null
            DnsQuery(transactionId, isResponse, opcode, recursionDesired, qdcount,
                domain, qtype, qclass, questionBytes, data)
        } catch (_: Exception) { null }
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
            var steps = 0
            while (pos < response.size && steps++ <= 128) {
                val length = response[pos].toInt() and 0xFF
                if (length == 0) return pos + 1
                if ((length and 0xC0) == 0xC0) {
                    if (pos + 1 >= response.size) return null
                    val pointer = ((length and 0x3F) shl 8) or (response[pos + 1].toInt() and 0xFF)
                    return if (pointer < response.size) pos + 2 else null
                }
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