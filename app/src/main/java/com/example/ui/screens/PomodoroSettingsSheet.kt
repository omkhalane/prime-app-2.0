package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PomodoroSettings
import com.example.ui.theme.OpalBlack
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsSheet(
    initialSettings: PomodoroSettings,
    onDismiss: () -> Unit,
    onSave: (PomodoroSettings) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    var focusMinutes by remember { mutableIntStateOf(initialSettings.focusMinutes) }
    var shortBreakMinutes by remember { mutableIntStateOf(initialSettings.shortBreakMinutes) }
    var longBreakMinutes by remember { mutableIntStateOf(initialSettings.longBreakMinutes) }
    var roundsPerCycle by remember { mutableIntStateOf(initialSettings.roundsPerCycle) }
    var totalCycles by remember { mutableIntStateOf(initialSettings.totalCycles) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16181C),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pomodoro Settings",
                    color = OpalTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = OpalTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Focus Duration
            StepperSettingRow(
                title = "Focus Duration",
                subtitle = "Length of each deep work block",
                valueText = "${focusMinutes} min",
                onDecrement = { if (focusMinutes > 1) focusMinutes -= 1 },
                onIncrement = { if (focusMinutes < 120) focusMinutes += 1 },
                accentColor = Color(0xFFFF7043),
                testTagPrefix = "focus_duration"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Short Break
            StepperSettingRow(
                title = "Short Break",
                subtitle = "Rest between normal focus rounds",
                valueText = "${shortBreakMinutes} min",
                onDecrement = { if (shortBreakMinutes > 1) shortBreakMinutes -= 1 },
                onIncrement = { if (shortBreakMinutes < 45) shortBreakMinutes += 1 },
                accentColor = Color(0xFF29B6F6),
                testTagPrefix = "short_break"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Long Break
            StepperSettingRow(
                title = "Long Break",
                subtitle = "Reward after completing cycle rounds",
                valueText = "${longBreakMinutes} min",
                onDecrement = { if (longBreakMinutes > 1) longBreakMinutes -= 1 },
                onIncrement = { if (longBreakMinutes < 60) longBreakMinutes += 1 },
                accentColor = Color(0xFFAB47BC),
                testTagPrefix = "long_break"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rounds per Cycle
            StepperSettingRow(
                title = "Focus Rounds per Cycle",
                subtitle = "Sessions before trigger of long break",
                valueText = "$roundsPerCycle rounds",
                onDecrement = { if (roundsPerCycle > 1) roundsPerCycle -= 1 },
                onIncrement = { if (roundsPerCycle < 12) roundsPerCycle += 1 },
                accentColor = OpalMint,
                testTagPrefix = "rounds_count"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Complete Cycles
            StepperSettingRow(
                title = "Complete Cycles",
                subtitle = "How many full sequences to run",
                valueText = "$totalCycles cycle${if (totalCycles > 1) "s" else ""}",
                onDecrement = { if (totalCycles > 1) totalCycles -= 1 },
                onIncrement = { if (totalCycles < 10) totalCycles += 1 },
                accentColor = Color(0xFFFFCA28),
                testTagPrefix = "cycles_count"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Restore defaults row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        focusMinutes = 25
                        shortBreakMinutes = 5
                        longBreakMinutes = 15
                        roundsPerCycle = 4
                        totalCycles = 1
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Restore Defaults",
                    tint = OpalTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Restore Defaults (25 / 5 / 15 · 4 rounds)",
                    color = OpalTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Save & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val validated = PomodoroSettings(
                            focusMinutes = focusMinutes.coerceIn(1, 120),
                            shortBreakMinutes = shortBreakMinutes.coerceIn(1, 45),
                            longBreakMinutes = longBreakMinutes.coerceIn(1, 60),
                            roundsPerCycle = roundsPerCycle.coerceIn(1, 12),
                            totalCycles = totalCycles.coerceIn(1, 10)
                        )
                        onSave(validated)
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_pomodoro_settings_button")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepperSettingRow(
    title: String,
    subtitle: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    accentColor: Color,
    testTagPrefix: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OpalCardBg)
            .border(1.dp, OpalCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = OpalTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = OpalTextSecondary,
                fontSize = 11.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22262B))
                    .clickable { onDecrement() }
                    .testTag("${testTagPrefix}_decrement"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valueText,
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22262B))
                    .clickable { onIncrement() }
                    .testTag("${testTagPrefix}_increment"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
