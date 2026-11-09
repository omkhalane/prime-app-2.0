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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import com.example.data.AppSpecificMetric
import com.example.data.DayUsagePoint
import com.example.data.RealScreenTimeTracker
import com.example.model.BlockedAppItem
import com.example.ui.components.AppIconBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimeGlassIconButton
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeGlassBorder
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeInfo
import com.example.ui.theme.PrimeSurfaceBorderBrush
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AppSpecificReportScreen(
    metric: AppSpecificMetric,
    onBackClick: () -> Unit,
    onSetAppTimerClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    BackHandler {
        onBackClick()
    }

    val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
    val weekRangeStr = rememberCurrentWeekRangeString()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .testTag("app_specific_report_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Bar: < [App Name] with real app icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimeGlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBackClick,
                containerSize = 40.dp,
                iconSize = 20.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            AppIconBadge(
                item = BlockedAppItem(
                    id = metric.packageName,
                    name = metric.appName,
                    packageName = metric.packageName,
                    isWebsite = false
                ),
                size = 34.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = metric.appName,
                color = PrimeTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Subheader: Current week range | "Daily ▾"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = weekRangeStr,
                color = PrimeTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimeSurfaceHigher)
                    .border(BorderStroke(1.dp, PrimeGlassBorder), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily",
                    color = PrimeInfo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = PrimeInfo,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Card 1: Screen time
        val maxScreenMs = (metric.dailyScreenTime.maxOrNull() ?: 0L).coerceAtLeast(4L * 3600 * 1000)
        val maxHours = ((maxScreenMs / (3600 * 1000L)) + 1).coerceAtLeast(4)

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Text(
                    text = "${RealScreenTimeTracker.formatDuration(metric.dailyAvgTimeMs)} / day",
                    color = PrimeTextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${RealScreenTimeTracker.formatDuration(metric.weeklyTotalTimeMs)} total this week",
                    color = PrimeTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                SingleColorBarChart(
                    dataPoints = metric.dailyScreenTime.mapIndexed { i, time ->
                        DayUsagePoint(
                            dayLabel = listOf("S", "M", "T", "W", "T", "F", "S")[i],
                            dayIndex = i,
                            screenTimeMs = time,
                            notifications = 0,
                            unlocks = 0,
                            isToday = (i == todayDow)
                        )
                    },
                    yAxisLabels = listOf("${maxHours} h", "${maxHours / 2} h"),
                    valueExtractor = { it.screenTimeMs },
                    maxValue = maxHours * 3600 * 1000L,
                    barColor = PrimeInfo
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom link: App timer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSetAppTimerClick() }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App timer",
                        color = PrimeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = PrimeTextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Notifications
        val maxNotif = (metric.dailyNotifications.maxOrNull() ?: 0).coerceAtLeast(3)

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Text(
                    text = "${metric.dailyAvgNotifications} notifications / day",
                    color = PrimeTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${metric.weeklyNotifications} notifications total this week",
                    color = PrimeTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                SingleColorBarChart(
                    dataPoints = metric.dailyNotifications.mapIndexed { i, count ->
                        DayUsagePoint(
                            dayLabel = listOf("S", "M", "T", "W", "T", "F", "S")[i],
                            dayIndex = i,
                            screenTimeMs = 0L,
                            notifications = count,
                            unlocks = 0,
                            isToday = (i == todayDow)
                        )
                    },
                    yAxisLabels = listOf("$maxNotif", "${(maxNotif * 2) / 3}", "${maxNotif / 3}"),
                    valueExtractor = { it.notifications.toLong() },
                    maxValue = maxNotif.toLong(),
                    barColor = PrimeInfo
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNotificationSettingsClick() }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notification settings",
                        color = PrimeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = PrimeTextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 3: App opens
        val maxOpens = (metric.dailyAppOpens.maxOrNull() ?: 0).coerceAtLeast(10)

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Text(
                    text = "${metric.dailyAvgAppOpens} app opens / day",
                    color = PrimeTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${metric.weeklyAppOpens} app opens total this week",
                    color = PrimeTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                SingleColorBarChart(
                    dataPoints = metric.dailyAppOpens.mapIndexed { i, count ->
                        DayUsagePoint(
                            dayLabel = listOf("S", "M", "T", "W", "T", "F", "S")[i],
                            dayIndex = i,
                            screenTimeMs = 0L,
                            notifications = 0,
                            unlocks = count,
                            isToday = (i == todayDow)
                        )
                    },
                    yAxisLabels = listOf("$maxOpens", "${maxOpens / 2}"),
                    valueExtractor = { it.unlocks.toLong() },
                    maxValue = maxOpens.toLong(),
                    barColor = PrimeInfo
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

private fun rememberCurrentWeekRangeString(): String {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    val startDay = cal.get(Calendar.DAY_OF_MONTH)
    val startMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)

    cal.add(Calendar.DAY_OF_WEEK, 6)
    val endDay = cal.get(Calendar.DAY_OF_MONTH)
    val endMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)

    return if (startMonth == endMonth) {
        "$startDay–$endDay $endMonth"
    } else {
        "$startDay $startMonth – $endDay $endMonth"
    }
}
