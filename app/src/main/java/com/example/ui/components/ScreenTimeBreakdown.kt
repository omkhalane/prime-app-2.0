package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.BlockedAppItem
import com.example.ui.theme.PrimeAccent
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

@Composable
fun ScreenTimeBreakdown(
    totalScreenTime: String,
    totalScreenTimeMillis: Long,
    apps: List<BlockedAppItem>,
    hasUsagePermission: Boolean,
    onRequestUsagePermission: () -> Unit,
    onRefreshStats: () -> Unit,
    onOpenDigitalWellbeing: () -> Unit,
    onAppClick: (BlockedAppItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val usedApps = apps.filter { !it.isWebsite && it.usageMillis > 0 }
        .sortedByDescending { it.usageMillis }
        .take(5)

    val maxUsageMillis = (usedApps.firstOrNull()?.usageMillis ?: 1L).coerceAtLeast(1L)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PrimeGlassCardBg)
            .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), RoundedCornerShape(24.dp))
            .clickable { onOpenDigitalWellbeing() }
            .padding(20.dp)
            .testTag("screen_time_breakdown_card")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Screen time",
                    color = PrimeTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Dashboard",
                        color = PrimeAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = "Open Dashboard",
                        tint = PrimeAccent,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    PrimeGlassIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "Refresh Screen Time",
                        onClick = onRefreshStats,
                        containerSize = 32.dp,
                        iconSize = 16.dp,
                        tint = PrimeTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = totalScreenTime,
                    color = PrimeTextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Today",
                    color = PrimeTextTertiary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (!hasUsagePermission) {
                // Banner inviting user to grant Usage Access
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(PrimeSurfaceHigher)
                        .border(BorderStroke(1.dp, PrimeAccent.copy(alpha = 0.35f)), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = PrimeAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Usage Access Required",
                                color = PrimeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Grant Usage Access in Android Settings so Prime can read your real foreground app stats and calculate screen time accurately.",
                            color = PrimeTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PrimePrimaryButton(
                            text = "Enable in Settings",
                            onClick = onRequestUsagePermission,
                            height = 40.dp,
                            testTag = "grant_usage_permission_button"
                        )
                    }
                }
            } else if (usedApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimeSurfaceHigher)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = PrimeAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Usage access active",
                            color = PrimeTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to open Prime Digital Wellbeing for weekly and app-specific reports!",
                            color = PrimeTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                // Real app items with exact usage bars
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    usedApps.forEach { app ->
                        AppUsageRow(
                            app = app,
                            maxUsageMillis = maxUsageMillis,
                            onClick = { onAppClick(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageRow(
    app: BlockedAppItem,
    maxUsageMillis: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconBadge(item = app, size = 32.dp)
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.name,
                    color = PrimeTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                if (app.isBlocked) {
                    Text(
                        text = "BLOCKED",
                        color = PrimeError,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Proportional Progress bar
            val fraction = (app.usageMillis.toFloat() / maxUsageMillis.toFloat()).coerceIn(0.08f, 1.0f)
            val barColor = if (app.isBlocked || app.category == "Distracting") {
                PrimeError
            } else {
                PrimeAccent
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PrimeSurfaceHigher)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = app.usageToday.replace(" today", ""),
            color = PrimeTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
