package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.json.JSONArray
import java.util.Calendar

class AppShieldScheduleTest {
    private fun time(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test fun activatesRuleOnlyInsideConfiguredWeekdayWindow() {
        val json = JSONArray().put(org.json.JSONObject().apply {
            put("is_paused", false)
            put("active_days", JSONArray(listOf(0, 1, 2, 3, 4)))
            put("from_time", "9:00 am")
            put("to_time", "5:00 pm")
            put("blocked_apps", JSONArray(listOf("com.example.social")))
        }).toString()
        assertEquals(setOf("com.example.social"), OpalRepository.activeRulePackages(json, time(2026, Calendar.NOVEMBER, 12, 10, 0)))
        assertEquals(emptySet<String>(), OpalRepository.activeRulePackages(json, time(2026, Calendar.NOVEMBER, 12, 18, 0)))
    }

    @Test fun supportsOvernightRulesFromPreviousActiveDay() {
        val json = JSONArray().put(org.json.JSONObject().apply {
            put("is_paused", false)
            put("active_days", JSONArray(listOf(3))) // Thursday
            put("from_time", "10:00 pm")
            put("to_time", "6:00 am")
            put("blocked_apps", JSONArray(listOf("com.example.social")))
        }).toString()
        assertEquals(setOf("com.example.social"), OpalRepository.activeRulePackages(json, time(2026, Calendar.NOVEMBER, 13, 1, 0)))
    }

    @Test fun pausedRulesAndMalformedTimesDoNotActivate() {
        val json = JSONArray()
            .put(org.json.JSONObject().apply {
                put("is_paused", true)
                put("active_days", JSONArray(listOf(0, 1, 2, 3, 4, 5, 6)))
                put("from_time", "00:00")
                put("to_time", "23:59")
                put("blocked_apps", JSONArray(listOf("com.example.paused")))
            })
            .put(org.json.JSONObject().apply {
                put("is_paused", false)
                put("active_days", JSONArray(listOf(0, 1, 2, 3, 4, 5, 6)))
                put("from_time", "not a time")
                put("to_time", "5:00 pm")
                put("blocked_apps", JSONArray(listOf("com.example.invalid")))
            }).toString()
        assertEquals(emptySet<String>(), OpalRepository.activeRulePackages(json, time(2026, Calendar.NOVEMBER, 12, 10, 0)))
    }
}
