package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BlockRule
import com.example.model.BlockedAppItem
import com.example.model.TimerPreset
import com.example.model.UserProfile
import com.example.model.AlarmItem
import com.example.model.ClockPreferences
import com.example.model.AlarmMode
import com.example.model.AlarmRecurrence
import com.example.model.ChallengeCategory
import com.example.model.ChallengeConfig
import com.example.model.ChallengeDifficulty
import com.example.model.PomodoroSessionType
import com.example.model.PomodoroSettings
import com.example.model.PomodoroState
import com.example.model.StopwatchLap
import com.example.alarm.AlarmSchedulerHelper
import com.example.service.OpalAccessibilityService
import com.example.util.FaviconManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import java.text.ParsePosition

class OpalRepository(
    private val context: Context? = null,
    private val loadAppsOnInit: Boolean = true
) {

    companion object {
        /** Re-evaluate scheduled app shields without querying UsageStats or creating a repository. */
        fun syncTimeBasedRuleBlocking(context: Context) {
            val prefs = context.getSharedPreferences("prime_focus_prefs", Context.MODE_PRIVATE)
            val manual = prefs.getStringSet("blocked_app_ids", emptySet()).orEmpty()
                .filterNot { it.startsWith("site_") }.toSet()
            val timer = prefs.getStringSet("timer_session_blocked_packages", emptySet()).orEmpty()
            val scheduled = activeRulePackagesFromJson(prefs.getString("block_rules_json", null), System.currentTimeMillis())
            val packageTargets = scheduled.filterNot { it.startsWith("site_") }.toSet()
            val customWebsiteEntries = prefs.getStringSet("custom_websites_set", emptySet()).orEmpty()
            val savedWebsiteDomains = customWebsiteEntries.mapNotNull { entry ->
                val parts = entry.split(":", limit = 2)
                if (parts.size == 2 && parts[1].toBoolean()) parts[0] else null
            }.toSet()
            val websiteIdToDomain = customWebsiteEntries.mapNotNull { entry ->
                val domain = entry.substringBefore(':').trim()
                if (domain.isBlank()) null else ("site_" + domain.replace(".", "_")) to domain
            }.toMap()
            val scheduledWebsiteDomains = scheduled.mapNotNull { websiteIdToDomain[it] }.toSet()
            OpalAccessibilityService.blockedPackages = manual + timer + packageTargets
            OpalAccessibilityService.updateCustomBlockedWebsites(savedWebsiteDomains + scheduledWebsiteDomains)
            OpalAccessibilityService.isBlockingActive =
                OpalAccessibilityService.blockedPackages.isNotEmpty() ||
                    OpalAccessibilityService.blockedWebsites.isNotEmpty()
        }

        private fun activeRulePackagesFromJson(json: String?, nowMillis: Long): Set<String> {
            if (json.isNullOrBlank()) return emptySet()
            return try {
                val jsonRules = JSONArray(json)
                val rules = (0 until jsonRules.length()).mapNotNull { index ->
                    val obj = jsonRules.optJSONObject(index) ?: return@mapNotNull null
                    val daysArray = obj.optJSONArray("active_days")
                    val days = if (daysArray == null) setOf(0, 1, 2, 3, 4)
                    else (0 until daysArray.length()).mapNotNull {
                        daysArray.optInt(it, -1).takeIf { day -> day in 0..6 }
                    }.toSet()
                    val appsArray = obj.optJSONArray("blocked_apps")
                    val apps = if (appsArray == null) emptyList() else
                        (0 until appsArray.length()).map { appsArray.optString(it) }.filter { it.isNotBlank() }
                    BlockRule(
                        id = obj.optString("id", "saved_rule_$index"),
                        title = obj.optString("title", "Shield"),
                        startsInText = obj.optString("starts_in", "Active"),
                        fromTime = obj.optString("from_time", "9:00 am"),
                        toTime = obj.optString("to_time", "5:00 pm"),
                        daysLabel = obj.optString("days_label", "Weekdays"),
                        activeDays = days,
                        blockedAppIds = apps,
                        isHardMode = obj.optBoolean("is_hard_mode", false),
                        allowBreaks = obj.optBoolean("allow_breaks", true),
                        isPaused = obj.optBoolean("is_paused", false),
                        isCustom = obj.optBoolean("is_custom", false)
                    )
                }
                activeRulePackages(rules, nowMillis)
            } catch (_: Exception) {
                emptySet()
            }
        }

        /** Pure evaluator: safe to unit-test without Android framework JSON or services. */
        internal fun activeRulePackages(rules: List<BlockRule>, nowMillis: Long): Set<String> {
            val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val today = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Monday ... 6=Sunday
            val yesterday = (today + 6) % 7
            val blocked = mutableSetOf<String>()
            for (rule in rules) {
                if (rule.isPaused || rule.blockedAppIds.isEmpty()) continue
                val days = rule.activeDays.filter { it in 0..6 }.toSet()
                if (days.isEmpty()) continue
                val start = parseRuleTime(rule.fromTime) ?: continue
                val end = parseRuleTime(rule.toTime) ?: continue
                val active = if (end > start) {
                    today in days && minute in start until end
                } else {
                    (today in days && minute >= start) || (minute < end && yesterday in days)
                }
                if (active) blocked.addAll(rule.blockedAppIds.filter { it.isNotBlank() })
            }
            return blocked
        }

        private fun parseRuleTime(value: String): Int? {
            val input = value.trim().uppercase(Locale.US)
            for (pattern in listOf("h:mm a", "hh:mm a", "H:mm")) {
                val format = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val position = ParsePosition(0)
                val parsed = format.parse(input, position) ?: continue
                if (position.index != input.length) continue
                val calendar = Calendar.getInstance().apply { time = parsed }
                return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            }
            return null
        }
    }


    private val prefs: SharedPreferences? = context?.getSharedPreferences("prime_focus_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Temporary timer-session targets must survive normal dashboard refreshes.
    private var timerSessionBlockedPackages: Set<String> = prefs?.getStringSet("timer_session_blocked_packages", emptySet())?.toSet() ?: emptySet()

    // Initial default rules (can be edited/saved by user)
    private val defaultRules = listOf(
        BlockRule(
            id = "rule_work_time",
            title = "Work Time",
            startsInText = "Active 9am–5pm",
            fromTime = "9:00 am",
            toTime = "5:00 pm",
            daysLabel = "Weekdays",
            activeDays = setOf(0, 1, 2, 3, 4),
            blockedAppIds = emptyList(),
            isHardMode = false,
            allowBreaks = true,
            isPaused = false
        ),
        BlockRule(
            id = "rule_weekend_zen",
            title = "Weekend Zen",
            startsInText = "Weekends",
            fromTime = "10:00 am",
            toTime = "8:00 pm",
            daysLabel = "Weekends",
            activeDays = setOf(5, 6),
            blockedAppIds = emptyList(),
            isHardMode = false,
            allowBreaks = true,
            isPaused = false
        )
    )

    private val defaultPresets = listOf(
        TimerPreset(
            id = "preset_deep_study",
            title = "Deep Study",
            durationMinutes = 90,
            formattedDuration = "1h 30m",
            category = "Academic & Mastery",
            description = "Undistracted deep academic and intellectual focus",
            drawableResId = com.example.R.drawable.img_deep_study,
            themeColorHex = 0xFF00E5FF
        ),
        TimerPreset(
            id = "preset_meeting",
            title = "Executive Meeting",
            durationMinutes = 45,
            formattedDuration = "45m",
            category = "Professional",
            description = "Laser-focused agenda without notification interruptions",
            drawableResId = com.example.R.drawable.img_preset_meeting,
            themeColorHex = 0xFFFFB300
        ),
        TimerPreset(
            id = "preset_nature",
            title = "Nature Immersion",
            durationMinutes = 60,
            formattedDuration = "1h",
            category = "Wellness & Offline",
            description = "Ground yourself in serenity, completely disconnect from screen noise",
            drawableResId = com.example.R.drawable.img_preset_nature,
            themeColorHex = 0xFF00E676
        ),
        TimerPreset(
            id = "preset_power_nap",
            title = "Power Nap",
            durationMinutes = 20,
            formattedDuration = "20m",
            category = "Recovery",
            description = "Quick restorative alpha-wave rejuvenation to recharge energy",
            drawableResId = com.example.R.drawable.img_preset_powernap,
            themeColorHex = 0xFFB388FF
        ),
        TimerPreset(
            id = "preset_commute",
            title = "Commute Zen",
            durationMinutes = 30,
            formattedDuration = "30m",
            category = "Mindfulness",
            description = "Transform travel into mindful reflection away from doomscrolling",
            drawableResId = com.example.R.drawable.img_preset_commute,
            themeColorHex = 0xFFFF7043
        ),
        TimerPreset(
            id = "preset_coding",
            title = "Coding Flow",
            durationMinutes = 120,
            formattedDuration = "2h",
            category = "Deep Work",
            description = "Unbroken algorithmic flow state for deep engineering builds",
            drawableResId = com.example.R.drawable.img_timer_hero,
            themeColorHex = 0xFF3EE7C6
        ),
        TimerPreset(
            id = "preset_reading",
            title = "Mindful Reading",
            durationMinutes = 40,
            formattedDuration = "40m",
            category = "Knowledge",
            description = "Deep reading, essays, and reflective comprehension",
            drawableResId = com.example.R.drawable.img_prime_crystal,
            themeColorHex = 0xFFE040FB
        ),
        TimerPreset(
            id = "preset_meditation",
            title = "Breath & Meditation",
            durationMinutes = 15,
            formattedDuration = "15m",
            category = "Centering",
            description = "Calm the central nervous system with rhythmic presence",
            drawableResId = com.example.R.drawable.img_onboarding_bg,
            themeColorHex = 0xFF40C4FF
        )
    )

    private val _appsFlow = MutableStateFlow<List<BlockedAppItem>>(emptyList())
    val appsFlow: StateFlow<List<BlockedAppItem>> = _appsFlow.asStateFlow()

    private val _rulesFlow = MutableStateFlow(loadSavedRules())
    val rulesFlow: StateFlow<List<BlockRule>> = _rulesFlow.asStateFlow()

    private val _presetsFlow = MutableStateFlow(loadSavedPresets())
    val presetsFlow: StateFlow<List<TimerPreset>> = _presetsFlow.asStateFlow()

    private val defaultAlarms = listOf(
        AlarmItem(
            id = "alarm_morning",
            hour = 7,
            minute = 0,
            name = "Morning Wake-Up",
            isEnabled = true,
            recurrence = AlarmRecurrence.WEEKDAYS,
            selectedDays = setOf(1, 2, 3, 4, 5),
            ringtoneId = "zen_crystal",
            ringtoneName = "Zen Crystal Chime",
            vibrationEnabled = true,
            snoozeDurationMinutes = 10,
            alarmMode = AlarmMode.WAKE_UP,
            challengeConfig = ChallengeConfig(
                category = ChallengeCategory.MATH,
                difficulty = ChallengeDifficulty.EASY,
                requiredConsecutive = 3
            )
        ),
        AlarmItem(
            id = "alarm_focus",
            hour = 9,
            minute = 30,
            name = "Daily Focus Standup",
            isEnabled = true,
            recurrence = AlarmRecurrence.WEEKDAYS,
            selectedDays = setOf(1, 2, 3, 4, 5),
            ringtoneId = "morning_breeze",
            ringtoneName = "Morning Breeze",
            vibrationEnabled = true,
            snoozeDurationMinutes = 5,
            alarmMode = AlarmMode.NORMAL
        ),
        AlarmItem(
            id = "alarm_winddown",
            hour = 22,
            minute = 30,
            name = "Evening Wind Down",
            isEnabled = false,
            recurrence = AlarmRecurrence.DAILY,
            selectedDays = setOf(1, 2, 3, 4, 5, 6, 7),
            ringtoneId = "gentle_harp",
            ringtoneName = "Gentle Harp Arpeggio",
            vibrationEnabled = false,
            snoozeDurationMinutes = 10,
            alarmMode = AlarmMode.NORMAL
        )
    )

    private val _clockPreferencesFlow = MutableStateFlow(loadClockPreferences())
    val clockPreferencesFlow: StateFlow<ClockPreferences> = _clockPreferencesFlow.asStateFlow()

    private val _alarmsFlow = MutableStateFlow(loadSavedAlarms())
    val alarmsFlow: StateFlow<List<AlarmItem>> = _alarmsFlow.asStateFlow()

    private val _pomodoroSettingsFlow = MutableStateFlow(loadSavedPomodoroSettings())
    val pomodoroSettingsFlow: StateFlow<PomodoroSettings> = _pomodoroSettingsFlow.asStateFlow()

    private val _pomodoroStateFlow = MutableStateFlow(loadSavedPomodoroState())
    val pomodoroStateFlow: StateFlow<PomodoroState> = _pomodoroStateFlow.asStateFlow()

    private val _stopwatchLapsFlow = MutableStateFlow<List<StopwatchLap>>(loadSavedStopwatchLaps())
    val stopwatchLapsFlow: StateFlow<List<StopwatchLap>> = _stopwatchLapsFlow.asStateFlow()

    private val _profileFlow = MutableStateFlow(
        UserProfile(
            name = prefs?.getString("user_name", "omk") ?: "omk",
            isPro = prefs?.getBoolean("is_pro", false) ?: false,
            emergencyPassesRemaining = (prefs?.getInt("emergency_passes_remaining", 1) ?: 1).coerceIn(0, 1)
        )
    )
    val profileFlow: StateFlow<UserProfile> = _profileFlow.asStateFlow()

    init {
        if (loadAppsOnInit) {
            if (context != null) scope.launch(Dispatchers.IO) { loadInitialAppsAndTrack() }
            else loadInitialAppsAndTrack()
        }
    }

    fun isRealOnboardingCompleted(): Boolean {
        return prefs?.getBoolean("is_onboarding_completed", false) ?: false
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs?.edit()?.putBoolean("is_onboarding_completed", completed)?.apply()
    }

    private fun getSavedBlockedIds(): Set<String> {
        return prefs?.getStringSet("blocked_app_ids", null) ?: emptySet()
    }

    private fun saveBlockedIds(ids: Set<String>) {
        prefs?.edit()?.putStringSet("blocked_app_ids", ids)?.apply()
    }

    /**
     * Load real user-added websites from SharedPreferences.
     */
    private fun loadSavedCustomWebsites(): List<BlockedAppItem> {
        val list = mutableListOf<BlockedAppItem>()
        try {
            val set = prefs?.getStringSet("custom_websites_set", null)
            if (set != null) {
                for (entry in set) {
                    val parts = entry.split(":")
                    if (parts.isNotEmpty()) {
                        val domain = parts[0]
                        val isBlocked = parts.getOrNull(1)?.toBoolean() ?: true
                        list.add(
                            BlockedAppItem(
                                id = "site_" + domain.replace(".", "_"),
                                name = domain,
                                packageName = domain,
                                isWebsite = true,
                                usageToday = "Added manually",
                                category = "Distracting",
                                isBlocked = isBlocked,
                                brandColorHex = 0xFF2D8879
                            )
                        )
                    }
                }
            }
        } catch (_: Throwable) { }
        return list
    }

    private fun saveCustomWebsites(items: List<BlockedAppItem>) {
        try {
            val websites = items.filter { it.isWebsite }
            val set = websites.map { "${it.name}:${it.isBlocked}" }.toSet()
            prefs?.edit()?.putStringSet("custom_websites_set", set)?.apply()
        } catch (_: Throwable) { }
    }

    private fun loadSavedRules(): List<BlockRule> {
        try {
            val jsonStr = prefs?.getString("block_rules_json", null) ?: return defaultRules
            val list = mutableListOf<BlockRule>()
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val activeDaysSet = mutableSetOf<Int>()
                val daysArr = obj.optJSONArray("active_days")
                if (daysArr != null) {
                    for (d in 0 until daysArr.length()) {
                        activeDaysSet.add(daysArr.getInt(d))
                    }
                }
                val blockedAppsList = mutableListOf<String>()
                val appsArr = obj.optJSONArray("blocked_apps")
                if (appsArr != null) {
                    for (a in 0 until appsArr.length()) {
                        blockedAppsList.add(appsArr.getString(a))
                    }
                }

                list.add(
                    BlockRule(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        startsInText = obj.optString("starts_in", "Active"),
                        fromTime = obj.optString("from_time", "9:00 am"),
                        toTime = obj.optString("to_time", "5:00 pm"),
                        daysLabel = obj.optString("days_label", "Weekdays"),
                        activeDays = if (daysArr == null) setOf(0, 1, 2, 3, 4) else activeDaysSet,
                        blockedAppIds = blockedAppsList,
                        isHardMode = obj.optBoolean("is_hard_mode", false),
                        allowBreaks = obj.optBoolean("allow_breaks", true),
                        isPaused = obj.optBoolean("is_paused", false),
                        isCustom = obj.optBoolean("is_custom", false)
                    )
                )
            }
            return list
        } catch (_: Throwable) {
            return defaultRules
        }
    }

    private fun saveRules(rules: List<BlockRule>) {
        try {
            val jsonArray = JSONArray()
            rules.forEach { rule ->
                val obj = JSONObject().apply {
                    put("id", rule.id)
                    put("title", rule.title)
                    put("starts_in", rule.startsInText)
                    put("from_time", rule.fromTime)
                    put("to_time", rule.toTime)
                    put("days_label", rule.daysLabel)
                    put("is_hard_mode", rule.isHardMode)
                    put("allow_breaks", rule.allowBreaks)
                    put("is_paused", rule.isPaused)
                    put("is_custom", rule.isCustom)
                    put("active_days", JSONArray(rule.activeDays.toList()))
                    put("blocked_apps", JSONArray(rule.blockedAppIds))
                }
                jsonArray.put(obj)
            }
            prefs?.edit()?.putString("block_rules_json", jsonArray.toString())?.apply()
        } catch (_: Throwable) { }
    }

    private fun loadSavedPresets(): List<TimerPreset> {
        val jsonStr = prefs?.getString("timer_presets_json", null) ?: return defaultPresets
        val list = mutableListOf<TimerPreset>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val def = defaultPresets.firstOrNull { it.id == id }
                list.add(
                    TimerPreset(
                        id = id,
                        title = obj.getString("title"),
                        durationMinutes = obj.getInt("duration_minutes"),
                        formattedDuration = obj.getString("formatted_duration"),
                        description = obj.optString("description", def?.description ?: ""),
                        category = obj.optString("category", def?.category ?: "Focus"),
                        drawableResId = if (obj.has("drawable_res")) obj.getInt("drawable_res") else (def?.drawableResId ?: com.example.R.drawable.img_deep_study),
                        themeColorHex = if (obj.has("theme_color")) obj.getLong("theme_color") else (def?.themeColorHex ?: 0xFF76F2BF)
                    )
                )
            }
        } catch (_: Exception) {
            return defaultPresets
        }
        val existingIds = list.map { it.id }.toSet()
        val missingDefaults = defaultPresets.filterNot { existingIds.contains(it.id) }
        return (list + missingDefaults).ifEmpty { defaultPresets }
    }

    fun loadInitialAppsAndTrack() {
        if (context != null) {
            val savedBlockedIds = getSavedBlockedIds()
            val customWebsites = loadSavedCustomWebsites().map { site ->
                if (savedBlockedIds.isNotEmpty()) {
                    site.copy(isBlocked = savedBlockedIds.contains(site.id))
                } else site
            }

            val trackingResult = RealScreenTimeTracker.queryRealScreenTime(
                context = context,
                existingWebsites = customWebsites,
                existingBlockedIds = savedBlockedIds,
                verifiedFocusMinutes = getTodayFocusMinutes(),
                verifiedBlockedMinutes = getTodayBlockingMinutes()
            )

            val streakInfo = StreakTracker.getStreakInfo(context)

            val categoryOverrides = getSavedCategoryOverrides()
            val appsWithOverrides = trackingResult.apps.map { app ->
                val overridden = categoryOverrides[app.id]
                if (overridden != null) app.copy(category = overridden) else app
            }

            _appsFlow.value = appsWithOverrides
            _profileFlow.update {
                it.copy(
                    screenTimeToday = trackingResult.totalScreenTimeFormatted,
                    screenTimeMillis = trackingResult.totalScreenTimeMillis,
                    score = trackingResult.score,
                    sleepScore = trackingResult.sleepScore,
                    focusScore = trackingResult.focusScore,
                    restScore = trackingResult.restScore,
                    streakDays = streakInfo.currentStreak,
                    bestStreakDays = streakInfo.bestStreak,
                    isActiveToday = streakInfo.isActiveToday,
                    weekDaysActive = streakInfo.weekDaysActive,
                    hasUsagePermission = trackingResult.hasUsagePermission
                )
            }
        }
        syncAccessibilityBlockList()
    }

    fun refreshRealScreenTime(): RealScreenTimeTracker.TrackingResult? {
        if (context == null) return null
        val currentWebsites = _appsFlow.value.filter { it.isWebsite }
        val currentBlockedIds = _appsFlow.value.filter { it.isBlocked }.map { it.id }.toSet()

        val trackingResult = RealScreenTimeTracker.queryRealScreenTime(
            context = context,
            existingWebsites = currentWebsites,
            existingBlockedIds = currentBlockedIds,
            verifiedFocusMinutes = getTodayFocusMinutes(),
            verifiedBlockedMinutes = getTodayBlockingMinutes()
        )

        val streakInfo = StreakTracker.getStreakInfo(context)

        val categoryOverrides = getSavedCategoryOverrides()
        val appsWithOverrides = trackingResult.apps.map { app ->
            val overridden = categoryOverrides[app.id]
            if (overridden != null) app.copy(category = overridden) else app
        }
        _appsFlow.value = appsWithOverrides
        _profileFlow.update {
            it.copy(
                screenTimeToday = trackingResult.totalScreenTimeFormatted,
                screenTimeMillis = trackingResult.totalScreenTimeMillis,
                score = trackingResult.score,
                sleepScore = trackingResult.sleepScore,
                focusScore = trackingResult.focusScore,
                restScore = trackingResult.restScore,
                streakDays = streakInfo.currentStreak,
                bestStreakDays = streakInfo.bestStreak,
                isActiveToday = streakInfo.isActiveToday,
                weekDaysActive = streakInfo.weekDaysActive,
                hasUsagePermission = trackingResult.hasUsagePermission
            )
        }
        syncAccessibilityBlockList()
        return trackingResult
    }

    fun logActivityAndBoostStreak() {
        if (context == null) return
        StreakTracker.logActivityToday(context)
        val streakInfo = StreakTracker.getStreakInfo(context)
        _profileFlow.update {
            it.copy(
                streakDays = streakInfo.currentStreak,
                bestStreakDays = streakInfo.bestStreak,
                isActiveToday = streakInfo.isActiveToday,
                weekDaysActive = streakInfo.weekDaysActive
            )
        }
    }

    fun updateUserName(name: String) {
        prefs?.edit()?.putString("user_name", name)?.apply()
        _profileFlow.update { it.copy(name = name) }
    }

    fun togglePro(enabled: Boolean) {
        prefs?.edit()?.putBoolean("is_pro", enabled)?.apply()
        _profileFlow.update { it.copy(isPro = enabled) }
    }

    @Synchronized
    fun useEmergencyPass(): Boolean {
        val remaining = _profileFlow.value.emergencyPassesRemaining
        if (remaining <= 0) return false
        val updated = (remaining - 1).coerceAtLeast(0)
        prefs?.edit()?.putInt("emergency_passes_remaining", updated)?.apply()
        _profileFlow.update { it.copy(emergencyPassesRemaining = updated) }
        return true
    }

    fun addCustomRule(newRule: BlockRule) {
        _rulesFlow.update { list ->
            val updated = list + newRule
            saveRules(updated)
            updated
        }
        syncAccessibilityBlockList()
    }

    fun deleteRule(ruleId: String) {
        _rulesFlow.update { list ->
            val updated = list.filter { it.id != ruleId }
            saveRules(updated)
            updated
        }
        syncAccessibilityBlockList()
    }

    fun setAppCategory(appId: String, newCategory: String) {
        _appsFlow.update { list ->
            val updated = list.map { if (it.id == appId) it.copy(category = newCategory) else it }
            updated
        }
        saveCategoryOverride(appId, newCategory)
    }

    private fun getSavedCategoryOverrides(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val json = prefs?.getString("category_overrides_json", null)
            if (json != null) {
                val obj = JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = obj.getString(k)
                }
            }
        } catch (_: Throwable) { }
        return map
    }

    private fun saveCategoryOverride(appId: String, category: String) {
        try {
            val current = getSavedCategoryOverrides().toMutableMap()
            current[appId] = category
            val obj = JSONObject()
            current.forEach { (k, v) -> obj.put(k, v) }
            prefs?.edit()?.putString("category_overrides_json", obj.toString())?.apply()
        } catch (_: Throwable) { }
    }

    fun toggleRulePause(ruleId: String) {
        _rulesFlow.update { list ->
            val updated = list.map { rule ->
                if (rule.id == ruleId) rule.copy(isPaused = !rule.isPaused) else rule
            }
            saveRules(updated)
            updated
        }
        syncAccessibilityBlockList()
    }

    fun updateRule(updatedRule: BlockRule) {
        _rulesFlow.update { list ->
            val updated = list.map { if (it.id == updatedRule.id) updatedRule else it }
            saveRules(updated)
            updated
        }
        syncAccessibilityBlockList()
    }

    fun toggleAppBlocked(appId: String) {
        _appsFlow.update { list ->
            val updated = list.map { item ->
                if (item.id == appId) item.copy(isBlocked = !item.isBlocked) else item
            }
            saveCustomWebsites(updated)
            updated
        }
        persistCurrentBlockedIds()
        syncAccessibilityBlockList()
    }

    fun removeBlockedApp(appId: String) {
        _appsFlow.update { list ->
            val updated = list.filter { it.id != appId }
            saveCustomWebsites(updated)
            updated
        }
        persistCurrentBlockedIds()
        syncAccessibilityBlockList()
    }

    /**
     * Real website addition:
     * - Cleans domain
     * - Validates format
     * - Proactively downloads and caches favicon to internal storage
     * - Persists into SharedPreferences
     */
    fun addCustomWebsite(domain: String) {
        val cleanDomain = FaviconManager.cleanDomain(domain)
        if (!FaviconManager.isValidDomain(cleanDomain)) return

        val newId = "site_" + cleanDomain.replace(".", "_")

        // Trigger asynchronous background download of favicon to local storage and sync with Website Blocker
        if (context != null) {
            scope.launch {
                FaviconManager.saveFaviconToLocalStorage(context, cleanDomain)
                try {
                    val bm = com.example.vpn.service.BlockingManager.getInstance(context)
                    bm.blocklistRepo.addCustomDomain(cleanDomain)
                    bm.reconcileActiveRules()
                } catch (_: Exception) {}
            }
        }

        _appsFlow.update { current ->
            if (current.any { it.name.equals(cleanDomain, ignoreCase = true) }) {
                current
            } else {
                val updated = current + BlockedAppItem(
                    id = newId,
                    name = cleanDomain,
                    packageName = cleanDomain,
                    isWebsite = true,
                    usageToday = "Added manually",
                    category = "Distracting",
                    isBlocked = true,
                    brandColorHex = 0xFF2D8879
                )
                saveCustomWebsites(updated)
                updated
            }
        }
        persistCurrentBlockedIds()
        syncAccessibilityBlockList()
    }

    private fun persistCurrentBlockedIds() {
        val blockedIds = _appsFlow.value.filter { it.isBlocked }.map { it.id }.toSet()
        saveBlockedIds(blockedIds)
    }

    fun syncAccessibilityBlockList() {
        val blockedPackageNames = _appsFlow.value
            .filter { it.isBlocked && !it.isWebsite && it.packageName.isNotBlank() }
            .map { it.packageName }
            .toSet()

        val blockedWebsiteDomains = _appsFlow.value
            .filter { it.isBlocked && it.isWebsite }
            .map { it.name }
            .toSet()

        val effectiveBlockedPackages = blockedPackageNames + timerSessionBlockedPackages
        OpalAccessibilityService.blockedPackages = effectiveBlockedPackages
        OpalAccessibilityService.updateCustomBlockedWebsites(blockedWebsiteDomains)
        context?.let { ctx ->
            try {
                syncTimeBasedRuleBlocking(ctx)
                com.example.vpn.service.BlockingManager.getInstance(ctx).onBlockingTargetsChanged()
            } catch (_: Exception) { }
        }
    }

    fun applyTimerSessionAppBlock(sessionPackageNames: Set<String>) {
        timerSessionBlockedPackages = sessionPackageNames.filter { it.isNotBlank() }.toSet()
        prefs?.edit()?.putStringSet("timer_session_blocked_packages", timerSessionBlockedPackages)?.apply()
        syncAccessibilityBlockList()
    }

    fun clearTimerSessionAppBlock() {
        timerSessionBlockedPackages = emptySet()
        prefs?.edit()?.remove("timer_session_blocked_packages")?.apply()
        syncAccessibilityBlockList()
    }

    // ==================== CLOCK PREFERENCES ====================

    fun saveClockPreferences(value: ClockPreferences) {
        val safe = value.copy(
            alarmVolume = value.alarmVolume.coerceIn(0, 100),
            alarmWallpaper = value.alarmWallpaper.takeIf { it in setOf("aurora", "sunrise", "forest", "ocean", "minimal") } ?: "aurora",
            timerTheme = value.timerTheme.takeIf { it in setOf("midnight", "ember", "ocean", "forest", "lavender") } ?: "midnight"
        )
        _clockPreferencesFlow.value = safe
        prefs?.edit()
            ?.putString("clock_default_ringtone_id", safe.defaultRingtoneId)
            ?.putString("clock_default_ringtone_name", safe.defaultRingtoneName)
            ?.putString("clock_default_ringtone_uri", safe.defaultRingtoneUriString)
            ?.putString("clock_alarm_wallpaper", safe.alarmWallpaper)
            ?.putString("clock_timer_theme", safe.timerTheme)
            ?.putBoolean("clock_alarm_fade_in", safe.alarmFadeIn)
            ?.putBoolean("clock_alarm_vibration", safe.alarmVibration)
            ?.putInt("clock_alarm_volume", safe.alarmVolume)
            ?.putBoolean("clock_session_sounds", safe.sessionSoundsEnabled)
            ?.apply()
    }

    private fun loadClockPreferences(): ClockPreferences = ClockPreferences(
        defaultRingtoneId = prefs?.getString("clock_default_ringtone_id", "zen_crystal") ?: "zen_crystal",
        defaultRingtoneName = prefs?.getString("clock_default_ringtone_name", "Zen Crystal Chime") ?: "Zen Crystal Chime",
        defaultRingtoneUriString = prefs?.getString("clock_default_ringtone_uri", null),
        alarmWallpaper = prefs?.getString("clock_alarm_wallpaper", "aurora") ?: "aurora",
        timerTheme = prefs?.getString("clock_timer_theme", "midnight") ?: "midnight",
        alarmFadeIn = prefs?.getBoolean("clock_alarm_fade_in", true) ?: true,
        alarmVibration = prefs?.getBoolean("clock_alarm_vibration", true) ?: true,
        alarmVolume = (prefs?.getInt("clock_alarm_volume", 85) ?: 85).coerceIn(0, 100),
        sessionSoundsEnabled = prefs?.getBoolean("clock_session_sounds", true) ?: true
    )

    // Standard timer uses an absolute deadline so it survives activity/process recreation.
    fun saveStandardTimerState(remainingSeconds: Int, selectedMinutes: Int, running: Boolean, paused: Boolean) {
        val remaining = remainingSeconds.coerceIn(0, 24 * 60 * 60)
        val selected = selectedMinutes.coerceIn(1, 24 * 60)
        val active = running && !paused
        prefs?.edit()
            ?.putInt("clock_timer_remaining", remaining)
            ?.putInt("clock_timer_selected", selected)
            ?.putBoolean("clock_timer_running", running)
            ?.putBoolean("clock_timer_paused", paused)
            ?.putLong("clock_timer_deadline", if (active) System.currentTimeMillis() + remaining * 1000L else 0L)
            ?.apply()
    }

    fun getSavedStandardTimerPaused(): Boolean = prefs?.getBoolean("clock_timer_paused", false) ?: false

    fun getSavedStandardTimerState(): Triple<Int, Int, Boolean> {
        val selected = (prefs?.getInt("clock_timer_selected", 30) ?: 30).coerceIn(1, 24 * 60)
        val running = prefs?.getBoolean("clock_timer_running", false) ?: false
        val paused = prefs?.getBoolean("clock_timer_paused", false) ?: false
        val stored = (prefs?.getInt("clock_timer_remaining", selected * 60) ?: selected * 60).coerceAtLeast(0)
        val deadline = prefs?.getLong("clock_timer_deadline", 0L) ?: 0L
        val remaining = if (running && !paused && deadline > 0L) {
            ((deadline - System.currentTimeMillis() + 999L) / 1000L).toInt().coerceAtLeast(0)
        } else stored
        return Triple(remaining, selected, running)
    }

    fun clearStandardTimerState(selectedMinutes: Int = 30) {
        val selected = selectedMinutes.coerceIn(1, 24 * 60)
        saveStandardTimerState(selected * 60, selected, false, false)
    }

    // Stopwatch is based on wall-clock elapsed time, not a coroutine tick counter.
    fun saveStopwatchState(elapsedMillis: Long, running: Boolean) {
        prefs?.edit()
            ?.putLong("clock_stopwatch_elapsed", elapsedMillis.coerceAtLeast(0L))
            ?.putLong("clock_stopwatch_started_at", if (running) System.currentTimeMillis() else 0L)
            ?.putBoolean("clock_stopwatch_running", running)
            ?.apply()
    }

    fun getSavedStopwatchState(): Pair<Long, Boolean> {
        val elapsed = (prefs?.getLong("clock_stopwatch_elapsed", 0L) ?: 0L).coerceAtLeast(0L)
        val startedAt = prefs?.getLong("clock_stopwatch_started_at", 0L) ?: 0L
        val running = prefs?.getBoolean("clock_stopwatch_running", false) ?: false
        return (if (running && startedAt > 0L) elapsed + (System.currentTimeMillis() - startedAt).coerceAtLeast(0L) else elapsed) to running
    }

    // ==================== ALARM PERSISTENCE ====================

    fun saveAlarm(alarm: AlarmItem) {
        _alarmsFlow.update { list ->
            val index = list.indexOfFirst { it.id == alarm.id }
            val updated = if (index >= 0) {
                list.toMutableList().apply { set(index, alarm) }
            } else {
                list + alarm
            }
            saveAlarms(updated)
            if (context != null) {
                AlarmSchedulerHelper.scheduleAlarm(context, alarm)
            }
            updated
        }
    }

    fun deleteAlarm(alarmId: String) {
        if (context != null) {
            AlarmSchedulerHelper.cancelAlarm(context, alarmId)
        }
        _alarmsFlow.update { list ->
            val updated = list.filter { it.id != alarmId }
            saveAlarms(updated)
            updated
        }
    }

    fun toggleAlarm(alarmId: String, isEnabled: Boolean) {
        _alarmsFlow.update { list ->
            val updated = list.map {
                if (it.id == alarmId) {
                    val toggled = it.copy(isEnabled = isEnabled)
                    if (context != null) {
                        AlarmSchedulerHelper.scheduleAlarm(context, toggled)
                    }
                    toggled
                } else it
            }
            saveAlarms(updated)
            updated
        }
    }

    fun onAlarmTriggered(alarmId: String) {
        _alarmsFlow.update { list ->
            val updated = list.map { alarm ->
                if (alarm.id == alarmId) {
                    if (alarm.recurrence == AlarmRecurrence.ONCE) {
                        alarm.copy(isEnabled = false)
                    } else {
                        if (context != null) {
                            AlarmSchedulerHelper.scheduleAlarm(context, alarm)
                        }
                        alarm
                    }
                } else alarm
            }
            saveAlarms(updated)
            updated
        }
    }

    private fun loadSavedAlarms(): List<AlarmItem> {
        val jsonStr = prefs?.getString("saved_alarms_json", null) ?: return defaultAlarms
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<AlarmItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val daysSet = mutableSetOf<Int>()
                val daysArr = obj.optJSONArray("selected_days")
                if (daysArr != null) {
                    for (d in 0 until daysArr.length()) {
                        daysSet.add(daysArr.getInt(d))
                    }
                }

                val chalObj = obj.optJSONObject("challenge_config")
                val challengeConfig = if (chalObj != null) {
                    ChallengeConfig(
                        category = ChallengeCategory.valueOf(chalObj.optString("category", "MATH")),
                        difficulty = ChallengeDifficulty.valueOf(chalObj.optString("difficulty", "MEDIUM")),
                        requiredConsecutive = chalObj.optInt("required_consecutive", 3),
                        isRandomizedCategory = chalObj.optBoolean("is_randomized", false),
                        combineMultipleChallenges = chalObj.optBoolean("combine_multiple", false)
                    )
                } else ChallengeConfig()

                list.add(
                    AlarmItem(
                        id = obj.getString("id"),
                        hour = obj.getInt("hour"),
                        minute = obj.getInt("minute"),
                        name = obj.optString("name", "Alarm"),
                        isEnabled = obj.optBoolean("is_enabled", true),
                        recurrence = AlarmRecurrence.valueOf(obj.optString("recurrence", "DAILY")),
                        selectedDays = if (daysArr == null) setOf(1, 2, 3, 4, 5) else daysSet,
                        specificDateMillis = if (obj.has("specific_date")) obj.getLong("specific_date") else null,
                        ringtoneId = obj.optString("ringtone_id", "zen_crystal"),
                        ringtoneName = obj.optString("ringtone_name", "Zen Crystal Chime"),
                        ringtoneUriString = if (obj.has("ringtone_uri")) obj.getString("ringtone_uri") else null,
                        vibrationEnabled = obj.optBoolean("vibration_enabled", true),
                        snoozeDurationMinutes = obj.optInt("snooze_duration", 10),
                        alarmMode = AlarmMode.valueOf(obj.optString("alarm_mode", "NORMAL")),
                        challengeConfig = challengeConfig
                    )
                )
            }
            list
        } catch (_: Throwable) {
            defaultAlarms
        }
    }

    private fun saveAlarms(alarms: List<AlarmItem>) {
        try {
            val jsonArray = JSONArray()
            alarms.forEach { alarm ->
                val obj = JSONObject().apply {
                    put("id", alarm.id)
                    put("hour", alarm.hour)
                    put("minute", alarm.minute)
                    put("name", alarm.name)
                    put("is_enabled", alarm.isEnabled)
                    put("recurrence", alarm.recurrence.name)
                    put("selected_days", JSONArray(alarm.selectedDays.toList()))
                    alarm.specificDateMillis?.let { put("specific_date", it) }
                    put("ringtone_id", alarm.ringtoneId)
                    put("ringtone_name", alarm.ringtoneName)
                    alarm.ringtoneUriString?.let { put("ringtone_uri", it) }
                    put("vibration_enabled", alarm.vibrationEnabled)
                    put("snooze_duration", alarm.snoozeDurationMinutes)
                    put("alarm_mode", alarm.alarmMode.name)

                    val chal = JSONObject().apply {
                        put("category", alarm.challengeConfig.category.name)
                        put("difficulty", alarm.challengeConfig.difficulty.name)
                        put("required_consecutive", alarm.challengeConfig.requiredConsecutive)
                        put("is_randomized", alarm.challengeConfig.isRandomizedCategory)
                        put("combine_multiple", alarm.challengeConfig.combineMultipleChallenges)
                    }
                    put("challenge_config", chal)
                }
                jsonArray.put(obj)
            }
            prefs?.edit()?.putString("saved_alarms_json", jsonArray.toString())?.apply()
        } catch (_: Throwable) { }
    }

    // ==================== POMODORO PERSISTENCE ====================

    fun updatePomodoroSettings(settings: PomodoroSettings) {
        _pomodoroSettingsFlow.value = settings
        try {
            val obj = JSONObject().apply {
                put("focus_minutes", settings.focusMinutes)
                put("short_break_minutes", settings.shortBreakMinutes)
                put("long_break_minutes", settings.longBreakMinutes)
                put("rounds_per_cycle", settings.roundsPerCycle)
                put("total_cycles", settings.totalCycles)
            }
            prefs?.edit()?.putString("pomodoro_settings_json", obj.toString())?.apply()
        } catch (_: Throwable) { }
    }

    private fun loadSavedPomodoroSettings(): PomodoroSettings {
        val jsonStr = prefs?.getString("pomodoro_settings_json", null) ?: return PomodoroSettings()
        return try {
            val obj = JSONObject(jsonStr)
            PomodoroSettings(
                focusMinutes = obj.optInt("focus_minutes", 25).coerceIn(1, 180),
                shortBreakMinutes = obj.optInt("short_break_minutes", 5).coerceIn(1, 60),
                longBreakMinutes = obj.optInt("long_break_minutes", 15).coerceIn(1, 120),
                roundsPerCycle = obj.optInt("rounds_per_cycle", 4).coerceIn(1, 12),
                totalCycles = obj.optInt("total_cycles", 1).coerceIn(1, 20)
            )
        } catch (_: Throwable) {
            PomodoroSettings()
        }
    }

    fun savePomodoroState(state: PomodoroState) {
        _pomodoroStateFlow.value = state
        try {
            val obj = JSONObject().apply {
                put("session_type", state.sessionType.name)
                put("current_round", state.currentRound)
                put("current_cycle", state.currentCycle)
                put("remaining_seconds", state.remainingSeconds)
                put("total_session_seconds", state.totalSessionSeconds)
                put("completed_rounds", state.completedRoundsCount)
                put("is_running", state.isRunning)
                put("is_paused", state.isPaused)
                put("deadline_ms", if (state.isRunning && !state.isPaused) System.currentTimeMillis() + state.remainingSeconds.coerceAtLeast(0) * 1000L else 0L)
            }
            prefs?.edit()?.putString("pomodoro_state_json", obj.toString())?.apply()
        } catch (_: Throwable) { }
    }

    private fun loadSavedPomodoroState(): PomodoroState {
        val jsonStr = prefs?.getString("pomodoro_state_json", null) ?: return PomodoroState()
        return try {
            val obj = JSONObject(jsonStr)
            PomodoroState(
                sessionType = PomodoroSessionType.valueOf(obj.optString("session_type", "FOCUS")),
                currentRound = obj.optInt("current_round", 1),
                currentCycle = obj.optInt("current_cycle", 1),
                totalSessionSeconds = obj.optInt("total_session_seconds", 25 * 60),
                completedRoundsCount = obj.optInt("completed_rounds", 0),
                isRunning = obj.optBoolean("is_running", false),
                isPaused = obj.optBoolean("is_paused", false),
                remainingSeconds = run {
                    val stored = obj.optInt("remaining_seconds", 25 * 60).coerceAtLeast(0)
                    val deadline = obj.optLong("deadline_ms", 0L)
                    if (obj.optBoolean("is_running", false) && !obj.optBoolean("is_paused", false) && deadline > 0L) {
                        ((deadline - System.currentTimeMillis() + 999L) / 1000L).toInt().coerceAtLeast(0)
                    } else stored
                }
            )
        } catch (_: Throwable) {
            PomodoroState()
        }
    }

    // ==================== STOPWATCH PERSISTENCE ====================

    fun saveStopwatchLaps(laps: List<StopwatchLap>) {
        _stopwatchLapsFlow.value = laps
        try {
            val arr = JSONArray()
            laps.forEach { lap ->
                val obj = JSONObject().apply {
                    put("lap_num", lap.lapNumber)
                    put("lap_time", lap.lapTimeMillis)
                    put("total_time", lap.totalTimeMillis)
                }
                arr.put(obj)
            }
            prefs?.edit()?.putString("stopwatch_laps_json", arr.toString())?.apply()
        } catch (_: Throwable) { }
    }

    private fun loadSavedStopwatchLaps(): List<StopwatchLap> {
        val jsonStr = prefs?.getString("stopwatch_laps_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<StopwatchLap>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StopwatchLap(
                        lapNumber = obj.getInt("lap_num"),
                        lapTimeMillis = obj.getLong("lap_time"),
                        totalTimeMillis = obj.getLong("total_time")
                    )
                )
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun getTodayDateKey(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Calendar.getInstance().time)
    }

    fun getTodayFocusMinutes(): Int {
        // Only completed focus sessions are counted. Screen-time headroom is not focus time.
        return (prefs?.getInt("verified_focus_min_${getTodayDateKey()}", 0) ?: 0).coerceAtLeast(0)
    }

    fun getTodayBlockingMinutes(): Int {
        // Flush the live interval so dashboard reads do not lag behind the 15-second checkpoint.
        context?.let { ctx ->
            try {
                com.example.vpn.service.BlockingManager.getInstance(ctx).flushBlockingDuration()
            } catch (_: Exception) { }
        }
        // Milliseconds are accumulated only while a real enforcement service is active
        // with at least one configured app or website target.
        val measuredMillis = prefs?.getLong("verified_block_ms_${getTodayDateKey()}", 0L) ?: 0L
        return (measuredMillis.coerceAtLeast(0L) / 60_000L).toInt()
    }

    fun addFocusMinutes(minutes: Int, isPomodoro: Boolean = false) {
        if (minutes <= 0) return
        val dateKey = getTodayDateKey()
        val currentMinutes = prefs?.getInt("verified_focus_min_$dateKey", 0) ?: 0
        val currentSessions = prefs?.getInt("verified_focus_sessions_$dateKey", 0) ?: 0
        val editor = prefs?.edit()
            ?.putInt("verified_focus_min_$dateKey", currentMinutes + minutes)
            ?.putInt("verified_focus_sessions_$dateKey", currentSessions + 1)

        if (isPomodoro) {
            val currentPomodoros = prefs?.getInt("verified_pomodoro_sessions_$dateKey", 0) ?: 0
            val currentPomodoroMinutes = prefs?.getInt("verified_pomodoro_min_$dateKey", 0) ?: 0
            editor?.putInt("verified_pomodoro_sessions_$dateKey", currentPomodoros + 1)
                ?.putInt("verified_pomodoro_min_$dateKey", currentPomodoroMinutes + minutes)
        }
        editor?.apply()
    }

    fun getTodayFocusSessions(): Int =
        (prefs?.getInt("verified_focus_sessions_${getTodayDateKey()}", 0) ?: 0).coerceAtLeast(0)

    fun getTodayPomodoroSessions(): Int =
        (prefs?.getInt("verified_pomodoro_sessions_${getTodayDateKey()}", 0) ?: 0).coerceAtLeast(0)

    fun getWeekFocusSessions(): Int {
        val calendar = java.util.Calendar.getInstance().apply {
            firstDayOfWeek = java.util.Calendar.SUNDAY
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val today = java.util.Calendar.getInstance()
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        var total = 0
        while (calendar.timeInMillis <= today.timeInMillis) {
            val dateKey = formatter.format(calendar.time)
            total += prefs?.getInt("verified_focus_sessions_$dateKey", 0) ?: 0
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return total.coerceAtLeast(0)
    }

    // Blocking time is recorded by BlockingManager from actual enforcement uptime.
    // Do not manually add estimated minutes here.
}
