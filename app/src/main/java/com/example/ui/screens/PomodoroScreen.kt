package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmSoundManager
import com.example.model.PomodoroSessionType
import com.example.model.PomodoroSettings
import com.example.model.PomodoroState
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimeGlassIconButton
import com.example.ui.components.PrimePrimaryButton
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
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
fun PomodoroScreen(
    state: PomodoroState,
    settings: PomodoroSettings,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onOpenSettings: () -> Unit,
    onApplyQuickPreset: (focusMin: Int, breakMin: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var sessionSoundEnabled by remember { mutableStateOf(AlarmSoundManager.isSessionSoundEnabled) }

    val sessionAccentColor = when (state.sessionType) {
        PomodoroSessionType.FOCUS -> Color(0xFFFF7043)
        PomodoroSessionType.SHORT_BREAK -> PrimeInfo
        PomodoroSessionType.LONG_BREAK -> Color(0xFFB388FF)
    }

    val progressFraction = if (state.totalSessionSeconds > 0) {
        1.0f - (state.remainingSeconds.toFloat() / state.totalSessionSeconds.toFloat())
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "pomodoro_progress"
    )

    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height * 0.38f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        sessionAccentColor.copy(alpha = if (state.isRunning) 0.18f else 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = size.width * 0.65f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                    testTag = "pomodoro_back_button"
                )

                Text(
                    text = "Pomodoro",
                    color = PrimeTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrimeGlassIconButton(
                        icon = if (sessionSoundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = if (sessionSoundEnabled) "Mute session sounds" else "Enable session sounds",
                        onClick = {
                            sessionSoundEnabled = !sessionSoundEnabled
                            AlarmSoundManager.isSessionSoundEnabled = sessionSoundEnabled
                        },
                        containerSize = 40.dp,
                        iconSize = 20.dp,
                        testTag = "pomodoro_sound_toggle"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    PrimeGlassIconButton(
                        icon = Icons.Rounded.MoreVert,
                        contentDescription = "Pomodoro Settings",
                        onClick = onOpenSettings,
                        containerSize = 40.dp,
                        iconSize = 20.dp,
                        testTag = "pomodoro_settings_menu_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Session Badge Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(sessionAccentColor.copy(alpha = 0.16f))
                    .border(BorderStroke(1.dp, sessionAccentColor.copy(alpha = 0.45f)), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (state.sessionType == PomodoroSessionType.FOCUS) {
                            Icons.Rounded.HourglassBottom
                        } else {
                            Icons.Rounded.Spa
                        },
                        contentDescription = state.sessionType.displayName,
                        tint = sessionAccentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.sessionType.displayName.uppercase(),
                        color = sessionAccentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Circular Countdown Dial
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = PrimeSurfaceHigher,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                sessionAccentColor.copy(alpha = 0.7f),
                                sessionAccentColor
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormatted,
                        color = PrimeTextPrimary,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-1).sp,
                        modifier = Modifier.testTag("pomodoro_digital_clock")
                    )

                    Text(
                        text = when {
                            !state.isRunning -> "Ready to focus"
                            state.isPaused -> "Paused"
                            state.sessionType == PomodoroSessionType.FOCUS -> "Deep in flow"
                            else -> "Rest & recharge"
                        },
                        color = PrimeTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Round & Cycle Tracker Cards
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ROUND",
                            color = PrimeTextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.currentRound} / ${settings.roundsPerCycle}",
                            color = PrimeTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CYCLE",
                            color = PrimeTextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.currentCycle} / ${settings.totalCycles}",
                            color = PrimeTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Round Dots Visualizer
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (r in 1..settings.roundsPerCycle) {
                    val isPast = r < state.currentRound
                    val isCurrent = r == state.currentRound
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isCurrent) 24.dp else 10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    isCurrent -> sessionAccentColor
                                    isPast -> sessionAccentColor.copy(alpha = 0.5f)
                                    else -> PrimeSurfaceHigher
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Controls
            if (!state.isRunning) {
                PrimePrimaryButton(
                    text = "Start Focus",
                    onClick = onStart,
                    icon = Icons.Rounded.PlayArrow,
                    modifier = Modifier.fillMaxWidth(0.85f),
                    height = 54.dp,
                    shape = RoundedCornerShape(27.dp),
                    testTag = "pomodoro_start_button"
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(PrimeGlassCardBg)
                            .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), CircleShape)
                            .clickable { onReset() }
                            .testTag("pomodoro_reset_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset",
                            tint = PrimeTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = if (state.isPaused) onResume else onPause,
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isPaused) sessionAccentColor else Color.White,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("pomodoro_pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = if (state.isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isPaused) "Resume" else "Pause",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(PrimeGlassCardBg)
                            .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), CircleShape)
                            .clickable { onSkip() }
                            .testTag("pomodoro_skip_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Skip Session",
                            tint = PrimeTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (!state.isRunning) {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Quick Presets",
                        color = PrimeTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PomodoroPresetChip(
                        title = "25 / 5",
                        subtitle = "Classic",
                        isSelected = settings.focusMinutes == 25 && settings.shortBreakMinutes == 5,
                        onClick = { onApplyQuickPreset(25, 5) },
                        modifier = Modifier.weight(1f)
                    )

                    PomodoroPresetChip(
                        title = "50 / 10",
                        subtitle = "Deep Flow",
                        isSelected = settings.focusMinutes == 50 && settings.shortBreakMinutes == 10,
                        onClick = { onApplyQuickPreset(50, 10) },
                        modifier = Modifier.weight(1f)
                    )

                    PomodoroPresetChip(
                        title = "15 / 3",
                        subtitle = "Sprint",
                        isSelected = settings.focusMinutes == 15 && settings.shortBreakMinutes == 3,
                        onClick = { onApplyQuickPreset(15, 3) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun PomodoroPresetChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF2C1914) else PrimeGlassCardBg)
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFFFF7043) else PrimeSubtleBorder
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = if (isSelected) Color(0xFFFF8A65) else PrimeTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = PrimeTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
