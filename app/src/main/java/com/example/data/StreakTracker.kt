package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int,
    val isActiveToday: Boolean,
    val weekDaysActive: List<Boolean>, // Mon, Tue, Wed, Thu, Fri, Sat, Sun
    val totalActiveDays: Int
)

object StreakTracker {

    private const val PREFS_NAME = "prime_streak_prefs"
    private const val KEY_ACTIVE_DATES = "active_dates"
    private const val KEY_BEST_STREAK = "best_streak"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Log activity today (e.g. focus session completed, timer run, or daily check-in).
     * If user already logged today, keeps it.
     * On a new day, increments streak by +1 if consecutive!
     */
    fun logActivityToday(context: Context) {
        val prefs = getPrefs(context)
        val todayStr = dateFormat.format(Date())
        val currentSet = prefs.getStringSet(KEY_ACTIVE_DATES, emptySet())?.toMutableSet() ?: mutableSetOf()

        if (!currentSet.contains(todayStr)) {
            currentSet.add(todayStr)
            val updatedStreak = calculateStreakFromDates(currentSet)
            val best = prefs.getInt(KEY_BEST_STREAK, 0).coerceAtLeast(updatedStreak)
            prefs.edit()
                .putStringSet(KEY_ACTIVE_DATES, currentSet)
                .putInt(KEY_BEST_STREAK, best)
                .apply()
        }
    }

    /**
     * Get real streak info:
     * Starts strictly from 0 for 0 active days.
     * Increments to +1 on each consecutive day change with recorded activity.
     */
    fun getStreakInfo(context: Context): StreakInfo {
        val prefs = getPrefs(context)
        val dates = prefs.getStringSet(KEY_ACTIVE_DATES, emptySet()) ?: emptySet()
        val todayStr = dateFormat.format(Date())
        val isActiveToday = dates.contains(todayStr)

        val currentStreak = calculateStreakFromDates(dates)
        val bestStreak = prefs.getInt(KEY_BEST_STREAK, 0).coerceAtLeast(currentStreak)
        val weekDaysActive = calculateCurrentWeekActive(dates)

        return StreakInfo(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            isActiveToday = isActiveToday,
            weekDaysActive = weekDaysActive,
            totalActiveDays = dates.size
        )
    }

    /**
     * Reset streaks (for testing or fresh start).
     */
    fun resetStreak(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    /**
     * Calculate consecutive streak from date strings (yyyy-MM-dd).
     * - Returns 0 if no active dates recorded.
     * - If active today, counts backwards: today (+1), yesterday (+1 if present), etc.
     * - If not active today yet, but active yesterday: streak is alive from yesterday (+1 on day change once logged).
     * - If neither today nor yesterday is present: streak is 0 (broken or never started).
     */
    fun calculateStreakFromDates(dates: Set<String>): Int {
        if (dates.isEmpty()) return 0

        val cal = Calendar.getInstance()
        val todayStr = dateFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = dateFormat.format(cal.time)

        val checkCal = Calendar.getInstance()

        return if (dates.contains(todayStr)) {
            var streak = 0
            while (true) {
                val dateStr = dateFormat.format(checkCal.time)
                if (dates.contains(dateStr)) {
                    streak++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
            streak
        } else if (dates.contains(yesterdayStr)) {
            var streak = 0
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            while (true) {
                val dateStr = dateFormat.format(checkCal.time)
                if (dates.contains(dateStr)) {
                    streak++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
            streak
        } else {
            0
        }
    }

    private fun calculateCurrentWeekActive(dates: Set<String>): List<Boolean> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val result = mutableListOf<Boolean>()
        for (i in 0 until 7) {
            val dateStr = dateFormat.format(cal.time)
            result.add(dates.contains(dateStr))
            cal.add(Calendar.DAY_OF_WEEK, 1)
        }
        return result
    }
}
