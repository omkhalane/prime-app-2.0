package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.model.BlockRule
import com.example.model.BlockedAppItem
import com.example.ui.components.StackedAppIcons
import com.example.ui.theme.OpalBlack
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalDanger
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary

@Composable
fun RuleEditSheet(
    rule: BlockRule,
    apps: List<BlockedAppItem>,
    onDismiss: () -> Unit,
    onSaveRule: (BlockRule) -> Unit,
    onSelectAppsClick: () -> Unit,
    onTogglePause: () -> Unit,
    onUpgradeProPrompt: () -> Unit,
    isCreating: Boolean = false,
    modifier: Modifier = Modifier
) {
    var fromTime by remember { mutableStateOf(rule.fromTime) }
    var toTime by remember { mutableStateOf(rule.toTime) }
    var activeDays by remember { mutableStateOf(rule.activeDays) }
    var isHardMode by remember { mutableStateOf(rule.isHardMode) }
    var allowBreaks by remember { mutableStateOf(rule.allowBreaks) }
    var ruleTitle by remember { mutableStateOf(rule.title) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var daysError by remember { mutableStateOf<String?>(null) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val blockedApps = apps.filter { it.id in rule.blockedAppIds }
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val scrollState = rememberScrollState()

    val availableTimes = listOf(
        "6:00 am", "7:00 am", "8:00 am", "9:00 am", "10:00 am", "11:00 am",
        "12:00 pm", "1:00 pm", "2:00 pm", "4:00 pm", "5:00 pm", "6:00 pm",
        "8:00 pm", "9:00 pm", "10:00 pm", "11:00 pm"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OpalBlack)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Header Bar: Back Button, Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22262B))
                        .clickable { onDismiss() }
                        .testTag("rule_edit_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OpalTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = if (isCreating) "Create Shield" else "Edit Shield",
                    color = OpalTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Placeholder to balance back button
                Box(modifier = Modifier.size(38.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Shield Title Card / Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(OpalCardBg)
                    .border(1.dp, OpalCardBorder, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = "Shield Name",
                        color = OpalTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ruleTitle,
                        onValueChange = {
                            ruleTitle = it
                            if (it.trim().isNotEmpty()) titleError = null
                        },
                        placeholder = {
                            Text(
                                text = "e.g., Deep Focus, Social Detox, Night Shield",
                                color = OpalTextSecondary.copy(alpha = 0.6f),
                                fontSize = 15.sp
                            )
                        },
                        singleLine = true,
                        isError = titleError != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpalMint,
                            unfocusedBorderColor = Color(0xFF2C3138),
                            focusedContainerColor = Color(0xFF1E2228),
                            unfocusedContainerColor = Color(0xFF1E2228),
                            focusedTextColor = OpalTextPrimary,
                            unfocusedTextColor = OpalTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shield_title_input")
                    )

                    AnimatedVisibility(visible = titleError != null) {
                        Text(
                            text = titleError ?: "",
                            color = OpalDanger,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Apps Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(OpalCardBg)
                    .border(1.dp, OpalCardBorder, RoundedCornerShape(22.dp))
                    .clickable { onSelectAppsClick() }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Apps Blocked",
                            color = OpalTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to choose which apps this shield restricts",
                            color = OpalTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StackedAppIcons(items = blockedApps, maxVisible = 2, iconSize = 20.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        val appsCount = blockedApps.count { !it.isWebsite }
                        val webCount = blockedApps.count { it.isWebsite }
                        val summaryText = when {
                            appsCount > 0 && webCount > 0 -> "$appsCount apps, $webCount web"
                            appsCount > 0 -> "$appsCount apps"
                            webCount > 0 -> "$webCount web"
                            else -> "All distracting"
                        }
                        Text(
                            text = summaryText,
                            color = OpalMint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Choose Apps",
                            tint = OpalTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time Selector Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(OpalCardBg)
                    .border(1.dp, OpalCardBorder, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Dotted line with 2 node dots
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 10.dp, end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8F949C))
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(38.dp)
                                .background(Color(0xFF3E434A))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .border(1.5.dp, Color(0xFF8F949C), CircleShape)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // From Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "From",
                                color = OpalTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF23272C))
                                    .clickable {
                                        showFromPicker = !showFromPicker
                                        showToPicker = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fromTime,
                                    color = OpalMint,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = OpalTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // From Time Quick Selector Chips
                        AnimatedVisibility(visible = showFromPicker) {
                            Column {
                                Text(
                                    text = "Select Start Time:",
                                    color = OpalTextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    availableTimes.take(8).forEach { t ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (fromTime == t) OpalMint else Color(0xFF1E2228))
                                                .clickable {
                                                    fromTime = t
                                                    showFromPicker = false
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = t,
                                                color = if (fromTime == t) Color.Black else OpalTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF24272B))

                        // To Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "To",
                                color = OpalTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF23272C))
                                    .clickable {
                                        showToPicker = !showToPicker
                                        showFromPicker = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = toTime,
                                    color = OpalMint,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = OpalTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // To Time Quick Selector Chips
                        AnimatedVisibility(visible = showToPicker) {
                            Column {
                                Text(
                                    text = "Select End Time:",
                                    color = OpalTextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    availableTimes.drop(6).forEach { t ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (toTime == t) OpalMint else Color(0xFF1E2228))
                                                .clickable {
                                                    toTime = t
                                                    showToPicker = false
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = t,
                                                color = if (toTime == t) Color.Black else OpalTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Days of the Week Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(OpalCardBg)
                    .border(1.dp, OpalCardBorder, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "On these days:",
                            color = OpalTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                        val daysLabel = when {
                            activeDays.size == 7 -> "Everyday"
                            activeDays == setOf(0, 1, 2, 3, 4) -> "Weekdays"
                            activeDays == setOf(5, 6) -> "Weekends"
                            else -> "${activeDays.size} days"
                        }
                        Text(
                            text = daysLabel,
                            color = OpalMint,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Day Circles: M, T, W, T, F, S, S
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayLabels.forEachIndexed { index, label ->
                            val isSelected = index in activeDays
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else Color(0xFF22262B))
                                    .clickable {
                                        activeDays = if (isSelected) {
                                            activeDays - index
                                        } else {
                                            activeDays + index
                                        }
                                        if (activeDays.isNotEmpty()) daysError = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else OpalTextSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = daysError != null) {
                        Text(
                            text = daysError ?: "",
                            color = OpalDanger,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hard Mode (PRO) Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(OpalCardBg)
                    .border(1.dp, OpalCardBorder, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Hard mode",
                                color = OpalTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, OpalMint, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = OpalMint,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "PRO",
                                        color = OpalMint,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Strict lockdown • No unblocks allowed",
                            color = OpalTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Switch(
                        checked = isHardMode,
                        onCheckedChange = { checked ->
                            isHardMode = checked
                            if (checked) onUpgradeProPrompt()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OpalMint,
                            uncheckedThumbColor = Color(0xFF6B7280),
                            uncheckedTrackColor = Color(0xFF262A30)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Allow Breaks Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(OpalCardBg)
                    .border(1.dp, OpalCardBorder, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allow short mindful breaks",
                            color = OpalTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Permit 15-minute snoozes during focus block",
                            color = OpalTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Switch(
                        checked = allowBreaks,
                        onCheckedChange = { allowBreaks = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OpalMint,
                            uncheckedThumbColor = Color(0xFF6B7280),
                            uncheckedTrackColor = Color(0xFF262A30)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // White Button with Black Text for Shield Edit / Save
            Button(
                onClick = {
                    if (ruleTitle.trim().isEmpty()) {
                        titleError = "Please enter a shield name"
                        return@Button
                    }
                    if (activeDays.isEmpty()) {
                        daysError = "Please select at least one active day"
                        return@Button
                    }

                    val daysSummary = when {
                        activeDays.size == 7 -> "Everyday"
                        activeDays == setOf(0, 1, 2, 3, 4) -> "Weekdays"
                        activeDays == setOf(5, 6) -> "Weekends"
                        else -> "${activeDays.size} days/week"
                    }

                    val updated = rule.copy(
                        title = ruleTitle.trim(),
                        fromTime = fromTime,
                        toTime = toTime,
                        daysLabel = daysSummary,
                        startsInText = "Active $fromTime–$toTime",
                        activeDays = activeDays,
                        isHardMode = isHardMode,
                        allowBreaks = allowBreaks,
                        isCustom = rule.isCustom || isCreating
                    )
                    onSaveRule(updated)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("rule_edit_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCreating) "Create Shield" else "Save Shield",
                    color = Color.Black,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isCreating) {
                Spacer(modifier = Modifier.height(16.dp))

                // Pause shield button
                Row(
                    modifier = Modifier
                        .clickable { onTogglePause() }
                        .padding(8.dp)
                        .testTag("rule_edit_pause_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (rule.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = if (rule.isPaused) OpalMint else OpalDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (rule.isPaused) "Resume shield" else "Pause shield",
                        color = if (rule.isPaused) OpalMint else OpalDanger,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
