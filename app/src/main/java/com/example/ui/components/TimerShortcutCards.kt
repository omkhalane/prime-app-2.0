package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlarmItem
import com.example.model.PomodoroSettings
import com.example.ui.theme.PrimeGlassBorder
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeSurfaceBorderBrush
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary

@Composable
fun TimerShortcutCards(
    pomodoroSettings: PomodoroSettings,
    stopwatchElapsedFormatted: String,
    nextAlarm: AlarmItem?,
    onOpenPomodoro: () -> Unit,
    onOpenStopwatch: () -> Unit,
    onOpenAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section header label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Focus Tools",
                color = PrimeTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tap to launch",
                color = PrimeTextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 3 Cards horizontally distributed in a Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Pomodoro Card
            PomodoroShortcutCard(
                focusMinutes = pomodoroSettings.focusMinutes,
                breakMinutes = pomodoroSettings.shortBreakMinutes,
                onClick = onOpenPomodoro,
                modifier = Modifier.weight(1f)
            )

            // 2. Stopwatch Card
            StopwatchShortcutCard(
                elapsedFormatted = stopwatchElapsedFormatted,
                onClick = onOpenStopwatch,
                modifier = Modifier.weight(1f)
            )

            // 3. Alarm Card
            AlarmShortcutCard(
                nextAlarm = nextAlarm,
                onClick = onOpenAlarm,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PomodoroShortcutCard(
    focusMinutes: Int,
    breakMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "pomodoro_scale"
    )

    val pomodoroOrange = Color(0xFFFF6E40)

    Box(
        modifier = modifier
            .scale(scale)
            .height(148.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF241410),
                        Color(0xFF180E0B),
                        Color(0xFF100907)
                    )
                )
            )
            .border(BorderStroke(1.dp, pomodoroOrange.copy(alpha = 0.35f)), RoundedCornerShape(22.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .testTag("shortcut_pomodoro_card")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(pomodoroOrange.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.25f),
                    radius = w * 0.65f
                )
            )

            drawArc(
                color = pomodoroOrange.copy(alpha = 0.16f),
                startAngle = 135f,
                sweepAngle = 200f,
                useCenter = false,
                topLeft = Offset(w * 0.35f, -h * 0.15f),
                size = Size(w * 0.9f, w * 0.9f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(pomodoroOrange.copy(alpha = 0.2f))
                    .border(BorderStroke(1.dp, pomodoroOrange.copy(alpha = 0.5f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.HourglassBottom,
                    contentDescription = "Pomodoro",
                    tint = pomodoroOrange,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = "Pomodoro",
                    color = PrimeTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${focusMinutes}m Focus\n${breakMinutes}m Break",
                    color = pomodoroOrange.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StopwatchShortcutCard(
    elapsedFormatted: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "stopwatch_scale"
    )

    val stopwatchCyan = Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .scale(scale)
            .height(148.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B1F28),
                        Color(0xFF07141A),
                        Color(0xFF050C10)
                    )
                )
            )
            .border(BorderStroke(1.dp, stopwatchCyan.copy(alpha = 0.35f)), RoundedCornerShape(22.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .testTag("shortcut_stopwatch_card")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(stopwatchCyan.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.25f),
                    radius = w * 0.65f
                )
            )

            drawArc(
                color = stopwatchCyan.copy(alpha = 0.16f),
                startAngle = 200f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.4f, -h * 0.1f),
                size = Size(w * 0.85f, w * 0.85f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(stopwatchCyan.copy(alpha = 0.2f))
                    .border(BorderStroke(1.dp, stopwatchCyan.copy(alpha = 0.5f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = "Stopwatch",
                    tint = stopwatchCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = "Stopwatch",
                    color = PrimeTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (elapsedFormatted.isNotBlank() && elapsedFormatted != "00:00.00") {
                        elapsedFormatted
                    } else {
                        "00:00.00\nPrecision Laps"
                    },
                    color = stopwatchCyan.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AlarmShortcutCard(
    nextAlarm: AlarmItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "alarm_scale"
    )

    val alarmPurple = Color(0xFFB388FF)

    val formattedNextAlarm = nextAlarm?.let {
        val h12 = if (it.hour % 12 == 0) 12 else it.hour % 12
        val amPm = if (it.hour < 12) "AM" else "PM"
        String.format("%d:%02d %s", h12, it.minute, amPm)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(148.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E132D),
                        Color(0xFF130C1D),
                        Color(0xFF0C0712)
                    )
                )
            )
            .border(BorderStroke(1.dp, alarmPurple.copy(alpha = 0.35f)), RoundedCornerShape(22.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .testTag("shortcut_alarm_card")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(alarmPurple.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.25f),
                    radius = w * 0.65f
                )
            )

            drawArc(
                color = alarmPurple.copy(alpha = 0.16f),
                startAngle = 160f,
                sweepAngle = 170f,
                useCenter = false,
                topLeft = Offset(w * 0.4f, -h * 0.1f),
                size = Size(w * 0.85f, w * 0.85f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(alarmPurple.copy(alpha = 0.2f))
                    .border(BorderStroke(1.dp, alarmPurple.copy(alpha = 0.5f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = "Alarms",
                    tint = alarmPurple,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = "Alarm",
                    color = PrimeTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (formattedNextAlarm != null) {
                        "$formattedNextAlarm\nUpcoming"
                    } else {
                        "No alarms\nTap to add"
                    },
                    color = alarmPurple.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
