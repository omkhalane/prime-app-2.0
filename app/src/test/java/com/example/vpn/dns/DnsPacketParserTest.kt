package com.example.vpn.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DnsPacketParserTest {
    private fun packet(qname: ByteArray): ByteArray {
        val header = byteArrayOf(0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        return header + qname + byteArrayOf(0x00, 0x01, 0x00, 0x01)
    }

    @Test fun parsesOrdinaryQuestion() {
        val qname = byteArrayOf(7, *"example".toByteArray(), 3, *"com".toByteArray(), 0)
        val query = DnsPacketParser.parseQuery(packet(qname))
        assertNotNull(query)
        assertEquals("example.com", query?.domain)
        assertEquals(0x1234, query?.transactionId)
    }

    @Test fun rejectsSelfReferencingCompressionPointer() {
        assertNull(DnsPacketParser.parseQuery(packet(byteArrayOf(0xC0.toByte(), 0x0C))))
    }

    @Test fun rejectsOutOfRangeCompressionPointer() {
        assertNull(DnsPacketParser.parseQuery(packet(byteArrayOf(0xFF.toByte(), 0xFF.toByte()))))
    }

    @Test fun rejectsReservedLabelEncoding() {
        assertNull(DnsPacketParser.parseQuery(packet(byteArrayOf(0x40, 0x00))))
    }

    @Test fun rejectsMultipleQuestions() {
        val query = packet(byteArrayOf(1, 'a'.code.toByte(), 0))
        query[5] = 2
        assertNull(DnsPacketParser.parseQuery(query))
    }

    @Test fun rejectsTruncatedQuestion() {
        assertNull(DnsPacketParser.parseQuery(packet(byteArrayOf(5, 'a'.code.toByte()))))
    }
}