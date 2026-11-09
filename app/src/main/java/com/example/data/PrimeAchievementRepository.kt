package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BadgeCategory
import com.example.model.BadgeLevel
import com.example.model.BadgeRarity
import com.example.model.DailyQuest
import com.example.model.PrimeBadge
import com.example.model.PrimeProgression
import com.example.model.PrimeRankTier
import com.example.model.PrimeScoreBreakdown
import com.example.model.PrimeTargets
import com.example.model.WeeklyMission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class PrimeAchievementRepository(private val context: Context? = null) {

    private val prefs: SharedPreferences? =
        context?.getSharedPreferences("prime_achievements_v3_prefs", Context.MODE_PRIVATE)

    private val _targetsFlow = MutableStateFlow(loadTargets())
    val targetsFlow: StateFlow<PrimeTargets> = _targetsFlow.asStateFlow()

    private val _badgesFlow = MutableStateFlow(loadBadges())
    val badgesFlow: StateFlow<List<PrimeBadge>> = _badgesFlow.asStateFlow()

    private val _dailyQuestsFlow = MutableStateFlow(loadDailyQuests())
    val dailyQuestsFlow: StateFlow<List<DailyQuest>> = _dailyQuestsFlow.asStateFlow()

    private val _weeklyMissionsFlow = MutableStateFlow(loadWeeklyMissions())
    val weeklyMissionsFlow: StateFlow<List<WeeklyMission>> = _weeklyMissionsFlow.asStateFlow()

    private val _progressionFlow = MutableStateFlow(calculateProgression(_badgesFlow.value))
    val progressionFlow: StateFlow<PrimeProgression> = _progressionFlow.asStateFlow()

    private fun loadTargets(): PrimeTargets {
        return PrimeTargets(
            screenTimeTargetMinutes = prefs?.getInt("target_screen_minutes", 180) ?: 180,
            focusTargetMinutes = prefs?.getInt("target_focus_minutes", 120) ?: 120,
            blockingTargetMinutes = prefs?.getInt("target_blocking_minutes", 180) ?: 180
        )
    }

    fun updateTargets(screenTarget: Int, focusTarget: Int, blockingTarget: Int) {
        val updated = PrimeTargets(
            screenTimeTargetMinutes = screenTarget.coerceIn(30, 600),
            focusTargetMinutes = focusTarget.coerceIn(15, 480),
            blockingTargetMinutes = blockingTarget.coerceIn(15, 600)
        )
        prefs?.edit()
            ?.putInt("target_screen_minutes", updated.screenTimeTargetMinutes)
            ?.putInt("target_focus_minutes", updated.focusTargetMinutes)
            ?.putInt("target_blocking_minutes", updated.blockingTargetMinutes)
            ?.apply()
        _targetsFlow.value = updated
    }

    private fun loadBadges(): List<PrimeBadge> {
        val unlockedJson = prefs?.getString("unlocked_badges_set", null)
        val unlockedMap = mutableMapOf<String, String>() // id to date
        if (unlockedJson != null) {
            try {
                val array = JSONArray(unlockedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    unlockedMap[obj.getString("id")] = obj.optString("date", "Unlocked")
                }
            } catch (_: Exception) {}
        }

        // These IDs were pre-unlocked by the old demo seed, not by verified user activity.
        if (prefs?.getBoolean("seeded_badge_cleanup_v1", false) != true) {
            val seededIds = setOf(
                "time_guardian", "screen_slayer", "time_reclaimer",
                "focus_hunter", "deep_thinker", "unbreakable_focus",
                "shield_bearer", "distraction_destroyer", "iron_will",
                "spark", "pomodoro_rookie", "flow_state"
            )
            seededIds.forEach { unlockedMap.remove(it) }
            val cleaned = JSONArray()
            unlockedMap.forEach { (id, date) ->
                cleaned.put(JSONObject().apply { put("id", id); put("date", date) })
            }
            prefs?.edit()?.putString("unlocked_badges_set", cleaned.toString())
                ?.putBoolean("seeded_badge_cleanup_v1", true)?.apply()
        }

        val baseList = generateAllBadges()
        return baseList.map { badge ->
            val customUnlockedDate = unlockedMap[badge.id]
            if (customUnlockedDate != null) {
                badge.copy(
                    isUnlocked = true,
                    unlockedDate = customUnlockedDate,
                    currentProgress = badge.targetRequirement
                )
            } else {
                badge
            }
        }
    }

    private fun saveBadgesState(badges: List<PrimeBadge>) {
        val array = JSONArray()
        badges.filter { it.isUnlocked }.forEach { badge ->
            val obj = JSONObject()
            obj.put("id", badge.id)
            obj.put("date", badge.unlockedDate ?: "Unlocked")
            array.put(obj)
        }
        prefs?.edit()?.putString("unlocked_badges_set", array.toString())?.apply()
    }

    fun pinBadgeToFavorite(badgeId: String) {
        val current = _progressionFlow.value.pinnedBadgeIds.toMutableList()
        if (current.contains(badgeId)) {
            current.remove(badgeId)
        } else {
            if (current.size >= 3) {
                current.removeAt(0)
            }
            current.add(badgeId)
        }
        prefs?.edit()?.putStringSet("pinned_badge_ids", current.toSet())?.apply()
        _progressionFlow.update { it.copy(pinnedBadgeIds = current) }
    }

    fun unlockBadge(badgeId: String) {
        val todayStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
        val updated = _badgesFlow.value.map { badge ->
            if (badge.id == badgeId && !badge.isUnlocked) {
                badge.copy(
                    isUnlocked = true,
                    unlockedDate = todayStr,
                    currentProgress = badge.targetRequirement
                )
            } else {
                badge
            }
        }
        _badgesFlow.value = updated
        saveBadgesState(updated)
        _progressionFlow.value = calculateProgression(updated)
    }

    fun claimDailyQuest(questId: String) {
        val quest = _dailyQuestsFlow.value.firstOrNull {
            it.id == questId && it.isCompleted && !it.isClaimed
        } ?: return
        val updated = _dailyQuestsFlow.value.map {
            if (it.id == questId) it.copy(isClaimed = true) else it
        }
        _dailyQuestsFlow.value = updated
        val key = "claimed_daily_quests_${todayKey()}"
        val claimed = prefs?.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        claimed.add(questId)
        val curXp = prefs?.getInt("bonus_xp", 0) ?: 0
        prefs?.edit()?.putStringSet(key, claimed)?.putInt("bonus_xp", curXp + quest.xpReward)?.apply()
        _progressionFlow.value = calculateProgression(_badgesFlow.value)
    }

    fun claimWeeklyMission(missionId: String) {
        val mission = _weeklyMissionsFlow.value.firstOrNull {
            it.id == missionId && it.isCompleted && !it.isClaimed
        } ?: return
        val updated = _weeklyMissionsFlow.value.map {
            if (it.id == missionId) it.copy(isClaimed = true) else it
        }
        _weeklyMissionsFlow.value = updated
        val key = "claimed_weekly_missions_${weekKey()}"
        val claimed = prefs?.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        claimed.add(missionId)
        val curXp = prefs?.getInt("bonus_xp", 0) ?: 0
        prefs?.edit()?.putStringSet(key, claimed)?.putInt("bonus_xp", curXp + mission.xpReward)?.apply()
        _progressionFlow.value = calculateProgression(_badgesFlow.value)
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun weekKey(): String {
        val calendar = java.util.Calendar.getInstance().apply {
            firstDayOfWeek = java.util.Calendar.SUNDAY
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
        }
        return SimpleDateFormat("yyyy-'W'ww", Locale.US).format(calendar.time)
    }

    fun updateDailyProgress(screenMinutes: Int?, completedPomodoroSessions: Int, blockingMinutes: Int) {
        val screenTarget = _targetsFlow.value.screenTimeTargetMinutes
        val updated = _dailyQuestsFlow.value.map { quest ->
            when (quest.id) {
                "quest_screen_time" -> quest.copy(
                    description = "Keep phone usage below $screenTarget min today",
                    currentProgress = screenMinutes ?: 0,
                    targetProgress = screenTarget,
                    isCompleted = screenMinutes != null && screenMinutes <= screenTarget
                )
                "quest_pomodoro" -> quest.copy(
                    currentProgress = completedPomodoroSessions.coerceAtLeast(0),
                    targetProgress = 2,
                    isCompleted = completedPomodoroSessions >= 2
                )
                "quest_blocking" -> quest.copy(
                    currentProgress = blockingMinutes.coerceAtLeast(0),
                    targetProgress = 60,
                    isCompleted = blockingMinutes >= 60
                )
                else -> quest
            }
        }
        _dailyQuestsFlow.value = updated
    }

    fun updateWeeklyFocusProgress(completedFocusSessions: Int) {
        val updated = _weeklyMissionsFlow.value.map { mission ->
            if (mission.id == "mission_focus_sessions") {
                mission.copy(
                    currentProgress = completedFocusSessions.coerceAtLeast(0),
                    targetProgress = 10,
                    isCompleted = completedFocusSessions >= 10
                )
            } else mission
        }
        _weeklyMissionsFlow.value = updated
    }

    private fun loadDailyQuests(): List<DailyQuest> {
        val claimedIds = prefs?.getStringSet("claimed_daily_quests_${todayKey()}", mutableSetOf()).orEmpty()
        return listOf(
            DailyQuest(
                id = "quest_screen_time",
                title = "Screen Time Control",
                description = "Keep phone usage below ${_targetsFlow.value.screenTimeTargetMinutes} min today",
                currentProgress = 0,
                targetProgress = _targetsFlow.value.screenTimeTargetMinutes,
                unit = "min",
                xpReward = 50,
                isCompleted = false,
                isClaimed = claimedIds.contains("quest_screen_time")
            ),
            DailyQuest(
                id = "quest_pomodoro",
                title = "Double Pomodoro",
                description = "Complete 2 verified Pomodoro rounds",
                currentProgress = 0,
                targetProgress = 2,
                unit = "rounds",
                xpReward = 60,
                isCompleted = false,
                isClaimed = claimedIds.contains("quest_pomodoro")
            ),
            DailyQuest(
                id = "quest_blocking",
                title = "Shield Wall",
                description = "Maintain active shields for 60 minutes",
                currentProgress = 0,
                targetProgress = 60,
                unit = "min",
                xpReward = 50,
                isCompleted = false,
                isClaimed = claimedIds.contains("quest_blocking")
            )
        )
    }

    private fun loadWeeklyMissions(): List<WeeklyMission> {
        val claimedIds = prefs?.getStringSet("claimed_weekly_missions_${weekKey()}", mutableSetOf()).orEmpty()
        return listOf(
            WeeklyMission(
                id = "mission_elite_avg",
                title = "Elite Standard",
                description = "Achieve 700+ weekly average PRIME score",
                currentProgress = 0,
                targetProgress = 700,
                unit = "score",
                xpReward = 150,
                isCompleted = false,
                isClaimed = claimedIds.contains("mission_elite_avg")
            ),
            WeeklyMission(
                id = "mission_focus_sessions",
                title = "Deep Focus Habit",
                description = "Complete 10 focused sessions this week",
                currentProgress = 0,
                targetProgress = 10,
                unit = "sessions",
                xpReward = 120,
                isCompleted = false,
                isClaimed = claimedIds.contains("mission_focus_sessions")
            ),
            WeeklyMission(
                id = "mission_reclaim_hours",
                title = "Time Liberator",
                description = "Reclaim 3 hours against your baseline",
                currentProgress = 0,
                targetProgress = 3,
                unit = "hours",
                xpReward = 150,
                isCompleted = false,
                isClaimed = claimedIds.contains("mission_reclaim_hours")
            )
        )
    }

    private fun calculateProgression(badges: List<PrimeBadge>): PrimeProgression {
        val unlocked = badges.filter { it.isUnlocked }
        val badgeXp = unlocked.sumOf { it.xpValue }
        val bonusXp = prefs?.getInt("bonus_xp", 0) ?: 0
        val totalXp = badgeXp + bonusXp

        val level = (1 + (totalXp / 400)).coerceAtLeast(1)
        val currentLevelXp = totalXp % 400
        val xpForNext = 400

        val savedPinned = prefs?.getStringSet("pinned_badge_ids", null)
            ?: emptySet()

        val totalCount = badges.size
        val unlockedCount = unlocked.size
        val completionPct = if (totalCount > 0) (unlockedCount.toFloat() / totalCount * 100f) else 0f

        return PrimeProgression(
            totalXp = totalXp,
            primeLevel = level,
            currentLevelXp = currentLevelXp,
            xpForNextLevel = xpForNext,
            pinnedBadgeIds = savedPinned.toList(),
            totalBadgesCount = totalCount,
            unlockedBadgesCount = unlockedCount,
            completionPercentage = (completionPct * 10).roundToInt() / 10f
        )
    }

    /**
     * Exact 42 achievements:
     * 36 Core Badges (12 Categories A to L x 3 badges each) + 6 Special Rare Badges.
     * All badges start locked with zero progress; unlocks are earned from recorded activity.
     */
    private fun generateAllBadges(): List<PrimeBadge> {
        val list = mutableListOf<PrimeBadge>()

        // ==================== Category A — Screen-time discipline ====================
        list.add(
            PrimeBadge(
                id = "time_guardian",
                name = "Time Guardian",
                category = BadgeCategory.CATEGORY_A,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Stay within screen-time target for 3 days. (Next: Level II · 7 days)",
                currentProgress = 0f,
                targetRequirement = 7f,
                unit = "days",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "hourglass"
            )
        )
        list.add(
            PrimeBadge(
                id = "screen_slayer",
                name = "Screen Slayer",
                category = BadgeCategory.CATEGORY_A,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Reduce screen time by 10% against baseline. (Next: Level II · 20%)",
                currentProgress = 0f,
                targetRequirement = 20f,
                unit = "%",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "phone"
            )
        )
        list.add(
            PrimeBadge(
                id = "time_reclaimer",
                name = "Time Reclaimer",
                category = BadgeCategory.CATEGORY_A,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Reclaim 5 hours against baseline. (Next: Level II · 20 hours)",
                currentProgress = 0f,
                targetRequirement = 20f,
                unit = "hrs",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "bolt"
            )
        )

        // ==================== Category B — Focus mastery ====================
        list.add(
            PrimeBadge(
                id = "focus_hunter",
                name = "Focus Hunter",
                category = BadgeCategory.CATEGORY_B,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 5 focus sessions. (Next: Level II · 25 sessions)",
                currentProgress = 0f,
                targetRequirement = 25f,
                unit = "sessions",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "target"
            )
        )
        list.add(
            PrimeBadge(
                id = "deep_thinker",
                name = "Deep Thinker",
                category = BadgeCategory.CATEGORY_B,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Accumulate 5 focus hours. (Next: Level II · 25 hours)",
                currentProgress = 0f,
                targetRequirement = 25f,
                unit = "hrs",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "self_improvement"
            )
        )
        list.add(
            PrimeBadge(
                id = "unbreakable_focus",
                name = "Unbreakable Focus",
                category = BadgeCategory.CATEGORY_B,
                level = BadgeLevel.LEVEL_II,
                rarity = BadgeRarity.UNCOMMON,
                xpValue = 250,
                description = "Complete a 90-minute session. (Next: Level III · 120 min)",
                currentProgress = 0f,
                targetRequirement = 120f,
                unit = "min",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "shield"
            )
        )

        // ==================== Category C — App & website blocking ====================
        list.add(
            PrimeBadge(
                id = "shield_bearer",
                name = "Shield Bearer",
                category = BadgeCategory.CATEGORY_C,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 5 successful blocking sessions. (Next: Level II · 25 sessions)",
                currentProgress = 0f,
                targetRequirement = 25f,
                unit = "sessions",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "shield"
            )
        )
        list.add(
            PrimeBadge(
                id = "distraction_destroyer",
                name = "Distraction Destroyer",
                category = BadgeCategory.CATEGORY_C,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Block 3 apps or websites. (Next: Level II · 10)",
                currentProgress = 0f,
                targetRequirement = 10f,
                unit = "apps",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "block"
            )
        )
        list.add(
            PrimeBadge(
                id = "iron_will",
                name = "Iron Will",
                category = BadgeCategory.CATEGORY_C,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 5 hours of successful protection. (Next: Level II · 25 hours)",
                currentProgress = 0f,
                targetRequirement = 25f,
                unit = "hrs",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "lock"
            )
        )

        // ==================== Category D — Streaks & consistency ====================
        list.add(
            PrimeBadge(
                id = "spark",
                name = "Spark",
                category = BadgeCategory.CATEGORY_D,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Maintain a 3-day streak. (Next: Level II · 7 days)",
                currentProgress = 0f,
                targetRequirement = 7f,
                unit = "days",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "bolt"
            )
        )
        list.add(
            PrimeBadge(
                id = "relentless",
                name = "Relentless",
                category = BadgeCategory.CATEGORY_D,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Reach a 21-day streak. (Next: Level II · 30 days)",
                currentProgress = 0f,
                targetRequirement = 21f,
                unit = "days",
                iconType = "star"
            )
        )
        list.add(
            PrimeBadge(
                id = "phoenix",
                name = "Phoenix",
                category = BadgeCategory.CATEGORY_D,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.UNCOMMON,
                xpValue = 100,
                description = "Reach a 90-day streak. (Next: Level II · 180 days)",
                currentProgress = 0f,
                targetRequirement = 90f,
                unit = "days",
                iconType = "star"
            )
        )

        // ==================== Category E — Pomodoro mastery ====================
        list.add(
            PrimeBadge(
                id = "pomodoro_rookie",
                name = "Pomodoro Rookie",
                category = BadgeCategory.CATEGORY_E,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 4 Pomodoros. (Next: Level II · 25)",
                currentProgress = 0f,
                targetRequirement = 25f,
                unit = "rounds",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "timer"
            )
        )
        list.add(
            PrimeBadge(
                id = "time_architect",
                name = "Time Architect",
                category = BadgeCategory.CATEGORY_E,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 5 Pomodoro hours. (Next: Level II · 25 hours)",
                currentProgress = 0f,
                targetRequirement = 5f,
                unit = "hrs",
                iconType = "timer"
            )
        )
        list.add(
            PrimeBadge(
                id = "flow_state",
                name = "Flow State",
                category = BadgeCategory.CATEGORY_E,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 3 consecutive Pomodoros. (Next: Level II · 6)",
                currentProgress = 0f,
                targetRequirement = 3f,
                unit = "rounds",
                isUnlocked = false,
                unlockedDate = null,
                iconType = "timer"
            )
        )

        // ==================== Category F — Digital detox ====================
        list.add(
            PrimeBadge(
                id = "offline_soul",
                name = "Offline Soul",
                category = BadgeCategory.CATEGORY_F,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 1 hour offline. (Next: Level II · 5 hours)",
                currentProgress = 0f,
                targetRequirement = 5f,
                unit = "hrs",
                iconType = "park"
            )
        )
        list.add(
            PrimeBadge(
                id = "digital_detoxer",
                name = "Digital Detoxer",
                category = BadgeCategory.CATEGORY_F,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 3 detox sessions. (Next: Level II · 15)",
                currentProgress = 0f,
                targetRequirement = 3f,
                unit = "sessions",
                iconType = "park"
            )
        )
        list.add(
            PrimeBadge(
                id = "digital_monk",
                name = "Digital Monk",
                category = BadgeCategory.CATEGORY_F,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete a 4-hour detox. (Next: Level II · 8 hours)",
                currentProgress = 0f,
                targetRequirement = 4f,
                unit = "hrs",
                iconType = "self_improvement"
            )
        )

        // ==================== Category G — Morning & night discipline ====================
        list.add(
            PrimeBadge(
                id = "early_riser",
                name = "Early Riser",
                category = BadgeCategory.CATEGORY_G,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 3 morning phone-free routines. (Next: Level II · 14)",
                currentProgress = 0f,
                targetRequirement = 3f,
                unit = "routines",
                iconType = "hourglass"
            )
        )
        list.add(
            PrimeBadge(
                id = "night_guardian",
                name = "Night Guardian",
                category = BadgeCategory.CATEGORY_G,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 3 phone-free bedtime routines. (Next: Level II · 14)",
                currentProgress = 0f,
                targetRequirement = 3f,
                unit = "routines",
                iconType = "nightlight"
            )
        )
        list.add(
            PrimeBadge(
                id = "perfect_balance",
                name = "Perfect Balance",
                category = BadgeCategory.CATEGORY_G,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Maintain both morning and night routines for 3 days.",
                currentProgress = 0f,
                targetRequirement = 3f,
                unit = "days",
                iconType = "nightlight"
            )
        )

        // ==================== Category H — Goal achievement ====================
        list.add(
            PrimeBadge(
                id = "goal_setter",
                name = "Goal Setter",
                category = BadgeCategory.CATEGORY_H,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 5 goals. (Next: Level II · 25)",
                currentProgress = 0f,
                targetRequirement = 5f,
                unit = "goals",
                iconType = "target"
            )
        )
        list.add(
            PrimeBadge(
                id = "goal_crusher",
                name = "Goal Crusher",
                category = BadgeCategory.CATEGORY_H,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 3 daily plans. (Next: Level II · 14)",
                currentProgress = 0f,
                targetRequirement = 3f,
                unit = "plans",
                iconType = "target"
            )
        )
        list.add(
            PrimeBadge(
                id = "mission_accomplished",
                name = "Mission Accomplished",
                category = BadgeCategory.CATEGORY_H,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Achieve 5 weekly goals. (Next: Level II · 20)",
                currentProgress = 0f,
                targetRequirement = 5f,
                unit = "goals",
                iconType = "target"
            )
        )

        // ==================== Category I — Stopwatch & timer ====================
        list.add(
            PrimeBadge(
                id = "clock_starter",
                name = "Clock Starter",
                category = BadgeCategory.CATEGORY_I,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 5 timer sessions. (Next: Level II · 25)",
                currentProgress = 0f,
                targetRequirement = 5f,
                unit = "sessions",
                iconType = "timer"
            )
        )
        list.add(
            PrimeBadge(
                id = "time_keeper",
                name = "Time Keeper",
                category = BadgeCategory.CATEGORY_I,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Accumulate 5 hours on timers. (Next: Level II · 25 hours)",
                currentProgress = 0f,
                targetRequirement = 5f,
                unit = "hrs",
                iconType = "timer"
            )
        )
        list.add(
            PrimeBadge(
                id = "master_of_time",
                name = "Master of Time",
                category = BadgeCategory.CATEGORY_I,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 10 timer sessions without abandoning. (Next: Level II · 50)",
                currentProgress = 0f,
                targetRequirement = 10f,
                unit = "sessions",
                iconType = "timer"
            )
        )

        // ==================== Category J — Weekly digital wellbeing ====================
        list.add(
            PrimeBadge(
                id = "mindful_mind",
                name = "Mindful Mind",
                category = BadgeCategory.CATEGORY_J,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 1 weekly wellbeing goal. (Next: Level II · 4)",
                currentProgress = 0f,
                targetRequirement = 1f,
                unit = "goals",
                iconType = "self_improvement"
            )
        )
        list.add(
            PrimeBadge(
                id = "life_balancer",
                name = "Life Balancer",
                category = BadgeCategory.CATEGORY_J,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Meet all 3 daily targets for 3 days. (Next: Level II · 7)",
                currentProgress = 0f,
                targetRequirement = 3f,
                unit = "days",
                iconType = "diamond"
            )
        )
        list.add(
            PrimeBadge(
                id = "inner_peace",
                name = "Inner Peace",
                category = BadgeCategory.CATEGORY_J,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Complete 5 mindful breaks. (Next: Level II · 25)",
                currentProgress = 0f,
                targetRequirement = 5f,
                unit = "breaks",
                iconType = "self_improvement"
            )
        )

        // ==================== Category K — Social & community ====================
        list.add(
            PrimeBadge(
                id = "challenger",
                name = "Challenger",
                category = BadgeCategory.CATEGORY_K,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Join 1 challenge. (Next: Level II · 5)",
                currentProgress = 0f,
                targetRequirement = 1f,
                unit = "challenges",
                iconType = "trophy"
            )
        )
        list.add(
            PrimeBadge(
                id = "top_performer",
                name = "Top Performer",
                category = BadgeCategory.CATEGORY_K,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.UNCOMMON,
                xpValue = 100,
                description = "Finish in the top 50% of a leaderboard. (Next: Level II · top 20%)",
                currentProgress = 0f,
                targetRequirement = 1f,
                unit = "rank",
                iconType = "trophy"
            )
        )
        list.add(
            PrimeBadge(
                id = "prime_champion",
                name = "Prime Champion",
                category = BadgeCategory.CATEGORY_K,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Win 1 challenge. (Next: Level II · 5)",
                currentProgress = 0f,
                targetRequirement = 1f,
                unit = "wins",
                iconType = "trophy"
            )
        )

        // ==================== Category L — Overall PRIME journey ====================
        list.add(
            PrimeBadge(
                id = "the_comeback",
                name = "The Comeback",
                category = BadgeCategory.CATEGORY_L,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.COMMON,
                xpValue = 100,
                description = "Earn 3 days above 500 score. (Next: Level II · 7 days >600)",
                currentProgress = 0f,
                targetRequirement = 3f,
                unit = "days",
                iconType = "star"
            )
        )
        list.add(
            PrimeBadge(
                id = "rising_prime",
                name = "Rising Prime",
                category = BadgeCategory.CATEGORY_L,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.UNCOMMON,
                xpValue = 100,
                description = "Reach a 7-day average of 600. (Next: Level II · 750)",
                currentProgress = 0f,
                targetRequirement = 600f,
                unit = "score",
                iconType = "diamond"
            )
        )
        list.add(
            PrimeBadge(
                id = "become_your_prime",
                name = "Become Your Prime",
                category = BadgeCategory.CATEGORY_L,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.RARE,
                xpValue = 100,
                description = "Reach 900 weekly average score. (Next: Maintain 30 days)",
                currentProgress = 0f,
                targetRequirement = 900f,
                unit = "avg",
                iconType = "diamond"
            )
        )

        // ==================== Special Badges — Rare achievements (6) ====================
        list.add(
            PrimeBadge(
                id = "the_first_prime",
                name = "The First Prime",
                category = BadgeCategory.SPECIAL,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.RARE,
                xpValue = 300,
                description = "Complete your first 7 days on PRIME.",
                currentProgress = 0f,
                targetRequirement = 7f,
                unit = "days",
                isSpecial = true,
                iconType = "diamond"
            )
        )
        list.add(
            PrimeBadge(
                id = "no_excuses",
                name = "No Excuses",
                category = BadgeCategory.SPECIAL,
                level = BadgeLevel.LEVEL_I,
                rarity = BadgeRarity.RARE,
                xpValue = 300,
                description = "Complete 7 consecutive days without breaking a scheduled protection session.",
                currentProgress = 0f,
                targetRequirement = 7f,
                unit = "days",
                isSpecial = true,
                iconType = "shield"
            )
        )
        list.add(
            PrimeBadge(
                id = "100_hours_reclaimed",
                name = "100 Hours Reclaimed",
                category = BadgeCategory.SPECIAL,
                level = BadgeLevel.LEVEL_II,
                rarity = BadgeRarity.EPIC,
                xpValue = 600,
                description = "Reclaim 100 hours against personal baseline.",
                currentProgress = 0f,
                targetRequirement = 100f,
                unit = "hrs",
                isSpecial = true,
                iconType = "bolt"
            )
        )
        list.add(
            PrimeBadge(
                id = "the_unstoppable",
                name = "The Unstoppable",
                category = BadgeCategory.SPECIAL,
                level = BadgeLevel.LEVEL_III,
                rarity = BadgeRarity.LEGENDARY,
                xpValue = 1000,
                description = "Maintain a 100-day discipline streak.",
                currentProgress = 0f,
                targetRequirement = 100f,
                unit = "days",
                isSpecial = true,
                iconType = "star"
            )
        )
        list.add(
            PrimeBadge(
                id = "24_hours_offline",
                name = "24 Hours Offline",
                category = BadgeCategory.SPECIAL,
                level = BadgeLevel.LEVEL_III,
                rarity = BadgeRarity.LEGENDARY,
                xpValue = 1000,
                description = "Complete one verified 24-hour digital detox.",
                currentProgress = 0f,
                targetRequirement = 24f,
                unit = "hrs",
                isSpecial = true,
                iconType = "park"
            )
        )
        list.add(
            PrimeBadge(
                id = "prime_legend",
                name = "PRIME Legend",
                category = BadgeCategory.SPECIAL,
                level = BadgeLevel.LEVEL_III,
                rarity = BadgeRarity.MYTHIC,
                xpValue = 2000,
                description = "Unlock all 36 core Level III badges.",
                currentProgress = 0f,
                targetRequirement = 36f,
                unit = "badges",
                isSpecial = true,
                iconType = "trophy"
            )
        )

        return list
    }
}
