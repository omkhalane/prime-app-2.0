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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
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
import com.example.ui.components.PrimeGlassIconButton
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeError
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary
import com.example.vpn.db.BlockedDomainEntity

@Composable
fun CustomBlocklistScreen(
    customDomains: List<BlockedDomainEntity>,
    onBackClick: () -> Unit,
    onAddDomain: (String, Boolean, Long) -> Unit,
    onDeleteDomain: (String) -> Unit,
    onTogglePermanent: (String, Boolean) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
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
            .testTag("custom_blocklist_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimeGlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBackClick,
                containerSize = 40.dp,
                iconSize = 20.dp
            )

            Text(
                text = "Custom Web Domains",
                color = PrimeTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            PrimeGlassIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Add Domain",
                onClick = { isAddDialogOpen = true },
                containerSize = 40.dp,
                iconSize = 20.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111111))
                .border(BorderStroke(1.dp, Color(0xFF222222)), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Add domains to your personal blocklist. You can mark domains as Permanent to block them 24/7, or session-only for focus blocks.",
                color = PrimeTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Domains List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("custom_domains_lazy_column")
        ) {
            items(customDomains, key = { it.domain }) { domainItem ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimeGlassCardBg)
                        .border(BorderStroke(1.dp, Color(0xFF1E1E1E)), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = domainItem.domain,
                                color = PrimeTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (domainItem.isPermanent) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                    contentDescription = "Permanent",
                                    tint = if (domainItem.isPermanent) Color(0xFF00E5FF) else PrimeTextTertiary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            onTogglePermanent(domainItem.domain, !domainItem.isPermanent)
                                        }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (domainItem.isPermanent) "Permanent 24/7 Block" else "Session Block",
                                    color = if (domainItem.isPermanent) Color(0xFF00E5FF) else PrimeTextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Switch(
                                checked = domainItem.isEnabled,
                                onCheckedChange = { onToggleEnabled(domainItem.domain, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = PrimeAccent,
                                    uncheckedThumbColor = PrimeTextSecondary,
                                    uncheckedTrackColor = PrimeSurfaceHigher
                                )
                            )

                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete Domain",
                                tint = PrimeError.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onDeleteDomain(domainItem.domain) }
                            )
                        }
                    }
                }
            }

            if (customDomains.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No custom domains yet",
                                color = PrimeTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button to block a specific URL",
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
        var inputDomain by remember { mutableStateOf("") }
        var isPermanent by remember { mutableStateOf(false) }
        var durationHours by remember { mutableStateOf("24") }

        androidx.compose.ui.window.Dialog(onDismissRequest = { isAddDialogOpen = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF141414))
                    .border(BorderStroke(1.dp, Color(0xFF262626)), RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Add Custom Web Domain",
                        color = PrimeTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter any domain or subdomain (e.g. news.ycombinator.com, twitch.tv).",
                        color = PrimeTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputDomain,
                        onValueChange = { inputDomain = it },
                        placeholder = { Text("domain.com", color = PrimeTextTertiary, fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
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

                    if (!isPermanent) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = durationHours,
                            onValueChange = { durationHours = it.filter(Char::isDigit).take(4) },
                            label = { Text("Block duration (hours)", color = PrimeTextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Permanent 24/7 Block",
                                color = PrimeTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Block at all times, outside focus sessions",
                                color = PrimeTextTertiary,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = isPermanent,
                            onCheckedChange = { isPermanent = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00E5FF),
                                uncheckedThumbColor = PrimeTextSecondary,
                                uncheckedTrackColor = PrimeSurfaceHigher
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

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
                            Text("Cancel", color = PrimeTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (inputDomain.isNotBlank()) PrimeAccent else Color(0xFF333333))
                                .clickable(enabled = inputDomain.isNotBlank()) {
                                    val hours = durationHours.toLongOrNull()?.coerceIn(1L, 8760L) ?: 24L
                                    onAddDomain(inputDomain, isPermanent, if (isPermanent) 0L else hours * 60L * 60L * 1000L)
                                    isAddDialogOpen = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Add", color = if (inputDomain.isNotBlank()) Color.Black else PrimeTextTertiary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
