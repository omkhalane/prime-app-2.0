package com.example

import com.example.data.OpalRepository
import com.example.data.RealScreenTimeTracker
import com.example.data.StreakTracker
import com.example.model.NavigationTab
import com.example.model.OnboardingStep
import com.example.service.OpalAccessibilityService
import com.example.util.FaviconManager
import com.example.util.formatAppName
import com.example.viewmodel.OpalViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

    @Test
    fun testAppNameTruncation() {
        // App name <= 15 chars: unchanged
        assertEquals("Instagram", formatAppName("Instagram"))
        assertEquals("WhatsApp", formatAppName("WhatsApp"))
        assertEquals("123456789012345", formatAppName("123456789012345")) // exactly 15 chars

        // App name > 15 chars: trimmed to 12 chars + "..."
        assertEquals("123456789012...", formatAppName("1234567890123456")) // 16 chars
        assertEquals("MySuperLongA...", formatAppName("MySuperLongApplication"))
        assertEquals("Prime Focus ...", formatAppName("Prime Focus Productivity Shield"))
    }

    @Test
    fun testStreakStartsAtZeroAndIncrementsOnDayChange() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // 0 days active -> streak must be 0
        assertEquals(0, StreakTracker.calculateStreakFromDates(emptySet()))

        // 1 day active today -> streak is 1
        val cal = Calendar.getInstance()
        val todayStr = dateFormat.format(cal.time)
        assertEquals(1, StreakTracker.calculateStreakFromDates(setOf(todayStr)))

        // Day change with consecutive activity: today + yesterday -> streak is 2 (+1 on day change!)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = dateFormat.format(cal.time)
        assertEquals(2, StreakTracker.calculateStreakFromDates(setOf(todayStr, yesterdayStr)))

        // Day before yesterday also active -> streak is 3
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val twoDaysAgoStr = dateFormat.format(cal.time)
        assertEquals(3, StreakTracker.calculateStreakFromDates(setOf(todayStr, yesterdayStr, twoDaysAgoStr)))

        // If today not logged yet, but yesterday was active, streak is preserved from yesterday
        assertEquals(2, StreakTracker.calculateStreakFromDates(setOf(yesterdayStr, twoDaysAgoStr)))

        // If both today and yesterday were missed, streak resets to 0
        assertEquals(0, StreakTracker.calculateStreakFromDates(setOf(twoDaysAgoStr)))
    }

    @Test
    fun testFaviconDomainValidationAndCleaning() {
        // Cleaning
        assertEquals("reddit.com", FaviconManager.cleanDomain("https://www.Reddit.com/r/android"))
        assertEquals("youtube.com", FaviconManager.cleanDomain("http://youtube.com/watch?v=123"))
        assertEquals("instagram.com", FaviconManager.cleanDomain("instagram.com/"))

        // Validation
        assertTrue(FaviconManager.isValidDomain("reddit.com"))
        assertTrue(FaviconManager.isValidDomain("https://sub.domain.co.uk"))
        assertTrue(FaviconManager.isValidDomain("youtube.com"))

        assertFalse(FaviconManager.isValidDomain(""))
        assertFalse(FaviconManager.isValidDomain("invalid"))
        assertFalse(FaviconManager.isValidDomain("spaces not allowed.com"))
        assertFalse(FaviconManager.isValidDomain(".startdot.com"))
    }

    @Test
    fun testRepositoryRulesAndPresets() {
        val repo = OpalRepository()
        val rules = repo.rulesFlow.value
        val presets = repo.presetsFlow.value
        val profile = repo.profileFlow.value

        assertEquals(2, rules.size)
        assertEquals("Work Time", rules[0].title)
        assertEquals(8, presets.size)
        assertEquals(0, profile.streakDays) // Starts strictly from 0
        assertEquals(0, profile.score) // Default score starts strictly at 0 without hardcoded fallbacks
    }

    @Test
    fun testRulePauseToggle() {
        val repo = OpalRepository()
        val ruleId = "rule_work_time"
        val initialPause = repo.rulesFlow.value.find { it.id == ruleId }?.isPaused ?: false
        assertFalse(initialPause)

        repo.toggleRulePause(ruleId)
        val paused = repo.rulesFlow.value.find { it.id == ruleId }?.isPaused ?: false
        assertTrue(paused)

        repo.toggleRulePause(ruleId)
        val resumed = repo.rulesFlow.value.find { it.id == ruleId }?.isPaused ?: false
        assertFalse(resumed)
    }

    @Test
    fun testAddCustomWebsite() {
        val repo = OpalRepository()
        repo.addCustomWebsite("https://news.ycombinator.com")
        val apps = repo.appsFlow.value
        assertTrue(apps.any { it.name == "news.ycombinator.com" && it.isWebsite })
    }

    @Test
    fun testTimerMinuteAdjustments() {
        val vm = OpalViewModel()
        assertEquals(30, vm.uiState.value.timerSelectedMinutes)

        vm.adjustTimerMinutes(15)
        assertEquals(45, vm.uiState.value.timerSelectedMinutes)

        vm.adjustTimerMinutes(-30)
        assertEquals(15, vm.uiState.value.timerSelectedMinutes)

        // Lower bound clamp (5m)
        vm.adjustTimerMinutes(-50)
        assertEquals(5, vm.uiState.value.timerSelectedMinutes)
    }

    @Test
    fun testEmergencyPassConsumption() {
        val repo = OpalRepository()
        assertEquals(1, repo.profileFlow.value.emergencyPassesRemaining)

        val pass1 = repo.useEmergencyPass()
        assertTrue(pass1)
        assertEquals(0, repo.profileFlow.value.emergencyPassesRemaining)

        val pass2 = repo.useEmergencyPass()
        assertFalse(pass2)
        assertEquals(0, repo.profileFlow.value.emergencyPassesRemaining)
    }

    @Test
    fun testFreshInstallOnboardingAndCompletion() {
        val repo = OpalRepository()
        val vm = OpalViewModel(repository = repo)

        // Fresh install: onboarding should be active
        assertTrue(vm.uiState.value.isOnboarding)
        assertEquals(OnboardingStep.WELCOME_ROCK, vm.uiState.value.onboardingStep)

        vm.nextOnboardingStep()
        assertEquals(OnboardingStep.NAME_INPUT, vm.uiState.value.onboardingStep)

        vm.previousOnboardingStep()
        assertEquals(OnboardingStep.WELCOME_ROCK, vm.uiState.value.onboardingStep)

        // Complete onboarding
        vm.completeOnboarding()
        assertFalse(vm.uiState.value.isOnboarding)
        assertEquals(NavigationTab.HOME, vm.uiState.value.currentTab)
    }

    @Test
    fun testDurationFormatting() {
        assertEquals("1h 15m", RealScreenTimeTracker.formatDuration(75 * 60 * 1000L))
        assertEquals("30m", RealScreenTimeTracker.formatDuration(30 * 60 * 1000L))
        assertEquals("2h", RealScreenTimeTracker.formatDuration(120 * 60 * 1000L))
        assertEquals("< 1m", RealScreenTimeTracker.formatDuration(20 * 1000L))
    }

    @Test
    fun testSnoozePackageInAccessibility() {
        val pkg = "com.android.chrome"
        assertFalse(OpalAccessibilityService.isPackageSnoozed(pkg))

        OpalAccessibilityService.snoozePackage(pkg, 60_000L)
        assertTrue(OpalAccessibilityService.isPackageSnoozed(pkg))
    }

    @Test
    fun testPomodoroDefaultsAndState() {
        val vm = OpalViewModel()
        val settings = vm.uiState.value.pomodoroSettings
        assertEquals(25, settings.focusMinutes)
        assertEquals(5, settings.shortBreakMinutes)
        assertEquals(15, settings.longBreakMinutes)
        assertEquals(4, settings.roundsPerCycle)
        assertEquals(1, settings.totalCycles)

        val state = vm.uiState.value.pomodoroState
        assertEquals(com.example.model.PomodoroSessionType.FOCUS, state.sessionType)
        assertEquals(1, state.currentRound)
        assertEquals(1, state.currentCycle)
        assertEquals(25 * 60, state.remainingSeconds)
    }

    @Test
    fun testStopwatchLapRecording() {
        val vm = OpalViewModel()
        vm.startStopwatch()
        assertTrue(vm.uiState.value.stopwatchState.isRunning)

        vm.recordLap()
        assertEquals(1, vm.uiState.value.stopwatchState.laps.size)

        vm.recordLap()
        assertEquals(2, vm.uiState.value.stopwatchState.laps.size)

        vm.resetStopwatch()
        assertFalse(vm.uiState.value.stopwatchState.isRunning)
        assertEquals(0, vm.uiState.value.stopwatchState.laps.size)
        assertEquals(0L, vm.uiState.value.stopwatchState.elapsedMillis)
    }

    @Test
    fun testAlarmTriggerCalculation() {
        val alarm = com.example.model.AlarmItem(
            id = "test_alarm",
            hour = 7,
            minute = 0,
            recurrence = com.example.model.AlarmRecurrence.DAILY
        )
        val now = System.currentTimeMillis()
        val nextTrigger = com.example.alarm.AlarmSchedulerHelper.calculateNextTriggerMillis(alarm, now)
        org.junit.Assert.assertNotNull(nextTrigger)
        assertTrue(nextTrigger!! > now)

        val formattedRemaining = com.example.alarm.AlarmSchedulerHelper.formatRemainingTime(nextTrigger, now)
        assertTrue(formattedRemaining.startsWith("Rings in"))
    }

    @Test
    fun testChallengeEngineGeneration() {
        val mathEasy = com.example.challenge.ChallengeEngine.generateChallenge(
            com.example.model.ChallengeCategory.MATH,
            com.example.model.ChallengeDifficulty.EASY
        )
        assertTrue(mathEasy is com.example.challenge.ChallengeItem.MathQuestion)
        val mathQ = mathEasy as com.example.challenge.ChallengeItem.MathQuestion
        assertTrue(mathQ.choices.contains(mathQ.correctAnswer))
        assertEquals(4, mathQ.choices.size)

        val seqHard = com.example.challenge.ChallengeEngine.generateChallenge(
            com.example.model.ChallengeCategory.SEQUENCE,
            com.example.model.ChallengeDifficulty.HARD
        )
        assertTrue(seqHard is com.example.challenge.ChallengeItem.SequenceQuestion)
    }

    @Test
    fun testTimerSessionAppBlockingSurvivesDashboardRefreshAndAutoUnblocks() {
        val repo = OpalRepository()
        repo.syncAccessibilityBlockList()
        val initialBlocked = OpalAccessibilityService.blockedPackages.toSet()

        val tempPackage = "com.temp.focus.session"
        repo.applyTimerSessionAppBlock(setOf(tempPackage))
        assertTrue(OpalAccessibilityService.blockedPackages.contains(tempPackage))
        assertTrue(OpalAccessibilityService.isBlockingActive)

        // A profile/dashboard refresh must not erase temporary timer rules.
        repo.syncAccessibilityBlockList()
        assertTrue(OpalAccessibilityService.blockedPackages.contains(tempPackage))

        repo.clearTimerSessionAppBlock()
        assertFalse(OpalAccessibilityService.blockedPackages.contains(tempPackage))
        assertEquals(initialBlocked, OpalAccessibilityService.blockedPackages)
    }

    @Test
    fun testBlockingMinutesAreZeroUntilEnforcementUptimeIsMeasured() {
        val repo = OpalRepository()
        assertEquals(0, repo.getTodayBlockingMinutes())
    }

    @Test
    fun testAllEightPresetsConfigured() {
        val repo = OpalRepository()
        val presets = repo.presetsFlow.value
        assertEquals(8, presets.size)

        val titles = presets.map { it.title }
        assertTrue(titles.contains("Deep Study"))
        assertTrue(titles.contains("Executive Meeting"))
        assertTrue(titles.contains("Nature Immersion"))
        assertTrue(titles.contains("Power Nap"))
        assertTrue(titles.contains("Commute Zen"))
        assertTrue(titles.contains("Coding Flow"))
        assertTrue(titles.contains("Mindful Reading"))
        assertTrue(titles.contains("Breath & Meditation"))

        presets.forEach { preset ->
            assertTrue(preset.durationMinutes > 0)
            assertTrue(preset.formattedDuration.isNotBlank())
            assertTrue(preset.category.isNotBlank())
            assertTrue(preset.themeColorHex != 0L)
            assertTrue(preset.drawableResId != 0)
        }
    }

    @Test
    fun testPresetPlayingSetsActivePresetAndTitleAndClearsOnStop() {
        val repo = OpalRepository()
        val viewModel = OpalViewModel(repo)
        val naturePreset = repo.presetsFlow.value.first { it.title == "Nature Immersion" }

        viewModel.startTimer(preset = naturePreset)
        assertEquals("Nature Immersion", viewModel.uiState.value.activePresetTitle)
        assertEquals(naturePreset.id, viewModel.uiState.value.activePreset?.id)
        assertEquals(naturePreset.drawableResId, viewModel.uiState.value.activePreset?.drawableResId)
        assertTrue(viewModel.uiState.value.isTimerRunning)
        assertEquals(60 * 60, viewModel.uiState.value.timerRemainingSeconds)

        viewModel.stopTimer()
        org.junit.Assert.assertNull(viewModel.uiState.value.activePresetTitle)
        org.junit.Assert.assertNull(viewModel.uiState.value.activePreset)
        assertFalse(viewModel.uiState.value.isTimerRunning)
    }

    @Test
    fun testPrimeScoreFormulasAndExampleDailyScore() {
        // Daily targets: Screen 180m, Focus 120m, Blocking 180m
        val targets = com.example.model.PrimeTargets(
            screenTimeTargetMinutes = 180,
            focusTargetMinutes = 120,
            blockingTargetMinutes = 180
        )

        // Screen time control (300 max)
        // At or below target: 150m actual (target 180m) -> 300 pts
        val screenScore = com.example.data.PrimeScoreCalculator.calculateScreenTimeScore(150, targets.screenTimeTargetMinutes)
        assertEquals(300f, screenScore, 0.01f)

        // At 1.5x target: 270m actual -> 150 pts
        val screenScore15 = com.example.data.PrimeScoreCalculator.calculateScreenTimeScore(270, 180)
        assertEquals(150f, screenScore15, 0.01f)

        // At 2x target: 360m actual -> 0 pts
        val screenScore20 = com.example.data.PrimeScoreCalculator.calculateScreenTimeScore(360, 180)
        assertEquals(0f, screenScore20, 0.01f)

        // Focus time (350 max)
        // 90 min actual out of 120 min target -> 350 * (90/120) = 262.5 pts
        val focusScore = com.example.data.PrimeScoreCalculator.calculateFocusScore(90, targets.focusTargetMinutes)
        assertEquals(262.5f, focusScore, 0.01f)

        // Blocking discipline (350 max)
        // 120 min actual out of 180 min target -> 350 * (120/180) = 233.33 pts
        val blockingScore = com.example.data.PrimeScoreCalculator.calculateBlockingScore(120, targets.blockingTargetMinutes)
        assertEquals(233.33f, blockingScore, 0.1f)

        // Total PRIME score: 300 + 262.5 + 233.3 = 795.83 -> rounds to 796
        val fullBreakdown = com.example.data.PrimeScoreCalculator.computeScore(
            actualScreenMinutes = 150,
            verifiedFocusMinutes = 90,
            verifiedBlockedMinutes = 120,
            targets = targets,
            hasPermission = true
        )
        assertEquals(796, fullBreakdown.totalScore)
        assertEquals(com.example.model.PrimeRankTier.ELITE, fullBreakdown.currentRank)
        assertEquals("Gold", fullBreakdown.currentRank.award)
    }

    @Test
    fun testPrimeEightRankingTiers() {
        assertEquals(com.example.model.PrimeRankTier.AWAKENING, com.example.model.PrimeRankTier.fromScore(100))
        assertEquals("Beginner", com.example.model.PrimeRankTier.AWAKENING.award)

        assertEquals(com.example.model.PrimeRankTier.EXPLORER, com.example.model.PrimeRankTier.fromScore(250))
        assertEquals("Bronze", com.example.model.PrimeRankTier.EXPLORER.award)

        assertEquals(com.example.model.PrimeRankTier.INITIATE, com.example.model.PrimeRankTier.fromScore(400))
        assertEquals("Bronze+", com.example.model.PrimeRankTier.INITIATE.award)

        assertEquals(com.example.model.PrimeRankTier.DISCIPLINED, com.example.model.PrimeRankTier.fromScore(550))
        assertEquals("Silver", com.example.model.PrimeRankTier.DISCIPLINED.award)

        assertEquals(com.example.model.PrimeRankTier.FOCUSED, com.example.model.PrimeRankTier.fromScore(650))
        assertEquals("Silver+", com.example.model.PrimeRankTier.FOCUSED.award)

        assertEquals(com.example.model.PrimeRankTier.ELITE, com.example.model.PrimeRankTier.fromScore(750))
        assertEquals("Gold", com.example.model.PrimeRankTier.ELITE.award)

        assertEquals(com.example.model.PrimeRankTier.MASTER, com.example.model.PrimeRankTier.fromScore(850))
        assertEquals("Platinum", com.example.model.PrimeRankTier.MASTER.award)

        assertEquals(com.example.model.PrimeRankTier.PRIME, com.example.model.PrimeRankTier.fromScore(950))
        assertEquals("Diamond", com.example.model.PrimeRankTier.PRIME.award)
    }

    @Test
    fun testPrimeAchievementRepositoryBadges() {
        val achievementRepo = com.example.data.PrimeAchievementRepository()
        val badges = achievementRepo.badgesFlow.value
        assertEquals(42, badges.size) // 36 core badges + 6 rare/special badges

        val unlockedBadges = badges.filter { it.isUnlocked }
        assertEquals(0, unlockedBadges.size) // No demo badges are pre-unlocked.

        val quests = achievementRepo.dailyQuestsFlow.value
        assertEquals(3, quests.size)
        assertTrue(quests.none { it.isCompleted })
        assertEquals(-1, quests.first { it.id == "quest_screen_time" }.currentProgress)
        assertTrue(quests.filterNot { it.id == "quest_screen_time" }.all { it.currentProgress == 0 })

        val missions = achievementRepo.weeklyMissionsFlow.value
        assertEquals(3, missions.size)
        assertTrue(missions.none { it.isCompleted })
        assertTrue(missions.all { it.currentProgress == 0 })

        val progression = achievementRepo.progressionFlow.value
        assertTrue(progression.primeLevel >= 1)
        assertEquals(0, progression.totalXp)
    }

    @Test
    fun testAchievementQuestsUseMeasuredProgressOnly() {
        val achievementRepo = com.example.data.PrimeAchievementRepository()
        achievementRepo.updateDailyProgress(
            screenMinutes = null,
            completedPomodoroSessions = 2,
            blockingMinutes = 60
        )
        val quests = achievementRepo.dailyQuestsFlow.value.associateBy { it.id }
        assertFalse(quests.getValue("quest_screen_time").isCompleted)
        assertTrue(quests.getValue("quest_pomodoro").isCompleted)
        assertTrue(quests.getValue("quest_blocking").isCompleted)

        achievementRepo.updateWeeklyFocusProgress(10)
        assertTrue(achievementRepo.weeklyMissionsFlow.value
            .first { it.id == "mission_focus_sessions" }.isCompleted)
    }

    // ==================== WEBSITE BLOCKER DNS & MATCHER TESTS ====================

    @Test
    fun testDomainMatcherNormalization() {
        assertEquals("instagram.com", com.example.vpn.dns.DomainMatcher.normalize("https://www.Instagram.COM/feed"))
        assertEquals("reddit.com", com.example.vpn.dns.DomainMatcher.normalize("http://reddit.com:8080/r/all"))
        assertEquals("tiktok.com", com.example.vpn.dns.DomainMatcher.normalize("  WWW.TIKTOK.COM/  "))
        assertEquals("youtube.com", com.example.vpn.dns.DomainMatcher.normalize("youtube.com."))
    }

    @Test
    fun testDomainMatcherSubdomainsAndBoundarySafety() {
        val blocked = setOf("instagram.com", "tiktok.com", "reddit.com")

        // Exact match
        assertTrue(com.example.vpn.dns.DomainMatcher.isBlocked("instagram.com", blocked))
        assertTrue(com.example.vpn.dns.DomainMatcher.isBlocked("tiktok.com", blocked))

        // Subdomains MUST match
        assertTrue(com.example.vpn.dns.DomainMatcher.isBlocked("www.instagram.com", blocked))
        assertTrue(com.example.vpn.dns.DomainMatcher.isBlocked("api.v2.instagram.com", blocked))
        assertTrue(com.example.vpn.dns.DomainMatcher.isBlocked("m.tiktok.com", blocked))
        assertTrue(com.example.vpn.dns.DomainMatcher.isBlocked("old.reddit.com", blocked))

        // Suffix boundary safety: false positives MUST be rejected
        assertFalse(com.example.vpn.dns.DomainMatcher.isBlocked("notinstagram.com", blocked))
        assertFalse(com.example.vpn.dns.DomainMatcher.isBlocked("fakeinstagram.com", blocked))
        assertFalse(com.example.vpn.dns.DomainMatcher.isBlocked("anti-tiktok.com", blocked))
        assertFalse(com.example.vpn.dns.DomainMatcher.isBlocked("subreddit.com", blocked))
        assertFalse(com.example.vpn.dns.DomainMatcher.isBlocked("google.com", blocked))
    }

    @Test
    fun testDomainMatcherAllowlistPrecedence() {
        val blocked = setOf("google.com", "github.com")
        val allowlist = setOf("docs.google.com")

        // Blocked domain
        assertTrue(com.example.vpn.dns.DomainMatcher.isBlocked("google.com", blocked, allowlist))
        assertTrue(com.example.vpn.dns.DomainMatcher.isBlocked("mail.google.com", blocked, allowlist))

        // Allowed subdomain overrides block
        assertFalse(com.example.vpn.dns.DomainMatcher.isBlocked("docs.google.com", blocked, allowlist))
    }

    @Test
    fun testDnsPacketParserAndSyntheticResponseBuilder() {
        // Construct a standard DNS query for "example.com" QTYPE A (1), QCLASS IN (1), TxID 0x1234
        // QNAME for "example.com": 7 'e' 'x' 'a' 'm' 'p' 'l' 'e' 3 'c' 'o' 'm' 0
        val queryBytes = byteArrayOf(
            0x12.toByte(), 0x34.toByte(), // ID: 0x1234
            0x01.toByte(), 0x00.toByte(), // Flags: Standard query, RD=1
            0x00.toByte(), 0x01.toByte(), // QDCOUNT: 1
            0x00.toByte(), 0x00.toByte(), // ANCOUNT: 0
            0x00.toByte(), 0x00.toByte(), // NSCOUNT: 0
            0x00.toByte(), 0x00.toByte(), // ARCOUNT: 0
            // QNAME: 7example3com0
            7, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
            3, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            0,
            0x00.toByte(), 0x01.toByte(), // QTYPE: A (1)
            0x00.toByte(), 0x01.toByte()  // QCLASS: IN (1)
        )

        val query = com.example.vpn.dns.DnsPacketParser.parseQuery(queryBytes)
        org.junit.Assert.assertNotNull(query)
        assertEquals(0x1234, query!!.transactionId)
        assertEquals("example.com", query.domain)
        assertEquals(com.example.vpn.dns.DnsPacketParser.QTYPE_A, query.qtype)
        assertEquals(1, query.qclass)

        // Build NXDOMAIN response
        val nxdomainResponse = com.example.vpn.dns.DnsPacketBuilder.buildBlockedResponse(query, com.example.vpn.dns.BlockingStrategy.NXDOMAIN)
        // Check TxID preserved
        assertEquals(0x12.toByte(), nxdomainResponse[0])
        assertEquals(0x34.toByte(), nxdomainResponse[1])
        // Flags: QR=1, RA=1, RCODE=3 (NXDOMAIN) -> 0x8183
        assertEquals(0x81.toByte(), nxdomainResponse[2])
        assertEquals(0x83.toByte(), nxdomainResponse[3])

        // Build Sinkhole (0.0.0.0) response
        val sinkholeResponse = com.example.vpn.dns.DnsPacketBuilder.buildBlockedResponse(query, com.example.vpn.dns.BlockingStrategy.SINKHOLE_IP)
        assertEquals(0x12.toByte(), sinkholeResponse[0])
        assertEquals(0x34.toByte(), sinkholeResponse[1])
        // Flags: QR=1, RA=1, RCODE=0 (NOERROR) -> 0x8180
        assertEquals(0x81.toByte(), sinkholeResponse[2])
        assertEquals(0x80.toByte(), sinkholeResponse[3])
        // ANCOUNT should be 1
        assertEquals(0x00.toByte(), sinkholeResponse[6])
        assertEquals(0x01.toByte(), sinkholeResponse[7])
        // Last 4 bytes should be 0.0.0.0
        val len = sinkholeResponse.size
        assertEquals(0.toByte(), sinkholeResponse[len - 4])
        assertEquals(0.toByte(), sinkholeResponse[len - 3])
        assertEquals(0.toByte(), sinkholeResponse[len - 2])
        assertEquals(0.toByte(), sinkholeResponse[len - 1])
    }

    @Test
    fun testAccessibilityServiceWebsiteNormalizationAndSnooze() {
        // Domain normalization
        assertEquals("youtube.com", OpalAccessibilityService.normalizeDomain("https://www.youtube.com/watch?v=123"))
        assertEquals("instagram.com", OpalAccessibilityService.normalizeDomain("http://m.instagram.com/reels/"))
        assertEquals("reddit.com", OpalAccessibilityService.normalizeDomain("reddit.com:443/r/android"))

        // Update blocked websites
        OpalAccessibilityService.updateBlockedWebsites(listOf("youtube.com", "instagram.com"))
        assertTrue(OpalAccessibilityService.blockedWebsites.contains("youtube.com"))
        assertTrue(OpalAccessibilityService.blockedWebsites.contains("instagram.com"))
        assertFalse(OpalAccessibilityService.blockedWebsites.contains("wikipedia.org"))

        // Snooze functionality
        assertFalse(OpalAccessibilityService.isWebsiteSnoozed("youtube.com"))
        OpalAccessibilityService.snoozeWebsite("youtube.com", 60_000L)
        assertTrue(OpalAccessibilityService.isWebsiteSnoozed("youtube.com"))
        assertTrue(OpalAccessibilityService.isWebsiteSnoozed("https://www.youtube.com/watch")) // normalized check
        assertFalse(OpalAccessibilityService.isWebsiteSnoozed("instagram.com"))
    }

    @Test
    fun testKnownBrowsersList() {
        assertTrue(OpalAccessibilityService.KNOWN_BROWSER_PACKAGES.contains("com.android.chrome"))
        assertTrue(OpalAccessibilityService.KNOWN_BROWSER_PACKAGES.contains("com.google.android.googlequicksearchbox"))
        assertTrue(OpalAccessibilityService.KNOWN_BROWSER_PACKAGES.contains("com.sec.android.app.sbrowser"))
        assertTrue(OpalAccessibilityService.KNOWN_BROWSER_PACKAGES.contains("org.mozilla.firefox"))
        assertTrue(OpalAccessibilityService.KNOWN_BROWSER_PACKAGES.contains("com.microsoft.emmx"))
        assertTrue(OpalAccessibilityService.KNOWN_BROWSER_PACKAGES.contains("com.brave.browser"))
    }

    @Test
    fun testDoHCanaryDomainIsBlockedWithNxdomain() {
        val queryBytes = byteArrayOf(
            0xAB.toByte(), 0xCD.toByte(), // ID: 0xABCD
            0x01.toByte(), 0x00.toByte(), // Flags: Standard query
            0x00.toByte(), 0x01.toByte(), // QDCOUNT: 1
            0x00.toByte(), 0x00.toByte(), // ANCOUNT: 0
            0x00.toByte(), 0x00.toByte(), // NSCOUNT: 0
            0x00.toByte(), 0x00.toByte(), // ARCOUNT: 0
            // QNAME: 19use-application-dns3net0
            19, 'u'.code.toByte(), 's'.code.toByte(), 'e'.code.toByte(), '-'.code.toByte(),
            'a'.code.toByte(), 'p'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'i'.code.toByte(),
            'c'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'i'.code.toByte(), 'o'.code.toByte(),
            'n'.code.toByte(), '-'.code.toByte(), 'd'.code.toByte(), 'n'.code.toByte(), 's'.code.toByte(),
            3, 'n'.code.toByte(), 'e'.code.toByte(), 't'.code.toByte(),
            0,
            0x00.toByte(), 0x01.toByte(), // QTYPE: A
            0x00.toByte(), 0x01.toByte()  // QCLASS: IN
        )

        val engine = com.example.vpn.dns.DnsFilterEngine()
        val result = engine.processDnsPayload(queryBytes)
        assertTrue(result is com.example.vpn.dns.DnsFilterResult.Blocked)
        val blocked = result as com.example.vpn.dns.DnsFilterResult.Blocked
        assertEquals("use-application-dns.net", blocked.domain)
        // Check NXDOMAIN RCODE in flags: 0x8183
        assertEquals(0x81.toByte(), blocked.responsePayload[2])
        assertEquals(0x83.toByte(), blocked.responsePayload[3])
    }
}


