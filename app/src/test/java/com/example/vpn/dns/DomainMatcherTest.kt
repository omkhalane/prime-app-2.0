package com.example.vpn.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainMatcherTest {
    @Test fun normalizesUrlAndInternationalizedHostnames() {
        assertEquals("example.com", DomainMatcher.normalize("https://www.example.com/path?q=1"))
        assertEquals("xn--mnich-kva.de", DomainMatcher.normalize("münich.de"))
    }

    @Test fun acceptsValidHostnamesAndRejectsMalformedInput() {
        assertTrue(DomainMatcher.isValidDomain("sub.example.com"))
        assertTrue(DomainMatcher.isValidDomain("https://example.com/path"))
        assertFalse(DomainMatcher.isValidDomain("example..com"))
        assertFalse(DomainMatcher.isValidDomain("example .com"))
        assertFalse(DomainMatcher.isValidDomain("user@example.com"))
        assertFalse(DomainMatcher.isValidDomain("-invalid.example"))
        assertFalse(DomainMatcher.isValidDomain("localhost"))
    }

    @Test fun matchingUsesDomainBoundaries() {
        assertTrue(DomainMatcher.matchesRule("m.example.com", "example.com"))
        assertFalse(DomainMatcher.matchesRule("notexample.com", "example.com"))
        assertFalse(DomainMatcher.matchesRule("example.com.attacker.test", "example.com"))
    }
}