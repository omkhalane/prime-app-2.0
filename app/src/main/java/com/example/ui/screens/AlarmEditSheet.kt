package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlarmItem
import com.example.model.AlarmMode
import com.example.model.AlarmRecurrence
import com.example.model.ChallengeCategory
import com.example.model.ChallengeConfig
import com.example.model.ChallengeDifficulty
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    initialAlarm: AlarmItem,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onOpenRingtonePicker: () -> Unit,
    onTestChallenge: (ChallengeConfig) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    var hour by remember { mutableIntStateOf(initialAlarm.hour) }
    var minute by remember { mutableIntStateOf(initialAlarm.minute) }
    var isAm by remember { mutableStateOf(initialAlarm.hour < 12) }

    var name by remember { mutableStateOf(initialAlarm.name) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var recurrence by remember { mutableStateOf(initialAlarm.recurrence) }
    var selectedDays by remember { mutableStateOf(initialAlarm.selectedDays) }
    var specificDateMillis by remember { mutableStateOf(initialAlarm.specificDateMillis) }
    var vibrationEnabled by remember { mutableStateOf(initialAlarm.vibrationEnabled) }
    var snoozeMinutes by remember { mutableIntStateOf(initialAlarm.snoozeDurationMinutes) }
    var alarmMode by remember { mutableStateOf(initialAlarm.alarmMode) }

    // Challenge Config
    var challengeCategory by remember { mutableStateOf(initialAlarm.challengeConfig.category) }
    var challengeDifficulty by remember { mutableStateOf(initialAlarm.challengeConfig.difficulty) }
    var requiredConsecutive by remember { mutableIntStateOf(initialAlarm.challengeConfig.requiredConsecutive) }
    var randomizeCategory by remember { mutableStateOf(initialAlarm.challengeConfig.isRandomizedCategory) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16181C),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(scrollState)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCreating) "Add Alarm" else "Edit Alarm",
                    color = OpalTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isCreating && onDelete != null) {
                        IconButton(
                            onClick = { onDelete(initialAlarm.id) },
                            modifier = Modifier.testTag("delete_alarm_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Alarm",
                                tint = Color(0xFFFF5252)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OpalTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Time Selector (Samsung Clock Style Stepper & AM/PM toggle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1B1E22))
                    .border(1.dp, OpalCardBorder, RoundedCornerShape(24.dp))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hour selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val displayHour = if (hour % 12 == 0) 12 else hour % 12
                        Text(
                            text = String.format("%02d", displayHour),
                            color = OpalTextPrimary,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable {
                                    val nextH = (hour + 1) % 24
                                    hour = nextH
                                    isAm = nextH < 12
                                }
                                .testTag("alarm_hour_picker")
                        )
                        Text(
                            text = "Tap to change",
                            color = OpalTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = ":",
                        color = OpalTextSecondary,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%02d", minute),
                            color = OpalTextPrimary,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable {
                                    minute = (minute + 5) % 60
                                }
                                .testTag("alarm_minute_picker")
                        )
                        Text(
                            text = "Tap to change",
                            color = OpalTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // AM / PM Pill Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAm) Color(0xFFB388FF) else Color(0xFF262A30))
                                .clickable {
                                    if (!isAm) {
                                        isAm = true
                                        hour = (hour - 12).coerceAtLeast(0)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AM",
                                color = if (isAm) Color.Black else OpalTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isAm) Color(0xFFB388FF) else Color(0xFF262A30))
                                .clickable {
                                    if (isAm) {
                                        isAm = false
                                        hour = (hour + 12).coerceAtMost(23)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PM",
                                color = if (!isAm) Color.Black else OpalTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Alarm Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Alarm Name", color = OpalTextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFB388FF),
                    unfocusedBorderColor = OpalCardBorder,
                    focusedContainerColor = OpalCardBg,
                    unfocusedContainerColor = OpalCardBg
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alarm_name_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Recurrence Tabs
            Text(
                text = "Recurrence",
                color = OpalTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlarmRecurrence.values().forEach { rec ->
                    val isSelected = recurrence == rec
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFFB388FF).copy(alpha = 0.22f) else OpalCardBg)
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFB388FF) else OpalCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                recurrence = rec
                                when (rec) {
                                    AlarmRecurrence.DAILY -> selectedDays = setOf(1, 2, 3, 4, 5, 6, 7)
                                    AlarmRecurrence.WEEKDAYS -> selectedDays = setOf(1, 2, 3, 4, 5)
                                    AlarmRecurrence.WEEKENDS -> selectedDays = setOf(6, 7)
                                    else -> {}
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rec.displayName,
                            color = if (isSelected) Color(0xFFD1C4E9) else OpalTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // If Recurrence is CUSTOM, show Day of Week Chips
            if (recurrence == AlarmRecurrence.CUSTOM) {
                Spacer(modifier = Modifier.height(14.dp))
                val daysLabels = listOf(
                    1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysLabels.forEach { (dayIndex, label) ->
                        val isDaySelected = selectedDays.contains(dayIndex)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDaySelected) Color(0xFFB388FF) else Color(0xFF22262B))
                                .clickable {
                                    selectedDays = if (isDaySelected) {
                                        if (selectedDays.size > 1) selectedDays - dayIndex else selectedDays
                                    } else {
                                        selectedDays + dayIndex
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isDaySelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // If Recurrence is ONCE, option to select a specific date via calendar
            if (recurrence == AlarmRecurrence.ONCE) {
                Spacer(modifier = Modifier.height(14.dp))
                val cal = Calendar.getInstance()
                specificDateMillis?.let { cal.timeInMillis = it }
                val dateStr = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(cal.time)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(OpalCardBg)
                        .border(1.dp, OpalCardBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            val nowCal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val picked = Calendar.getInstance().apply {
                                        set(y, m, d, hour, minute, 0)
                                    }
                                    specificDateMillis = picked.timeInMillis
                                },
                                nowCal.get(Calendar.YEAR),
                                nowCal.get(Calendar.MONTH),
                                nowCal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Date",
                            tint = Color(0xFFB388FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Calendar Date",
                                color = OpalTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (specificDateMillis != null) dateStr else "Next occurrence (Tomorrow)",
                                color = OpalTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = "Change",
                        color = Color(0xFFB388FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Ringtone Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OpalCardBg)
                    .border(1.dp, OpalCardBorder, RoundedCornerShape(16.dp))
                    .clickable { onOpenRingtonePicker() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Ringtone",
                        tint = Color(0xFFB388FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ringtone",
                            color = OpalTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = initialAlarm.ringtoneName,
                            color = OpalTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = "Select",
                    color = Color(0xFFB388FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vibration Switch
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Vibrate",
                        tint = OpalTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Vibrate when ringing",
                        color = OpalTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Switch(
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFB388FF),
                        uncheckedThumbColor = OpalTextSecondary,
                        uncheckedTrackColor = Color(0xFF262A30)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Alarm Mode (Normal, Wake-Up, Hard Mode)
            Text(
                text = "Alarm Mode",
                color = OpalTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlarmMode.values().forEach { mode ->
                    val isSelected = alarmMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) {
                                    if (mode == AlarmMode.HARD_MODE) Color(0xFF3B1515) else Color(0xFF261A36)
                                } else OpalCardBg
                            )
                            .border(
                                1.dp,
                                if (isSelected) {
                                    if (mode == AlarmMode.HARD_MODE) Color(0xFFFF5252) else Color(0xFFB388FF)
                                } else OpalCardBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { alarmMode = mode }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) Color.White else OpalTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Challenge Configurations (Visible if Wake-Up or Hard Mode)
            if (alarmMode != AlarmMode.NORMAL) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1826))
                        .border(1.dp, Color(0xFFB388FF).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Challenge",
                                    tint = Color(0xFFB388FF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Challenge Settings",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Test Challenge Button
                            Button(
                                onClick = {
                                    val currentConfig = ChallengeConfig(
                                        category = challengeCategory,
                                        difficulty = challengeDifficulty,
                                        requiredConsecutive = requiredConsecutive.coerceIn(1, 5),
                                        isRandomizedCategory = randomizeCategory
                                    )
                                    onTestChallenge(currentConfig)
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB388FF),
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("test_challenge_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleOutline,
                                    contentDescription = "Test",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Challenge", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Chips
                        Text("Category", color = OpalTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChallengeCategory.values().forEach { cat ->
                                val isCatSel = challengeCategory == cat
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCatSel) Color(0xFFB388FF).copy(alpha = 0.2f) else Color(0xFF262030))
                                        .border(
                                            1.dp,
                                            if (isCatSel) Color(0xFFB388FF) else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { challengeCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.displayName,
                                        color = if (isCatSel) Color.White else OpalTextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCatSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isCatSel) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFB388FF))
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Difficulty & Consecutive Required
                        Text("Difficulty", color = OpalTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ChallengeDifficulty.values().forEach { diff ->
                                val isDiffSel = challengeDifficulty == diff
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDiffSel) Color(0xFFB388FF) else Color(0xFF262030))
                                        .clickable { challengeDifficulty = diff }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = diff.displayName,
                                        color = if (isDiffSel) Color.Black else OpalTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Consecutive Answers: $requiredConsecutive",
                                color = OpalTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..5).forEach { count ->
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (requiredConsecutive == count) Color(0xFFB388FF) else Color(0xFF262030)
                                            )
                                            .clickable { requiredConsecutive = count },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$count",
                                            color = if (requiredConsecutive == count) Color.Black else OpalTextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons (Save & Cancel)
            validationError?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFFF8A80),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

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
                        .height(52.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val configured = initialAlarm.copy(
                            hour = hour.coerceIn(0, 23),
                            minute = minute.coerceIn(0, 59),
                            name = name.trim().take(40).ifBlank { "Alarm" },
                            isEnabled = true,
                            recurrence = recurrence,
                            selectedDays = selectedDays.filter { it in 1..7 }.toSet().ifEmpty { setOf(1, 2, 3, 4, 5) },
                            specificDateMillis = if (recurrence == AlarmRecurrence.ONCE) specificDateMillis else null,
                            vibrationEnabled = vibrationEnabled,
                            snoozeDurationMinutes = snoozeMinutes.coerceIn(1, 30),
                            alarmMode = alarmMode,
                            challengeConfig = ChallengeConfig(
                                category = challengeCategory,
                                difficulty = challengeDifficulty,
                                requiredConsecutive = requiredConsecutive,
                                isRandomizedCategory = randomizeCategory
                            )
                        )
                        val scheduledAt = specificDateMillis?.let { selectedDate ->
                            Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }
                        if (recurrence == AlarmRecurrence.ONCE && scheduledAt != null && scheduledAt <= System.currentTimeMillis()) {
                            validationError = "Choose a future date and time for a one-time alarm."
                        } else {
                            validationError = null
                            onSave(configured)
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_alarm_button")
                ) {
                    Text("Save Alarm", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
