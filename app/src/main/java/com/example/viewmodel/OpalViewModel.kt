package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSpecificMetric
import com.example.data.AppUsageData
import com.example.data.CategoryUsage
import com.example.data.DayUsagePoint
import com.example.data.HourlyPeakUsage
import com.example.data.OpalRepository
import com.example.data.WellbeingRepository
import com.example.model.BlockRule
import com.example.model.ClockPreferences
import com.example.model.BlockedAppItem
import com.example.model.NavigationTab
import com.example.model.OnboardingStep
import com.example.model.TimerPreset
import com.example.model.UserProfile
import com.example.model.PrimeBadge
import com.example.model.PrimeProgression
import com.example.model.PrimeRankTier
import com.example.model.PrimeScoreBreakdown
import com.example.model.PrimeTargets
import com.example.model.DailyQuest
import com.example.model.WeeklyMission
import com.example.data.PrimeAchievementRepository
import com.example.data.PrimeScoreCalculator
import com.example.ui.screens.BlockedAppActivity
import com.example.util.PermissionHelper
import com.example.alarm.AlarmSoundManager
import com.example.alarm.ClockTimerReceiver
import com.example.vpn.db.BlockedDomainEntity
import com.example.vpn.db.CategoryEntity
import com.example.vpn.db.BlockingScheduleEntity
import com.example.vpn.db.BlockedWebsiteEventEntity
import com.example.vpn.db.DomainAttemptCount
import com.example.vpn.db.CategoryAttemptCount
import com.example.vpn.service.BlockingManager
import com.example.vpn.service.VpnPermissionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.os.SystemClock
import com.example.alarm.AlarmSchedulerHelper
import com.example.alarm.AlarmSoundManager
import com.example.model.AlarmItem
import com.example.model.ChallengeConfig
import com.example.model.PomodoroSessionType
import com.example.model.PomodoroSettings
import com.example.model.PomodoroState
import com.example.model.RingtoneOption
import com.example.model.StopwatchLap
import com.example.model.StopwatchState

enum class TimerToolSubscreen {
    NONE,
    POMODORO,
    STOPWATCH,
    ALARM
}

data class OpalUiState(
    val currentTab: NavigationTab = NavigationTab.HOME,
    val profile: UserProfile = UserProfile(),
    val apps: List<BlockedAppItem> = emptyList(),
    val rules: List<BlockRule> = emptyList(),
    val presets: List<TimerPreset> = emptyList(),
    // Standard Timer state
    val timerSelectedMinutes: Int = 30,
    val isTimerRunning: Boolean = false,
    val isTimerPaused: Boolean = false,
    val timerRemainingSeconds: Int = 30 * 60,
    val timerBlockAppsEnabled: Boolean = true,
    val timerSessionBlockedAppIds: Set<String> = emptySet(),
    val isTimerBlockAppsSheetOpen: Boolean = false,
    val activePresetTitle: String? = null,
    val activePreset: TimerPreset? = null,
    // Timer Tools Subscreens
    val activeTimerTool: TimerToolSubscreen = TimerToolSubscreen.NONE,
    val alarms: List<AlarmItem> = emptyList(),
    val clockPreferences: ClockPreferences = ClockPreferences(),
    val pomodoroSettings: PomodoroSettings = PomodoroSettings(),
    val pomodoroState: PomodoroState = PomodoroState(),
    val stopwatchState: StopwatchState = StopwatchState(),
    val isPomodoroSettingsOpen: Boolean = false,
    val editingAlarm: AlarmItem? = null,
    val isCreatingAlarm: Boolean = false,
    val isRingtonePickerOpen: Boolean = false,
    val testingChallengeConfig: ChallengeConfig? = null,
    // Sheet / Dialog states
    val isSettingsOpen: Boolean = false,
    val isProModalOpen: Boolean = false,
    val isQuickFocusSheetOpen: Boolean = false,
    val viewingRuleDetail: BlockRule? = null,
    val editingRule: BlockRule? = null,
    val isSelectAppsSheetOpen: Boolean = false,
    val isAddWebsiteSheetOpen: Boolean = false,
    val selectedTimerPreset: TimerPreset? = null,
    val isCreatingRule: Boolean = false,
    val isReauthDialogOpen: Boolean = false,
    val isNeverAllowedUnlocked: Boolean = false,
    val activeCategorySheet: String? = null,
    // Digital Wellbeing screen states
    val isDigitalWellbeingOpen: Boolean = false,
    val isDailyDashboardOpen: Boolean = false,
    val isWeeklyReportOpen: Boolean = false,
    val viewingAppSpecificMetric: AppSpecificMetric? = null,
    val todayAppUsage: List<AppUsageData> = emptyList(),
    val todayCategoryUsage: List<CategoryUsage> = emptyList(),
    val weeklyAppUsage: List<AppUsageData> = emptyList(),
    val categoryUsage: List<CategoryUsage> = emptyList(),
    val hourlyPeakUsage: List<HourlyPeakUsage> = emptyList(),
    val dayUsagePoints: List<DayUsagePoint> = emptyList(),
    val weeklyHeatmap: List<List<Int>> = emptyList(),
    val wellbeingError: String? = null,
    val isWellbeingLoading: Boolean = false,
    // Onboarding flow state
    val isOnboarding: Boolean = false,
    val onboardingStep: OnboardingStep = OnboardingStep.WELCOME_ROCK,
    val onboardingName: String = "omk",
    val onboardingGoal: String = "Focus better",
    val onboardingScreenTimeEst: String = "5–7 hours",
    val overlayPermissionGranted: Boolean = false,
    val screenTimePermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = true,
    val accessibilityPermissionGranted: Boolean = false,
    val snackbarMessage: String? = null,
    // PRIME Scoring & Achievement Hub states
    val primeScoreBreakdown: PrimeScoreBreakdown = PrimeScoreBreakdown(),
    val primeTargets: PrimeTargets = PrimeTargets(),
    val primeProgression: PrimeProgression = PrimeProgression(),
    val badges: List<PrimeBadge> = emptyList(),
    val dailyQuests: List<DailyQuest> = emptyList(),
    val weeklyMissions: List<WeeklyMission> = emptyList(),
    val isAchievementDashboardOpen: Boolean = false,
    val isTargetSettingsOpen: Boolean = false,
    val isScoreFormulaOpen: Boolean = false,
    val selectedBadgeForDetail: PrimeBadge? = null,
    // Website Blocker & Local VPN states
    val isWebsiteBlockerDashboardOpen: Boolean = false,
    val isWebsiteSelectionScreenOpen: Boolean = false,
    val isCustomBlocklistScreenOpen: Boolean = false,
    val isBlockingScheduleScreenOpen: Boolean = false,
    val isBlockingAnalyticsScreenOpen: Boolean = false,
    val isVpnRunning: Boolean = false,
    val isPermanentWebsiteBlockingEnabled: Boolean = false,
    val hasVpnPermission: Boolean = false,
    val activeWebsiteRulesCount: Int = 0,
    val todayBlockedWebsitesCount: Int = 0,
    val totalBlockedWebsitesCount: Int = 0,
    val uniqueBlockedWebsitesCount: Int = 0,
    val websiteCategories: List<CategoryEntity> = emptyList(),
    val blockedDomainsList: List<BlockedDomainEntity> = emptyList(),
    val blockingSchedulesList: List<BlockingScheduleEntity> = emptyList(),
    val topBlockedWebsitesList: List<DomainAttemptCount> = emptyList(),
    val websiteCategoryDistribution: List<CategoryAttemptCount> = emptyList(),
    val recentBlockedEventsList: List<BlockedWebsiteEventEntity> = emptyList()
)

class OpalViewModel(
    val repository: OpalRepository = OpalRepository(),
    private val context: Context? = null
) : ViewModel() {

    private val wellbeingRepo: WellbeingRepository? = context?.let { WellbeingRepository(it) }
    val achievementRepo: PrimeAchievementRepository = PrimeAchievementRepository(context)
    val blockingManager: BlockingManager? = context?.let { BlockingManager.getInstance(it) }

    private val _uiState = MutableStateFlow(
        OpalUiState(
            apps = repository.appsFlow.value,
            rules = repository.rulesFlow.value,
            presets = repository.presetsFlow.value,
            profile = repository.profileFlow.value,
            alarms = repository.alarmsFlow.value,
            clockPreferences = repository.clockPreferencesFlow.value,
            timerRemainingSeconds = repository.getSavedStandardTimerState().first,
            timerSelectedMinutes = repository.getSavedStandardTimerState().second,
            isTimerRunning = repository.getSavedStandardTimerState().third,
            isTimerPaused = repository.getSavedStandardTimerPaused(),
            stopwatchState = run { val saved = repository.getSavedStopwatchState(); StopwatchState(isRunning = saved.second, elapsedMillis = saved.first, laps = repository.stopwatchLapsFlow.value) },
            pomodoroSettings = repository.pomodoroSettingsFlow.value,
            pomodoroState = repository.pomodoroStateFlow.value,
            isOnboarding = !repository.isRealOnboardingCompleted(),
            overlayPermissionGranted = context?.let { PermissionHelper.hasOverlayPermission(it) } ?: false,
            screenTimePermissionGranted = context?.let { PermissionHelper.hasUsageAccessPermission(it) } ?: false,
            notificationPermissionGranted = context?.let { PermissionHelper.hasNotificationPermission(it) } ?: true,
            accessibilityPermissionGranted = context?.let { PermissionHelper.isAccessibilityPermissionGranted(it) } ?: false,
            primeTargets = achievementRepo.targetsFlow.value,
            badges = achievementRepo.badgesFlow.value,
            dailyQuests = achievementRepo.dailyQuestsFlow.value,
            weeklyMissions = achievementRepo.weeklyMissionsFlow.value,
            primeProgression = achievementRepo.progressionFlow.value,
            primeScoreBreakdown = computeCurrentPrimeScore(
                screenMillis = repository.profileFlow.value.screenTimeMillis,
                targets = achievementRepo.targetsFlow.value
            )
        )
    )
    private var timerJob: Job? = null
    private var pomodoroJob: Job? = null
    private var stopwatchJob: Job? = null
    private var stopwatchBaseElapsed: Long = 0L
    private var stopwatchStartTime: Long = 0L

    val uiState: StateFlow<OpalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appsFlow.collect { apps -> _uiState.update { it.copy(apps = apps) } }
        }
        viewModelScope.launch {
            repository.rulesFlow.collect { rules -> _uiState.update { it.copy(rules = rules) } }
        }
        viewModelScope.launch {
            repository.presetsFlow.collect { presets -> _uiState.update { it.copy(presets = presets) } }
        }
        viewModelScope.launch {
            repository.profileFlow.collect { profile ->
                val updatedScore = computeCurrentPrimeScore(profile.screenTimeMillis, _uiState.value.primeTargets)
                _uiState.update {
                    it.copy(
                        profile = profile.copy(score = updatedScore.totalScore),
                        primeScoreBreakdown = updatedScore
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.alarmsFlow.collect { alarms -> _uiState.update { it.copy(alarms = alarms) } }
        }
        viewModelScope.launch {
            repository.clockPreferencesFlow.collect { preferences ->
                AlarmSoundManager.isSessionSoundEnabled = preferences.sessionSoundsEnabled
                _uiState.update { it.copy(clockPreferences = preferences) }
            }
        }
        if (_uiState.value.isTimerRunning) {
            startRecoveredTimerLoop()
            context?.let { ctx ->
                ClockTimerReceiver.schedule(ctx, _uiState.value.timerRemainingSeconds * 1000L, "Focus timer complete", "Your focus session is finished. Take a mindful break.")
            }
        }
        if (_uiState.value.pomodoroState.isRunning && !_uiState.value.pomodoroState.isPaused) {
            launchPomodoroLoop()
            context?.let { ctx ->
                ClockTimerReceiver.schedule(
                    ctx,
                    _uiState.value.pomodoroState.remainingSeconds.coerceAtLeast(1) * 1000L,
                    "Pomodoro session complete",
                    "Your ${_uiState.value.pomodoroState.sessionType.displayName.lowercase()} session is finished.",
                    _uiState.value.clockPreferences.sessionSoundsEnabled
                )
            }
        }
        if (_uiState.value.stopwatchState.isRunning) {
            val saved = repository.getSavedStopwatchState()
            stopwatchBaseElapsed = saved.first
            stopwatchStartTime = System.currentTimeMillis()
            launchStopwatchLoop()
        }
        viewModelScope.launch {
            repository.pomodoroSettingsFlow.collect { settings -> _uiState.update { it.copy(pomodoroSettings = settings) } }
        }
        viewModelScope.launch {
            achievementRepo.targetsFlow.collect { targets ->
                _uiState.update { state ->
                    val updatedScore = computeCurrentPrimeScore(state.profile.screenTimeMillis, targets)
                    state.copy(
                        primeTargets = targets,
                        primeScoreBreakdown = updatedScore,
                        profile = state.profile.copy(score = updatedScore.totalScore)
                    )
                }
            }
        }
        viewModelScope.launch {
            achievementRepo.badgesFlow.collect { badges ->
                _uiState.update { it.copy(badges = badges) }
            }
        }
        viewModelScope.launch {
            achievementRepo.dailyQuestsFlow.collect { quests ->
                _uiState.update { it.copy(dailyQuests = quests) }
            }
        }
        viewModelScope.launch {
            achievementRepo.weeklyMissionsFlow.collect { missions ->
                _uiState.update { it.copy(weeklyMissions = missions) }
            }
        }
        viewModelScope.launch {
            achievementRepo.progressionFlow.collect { prog ->
                _uiState.update { it.copy(primeProgression = prog) }
            }
        }

        blockingManager?.let { manager ->
            viewModelScope.launch {
                manager.isVpnRunning.collect { running ->
                    _uiState.update { it.copy(isVpnRunning = running) }
                }
            }
            viewModelScope.launch {
                manager.isPermanentProtectionEnabled.collect { perm ->
                    _uiState.update { it.copy(isPermanentWebsiteBlockingEnabled = perm) }
                }
            }
            viewModelScope.launch {
                manager.activeRulesCount.collect { count ->
                    _uiState.update { it.copy(activeWebsiteRulesCount = count) }
                }
            }
            viewModelScope.launch {
                manager.analyticsRepo.getTodayBlockedCount().collect { count ->
                    _uiState.update { it.copy(todayBlockedWebsitesCount = count) }
                }
            }
            viewModelScope.launch {
                manager.analyticsRepo.getTotalBlockedCount().collect { count ->
                    _uiState.update { it.copy(totalBlockedWebsitesCount = count) }
                }
            }
            viewModelScope.launch {
                manager.analyticsRepo.getUniqueDomainsCount().collect { count ->
                    _uiState.update { it.copy(uniqueBlockedWebsitesCount = count) }
                }
            }
            viewModelScope.launch {
                manager.analyticsRepo.getTopBlockedDomains().collect { domains ->
                    _uiState.update { it.copy(topBlockedWebsitesList = domains) }
                }
            }
            viewModelScope.launch {
                manager.analyticsRepo.getCategoryDistribution().collect { cats ->
                    _uiState.update { it.copy(websiteCategoryDistribution = cats) }
                }
            }
            viewModelScope.launch {
                manager.analyticsRepo.getRecentEvents().collect { events ->
                    _uiState.update { it.copy(recentBlockedEventsList = events) }
                }
            }
            viewModelScope.launch {
                manager.blocklistRepo.getAllCategories().collect { cats ->
                    _uiState.update { it.copy(websiteCategories = cats) }
                }
            }
            viewModelScope.launch {
                manager.blocklistRepo.getAllDomains().collect { doms ->
                    _uiState.update { it.copy(blockedDomainsList = doms) }
                }
            }
            viewModelScope.launch {
                manager.scheduleRepo.getAllSchedules().collect { scheds ->
                    _uiState.update { it.copy(blockingSchedulesList = scheds) }
                }
            }
        }

        refreshPermissions()
        refreshWellbeingData()
    }

    fun refreshPermissions() {
        val ctx = context ?: return
        val overlay = PermissionHelper.hasOverlayPermission(ctx)
        val screenTime = PermissionHelper.hasUsageAccessPermission(ctx)
        val notif = PermissionHelper.hasNotificationPermission(ctx)
        val access = PermissionHelper.isAccessibilityPermissionGranted(ctx)
        val vpnPrepared = VpnPermissionManager.isVpnPrepared(ctx)

        val updatedScore = computeCurrentPrimeScore(
            screenMillis = _uiState.value.profile.screenTimeMillis,
            targets = _uiState.value.primeTargets,
            hasPermission = screenTime
        )

        _uiState.value = _uiState.value.copy(
            overlayPermissionGranted = overlay,
            screenTimePermissionGranted = screenTime,
            notificationPermissionGranted = notif,
            accessibilityPermissionGranted = access,
            hasVpnPermission = vpnPrepared,
            primeScoreBreakdown = updatedScore,
            profile = _uiState.value.profile.copy(score = updatedScore.totalScore)
        )

        if (access) {
            repository.syncAccessibilityBlockList()
            viewModelScope.launch {
                try {
                    blockingManager?.reconcileActiveRules()
                } catch (_: Exception) {}
            }
        }
    }

    fun refreshRealScreenTime() {
        refreshPermissions()
        refreshWellbeingData()
    }

    fun refreshWellbeingData() {
        val repo = wellbeingRepo ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isWellbeingLoading = true,
                    wellbeingError = null,
                    todayAppUsage = emptyList(),
                    todayCategoryUsage = emptyList(),
                    weeklyAppUsage = emptyList(),
                    categoryUsage = emptyList(),
                    hourlyPeakUsage = emptyList(),
                    dayUsagePoints = emptyList(),
                    weeklyHeatmap = emptyList()
                )
            }

            if (!repo.hasUsageStatsPermission()) {
                withContext(Dispatchers.IO) { repository.refreshRealScreenTime() }
                refreshAchievementProgress(screenMinutes = null)
                _uiState.update {
                    it.copy(
                        isWellbeingLoading = false,
                        screenTimePermissionGranted = false,
                        wellbeingError = "Usage Access permission is required to read device screen time and wellbeing analytics."
                    )
                }
                return@launch
            }

            // Keep the home/profile screen-time value in sync with the same refresh.
            val trackerResult = withContext(Dispatchers.IO) { repository.refreshRealScreenTime() }

            val todayAppsResult = repo.getTodayAppUsage()
            val weeklyAppsResult = repo.getWeeklyAppUsage()
            val hourlyResult = repo.getHourlyPeakUsageData()
            val trendResult = repo.get7DayWeeklyTrend()
            val heatmapResult = repo.getWeeklyHeatmapMatrix()

            val todayApps = todayAppsResult.getOrNull().orEmpty()
            val screenMinutes = todayAppsResult.getOrNull()?.sumOf { it.totalTimeMs }
                ?.div(60_000L)?.toInt()
            refreshAchievementProgress(screenMinutes)
            val weeklyApps = weeklyAppsResult.getOrNull().orEmpty()
            val todayCategories = repo.getCategoryWiseUsage(todayApps)
            val weeklyCategories = repo.getCategoryWiseUsage(weeklyApps)
            val hourly = hourlyResult.getOrNull().orEmpty()
            val trend = trendResult.getOrNull().orEmpty()
            val heatmap = heatmapResult.getOrNull().orEmpty()

            val errors = listOf(
                todayAppsResult.exceptionOrNull(),
                weeklyAppsResult.exceptionOrNull(),
                hourlyResult.exceptionOrNull(),
                trendResult.exceptionOrNull(),
                heatmapResult.exceptionOrNull()
            ).filterNotNull()

            val errorMessage = errors.firstOrNull()?.let { error ->
                error.message ?: "Android usage statistics could not be read."
            } ?: trackerResult?.errorMessage

            _uiState.update {
                it.copy(
                    isWellbeingLoading = false,
                    screenTimePermissionGranted = true,
                    wellbeingError = errorMessage,
                    todayAppUsage = todayApps,
                    todayCategoryUsage = todayCategories,
                    weeklyAppUsage = weeklyApps,
                    categoryUsage = weeklyCategories,
                    hourlyPeakUsage = hourly,
                    dayUsagePoints = trend,
                    weeklyHeatmap = heatmap
                )
            }
        }
    }

    private fun refreshAchievementProgress(screenMinutes: Int?) {
        achievementRepo.updateDailyProgress(
            screenMinutes = screenMinutes,
            completedPomodoroSessions = repository.getTodayPomodoroSessions(),
            blockingMinutes = repository.getTodayBlockingMinutes()
        )
        achievementRepo.updateWeeklyFocusProgress(repository.getWeekFocusSessions())
    }

    // Digital Wellbeing navigation
    fun openDigitalWellbeing() {
        refreshWellbeingData()
        _uiState.value = _uiState.value.copy(isDigitalWellbeingOpen = true)
    }

    fun closeDigitalWellbeing() {
        _uiState.value = _uiState.value.copy(
            isDigitalWellbeingOpen = false,
            isDailyDashboardOpen = false,
            isWeeklyReportOpen = false,
            viewingAppSpecificMetric = null
        )
    }

    fun openDailyDashboard() {
        refreshWellbeingData()
        _uiState.value = _uiState.value.copy(isDailyDashboardOpen = true)
    }

    fun closeDailyDashboard() {
        _uiState.value = _uiState.value.copy(isDailyDashboardOpen = false)
    }

    fun openWeeklyReport() {
        refreshWellbeingData()
        _uiState.value = _uiState.value.copy(isWeeklyReportOpen = true)
    }

    fun closeWeeklyReport() {
        _uiState.value = _uiState.value.copy(isWeeklyReportOpen = false)
    }

    fun openAppSpecificReport(packageName: String) {
        val repo = wellbeingRepo
        if (repo != null) {
            viewModelScope.launch {
                val metricResult = repo.getAppSpecificMetric(packageName)
                metricResult.onSuccess { metric ->
                    _uiState.value = _uiState.value.copy(viewingAppSpecificMetric = metric, wellbeingError = null)
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Error fetching app metrics: ${err.message}")
                }
            }
        }
    }

    fun closeAppSpecificReport() {
        _uiState.value = _uiState.value.copy(viewingAppSpecificMetric = null)
    }

    fun logDailyActivity() {
        repository.logActivityAndBoostStreak()
        _uiState.value = _uiState.value.copy(
            profile = repository.profileFlow.value,
            snackbarMessage = "Activity recorded! Your streak is alive."
        )
    }

    fun previewBlockOverlay(app: BlockedAppItem) {
        val ctx = context ?: return
        val pkg = if (app.packageName.isNotBlank()) app.packageName else "com.example.prime"
        BlockedAppActivity.start(ctx, pkg, app.name)
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    // Timer logic
    fun adjustTimerMinutes(deltaMinutes: Int) {
        if (_uiState.value.isTimerRunning) return
        val current = _uiState.value.timerSelectedMinutes
        val updated = (current + deltaMinutes).coerceIn(5, 180)
        _uiState.value = _uiState.value.copy(
            timerSelectedMinutes = updated,
            timerRemainingSeconds = updated * 60
        )
        repository.saveStandardTimerState(updated * 60, updated, false, false)
    }

    fun toggleTimerBlockApps() {
        _uiState.value = _uiState.value.copy(
            timerBlockAppsEnabled = !_uiState.value.timerBlockAppsEnabled
        )
    }

    fun openTimerBlockAppsSheet() {
        _uiState.value = _uiState.value.copy(isTimerBlockAppsSheetOpen = true)
    }

    fun closeTimerBlockAppsSheet() {
        _uiState.value = _uiState.value.copy(isTimerBlockAppsSheetOpen = false)
    }

    fun toggleTimerSessionApp(appId: String) {
        val current = _uiState.value.timerSessionBlockedAppIds.toMutableSet()
        if (current.contains(appId)) {
            current.remove(appId)
        } else {
            current.add(appId)
        }
        _uiState.value = _uiState.value.copy(
            timerSessionBlockedAppIds = current,
            timerBlockAppsEnabled = true
        )
    }

    fun selectAllTimerSessionApps() {
        val selectable = _uiState.value.apps.filter { !it.isBlocked }.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(
            timerSessionBlockedAppIds = selectable,
            timerBlockAppsEnabled = true
        )
    }

    fun clearTimerSessionApps() {
        _uiState.value = _uiState.value.copy(
            timerSessionBlockedAppIds = emptySet()
        )
    }

    fun startTimer(
        presetTitle: String? = null,
        customMinutes: Int? = null,
        preset: TimerPreset? = null
    ) {
        val resolvedPreset = preset ?: _uiState.value.presets.firstOrNull { it.title.equals(presetTitle, ignoreCase = true) }
        val title = resolvedPreset?.title ?: presetTitle
        val minutes = (customMinutes ?: (resolvedPreset?.durationMinutes ?: _uiState.value.timerSelectedMinutes)).coerceIn(1, 24 * 60)
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            timerSelectedMinutes = minutes,
            timerRemainingSeconds = minutes * 60,
            isTimerRunning = true,
            isTimerPaused = false,
            activePresetTitle = title,
            activePreset = resolvedPreset,
            isQuickFocusSheetOpen = false,
            selectedTimerPreset = null
        )

        repository.saveStandardTimerState(minutes * 60, minutes, true, false)
        context?.let { ClockTimerReceiver.schedule(it, minutes * 60_000L, "Focus timer complete", "Your focus session is finished. Take a mindful break.") }

        // Enforce session app blocking
        if (_uiState.value.timerBlockAppsEnabled) {
            val sessionAppIds = _uiState.value.timerSessionBlockedAppIds
            val sessionPackages = _uiState.value.apps
                .filter { sessionAppIds.contains(it.id) && !it.isWebsite && it.packageName.isNotBlank() }
                .map { it.packageName }
                .toSet()
            repository.applyTimerSessionAppBlock(sessionPackages)
        }

        // Activate website blocking shield during focus session
        blockingManager?.onFocusSessionStarted()

        context?.let { ctx ->
            PermissionHelper.sendPrimeNudgeNotification(
                ctx,
                "Focus Session Started",
                "Shield is active for ${minutes}m. Distracting apps and websites are blocked."
            )
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.isTimerRunning && _uiState.value.timerRemainingSeconds > 0) {
                delay(1000L)
                if (!_uiState.value.isTimerPaused) {
                    val remaining = _uiState.value.timerRemainingSeconds - 1
                    if (remaining <= 0) {
                        val completedMin = _uiState.value.timerSelectedMinutes
                        repository.addFocusMinutes(completedMin)
                        repository.clearStandardTimerState(_uiState.value.timerSelectedMinutes)
                        context?.let { ClockTimerReceiver.cancel(it) }
                        _uiState.value = _uiState.value.copy(
                            timerRemainingSeconds = 0,
                            isTimerRunning = false,
                            isTimerPaused = false,
                            activePresetTitle = null,
                            activePreset = null,
                            snackbarMessage = "Focus session completed! Great job."
                        )
                        // Automatically unblock session-blocked apps and restore website rules
                        repository.clearTimerSessionAppBlock()
                        blockingManager?.onFocusSessionEnded()
                        repository.logActivityAndBoostStreak()
                        val tracking = repository.refreshRealScreenTime()
                        refreshAchievementProgress(
                            if (tracking?.errorMessage == null && tracking != null) {
                                (tracking.totalScreenTimeMillis / 60_000L).toInt()
                            } else null
                        )
                        context?.let { ctx ->
                            PermissionHelper.sendPrimeNudgeNotification(
                                ctx,
                                "Session Completed",
                                "Well done! You successfully completed your focus block."
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(timerRemainingSeconds = remaining)
                        repository.saveStandardTimerState(remaining, _uiState.value.timerSelectedMinutes, true, false)
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        val paused = !_uiState.value.isTimerPaused
        _uiState.value = _uiState.value.copy(isTimerPaused = paused)
        context?.let { ctx ->
            if (paused) ClockTimerReceiver.cancel(ctx)
            else ClockTimerReceiver.schedule(ctx, _uiState.value.timerRemainingSeconds * 1000L, "Focus timer complete", "Your focus session is finished. Take a mindful break.")
        }
        repository.saveStandardTimerState(
            _uiState.value.timerRemainingSeconds,
            _uiState.value.timerSelectedMinutes,
            _uiState.value.isTimerRunning,
            paused
        )
    }

    fun stopTimer() {
        timerJob?.cancel()
        repository.clearStandardTimerState(_uiState.value.timerSelectedMinutes)
        context?.let { ClockTimerReceiver.cancel(it) }
        // Automatically unblock session-blocked apps and websites when session is stopped
        repository.clearTimerSessionAppBlock()
        blockingManager?.onFocusSessionEnded()
        _uiState.value = _uiState.value.copy(
            isTimerRunning = false,
            isTimerPaused = false,
            timerRemainingSeconds = _uiState.value.timerSelectedMinutes * 60,
            activePresetTitle = null,
            activePreset = null
        )
    }

    private fun startRecoveredTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isTimerRunning && _uiState.value.timerRemainingSeconds > 0) {
                delay(1000L)
                if (!_uiState.value.isTimerPaused) {
                    val remaining = repository.getSavedStandardTimerState().first
                    if (remaining <= 0) {
                        repository.clearStandardTimerState(_uiState.value.timerSelectedMinutes)
                        context?.let { ClockTimerReceiver.cancel(it) }
                        repository.clearTimerSessionAppBlock()
                        blockingManager?.onFocusSessionEnded()
                        _uiState.value = _uiState.value.copy(
                            isTimerRunning = false,
                            isTimerPaused = false,
                            timerRemainingSeconds = 0,
                            activePresetTitle = null,
                            activePreset = null,
                            snackbarMessage = "Focus session completed!"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(timerRemainingSeconds = remaining)
                    }
                }
            }
        }
    }

    fun saveClockPreferences(preferences: ClockPreferences) {
        repository.saveClockPreferences(preferences)
        AlarmSoundManager.isSessionSoundEnabled = preferences.sessionSoundsEnabled
        _uiState.value = _uiState.value.copy(
            clockPreferences = preferences,
            snackbarMessage = "Clock settings saved"
        )
    }

    // ==================== TIMER TOOLS SUBSCREEN NAVIGATION ====================

    fun openTimerTool(tool: TimerToolSubscreen) {
        _uiState.value = _uiState.value.copy(activeTimerTool = tool)
    }

    fun closeTimerTool() {
        _uiState.value = _uiState.value.copy(
            activeTimerTool = TimerToolSubscreen.NONE,
            isPomodoroSettingsOpen = false,
            isCreatingAlarm = false,
            editingAlarm = null,
            isRingtonePickerOpen = false,
            testingChallengeConfig = null
        )
    }

    // ==================== POMODORO CONTROLS ====================

    fun openPomodoroSettings() {
        _uiState.value = _uiState.value.copy(isPomodoroSettingsOpen = true)
    }

    fun closePomodoroSettings() {
        _uiState.value = _uiState.value.copy(isPomodoroSettingsOpen = false)
    }

    fun savePomodoroSettings(requestedSettings: PomodoroSettings) {
        val settings = requestedSettings.copy(
            focusMinutes = requestedSettings.focusMinutes.coerceIn(1, 180),
            shortBreakMinutes = requestedSettings.shortBreakMinutes.coerceIn(1, 60),
            longBreakMinutes = requestedSettings.longBreakMinutes.coerceIn(1, 120),
            roundsPerCycle = requestedSettings.roundsPerCycle.coerceIn(1, 12),
            totalCycles = requestedSettings.totalCycles.coerceIn(1, 20)
        )
        repository.updatePomodoroSettings(settings)
        val currentState = _uiState.value.pomodoroState
        val newDuration = if (!currentState.isRunning) {
            when (currentState.sessionType) {
                PomodoroSessionType.FOCUS -> settings.focusMinutes * 60
                PomodoroSessionType.SHORT_BREAK -> settings.shortBreakMinutes * 60
                PomodoroSessionType.LONG_BREAK -> settings.longBreakMinutes * 60
            }
        } else currentState.remainingSeconds

        val updatedState = currentState.copy(
            remainingSeconds = newDuration,
            totalSessionSeconds = if (!currentState.isRunning) newDuration else currentState.totalSessionSeconds
        )
        repository.savePomodoroState(updatedState)
        _uiState.value = _uiState.value.copy(
            pomodoroSettings = settings,
            pomodoroState = updatedState,
            isPomodoroSettingsOpen = false,
            snackbarMessage = "Pomodoro configuration saved"
        )
    }

    fun applyPomodoroQuickPreset(focusMin: Int, breakMin: Int) {
        val current = _uiState.value.pomodoroSettings
        val updated = current.copy(focusMinutes = focusMin, shortBreakMinutes = breakMin)
        savePomodoroSettings(updated)
    }

    fun startPomodoro() {
        val settings = _uiState.value.pomodoroSettings
        val current = _uiState.value.pomodoroState
        val targetSeconds = if (current.remainingSeconds > 0) current.remainingSeconds else settings.focusMinutes * 60

        val runningState = current.copy(
            isRunning = true,
            isPaused = false,
            remainingSeconds = targetSeconds,
            totalSessionSeconds = targetSeconds
        )
        _uiState.value = _uiState.value.copy(pomodoroState = runningState)
        repository.savePomodoroState(runningState)
        context?.let { ClockTimerReceiver.schedule(it, targetSeconds * 1000L, "Pomodoro session complete", "Your ${current.sessionType.displayName.lowercase()} session is finished.", _uiState.value.clockPreferences.sessionSoundsEnabled) }

        launchPomodoroLoop()
    }

    fun pausePomodoro() {
        val updated = _uiState.value.pomodoroState.copy(isPaused = true)
        _uiState.value = _uiState.value.copy(pomodoroState = updated)
        repository.savePomodoroState(updated)
        context?.let { ClockTimerReceiver.cancel(it) }
    }

    fun resumePomodoro() {
        val updated = _uiState.value.pomodoroState.copy(isPaused = false)
        _uiState.value = _uiState.value.copy(pomodoroState = updated)
        repository.savePomodoroState(updated)
        context?.let { ClockTimerReceiver.schedule(it, updated.remainingSeconds * 1000L, "Pomodoro session complete", "Your ${updated.sessionType.displayName.lowercase()} session is finished.", _uiState.value.clockPreferences.sessionSoundsEnabled) }
        launchPomodoroLoop()
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        context?.let { ClockTimerReceiver.cancel(it) }
        val settings = _uiState.value.pomodoroSettings
        val resetState = PomodoroState(
            sessionType = PomodoroSessionType.FOCUS,
            currentRound = 1,
            currentCycle = 1,
            remainingSeconds = settings.focusMinutes * 60,
            totalSessionSeconds = settings.focusMinutes * 60,
            isRunning = false,
            isPaused = false,
            completedRoundsCount = 0
        )
        _uiState.value = _uiState.value.copy(pomodoroState = resetState)
        repository.savePomodoroState(resetState)
    }

    fun skipPomodoroSession() {
        transitionToNextPomodoroSession()
    }

    private fun launchPomodoroLoop() {
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            while (_uiState.value.pomodoroState.isRunning) {
                delay(1000L)
                val curr = _uiState.value.pomodoroState
                if (!curr.isPaused && curr.isRunning) {
                    val nextRemaining = curr.remainingSeconds - 1
                    if (nextRemaining <= 0) {
                        transitionToNextPomodoroSession(completedNaturally = true)
                    } else {
                        val updated = curr.copy(remainingSeconds = nextRemaining)
                        _uiState.value = _uiState.value.copy(pomodoroState = updated)
                        repository.savePomodoroState(updated)
                    }
                }
            }
        }
    }

    private fun transitionToNextPomodoroSession(completedNaturally: Boolean = false) {
        AlarmSoundManager.playSessionTransitionChime()
        val settings = _uiState.value.pomodoroSettings
        val curr = _uiState.value.pomodoroState

        when (curr.sessionType) {
            PomodoroSessionType.FOCUS -> {
                if (completedNaturally) {
                    repository.addFocusMinutes(settings.focusMinutes, isPomodoro = true)
                    repository.logActivityAndBoostStreak()
                    val tracking = repository.refreshRealScreenTime()
                    refreshAchievementProgress(
                        if (tracking?.errorMessage == null && tracking != null) {
                            (tracking.totalScreenTimeMillis / 60_000L).toInt()
                        } else null
                    )
                }
                val nextRounds = curr.completedRoundsCount + if (completedNaturally) 1 else 0
                if (completedNaturally && curr.currentRound >= settings.roundsPerCycle) {
                    val longBreakSec = settings.longBreakMinutes * 60
                    val nextState = curr.copy(
                        sessionType = PomodoroSessionType.LONG_BREAK,
                        remainingSeconds = longBreakSec,
                        totalSessionSeconds = longBreakSec,
                        completedRoundsCount = nextRounds
                    )
                    _uiState.value = _uiState.value.copy(
                        pomodoroState = nextState,
                        snackbarMessage = "Cycle complete! Time for a ${settings.longBreakMinutes}m Long Break."
                    )
                    repository.savePomodoroState(nextState)
                } else {
                    val shortBreakSec = settings.shortBreakMinutes * 60
                    val nextState = curr.copy(
                        sessionType = PomodoroSessionType.SHORT_BREAK,
                        remainingSeconds = shortBreakSec,
                        totalSessionSeconds = shortBreakSec,
                        completedRoundsCount = nextRounds
                    )
                    _uiState.value = _uiState.value.copy(
                        pomodoroState = nextState,
                        snackbarMessage = "Focus round finished! Take a ${settings.shortBreakMinutes}m Short Break."
                    )
                    repository.savePomodoroState(nextState)
                }
            }
            PomodoroSessionType.SHORT_BREAK -> {
                val nextRound = curr.currentRound + 1
                val focusSec = settings.focusMinutes * 60
                val nextState = curr.copy(
                    sessionType = PomodoroSessionType.FOCUS,
                    currentRound = nextRound,
                    remainingSeconds = focusSec,
                    totalSessionSeconds = focusSec
                )
                _uiState.value = _uiState.value.copy(
                    pomodoroState = nextState,
                    snackbarMessage = "Break over! Starting Focus Round $nextRound."
                )
                repository.savePomodoroState(nextState)
            }
            PomodoroSessionType.LONG_BREAK -> {
                if (curr.currentCycle < settings.totalCycles) {
                    val nextCycle = curr.currentCycle + 1
                    val focusSec = settings.focusMinutes * 60
                    val nextState = curr.copy(
                        sessionType = PomodoroSessionType.FOCUS,
                        currentRound = 1,
                        currentCycle = nextCycle,
                        remainingSeconds = focusSec,
                        totalSessionSeconds = focusSec
                    )
                    _uiState.value = _uiState.value.copy(
                        pomodoroState = nextState,
                        snackbarMessage = "Starting Pomodoro Cycle $nextCycle!"
                    )
                    repository.savePomodoroState(nextState)
                } else {
                    val focusSec = settings.focusMinutes * 60
                    val finishedState = curr.copy(
                        sessionType = PomodoroSessionType.FOCUS,
                        currentRound = 1,
                        currentCycle = 1,
                        remainingSeconds = focusSec,
                        totalSessionSeconds = focusSec,
                        isRunning = false,
                        isPaused = false
                    )
                    pomodoroJob?.cancel()
                    _uiState.value = _uiState.value.copy(
                        pomodoroState = finishedState,
                        snackbarMessage = "Congratulations! All ${settings.totalCycles} Pomodoro cycle(s) completed."
                    )
                    repository.savePomodoroState(finishedState)
                }
            }
        }
        val nextPomodoro = _uiState.value.pomodoroState
        context?.let { ctx ->
            if (nextPomodoro.isRunning && !nextPomodoro.isPaused) {
                ClockTimerReceiver.schedule(
                    ctx,
                    nextPomodoro.remainingSeconds.coerceAtLeast(1) * 1000L,
                    "Pomodoro session complete",
                    "Your ${nextPomodoro.sessionType.displayName.lowercase()} session is finished.",
                    _uiState.value.clockPreferences.sessionSoundsEnabled
                )
            } else {
                ClockTimerReceiver.cancel(ctx)
            }
        }
    }

    // ==================== STOPWATCH CONTROLS ===================

    fun startStopwatch() {
        stopwatchStartTime = System.currentTimeMillis()
        stopwatchBaseElapsed = 0L
        repository.saveStopwatchState(0L, true)
        repository.saveStopwatchLaps(emptyList())
        _uiState.value = _uiState.value.copy(
            stopwatchState = _uiState.value.stopwatchState.copy(
                isRunning = true,
                elapsedMillis = 0L,
                laps = emptyList()
            )
        )
        launchStopwatchLoop()
    }

    fun pauseStopwatch() {
        stopwatchJob?.cancel()
        val now = System.currentTimeMillis()
        val totalElapsed = stopwatchBaseElapsed + (now - stopwatchStartTime)
        stopwatchBaseElapsed = totalElapsed
        repository.saveStopwatchState(totalElapsed, false)
        _uiState.value = _uiState.value.copy(
            stopwatchState = _uiState.value.stopwatchState.copy(
                isRunning = false,
                elapsedMillis = totalElapsed
            )
        )
    }

    fun resumeStopwatch() {
        stopwatchStartTime = System.currentTimeMillis()
        repository.saveStopwatchState(stopwatchBaseElapsed, true)
        _uiState.value = _uiState.value.copy(
            stopwatchState = _uiState.value.stopwatchState.copy(isRunning = true)
        )
        launchStopwatchLoop()
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        stopwatchBaseElapsed = 0L
        repository.saveStopwatchState(0L, false)
        val resetState = StopwatchState(
            isRunning = false,
            elapsedMillis = 0L,
            laps = emptyList()
        )
        _uiState.value = _uiState.value.copy(stopwatchState = resetState)
        repository.saveStopwatchLaps(emptyList())
    }

    fun recordLap() {
        val currentLaps = _uiState.value.stopwatchState.laps
        val totalElapsed = _uiState.value.stopwatchState.elapsedMillis
        val previousTotal = currentLaps.firstOrNull()?.totalTimeMillis ?: 0L
        val lapDuration = (totalElapsed - previousTotal).coerceAtLeast(0L)
        val newLap = StopwatchLap(
            lapNumber = currentLaps.size + 1,
            lapTimeMillis = lapDuration,
            totalTimeMillis = totalElapsed
        )
        val updatedLaps = listOf(newLap) + currentLaps
        _uiState.value = _uiState.value.copy(
            stopwatchState = _uiState.value.stopwatchState.copy(laps = updatedLaps)
        )
        repository.saveStopwatchLaps(updatedLaps)
    }

    private fun launchStopwatchLoop() {
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (_uiState.value.stopwatchState.isRunning) {
                delay(30L)
                val currentElapsed = stopwatchBaseElapsed + (System.currentTimeMillis() - stopwatchStartTime)
                _uiState.value = _uiState.value.copy(
                    stopwatchState = _uiState.value.stopwatchState.copy(elapsedMillis = currentElapsed)
                )
            }
        }
    }

    // ==================== ALARM ACTIONS ====================

    fun openCreateAlarm() {
        val defaults = _uiState.value.clockPreferences
        val blankAlarm = AlarmItem(
            id = "alarm_${System.currentTimeMillis()}",
            hour = 7,
            minute = 0,
            name = "Morning Alarm",
            isEnabled = true,
            ringtoneId = defaults.defaultRingtoneId,
            ringtoneName = defaults.defaultRingtoneName,
            ringtoneUriString = defaults.defaultRingtoneUriString,
            vibrationEnabled = defaults.alarmVibration
        )
        _uiState.value = _uiState.value.copy(
            isCreatingAlarm = true,
            editingAlarm = blankAlarm
        )
    }

    fun openEditAlarm(alarm: AlarmItem) {
        _uiState.value = _uiState.value.copy(
            isCreatingAlarm = false,
            editingAlarm = alarm
        )
    }

    fun closeAlarmEdit() {
        _uiState.value = _uiState.value.copy(
            isCreatingAlarm = false,
            editingAlarm = null,
            isRingtonePickerOpen = false,
            testingChallengeConfig = null
        )
    }

    fun saveAlarm(alarm: AlarmItem) {
        repository.saveAlarm(alarm.copy(
            hour = alarm.hour.coerceIn(0, 23),
            minute = alarm.minute.coerceIn(0, 59),
            name = alarm.name.trim().take(40).ifBlank { "Alarm" },
            snoozeDurationMinutes = alarm.snoozeDurationMinutes.coerceIn(1, 30),
            selectedDays = alarm.selectedDays.filter { it in 1..7 }.toSet().ifEmpty { setOf(1, 2, 3, 4, 5) },
            challengeConfig = alarm.challengeConfig.copy(requiredConsecutive = alarm.challengeConfig.requiredConsecutive.coerceIn(1, 5))
        ))
        _uiState.value = _uiState.value.copy(
            isCreatingAlarm = false,
            editingAlarm = null,
            isRingtonePickerOpen = false,
            testingChallengeConfig = null,
            snackbarMessage = "Alarm \"${alarm.name}\" saved"
        )
    }

    fun deleteAlarm(alarmId: String) {
        repository.deleteAlarm(alarmId)
        _uiState.value = _uiState.value.copy(
            isCreatingAlarm = false,
            editingAlarm = null,
            snackbarMessage = "Alarm deleted"
        )
    }

    fun toggleAlarm(alarmId: String, isEnabled: Boolean) {
        repository.toggleAlarm(alarmId, isEnabled)
        _uiState.value = _uiState.value.copy(
            snackbarMessage = if (isEnabled) "Alarm turned on" else "Alarm turned off"
        )
    }

    fun openRingtonePicker() {
        _uiState.value = _uiState.value.copy(isRingtonePickerOpen = true)
    }

    fun closeRingtonePicker() {
        _uiState.value = _uiState.value.copy(isRingtonePickerOpen = false)
    }

    fun selectRingtone(option: RingtoneOption) {
        val currentEdit = _uiState.value.editingAlarm ?: return
        val updated = currentEdit.copy(
            ringtoneId = option.id,
            ringtoneName = option.title,
            ringtoneUriString = option.uriString
        )
        _uiState.value = _uiState.value.copy(
            editingAlarm = updated,
            isRingtonePickerOpen = false
        )
    }

    fun testChallenge(config: ChallengeConfig) {
        _uiState.value = _uiState.value.copy(testingChallengeConfig = config)
    }

    fun closeTestChallenge() {
        _uiState.value = _uiState.value.copy(testingChallengeConfig = null)
    }

    // Sheets & Dialogs
    fun openSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = false)
    }

    fun openProModal() {
        _uiState.value = _uiState.value.copy(isProModalOpen = true)
    }

    fun closeProModal() {
        _uiState.value = _uiState.value.copy(isProModalOpen = false)
    }

    fun upgradeToPro() {
        repository.togglePro(true)
        _uiState.value = _uiState.value.copy(
            isProModalOpen = false,
            snackbarMessage = "Welcome to Prime PRO! All features unlocked."
        )
    }

    fun openQuickFocusSheet() {
        _uiState.value = _uiState.value.copy(isQuickFocusSheetOpen = true)
    }

    fun closeQuickFocusSheet() {
        _uiState.value = _uiState.value.copy(isQuickFocusSheetOpen = false)
    }

    fun viewRuleDetail(rule: BlockRule) {
        _uiState.value = _uiState.value.copy(viewingRuleDetail = rule)
    }

    fun closeRuleDetail() {
        _uiState.value = _uiState.value.copy(viewingRuleDetail = null)
    }

    fun editRule(rule: BlockRule) {
        _uiState.value = _uiState.value.copy(
            viewingRuleDetail = null,
            editingRule = rule
        )
    }

    fun closeEditRule() {
        _uiState.value = _uiState.value.copy(editingRule = null)
    }

    fun openCreateRuleSheet() {
        _uiState.value = _uiState.value.copy(isCreatingRule = true)
    }

    fun closeCreateRuleSheet() {
        _uiState.value = _uiState.value.copy(isCreatingRule = false)
    }

    fun createCustomRule(rule: BlockRule) {
        repository.addCustomRule(rule)
        _uiState.value = _uiState.value.copy(
            isCreatingRule = false,
            snackbarMessage = "Custom Shield \"${rule.title}\" created!"
        )
    }

    fun deleteRule(ruleId: String) {
        repository.deleteRule(ruleId)
        _uiState.value = _uiState.value.copy(
            viewingRuleDetail = null,
            editingRule = null,
            snackbarMessage = "Shield removed"
        )
    }

    fun openCategorySheet(categoryName: String) {
        if (categoryName == "Never allowed" && !_uiState.value.isNeverAllowedUnlocked) {
            _uiState.value = _uiState.value.copy(isReauthDialogOpen = true)
        } else {
            _uiState.value = _uiState.value.copy(activeCategorySheet = categoryName)
        }
    }

    fun closeCategorySheet() {
        _uiState.value = _uiState.value.copy(activeCategorySheet = null)
    }

    fun onReauthSuccess() {
        _uiState.value = _uiState.value.copy(
            isReauthDialogOpen = false,
            isNeverAllowedUnlocked = true,
            activeCategorySheet = "Never allowed",
            snackbarMessage = "Re-authentication verified. Never Allowed unlocked."
        )
    }

    fun onReauthFailure() {
        _uiState.value = _uiState.value.copy(
            isReauthDialogOpen = false,
            snackbarMessage = "Authentication failed or cancelled."
        )
    }

    fun dismissReauthDialog() {
        _uiState.value = _uiState.value.copy(isReauthDialogOpen = false)
    }

    fun setAppCategory(appId: String, newCategory: String) {
        repository.setAppCategory(appId, newCategory)
        _uiState.value = _uiState.value.copy(apps = repository.appsFlow.value)
    }

    fun saveRule(rule: BlockRule) {
        repository.updateRule(rule)
        _uiState.value = _uiState.value.copy(
            editingRule = null,
            snackbarMessage = "Shield \"${rule.title}\" saved"
        )
    }

    fun toggleRulePause(ruleId: String) {
        repository.toggleRulePause(ruleId)
        val currentRule = _uiState.value.rules.find { it.id == ruleId }
        val isNowPaused = !(currentRule?.isPaused ?: false)
        _uiState.value = _uiState.value.copy(
            viewingRuleDetail = _uiState.value.viewingRuleDetail?.copy(isPaused = isNowPaused),
            editingRule = _uiState.value.editingRule?.copy(isPaused = isNowPaused),
            snackbarMessage = if (isNowPaused) "Shield paused" else "Shield resumed"
        )
    }

    fun openSelectAppsSheet() {
        _uiState.value = _uiState.value.copy(isSelectAppsSheetOpen = true)
    }

    fun closeSelectAppsSheet() {
        _uiState.value = _uiState.value.copy(isSelectAppsSheetOpen = false)
    }

    fun openAddWebsiteSheet() {
        _uiState.value = _uiState.value.copy(isAddWebsiteSheetOpen = true)
    }

    fun closeAddWebsiteSheet() {
        _uiState.value = _uiState.value.copy(isAddWebsiteSheetOpen = false)
    }

    fun addWebsite(domain: String) {
        repository.addCustomWebsite(domain)
        _uiState.value = _uiState.value.copy(
            isAddWebsiteSheetOpen = false,
            snackbarMessage = "Added $domain"
        )
    }

    fun removeBlockedItem(itemId: String) {
        repository.removeBlockedApp(itemId)
        _uiState.value = _uiState.value.copy(apps = repository.appsFlow.value)
    }

    fun toggleItemBlocked(itemId: String) {
        repository.toggleAppBlocked(itemId)
        _uiState.value = _uiState.value.copy(apps = repository.appsFlow.value)
    }

    fun selectTimerPreset(preset: TimerPreset) {
        _uiState.value = _uiState.value.copy(selectedTimerPreset = preset)
    }

    fun closeTimerPreset() {
        _uiState.value = _uiState.value.copy(selectedTimerPreset = null)
    }

    fun useEmergencyPass() {
        val success = repository.useEmergencyPass()
        if (success) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Emergency pass activated. 15m unblock granted.")
        } else {
            _uiState.value = _uiState.value.copy(snackbarMessage = "No emergency passes remaining.")
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    // Onboarding navigation
    fun startOnboarding() {
        _uiState.value = _uiState.value.copy(
            isOnboarding = true,
            onboardingStep = OnboardingStep.WELCOME_ROCK,
            isSettingsOpen = false
        )
    }

    fun resetOnboarding() {
        repository.setOnboardingCompleted(false)
        startOnboarding()
    }

    fun setOnboardingName(name: String) {
        _uiState.value = _uiState.value.copy(onboardingName = name)
        repository.updateUserName(name)
    }

    fun setOnboardingGoal(goal: String) {
        _uiState.value = _uiState.value.copy(onboardingGoal = goal)
    }

    fun setOnboardingScreenTime(est: String) {
        _uiState.value = _uiState.value.copy(onboardingScreenTimeEst = est)
    }

    fun toggleOverlayPermission() {
        _uiState.value = _uiState.value.copy(
            overlayPermissionGranted = !_uiState.value.overlayPermissionGranted
        )
    }

    fun toggleScreenTimePermission() {
        _uiState.value = _uiState.value.copy(
            screenTimePermissionGranted = !_uiState.value.screenTimePermissionGranted
        )
    }

    fun nextOnboardingStep() {
        val current = _uiState.value.onboardingStep
        val values = OnboardingStep.values()
        val nextIndex = current.ordinal + 1
        if (nextIndex < values.size) {
            _uiState.value = _uiState.value.copy(onboardingStep = values[nextIndex])
        } else {
            completeOnboarding()
        }
    }

    fun previousOnboardingStep() {
        val current = _uiState.value.onboardingStep
        val values = OnboardingStep.values()
        val prevIndex = current.ordinal - 1
        if (prevIndex >= 0) {
            _uiState.value = _uiState.value.copy(onboardingStep = values[prevIndex])
        } else {
            _uiState.value = _uiState.value.copy(isOnboarding = false)
        }
    }

    fun completeOnboarding() {
        repository.setOnboardingCompleted(true)
        repository.logActivityAndBoostStreak()
        _uiState.value = _uiState.value.copy(
            isOnboarding = false,
            currentTab = NavigationTab.HOME,
            snackbarMessage = "Welcome to Prime! Become Your Prime."
        )
        context?.let { ctx ->
            PermissionHelper.sendPrimeNudgeNotification(
                ctx,
                "Prime Focus Shield Activated",
                "Prime is actively protecting your focus and tracking screen time."
            )
        }
    }

    // PRIME Scoring & Achievement Methods
    fun computeCurrentPrimeScore(
        screenMillis: Long,
        targets: PrimeTargets,
        hasPermission: Boolean = context?.let { PermissionHelper.hasUsageAccessPermission(it) } ?: false
    ): PrimeScoreBreakdown {
        if (!hasPermission) {
            return PrimeScoreBreakdown(
                screenTimeScore = 0f,
                focusScore = 0f,
                blockingScore = 0f,
                totalScore = 0,
                actualScreenMinutes = 0,
                targetScreenMinutes = targets.screenTimeTargetMinutes,
                actualFocusMinutes = 0,
                targetFocusMinutes = targets.focusTargetMinutes,
                actualBlockingMinutes = 0,
                targetBlockingMinutes = targets.blockingTargetMinutes,
                rolling7DayAverage = 0,
                currentRank = PrimeRankTier.AWAKENING,
                hasPermission = false,
                errorMessage = "Usage access permission required"
            )
        }

        val actualScreenMin = (screenMillis / (1000 * 60)).toInt()
        val verifiedFocusMin = repository.getTodayFocusMinutes()
        val verifiedBlockedMin = repository.getTodayBlockingMinutes()
        return PrimeScoreCalculator.computeScore(
            actualScreenMinutes = actualScreenMin,
            verifiedFocusMinutes = verifiedFocusMin,
            verifiedBlockedMinutes = verifiedBlockedMin,
            targets = targets,
            hasPermission = true
        )
    }

    fun openAchievementDashboard() {
        _uiState.update { it.copy(isAchievementDashboardOpen = true) }
    }

    fun closeAchievementDashboard() {
        _uiState.update { it.copy(isAchievementDashboardOpen = false) }
    }

    fun openTargetSettings() {
        _uiState.update { it.copy(isTargetSettingsOpen = true) }
    }

    fun closeTargetSettings() {
        _uiState.update { it.copy(isTargetSettingsOpen = false) }
    }

    fun openScoreFormula() {
        _uiState.update { it.copy(isScoreFormulaOpen = true) }
    }

    fun closeScoreFormula() {
        _uiState.update { it.copy(isScoreFormulaOpen = false) }
    }

    fun savePrimeTargets(screenMin: Int, focusMin: Int, blockingMin: Int) {
        achievementRepo.updateTargets(screenMin, focusMin, blockingMin)
        val targets = achievementRepo.targetsFlow.value
        val score = computeCurrentPrimeScore(_uiState.value.profile.screenTimeMillis, targets)
        _uiState.update {
            it.copy(
                primeTargets = targets,
                primeScoreBreakdown = score,
                profile = it.profile.copy(score = score.totalScore),
                snackbarMessage = "Daily targets saved! PRIME score updated."
            )
        }
    }

    fun pinBadgeToShowcase(badgeId: String) {
        achievementRepo.pinBadgeToFavorite(badgeId)
    }

    fun claimDailyQuest(questId: String) {
        achievementRepo.claimDailyQuest(questId)
        _uiState.update { it.copy(snackbarMessage = "Quest reward claimed! +XP added.") }
    }

    fun claimWeeklyMission(missionId: String) {
        achievementRepo.claimWeeklyMission(missionId)
        _uiState.update { it.copy(snackbarMessage = "Weekly mission reward claimed! +XP added.") }
    }

    fun selectBadgeForDetail(badge: PrimeBadge?) {
        _uiState.update { it.copy(selectedBadgeForDetail = badge) }
    }

    // ==================== WEBSITE BLOCKER NAVIGATION & ACTIONS ====================

    fun openWebsiteBlockerDashboard() {
        _uiState.update { it.copy(isWebsiteBlockerDashboardOpen = true) }
    }

    fun closeWebsiteBlockerDashboard() {
        _uiState.update { it.copy(isWebsiteBlockerDashboardOpen = false) }
    }

    fun openWebsiteSelectionScreen() {
        _uiState.update { it.copy(isWebsiteSelectionScreenOpen = true) }
    }

    fun closeWebsiteSelectionScreen() {
        _uiState.update { it.copy(isWebsiteSelectionScreenOpen = false) }
    }

    fun openCustomBlocklistScreen() {
        _uiState.update { it.copy(isCustomBlocklistScreenOpen = true) }
    }

    fun closeCustomBlocklistScreen() {
        _uiState.update { it.copy(isCustomBlocklistScreenOpen = false) }
    }

    fun openBlockingScheduleScreen() {
        _uiState.update { it.copy(isBlockingScheduleScreenOpen = true) }
    }

    fun closeBlockingScheduleScreen() {
        _uiState.update { it.copy(isBlockingScheduleScreenOpen = false) }
    }

    fun openBlockingAnalyticsScreen() {
        _uiState.update { it.copy(isBlockingAnalyticsScreenOpen = true) }
    }

    fun closeBlockingAnalyticsScreen() {
        _uiState.update { it.copy(isBlockingAnalyticsScreenOpen = false) }
    }

    fun togglePermanentWebsiteProtection(enabled: Boolean) {
        blockingManager?.togglePermanentProtection(enabled)
    }

    fun toggleWebsiteCategory(categoryId: String, enabled: Boolean) {
        viewModelScope.launch {
            blockingManager?.blocklistRepo?.toggleCategoryEnabled(categoryId, enabled)
            blockingManager?.reconcileActiveRules()
        }
    }

    fun toggleWebsiteDomain(domain: String, blocked: Boolean) {
        viewModelScope.launch {
            blockingManager?.blocklistRepo?.toggleDomainEnabled(domain, blocked)
            blockingManager?.reconcileActiveRules()
        }
    }

    fun toggleWebsiteFavorite(domain: String, favorite: Boolean) {
        viewModelScope.launch {
            blockingManager?.blocklistRepo?.toggleDomainFavorite(domain, favorite)
        }
    }

    fun toggleWebsitePermanent(domain: String, permanent: Boolean) {
        viewModelScope.launch {
            blockingManager?.blocklistRepo?.toggleDomainPermanent(domain, permanent)
            blockingManager?.reconcileActiveRules()
        }
    }

    fun addCustomWebsiteDomain(domain: String, categoryId: String = "Custom Websites", permanent: Boolean = false) {
        viewModelScope.launch {
            blockingManager?.blocklistRepo?.addCustomDomain(domain, categoryId, permanent)
            blockingManager?.reconcileActiveRules()
            _uiState.update { it.copy(snackbarMessage = "Added $domain to blocklist") }
        }
    }

    fun deleteCustomWebsiteDomain(domain: String) {
        viewModelScope.launch {
            blockingManager?.blocklistRepo?.deleteDomain(domain)
            blockingManager?.reconcileActiveRules()
            _uiState.update { it.copy(snackbarMessage = "Removed $domain") }
        }
    }

    fun addBlockingSchedule(
        title: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: String
    ) {
        viewModelScope.launch {
            val days = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            blockingManager?.scheduleRepo?.addSchedule(
                title = title,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                activeDays = days,
                categories = emptySet()
            )
            blockingManager?.reconcileActiveRules()
            _uiState.update { it.copy(snackbarMessage = "Schedule '$title' created") }
        }
    }

    fun toggleBlockingSchedule(scheduleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            blockingManager?.scheduleRepo?.toggleScheduleEnabled(scheduleId, isEnabled)
            blockingManager?.reconcileActiveRules()
        }
    }

    fun deleteBlockingSchedule(scheduleId: String) {
        viewModelScope.launch {
            blockingManager?.scheduleRepo?.deleteSchedule(scheduleId)
            blockingManager?.reconcileActiveRules()
        }
    }

    fun clearWebsiteBlockingHistory() {
        viewModelScope.launch {
            blockingManager?.analyticsRepo?.clearHistory()
            _uiState.update { it.copy(snackbarMessage = "Blocking history cleared") }
        }
    }
}

