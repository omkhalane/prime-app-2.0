package com.example.vpn.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IpPacketHelperTest {
    private fun ipv4Udp(fragmentField: Int = 0, udpLength: Int = 11): ByteArray {
        val payload = byteArrayOf(1, 2, 3)
        val packet = ByteArray(31)
        packet[0] = 0x45
        packet[2] = 0
        packet[3] = 31
        packet[6] = (fragmentField ushr 8).toByte()
        packet[7] = fragmentField.toByte()
        packet[9] = IpPacketHelper.PROTOCOL_UDP
        packet[12] = 10; packet[15] = 1
        packet[16] = 10; packet[19] = 2
        packet[20] = 0x04; packet[21] = 0xD2.toByte()
        packet[22] = 0; packet[23] = 53
        packet[24] = (udpLength ushr 8).toByte()
        packet[25] = udpLength.toByte()
        System.arraycopy(payload, 0, packet, 28, payload.size)
        return packet
    }

    @Test fun parsesValidIpv4DnsDatagram() {
        val parsed = IpPacketHelper.parseIpv4Udp(ipv4Udp(), 31)
        assertEquals(1234, parsed?.sourcePort)
        assertEquals(53, parsed?.destPort)
        assertEquals(3, parsed?.payload?.size)
    }

    @Test fun rejectsFragmentedIpv4Datagram() {
        assertNull(IpPacketHelper.parseIpv4Udp(ipv4Udp(fragmentField = 0x2000), 31))
    }

    @Test fun rejectsUdpLengthBeyondIpv4Payload() {
        assertNull(IpPacketHelper.parseIpv4Udp(ipv4Udp(udpLength = 20), 31))
    }

    @Test fun rejectsLengthLargerThanBackingArray() {
        assertNull(IpPacketHelper.parseIpv4Udp(ipv4Udp(), 32))
        assertNull(IpPacketHelper.parseIpv6Udp(ByteArray(48), 49))
    }
}
