package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockedAppItem
import com.example.ui.components.AppIconBadge
import com.example.ui.theme.OpalBlack
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary
import com.example.util.formatAppName

@Composable
fun TimerBlockAppsSheet(
    apps: List<BlockedAppItem>,
    sessionBlockedAppIds: Set<String>,
    onToggleSessionApp: (String) -> Unit,
    onSelectAllUnblocked: () -> Unit,
    onClearSelected: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val shieldedApps = apps.filter { it.isBlocked }
    val unblockedApps = apps.filter { !it.isBlocked }

    val filteredApps = apps.filter { item ->
        searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.packageName.contains(searchQuery, ignoreCase = true)
    }.sortedWith(compareBy({ !it.isBlocked }, { it.name.lowercase() }))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OpalBlack)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OpalCardBg)
                        .border(1.dp, OpalCardBorder, CircleShape)
                        .clickable { onDismiss() }
                        .testTag("close_timer_block_apps"),
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
                    text = "Block Apps for Session",
                    color = OpalTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Done",
                    color = OpalMint,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("done_timer_block_apps")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explanation note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141920))
                    .border(1.dp, OpalCardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = OpalMint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Shielded apps are locked in grayscale. Any additional unblocked apps you select here will be blocked during this timer session and automatically unblocked when the timer ends.",
                    color = OpalTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search apps to block...",
                        color = OpalTextSecondary.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = OpalTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("search_timer_apps_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OpalCardBg,
                    unfocusedContainerColor = OpalCardBg,
                    focusedBorderColor = OpalMint,
                    unfocusedBorderColor = OpalCardBorder,
                    focusedTextColor = OpalTextPrimary,
                    unfocusedTextColor = OpalTextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick select / clear chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = OpalCardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OpalCardBorder),
                    modifier = Modifier
                        .clickable { onSelectAllUnblocked() }
                        .testTag("select_all_timer_apps_btn")
                ) {
                    Text(
                        text = "Select all unblocked (${unblockedApps.size})",
                        color = OpalMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (sessionBlockedAppIds.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = OpalCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OpalCardBorder),
                        modifier = Modifier
                            .clickable { onClearSelected() }
                            .testTag("clear_timer_apps_btn")
                    ) {
                        Text(
                            text = "Clear selection",
                            color = OpalTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Apps List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.id }) { item ->
                    val isShielded = item.isBlocked
                    val isSessionSelected = sessionBlockedAppIds.contains(item.id)

                    val itemAlpha = if (isShielded) 0.45f else 1f
                    val containerBg = if (isSessionSelected && !isShielded) {
                        OpalMint.copy(alpha = 0.08f)
                    } else {
                        OpalCardBg
                    }
                    val borderColor = if (isSessionSelected && !isShielded) {
                        OpalMint.copy(alpha = 0.4f)
                    } else {
                        OpalCardBorder
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(containerBg)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isShielded) {
                                onToggleSessionApp(item.id)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("timer_app_row_${item.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App Icon (Grayscale / dimmed if shielded)
                        Box(modifier = Modifier.alpha(itemAlpha)) {
                            AppIconBadge(item = item, size = 38.dp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatAppName(item.name),
                                    color = if (isShielded) OpalTextSecondary else OpalTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (isShielded) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF262A30))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color(0xFFAAAAAA),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Shielded",
                                                color = Color(0xFFAAAAAA),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = if (isShielded) {
                                    "Already blocked in Shields"
                                } else if (item.isWebsite) {
                                    "Website • Unblocks after timer"
                                } else {
                                    "Unblocks automatically after timer"
                                },
                                color = if (isShielded) Color(0xFF6E747C) else OpalTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Checkbox
                        if (isShielded) {
                            // Disabled, permanently checked state
                            Checkbox(
                                checked = true,
                                onCheckedChange = null,
                                enabled = false,
                                colors = CheckboxDefaults.colors(
                                    disabledCheckedColor = Color(0xFF4A4E54),
                                    disabledUncheckedColor = Color(0xFF33383F),
                                    checkmarkColor = Color(0xFF888E96)
                                )
                            )
                        } else {
                            Checkbox(
                                checked = isSessionSelected,
                                onCheckedChange = { onToggleSessionApp(item.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = OpalMint,
                                    uncheckedColor = OpalTextSecondary,
                                    checkmarkColor = Color.Black
                                ),
                                modifier = Modifier.testTag("checkbox_${item.id}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Apply Button
            val totalBlockingCount = shieldedApps.size + sessionBlockedAppIds.size
            Button(
                onClick = { onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("apply_timer_apps_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OpalMint,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (sessionBlockedAppIds.isEmpty()) {
                        "Block Shields Only (${shieldedApps.size} apps)"
                    } else {
                        "Block for Session (${sessionBlockedAppIds.size} added • $totalBlockingCount total)"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
