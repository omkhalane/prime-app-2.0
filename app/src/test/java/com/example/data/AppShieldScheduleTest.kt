package com.example.data

import com.example.model.BlockRule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AppShieldScheduleTest {
    private fun time(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun rule(
        days: Set<Int>,
        from: String,
        to: String,
        apps: List<String> = listOf("com.example.social"),
        paused: Boolean = false
    ) = BlockRule(
        id = "test-rule",
        title = "Test",
        startsInText = "Active",
        fromTime = from,
        toTime = to,
        activeDays = days,
        blockedAppIds = apps,
        isPaused = paused
    )

    @Test fun activatesRuleOnlyInsideConfiguredWeekdayWindow() {
        val weekdayRule = rule(setOf(0, 1, 2, 3, 4), "9:00 am", "5:00 pm")
        assertEquals(setOf("com.example.social"), OpalRepository.activeRulePackages(
            listOf(weekdayRule), time(2026, Calendar.NOVEMBER, 12, 10, 0)
        ))
        assertEquals(emptySet<String>(), OpalRepository.activeRulePackages(
            listOf(weekdayRule), time(2026, Calendar.NOVEMBER, 12, 18, 0)
        ))
    }

    @Test fun supportsOvernightRulesFromPreviousActiveDay() {
        val overnightRule = rule(setOf(3), "10:00 pm", "6:00 am") // Thursday
        assertEquals(setOf("com.example.social"), OpalRepository.activeRulePackages(
            listOf(overnightRule), time(2026, Calendar.NOVEMBER, 13, 1, 0)
        ))
    }

    @Test fun pausedRulesAndMalformedTimesDoNotActivate() {
        val paused = rule(setOf(0, 1, 2, 3, 4, 5, 6), "00:00", "23:59", paused = true)
        val malformed = rule(setOf(0, 1, 2, 3, 4, 5, 6), "not a time", "5:00 pm", apps = listOf("com.example.invalid"))
        val emptyDays = rule(emptySet(), "9:00 am", "5:00 pm", apps = listOf("com.example.empty"))
        assertEquals(emptySet<String>(), OpalRepository.activeRulePackages(
            listOf(paused, malformed, emptyDays), time(2026, Calendar.NOVEMBER, 12, 10, 0)
        ))
    }
}
