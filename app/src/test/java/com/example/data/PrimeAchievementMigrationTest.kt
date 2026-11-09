package com.example.data

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrimeAchievementMigrationTest {

    @Test
    fun clearsUnverifiedDemoXpClaimsBadgesAndPins() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("prime_achievements_v3_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("bonus_xp", 450 + 120)
            .putStringSet("claimed_daily_quests_2026-11-09", setOf("quest_screen_time", "quest_pomodoro"))
            .putStringSet("claimed_weekly_missions_2026-W46", setOf("mission_elite_avg"))
            .putStringSet("pinned_badge_ids", setOf("time_guardian", "focus_hunter"))
            .putString(
                "unlocked_badges_set",
                """[{"id":"time_guardian","date":"Yesterday"},{"id":"focus_hunter","date":"Yesterday"}]"""
            )
            .apply()

        val repository = PrimeAchievementRepository(context)

        assertEquals(0, repository.progressionFlow.value.totalXp)
        assertTrue(repository.progressionFlow.value.pinnedBadgeIds.isEmpty())
        assertTrue(repository.badgesFlow.value.none { it.isUnlocked })
        assertTrue(repository.dailyQuestsFlow.value.none { it.isClaimed })
        assertTrue(repository.weeklyMissionsFlow.value.none { it.isClaimed })
        assertFalse(prefs.contains("claimed_daily_quests_2026-11-09"))
        assertFalse(prefs.contains("claimed_weekly_missions_2026-W46"))
        assertEquals(0, prefs.getInt("bonus_xp", -1))
    }
}
