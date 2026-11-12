package com.example.model

/**
 * Shared onboarding name rules used by both Compose and the ViewModel.
 * Names are normalized for display/storage but are not restricted to ASCII.
 */
object OnboardingInputValidator {
    fun normalizeName(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").take(30)

    fun isValidName(value: String): Boolean =
        normalizeName(value).length in 2..30
}
