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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppUsageData
import com.example.data.DayUsagePoint
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
import com.example.ui.theme.PrimeInfo
import com.example.ui.theme.PrimeSubtleBorder
import com.example.ui.theme.PrimeSurfaceBorderBrush
import com.example.ui.theme.PrimeSurfaceElevated
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun WeeklyReportScreen(
    weeklyApps: List<AppUsageData>,
    trendPoints: List<DayUsagePoint>,
    heatmapMatrix: List<List<Int>>,
    hasUsagePermission: Boolean,
    errorMessage: String? = null,
    onBackClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onRetryFetch: () -> Unit,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    BackHandler {
        onBackClick()
    }

    val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
    val elapsedDays = (todayDow + 1).coerceAtLeast(1)
    val totalWeekMs = weeklyApps.sumOf { it.totalTimeMs }
    val dailyAvgMs = if (weeklyApps.isNotEmpty()) totalWeekMs / elapsedDays else 0L

    val dateRangeHeader = rememberCurrentWeekRangeString()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .testTag("weekly_report_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Navigation Bar
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
                    text = "Weekly report",
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

        Spacer(modifier = Modifier.height(14.dp))

        // Subtitle: Date range & Week number
        Text(
            text = dateRangeHeader,
            color = PrimeTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Error State Card if permission missing or query error
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
                            text = if (!hasUsagePermission) "Usage Access Permission Required" else "Error Fetching Weekly Data",
                            color = PrimeTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = errorMessage ?: "Android requires Usage Access permission to read 7-day usage statistics and hourly peak analysis.",
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

        // 1. Daily Average Screen Time Hero Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Text(
                    text = if (weeklyApps.isEmpty()) "No data" else RealScreenTimeTracker.formatDuration(dailyAvgMs),
                    color = PrimeTextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Daily average screen time",
                    color = PrimeTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (trendPoints.isNotEmpty()) {
                    WeeklyAverageBarChart(
                        dailyTimes = trendPoints.filter { it.dayIndex <= Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 }.map { it.screenTimeMs },
                        dailyAvgMs = dailyAvgMs
                    )
                } else {
                    Text("Weekly screen-time data unavailable", color = PrimeTextSecondary, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Top Category Spotlight
        val topCategoryName = weeklyApps.groupBy { it.category }
            .maxByOrNull { entry -> entry.value.sumOf { it.totalTimeMs } }?.key
        val topCategoryApps = topCategoryName?.let { category -> weeklyApps.filter { it.category == category } }.orEmpty()
        val categoryTotalMs = topCategoryApps.sumOf { it.totalTimeMs }

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
                        text = topCategoryName ?: "No usage data",
                        color = PrimeTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF6B6B).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "${topCategoryName ?: "No usage data"} category",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (weeklyApps.isEmpty()) "No data" else RealScreenTimeTracker.formatDuration(categoryTotalMs),
                    color = PrimeTextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (weeklyApps.isEmpty()) "Category average unavailable" else "${RealScreenTimeTracker.formatDuration(categoryTotalMs / elapsedDays)} daily average for this category",
                    color = PrimeTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (topCategoryApps.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        topCategoryApps.take(4).forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAppClick(app.packageName) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIconBadge(
                                        item = BlockedAppItem(
                                            id = app.packageName,
                                            name = app.appName,
                                            packageName = app.packageName,
                                            isWebsite = false
                                        ),
                                        size = 36.dp
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
                                    color = PrimeTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = if (!hasUsagePermission) "Permission needed to view category usage" else "No category activity tracked this week",
                        color = PrimeTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Peak Usage Times Heatmap Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Text(
                    text = "Peak usage times",
                    color = PrimeTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (trendPoints.isEmpty()) "No data" else RealScreenTimeTracker.formatDuration(totalWeekMs),
                        color = PrimeTextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Total screen time",
                        color = PrimeTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Heatmap 4-level legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val legendLevels = listOf(
                        "0" to Color(0xFF263238),
                        "1~14" to Color(0xFF1E88E5),
                        "15~29" to Color(0xFF0D47A1),
                        "30+" to PrimeAccent
                    )
                    legendLevels.forEach { (label, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = PrimeTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (heatmapMatrix.size == 7 && heatmapMatrix.all { it.size == 24 }) {
                    Heatmap24HourGrid(matrix = heatmapMatrix)
                } else {
                    Text("Hourly usage data unavailable", color = PrimeTextSecondary, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Carousel Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(PrimeTextPrimary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(PrimeSurfaceHigher))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(PrimeSurfaceHigher))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Most Used Apps Row
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Text(
                    text = "Most used apps",
                    color = PrimeTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (weeklyApps.isNotEmpty()) {
                    val top3 = weeklyApps.take(3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        top3.forEach { app ->
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
                            text = if (!hasUsagePermission) "Permission needed to load weekly app usage" else "No app usage recorded this week",
                            color = PrimeTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun WeeklyAverageBarChart(dailyTimes: List<Long>, dailyAvgMs: Long) {
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    val maxMs = (dailyTimes.maxOrNull() ?: 0L).coerceAtLeast(9L * 3600 * 1000)

    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        // Y-axis grid lines
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val topH = (maxMs / (3600 * 1000L)).toInt()
            val midH = (topH * 2) / 3
            val lowH = topH / 3
            listOf("${topH} h", "${midH} h", "${lowH} h").forEach { label ->
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

        // Dashed Red Goal/Average Line
        if (dailyAvgMs > 0 && maxMs > 0) {
            val avgFraction = (dailyAvgMs.toFloat() / maxMs.toFloat()).coerceIn(0.1f, 0.9f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.BottomStart)
                    .padding(bottom = (120 * avgFraction + 20).dp)
                    .height(1.5.dp)
                    .background(PrimeError.copy(alpha = 0.85f))
            )
        }

        // Bars for elapsed days only
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 44.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            dailyTimes.forEach { time ->
                val fraction = if (maxMs > 0) (time.toFloat() / maxMs.toFloat()).coerceIn(0.04f, 1f) else 0.04f
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height((110 * fraction).dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(PrimeInfo)
                )
            }
        }

        // Day Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 44.dp)
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dayLabels.take(dailyTimes.size).forEach { label ->
                Text(
                    text = label,
                    color = PrimeTextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun Heatmap24HourGrid(matrix: List<List<Int>>) {
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

    val colorLevels = listOf(
        PrimeSurfaceHigher,
        Color(0xFF2C394B),
        Color(0xFF1E88E5),
        PrimeAccent
    )

    if (matrix.size != 7 || matrix.any { it.size != 24 }) {
        Text("Hourly usage data unavailable", color = PrimeTextSecondary, fontSize = 13.sp)
        return
    }
    val rows = matrix

    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { dayIdx, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { intensity ->
                    val color = if (intensity < 0) PrimeBackground else colorLevels.getOrElse(intensity) { colorLevels.last() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(13.dp)
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dayLabels.getOrElse(dayIdx) { "" },
                    color = PrimeTextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.width(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("0", "6", "12", "18", "24 h").forEach { h ->
                Text(
                    text = h,
                    color = PrimeTextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun rememberCurrentWeekRangeString(): String {
    val cal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.SUNDAY }
    val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)

    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    val startDay = cal.get(Calendar.DAY_OF_MONTH)
    val startMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)

    cal.add(Calendar.DAY_OF_WEEK, 6)
    val endDay = cal.get(Calendar.DAY_OF_MONTH)
    val endMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)

    return if (startMonth == endMonth) {
        "$startDay–$endDay $endMonth (Week $weekOfYear)"
    } else {
        "$startDay $startMonth – $endDay $endMonth (Week $weekOfYear)"
    }
}
