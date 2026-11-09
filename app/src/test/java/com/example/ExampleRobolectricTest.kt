package com.example

import com.example.model.BlockRule
import com.example.model.NavigationTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun testAppNameString() {
        assertEquals("Prime", "Prime")
    }

    @Test
    fun testNavigationTabShields() {
        assertEquals("Shields", NavigationTab.MY_APPS.title)
    }

    @Test
    fun testCustomShieldRule() {
        val rule = BlockRule(
            id = "custom_shield_1",
            title = "Deep Study",
            startsInText = "Active 8am–12pm",
            fromTime = "8:00 am",
            toTime = "12:00 pm",
            daysLabel = "Weekdays",
            activeDays = setOf(0, 1, 2, 3, 4),
            blockedAppIds = listOf("com.instagram.android"),
            isHardMode = true,
            allowBreaks = false,
            isPaused = false,
            isCustom = true
        )

        assertEquals("Deep Study", rule.title)
        assertTrue(rule.isCustom)
        assertTrue(rule.isHardMode)
        assertFalse(rule.allowBreaks)
        assertFalse(rule.isPaused)
        assertEquals(5, rule.activeDays.size)
    }
}
