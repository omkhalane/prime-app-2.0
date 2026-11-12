package com.example.vpn.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.service.OpalAccessibilityService
import com.example.vpn.repo.BlockingAnalyticsRepository
import com.example.vpn.repo.BlockingScheduleRepository
import com.example.vpn.repo.BlocklistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockingManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BlockingManager"
        @Volatile
        private var INSTANCE: BlockingManager? = null

        fun getInstance(context: Context): BlockingManager {
            return INSTANCE ?: synchronized(this) {
                val instance = BlockingManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("prime_blocking_manager_prefs", Context.MODE_PRIVATE)
    // Keep measured protection time beside PRIME's verified wellbeing counters.
    private val wellbeingPrefs: SharedPreferences = context.getSharedPreferences("prime_focus_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reconcileMutex = Mutex()
    @Volatile private var lastBlockingCheckpointMillis: Long? = null

    val blocklistRepo = BlocklistRepository(context)
    val analyticsRepo = BlockingAnalyticsRepository(context)
    val scheduleRepo = BlockingScheduleRepository(context)

    private val _isVpnRunning = MutableStateFlow(PrimeVpnService.isServiceRunning)
    val isVpnRunning: StateFlow<Boolean> = _isVpnRunning.asStateFlow()

    private val _isPermanentProtectionEnabled = MutableStateFlow(
        prefs.getBoolean("permanent_protection_enabled", false)
    )
    val isPermanentProtectionEnabled: StateFlow<Boolean> = _isPermanentProtectionEnabled.asStateFlow()

    private val _activeRulesCount = MutableStateFlow(0)
    val activeRulesCount: StateFlow<Int> = _activeRulesCount.asStateFlow()

    // Focus Session active state
    @Volatile private var isSessionActive = prefs.getBoolean("focus_session_active", false)
    @Volatile private var sessionBlockedDomains: Set<String> =
        (prefs.getStringSet("focus_session_domains", emptySet()) ?: emptySet()).toSet()
    @Volatile private var sessionBlockedCategories: Set<String> =
        (prefs.getStringSet("focus_session_categories", emptySet()) ?: emptySet()).toSet()

    init {
        BlockingRecoveryManager.enqueuePeriodicReconciliation(context)
        scope.launch {
            while (isActive) {
                runCatching { reconcileActiveRules() }
                    .onFailure { Log.w(TAG, "Blocking reconciliation failed; will retry", it) }
                delay(30_000L)
            }
        }
    }

    fun updateVpnRunningState(running: Boolean) {
        _isVpnRunning.value = running
        checkpointBlockingDuration()
    }

    fun onVpnRevoked() {
        _isVpnRunning.value = false
        checkpointBlockingDuration()
    }

    fun onAccessibilityServiceStateChanged() {
        checkpointBlockingDuration()
    }

    /** Call after the effective app/domain block lists change. */
    fun onBlockingTargetsChanged() {
        checkpointBlockingDuration()
    }

    /** Flush the current active interval before a wellbeing score is read. */
    fun flushBlockingDuration() {
        checkpointBlockingDuration()
    }

    /**
     * Count only while a configured target is protected by a running enforcement service.
     * Checkpointing every 15 seconds limits lost time if Android kills the process.
     */
    @Synchronized
    private fun checkpointBlockingDuration() {
        val now = System.currentTimeMillis()
        val appProtectionActive = OpalAccessibilityService.blockedPackages.any {
            !OpalAccessibilityService.isPackageSnoozed(it)
        } && OpalAccessibilityService.isServiceRunning
        val websiteProtectionActive = OpalAccessibilityService.blockedWebsites.any {
            !OpalAccessibilityService.isWebsiteSnoozed(it)
        } && (PrimeVpnService.isServiceRunning || OpalAccessibilityService.isServiceRunning)
        val actuallyEnforced = OpalAccessibilityService.isBlockingActive &&
            (appProtectionActive || websiteProtectionActive)

        val previous = lastBlockingCheckpointMillis
        if (previous != null && now > previous) {
            addMeasuredBlockingInterval(previous, now)
        }
        lastBlockingCheckpointMillis = if (actuallyEnforced) now else null
    }

    private fun addMeasuredBlockingInterval(startMillis: Long, endMillis: Long) {
        BlockingDurationSlices.split(startMillis, endMillis).forEach { slice ->
            val key = "verified_block_ms_${slice.dateKey}"
            val current = wellbeingPrefs.getLong(key, 0L)
            wellbeingPrefs.edit().putLong(key, current + slice.durationMillis).apply()
        }
    }

    fun onDomainBlocked(domain: String, category: String, reason: String) {
        analyticsRepo.logBlockedAttempt(domain, category, reason)
    }

    suspend fun resolveCurrentActiveRules(): Map<String, String> = withContext(Dispatchers.IO) {
        val scheduleCategories = mutableSetOf<String>()
        val scheduleDomains = mutableSetOf<String>()

        val activeSchedules = scheduleRepo.getCurrentlyActiveSchedules()
        for (sched in activeSchedules) {
            scheduleCategories.addAll(sched.blockedCategoriesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            scheduleDomains.addAll(sched.customDomainsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() })
        }

        val allSessionDomains = sessionBlockedDomains + scheduleDomains
        val allSessionCategories = sessionBlockedCategories + scheduleCategories

        val activeMap = blocklistRepo.resolveActiveBlockingMap(
            sessionDomains = allSessionDomains,
            sessionCategories = allSessionCategories
        )
        _activeRulesCount.value = activeMap.size
        // Synchronize in-browser interceptor rules
        OpalAccessibilityService.updateBlockedWebsites(activeMap.keys, activeMap)
        checkpointBlockingDuration()
        activeMap
    }

    suspend fun reconcileActiveRules() = reconcileMutex.withLock {
        val rules = resolveCurrentActiveRules()
        _activeRulesCount.value = rules.size
        val shouldRun = _isPermanentProtectionEnabled.value || isSessionActive ||
            scheduleRepo.getCurrentlyActiveSchedules().isNotEmpty()

        if (shouldRun && !PrimeVpnService.isServiceRunning) {
            if (VpnPermissionManager.isVpnPrepared(context)) PrimeVpnService.startService(context)
        } else if (!shouldRun && PrimeVpnService.isServiceRunning) {
            PrimeVpnService.stopService(context)
        } else if (PrimeVpnService.isServiceRunning) {
            val intent = android.content.Intent(context, PrimeVpnService::class.java).apply {
                action = PrimeVpnService.ACTION_RELOAD_RULES
            }
            context.startService(intent)
        }
        checkpointBlockingDuration()
    }

    fun togglePermanentProtection(enabled: Boolean) {
        _isPermanentProtectionEnabled.value = enabled
        prefs.edit().putBoolean("permanent_protection_enabled", enabled).apply()
        scope.launch {
            reconcileActiveRules()
        }
    }

    fun onFocusSessionStarted(
        blockedDomains: Set<String> = emptySet(),
        blockedCategories: Set<String> = emptySet()
    ) {
        isSessionActive = true
        sessionBlockedDomains = blockedDomains.filter { it.isNotBlank() }.toSet()
        sessionBlockedCategories = blockedCategories.filter { it.isNotBlank() }.toSet()
        prefs.edit().putBoolean("focus_session_active", true)
            .putStringSet("focus_session_domains", sessionBlockedDomains)
            .putStringSet("focus_session_categories", sessionBlockedCategories).apply()
        scope.launch {
            reconcileActiveRules()
        }
    }

    fun onFocusSessionEnded() {
        isSessionActive = false
        sessionBlockedDomains = emptySet()
        sessionBlockedCategories = emptySet()
        prefs.edit().putBoolean("focus_session_active", false)
            .remove("focus_session_domains").remove("focus_session_categories").apply()
        scope.launch {
            reconcileActiveRules()
        }
    }

    fun startWebsiteShieldManual() {
        togglePermanentProtection(true)
    }

    fun stopWebsiteShieldManual() {
        togglePermanentProtection(false)
        isSessionActive = false
        sessionBlockedDomains = emptySet()
        sessionBlockedCategories = emptySet()
        prefs.edit().putBoolean("focus_session_active", false)
            .remove("focus_session_domains").remove("focus_session_categories").apply()
        PrimeVpnService.stopService(context)
        _isVpnRunning.value = false
        checkpointBlockingDuration()
    }
}
