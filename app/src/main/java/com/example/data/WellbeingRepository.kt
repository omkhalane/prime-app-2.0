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

data class AppUsageData(
    val packageName: String,
    val appName: String,
    val totalTimeMs: Long,
    val category: String,
    val openCount: Int = 0,
    val notificationCount: Int = 0
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

        val appMap = mutableMapOf<String, Long>()
        val openCounts = mutableMapOf<String, Int>()

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
            aggregated?.forEach { (pkg, stat) ->
                if (!RealScreenTimeTracker.isIgnoredPackage(context, pkg, homePackages) && stat.totalTimeInForeground > 0 && stat.lastTimeUsed >= startTime) {
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
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED ->
                        openCounts[pkg] = (openCounts[pkg] ?: 0) + 1
                    12 ->
                        notificationCounts[pkg] = (notificationCounts[pkg] ?: 0) + 1
                }
            }
        } catch (_: Exception) {
            // Android may not expose these event counters on every device.
        }

        val result = appMap.map { (pkg, duration) ->
            val appLabel = getAppName(pkg)
            val cat = getAppCategory(pkg)
            val opens = openCounts[pkg] ?: 0
            val notifications = notificationCounts[pkg] ?: 0
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

        val appMap = mutableMapOf<String, Long>()
        val openCounts = mutableMapOf<String, Int>()

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis

        // Android's aggregate API returns a total per package for the requested range.
        try {
            val aggregated = sm.queryAndAggregateUsageStats(startTime, endTime)
            aggregated?.forEach { (pkg, stat) ->
                if (stat.totalTimeInForeground > 0 &&
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
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED ->
                        openCounts[pkg] = (openCounts[pkg] ?: 0) + 1
                    12 ->
                        notificationCounts[pkg] = (notificationCounts[pkg] ?: 0) + 1
                }
            }
        } catch (_: Exception) {
            // Android may not expose every event on every device.
        }

        val result = appMap.map { (pkg, duration) ->
            val appLabel = getAppName(pkg)
            val cat = getAppCategory(pkg)
            val opens = openCounts[pkg] ?: 0
            val notifications = notificationCounts[pkg] ?: 0
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

    // 3. Peak Usage Hourly Analysis (0-23 Hours) from real system events
    suspend fun getHourlyPeakUsageData(): Result<List<HourlyPeakUsage>> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            return@withContext Result.failure(SecurityException("Usage access permission is required."))
        }
        val sm = usageStatsManager ?: return@withContext Result.failure(IllegalStateException("UsageStatsManager not available."))

        val hourlyMap = IntArray(24) { 0 }.toMutableList()

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis

        try {
            val events = sm.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            val lastEventTime = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (pkg == context.packageName) continue

                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastEventTime[pkg] = event.timeStamp
                } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == UsageEvents.Event.ACTIVITY_STOPPED) {
                    val start = lastEventTime.remove(pkg)
                    if (start != null) {
                        var currentStart: Long = start
                        val end: Long = minOf(event.timeStamp, start + 2 * 60 * 60 * 1000L)

                        while (currentStart < end) {
                            val slotCal = Calendar.getInstance().apply { timeInMillis = currentStart }
                            val hour = slotCal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)

                            slotCal.set(Calendar.MINUTE, 0)
                            slotCal.set(Calendar.SECOND, 0)
                            slotCal.set(Calendar.MILLISECOND, 0)
                            slotCal.add(Calendar.HOUR_OF_DAY, 1)
                            val nextHourStart: Long = slotCal.timeInMillis

                            val chunkEnd: Long = minOf(end, nextHourStart)
                            val chunkDuration = (chunkEnd - currentStart).toInt()

                            hourlyMap[hour] = (hourlyMap[hour] + chunkDuration)
                            currentStart = chunkEnd
                        }
                    }
                } else if (event.eventType == 16 || event.eventType == 17 || event.eventType == 26) {
                    lastEventTime.clear()
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        Result.success(hourlyMap.mapIndexed { hour, duration -> HourlyPeakUsage(hour, duration.toLong()) })
    }

    // Real 7x24 Heatmap Matrix (Sun..Sat x 0..23h) computed strictly from system UsageEvents
    suspend fun getWeeklyHeatmapMatrix(): Result<List<List<Int>>> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            return@withContext Result.failure(SecurityException("Usage access permission is required."))
        }
        val sm = usageStatsManager ?: return@withContext Result.failure(IllegalStateException("UsageStatsManager not available."))

        // matrix: 7 days (Sun=0..Sat=6) x 24 hours (0..23) in milliseconds
        val gridMillis = Array(7) { LongArray(24) { 0L } }

        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStart = cal.timeInMillis
        val weekEnd = System.currentTimeMillis()

        try {
            val events = sm.queryEvents(weekStart, weekEnd)
            val event = UsageEvents.Event()
            val lastEventTime = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (pkg == context.packageName) continue

                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastEventTime[pkg] = event.timeStamp
                } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == UsageEvents.Event.ACTIVITY_STOPPED) {
                    val start = lastEventTime.remove(pkg)
                    if (start != null && start >= weekStart) {
                        var currentStart: Long = start
                        val end: Long = minOf(event.timeStamp, start + 2 * 60 * 60 * 1000L)

                        while (currentStart < end) {
                            val slotCal = Calendar.getInstance().apply { timeInMillis = currentStart }
                            val dow = (slotCal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
                            val hour = slotCal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)

                            slotCal.set(Calendar.MINUTE, 0)
                            slotCal.set(Calendar.SECOND, 0)
                            slotCal.set(Calendar.MILLISECOND, 0)
                            slotCal.add(Calendar.HOUR_OF_DAY, 1)
                            val nextHourStart: Long = slotCal.timeInMillis

                            val chunkEnd: Long = minOf(end, nextHourStart)
                            val chunkDuration = (chunkEnd - currentStart).coerceAtLeast(0L)

                            gridMillis[dow][hour] += chunkDuration
                            currentStart = chunkEnd
                        }
                    }
                } else if (event.eventType == 16 || event.eventType == 17 || event.eventType == 26) {
                    lastEventTime.clear()
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        // Map milliseconds to levels: 0 (0m), 1 (1..15m), 2 (15..30m), 3 (30..60m)
        val matrix = gridMillis.map { row ->
            row.map { ms ->
                val minutes = (ms / (1000 * 60)).toInt()
                when {
                    minutes <= 0 -> 0
                    minutes in 1..14 -> 1
                    minutes in 15..29 -> 2
                    else -> 3
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

        val points = mutableListOf<DayUsagePoint>()

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

            var dayScreenTime = 0L
            var dayNotifications = 0
            var dayUnlocks = 0

            if (dayStart <= System.currentTimeMillis()) {
                try {
                    val dailyStats = sm.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        dayStart,
                        dayEnd
                    )
                    dailyStats?.forEach { stat ->
                        val pkg = stat.packageName ?: return@forEach
                        if (!RealScreenTimeTracker.isIgnoredPackage(context, pkg, homePackages) &&
                            stat.totalTimeInForeground > 0
                        ) {
                            dayScreenTime += stat.totalTimeInForeground
                        }
                    }
                } catch (_: Exception) { }

                try {
                    val events = sm.queryEvents(dayStart, dayEnd)
                    val event = UsageEvents.Event()
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        val pkg = event.packageName
                        when (event.eventType) {
                            12 -> if (pkg != null &&
                                !RealScreenTimeTracker.isIgnoredPackage(context, pkg, homePackages)
                            ) dayNotifications++
                            18 -> dayUnlocks++
                        }
                    }
                } catch (_: Exception) { }
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
                    val dailyStats = sm.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        dayStart,
                        dayEnd
                    )
                    dailyStats?.find { it.packageName == packageName }?.let {
                        dayMs = it.totalTimeInForeground
                    }
                } catch (_: Exception) { }

                try {
                    val events = sm.queryEvents(dayStart, dayEnd)
                    val event = UsageEvents.Event()
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        if (event.packageName == packageName) {
                            when (event.eventType) {
                                UsageEvents.Event.ACTIVITY_RESUMED -> opens++
                                12 -> notifications++
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            dailyTimes.add(dayMs)
            dailyOpens.add(opens)
            dailyNotifications.add(notifications)
            totalWeeklyMs += dayMs
            totalWeeklyOpens += opens
            totalWeeklyNotifications += notifications
        }

        val dailyAvgMs = totalWeeklyMs / 7
        val dailyAvgOpens = totalWeeklyOpens / 7

        Result.success(
            AppSpecificMetric(
                packageName = packageName,
                appName = appName,
                weeklyTotalTimeMs = totalWeeklyMs,
                dailyAvgTimeMs = dailyAvgMs,
                weeklyNotifications = totalWeeklyNotifications,
                dailyAvgNotifications = totalWeeklyNotifications / 7,
                weeklyAppOpens = totalWeeklyOpens,
                dailyAvgAppOpens = dailyAvgOpens,
                dailyScreenTime = dailyTimes,
                dailyNotifications = dailyNotifications,
                dailyAppOpens = dailyOpens
            )
        )
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
