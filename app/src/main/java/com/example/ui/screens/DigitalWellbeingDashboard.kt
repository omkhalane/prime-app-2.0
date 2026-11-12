package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
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
import com.example.data.AppUsageData
import com.example.data.CategoryUsage
import com.example.data.RealScreenTimeTracker
import com.example.model.BlockedAppItem
import com.example.ui.components.AppIconBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimeGlassButton
import com.example.ui.components.PrimeGlassIconButton
import com.example.ui.components.PrimePrimaryButton
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

@Composable
fun DigitalWellbeingDashboard(
    totalScreenTime: String,
    totalScreenTimeMillis: Long,
    categories: List<CategoryUsage>,
    topApps: List<AppUsageData>,
    hasUsagePermission: Boolean,
    errorMessage: String? = null,
    onBackClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onRetryFetch: () -> Unit,
    onOpenDailyDashboard: () -> Unit,
    onOpenWeeklyReport: () -> Unit,
    onAppClick: (String) -> Unit,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    BackHandler {
        onBackClick()
    }

    val totalMs = if (totalScreenTimeMillis > 0) totalScreenTimeMillis
                  else categories.sumOf { it.totalTimeMs }
    val displayTotal = if (totalScreenTime.isNotBlank() && totalScreenTime != "Permission Required") totalScreenTime
                       else if (totalMs > 0) RealScreenTimeTracker.formatDuration(totalMs)
                       else "0m"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .testTag("digital_wellbeing_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                PrimeGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBackClick,
                    containerSize = 44.dp,
                    iconSize = 20.dp
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimeGlassIconButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = "Refresh",
                    onClick = onRetryFetch,
                    containerSize = 40.dp,
                    iconSize = 18.dp,
                    tint = PrimeTextSecondary
                )

                PrimeGlassIconButton(
                    icon = Icons.Rounded.BarChart,
                    contentDescription = "Weekly Report",
                    onClick = onOpenWeeklyReport,
                    containerSize = 40.dp,
                    iconSize = 20.dp
                )

                PrimeGlassIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "Menu",
                    onClick = onOpenDailyDashboard,
                    containerSize = 40.dp,
                    iconSize = 20.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Native-style page heading: keep the analytics content primary.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Digital Wellbeing",
                color = PrimeTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your device activity",
                color = PrimeTextSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Permission / Fetch Error State Card
        if (!hasUsagePermission || errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(PrimeGlassCardBg)
                    .border(BorderStroke(1.dp, PrimeError.copy(alpha = 0.45f)), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "Error fetching",
                            tint = PrimeError,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (!hasUsagePermission) "Usage Access Permission Required" else "Error Fetching Data",
                            color = PrimeTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = errorMessage ?: "Android requires Usage Access permission to read real screen time and per-app usage stats. Without this permission, real metrics cannot be fetched.",
                        color = PrimeTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimePrimaryButton(
                            text = "Open Settings",
                            onClick = onRequestPermission,
                            modifier = Modifier.weight(1f),
                            height = 42.dp
                        )

                        PrimeGlassButton(
                            text = "Retry Fetch",
                            onClick = onRetryFetch,
                            modifier = Modifier.weight(1f),
                            height = 42.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 1. Screen Time Hero Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            onClick = onOpenDailyDashboard
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayTotal,
                        color = PrimeTextPrimary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = "View Details",
                        tint = PrimeTextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Segmented Multi-Color Progress Bar
                if (categories.isNotEmpty() && totalMs > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(PrimeSurfaceHigher)
                    ) {
                        categories.forEach { cat ->
                            val weight = cat.totalTimeMs.toFloat().coerceAtLeast(0.001f)
                            Box(
                                modifier = Modifier
                                    .weight(weight)
                                    .height(18.dp)
                                    .background(Color(cat.colorHex))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Category Legends with exact times
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        categories.forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(cat.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = cat.categoryName,
                                        color = PrimeTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = RealScreenTimeTracker.formatDuration(cat.totalTimeMs),
                                    color = PrimeTextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(PrimeSurfaceHigher)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (!hasUsagePermission) "Usage data unavailable until permission is granted" else "No foreground app activity recorded yet today",
                        color = PrimeTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Most Used Apps Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Most used apps",
                        color = PrimeTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Weekly report ›",
                        color = PrimeAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onOpenWeeklyReport() }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (topApps.isNotEmpty()) {
                    val displayApps = topApps.take(3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        displayApps.forEach { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onAppClick(app.packageName) }
                                    .padding(8.dp)
                            ) {
                                AppIconBadge(
                                    item = BlockedAppItem(
                                        id = app.packageName,
                                        name = app.appName,
                                        packageName = app.packageName,
                                        isWebsite = false
                                    ),
                                    size = 50.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = app.appName,
                                    color = PrimeTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = RealScreenTimeTracker.formatDuration(app.totalTimeMs),
                                    color = PrimeTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (!hasUsagePermission) "Permission needed to list real installed apps" else "No app usage recorded yet today",
                            color = PrimeTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Screen Time Goal Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            onClick = onOpenWeeklyReport
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimeSurfaceHigher)
                            .border(BorderStroke(1.dp, PrimeGlassBorder), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Screen Time Goal",
                            tint = PrimeAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Screen time goal",
                            color = PrimeTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Daily limit: 4h 00m • On track today",
                            color = PrimeTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = PrimeTextTertiary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
