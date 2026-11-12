package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingInputValidatorTest {
    @Test
    fun normalizesWhitespaceWithoutDroppingUnicodeNames() {
        assertEquals("Aarav Kulkarni", OnboardingInputValidator.normalizeName("  Aarav   Kulkarni  "))
        assertEquals("李 明", OnboardingInputValidator.normalizeName(" 李   明 "))
    }

    @Test
    fun rejectsBlankAndSingleCharacterNames() {
        assertFalse(OnboardingInputValidator.isValidName(""))
        assertFalse(OnboardingInputValidator.isValidName("  "))
        assertFalse(OnboardingInputValidator.isValidName("A"))
    }

    @Test
    fun acceptsNamesWithinSupportedLength() {
        assertTrue(OnboardingInputValidator.isValidName("Om"))
        assertTrue(OnboardingInputValidator.isValidName("Aarav Kulkarni"))
        assertTrue(OnboardingInputValidator.isValidName("A".repeat(30)))
    }

    @Test
    fun rejectsNamesLongerThanThirtyCharacters() {
        assertFalse(OnboardingInputValidator.isValidName("A".repeat(31)))
    }
}
