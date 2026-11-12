package com.example.vpn.dns

import java.net.IDN
import java.util.Locale

object DomainMatcher {

    /**
     * Normalizes a raw input string into a standard lowercase FQDN without protocols,
     * paths, ports, or trailing dots.
     */
    fun normalize(rawDomain: String): String {
        var cleaned = rawDomain.trim().lowercase(Locale.ROOT)

        // Strip protocol if present
        if (cleaned.startsWith("http://")) {
            cleaned = cleaned.removePrefix("http://")
        } else if (cleaned.startsWith("https://")) {
            cleaned = cleaned.removePrefix("https://")
        } else if (cleaned.startsWith("ftp://")) {
            cleaned = cleaned.removePrefix("ftp://")
        }

        // Strip path, query params, and anchors
        val slashIndex = cleaned.indexOf('/')
        if (slashIndex != -1) {
            cleaned = cleaned.substring(0, slashIndex)
        }
        val questionIndex = cleaned.indexOf('?')
        if (questionIndex != -1) {
            cleaned = cleaned.substring(0, questionIndex)
        }
        val hashIndex = cleaned.indexOf('#')
        if (hashIndex != -1) {
            cleaned = cleaned.substring(0, hashIndex)
        }

        // Strip port if present
        val colonIndex = cleaned.indexOf(':')
        if (colonIndex != -1) {
            cleaned = cleaned.substring(0, colonIndex)
        }

        // Strip leading wildcards, dots, or www.
        cleaned = cleaned.removePrefix("*.").removePrefix(".")
        if (cleaned.startsWith("www.")) {
            cleaned = cleaned.removePrefix("www.")
        }
        cleaned = cleaned.removeSuffix(".")

        cleaned = cleaned.removeSuffix(".")
        return try {
            IDN.toASCII(cleaned, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
        } catch (_: IllegalArgumentException) {
            cleaned
        }
    }

    /** Returns true only for a syntactically valid DNS hostname or IPv4 literal. */
    fun isValidDomain(rawDomain: String): Boolean {
        val normalized = normalize(rawDomain)
        if (normalized.isBlank() || normalized.length > 253 || normalized.any { it.isWhitespace() || it == '@' || it == '/' }) return false
        val labels = normalized.split('.')
        if (labels.size < 2) return false
        return labels.all { label ->
            label.isNotEmpty() && label.length <= 63 &&
                label.first().isLetterOrDigit() && label.last().isLetterOrDigit() &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }

    /**
     * Checks if a single domain matches a specific rule.
     * Rules match exact domain AND any subdomains (e.g., "instagram.com" matches "www.instagram.com" and "m.instagram.com").
     * Safe boundary check prevents false positives ("notinstagram.com" or "instagram.com.attacker.org" will NOT match).
     */
    fun matchesRule(domain: String, rule: String): Boolean {
        val normDomain = normalize(domain)
        val normRule = normalize(rule)

        if (normDomain.isEmpty() || normRule.isEmpty()) return false

        // Exact match
        if (normDomain == normRule) return true

        // Subdomain match: must end with ".<rule>"
        if (normDomain.endsWith(".$normRule")) return true

        return false
    }

    /**
     * Evaluates whether a requested domain should be blocked based on active block rules and an optional allowlist.
     * Allowlist takes precedence over block rules.
     */
    fun isBlocked(
        domain: String,
        blockedRules: Collection<String>,
        allowlist: Collection<String> = emptySet()
    ): Boolean {
        val normDomain = normalize(domain)
        if (normDomain.isEmpty()) return false

        // 1. Check allowlist first: if domain matches an explicit allow rule, permit it
        for (allowed in allowlist) {
            if (matchesRule(normDomain, allowed)) {
                return false
            }
        }

        // 2. Check blocklist
        for (rule in blockedRules) {
            if (matchesRule(normDomain, rule)) {
                return true
            }
        }

        return false
    }
}
