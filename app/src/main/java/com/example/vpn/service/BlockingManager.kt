package com.example.vpn.service

import android.content.Context
import android.content.SharedPreferences
import com.example.service.OpalAccessibilityService
import com.example.vpn.repo.BlockingAnalyticsRepository
import com.example.vpn.repo.BlockingScheduleRepository
import com.example.vpn.repo.BlocklistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockingManager private constructor(private val context: Context) {

    companion object {
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
    private val scope = CoroutineScope(Dispatchers.IO)

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
    private var isSessionActive = false
    private var sessionBlockedDomains = mutableSetOf<String>()
    private var sessionBlockedCategories = mutableSetOf<String>()

    init {
        scope.launch {
            reconcileActiveRules()
        }
    }

    fun updateVpnRunningState(running: Boolean) {
        _isVpnRunning.value = running
    }

    fun onVpnRevoked() {
        _isVpnRunning.value = false
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
        activeMap
    }

    suspend fun reconcileActiveRules() {
        val rules = resolveCurrentActiveRules()
        _activeRulesCount.value = rules.size

        // Should the VPN be running?
        val shouldRun = _isPermanentProtectionEnabled.value || isSessionActive || scheduleRepo.getCurrentlyActiveSchedules().isNotEmpty()

        if (shouldRun && !PrimeVpnService.isServiceRunning) {
            if (VpnPermissionManager.isVpnPrepared(context)) {
                PrimeVpnService.startService(context)
            }
        } else if (!shouldRun && PrimeVpnService.isServiceRunning) {
            PrimeVpnService.stopService(context)
        } else if (PrimeVpnService.isServiceRunning) {
            // Signal service to reload in-memory rules
            val intent = android.content.Intent(context, PrimeVpnService::class.java).apply {
                action = PrimeVpnService.ACTION_RELOAD_RULES
            }
            context.startService(intent)
        }
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
        sessionBlockedDomains.clear()
        sessionBlockedDomains.addAll(blockedDomains)
        sessionBlockedCategories.clear()
        sessionBlockedCategories.addAll(blockedCategories)
        scope.launch {
            reconcileActiveRules()
        }
    }

    fun onFocusSessionEnded() {
        isSessionActive = false
        sessionBlockedDomains.clear()
        sessionBlockedCategories.clear()
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
        sessionBlockedDomains.clear()
        sessionBlockedCategories.clear()
        PrimeVpnService.stopService(context)
        _isVpnRunning.value = false
    }
}
