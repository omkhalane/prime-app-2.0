package com.example.util

/**
 * Format and truncate application name.
 * Rule: If app name is longer than 15 chars, trim it down to {12 chars} + "..."
 */
fun formatAppName(name: String): String {
    val trimmed = name.trim()
    return if (trimmed.length > 15) {
        trimmed.take(12) + "..."
    } else {
        trimmed
    }
}
