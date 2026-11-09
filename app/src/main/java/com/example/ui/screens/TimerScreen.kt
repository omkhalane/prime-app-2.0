package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.alarm.AlarmSchedulerHelper
import com.example.model.TimerPreset
import com.example.ui.components.DigitalClockView
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimeGlassIconButton
import com.example.ui.components.PrimePrimaryButton
import com.example.ui.components.TimerShortcutCards
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
import com.example.viewmodel.OpalUiState

@Composable
fun TimerScreen(
    uiState: OpalUiState,
    onAdjustMinutes: (Int) -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onToggleBlockApps: () -> Unit,
    onOpenBlockAppsSheet: () -> Unit = {},
    onSelectPreset: (TimerPreset) -> Unit,
    onOpenPomodoro: () -> Unit,
    onOpenStopwatch: () -> Unit,
    onOpenAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val presetsScrollState = rememberScrollState()

    val swMillis = uiState.stopwatchState.elapsedMillis
    val swHours = (swMillis / (1000 * 60 * 60))
    val swMins = (swMillis / (1000 * 60)) % 60
    val swSecs = (swMillis / 1000) % 60
    val swCentis = (swMillis % 1000) / 10
    val stopwatchElapsedFormatted = if (swHours > 0) {
        String.format("%02d:%02d:%02d", swHours, swMins, swSecs)
    } else {
        String.format("%02d:%02d.%02d", swMins, swSecs, swCentis)
    }

    val nowMillis = System.currentTimeMillis()
    val nextAlarm = uiState.alarms.filter { it.isEnabled }
        .mapNotNull { alarm ->
            val next = AlarmSchedulerHelper.calculateNextTriggerMillis(alarm, nowMillis)
            if (next != null) alarm to next else null
        }
        .minByOrNull { it.second }?.first

    val formattedTime = if (uiState.isTimerRunning) {
        val minutes = uiState.timerRemainingSeconds / 60
        val seconds = uiState.timerRemainingSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    } else {
        String.format("%02d:00", uiState.timerSelectedMinutes)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
    ) {
        // Focus Sanctuary Hero Backdrop at top
        val activeHeroDrawable = uiState.activePreset?.drawableResId ?: R.drawable.img_timer_hero
        Image(
            painter = painterResource(id = activeHeroDrawable),
            contentDescription = uiState.activePresetTitle ?: "Timer Focus Backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .align(Alignment.TopCenter)
        )

        // Gradient fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.6f to PrimeBackground.copy(alpha = 0.6f),
                        1.0f to PrimeBackground
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title row with active preset indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timer",
                    color = PrimeTextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.testTag("timer_screen_title")
                )

                if (uiState.activePresetTitle != null) {
                    val themeColor = uiState.activePreset?.let { Color(it.themeColorHex) } ?: PrimeAccent
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimeGlassCardBg)
                            .border(BorderStroke(1.dp, themeColor.copy(alpha = 0.6f)), RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = uiState.activePresetTitle,
                            color = themeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preset name prominently displayed directly above the timer clock
            if (uiState.activePresetTitle != null) {
                val themeColor = uiState.activePreset?.let { Color(it.themeColorHex) } ?: PrimeAccent
                Box(
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PrimeGlassCardBg)
                        .border(BorderStroke(1.dp, themeColor.copy(alpha = 0.8f)), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("timer_active_preset_above_clock")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(themeColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.activePresetTitle.uppercase(),
                            color = themeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        uiState.activePreset?.category?.let { cat ->
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• $cat",
                                color = PrimeTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Digital Clock View
            DigitalClockView(formattedTime = formattedTime)

            Spacer(modifier = Modifier.height(26.dp))

            // Stepper controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Minus button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimeGlassCardBg)
                        .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), CircleShape)
                        .clickable(enabled = !uiState.isTimerRunning) { onAdjustMinutes(-5) }
                        .testTag("timer_minus_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Minus 5 minutes",
                        tint = if (uiState.isTimerRunning) PrimeTextTertiary else PrimeTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Middle pill showing selected duration
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(PrimeGlassCardBg)
                        .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.isTimerRunning) {
                            if (uiState.isTimerPaused) "Paused" else "In Progress"
                        } else {
                            "${uiState.timerSelectedMinutes}m"
                        },
                        color = PrimeTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Plus button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimeGlassCardBg)
                        .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), CircleShape)
                        .clickable(enabled = !uiState.isTimerRunning) { onAdjustMinutes(5) }
                        .testTag("timer_plus_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Plus 5 minutes",
                        tint = if (uiState.isTimerRunning) PrimeTextTertiary else PrimeTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Start/Pause/Stop Buttons
            if (!uiState.isTimerRunning) {
                PrimePrimaryButton(
                    text = "Start Focus",
                    onClick = onStartTimer,
                    icon = Icons.Rounded.PlayArrow,
                    modifier = Modifier.fillMaxWidth(0.85f),
                    height = 54.dp,
                    shape = RoundedCornerShape(27.dp),
                    testTag = "timer_start_button"
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onPauseTimer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isTimerPaused) PrimeAccent else PrimeSurfaceHigher,
                            contentColor = if (uiState.isTimerPaused) PrimeBackground else PrimeTextPrimary
                        ),
                        shape = RoundedCornerShape(27.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("timer_pause_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isTimerPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = if (uiState.isTimerPaused) "Resume" else "Pause",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isTimerPaused) "Resume" else "Pause",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onStopTimer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimeError.copy(alpha = 0.2f),
                            contentColor = PrimeError
                        ),
                        border = BorderStroke(1.dp, PrimeError.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(27.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("timer_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = "Stop",
                            tint = PrimeError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stop",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Block Apps Pill Toggle & Configuration
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141414))
                    .border(BorderStroke(1.dp, Color(0xFF262626)), RoundedCornerShape(20.dp))
                    .clickable { onOpenBlockAppsSheet() }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
                    .testTag("timer_block_apps_pill"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Block Apps",
                    tint = if (uiState.timerBlockAppsEnabled) PrimeAccent else PrimeTextTertiary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val blockStatusText = if (!uiState.timerBlockAppsEnabled) {
                    "Block apps: Disabled"
                } else if (uiState.timerSessionBlockedAppIds.isNotEmpty()) {
                    "Block apps: +${uiState.timerSessionBlockedAppIds.size} session added"
                } else {
                    "Block apps: Shields only"
                }
                Text(
                    text = blockStatusText,
                    color = if (uiState.timerBlockAppsEnabled) PrimeTextPrimary else PrimeTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Focus Tools Shortcut Cards
            TimerShortcutCards(
                pomodoroSettings = uiState.pomodoroSettings,
                stopwatchElapsedFormatted = stopwatchElapsedFormatted,
                nextAlarm = nextAlarm,
                onOpenPomodoro = onOpenPomodoro,
                onOpenStopwatch = onOpenStopwatch,
                onOpenAlarm = onOpenAlarm
            )

            Spacer(modifier = Modifier.height(28.dp))

            // "For you" Presets Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "For you",
                    color = PrimeTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                )

                Text(
                    text = "${uiState.presets.size} modes",
                    color = PrimeTextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Presets Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(presetsScrollState),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.presets.forEach { preset ->
                    val isActive = uiState.activePreset?.id == preset.id && uiState.isTimerRunning
                    PresetCard(
                        preset = preset,
                        isActive = isActive,
                        onClick = { onSelectPreset(preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun PresetCard(
    preset: TimerPreset,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = Color(preset.themeColorHex)
    val cardBorder = if (isActive) {
        BorderStroke(2.dp, themeColor)
    } else {
        BorderStroke(1.dp, themeColor.copy(alpha = 0.4f))
    }

    Box(
        modifier = modifier
            .width(210.dp)
            .height(260.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(PrimeGlassCardBg)
            .border(cardBorder, RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .testTag("preset_card_${preset.id}")
    ) {
        // Preset-specific artwork
        Image(
            painter = painterResource(id = preset.drawableResId),
            contentDescription = preset.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.35f to Color.Transparent,
                        0.75f to PrimeBackground.copy(alpha = 0.85f),
                        1.0f to PrimeBackground
                    )
                )
        )

        // Top category badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141414))
                .border(BorderStroke(1.dp, Color(0xFF262626)), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = preset.category.uppercase(),
                color = PrimeTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Active running indicator badge on top right
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimeAccent)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = preset.title,
                color = PrimeTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = preset.formattedDuration,
                color = PrimeTextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Start / Launch pill with clean Opal styling
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) PrimeAccent else Color(0xFF181818))
                    .border(BorderStroke(1.dp, if (isActive) PrimeAccent else Color(0xFF2E2E2E)), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Start",
                        tint = if (isActive) Color.Black else PrimeTextPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isActive) "Active Focus" else "Open Theme",
                        color = if (isActive) Color.Black else PrimeTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
