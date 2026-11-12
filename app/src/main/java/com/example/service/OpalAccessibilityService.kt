package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.ui.screens.BlockedAppActivity
import com.example.vpn.service.BlockingManager
import java.net.URI
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class OpalAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "OpalAccessibility"

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        @Volatile
        var isBlockingActive: Boolean = true

        @Volatile
        var blockedPackages: Set<String> = emptySet()

        // Blocked website domain rules (e.g., "youtube.com", "instagram.com", "reddit.com")
        @Volatile
        var blockedWebsites: Set<String> = emptySet()
            private set

        private val websiteCategoryMap = ConcurrentHashMap<String, String>()

        private val snoozedPackagesUntilMap = ConcurrentHashMap<String, Long>()
        private val snoozedWebsitesUntilMap = ConcurrentHashMap<String, Long>()

        fun snoozePackage(packageName: String, durationMillis: Long) {
            snoozedPackagesUntilMap[packageName] = System.currentTimeMillis() + durationMillis
        }

        fun isPackageSnoozed(packageName: String): Boolean {
            val expiry = snoozedPackagesUntilMap[packageName] ?: return false
            return if (System.currentTimeMillis() < expiry) {
                true
            } else {
                snoozedPackagesUntilMap.remove(packageName)
                false
            }
        }

        fun snoozeWebsite(domain: String, durationMillis: Long) {
            val clean = normalizeDomain(domain)
            snoozedWebsitesUntilMap[clean] = System.currentTimeMillis() + durationMillis
        }

        fun isWebsiteSnoozed(domain: String): Boolean {
            val clean = normalizeDomain(domain)
            val expiry = snoozedWebsitesUntilMap[clean] ?: return false
            return if (System.currentTimeMillis() < expiry) {
                true
            } else {
                snoozedWebsitesUntilMap.remove(clean)
                false
            }
        }

        private fun safeLog(priority: Int, tag: String, message: String) {
            try {
                when (priority) {
                    Log.DEBUG -> Log.d(tag, message)
                    Log.INFO -> Log.i(tag, message)
                    Log.WARN -> Log.w(tag, message)
                    Log.ERROR -> Log.e(tag, message)
                }
            } catch (_: Throwable) {}
        }

        fun updateBlockedWebsites(domains: Collection<String>, categories: Map<String, String> = emptyMap()) {
            val normalized = domains.map { normalizeDomain(it) }.filter { it.isNotBlank() }.toSet()
            blockedWebsites = normalized
            websiteCategoryMap.clear()
            categories.forEach { (d, cat) ->
                websiteCategoryMap[normalizeDomain(d)] = cat
            }
            isBlockingActive = blockedPackages.isNotEmpty() || blockedWebsites.isNotEmpty()
            safeLog(Log.DEBUG, TAG, "Updated blocked websites count: ${blockedWebsites.size}")
        }

        fun normalizeDomain(raw: String): String {
            return raw.trim()
                .lowercase(Locale.ROOT)
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .removePrefix("m.")
                .substringBefore('/')
                .substringBefore('?')
                .substringBefore('#')
                .substringBefore(':')
        }

        // List of all popular browser package names on Android
        val KNOWN_BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "com.google.android.googlequicksearchbox", // Google App / Chrome Custom Tabs
            "com.sec.android.app.sbrowser", // Samsung Internet
            "com.sec.android.app.sbrowser.beta",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fenix", // Firefox Android
            "org.mozilla.focus",
            "com.microsoft.emmx", // Microsoft Edge
            "com.microsoft.emmx.beta",
            "com.opera.browser", // Opera
            "com.opera.mini.native",
            "com.opera.touch",
            "com.opera.gx",
            "com.brave.browser", // Brave
            "com.brave.browser_nightly",
            "com.brave.browser_beta",
            "com.duckduckgo.mobile.android", // DuckDuckGo
            "com.vivaldi.browser", // Vivaldi
            "com.mi.globalbrowser", // Xiaomi Mi Browser
            "com.UCMobile.intl", // UC Browser
            "com.android.browser" // AOSP Browser
        )

        // Common address / URL bar resource IDs in browsers
        private val KNOWN_URL_VIEW_IDS = listOf(
            "com.android.chrome:id/url_bar",
            "com.android.chrome:id/search_box_text",
            "com.chrome.beta:id/url_bar",
            "com.chrome.dev:id/url_bar",
            "com.chrome.canary:id/url_bar",
            "com.google.android.googlequicksearchbox:id/googleapp_search_box",
            "com.google.android.googlequicksearchbox:id/url_bar",
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.sec.android.app.sbrowser:id/url_bar",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "org.mozilla.firefox:id/toolbar_url_text",
            "org.mozilla.fenix:id/toolbar_url_text",
            "org.mozilla.focus:id/display_url",
            "com.microsoft.emmx:id/url_bar",
            "com.microsoft.emmx:id/search_box",
            "com.opera.browser:id/url_field",
            "com.opera.browser:id/url_bar",
            "com.brave.browser:id/url_bar",
            "com.duckduckgo.mobile.android:id/omnibarTextInput"
        )
    }

    private var lastBlockedPackage: String? = null
    private var lastBlockedPackageTime: Long = 0L

    private var lastBlockedWebsite: String? = null
    private var lastBlockedWebsiteTime: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        try {
            BlockingManager.getInstance(applicationContext).onAccessibilityServiceStateChanged()
        } catch (_: Exception) { }
        safeLog(Log.INFO, TAG, "PRIME Accessibility Service connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isBlockingActive || event == null) return
        val packageName = event.packageName?.toString() ?: return

        // Safety: Never block Prime itself, system UI, or home launchers
        if (packageName == applicationContext.packageName ||
            packageName == "com.android.systemui" ||
            packageName.contains("launcher", ignoreCase = true)
        ) {
            return
        }

        // 1. In-Browser Website Detection and Interception
        if (isBrowser(packageName) && blockedWebsites.isNotEmpty()) {
            checkAndInterceptBrowserWebsite(event, packageName)
        }

        // 2. Native App Package Blocking (e.g. Instagram, TikTok, YouTube apps)
        if (blockedPackages.contains(packageName)) {
            checkAndInterceptApp(packageName)
        }
    }

    private fun isBrowser(packageName: String): Boolean {
        if (KNOWN_BROWSER_PACKAGES.contains(packageName)) return true
        val lower = packageName.lowercase(Locale.ROOT)
        return lower.contains("browser") || lower.contains("chrome") || lower.contains("firefox")
    }

    /**
     * Inspects browser address bars or URL nodes in the active window
     * and triggers instant blocking if matching a blocked domain rule.
     */
    private fun checkAndInterceptBrowserWebsite(event: AccessibilityEvent, browserPackage: String) {
        val rootNode = rootInActiveWindow ?: event.source ?: return
        val rawUrlText = try {
            extractUrlFromNodeHierarchy(rootNode)
        } finally {
            rootNode.recycle()
        } ?: return

        val matchedDomain = matchBlockedDomain(rawUrlText) ?: return

        if (isWebsiteSnoozed(matchedDomain)) {
            return
        }

        val now = System.currentTimeMillis()
        if (matchedDomain == lastBlockedWebsite && (now - lastBlockedWebsiteTime) < 1500L) {
            return
        }
        lastBlockedWebsite = matchedDomain
        lastBlockedWebsiteTime = now

        safeLog(Log.WARN, TAG, "BLOCKED WEBSITE DETECTED: $matchedDomain in $browserPackage (Raw: $rawUrlText)")

        // Instantly navigate back / close page in the browser
        performGlobalAction(GLOBAL_ACTION_BACK)

        // Launch full-screen Prime Blocking Overlay
        val category = websiteCategoryMap[matchedDomain] ?: "Distracting Content"
        mainHandler.post {
            BlockedAppActivity.startForWebsite(
                context = applicationContext,
                domain = matchedDomain,
                fullUrl = rawUrlText,
                browserPackage = browserPackage,
                category = category
            )
        }

        // Record in Blocking Analytics
        try {
            BlockingManager.getInstance(applicationContext).onDomainBlocked(
                domain = matchedDomain,
                category = category,
                reason = "Accessibility Real-Time Shield ($browserPackage)"
            )
        } catch (_: Exception) {}
    }

    private fun extractUrlFromNodeHierarchy(root: AccessibilityNodeInfo): String? {
        // Fast path 1: Check known view IDs
        for (viewId in KNOWN_URL_VIEW_IDS) {
            try {
                val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
                for (node in nodes) {
                    try {
                        val text = node.text?.toString()?.trim()
                        if (!text.isNullOrEmpty() && (text.contains('.') || text.startsWith("http"))) {
                            return text
                        }
                    } finally {
                        node.recycle()
                    }
                }
            } catch (_: Exception) {}
        }

        // Fast path 2: Shallow traversal of window nodes (max 40 nodes)
        return findUrlByTraversal(root, 0, intArrayOf(40))
    }

    private fun findUrlByTraversal(node: AccessibilityNodeInfo?, currentDepth: Int, remainingNodes: IntArray): String? {
        if (node == null || currentDepth > 5 || remainingNodes[0] <= 0) return null
        remainingNodes[0]--

        val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""
        val text = node.text?.toString()?.trim()

        if (!text.isNullOrEmpty() && (
                viewId.contains("url") ||
                viewId.contains("address") ||
                viewId.contains("search") ||
                viewId.contains("location") ||
                viewId.contains("omnibar")
            )
        ) {
            if (text.contains('.') || text.startsWith("http")) {
                return text
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = try {
                findUrlByTraversal(child, currentDepth + 1, remainingNodes)
            } finally {
                child.recycle()
            }
            if (found != null) return found
        }

        return null
    }

    /**
     * Matches raw URL text against our active blocked domains.
     * Supports exact matches, subdomain matches (m.youtube.com -> youtube.com),
     * and query string direct domain visits.
     */
    private fun matchBlockedDomain(rawText: String): String? {
        // Only treat an actual hostname/URL as a visit. A search query such as
        // "how does instagram.com work" must not trigger a false-positive block.
        if (!com.example.vpn.dns.DomainMatcher.isValidDomain(rawText)) return null
        val cleanHost = com.example.vpn.dns.DomainMatcher.normalize(rawText)
        return blockedWebsites.firstOrNull { rule ->
            com.example.vpn.dns.DomainMatcher.matchesRule(cleanHost, rule)
        }
    }

    /**
     * Intercepts native Android applications (e.g. Instagram app, TikTok app).
     */
    private fun checkAndInterceptApp(packageName: String) {
        if (!blockedPackages.contains(packageName)) return
        if (isPackageSnoozed(packageName)) return

        val now = System.currentTimeMillis()
        if (packageName == lastBlockedPackage && (now - lastBlockedPackageTime) < 1200L) {
            return
        }
        lastBlockedPackage = packageName
        lastBlockedPackageTime = now

        safeLog(Log.WARN, TAG, "BLOCKED APP DETECTED: $packageName")

        // Instantly dismiss the blocked app to Home screen so it cannot be used
        performGlobalAction(GLOBAL_ACTION_HOME)

        val appLabel = try {
            val pm = applicationContext.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.')
        }

        mainHandler.postDelayed({
            BlockedAppActivity.start(applicationContext, packageName, appLabel)
        }, 80L)

        try {
            BlockingManager.getInstance(applicationContext).onDomainBlocked(
                domain = packageName,
                category = "App Blocker",
                reason = "Focus Shield ($appLabel)"
            )
        } catch (_: Exception) {}
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        try {
            BlockingManager.getInstance(applicationContext).onAccessibilityServiceStateChanged()
        } catch (_: Exception) { }
    }
}
