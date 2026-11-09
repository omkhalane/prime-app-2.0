package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.model.BlockedAppItem
import com.example.ui.components.AppIconBadge
import com.example.ui.theme.OpalBlack
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary

@Composable
fun SelectAppsSheet(
    apps: List<BlockedAppItem>,
    onDismiss: () -> Unit,
    onToggleBlock: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onAddCustomWebsiteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilterTab by remember { mutableStateOf("All") } // "All", "Blocked", "Apps", "Websites"
    var searchQuery by remember { mutableStateOf("") }
    val tabsScrollState = rememberScrollState()

    val selectedCount = apps.count { it.isBlocked && !it.isWebsite }
    val websiteCount = apps.count { it.isBlocked && it.isWebsite }
    val totalBlockedCount = apps.count { it.isBlocked }

    val filteredList = apps.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                item.packageName.contains(searchQuery, ignoreCase = true)
        val matchesTab = when (selectedFilterTab) {
            "Blocked" -> item.isBlocked
            "Apps" -> !item.isWebsite
            "Websites" -> item.isWebsite
            else -> true
        }
        matchesSearch && matchesTab
    }

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

            // Header: Back Arrow + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22262B))
                        .clickable { onDismiss() }
                        .testTag("select_apps_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OpalTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Select apps & websites",
                        color = OpalTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalBlockedCount items currently blocked",
                        color = OpalMint,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search installed apps...",
                        color = OpalTextSecondary,
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = OpalTextSecondary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OpalMint,
                    unfocusedBorderColor = OpalCardBorder,
                    focusedContainerColor = OpalCardBg,
                    unfocusedContainerColor = OpalCardBg,
                    focusedTextColor = OpalTextPrimary,
                    unfocusedTextColor = OpalTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apps_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-filter tabs (All, Blocked, Apps, Websites)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tabsScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    "All" to "All (${apps.size})",
                    "Blocked" to "Blocked ($totalBlockedCount)",
                    "Apps" to "Apps (${apps.count { !it.isWebsite }})",
                    "Websites" to "Websites"
                )

                tabs.forEach { (tabKey, tabLabel) ->
                    val isSelected = selectedFilterTab == tabKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) OpalMint.copy(alpha = 0.2f) else Color(0xFF1D2024))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) OpalMint else OpalCardBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilterTab = tabKey }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tabLabel,
                            color = if (isSelected) OpalMint else OpalTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // App List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(OpalCardBg)
                    .border(1.dp, OpalCardBorder, RoundedCornerShape(24.dp))
                    .padding(vertical = 4.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    AppSelectionRow(
                        item = item,
                        onToggle = { onToggleBlock(item.id) }
                    )
                }

                // Add website manually button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddCustomWebsiteClick() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22262B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Website Manually",
                                tint = OpalMint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Add website manually",
                                color = OpalTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Block any URL domain",
                                color = OpalTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary text
            Text(
                text = "$selectedCount apps, $websiteCount websites selected for Focus Shield",
                color = OpalTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Done Button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("select_apps_done_button")
            ) {
                Text(
                    text = "Save Selection",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AppSelectionRow(
    item: BlockedAppItem,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconBadge(item = item, size = 38.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = com.example.util.formatAppName(item.name),
                color = OpalTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.usageToday,
                color = OpalTextSecondary,
                fontSize = 13.sp
            )
        }

        Checkbox(
            checked = item.isBlocked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = OpalMint,
                checkmarkColor = Color.Black,
                uncheckedColor = Color(0xFF6B7280)
            )
        )
    }
}
