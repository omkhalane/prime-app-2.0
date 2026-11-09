package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmSchedulerHelper
import com.example.model.AlarmItem
import com.example.model.AlarmMode
import com.example.model.AlarmRecurrence
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimeGlassIconButton
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmScreen(
    alarms: List<AlarmItem>,
    onBack: () -> Unit,
    onAddNewAlarm: () -> Unit,
    onEditAlarm: (AlarmItem) -> Unit,
    onToggleAlarm: (String, Boolean) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val liveDate = remember(currentTimeMillis) {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(currentTimeMillis))
    }
    val liveClock = remember(currentTimeMillis) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(currentTimeMillis))
    }

    val upcomingAlarmWithTrigger = remember(alarms, currentTimeMillis) {
        alarms.filter { it.isEnabled }
            .mapNotNull { alarm ->
                val next = AlarmSchedulerHelper.calculateNextTriggerMillis(alarm, currentTimeMillis)
                if (next != null) alarm to next else null
            }
            .minByOrNull { it.second }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimeGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    containerSize = 40.dp,
                    iconSize = 20.dp,
                    testTag = "alarm_screen_back_button"
                )

                Text(
                    text = "Alarms",
                    color = PrimeTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                PrimeGlassIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "Add Alarm",
                    onClick = onAddNewAlarm,
                    containerSize = 40.dp,
                    iconSize = 20.dp,
                    tint = Color(0xFFB388FF),
                    testTag = "add_alarm_top_button"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Time & Date Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = liveDate,
                        color = PrimeTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = liveClock,
                        color = PrimeTextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Upcoming Alarm Banner
            if (upcomingAlarmWithTrigger != null) {
                val (alarm, triggerMillis) = upcomingAlarmWithTrigger
                val remainingText = AlarmSchedulerHelper.formatRemainingTime(triggerMillis, currentTimeMillis)
                val triggerDateStr = SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(triggerMillis))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF26153D), Color(0xFF190C29))
                            )
                        )
                        .border(BorderStroke(1.dp, Color(0xFFB388FF).copy(alpha = 0.4f)), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFB388FF).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "Upcoming Alarm",
                                tint = Color(0xFFB388FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = remainingText,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Next: ${alarm.name} ($triggerDateStr)",
                                color = Color(0xFFD1C4E9),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No upcoming alarms scheduled",
                            color = PrimeTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Alarm List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Alarms (${alarms.size})",
                    color = PrimeTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Alarm Items List or Empty State
            if (alarms.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCardItem(
                            alarm = alarm,
                            onToggle = { isEnabled -> onToggleAlarm(alarm.id, isEnabled) },
                            onClick = { onEditAlarm(alarm) },
                            onDelete = { onDeleteAlarm(alarm.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PrimeSurfaceHigher)
                                .border(BorderStroke(1.dp, PrimeGlassBorder), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = "No Alarms",
                                tint = Color(0xFFB388FF),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Alarms Created",
                            color = PrimeTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the + button to schedule your first wake-up or focus alarm.",
                            color = PrimeTextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Alarm
        FloatingActionButton(
            onClick = onAddNewAlarm,
            containerColor = Color(0xFFB388FF),
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_alarm_fab")
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add Alarm",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AlarmCardItem(
    alarm: AlarmItem,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val h12 = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
    val amPm = if (alarm.hour < 12) "AM" else "PM"
    val timeFormatted = String.format("%02d:%02d", h12, alarm.minute)

    val recurrenceSummary = when (alarm.recurrence) {
        AlarmRecurrence.ONCE -> if (alarm.specificDateMillis != null) {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(alarm.specificDateMillis))
        } else "Once"
        AlarmRecurrence.DAILY -> "Every day"
        AlarmRecurrence.WEEKDAYS -> "Weekdays"
        AlarmRecurrence.WEEKENDS -> "Weekends"
        AlarmRecurrence.CUSTOM -> {
            val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
            alarm.selectedDays.sorted().mapNotNull { dayNames.getOrNull(it - 1) }.joinToString(" ")
        }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("alarm_item_${alarm.id}"),
        shape = RoundedCornerShape(22.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeFormatted,
                        color = if (alarm.isEnabled) PrimeTextPrimary else PrimeTextTertiary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = amPm,
                        color = if (alarm.isEnabled) Color(0xFFB388FF) else PrimeTextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = alarm.name,
                        color = if (alarm.isEnabled) PrimeTextPrimary else PrimeTextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "·",
                        color = PrimeTextTertiary,
                        fontSize = 13.sp
                    )

                    Text(
                        text = recurrenceSummary,
                        color = PrimeTextSecondary,
                        fontSize = 12.sp
                    )

                    if (alarm.alarmMode != AlarmMode.NORMAL) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (alarm.alarmMode == AlarmMode.HARD_MODE) {
                                        PrimeError.copy(alpha = 0.2f)
                                    } else {
                                        Color(0xFFB388FF).copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Psychology,
                                    contentDescription = "Challenge",
                                    tint = if (alarm.alarmMode == AlarmMode.HARD_MODE) PrimeError else Color(0xFFB388FF),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (alarm.alarmMode == AlarmMode.HARD_MODE) "HARD" else "WAKE-UP",
                                    color = if (alarm.alarmMode == AlarmMode.HARD_MODE) PrimeError else Color(0xFFB388FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Right side: Switch & Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = PrimeTextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFB388FF),
                        uncheckedThumbColor = PrimeTextTertiary,
                        uncheckedTrackColor = PrimeSurfaceHigher
                    ),
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }
        }
    }
}
