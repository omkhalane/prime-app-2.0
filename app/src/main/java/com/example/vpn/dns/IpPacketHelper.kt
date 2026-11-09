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
        if (length < 28) return null // 20 bytes IPv4 header + 8 bytes UDP header

        val buffer = ByteBuffer.wrap(packet, 0, length)
        val versionAndIhl = buffer.get().toInt() and 0xFF
        val version = versionAndIhl shr 4
        if (version != 4) return null // Only IPv4 supported for DNS tunnel

        val ihl = (versionAndIhl and 0x0F) * 4
        if (ihl < 20 || ihl > length) return null

        buffer.position(9)
        val protocol = buffer.get()
        if (protocol != PROTOCOL_UDP) return null

        buffer.position(12)
        val sourceIp = ByteArray(4)
        buffer.get(sourceIp)
        val destIp = ByteArray(4)
        buffer.get(destIp)

        // Move to UDP header (at ihl offset)
        buffer.position(ihl)
        if (buffer.remaining() < 8) return null

        val sourcePort = buffer.short.toInt() and 0xFFFF
        val destPort = buffer.short.toInt() and 0xFFFF
        val udpLength = buffer.short.toInt() and 0xFFFF
        buffer.short // udp checksum

        val payloadLength = udpLength - 8
        if (payloadLength < 0 || buffer.remaining() < payloadLength) return null

        val payload = ByteArray(payloadLength)
        buffer.get(payload)

        return ParsedUdpPacket(
            sourceIp = sourceIp,
            destIp = destIp,
            sourcePort = sourcePort,
            destPort = destPort,
            payload = payload
        )
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
