package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.PrimeScoreCalculator
import com.example.model.PrimeScoreBreakdown
import com.example.model.PrimeTargets
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary
import kotlin.math.roundToInt

@Composable
fun PrimeTargetSettingsDialog(
    currentTargets: PrimeTargets,
    currentScoreBreakdown: PrimeScoreBreakdown,
    onSaveTargets: (screenMin: Int, focusMin: Int, blockingMin: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var screenTarget by remember { mutableIntStateOf(currentTargets.screenTimeTargetMinutes) }
    var focusTarget by remember { mutableIntStateOf(currentTargets.focusTargetMinutes) }
    var blockingTarget by remember { mutableIntStateOf(currentTargets.blockingTargetMinutes) }

    // Live recalculation with temporary targets
    val simulatedBreakdown = remember(screenTarget, focusTarget, blockingTarget) {
        PrimeScoreCalculator.computeScore(
            actualScreenMinutes = currentScoreBreakdown.actualScreenMinutes,
            verifiedFocusMinutes = currentScoreBreakdown.actualFocusMinutes,
            verifiedBlockedMinutes = currentScoreBreakdown.actualBlockingMinutes,
            targets = PrimeTargets(
                screenTimeTargetMinutes = screenTarget,
                focusTargetMinutes = focusTarget,
                blockingTargetMinutes = blockingTarget
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF14171D),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262C38)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(OpalMint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = OpalMint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Personalized Targets",
                            color = OpalTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OpalTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "PRIME normalizes your score based on your personal targets so everyone competes fairly.",
                    color = OpalTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C2028))
                        .border(1.dp, OpalMint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ESTIMATED PRIME SCORE",
                                color = OpalTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${simulatedBreakdown.totalScore}",
                                    color = OpalMint,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = " / 1,000",
                                    color = OpalTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(bottom = 5.dp, start = 4.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Rank: ${simulatedBreakdown.currentRank.title}",
                                color = simulatedBreakdown.currentRank.primaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = simulatedBreakdown.currentRank.award,
                                color = OpalTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Target 1: Screen Time Target (T)
                TargetSliderRow(
                    title = "Screen-Time Target (T)",
                    valueMinutes = screenTarget,
                    min = 60,
                    max = 480,
                    step = 15,
                    description = "Maintain at or below this to earn full 300 pts.",
                    onValueChange = { screenTarget = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Target 2: Focus Target (F_t)
                TargetSliderRow(
                    title = "Daily Focus Target (F_t)",
                    valueMinutes = focusTarget,
                    min = 30,
                    max = 360,
                    step = 15,
                    description = "Verified focus & pomodoro minutes to earn 350 pts.",
                    onValueChange = { focusTarget = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Target 3: Blocking Target (B_t)
                TargetSliderRow(
                    title = "Daily Blocking Target (B_t)",
                    valueMinutes = blockingTarget,
                    min = 30,
                    max = 480,
                    step = 15,
                    description = "Protected distraction-free minutes to earn 350 pts.",
                    onValueChange = { blockingTarget = it }
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Save button
                Button(
                    onClick = {
                        onSaveTargets(screenTarget, focusTarget, blockingTarget)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OpalMint,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "Save Daily Targets",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetSliderRow(
    title: String,
    valueMinutes: Int,
    min: Int,
    max: Int,
    step: Int,
    description: String,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = OpalTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            val hours = valueMinutes / 60
            val mins = valueMinutes % 60
            val formatted = if (hours > 0 && mins > 0) "${hours}h ${mins}m"
            else if (hours > 0) "${hours}h"
            else "${mins}m"

            Text(
                text = formatted,
                color = OpalMint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            color = OpalTextSecondary,
            fontSize = 11.sp
        )

        Slider(
            value = valueMinutes.toFloat(),
            onValueChange = { floatVal ->
                val rounded = ((floatVal / step).roundToInt() * step).coerceIn(min, max)
                onValueChange(rounded)
            },
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = OpalMint,
                activeTrackColor = OpalMint,
                inactiveTrackColor = Color(0xFF262C38)
            )
        )
    }
}
