package com.example

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.OpalRepository
import com.example.model.BlockRule
import com.example.model.NavigationTab
import com.example.ui.components.OpalBottomNavigation
import com.example.ui.screens.AddWebsiteDialog
import com.example.ui.screens.AlarmEditSheet
import com.example.ui.screens.AlarmScreen
import com.example.ui.screens.AppSpecificReportScreen
import com.example.ui.screens.CategoryDetailSheet
import com.example.ui.screens.ChallengeScreen
import com.example.ui.screens.DigitalWellbeingDashboard
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyAppsScreen
import com.example.ui.screens.OnboardingFlow
import com.example.ui.screens.PomodoroScreen
import com.example.ui.screens.PomodoroSettingsSheet
import com.example.ui.screens.PrimeAchievementDashboard
import com.example.ui.screens.PrimeScoreFormulaDialog
import com.example.ui.screens.PrimeTargetSettingsDialog
import com.example.ui.screens.QuickFocusSheet
import com.example.ui.screens.RingtoneSelectionSheet
import com.example.ui.screens.RuleDetailSheet
import com.example.ui.screens.RuleEditSheet
import com.example.ui.screens.SelectAppsSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StopwatchScreen
import com.example.ui.screens.SystemReauthDialog
import com.example.ui.screens.TimerBlockAppsSheet
import com.example.ui.screens.TimerPresetSheet
import com.example.ui.screens.TimerScreen
import com.example.ui.screens.BlockingAnalyticsScreen
import com.example.ui.screens.BlockingScheduleScreen
import com.example.ui.screens.CustomBlocklistScreen
import com.example.ui.screens.WebsiteBlockerDashboard
import com.example.ui.screens.WebsiteSelectionScreen
import com.example.ui.screens.WeeklyReportScreen
import com.example.ui.screens.WellbeingDailyDashboard
import com.example.vpn.service.VpnPermissionManager
import com.example.ui.theme.OpalBlack
import com.example.ui.theme.OpalTheme
import com.example.util.DeviceSecurityHelper
import com.example.util.PermissionHelper
import com.example.viewmodel.OpalViewModel
import com.example.viewmodel.TimerToolSubscreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpalTheme {
                OpalApp()
            }
        }
    }
}

@Composable
fun OpalApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val viewModel: OpalViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OpalViewModel(
                    repository = OpalRepository(appContext),
                    context = appContext
                ) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingSystemAuthAction = remember { mutableStateOf<(() -> Unit)?>(null) }

    // Launcher for real Android 13+ Notification Runtime Permission
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshPermissions()
        if (isGranted) {
            PermissionHelper.sendPrimeNudgeNotification(
                context,
                "Prime Notifications Enabled",
                "You'll get mindful nudges and session alerts."
            )
        }
        viewModel.nextOnboardingStep()
    }

    // Android-managed biometric or device-credential authentication.
    val activity = context.findFragmentActivity()
    val requestDeviceReauth: (() -> Unit) -> Unit = { onAuthenticated ->
        if (!DeviceSecurityHelper.isDeviceSecure(context) || activity == null) {
            viewModel.onReauthFailure()
        } else {
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val pending = pendingSystemAuthAction.value
                        pendingSystemAuthAction.value = null
                        pending?.invoke() ?: onAuthenticated()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        pendingSystemAuthAction.value = null
                        viewModel.onReauthFailure()
                    }
                }
            )
            val promptBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verify it's you")
                .setSubtitle("Use your fingerprint, biometrics or phone screen lock")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                @Suppress("DEPRECATION")
                promptBuilder.setDeviceCredentialAllowed(true)
            }
            val promptInfo = promptBuilder.build()
            pendingSystemAuthAction.value = onAuthenticated
            prompt.authenticate(promptInfo)
        }
    }

    // Launcher for Android VpnService permission request
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.refreshPermissions()
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.togglePermanentWebsiteProtection(true)
        }
    }

    val onRequestVpnPermission: () -> Unit = {
        val intent = VpnPermissionManager.getPrepareVpnIntent(context)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            viewModel.refreshPermissions()
        }
    }

    // Refresh permissions and live screen time whenever app resumes (e.g., returning from Settings)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshRealScreenTime()
        viewModel.refreshPermissions()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Handle back button logically
    BackHandler(
        enabled = uiState.isOnboarding ||
                uiState.isWebsiteBlockerDashboardOpen ||
                uiState.isWebsiteSelectionScreenOpen ||
                uiState.isCustomBlocklistScreenOpen ||
                uiState.isBlockingScheduleScreenOpen ||
                uiState.isBlockingAnalyticsScreenOpen ||
                uiState.isAchievementDashboardOpen ||
                uiState.isTargetSettingsOpen ||
                uiState.isScoreFormulaOpen ||
                uiState.selectedBadgeForDetail != null ||
                uiState.isSettingsOpen ||
                uiState.viewingAppSpecificMetric != null ||
                uiState.isWeeklyReportOpen ||
                uiState.isDailyDashboardOpen ||
                uiState.isDigitalWellbeingOpen ||
                uiState.isCreatingRule ||
                uiState.editingRule != null ||
                uiState.viewingRuleDetail != null ||
                uiState.isReauthDialogOpen ||
                uiState.activeCategorySheet != null ||
                uiState.isTimerBlockAppsSheetOpen ||
                uiState.isSelectAppsSheetOpen ||
                uiState.selectedTimerPreset != null ||
                uiState.testingChallengeConfig != null ||
                uiState.isRingtonePickerOpen ||
                uiState.isCreatingAlarm ||
                uiState.editingAlarm != null ||
                uiState.isPomodoroSettingsOpen ||
                uiState.activeTimerTool != TimerToolSubscreen.NONE ||
                uiState.currentTab != NavigationTab.HOME
    ) {
        when {
            uiState.isOnboarding -> viewModel.previousOnboardingStep()
            uiState.isWebsiteSelectionScreenOpen -> viewModel.closeWebsiteSelectionScreen()
            uiState.isCustomBlocklistScreenOpen -> viewModel.closeCustomBlocklistScreen()
            uiState.isBlockingScheduleScreenOpen -> viewModel.closeBlockingScheduleScreen()
            uiState.isBlockingAnalyticsScreenOpen -> viewModel.closeBlockingAnalyticsScreen()
            uiState.isWebsiteBlockerDashboardOpen -> viewModel.closeWebsiteBlockerDashboard()
            uiState.selectedBadgeForDetail != null -> viewModel.selectBadgeForDetail(null)
            uiState.isScoreFormulaOpen -> viewModel.closeScoreFormula()
            uiState.isTargetSettingsOpen -> viewModel.closeTargetSettings()
            uiState.isAchievementDashboardOpen -> viewModel.closeAchievementDashboard()
            uiState.testingChallengeConfig != null -> viewModel.closeTestChallenge()
            uiState.isRingtonePickerOpen -> viewModel.closeRingtonePicker()
            uiState.isCreatingAlarm || uiState.editingAlarm != null -> viewModel.closeAlarmEdit()
            uiState.isPomodoroSettingsOpen -> viewModel.closePomodoroSettings()
            uiState.activeTimerTool != TimerToolSubscreen.NONE -> viewModel.closeTimerTool()
            uiState.isTimerBlockAppsSheetOpen -> viewModel.closeTimerBlockAppsSheet()
            uiState.isReauthDialogOpen -> viewModel.dismissReauthDialog()
            uiState.activeCategorySheet != null -> viewModel.closeCategorySheet()
            uiState.viewingAppSpecificMetric != null -> viewModel.closeAppSpecificReport()
            uiState.isWeeklyReportOpen -> viewModel.closeWeeklyReport()
            uiState.isDailyDashboardOpen -> viewModel.closeDailyDashboard()
            uiState.isDigitalWellbeingOpen -> viewModel.closeDigitalWellbeing()
            uiState.isSettingsOpen -> viewModel.closeSettings()
            uiState.isSelectAppsSheetOpen -> viewModel.closeSelectAppsSheet()
            uiState.isCreatingRule -> viewModel.closeCreateRuleSheet()
            uiState.editingRule != null -> viewModel.closeEditRule()
            uiState.viewingRuleDetail != null -> viewModel.closeRuleDetail()
            uiState.selectedTimerPreset != null -> viewModel.closeTimerPreset()
            uiState.currentTab != NavigationTab.HOME -> viewModel.selectTab(NavigationTab.HOME)
        }
    }

    if (uiState.isOnboarding) {
        OnboardingFlow(
            uiState = uiState,
            onNextStep = { viewModel.nextOnboardingStep() },
            onPrevStep = { viewModel.previousOnboardingStep() },
            onSetName = { viewModel.setOnboardingName(it) },
            onSetGoal = { viewModel.setOnboardingGoal(it) },
            onSetScreenTime = { viewModel.setOnboardingScreenTime(it) },
            onRequestOverlayPermission = {
                PermissionHelper.requestOverlayPermission(context)
            },
            onRequestScreenTimePermission = {
                PermissionHelper.requestUsageAccessPermission(context)
            },
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    PermissionHelper.createNotificationChannel(context)
                    viewModel.refreshPermissions()
                    viewModel.nextOnboardingStep()
                }
            },
            onRequestAccessibilityPermission = {
                PermissionHelper.requestAccessibilityPermission(context)
            },
            onToggleAppBlocked = { viewModel.toggleItemBlocked(it) },
            onComplete = { viewModel.completeOnboarding() }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = OpalBlack,
            bottomBar = {
                if (!uiState.isOnboarding &&
                    !uiState.isSettingsOpen &&
                    !uiState.isAchievementDashboardOpen &&
                    !uiState.isWebsiteBlockerDashboardOpen &&
                    !uiState.isWebsiteSelectionScreenOpen &&
                    !uiState.isCustomBlocklistScreenOpen &&
                    !uiState.isBlockingScheduleScreenOpen &&
                    !uiState.isBlockingAnalyticsScreenOpen &&
                    !uiState.isCreatingRule &&
                    uiState.editingRule == null &&
                    uiState.viewingRuleDetail == null &&
                    !uiState.isTargetSettingsOpen &&
                    !uiState.isScoreFormulaOpen &&
                    uiState.selectedBadgeForDetail == null &&
                    !uiState.isReauthDialogOpen &&
                    uiState.activeCategorySheet == null &&
                    !uiState.isTimerBlockAppsSheetOpen &&
                    !uiState.isSelectAppsSheetOpen &&
                    uiState.selectedTimerPreset == null &&
                    !uiState.isRingtonePickerOpen &&
                    !uiState.isPomodoroSettingsOpen &&
                    !uiState.isDigitalWellbeingOpen &&
                    !uiState.isDailyDashboardOpen &&
                    !uiState.isWeeklyReportOpen &&
                    uiState.viewingAppSpecificMetric == null &&
                    uiState.activeTimerTool == TimerToolSubscreen.NONE &&
                    !uiState.isCreatingAlarm &&
                    uiState.editingAlarm == null &&
                    uiState.testingChallengeConfig == null
                ) {
                    OpalBottomNavigation(
                        currentTab = uiState.currentTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .background(OpalBlack)
            ) {
                // Tab Content
                when (uiState.currentTab) {
                    NavigationTab.HOME -> {
                        HomeScreen(
                            uiState = uiState,
                            onProfileClick = { viewModel.selectTab(NavigationTab.PROFILE) },
                            onNavigateToMyApps = { viewModel.selectTab(NavigationTab.MY_APPS) },
                            onOpenDigitalWellbeing = { viewModel.openDigitalWellbeing() },
                            onRequestUsagePermission = {
                                PermissionHelper.requestUsageAccessPermission(context)
                            },
                            onRequestAccessibilityPermission = {
                                PermissionHelper.requestAccessibilityPermission(context)
                            },
                            onRefreshStats = { viewModel.refreshRealScreenTime() },
                            onLogTodayActivity = { viewModel.logDailyActivity() },
                            onOpenAchievementDashboard = { viewModel.openAchievementDashboard() },
                            onOpenTargetSettings = { viewModel.openTargetSettings() },
                            onOpenFormulaDialog = { viewModel.openScoreFormula() },
                            onBadgeClick = { badge -> viewModel.selectBadgeForDetail(badge) },
                            onClaimQuest = { questId -> viewModel.claimDailyQuest(questId) },
                            onOpenWebsiteBlocker = { viewModel.openWebsiteBlockerDashboard() },
                            onOpenFocus = { viewModel.selectTab(NavigationTab.TIMER) }
                        )
                    }
                    NavigationTab.MY_APPS -> {
                        MyAppsScreen(
                            uiState = uiState,
                            onRuleClick = { viewModel.viewRuleDetail(it) },
                            onAddCustomShield = { viewModel.openCreateRuleSheet() },
                            onCategoryClick = { category ->
                                viewModel.openCategorySheet(category)
                            },
                            onUnblockApp = { viewModel.toggleItemBlocked(it) },
                            onBlockedAppClick = { viewModel.previewBlockOverlay(it) },
                            onOpenWebsiteBlocker = { viewModel.openWebsiteBlockerDashboard() }
                        )
                    }
                    NavigationTab.TIMER -> {
                        TimerScreen(
                            uiState = uiState,
                            onAdjustMinutes = { viewModel.adjustTimerMinutes(it) },
                            onStartTimer = { viewModel.startTimer() },
                            onPauseTimer = { viewModel.pauseTimer() },
                            onStopTimer = { viewModel.stopTimer() },
                            onToggleBlockApps = { viewModel.toggleTimerBlockApps() },
                            onOpenBlockAppsSheet = { viewModel.openTimerBlockAppsSheet() },
                            onSelectPreset = { viewModel.selectTimerPreset(it) },
                            onOpenPomodoro = { viewModel.openTimerTool(TimerToolSubscreen.POMODORO) },
                            onOpenStopwatch = { viewModel.openTimerTool(TimerToolSubscreen.STOPWATCH) },
                            onOpenAlarm = { viewModel.openTimerTool(TimerToolSubscreen.ALARM) }
                        )
                    }
                    NavigationTab.INSIGHTS -> {
                        DigitalWellbeingDashboard(
                            totalScreenTime = uiState.profile.screenTimeToday,
                            totalScreenTimeMillis = uiState.profile.screenTimeMillis,
                            categories = uiState.todayCategoryUsage,
                            topApps = uiState.todayAppUsage,
                            hasUsagePermission = uiState.screenTimePermissionGranted,
                            errorMessage = uiState.wellbeingError,
                            onBackClick = { viewModel.selectTab(NavigationTab.HOME) },
                            onRequestPermission = { PermissionHelper.requestUsageAccessPermission(context) },
                            onRetryFetch = { viewModel.refreshWellbeingData() },
                            onOpenDailyDashboard = { viewModel.openDailyDashboard() },
                            onOpenWeeklyReport = { viewModel.openWeeklyReport() },
                            onAppClick = { viewModel.openAppSpecificReport(it) },
                            showBackButton = false
                        )
                    }
                    NavigationTab.PROFILE -> {
                        SettingsScreen(
                            profile = uiState.profile,
                            onBackClick = { viewModel.selectTab(NavigationTab.HOME) },
                            onTryProClick = { viewModel.upgradeToPro() },
                            onUseEmergencyPass = { viewModel.useEmergencyPass() },
                            onReplayOnboarding = { viewModel.startOnboarding() },
                            showBackButton = false
                        )
                    }
                }

                // Digital Wellbeing Dashboard Overlay
                if (uiState.isDigitalWellbeingOpen) {
                    DigitalWellbeingDashboard(
                        totalScreenTime = uiState.profile.screenTimeToday,
                        totalScreenTimeMillis = uiState.profile.screenTimeMillis,
                        categories = uiState.todayCategoryUsage,
                        topApps = uiState.todayAppUsage,
                        hasUsagePermission = uiState.screenTimePermissionGranted,
                        errorMessage = uiState.wellbeingError,
                        onBackClick = { viewModel.closeDigitalWellbeing() },
                        onRequestPermission = { PermissionHelper.requestUsageAccessPermission(context) },
                        onRetryFetch = { viewModel.refreshWellbeingData() },
                        onOpenDailyDashboard = { viewModel.openDailyDashboard() },
                        onOpenWeeklyReport = { viewModel.openWeeklyReport() },
                        onAppClick = { viewModel.openAppSpecificReport(it) }
                    )
                }

                // Wellbeing Daily Dashboard ("< Dashboard" with Screen Time, Notifications, Unlocks)
                if (uiState.isDailyDashboardOpen) {
                    WellbeingDailyDashboard(
                        totalScreenTime = uiState.profile.screenTimeToday,
                        trendPoints = uiState.dayUsagePoints,
                        topApps = uiState.todayAppUsage,
                        hasUsagePermission = uiState.screenTimePermissionGranted,
                        errorMessage = uiState.wellbeingError,
                        onBackClick = { viewModel.closeDailyDashboard() },
                        onRequestPermission = { PermissionHelper.requestUsageAccessPermission(context) },
                        onRetryFetch = { viewModel.refreshWellbeingData() },
                        onAppClick = { viewModel.openAppSpecificReport(it) },
                        onOpenWeeklyReport = { viewModel.openWeeklyReport() }
                    )
                }

                // Weekly Report Screen ("< Weekly report" with Daily Avg, Social Category, Peak Heatmap)
                if (uiState.isWeeklyReportOpen) {
                    WeeklyReportScreen(
                        weeklyApps = uiState.weeklyAppUsage,
                        trendPoints = uiState.dayUsagePoints,
                        heatmapMatrix = uiState.weeklyHeatmap,
                        hasUsagePermission = uiState.screenTimePermissionGranted,
                        errorMessage = uiState.wellbeingError,
                        onBackClick = { viewModel.closeWeeklyReport() },
                        onRequestPermission = { PermissionHelper.requestUsageAccessPermission(context) },
                        onRetryFetch = { viewModel.refreshWellbeingData() },
                        onAppClick = { viewModel.openAppSpecificReport(it) }
                    )
                }

                // App Specific Report Screen ("< [App Name]" with Daily avg, notifs, app opens)
                uiState.viewingAppSpecificMetric?.let { metric ->
                    AppSpecificReportScreen(
                        metric = metric,
                        onBackClick = { viewModel.closeAppSpecificReport() },
                        onSetAppTimerClick = {
                            viewModel.closeAppSpecificReport()
                            viewModel.selectTab(NavigationTab.TIMER)
                        },
                        onNotificationSettingsClick = {
                            viewModel.openSettings()
                        }
                    )
                }

                // Settings Screen Overlay
                if (uiState.isSettingsOpen) {
                    SettingsScreen(
                        profile = uiState.profile,
                        onBackClick = { viewModel.closeSettings() },
                        onTryProClick = { viewModel.upgradeToPro() },
                        onUseEmergencyPass = { viewModel.useEmergencyPass() },
                        onReplayOnboarding = { viewModel.startOnboarding() }
                    )
                }

                // Edit Shield Screen Overlay
                uiState.editingRule?.let { rule ->
                    RuleEditSheet(
                        rule = rule,
                        apps = uiState.apps,
                        onDismiss = { viewModel.closeEditRule() },
                        onSaveRule = { viewModel.saveRule(it) },
                        onSelectAppsClick = { viewModel.openSelectAppsSheet() },
                        onTogglePause = { viewModel.toggleRulePause(rule.id) },
                        onUpgradeProPrompt = { viewModel.upgradeToPro() }
                    )
                }

                // Create Custom Shield Screen Overlay
                if (uiState.isCreatingRule) {
                    val blankRule = remember {
                        BlockRule(
                            id = "shield_custom_${System.currentTimeMillis()}",
                            title = "",
                            startsInText = "Active 9am–5pm",
                            fromTime = "9:00 am",
                            toTime = "5:00 pm",
                            daysLabel = "Weekdays",
                            activeDays = setOf(0, 1, 2, 3, 4),
                            blockedAppIds = emptyList(),
                            isHardMode = false,
                            allowBreaks = true,
                            isPaused = false,
                            isCustom = true
                        )
                    }
                    RuleEditSheet(
                        rule = blankRule,
                        apps = uiState.apps,
                        onDismiss = { viewModel.closeCreateRuleSheet() },
                        onSaveRule = { viewModel.createCustomRule(it) },
                        onSelectAppsClick = { viewModel.openSelectAppsSheet() },
                        onTogglePause = { },
                        onUpgradeProPrompt = { viewModel.upgradeToPro() },
                        isCreating = true
                    )
                }

                // Category Detail Sheet Overlay
                uiState.activeCategorySheet?.let { categoryName ->
                    CategoryDetailSheet(
                        categoryTitle = categoryName,
                        apps = uiState.apps,
                        onDismiss = { viewModel.closeCategorySheet() },
                        onMoveAppCategory = { appId, newCategory ->
                            viewModel.setAppCategory(appId, newCategory)
                        },
                        onToggleBlock = { viewModel.toggleItemBlocked(it) }
                    )
                }

                // System Re-authentication Modal for Never Allowed apps
                if (uiState.isReauthDialogOpen) {
                    val isSecure = DeviceSecurityHelper.isDeviceSecure(context)
                    SystemReauthDialog(
                        onDismiss = { viewModel.dismissReauthDialog() },
                        isSystemLockAvailable = isSecure,
                        onLaunchSystemPrompt = {
                            requestDeviceReauth { viewModel.onReauthSuccess() }
                        }
                    )
                }

                // Select Apps Sheet Overlay
                if (uiState.isSelectAppsSheetOpen) {
                    SelectAppsSheet(
                        apps = uiState.apps,
                        onDismiss = { viewModel.closeSelectAppsSheet() },
                        onToggleBlock = { viewModel.toggleItemBlocked(it) },
                        onRemoveItem = { viewModel.removeBlockedItem(it) },
                        onAddCustomWebsiteClick = { viewModel.openAddWebsiteSheet() }
                    )
                }

                // Add Website Bottom Sheet
                if (uiState.isAddWebsiteSheetOpen) {
                    AddWebsiteDialog(
                        existingWebsites = uiState.apps.filter { it.isWebsite }.map { it.name },
                        onDismiss = { viewModel.closeAddWebsiteSheet() },
                        onAddWebsite = { viewModel.addWebsite(it) }
                    )
                }

                // View Rule Detail Bottom Sheet
                uiState.viewingRuleDetail?.let { rule ->
                    RuleDetailSheet(
                        rule = rule,
                        apps = uiState.apps,
                        onDismiss = { viewModel.closeRuleDetail() },
                        onEditClick = { viewModel.editRule(rule) },
                        onTogglePause = { viewModel.toggleRulePause(rule.id) },
                        onDeleteRule = { viewModel.deleteRule(it) }
                    )
                }

                // Timer Preset Bottom Sheet (Big Themed Modal)
                uiState.selectedTimerPreset?.let { preset ->
                    TimerPresetSheet(
                        preset = preset,
                        blockedAppsCount = uiState.apps.count { it.isBlocked } + uiState.timerSessionBlockedAppIds.size,
                        onConfigureApps = { viewModel.openTimerBlockAppsSheet() },
                        onDismiss = { viewModel.closeTimerPreset() },
                        onStartFocus = {
                            viewModel.startTimer(preset = preset)
                        }
                    )
                }

                // Timer Session Block Apps Sheet
                if (uiState.isTimerBlockAppsSheetOpen) {
                    TimerBlockAppsSheet(
                        apps = uiState.apps,
                        sessionBlockedAppIds = uiState.timerSessionBlockedAppIds,
                        onToggleSessionApp = { viewModel.toggleTimerSessionApp(it) },
                        onSelectAllUnblocked = { viewModel.selectAllTimerSessionApps() },
                        onClearSelected = { viewModel.clearTimerSessionApps() },
                        onDismiss = { viewModel.closeTimerBlockAppsSheet() }
                    )
                }

                // Quick Focus Duration Bottom Sheet
                if (uiState.isQuickFocusSheetOpen) {
                    QuickFocusSheet(
                        onDismiss = { viewModel.closeQuickFocusSheet() },
                        onSelectMinutes = { viewModel.startTimer(customMinutes = it) }
                    )
                }

                // ==================== TIMER TOOLS FULL SUBSCREENS & SHEETS ====================

                // 1. Pomodoro Screen Subscreen
                if (uiState.activeTimerTool == TimerToolSubscreen.POMODORO) {
                    PomodoroScreen(
                        state = uiState.pomodoroState,
                        settings = uiState.pomodoroSettings,
                        themeId = uiState.clockPreferences.timerTheme,
                        onBack = { viewModel.closeTimerTool() },
                        onStart = { viewModel.startPomodoro() },
                        onPause = { viewModel.pausePomodoro() },
                        onResume = { viewModel.resumePomodoro() },
                        onReset = { viewModel.resetPomodoro() },
                        onSkip = { viewModel.skipPomodoroSession() },
                        onOpenSettings = { viewModel.openPomodoroSettings() },
                        onApplyQuickPreset = { focusMin, breakMin ->
                            viewModel.applyPomodoroQuickPreset(focusMin, breakMin)
                        }
                    )
                }

                // Pomodoro Settings Sheet
                if (uiState.isPomodoroSettingsOpen) {
                    PomodoroSettingsSheet(
                        initialSettings = uiState.pomodoroSettings,
                        onDismiss = { viewModel.closePomodoroSettings() },
                        onSave = { viewModel.savePomodoroSettings(it) }
                    )
                }

                // 2. Stopwatch Screen Subscreen
                if (uiState.activeTimerTool == TimerToolSubscreen.STOPWATCH) {
                    StopwatchScreen(
                        elapsedMillis = uiState.stopwatchState.elapsedMillis,
                        isRunning = uiState.stopwatchState.isRunning,
                        laps = uiState.stopwatchState.laps,
                        themeId = uiState.clockPreferences.timerTheme,
                        onBack = { viewModel.closeTimerTool() },
                        onStart = { viewModel.startStopwatch() },
                        onPause = { viewModel.pauseStopwatch() },
                        onResume = { viewModel.resumeStopwatch() },
                        onReset = { viewModel.resetStopwatch() },
                        onLap = { viewModel.recordLap() }
                    )
                }

                // 3. Alarm Screen Subscreen
                if (uiState.activeTimerTool == TimerToolSubscreen.ALARM) {
                    AlarmScreen(
                        alarms = uiState.alarms,
                        onBack = { viewModel.closeTimerTool() },
                        onAddNewAlarm = { viewModel.openCreateAlarm() },
                        onEditAlarm = { viewModel.openEditAlarm(it) },
                        onToggleAlarm = { id, enabled -> viewModel.toggleAlarm(id, enabled) },
                        onDeleteAlarm = { viewModel.deleteAlarm(it) },
                        clockPreferences = uiState.clockPreferences,
                        onSaveClockPreferences = { viewModel.saveClockPreferences(it) }
                    )
                }

                // Alarm Edit / Create Bottom Sheet
                if (uiState.isCreatingAlarm || uiState.editingAlarm != null) {
                    uiState.editingAlarm?.let { alarm ->
                        AlarmEditSheet(
                            initialAlarm = alarm,
                            isCreating = uiState.isCreatingAlarm,
                            onDismiss = { viewModel.closeAlarmEdit() },
                            onSave = { viewModel.saveAlarm(it) },
                            onDelete = { viewModel.deleteAlarm(it) },
                            onOpenRingtonePicker = { viewModel.openRingtonePicker() },
                            onTestChallenge = { viewModel.testChallenge(it) }
                        )
                    }
                }

                // Ringtone Selection Sheet
                if (uiState.isRingtonePickerOpen) {
                    RingtoneSelectionSheet(
                        selectedRingtoneId = uiState.editingAlarm?.ringtoneId ?: "zen_crystal",
                        selectedRingtoneUriString = uiState.editingAlarm?.ringtoneUriString,
                        selectedRingtoneName = uiState.editingAlarm?.ringtoneName,
                        onDismiss = { viewModel.closeRingtonePicker() },
                        onSelectRingtone = { viewModel.selectRingtone(it) }
                    )
                }

                // Test Challenge Full-Screen Modal
                uiState.testingChallengeConfig?.let { config ->
                    ChallengeScreen(
                        config = config,
                        isTestMode = true,
                        onChallengeCompleted = { viewModel.closeTestChallenge() },
                        onDismissTest = { viewModel.closeTestChallenge() }
                    )
                }

                // ==================== PRIME ACHIEVEMENTS & SCORING OVERLAYS ====================

                // PRIME Achievement Dashboard Subscreen
                if (uiState.isAchievementDashboardOpen) {
                    PrimeAchievementDashboard(
                        scoreBreakdown = uiState.primeScoreBreakdown,
                        progression = uiState.primeProgression,
                        badges = uiState.badges,
                        dailyQuests = uiState.dailyQuests,
                        weeklyMissions = uiState.weeklyMissions,
                        onPinBadge = { viewModel.pinBadgeToShowcase(it) },
                        onClaimQuest = { viewModel.claimDailyQuest(it) },
                        onClaimMission = { viewModel.claimWeeklyMission(it) },
                        onOpenFormulaDialog = { viewModel.openScoreFormula() },
                        onBack = { viewModel.closeAchievementDashboard() }
                    )
                }

                // PRIME Target Settings Dialog
                if (uiState.isTargetSettingsOpen) {
                    PrimeTargetSettingsDialog(
                        currentTargets = uiState.primeTargets,
                        currentScoreBreakdown = uiState.primeScoreBreakdown,
                        onSaveTargets = { screen, focus, blocking ->
                            viewModel.savePrimeTargets(screen, focus, blocking)
                        },
                        onDismiss = { viewModel.closeTargetSettings() }
                    )
                }

                // PRIME Score Formula Dialog
                if (uiState.isScoreFormulaOpen) {
                    PrimeScoreFormulaDialog(
                        onDismiss = { viewModel.closeScoreFormula() }
                    )
                }

                // ==================== WEBSITE BLOCKER FULL SCREENS ====================

                // 1. Website Blocker Dashboard
                if (uiState.isWebsiteBlockerDashboardOpen) {
                    WebsiteBlockerDashboard(
                        isVpnRunning = uiState.isVpnRunning,
                        isPermanentProtectionEnabled = uiState.isPermanentWebsiteBlockingEnabled,
                        hasVpnPermission = uiState.hasVpnPermission,
                        activeRulesCount = uiState.activeWebsiteRulesCount,
                        blockedTodayCount = uiState.todayBlockedWebsitesCount,
                        categories = uiState.websiteCategories,
                        domains = uiState.blockedDomainsList,
                        onBackClick = { viewModel.closeWebsiteBlockerDashboard() },
                        onRequestVpnPermission = onRequestVpnPermission,
                        onTogglePermanentProtection = { enabled ->
                            if (enabled && !uiState.hasVpnPermission) {
                                onRequestVpnPermission()
                            } else if (!enabled) {
                                requestDeviceReauth { viewModel.togglePermanentWebsiteProtection(false) }
                            } else {
                                viewModel.togglePermanentWebsiteProtection(true)
                            }
                        },
                        onToggleCategory = { catId, enabled ->
                            viewModel.toggleWebsiteCategory(catId, enabled)
                            if (catId == "Entertainment") viewModel.toggleWebsiteCategory("Video Streaming", enabled)
                        },
                        onOpenCategorySelection = { viewModel.openWebsiteSelectionScreen() },
                        onOpenCustomBlocklist = { viewModel.openCustomBlocklistScreen() },
                        onOpenSchedules = { viewModel.openBlockingScheduleScreen() },
                        onOpenAnalytics = { viewModel.openBlockingAnalyticsScreen() }
                    )
                }

                // 2. Website & Category Selection Screen
                if (uiState.isWebsiteSelectionScreenOpen) {
                    WebsiteSelectionScreen(
                        categories = uiState.websiteCategories,
                        domains = uiState.blockedDomainsList,
                        onBackClick = { viewModel.closeWebsiteSelectionScreen() },
                        onToggleDomainEnabled = { domain, enabled ->
                            val item = uiState.blockedDomainsList.firstOrNull { it.domain == domain }
                            if (item?.isPermanent == true && !enabled) {
                                requestDeviceReauth {
                                    viewModel.toggleWebsitePermanent(domain, false, reauthenticated = true)
                                    viewModel.toggleWebsiteDomain(domain, false)
                                }
                            } else {
                                viewModel.toggleWebsiteDomain(domain, enabled)
                            }
                        },
                        onToggleDomainFavorite = { domain, fav ->
                            viewModel.toggleWebsiteFavorite(domain, fav)
                        },
                        onToggleCategoryEnabled = { catId, enabled ->
                            viewModel.toggleWebsiteCategory(catId, enabled)
                            if (catId == "Entertainment") viewModel.toggleWebsiteCategory("Video Streaming", enabled)
                        },
                        onAddCustomDomain = { domain, categoryId ->
                            viewModel.addCustomWebsiteDomain(domain, categoryId)
                        },
                        onAddCustomCategory = { name, description ->
                            viewModel.addCustomWebsiteCategory(name, description)
                        }
                    )
                }

                // 3. Custom Blocklist Screen
                if (uiState.isCustomBlocklistScreenOpen) {
                    CustomBlocklistScreen(
                        customDomains = uiState.blockedDomainsList.filter { it.isCustom },
                        onBackClick = { viewModel.closeCustomBlocklistScreen() },
                        onAddDomain = { domain, isPermanent, durationMillis ->
                            viewModel.addCustomWebsiteDomain(domain, "Custom Websites", isPermanent, durationMillis)
                        },
                        onDeleteDomain = { domain ->
                            val item = uiState.blockedDomainsList.firstOrNull { it.domain == domain }
                            if (item?.isPermanent == true) {
                                requestDeviceReauth { viewModel.deleteCustomWebsiteDomain(domain, reauthenticated = true) }
                            } else {
                                viewModel.deleteCustomWebsiteDomain(domain)
                            }
                        },
                        onTogglePermanent = { domain, isPermanent ->
                            if (!isPermanent) {
                                requestDeviceReauth {
                                    viewModel.toggleWebsitePermanent(domain, false, reauthenticated = true)
                                    viewModel.toggleWebsiteDomain(domain, false)
                                }
                            } else {
                                viewModel.toggleWebsitePermanent(domain, true)
                            }
                        },
                        onToggleEnabled = { domain, isBlocked ->
                            val item = uiState.blockedDomainsList.firstOrNull { it.domain == domain }
                            if (item?.isPermanent == true && !isBlocked) {
                                requestDeviceReauth {
                                    viewModel.toggleWebsitePermanent(domain, false, reauthenticated = true)
                                    viewModel.toggleWebsiteDomain(domain, false)
                                }
                            } else {
                                viewModel.toggleWebsiteDomain(domain, isBlocked)
                            }
                        }
                    )
                }

                // 4. Blocking Schedule Screen
                if (uiState.isBlockingScheduleScreenOpen) {
                    BlockingScheduleScreen(
                        schedules = uiState.blockingSchedulesList,
                        onBackClick = { viewModel.closeBlockingScheduleScreen() },
                        categories = uiState.websiteCategories,
                        onAddSchedule = { title, startH, startM, endH, endM, days, selectedCategories, startDate, endDate ->
                            val daysStr = days.joinToString(",")
                            viewModel.addBlockingSchedule(title, startH, startM, endH, endM, daysStr, selectedCategories, startDate, endDate)
                        },
                        onToggleSchedule = { scheduleId, isEnabled ->
                            viewModel.toggleBlockingSchedule(scheduleId, isEnabled)
                        },
                        onDeleteSchedule = { scheduleId ->
                            viewModel.deleteBlockingSchedule(scheduleId)
                        }
                    )
                }

                // 5. Blocking Analytics Screen
                if (uiState.isBlockingAnalyticsScreenOpen) {
                    BlockingAnalyticsScreen(
                        totalBlockedCount = uiState.totalBlockedWebsitesCount,
                        todayBlockedCount = uiState.todayBlockedWebsitesCount,
                        uniqueDomainsCount = uiState.uniqueBlockedWebsitesCount,
                        topBlockedDomains = uiState.topBlockedWebsitesList,
                        categoryDistribution = uiState.websiteCategoryDistribution,
                        recentEvents = uiState.recentBlockedEventsList,
                        onBackClick = { viewModel.closeBlockingAnalyticsScreen() },
                        onClearHistory = { viewModel.clearWebsiteBlockingHistory() }
                    )
                }
            }
        }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context = this
    while (true) {
        if (current is FragmentActivity) return current
        if (current !is ContextWrapper) return null
        current = current.baseContext
    }
}
