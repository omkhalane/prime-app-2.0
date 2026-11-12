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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.ui.components.PrimeBackHeader
import com.example.ui.components.PrimeGlassIconButton
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeError
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary
import com.example.vpn.db.BlockingScheduleEntity
import com.example.vpn.db.CategoryEntity

@Composable
fun BlockingScheduleScreen(
    schedules: List<BlockingScheduleEntity>,
    onBackClick: () -> Unit,
    onAddSchedule: (title: String, startH: Int, startM: Int, endH: Int, endM: Int, days: Set<Int>, categories: Set<String>, startDateIso: String?, endDateIso: String?) -> Unit,
    categories: List<CategoryEntity> = emptyList(),
    onToggleSchedule: (String, Boolean) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAddDialogOpen by remember { mutableStateOf(false) }

    BackHandler {
        onBackClick()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .testTag("blocking_schedule_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        PrimeBackHeader(
            title = "Schedules",
            subtitle = "Automate your website protection.",
            onBackClick = onBackClick,
            trailing = {
                PrimeGlassIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "New Schedule",
                    onClick = { isAddDialogOpen = true },
                    containerSize = 44.dp,
                    iconSize = 20.dp
                )
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111111))
                .border(BorderStroke(1.dp, Color(0xFF222222)), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Automated schedules engage local website filtering automatically during your designated work and sleep hours. Survives phone restarts.",
                color = PrimeTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("schedules_lazy_column")
        ) {
            items(schedules, key = { it.id }) { schedule ->
                val startFormatted = String.format("%02d:%02d", schedule.startHour, schedule.startMinute)
                val endFormatted = String.format("%02d:%02d", schedule.endHour, schedule.endMinute)
                val activeDays = schedule.activeDaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(PrimeGlassCardBg)
                        .border(BorderStroke(1.dp, Color(0xFF1E1E1E)), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = schedule.title,
                                    color = PrimeTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$startFormatted – $endFormatted",
                                    color = PrimeAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Switch(
                                    checked = schedule.isEnabled,
                                    onCheckedChange = { onToggleSchedule(schedule.id, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = PrimeAccent,
                                        uncheckedThumbColor = PrimeTextSecondary,
                                        uncheckedTrackColor = PrimeSurfaceHigher
                                    )
                                )

                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = PrimeError.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { onDeleteSchedule(schedule.id) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Day of Week Pills
                        val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            dayLabels.forEachIndexed { index, label ->
                                val isActive = activeDays.contains(index)
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) PrimeAccent.copy(alpha = 0.2f) else Color(0xFF181818))
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (isActive) PrimeAccent else Color(0xFF282828)
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isActive) PrimeAccent else PrimeTextTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (schedule.startDateIso != null || schedule.endDateIso != null) {
                            Spacer(Modifier.height(6.dp))
                            Text("Dates: ${schedule.startDateIso ?: "Any"} – ${schedule.endDateIso ?: "Any"}", color = PrimeTextTertiary, fontSize = 11.sp)
                        }
                        if (schedule.blockedCategoriesCsv.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Blocks: " + schedule.blockedCategoriesCsv.replace(",", ", "),
                                color = PrimeTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (schedules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No automated schedules yet",
                                color = PrimeTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap + to create a recurring website blocking schedule",
                                color = PrimeTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (isAddDialogOpen) {
        var scheduleTitle by remember { mutableStateOf("Deep Work Hours") }
        var startH by remember { mutableStateOf(9) }
        var startM by remember { mutableStateOf(0) }
        var endH by remember { mutableStateOf(17) }
        var endM by remember { mutableStateOf(0) }
        var startTimeText by remember { mutableStateOf("09:00") }
        var endTimeText by remember { mutableStateOf("17:00") }
        var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) } // Weekdays (Mon-Fri)
        var selectedCats by remember { mutableStateOf(setOf("Social Media", "Video Streaming", "Entertainment")) }
        var startDateIso by remember { mutableStateOf("") }
        var endDateIso by remember { mutableStateOf("") }

        androidx.compose.ui.window.Dialog(onDismissRequest = { isAddDialogOpen = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF141414))
                    .border(BorderStroke(1.dp, Color(0xFF262626)), RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Create Blocking Schedule",
                        color = PrimeTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = scheduleTitle,
                        onValueChange = { scheduleTitle = it },
                        label = { Text("Schedule Name", color = PrimeTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedBorderColor = PrimeAccent,
                            unfocusedBorderColor = Color(0xFF2C2C2C),
                            focusedTextColor = PrimeTextPrimary,
                            unfocusedTextColor = PrimeTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Date range (optional, yyyy-MM-dd)", color = PrimeTextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = startDateIso, onValueChange = { startDateIso = it.take(10) }, label = { Text("From date") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = endDateIso, onValueChange = { endDateIso = it.take(10) }, label = { Text("To date") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))

                    Text("Repeat", color = PrimeTextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Every day" to (0..6).toSet(), "Weekdays" to (1..5).toSet(), "Weekends" to setOf(0, 6)).forEach { (label, days) ->
                            Text(label, color = if (selectedDays == days) Color.Black else PrimeTextPrimary, fontSize = 10.sp,
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selectedDays == days) PrimeAccent else Color(0xFF222222))
                                    .clickable { selectedDays = days }.padding(vertical = 9.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Or choose days", color = PrimeTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        dayLabels.forEachIndexed { index, label ->
                            val isSelected = selectedDays.contains(index)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) PrimeAccent else Color(0xFF1E1E1E))
                                    .clickable {
                                        selectedDays = if (isSelected) selectedDays - index else selectedDays + index
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else PrimeTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Spacer(Modifier.height(12.dp))
                    Text("Blocked categories", color = PrimeTextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    (categories.map { it.name }.ifEmpty { selectedCats.toList() }).forEach { categoryName ->
                        Row(Modifier.fillMaxWidth().clickable { selectedCats = if (categoryName in selectedCats) selectedCats - categoryName else selectedCats + categoryName }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = categoryName in selectedCats, onCheckedChange = { checked -> selectedCats = if (checked) selectedCats + categoryName else selectedCats - categoryName })
                            Spacer(Modifier.width(8.dp))
                            Text(categoryName, color = PrimeTextPrimary, fontSize = 13.sp)
                        }
                    }

                    OutlinedTextField(value = startTimeText, onValueChange = { startTimeText = it.take(5) }, label = { Text("Start time (HH:mm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = endTimeText, onValueChange = { endTimeText = it.take(5) }, label = { Text("End time (HH:mm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Time Window: $startTimeText – $endTimeText",
                        color = PrimeAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222222))
                                .clickable { isAddDialogOpen = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", color = PrimeTextSecondary, fontSize = 14.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimeAccent)
                                .clickable {
                                    val startParts = startTimeText.split(":").mapNotNull { it.toIntOrNull() }
                                    val endParts = endTimeText.split(":").mapNotNull { it.toIntOrNull() }
                                    val dateFormatOk = (startDateIso.isBlank() || startDateIso.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) &&
                                        (endDateIso.isBlank() || endDateIso.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) &&
                                        (startDateIso.isBlank() || endDateIso.isBlank() || startDateIso <= endDateIso)
                                    if (startParts.size == 2 && endParts.size == 2 && startParts[0] in 0..23 && endParts[0] in 0..23 && startParts[1] in 0..59 && endParts[1] in 0..59 && selectedDays.isNotEmpty() && dateFormatOk) {
                                        onAddSchedule(scheduleTitle, startParts[0], startParts[1], endParts[0], endParts[1], selectedDays, selectedCats, startDateIso.ifBlank { null }, endDateIso.ifBlank { null })
                                        isAddDialogOpen = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Save Schedule", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
