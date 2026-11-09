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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
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
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary
import com.example.vpn.db.BlockedDomainEntity
import com.example.vpn.db.CategoryEntity

@Composable
fun WebsiteSelectionScreen(
    categories: List<CategoryEntity>,
    domains: List<BlockedDomainEntity>,
    onBackClick: () -> Unit,
    onToggleDomainEnabled: (String, Boolean) -> Unit,
    onToggleDomainFavorite: (String, Boolean) -> Unit,
    onToggleCategoryEnabled: (String, Boolean) -> Unit,
    onAddCustomDomain: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) } // null = All
    var isAddDomainDialogOpen by remember { mutableStateOf(false) }

    BackHandler {
        onBackClick()
    }

    val filteredDomains = remember(searchQuery, selectedCategoryFilter, domains) {
        domains.filter { domainItem ->
            val matchesSearch = searchQuery.isBlank() ||
                domainItem.domain.contains(searchQuery, ignoreCase = true) ||
                domainItem.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == null || domainItem.category == selectedCategoryFilter
            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .testTag("website_selection_screen")
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
                text = "Websites & Categories",
                color = PrimeTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            PrimeGlassIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Add Custom Website",
                onClick = { isAddDomainDialogOpen = true },
                containerSize = 40.dp,
                iconSize = 20.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search websites or categories...", color = PrimeTextTertiary, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = PrimeTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = PrimeTextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { searchQuery = "" }
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PrimeGlassCardBg,
                unfocusedContainerColor = PrimeGlassCardBg,
                focusedBorderColor = PrimeAccent,
                unfocusedBorderColor = Color(0xFF222222),
                focusedTextColor = PrimeTextPrimary,
                unfocusedTextColor = PrimeTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("website_search_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Category Filter Chips Carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                val isSelected = selectedCategoryFilter == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimeAccent else Color(0xFF161616))
                        .clickable { selectedCategoryFilter = null }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "All (${domains.size})",
                        color = if (isSelected) Color.Black else PrimeTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            items(categories) { category ->
                val isSelected = selectedCategoryFilter == category.name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(category.colorHex) else Color(0xFF161616))
                        .clickable { selectedCategoryFilter = category.name }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category.name,
                        color = if (isSelected) Color.Black else PrimeTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Category Banner (if category is filtered)
        selectedCategoryFilter?.let { catName ->
            val catObj = categories.firstOrNull { it.name == catName }
            if (catObj != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF111111))
                        .border(BorderStroke(1.dp, Color(catObj.colorHex).copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Category: ${catObj.name}",
                                color = PrimeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (catObj.isEnabled) "All ${catObj.name} sites are blocked" else "Category is allowed",
                                color = if (catObj.isEnabled) PrimeAccent else PrimeTextTertiary,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = catObj.isEnabled,
                            onCheckedChange = { onToggleCategoryEnabled(catObj.name, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(catObj.colorHex),
                                uncheckedThumbColor = PrimeTextSecondary,
                                uncheckedTrackColor = PrimeSurfaceHigher
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Domain List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("website_domain_list")
        ) {
            items(filteredDomains, key = { it.domain }) { domainItem ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimeGlassCardBg)
                        .border(BorderStroke(1.dp, Color(0xFF1C1C1C)), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (domainItem.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                contentDescription = "Favorite",
                                tint = if (domainItem.isFavorite) Color(0xFFFFD700) else PrimeTextTertiary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        onToggleDomainFavorite(domainItem.domain, !domainItem.isFavorite)
                                    }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = domainItem.domain,
                                    color = PrimeTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = domainItem.category + if (domainItem.isPermanent) " • Permanent" else "",
                                    color = PrimeTextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = domainItem.isEnabled,
                            onCheckedChange = { onToggleDomainEnabled(domainItem.domain, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = PrimeAccent,
                                uncheckedThumbColor = PrimeTextSecondary,
                                uncheckedTrackColor = PrimeSurfaceHigher
                            )
                        )
                    }
                }
            }

            if (filteredDomains.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No websites found matching \"$searchQuery\"",
                            color = PrimeTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add Domain Dialog
    if (isAddDomainDialogOpen) {
        var inputDomain by remember { mutableStateOf("") }
        var selectedCat by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Custom Websites") }

        androidx.compose.ui.window.Dialog(onDismissRequest = { isAddDomainDialogOpen = false }) {
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
                        text = "Add Blocked Website",
                        color = PrimeTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter domain (e.g. reddit.com, news.ycombinator.com). Subdomains are automatically filtered.",
                        color = PrimeTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputDomain,
                        onValueChange = { inputDomain = it },
                        placeholder = { Text("example.com", color = PrimeTextTertiary, fontSize = 14.sp) },
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
                                .clickable { isAddDomainDialogOpen = false },
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
                                    onAddCustomDomain(inputDomain, selectedCat)
                                    isAddDomainDialogOpen = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Add Domain", color = if (inputDomain.isNotBlank()) Color.Black else PrimeTextTertiary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
