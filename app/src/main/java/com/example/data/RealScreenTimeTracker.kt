package com.example.data

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.example.model.BlockedAppItem
import com.example.util.PermissionHelper
import java.util.Calendar

object RealScreenTimeTracker {

    data class TrackingResult(
        val hasUsagePermission: Boolean,
        val totalScreenTimeFormatted: String,
        val totalScreenTimeMillis: Long,
        val apps: List<BlockedAppItem>,
        val score: Int,
        val sleepScore: Int,
        val focusScore: Int,
        val restScore: Int,
        val errorMessage: String? = null
    )

    fun hasUsageStatsPermission(context: Context): Boolean {
        return PermissionHelper.hasUsageAccessPermission(context)
    }

    fun requestUsageStatsPermission(context: Context) {
        PermissionHelper.requestUsageAccessPermission(context)
    }

    /**
     * Determines whether a package is a system launcher, IME keyboard, system UI,
     * wallpaper/ambient service, or background framework process that must NOT be
     * counted towards user screen time (matching Android native Digital Wellbeing).
     */
    fun isIgnoredPackage(
        context: Context,
        packageName: String,
        homePackages: Set<String> = emptySet()
    ): Boolean {
        if (packageName == context.packageName) return true
        if (homePackages.contains(packageName)) return true

        val lower = packageName.lowercase()

        // 1. Android OS & System UI
        if (lower == "android" || lower == "com.android.systemui") return true

        // 2. Launchers / Home screens (Samsung One UI Home, Pixel Launcher, etc.)
        if (lower == "com.sec.android.app.launcher" ||
            lower == "com.google.android.apps.nexuslauncher" ||
            lower == "com.miui.home" ||
            lower == "com.huawei.android.launcher" ||
            lower == "com.oppo.launcher" ||
            lower == "com.bbk.launcher2" ||
            lower == "com.oneplus.launcher" ||
            lower == "com.teslacoilsw.launcher" ||
            lower.endsWith(".launcher") ||
            lower.contains(".launcher.")
        ) return true

        // 3. Keyboards / Input Method Editors
        if (lower.contains("honeyboard") ||
            lower.contains("inputmethod") ||
            lower == "com.google.android.inputmethod.latin" ||
            lower == "com.touchtype.swiftkey"
        ) return true

        // 4. Lockscreen, Wallpaper, Ambient services
        if (lower.contains("keyguard") ||
            lower.contains("wallpaper") ||
            lower.contains("ambient") ||
            lower.contains("screensaver")
        ) return true

        // 5. Background Google Play services daemon
        if (lower == "com.google.android.gms") return true

        return false
    }

    fun queryRealScreenTime(
        context: Context,
        existingWebsites: List<BlockedAppItem> = emptyList(),
        existingBlockedIds: Set<String> = emptySet(),
        verifiedFocusMinutes: Int = 0,
        verifiedBlockedMinutes: Int = 0
    ): TrackingResult {
        val pm = context.packageManager
        val hasPermission = hasUsageStatsPermission(context)

        // 1. Detect all Home Launcher packages dynamically
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val homePackages: Set<String> = try {
            pm.queryIntentActivities(homeIntent, 0).mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }

        // 2. Query real installed applications with launcher intent (works without usage permission)
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = try {
            pm.queryIntentActivities(launcherIntent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        // 3. Query UsageStats strictly for Today (midnight to now)
        val usageStatsMap = mutableMapOf<String, Long>()
        var fetchError: String? = null

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        if (hasPermission) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager != null) {
                // Primary: Authoritative aggregated stats from Android System
                try {
                    val aggregated = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
                    aggregated?.forEach { (pkg, stat) ->
                        if (!isIgnoredPackage(context, pkg, homePackages) && stat.totalTimeInForeground > 0 && stat.lastTimeUsed >= startTime) {
                            usageStatsMap[pkg] = stat.totalTimeInForeground
                        }
                    }
                } catch (e: Exception) {
                    fetchError = "Unable to read Android usage statistics: ${e.message}"
                }

                }
            } else {
                fetchError = "UsageStatsManager system service not available on device."
            }
        }

        // 4. Build list of real installed user applications
        val realAppItems = mutableListOf<BlockedAppItem>()
        val seenPackages = mutableSetOf<String>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (seenPackages.contains(pkg) || isIgnoredPackage(context, pkg, homePackages)) continue
            seenPackages.add(pkg)

            val rawLabel = try {
                info.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkg.substringAfterLast('.')
            }
            val appName = com.example.util.formatAppName(rawLabel)

            val usageMillis = usageStatsMap[pkg] ?: 0L
            val isAllowed = isAlwaysAllowedApp(appName, pkg)
            val primeCategory = if (isAllowed) "Always allowed" else "Distracting"
            val wellbeingCategory = getDigitalWellbeingCategory(context, pkg, appName)

            val isBlocked = existingBlockedIds.contains(pkg)

            val usageText = when {
                !hasPermission -> "Not tracked yet"
                usageMillis > 0 -> formatDuration(usageMillis) + " today"
                else -> "Not used today"
            }

            realAppItems.add(
                BlockedAppItem(
                    id = pkg,
                    name = appName,
                    packageName = pkg,
                    isWebsite = false,
                    usageToday = usageText,
                    usageMillis = usageMillis,
                    category = primeCategory,
                    wellbeingCategory = wellbeingCategory,
                    isBlocked = isBlocked,
                    brandColorHex = getBrandColor(appName, pkg)
                )
            )
        }

        // Sort: apps with most usage first, then alphabetically
        realAppItems.sortByDescending { it.usageMillis }

        // Combine with user-configured websites
        val combined = realAppItems + existingWebsites

        if (!hasPermission) {
            return TrackingResult(
                hasUsagePermission = false,
                totalScreenTimeFormatted = "Permission Required",
                totalScreenTimeMillis = 0L,
                apps = combined,
                score = 0,
                sleepScore = 0,
                focusScore = 0,
                restScore = 0,
                errorMessage = "Usage access permission is required to fetch real screen time."
            )
        }

        // Total screen time is strictly the sum of all foreground user apps used today (matching Digital Wellbeing)
        val totalForegroundMillis = realAppItems.sumOf { it.usageMillis }

        val formattedTotal = if (totalForegroundMillis > 0) {
            formatDuration(totalForegroundMillis)
        } else {
            "0m"
        }

        // 5. Calculate normalized PRIME score (0-1000 points)
        val minutesToday = (totalForegroundMillis / (1000 * 60)).toInt()

        // Only completed, verified sessions contribute to focus and blocking scores.
        val effectiveFocusMin = verifiedFocusMinutes.coerceAtLeast(0)
        val effectiveBlockedMin = verifiedBlockedMinutes.coerceAtLeast(0)

        val screenTimeScore = PrimeScoreCalculator.calculateScreenTimeScore(
            actualMinutes = minutesToday,
            targetMinutes = 180
        )
        val focusScore = PrimeScoreCalculator.calculateFocusScore(
            verifiedFocusMinutes = effectiveFocusMin,
            targetFocusMinutes = 120
        )
        val blockingScore = PrimeScoreCalculator.calculateBlockingScore(
            verifiedBlockedMinutes = effectiveBlockedMin,
            targetBlockedMinutes = 180
        )
        val totalPrimeScore = (screenTimeScore + focusScore + blockingScore).toInt().coerceIn(0, 1000)

        val sleepScore = screenTimeScore.toInt().coerceIn(0, 300)
        val finalFocusScore = focusScore.toInt().coerceIn(0, 350)
        val restScore = blockingScore.toInt().coerceIn(0, 350)

        return TrackingResult(
            hasUsagePermission = true,
            totalScreenTimeFormatted = formattedTotal,
            totalScreenTimeMillis = totalForegroundMillis,
            apps = combined,
            score = totalPrimeScore,
            sleepScore = sleepScore,
            focusScore = finalFocusScore,
            restScore = restScore,
            errorMessage = fetchError
        )
    }

    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / (1000 * 60)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            mins > 0 -> "${mins}m"
            else -> "< 1m"
        }
    }

    fun getDigitalWellbeingCategory(context: Context, pkg: String, name: String): String {
        val lower = (name + " " + pkg).lowercase()
        return when {
            lower.contains("instagram") ||
            lower.contains("whatsapp") ||
            lower.contains("reddit") ||
            lower.contains("twitter") ||
            lower.contains("x.com") ||
            lower.contains("facebook") ||
            lower.contains("snapchat") ||
            lower.contains("linkedin") ||
            lower.contains("telegram") ||
            lower.contains("discord") ||
            lower.contains("social") -> "Social"

            lower.contains("youtube") ||
            lower.contains("netflix") ||
            lower.contains("spotify") ||
            lower.contains("twitch") ||
            lower.contains("prime video") ||
            lower.contains("music") ||
            lower.contains("video") -> "Video"

            lower.contains("chrome") ||
            lower.contains("browser") ||
            lower.contains("gmail") ||
            lower.contains("docs") ||
            lower.contains("sheets") ||
            lower.contains("drive") ||
            lower.contains("calendar") ||
            lower.contains("calculator") ||
            lower.contains("myjio") ||
            lower.contains("jio") ||
            lower.contains("bank") ||
            lower.contains("finance") ||
            lower.contains("pay") ||
            lower.contains("productivity") -> "Productivity and finance"

            lower.contains("game") -> "Games"
            lower.contains("settings") || lower.contains("system") -> "System"
            else -> "Other"
        }
    }

    fun isAlwaysAllowedApp(name: String, pkg: String): Boolean {
        val lower = (name + " " + pkg).lowercase()
        return lower.contains("phone") ||
                lower.contains("dialer") ||
                lower.contains("telecom") ||
                lower.contains("contacts") ||
                lower.contains("messaging") ||
                lower.contains("messages") ||
                lower.contains("mms") ||
                lower.contains("camera") ||
                lower.contains("clock") ||
                lower.contains("deskclock") ||
                lower.contains("alarm") ||
                lower.contains("calculator") ||
                lower.contains("settings") ||
                lower.contains("emergency") ||
                lower.contains("maps") ||
                lower.contains("authenticator")
    }

    fun isDistractingApp(name: String, pkg: String): Boolean {
        if (isAlwaysAllowedApp(name, pkg)) return false
        val lower = (name + " " + pkg).lowercase()
        return lower.contains("instagram") ||
                lower.contains("reddit") ||
                lower.contains("tiktok") ||
                lower.contains("twitter") ||
                lower.contains("x.com") ||
                lower.contains("youtube") ||
                lower.contains("yt music") ||
                lower.contains("facebook") ||
                lower.contains("snapchat") ||
                lower.contains("netflix") ||
                lower.contains("linkedin") ||
                lower.contains("pinterest") ||
                lower.contains("twitch") ||
                lower.contains("discord") ||
                lower.contains("spotify") ||
                lower.contains("music") ||
                lower.contains("video") ||
                lower.contains("chrome") ||
                lower.contains("browser") ||
                lower.contains("game") ||
                lower.contains("shop") ||
                lower.contains("amazon") ||
                lower.contains("reels") ||
                lower.contains("shorts") ||
                lower.contains("news")
    }

    private fun getBrandColor(name: String, pkg: String): Long {
        val lower = (name + " " + pkg).lowercase()
        return when {
            lower.contains("instagram") -> 0xFFE1306C
            lower.contains("linkedin") -> 0xFF0077B5
            lower.contains("reddit") -> 0xFFFF4500
            lower.contains("youtube") -> 0xFFFF0000
            lower.contains("twitter") || lower.contains("x.com") -> 0xFF1DA1F2
            lower.contains("chrome") -> 0xFFFBBC05
            lower.contains("whatsapp") -> 0xFF25D366
            lower.contains("phone") -> 0xFF25D366
            lower.contains("calculator") -> 0xFF4CAF50
            lower.contains("messages") -> 0xFF1A73E8
            lower.contains("camera") -> 0xFF5F6368
            lower.contains("gmail") -> 0xFFEA4335
            lower.contains("jio") -> 0xFF0A2885
            else -> {
                val colors = listOf(0xFF5DF2B5, 0xFF4285F4, 0xFF9C27B0, 0xFFFF9800, 0xFF00BCD4, 0xFF00E676)
                colors[Math.abs(name.hashCode()) % colors.size]
            }
        }
    }
}
