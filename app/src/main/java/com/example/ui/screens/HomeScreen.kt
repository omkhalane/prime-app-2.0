package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PrimeBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.MomentumCard
import com.example.ui.components.OpalTopBar
import com.example.ui.components.PrimeCurrentRankHeroBadge
import com.example.ui.components.PrimeGlassButton
import com.example.ui.components.PrimePrimaryButton
import com.example.ui.components.ScoreTreeChart
import com.example.ui.components.ScreenTimeBreakdown
import com.example.ui.components.StackedAppIcons
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeSurfaceBorderBrush
import com.example.ui.theme.PrimeSurfaceElevated
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary
import com.example.viewmodel.OpalUiState

@Composable
fun HomeScreen(
    uiState: OpalUiState,
    onProfileClick: () -> Unit,
    onNavigateToMyApps: () -> Unit,
    onOpenDigitalWellbeing: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRefreshStats: () -> Unit,
    onLogTodayActivity: () -> Unit,
    onOpenAchievementDashboard: () -> Unit = {},
    onOpenTargetSettings: () -> Unit = {},
    onOpenFormulaDialog: () -> Unit = {},
    onBadgeClick: (PrimeBadge) -> Unit = {},
    onClaimQuest: (String) -> Unit = {},
    onOpenWebsiteBlocker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val blockedAppsList = uiState.apps.filter { it.isBlocked && !it.isWebsite }
    val blockedWebsitesCount = uiState.apps.count { it.isBlocked && it.isWebsite }
    val activeRulesCount = uiState.rules.count { !it.isPaused }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Prime Top Bar with Logo and Streak
            OpalTopBar(
                streakDays = uiState.profile.streakDays,
                onProfileClick = onProfileClick
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Dynamic User Rank Badge Hero based on score & ranking tier
                PrimeCurrentRankHeroBadge(
                    rankTier = uiState.primeScoreBreakdown.currentRank,
                    score = uiState.primeScoreBreakdown.totalScore,
                    hasPermission = uiState.screenTimePermissionGranted,
                    onClick = onOpenAchievementDashboard,
                    modifier = Modifier.testTag("home_milestone_badge")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Score Tree Chart matching Apple-inspired Liquid Glass styling
                ScoreTreeChart(
                    totalScore = uiState.primeScoreBreakdown.totalScore,
                    sleepScore = uiState.primeScoreBreakdown.screenTimeScore.toInt(),
                    focusScore = uiState.primeScoreBreakdown.focusScore.toInt(),
                    restScore = uiState.primeScoreBreakdown.blockingScore.toInt(),
                    hasPermission = uiState.screenTimePermissionGranted,
                    errorMessage = uiState.primeScoreBreakdown.errorMessage,
                    onRequestPermission = onRequestUsagePermission,
                    onChartClick = onOpenFormulaDialog
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Button directly below the Score Tree Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141414))
                        .border(BorderStroke(1.dp, Color(0xFF262626)), RoundedCornerShape(16.dp))
                        .clickable { onOpenAchievementDashboard() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("open_achievement_hub_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = PrimeAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Your progress & achievements",
                            color = PrimeTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Optional App Blocking Setup Banner if Accessibility is not granted yet
                if (!uiState.accessibilityPermissionGranted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimeAccent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = PrimeAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Enable Real App Blocking",
                                    color = PrimeTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Turn on Prime's Focus Shield in Accessibility settings so Prime can intercept blocked apps and display the focus overlay immediately.",
                                color = PrimeTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            PrimePrimaryButton(
                                text = "Turn On Focus Shield",
                                onClick = onRequestAccessibilityPermission,
                                height = 44.dp,
                                testTag = "enable_accessibility_banner_btn"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Screen Time Breakdown Card with click to open Prime Digital Wellbeing
                ScreenTimeBreakdown(
                    totalScreenTime = uiState.profile.screenTimeToday,
                    totalScreenTimeMillis = uiState.profile.screenTimeMillis,
                    apps = uiState.apps,
                    hasUsagePermission = uiState.profile.hasUsagePermission,
                    onRequestUsagePermission = onRequestUsagePermission,
                    onRefreshStats = onRefreshStats,
                    onOpenDigitalWellbeing = onOpenDigitalWellbeing,
                    onAppClick = { onOpenDigitalWellbeing() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Momentum Card (Real Streaks Counting based on days and activity)
                MomentumCard(
                    streakDays = uiState.profile.streakDays,
                    isActiveToday = uiState.profile.isActiveToday,
                    weekDaysActive = uiState.profile.weekDaysActive,
                    onLogTodayActivity = onLogTodayActivity
                )

                Spacer(modifier = Modifier.height(16.dp))

                // My apps status card (Real Blocked Apps preview)
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_my_apps_card"),
                    shape = RoundedCornerShape(24.dp),
                    onClick = onNavigateToMyApps
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Shields",
                                color = PrimeTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = "View Shields",
                                tint = PrimeTextTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (activeRulesCount > 0) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                contentDescription = "Shields status",
                                tint = if (activeRulesCount > 0) PrimeAccent else PrimeTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$activeRulesCount shields are active",
                                color = PrimeTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Real active blocked apps icon stack
                        if (blockedAppsList.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StackedAppIcons(
                                    items = blockedAppsList,
                                    maxVisible = 4,
                                    iconSize = 42.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "${blockedAppsList.size} apps" +
                                        if (blockedWebsitesCount > 0) ", $blockedWebsitesCount websites blocked" else " blocked",
                                color = PrimeAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            // Empty state
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp)
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(PrimeSurfaceHigher),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (it == 1) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Active",
                                                tint = PrimeTextTertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "No apps are blocked • Tap to select apps",
                                color = PrimeTextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Website Blocker Card
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_website_blocker_card"),
                    shape = RoundedCornerShape(24.dp),
                    onClick = onOpenWebsiteBlocker
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (uiState.isVpnRunning) PrimeAccent.copy(alpha = 0.15f)
                                            else PrimeSurfaceHigher
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = "Website Blocker",
                                        tint = if (uiState.isVpnRunning) PrimeAccent else PrimeTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Website Blocker",
                                        color = PrimeTextPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (uiState.isVpnRunning) "Protection is active" else "Protection is off",
                                        color = if (uiState.isVpnRunning) PrimeAccent else PrimeTextTertiary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = "Open Website Blocker",
                                tint = PrimeTextTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.activeWebsiteRulesCount} active domain rules",
                                color = PrimeTextSecondary,
                                fontSize = 14.sp
                            )
                            if (uiState.todayBlockedWebsitesCount > 0) {
                                Text(
                                    text = "${uiState.todayBlockedWebsitesCount} blocked today",
                                    color = PrimeAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                ProductivityHubCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))

                // Clean bottom padding for navigation bar
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
