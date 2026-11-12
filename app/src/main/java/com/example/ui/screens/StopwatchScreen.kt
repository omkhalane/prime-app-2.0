package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StopwatchLap
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
fun StopwatchScreen(
    elapsedMillis: Long,
    isRunning: Boolean,
    laps: List<StopwatchLap>,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit,
    themeId: String = "midnight",
    modifier: Modifier = Modifier
) {
    val fastestLapNumber = if (laps.size >= 2) {
        laps.minByOrNull { it.lapTimeMillis }?.lapNumber
    } else null

    val slowestLapNumber = if (laps.size >= 2) {
        laps.maxByOrNull { it.lapTimeMillis }?.lapNumber
    } else null

    val hours = (elapsedMillis / (1000 * 60 * 60))
    val minutes = (elapsedMillis / (1000 * 60)) % 60
    val seconds = (elapsedMillis / 1000) % 60
    val centis = (elapsedMillis % 1000) / 10

    val mainTimeFormatted = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
    val centisFormatted = String.format(".%02d", centis)

    val themeBackground = when (themeId) {
        "ember" -> listOf(Color(0xFF241719), Color(0xFF351C20), PrimeBackground)
        "ocean" -> listOf(Color(0xFF102B3B), Color(0xFF173C4B), PrimeBackground)
        "forest" -> listOf(Color(0xFF15352B), Color(0xFF1E493A), PrimeBackground)
        "lavender" -> listOf(Color(0xFF2D2144), Color(0xFF392B55), PrimeBackground)
        else -> listOf(Color(0xFF101827), Color(0xFF1B2638), PrimeBackground)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(themeBackground))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                    containerSize = 44.dp,
                    iconSize = 20.dp,
                    testTag = "stopwatch_back_button"
                )

                Text(
                    text = "Stopwatch",
                    color = PrimeTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Box(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Digital Stopwatch Large Display with Liquid Glass
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(PrimeGlassCardBg)
                    .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), RoundedCornerShape(32.dp))
                    .padding(vertical = 36.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = mainTimeFormatted,
                        color = PrimeTextPrimary,
                        fontSize = if (hours > 0) 46.sp else 58.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.testTag("stopwatch_digital_time")
                    )
                    Text(
                        text = centisFormatted,
                        color = PrimeAccent,
                        fontSize = if (hours > 0) 28.sp else 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .testTag("stopwatch_digital_centis")
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(0.92f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRunning && elapsedMillis == 0L) {
                    PrimePrimaryButton(
                        text = "Start",
                        onClick = onStart,
                        icon = Icons.Rounded.PlayArrow,
                        modifier = Modifier.fillMaxWidth(),
                        height = 54.dp,
                        shape = RoundedCornerShape(27.dp),
                        testTag = "stopwatch_start_button"
                    )
                } else if (isRunning) {
                    Button(
                        onClick = onLap,
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimeSurfaceHigher,
                            contentColor = PrimeTextPrimary
                        ),
                        border = BorderStroke(1.dp, PrimeGlassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("stopwatch_lap_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Flag,
                            contentDescription = "Lap",
                            tint = PrimeAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lap", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onPause,
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimeError.copy(alpha = 0.2f),
                            contentColor = PrimeError
                        ),
                        border = BorderStroke(1.dp, PrimeError.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("stopwatch_pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onReset,
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimeSurfaceHigher,
                            contentColor = PrimeTextPrimary
                        ),
                        border = BorderStroke(1.dp, PrimeGlassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("stopwatch_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset", fontWeight = FontWeight.SemiBold)
                    }

                    PrimePrimaryButton(
                        text = "Resume",
                        onClick = onResume,
                        icon = Icons.Rounded.PlayArrow,
                        modifier = Modifier.weight(1f),
                        height = 54.dp,
                        shape = RoundedCornerShape(27.dp),
                        testTag = "stopwatch_resume_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Lap History Table
            if (laps.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAP",
                        color = PrimeTextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "LAP TIME",
                        color = PrimeTextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "TOTAL TIME",
                        color = PrimeTextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.0f)
                    )
                }

                HorizontalDivider(color = PrimeSubtleBorder, thickness = 1.dp)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(laps) { lap ->
                        val isFastest = lap.lapNumber == fastestLapNumber
                        val isSlowest = lap.lapNumber == slowestLapNumber

                        val lapColor = when {
                            isFastest -> PrimeAccent
                            isSlowest -> PrimeError
                            else -> PrimeTextPrimary
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1.1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("%02d", lap.lapNumber),
                                    color = PrimeTextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isFastest) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PrimeAccent.copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "FASTEST",
                                            color = PrimeAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else if (isSlowest) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PrimeError.copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SLOWEST",
                                            color = PrimeError,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = formatLapDuration(lap.lapTimeMillis),
                                color = lapColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1.1f)
                            )

                            Text(
                                text = formatLapDuration(lap.totalTimeMillis),
                                color = PrimeTextSecondary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.0f)
                            )
                        }
                        HorizontalDivider(color = PrimeSubtleBorder, thickness = 0.5.dp)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Press Lap while running to record split times",
                        color = PrimeTextTertiary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun formatLapDuration(millis: Long): String {
    val m = (millis / (1000 * 60)) % 60
    val s = (millis / 1000) % 60
    val cs = (millis % 1000) / 10
    return String.format("%02d:%02d.%02d", m, s, cs)
}
