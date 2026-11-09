package com.example.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import com.example.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

// Android exposes this UsageEvents event as a @SystemApi, so keep its stable
// platform event value here instead of referencing a non-SDK constant.
private const val USAGE_EVENT_END_OF_DAY = 3
private const val USAGE_EVENT_CONTINUE_PREVIOUS_DAY = 4
private const val USAGE_EVENT_NOTIFICATION_INTERRUPTION = 12

data class AppUsageData(
    val packageName: String,
    val appName: String,
    val totalTimeMs: Long,
    val category: String,
    val openCount: Int? = null,
    val notificationCount: Int? = null
)

data class CategoryUsage(
    val categoryName: String,
    val totalTimeMs: Long,
    val colorHex: Long = 0xFF26E5B0
)

data class HourlyPeakUsage(
    val hourOfDay: Int, // 0 to 23
    val totalTimeMs: Long
)

data class DayUsagePoint(
    val dayLabel: String, // "S", "M", "T", "W", "T", "F", "S"
    val dayIndex: Int,    // 0..6 (Sun..Sat)
    val screenTimeMs: Long,
    val notifications: Int,
    val unlocks: Int,
    val isToday: Boolean = false
)

data class AppSpecificMetric(
    val packageName: String,
    val appName: String,
    val weeklyTotalTimeMs: Long,
    val dailyAvgTimeMs: Long,
    val weeklyNotifications: Int,
    val dailyAvgNotifications: Int,
    val weeklyAppOpens: Int,
    val dailyAvgAppOpens: Int,
    val dailyScreenTime: List<Long>, // 7 values for Sun..Sat
    val dailyNotifications: List<Int>,
    val dailyAppOpens: List<Int>
)

sealed class WellbeingFetchState {
    object Idle : WellbeingFetchState()
    object Loading : WellbeingFetchState()
    data class Success(
        val weeklyApps: List<AppUsageData>,
        val categories: List<CategoryUsage>,
        val hourlyData: List<HourlyPeakUsage>,
        val trendPoints: List<DayUsagePoint>
    ) : WellbeingFetchState()
    data class Error(val message: String) : WellbeingFetchState()
    object PermissionRequired : WellbeingFetchState()
}

class WellbeingRepository(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val packageManager = context.packageManager

    fun hasUsageStatsPermission(): Boolean {
        return PermissionHelper.hasUsageAccessPermission(context)
    }

    fun requestUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    // 1. Fetch real today's app usage strictly for today (midnight to now)
    suspend fun getTodayAppUsage(): Result<List<AppUsageData>> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            return@withContext Result.failure(SecurityException("Usage access permission is required."))
        }
        val sm = usageStatsManager ?: return@withContext Result.failure(IllegalStateException("UsageStatsManager not available."))

        val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val homePackages: Set<String> = try {
            packageManager.queryIntentActivities(homeIntent, 0).mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
        val launchablePackages = getLaunchablePackages()

        val appMap = mutableMapOf<String, Long>()
        val openCounts = mutableMapOf<String, Int>()
        val packagesWithEvents = mutableSetOf<String>()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        // Primary: Authoritative daily aggregation from Android system
        try {
            val aggregated = sm.queryAndAggregateUsageStats(startTime, endTime)
                ?: return@withContext Result.failure(IllegalStateException("Android returned no daily usage aggregate."))
            aggregated.forEach { (pkg, stat) ->
                if (launchablePackages.contains(pkg) && !RealScreenTimeTracker.isIgnoredPackage(context, pkg, homePackages) && stat.totalTimeInForeground > 0 && stat.lastTimeUsed >= startTime) {
                    appMap[pkg] = stat.totalTimeInForeground
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        // UsageEvents provide real event counters only; foreground duration is read from
        // Android's aggregate UsageStats API above, never reconstructed from partial sessions.
        val notificationCounts = mutableMapOf<String, Int>()
        try {
            val events = sm.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (RealScreenTimeTracker.isIgnoredPackage(context, pkg, homePackages)) continue
                packagesWithEvents.add(pkg)
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED ->
                        openCounts[pkg] = (openCounts[pkg] ?: 0) + 1
                    USAGE_EVENT_NOTIFICATION_INTERRUPTION ->
                        notificationCounts[pkg] = (notificationCounts[pkg] ?: 0) + 1
                }
            }
        } catch (_: Exception) {
            // Android foreground durations remain valid; event counters stay null when unavailable.
        }
        val result = appMap.map { (pkg, duration) ->
            val appLabel = getAppName(pkg)
            val cat = getAppCategory(pkg)
            val hasEvents = packagesWithEvents.contains(pkg)
            val opens = if (hasEvents) openCounts[pkg] ?: 0 else null
            val notifications = if (hasEvents) notificationCounts[pkg] ?: 0 else null
            AppUsageData(pkg, appLabel, duration, cat, opens, notifications)
        }.sortedByDescending { it.totalTimeMs }

        Result.success(result)
    }

    // 2. Fetch real weekly app usage from system, filtering out launchers and system daemons
    suspend fun getWeeklyAppUsage(): Result<List<AppUsageData>> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            return@withContext Result.failure(SecurityException("Usage access permission is required."))
        }
        val sm = usageStatsManager ?: return@withContext Result.failure(IllegalStateException("UsageStatsManager not available."))

        val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val homePackages: Set<String> = try {
            packageManager.queryIntentActivities(homeIntent, 0).mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }

        val launchablePackages = getLaunchablePackages()

        val appMap = mutableMapOf<String, Long>()
        val openCounts = mutableMapOf<String, Int>()
        val packagesWithEvents = mutableSetOf<String>()

        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        // Android's aggregate API returns a total per package for the requested range.
        try {
            val aggregated = sm.queryAndAggregateUsageStats(startTime, endTime)
                ?: return@withContext Result.failure(IllegalStateException("Android returned no weekly usage aggregate."))
            aggregated.forEach { (pkg, stat) ->
                if (launchablePackages.contains(pkg) && stat.totalTimeInForeground > 0 &&
                    !RealScreenTimeTracker.isIgnoredPackage(context, pkg, homePackages)
                ) {
                    appMap[pkg] = stat.totalTimeInForeground
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        // Count real opens and notification interruption events; do not infer either from time.
        val notificationCounts = mutableMapOf<String, Int>()
        try {
            val events = sm.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (RealScreenTimeTracker.isIgnoredPackage(context, pkg, homePackages)) continue
                packagesWithEvents.add(pkg)
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED ->
                        openCounts[pkg] = (openCounts[pkg] ?: 0) + 1
                    USAGE_EVENT_NOTIFICATION_INTERRUPTION ->
                        notificationCounts[pkg] = (notificationCounts[pkg] ?: 0) + 1
                }
            }
        } catch (_: Exception) {
            // Android foreground durations remain valid; event counters stay null when unavailable.
        }
        val result = appMap.map { (pkg, duration) ->
            val appLabel = getAppName(pkg)
            val cat = getAppCategory(pkg)
            val hasEvents = packagesWithEvents.contains(pkg)
            val opens = if (hasEvents) openCounts[pkg] ?: 0 else null
            val notifications = if (hasEvents) notificationCounts[pkg] ?: 0 else null
            AppUsageData(pkg, appLabel, duration, cat, opens, notifications)
        }.sortedByDescending { it.totalTimeMs }

        Result.success(result)
    }

    // 3. Real Category Breakdown strictly from tracked apps
    suspend fun getCategoryWiseUsage(appUsageList: List<AppUsageData>): List<CategoryUsage> = withContext(Dispatchers.IO) {
        val categoryColors = mapOf(
            "Social" to 0xFF26E5B0,                    // Teal / Emerald
            "Social Media" to 0xFF26E5B0,
            "Video" to 0xFF3B82F6,                     // Blue
            "Productivity and finance" to 0xFF8B5CF6,  // Purple
            "Productivity" to 0xFF8B5CF6,
            "Entertainment" to 0xFFB388FF,
            "Games" to 0xFFF59E0B,                     // Amber
            "System" to 0xFF64748B,
            "Other" to 0xFF90A4AE,
            "Others" to 0xFF90A4AE
        )

        appUsageList.groupBy { it.category }
            .map { (category, list) ->
                val normCategory = when (category) {
                    "Social Media" -> "Social"
                    "Productivity" -> "Productivity and finance"
                    "Others" -> "Other"
                    else -> category
                }
                CategoryUsage(
                    categoryName = normCategory,
                    totalTimeMs = list.sumOf { it.totalTimeMs },
                    colorHex = categoryColors[normCategory] ?: 0xFF26E5B0
                )
            }.sortedByDescending { it.totalTimeMs }
    }

    // Build hourly totals from a single foreground-app timeline. This avoids
    // independently pairing each package's events, which can double-count time.
    suspend fun getHourlyPeakUsageData(): Result<List<HourlyPeakUsage>> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            return@withContext Result.failure(SecurityException("Usage access permission is required."))
        }
        val sm = usageStatsManager ?: return@withContext Result.failure(IllegalStateException("UsageStatsManager not available."))
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        val homePackages = getHomePackages()
        val segments = queryForegroundSegments(sm, startTime, endTime, homePackages, getLaunchablePackages())
            .getOrElse { return@withContext Result.failure(it) }
        val hourly = LongArray(24)
        segments.forEach { segment ->
            var cursor = segment.start
            while (cursor < segment.end) {
                val slot = Calendar.getInstance().apply { timeInMillis = cursor }
                val hour = slot.get(Calendar.HOUR_OF_DAY)
                slot.set(Calendar.MINUTE, 0)
                slot.set(Calendar.SECOND, 0)
                slot.set(Calendar.MILLISECOND, 0)
                slot.add(Calendar.HOUR_OF_DAY, 1)
                val chunkEnd = minOf(segment.end, slot.timeInMillis)
                hourly[hour] += (chunkEnd - cursor).coerceAtLeast(0L)
                cursor = chunkEnd
            }
        }
        Result.success(hourly.mapIndexed { hour, duration -> HourlyPeakUsage(hour, duration) })
    }

    // 7x24 heatmap from the same foreground timeline used by the hourly chart.
    suspend fun getWeeklyHeatmapMatrix(): Result<List<List<Int>>> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            return@withContext Result.failure(SecurityException("Usage access permission is required."))
        }
        val sm = usageStatsManager ?: return@withContext Result.failure(IllegalStateException("UsageStatsManager not available."))
        val weekStart = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = System.currentTimeMillis()
        val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        val segments = queryForegroundSegments(sm, weekStart, now, getHomePackages(), getLaunchablePackages())
            .getOrElse { return@withContext Result.failure(it) }
        val gridMillis = Array(7) { LongArray(24) }
        segments.forEach { segment ->
            var cursor = segment.start
            while (cursor < segment.end) {
                val slot = Calendar.getInstance().apply { timeInMillis = cursor }
                val dow = (slot.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
                val hour = slot.get(Calendar.HOUR_OF_DAY)
                slot.set(Calendar.MINUTE, 0)
                slot.set(Calendar.SECOND, 0)
                slot.set(Calendar.MILLISECOND, 0)
                slot.add(Calendar.HOUR_OF_DAY, 1)
                val chunkEnd = minOf(segment.end, slot.timeInMillis)
                gridMillis[dow][hour] += (chunkEnd - cursor).coerceAtLeast(0L)
                cursor = chunkEnd
            }
        }
        val matrix = gridMillis.mapIndexed { dayIndex, row ->
            if (dayIndex > todayDow) {
                List(24) { -1 } // Future weekdays are not zero-usage observations.
            } else {
                row.map { ms ->
                    val minutes = (ms / 60_000L).toInt()
                    when {
                        minutes <= 0 -> 0
                        minutes < 15 -> 1
                        minutes < 30 -> 2
                        else -> 3
                    }
                }
            }
        }
        Result.success(matrix)
    }

    // 4. Daily Report 7-day trend (Sunday to Saturday) strictly from real days
    suspend fun get7DayWeeklyTrend(): Result<List<DayUsagePoint>> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            return@withContext Result.failure(SecurityException("Usage access permission is required."))
        }
        val sm = usageStatsManager ?: return@withContext Result.failure(IllegalStateException("UsageStatsManager not available."))

        val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
        val nowCal = Calendar.getInstance()
        val todayDow = nowCal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday .. 6=Saturday

        val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val homePackages: Set<String> = try {
            packageManager.queryIntentActivities(homeIntent, 0).mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
        val launchablePackages = getLaunchablePackages()

        val points = mutableListOf<DayUsagePoint>()

        for (dow in 0..todayDow) {
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.SUNDAY
                set(Calendar.DAY_OF_WEEK, dow + 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = minOf(cal.timeInMillis, System.currentTimeMillis())

            var dayScreenTime = 0L
            var dayNotifications = 0
            var dayUnlocks = 0

            if (dayStart <= System.currentTimeMillis()) {
                try {
                    val dailyStats = sm.queryAndAggregateUsageStats(dayStart, dayEnd)
                        ?: return@withContext Result.failure(IllegalStateException("Android returned no daily usage aggregate for ${dayLabels[dow]}."))
                    dailyStats.forEach { (pkg, stat) ->
                        if (launchablePackages.contains(pkg) && !RealScreenTimeTracker.isIgnoredPackage(context, pkg, homePackages) &&
                            stat.totalTimeInForeground > 0
                        ) {
                            dayScreenTime += stat.totalTimeInForeground
                        }
                    }
                } catch (e: Exception) {
                    return@withContext Result.failure(e)
                }

                var sawDailyEvent = false
                try {
                    val events = sm.queryEvents(dayStart, dayEnd)
                    val event = UsageEvents.Event()
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        sawDailyEvent = true
                        val pkg = event.packageName
                        when (event.eventType) {
                            USAGE_EVENT_NOTIFICATION_INTERRUPTION -> if (pkg != null &&
                                !RealScreenTimeTracker.isIgnoredPackage(context, pkg, homePackages)
                            ) dayNotifications++
                            UsageEvents.Event.KEYGUARD_HIDDEN -> dayUnlocks++
                        }
                    }
                } catch (e: Exception) {
                    return@withContext Result.failure(e)
                }
                if (!sawDailyEvent && dayScreenTime > 0L) {
                    return@withContext Result.failure(IllegalStateException("Android returned screen-time totals but no usage events for ${dayLabels[dow]}."))
                }
            }

            points.add(
                DayUsagePoint(
                    dayLabel = dayLabels[dow],
                    dayIndex = dow,
                    screenTimeMs = dayScreenTime,
                    notifications = dayNotifications,
                    unlocks = dayUnlocks,
                    isToday = (dow == todayDow)
                )
            )
        }

        Result.success(points)
    }

    // 5. Detailed App Specific Metric from real package telemetry
    suspend fun getAppSpecificMetric(packageName: String): Result<AppSpecificMetric> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            return@withContext Result.failure(SecurityException("Usage access permission is required."))
        }
        val sm = usageStatsManager ?: return@withContext Result.failure(IllegalStateException("UsageStatsManager not available."))

        val appName = getAppName(packageName)
        val dailyTimes = mutableListOf<Long>()
        val dailyOpens = mutableListOf<Int>()
        val dailyNotifications = mutableListOf<Int>()
        var totalWeeklyMs = 0L
        var totalWeeklyOpens = 0
        var totalWeeklyNotifications = 0

        for (dow in 0..6) {
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.SUNDAY
                set(Calendar.DAY_OF_WEEK, dow + 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = minOf(cal.timeInMillis, System.currentTimeMillis())

            var dayMs = 0L
            var opens = 0
            var notifications = 0

            if (dayStart <= System.currentTimeMillis()) {
                try {
                    val dailyStats = sm.queryAndAggregateUsageStats(dayStart, dayEnd)
                        ?: return@withContext Result.failure(IllegalStateException("Android returned no daily usage aggregate for $appName."))
                    dayMs = dailyStats[packageName]?.totalTimeInForeground ?: 0L
                } catch (e: Exception) {
                    return@withContext Result.failure(e)
                }

                var sawAppEvent = false
                try {
                    val events = sm.queryEvents(dayStart, dayEnd)
                    val event = UsageEvents.Event()
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        if (event.packageName == packageName) {
                            sawAppEvent = true
                            when (event.eventType) {
                                UsageEvents.Event.ACTIVITY_RESUMED -> opens++
                                USAGE_EVENT_NOTIFICATION_INTERRUPTION -> notifications++
                            }
                        }
                    }
                } catch (e: Exception) {
                    return@withContext Result.failure(e)
                }
                if (!sawAppEvent && dayMs > 0L) {
                    return@withContext Result.failure(IllegalStateException("Android returned screen-time totals but no usage events for $appName on weekday index $dow."))
                }
            }

            dailyTimes.add(dayMs)
            dailyOpens.add(opens)
            dailyNotifications.add(notifications)
            totalWeeklyMs += dayMs
            totalWeeklyOpens += opens
            totalWeeklyNotifications += notifications
        }

        val elapsedDays = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)).coerceAtLeast(1)
        val dailyAvgMs = totalWeeklyMs / elapsedDays
        val dailyAvgOpens = totalWeeklyOpens / elapsedDays

        Result.success(
            AppSpecificMetric(
                packageName = packageName,
                appName = appName,
                weeklyTotalTimeMs = totalWeeklyMs,
                dailyAvgTimeMs = dailyAvgMs,
                weeklyNotifications = totalWeeklyNotifications,
                dailyAvgNotifications = totalWeeklyNotifications / elapsedDays,
                weeklyAppOpens = totalWeeklyOpens,
                dailyAvgAppOpens = dailyAvgOpens,
                dailyScreenTime = dailyTimes,
                dailyNotifications = dailyNotifications,
                dailyAppOpens = dailyOpens
            )
        )
    }

    private data class ForegroundSegment(val packageName: String, val start: Long, val end: Long)

    private fun getLaunchablePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return try {
            packageManager.queryIntentActivities(intent, 0).mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun getHomePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        return try {
            packageManager.queryIntentActivities(intent, 0).mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun queryForegroundSegments(
        manager: UsageStatsManager,
        startTime: Long,
        endTime: Long,
        homePackages: Set<String>,
        launchablePackages: Set<String>
    ): Result<List<ForegroundSegment>> {
        return try {
            val events = manager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            val segments = mutableListOf<ForegroundSegment>()
            var sawEvent = false
            var activePackage: String? = null
            var activeStart = 0L
            var activeActivityCount = 0
            var screenInteractive = true
            var keyguardVisible = false

            fun closeActive(at: Long) {
                val pkg = activePackage
                if (pkg != null && screenInteractive && !keyguardVisible && at > activeStart) {
                    segments.add(ForegroundSegment(pkg, activeStart, minOf(at, endTime)))
                }
                activePackage = null
                activeStart = 0L
                activeActivityCount = 0
            }

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                sawEvent = true
                val timestamp = event.timeStamp.coerceIn(startTime, endTime)
                val pkg = event.packageName
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED, USAGE_EVENT_CONTINUE_PREVIOUS_DAY -> {
                        if (pkg != null) {
                            if (activePackage != pkg) {
                                closeActive(timestamp)
                                if (screenInteractive && !keyguardVisible && launchablePackages.contains(pkg) &&
                                    !RealScreenTimeTracker.isIgnoredPackage(context, pkg, homePackages)
                                ) {
                                    activePackage = pkg
                                    activeStart = timestamp
                                    activeActivityCount = 1
                                }
                            } else {
                                // One package can have multiple resumed activities. Do not
                                // end its foreground interval when only one activity pauses.
                                activeActivityCount++
                            }
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        if (pkg != null && activePackage == pkg) {
                            activeActivityCount--
                            if (activeActivityCount <= 0) closeActive(timestamp)
                        }
                    }
                    USAGE_EVENT_END_OF_DAY -> {
                        if (pkg != null && activePackage == pkg) closeActive(timestamp)
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN -> {
                        closeActive(timestamp)
                        if (event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                            screenInteractive = false
                        } else {
                            keyguardVisible = true
                        }
                    }
                    UsageEvents.Event.SCREEN_INTERACTIVE -> screenInteractive = true
                    UsageEvents.Event.KEYGUARD_HIDDEN -> keyguardVisible = false
                }
            }
            closeActive(endTime)
            if (!sawEvent) {
                Result.failure(IllegalStateException("Android did not expose usage events for the requested time range."))
            } else {
                Result.success(segments)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getAppName(packageName: String): String {
        val raw = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.')
        }
        return com.example.util.formatAppName(raw)
    }

    private fun getAppCategory(packageName: String): String {
        val appName = getAppName(packageName)
        return RealScreenTimeTracker.getDigitalWellbeingCategory(context, packageName, appName)
    }
}
