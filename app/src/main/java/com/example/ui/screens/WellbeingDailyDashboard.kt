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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppUsageData
import com.example.data.DayUsagePoint
import com.example.data.RealScreenTimeTracker
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimeGlassButton
import com.example.ui.components.PrimeGlassIconButton
import com.example.ui.components.PrimePrimaryButton
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeError
import com.example.ui.theme.PrimeGlassBorder
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeInfo
import com.example.ui.theme.PrimeSubtleBorder
import com.example.ui.theme.PrimeSurfaceBorderBrush
import com.example.ui.theme.PrimeSurfaceElevated
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary

@Composable
fun WellbeingDailyDashboard(
    totalScreenTime: String,
    trendPoints: List<DayUsagePoint>,
    topApps: List<AppUsageData>,
    hasUsagePermission: Boolean,
    errorMessage: String? = null,
    onBackClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onRetryFetch: () -> Unit,
    onAppClick: (String) -> Unit,
    onOpenWeeklyReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isViewAllScreenTime by remember { mutableStateOf(false) }

    BackHandler {
        onBackClick()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .testTag("wellbeing_daily_dashboard")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Bar: < Dashboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimeGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBackClick,
                    containerSize = 40.dp,
                    iconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Dashboard",
                    color = PrimeTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
            }

            PrimeGlassIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                onClick = onRetryFetch,
                containerSize = 40.dp,
                iconSize = 18.dp,
                tint = PrimeTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Sub-bar: "Today" | "Categories"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today",
                color = PrimeTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Categories",
                color = PrimeAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenWeeklyReport() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error Banner if permission missing or query error
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
                        text = errorMessage ?: "Android requires Usage Access permission to read daily foreground app usage and device metrics.",
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
                            text = "Retry",
                            onClick = onRetryFetch,
                            modifier = Modifier.weight(1f),
                            height = 42.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Card 1: Screen Time Stacked Bar Chart
        val currentScreenTime = if (totalScreenTime.isNotBlank() && totalScreenTime != "Permission Required") totalScreenTime else "0m"
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Text(
                    text = currentScreenTime,
                    color = PrimeTextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                StackedBarChart(
                    dataPoints = trendPoints,
                    yAxisLabels = listOf("8 h", "4 h"),
                    valueExtractor = { it.screenTimeMs },
                    maxValue = (trendPoints.maxOfOrNull { it.screenTimeMs } ?: 0L).coerceAtLeast(8L * 3600 * 1000)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Real App breakdown list
                if (topApps.isNotEmpty()) {
                    val colors = listOf(0xFF00E59A, 0xFF65A9FF, 0xFFB388FF, 0xFFFFB547)
                    val visibleApps = if (isViewAllScreenTime) topApps else topApps.take(4)

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        visibleApps.forEachIndexed { idx, app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAppClick(app.packageName) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(colors[idx % colors.size]))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = app.appName,
                                        color = PrimeTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = RealScreenTimeTracker.formatDuration(app.totalTimeMs),
                                    color = PrimeTextSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (topApps.size > 4) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isViewAllScreenTime) "Collapse" else "View all",
                            color = PrimeAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clickable { isViewAllScreenTime = !isViewAllScreenTime }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (!hasUsagePermission) "Usage stats unavailable without permission" else "No foreground apps recorded today",
                            color = PrimeTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Notifications Stacked Bar Chart
        val totalNotifs = trendPoints.firstOrNull { it.isToday }?.notifications ?: 0
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Text(
                    text = "$totalNotifs notification interruptions",
                    color = PrimeTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                StackedBarChart(
                    dataPoints = trendPoints,
                    yAxisLabels = listOf("300", "200", "100"),
                    valueExtractor = { it.notifications.toLong() },
                    maxValue = (trendPoints.maxOfOrNull { it.notifications.toLong() } ?: 0L).coerceAtLeast(100L)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (totalNotifs > 0) "System notifications recorded today" else "No notification interruptions logged today",
                    color = PrimeTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 3: Unlocks Bar Chart
        val todayUnlocks = trendPoints.firstOrNull { it.isToday }?.unlocks ?: 0
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Text(
                    text = "$todayUnlocks unlocks",
                    color = PrimeTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                SingleColorBarChart(
                    dataPoints = trendPoints,
                    yAxisLabels = listOf("90", "60", "30"),
                    valueExtractor = { it.unlocks.toLong() },
                    maxValue = (trendPoints.maxOfOrNull { it.unlocks.toLong() } ?: 0L).coerceAtLeast(30L),
                    barColor = PrimeInfo
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun StackedBarChart(
    dataPoints: List<DayUsagePoint>,
    yAxisLabels: List<String>,
    valueExtractor: (DayUsagePoint) -> Long,
    maxValue: Long
) {
    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        // Y-axis labels & grid lines
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            yAxisLabels.forEach { label ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(PrimeGlassBorder)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        color = PrimeTextTertiary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PrimeGlassBorder)
            )
        }

        // 7 Bars
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 44.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            dataPoints.forEach { point ->
                val value = valueExtractor(point)
                val fraction = if (maxValue > 0) (value.toFloat() / maxValue.toFloat()).coerceIn(0.04f, 1f) else 0.04f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height((100 * fraction).dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(PrimeAccent)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(0.35f).fillMaxWidth().background(Color(0xFFE2E8F0)))
                            Box(modifier = Modifier.weight(0.25f).fillMaxWidth().background(Color(0xFFB388FF)))
                            Box(modifier = Modifier.weight(0.20f).fillMaxWidth().background(Color(0xFF65A9FF)))
                            Box(modifier = Modifier.weight(0.20f).fillMaxWidth().background(PrimeAccent))
                        }
                    }
                }
            }
        }

        // Bottom day labels with current day highlighted
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 44.dp)
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dataPoints.forEach { point ->
                if (point.isToday) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(PrimeSurfaceHigher)
                            .border(BorderStroke(1.dp, PrimeAccent.copy(alpha = 0.5f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = point.dayLabel,
                            color = PrimeAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = point.dayLabel,
                        color = PrimeTextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SingleColorBarChart(
    dataPoints: List<DayUsagePoint>,
    yAxisLabels: List<String>,
    valueExtractor: (DayUsagePoint) -> Long,
    maxValue: Long,
    barColor: Color = Color(0xFF65A9FF)
) {
    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            yAxisLabels.forEach { label ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(PrimeGlassBorder)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        color = PrimeTextTertiary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PrimeGlassBorder)
            )
        }

        // 7 Bars
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 44.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            dataPoints.forEach { point ->
                val value = valueExtractor(point)
                val fraction = if (maxValue > 0) (value.toFloat() / maxValue.toFloat()).coerceIn(0.04f, 1f) else 0.04f

                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height((100 * fraction).dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(barColor)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 44.dp)
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dataPoints.forEach { point ->
                if (point.isToday) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(PrimeSurfaceHigher)
                            .border(BorderStroke(1.dp, barColor.copy(alpha = 0.5f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = point.dayLabel,
                            color = barColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = point.dayLabel,
                        color = PrimeTextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
