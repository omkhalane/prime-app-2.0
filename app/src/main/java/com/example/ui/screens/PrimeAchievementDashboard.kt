package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.BadgeCategory
import com.example.model.DailyQuest
import com.example.model.PrimeBadge
import com.example.model.PrimeProgression
import com.example.model.PrimeScoreBreakdown
import com.example.model.WeeklyMission
import com.example.ui.components.PrimeHexagonBadge
import com.example.ui.theme.OpalBlack
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary

@Composable
fun PrimeAchievementDashboard(
    scoreBreakdown: PrimeScoreBreakdown,
    progression: PrimeProgression,
    badges: List<PrimeBadge>,
    dailyQuests: List<DailyQuest>,
    weeklyMissions: List<WeeklyMission>,
    onPinBadge: (String) -> Unit,
    onClaimQuest: (String) -> Unit,
    onClaimMission: (String) -> Unit,
    onOpenFormulaDialog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf<BadgeCategory?>(null) }
    var selectedStatusFilter by remember { mutableStateOf(BadgeStatusFilter.ALL) }
    var selectedBadgeForDetail by remember { mutableStateOf<PrimeBadge?>(null) }
    var currentTab by remember { mutableStateOf(DashboardSection.BADGES) }

    val unlockedCount = remember(badges) { badges.count { it.isUnlocked } }
    val lockedCount = remember(badges) { badges.count { !it.isUnlocked } }

    val filteredBadges = remember(selectedStatusFilter, selectedCategoryFilter, badges) {
        badges.filter { badge ->
            val matchesCategory = selectedCategoryFilter == null || badge.category == selectedCategoryFilter
            val matchesStatus = when (selectedStatusFilter) {
                BadgeStatusFilter.ALL -> true
                BadgeStatusFilter.GOTTEN -> badge.isUnlocked
                BadgeStatusFilter.LOCKED -> !badge.isUnlocked
            }
            matchesCategory && matchesStatus
        }
    }

    val pinnedBadges = remember(progression.pinnedBadgeIds, badges) {
        progression.pinnedBadgeIds.mapNotNull { id -> badges.find { it.id == id } }
            .ifEmpty { badges.filter { it.isUnlocked }.take(3) }
    }

    val inProgressBadges = remember(badges) {
        badges.filter { !it.isUnlocked && it.currentProgress > 0f }
            .sortedByDescending { it.currentProgress / it.targetRequirement }
            .take(6)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OpalBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16191D))
                            .border(1.dp, Color(0x26FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OpalTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PRIME Achievements",
                            color = OpalTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = "Rank ${scoreBreakdown.currentRank.tierNumber} · ${scoreBreakdown.currentRank.title}",
                            color = scoreBreakdown.currentRank.primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onOpenFormulaDialog,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16191D))
                        .border(1.dp, Color(0x26FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Scoring Guide",
                        tint = OpalMint
                    )
                }
            }

            // Scrollable Dashboard Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Button that opens the How Score Is Calculated & Works bottom popup
                Button(
                    onClick = onOpenFormulaDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF101419),
                        contentColor = OpalMint
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, OpalMint.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .testTag("how_score_is_calculated_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = OpalMint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "How Score Is Calculated & How It Works",
                        color = OpalMint,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prime Identity Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xDF12151A))
                        .border(1.dp, Color(0x3300E59A), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PRIME IDENTITY",
                                    color = OpalTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${scoreBreakdown.totalScore}",
                                        color = OpalMint,
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = " pts (Today)",
                                        color = OpalTextSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(scoreBreakdown.currentRank.primaryColor.copy(alpha = 0.2f))
                                        .border(1.dp, scoreBreakdown.currentRank.primaryColor, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${scoreBreakdown.currentRank.title} · ${scoreBreakdown.currentRank.award}",
                                        color = scoreBreakdown.currentRank.primaryColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "7-Day Avg: ${scoreBreakdown.rolling7DayAverage} pts",
                                    color = OpalTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Level & XP Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Prime Level ${progression.primeLevel}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${progression.currentLevelXp} / ${progression.xpForNextLevel} XP (${progression.totalXp} Total)",
                                color = OpalTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = {
                                (progression.currentLevelXp.toFloat() / progression.xpForNextLevel.toFloat()).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = OpalMint,
                            trackColor = Color(0xFF252A34),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Overview Numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverviewStatCard(
                        title = "Unlocked",
                        value = "${progression.unlockedBadgesCount}",
                        subtitle = "of ${progression.totalBadgesCount} total",
                        modifier = Modifier.weight(1f)
                    )
                    OverviewStatCard(
                        title = "Remaining",
                        value = "${progression.totalBadgesCount - progression.unlockedBadgesCount}",
                        subtitle = "to unlock",
                        modifier = Modifier.weight(1f)
                    )
                    OverviewStatCard(
                        title = "Completion",
                        value = "${progression.completionPercentage}%",
                        subtitle = "mastery rate",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Featured Showcase
                Text(
                    text = "FEATURED SHOWCASE (PINNED)",
                    color = OpalTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xD914171D))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pinnedBadges.forEach { badge ->
                        PrimeHexagonBadge(
                            badge = badge,
                            badgeSize = 88.dp,
                            showLabel = true,
                            onClick = { selectedBadgeForDetail = badge }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tab Switcher: Badges vs Quests & Missions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xDF14171D))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    DashboardTabButton(
                        title = "Badge Collection (${badges.size})",
                        isSelected = currentTab == DashboardSection.BADGES,
                        onClick = { currentTab = DashboardSection.BADGES },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardTabButton(
                        title = "Daily Quests & Missions",
                        isSelected = currentTab == DashboardSection.QUESTS,
                        onClick = { currentTab = DashboardSection.QUESTS },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (currentTab) {
                    DashboardSection.BADGES -> {
                        // Status Filter Row: All, Gotten, Locked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161920))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BadgeStatusChip(
                                title = "All (${badges.size})",
                                isSelected = selectedStatusFilter == BadgeStatusFilter.ALL,
                                onClick = { selectedStatusFilter = BadgeStatusFilter.ALL },
                                modifier = Modifier.weight(1f)
                            )
                            BadgeStatusChip(
                                title = "Gotten ($unlockedCount)",
                                isSelected = selectedStatusFilter == BadgeStatusFilter.GOTTEN,
                                onClick = { selectedStatusFilter = BadgeStatusFilter.GOTTEN },
                                modifier = Modifier.weight(1f)
                            )
                            BadgeStatusChip(
                                title = "Locked ($lockedCount)",
                                isSelected = selectedStatusFilter == BadgeStatusFilter.LOCKED,
                                onClick = { selectedStatusFilter = BadgeStatusFilter.LOCKED },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Horizontal Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryFilterChip(
                                label = "All Categories",
                                isSelected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null }
                            )

                            BadgeCategory.entries.forEach { cat ->
                                CategoryFilterChip(
                                    label = "${cat.code}: ${cat.title}",
                                    isSelected = selectedCategoryFilter == cat,
                                    onClick = { selectedCategoryFilter = cat }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // In-Progress Badges highlight (if "All" is selected)
                        if (selectedCategoryFilter == null && inProgressBadges.isNotEmpty()) {
                            Text(
                                text = "IN PROGRESS (NEXT UNLOCKS)",
                                color = OpalTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                inProgressBadges.forEach { badge ->
                                    Box(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF181B22))
                                            .border(1.dp, Color(0xFF282E3B), RoundedCornerShape(16.dp))
                                            .clickable { selectedBadgeForDetail = badge }
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            PrimeHexagonBadge(
                                                badge = badge,
                                                badgeSize = 72.dp,
                                                showLabel = false
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = badge.name,
                                                color = OpalTextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { (badge.currentProgress / badge.targetRequirement).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = OpalMint,
                                                trackColor = Color(0xFF252A34)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${badge.currentProgress.toInt()}/${badge.targetRequirement.toInt()} ${badge.unit}",
                                                color = OpalTextSecondary,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Badge Grid (3 per row)
                        Text(
                            text = if (selectedCategoryFilter != null) {
                                "${selectedCategoryFilter!!.title.uppercase()} (${filteredBadges.size})"
                            } else {
                                "ALL 36 CORE & SPECIAL BADGES (${filteredBadges.size})"
                            },
                            color = OpalTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Grid rendered as chunked rows inside vertical scroll
                        filteredBadges.chunked(3).forEach { rowBadges ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowBadges.forEach { badge ->
                                    PrimeHexagonBadge(
                                        badge = badge,
                                        badgeSize = 92.dp,
                                        showLabel = true,
                                        onClick = { selectedBadgeForDetail = badge }
                                    )
                                }
                                // Empty spacer for remainder in last row
                                if (rowBadges.size < 3) {
                                    repeat(3 - rowBadges.size) {
                                        Spacer(modifier = Modifier.width(92.dp))
                                    }
                                }
                            }
                        }
                    }

                    DashboardSection.QUESTS -> {
                        // Daily Quests
                        Text(
                            text = "DAILY QUESTS · RESET AT MIDNIGHT",
                            color = OpalTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        dailyQuests.forEach { quest ->
                            QuestCard(
                                title = quest.title,
                                description = quest.description,
                                progress = quest.currentProgress,
                                target = quest.targetProgress,
                                unit = quest.unit,
                                xpReward = quest.xpReward,
                                isCompleted = quest.isCompleted,
                                isClaimed = quest.isClaimed,
                                onClaim = { onClaimQuest(quest.id) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Weekly Missions
                        Text(
                            text = "WEEKLY MISSIONS · RESET ON MONDAY",
                            color = OpalTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        weeklyMissions.forEach { mission ->
                            QuestCard(
                                title = mission.title,
                                description = mission.description,
                                progress = mission.currentProgress,
                                target = mission.targetProgress,
                                unit = mission.unit,
                                xpReward = mission.xpReward,
                                isCompleted = mission.isCompleted,
                                isClaimed = mission.isClaimed,
                                onClaim = { onClaimMission(mission.id) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }

    // Interactive Badge Detail Modal
    selectedBadgeForDetail?.let { badge ->
        BadgeDetailDialog(
            badge = badge,
            isPinned = progression.pinnedBadgeIds.contains(badge.id),
            onTogglePin = { onPinBadge(badge.id) },
            onDismiss = { selectedBadgeForDetail = null }
        )
    }
}

private enum class DashboardSection {
    BADGES,
    QUESTS
}

@Composable
private fun OverviewStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161920))
            .border(1.dp, Color(0xFF262C38), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                color = OpalTextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = OpalTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun DashboardTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) OpalMint else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else OpalTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) OpalMint.copy(alpha = 0.2f) else Color(0xFF1B1E26))
            .border(
                1.dp,
                if (isSelected) OpalMint else Color(0xFF2B303C),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) OpalMint else OpalTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun QuestCard(
    title: String,
    description: String,
    progress: Int,
    target: Int,
    unit: String,
    xpReward: Int,
    isCompleted: Boolean,
    isClaimed: Boolean,
    onClaim: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF171A21))
            .border(1.dp, Color(0xFF252A36), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = OpalTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OpalMint.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+$xpReward XP",
                            color = OpalMint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = OpalTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (progress.toFloat() / target.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp)),
                    color = OpalMint,
                    trackColor = Color(0xFF252B36)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$progress / $target $unit",
                    color = OpalTextSecondary,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isClaimed) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E232C))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Claimed",
                        color = OpalMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isCompleted) {
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OpalMint,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "Claim",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "In Progress",
                    color = OpalTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun BadgeDetailDialog(
    badge: PrimeBadge,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF13161C),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262C38)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OpalTextSecondary
                        )
                    }
                }

                // Enlarged 130dp Hexagonal Shield Medal
                PrimeHexagonBadge(
                    badge = badge,
                    badgeSize = 130.dp,
                    showLabel = false
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = badge.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${badge.level.title} · ${badge.rarity.label}",
                        color = badge.rarity.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• +${badge.xpValue} XP",
                        color = OpalMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1D24))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = badge.description,
                            color = OpalTextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = {
                                (badge.currentProgress / badge.targetRequirement).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = OpalMint,
                            trackColor = Color(0xFF282F3C)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progress: ${badge.currentProgress.toInt()} / ${badge.targetRequirement.toInt()} ${badge.unit}",
                                color = OpalTextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (badge.isUnlocked) "Status: Unlocked" else "Status: Locked",
                                color = if (badge.isUnlocked) OpalMint else Color(0xFFFFB74D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (badge.isUnlocked && badge.unlockedDate != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Unlocked: ${badge.unlockedDate}",
                        color = OpalTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Pin / Unpin from Showcase
                OutlinedButton(
                    onClick = onTogglePin,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isPinned) OpalMint else OpalTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isPinned) OpalMint else OpalTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPinned) "Pinned in Showcase" else "Pin to Showcase (Max 3)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

enum class BadgeStatusFilter(val title: String) {
    ALL("All"),
    GOTTEN("Gotten"),
    LOCKED("Locked")
}

@Composable
private fun BadgeStatusChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) OpalMint else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) OpalMint else Color(0xFF2A2E38),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else OpalTextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
