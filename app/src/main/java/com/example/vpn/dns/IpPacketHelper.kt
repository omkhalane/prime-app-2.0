package com.example.vpn.dns

import java.nio.ByteBuffer

data class ParsedUdpPacket(
    val sourceIp: ByteArray,
    val destIp: ByteArray,
    val sourcePort: Int,
    val destPort: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ParsedUdpPacket
        return sourcePort == other.sourcePort && destPort == other.destPort && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = sourcePort
        result = 31 * result + destPort
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

object IpPacketHelper {

    const val PROTOCOL_UDP: Byte = 17
    const val PROTOCOL_TCP: Byte = 6

    /**
     * Parses an IPv4 UDP packet from raw TUN bytes.
     * Returns null if not IPv4, not UDP, or packet is incomplete.
     */
    fun parseIpv4Udp(packet: ByteArray, length: Int): ParsedUdpPacket? {
        if (length < 28 || length > packet.size) return null
        return try {
            val buffer = ByteBuffer.wrap(packet, 0, length)
            val versionAndIhl = buffer.get().toInt() and 0xFF
            if ((versionAndIhl ushr 4) != 4) return null
            val ihl = (versionAndIhl and 0x0F) * 4
            if (ihl < 20 || ihl + 8 > length) return null

            val totalLength = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
            if (totalLength < ihl + 8 || totalLength > length) return null

            // Fragmented IPv4 datagrams cannot be parsed as a complete UDP DNS message.
            val fragmentField = ((packet[6].toInt() and 0xFF) shl 8) or (packet[7].toInt() and 0xFF)
            if ((fragmentField and 0x3FFF) != 0) return null

            if ((packet[9].toInt() and 0xFF) != PROTOCOL_UDP.toInt()) return null

            val sourceIp = packet.copyOfRange(12, 16)
            val destIp = packet.copyOfRange(16, 20)
            buffer.position(ihl)
            buffer.limit(totalLength)
            val sourcePort = buffer.short.toInt() and 0xFFFF
            val destPort = buffer.short.toInt() and 0xFFFF
            val udpLength = buffer.short.toInt() and 0xFFFF
            buffer.short
            if (udpLength < 8 || udpLength > totalLength - ihl) return null
            val payloadLength = udpLength - 8
            if (buffer.remaining() < payloadLength) return null
            val payload = ByteArray(payloadLength)
            buffer.get(payload)
            ParsedUdpPacket(sourceIp, destIp, sourcePort, destPort, payload)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Builds an IPv4 UDP packet to send a DNS response back to the client via the TUN interface.
     * Swaps source and destination IPs and ports.
     */
    fun buildIpv4UdpPacket(
        sourceIp: ByteArray,
        destIp: ByteArray,
        sourcePort: Int,
        destPort: Int,
        udpPayload: ByteArray
    ): ByteArray {
        val ipHeaderLength = 20
        val udpHeaderLength = 8
        val totalLength = ipHeaderLength + udpHeaderLength + udpPayload.size
        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet)

        // 1. IPv4 Header
        buffer.put(0x45.toByte()) // Version 4, IHL 5 (20 bytes)
        buffer.put(0x00.toByte()) // DSCP/ECN
        buffer.putShort(totalLength.toShort()) // Total length
        buffer.putShort(0.toShort()) // Identification
        buffer.putShort(0x4000.toShort()) // Flags: Don't Fragment (DF)
        buffer.put(64.toByte()) // TTL: 64
        buffer.put(PROTOCOL_UDP) // Protocol: UDP (17)
        buffer.putShort(0.toShort()) // Checksum placeholder
        buffer.put(sourceIp)
        buffer.put(destIp)

        // Compute and insert IPv4 Header Checksum
        val ipChecksum = computeIpChecksum(packet, 0, ipHeaderLength)
        buffer.putShort(10, ipChecksum.toShort())

        // 2. UDP Header
        buffer.position(ipHeaderLength)
        buffer.putShort(sourcePort.toShort())
        buffer.putShort(destPort.toShort())
        buffer.putShort((udpHeaderLength + udpPayload.size).toShort())
        buffer.putShort(0.toShort()) // Checksum (0 is valid in IPv4 UDP)

        // 3. UDP Payload (DNS Response)
        buffer.put(udpPayload)

        return packet
    }


    /** Parses a basic IPv6 UDP packet. Extension-header chains are deliberately rejected. */
    fun parseIpv6Udp(packet: ByteArray, length: Int): ParsedUdpPacket? {
        if (length < 48 || ((packet[0].toInt() ushr 4) and 0x0F) != 6) return null
        val buffer = ByteBuffer.wrap(packet, 0, length)
        val payloadLength = buffer.getShort(4).toInt() and 0xFFFF
        if ((buffer.get(6).toInt() and 0xFF) != PROTOCOL_UDP.toInt()) return null
        if (payloadLength < 8 || payloadLength > length - 40) return null
        val sourceIp = ByteArray(16)
        val destIp = ByteArray(16)
        buffer.position(8)
        buffer.get(sourceIp)
        buffer.get(destIp)
        buffer.position(40)
        val sourcePort = buffer.short.toInt() and 0xFFFF
        val destPort = buffer.short.toInt() and 0xFFFF
        val udpLength = buffer.short.toInt() and 0xFFFF
        buffer.short
        if (udpLength < 8 || udpLength > payloadLength || buffer.remaining() < udpLength - 8) return null
        val payload = ByteArray(udpLength - 8)
        buffer.get(payload)
        return ParsedUdpPacket(sourceIp, destIp, sourcePort, destPort, payload)
    }

    /** Builds an IPv6 UDP response with the mandatory IPv6 UDP pseudo-header checksum. */
    fun buildIpv6UdpPacket(sourceIp: ByteArray, destIp: ByteArray, sourcePort: Int,
                           destPort: Int, udpPayload: ByteArray): ByteArray {
        require(sourceIp.size == 16 && destIp.size == 16) { "IPv6 addresses must be 16 bytes" }
        val udpLength = 8 + udpPayload.size
        require(udpLength <= 0xFFFF) { "UDP payload is too large" }
        val packet = ByteArray(40 + udpLength)
        val buffer = ByteBuffer.wrap(packet)
        buffer.put(0x60.toByte())
        buffer.put(0); buffer.put(0); buffer.put(0)
        buffer.putShort(udpLength.toShort())
        buffer.put(PROTOCOL_UDP)
        buffer.put(64.toByte())
        buffer.put(sourceIp)
        buffer.put(destIp)
        buffer.putShort(sourcePort.toShort())
        buffer.putShort(destPort.toShort())
        buffer.putShort(udpLength.toShort())
        buffer.putShort(0)
        buffer.put(udpPayload)

        val pseudo = ByteArray(40 + udpLength)
        System.arraycopy(sourceIp, 0, pseudo, 0, 16)
        System.arraycopy(destIp, 0, pseudo, 16, 16)
        pseudo[32] = ((udpLength ushr 24) and 0xFF).toByte()
        pseudo[33] = ((udpLength ushr 16) and 0xFF).toByte()
        pseudo[34] = ((udpLength ushr 8) and 0xFF).toByte()
        pseudo[35] = (udpLength and 0xFF).toByte()
        pseudo[39] = PROTOCOL_UDP
        System.arraycopy(packet, 40, pseudo, 40, udpLength)
        var checksum = computeChecksum(pseudo)
        if (checksum == 0) checksum = 0xFFFF
        ByteBuffer.wrap(packet).putShort(46, checksum.toShort())
        return packet
    }

    private fun computeChecksum(data: ByteArray): Int {
        var sum = 0L
        var i = 0
        while (i < data.size) {
            val high = data[i].toInt() and 0xFF
            val low = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else 0
            sum += ((high shl 8) or low).toLong()
            i += 2
        }
        while ((sum ushr 16) != 0L) sum = (sum and 0xFFFFL) + (sum ushr 16)
        return sum.inv().toInt() and 0xFFFF
    }

    private fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val high = data[i].toInt() and 0xFF
            val low = if (i + 1 < offset + length) data[i + 1].toInt() and 0xFF else 0
            val word = (high shl 8) or low
            sum += word
            i += 2
        }
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }
}
