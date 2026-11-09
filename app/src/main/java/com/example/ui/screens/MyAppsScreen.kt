package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockRule
import com.example.model.BlockedAppItem
import com.example.ui.components.AppIconBadge
import com.example.ui.components.GlassCard
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeError
import com.example.ui.theme.PrimeGlassBorder
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeSubtleBorder
import com.example.ui.theme.PrimeSurfaceBorderBrush
import com.example.ui.theme.PrimeSurfaceElevated
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary
import com.example.util.formatAppName
import com.example.viewmodel.OpalUiState

@Composable
fun MyAppsScreen(
    uiState: OpalUiState,
    onRuleClick: (BlockRule) -> Unit,
    onAddCustomShield: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onUnblockApp: (String) -> Unit,
    onBlockedAppClick: (BlockedAppItem) -> Unit,
    onOpenWebsiteBlocker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val rulesScrollState = rememberScrollState()
    val blockedScrollState = rememberScrollState()
    val categoriesScrollState = rememberScrollState()

    val blockedList = uiState.apps.filter { it.isBlocked }
    val distractingList = uiState.apps.filter { it.category == "Distracting" }
    val allowedList = uiState.apps.filter { it.category == "Always allowed" }
    val neverAllowedList = uiState.apps.filter { it.category == "Never allowed" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title: Shields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = PrimeAccent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Shields",
                    color = PrimeTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.testTag("shields_screen_title")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Blocked section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Blocked",
                    tint = if (blockedList.isNotEmpty()) PrimeAccent else PrimeTextTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Blocked (${blockedList.size})",
                    color = PrimeTextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Blocked items
        if (blockedList.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(blockedScrollState),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                blockedList.forEach { app ->
                    BlockedAppMiniLogo(
                        app = app,
                        onClick = { onBlockedAppClick(app) }
                    )
                }
            }
        } else {
            // Empty state placeholder
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                onClick = { onCategoryClick("Distracting") }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimeSurfaceHigher),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = PrimeTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No apps are blocked currently",
                        color = PrimeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap categories below to select distracting apps to block",
                        color = PrimeAccent,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Shields Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Shields",
                    color = PrimeTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimeSurfaceHigher)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${uiState.rules.size}",
                        color = PrimeAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Add Custom Shield Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimeGlassCardBg)
                    .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), RoundedCornerShape(16.dp))
                    .clickable { onAddCustomShield() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("add_custom_shield_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Custom Shield",
                        tint = PrimeAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Shield",
                        color = PrimeAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Shields Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rulesScrollState),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.rules.forEach { rule ->
                ShieldCard(
                    rule = rule,
                    blockedCount = blockedList.size,
                    onClick = { onRuleClick(rule) }
                )
            }

            // Create New Custom Shield Card
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(PrimeGlassCardBg)
                    .border(BorderStroke(1.dp, PrimeAccent.copy(alpha = 0.35f)), RoundedCornerShape(26.dp))
                    .clickable { onAddCustomShield() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PrimeSurfaceHigher)
                            .border(BorderStroke(1.dp, PrimeAccent.copy(alpha = 0.4f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "New Shield",
                            tint = PrimeAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "New Shield",
                        color = PrimeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Custom schedule",
                        color = PrimeTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Website Blocker Banner Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("website_blocker_entry_card"),
            shape = RoundedCornerShape(22.dp),
            onClick = onOpenWebsiteBlocker
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (uiState.isVpnRunning) PrimeAccent.copy(alpha = 0.15f)
                            else PrimeSurfaceHigher
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (uiState.isVpnRunning) PrimeAccent.copy(alpha = 0.4f)
                                else PrimeGlassBorder
                            ),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = "Website Blocker",
                        tint = if (uiState.isVpnRunning) PrimeAccent else PrimeTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Website Blocker",
                            color = PrimeTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isVpnRunning) PrimeAccent else Color(0xFF666666))
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (uiState.isVpnRunning) {
                            "Local VPN Active • ${uiState.activeWebsiteRulesCount} domains filtered"
                        } else {
                            "${uiState.activeWebsiteRulesCount} domains configured • Tap to manage"
                        },
                        color = PrimeTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = "Open Website Blocker",
                    tint = PrimeTextTertiary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Installed Apps Categories Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Device Apps & Categories",
                color = PrimeTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp
            )

            Text(
                text = "${uiState.apps.count { !it.isWebsite }} installed",
                color = PrimeTextSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Categories Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(categoriesScrollState),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Distracting Card
            AppCategoryCard(
                title = "Distracting",
                countLabel = "${distractingList.size} items",
                items = distractingList,
                onClick = { onCategoryClick("Distracting") },
                modifier = Modifier.width(152.dp)
            )

            // Always Allowed Card
            AppCategoryCard(
                title = "Always allowed",
                countLabel = "${allowedList.size} items",
                items = allowedList,
                onClick = { onCategoryClick("Always allowed") },
                modifier = Modifier.width(152.dp)
            )

            // Never Allowed Card
            NeverAllowedCategoryCard(
                count = neverAllowedList.size,
                items = neverAllowedList,
                onClick = { onCategoryClick("Never allowed") },
                modifier = Modifier.width(152.dp)
            )
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun BlockedAppMiniLogo(
    app: BlockedAppItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .width(54.dp)
            .testTag("blocked_mini_logo_${app.id}")
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PrimeSurfaceHigher)
                .border(BorderStroke(1.dp, PrimeGlassBorder), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            AppIconBadge(
                item = app,
                size = 48.dp,
                modifier = Modifier.drawWithContent {
                    drawContent()
                    drawRect(
                        color = Color.Gray,
                        blendMode = BlendMode.Color
                    )
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Locked",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = formatAppName(app.name),
            color = PrimeTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ShieldCard(
    rule: BlockRule,
    blockedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(180.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(PrimeGlassCardBg)
            .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .testTag("rule_card_${rule.id}")
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(PrimeSurfaceHigher)
                        .border(BorderStroke(1.dp, PrimeGlassBorder), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = PrimeAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "›",
                        color = PrimeTextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = PrimeAccent,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimeSurfaceHigher)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (rule.isPaused) "Paused" else rule.startsInText,
                        color = if (rule.isPaused) PrimeError else PrimeTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column {
                Text(
                    text = rule.title,
                    color = PrimeTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$blockedCount apps blocked\n${rule.daysLabel}",
                    color = PrimeTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun AppCategoryCard(
    title: String,
    countLabel: String,
    items: List<BlockedAppItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(180.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(PrimeGlassCardBg)
            .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .testTag("category_card_$title")
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val row1 = items.take(2)
                val row2 = items.drop(2).take(1)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row1.forEach { item ->
                        AppIconBadge(item = item, size = 36.dp)
                    }
                    if (row1.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimeSurfaceHigher)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row2.forEach { item ->
                        AppIconBadge(item = item, size = 36.dp)
                    }
                    val rem = (items.size - 3).coerceAtLeast(0)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimeSurfaceHigher),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$rem",
                            color = PrimeTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    color = PrimeTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = countLabel,
                    color = PrimeTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun NeverAllowedCategoryCard(
    count: Int,
    items: List<BlockedAppItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(180.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF262C38),
                        Color(0xFF1B202A)
                    )
                )
            )
            .border(BorderStroke(1.2.dp, Color(0xFF4C586E)), RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .testTag("category_card_Never_allowed")
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimeSurfaceHigher)
                            .border(BorderStroke(1.dp, PrimeAccent.copy(alpha = 0.5f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Strict Lock",
                            tint = PrimeAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141820))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "LOCKED",
                            color = PrimeAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val previewItems = items.take(2)
                    if (previewItems.isNotEmpty()) {
                        previewItems.forEach { app ->
                            AppIconBadge(item = app, size = 30.dp)
                        }
                    } else {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2A3240)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VisibilityOff,
                                    contentDescription = null,
                                    tint = PrimeTextSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column {
                Text(
                    text = "Never allowed",
                    color = PrimeTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$count items • Re-auth",
                    color = PrimeAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
