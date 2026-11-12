package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.*
import com.example.vpn.db.CategoryEntity
import com.example.vpn.db.BlockedDomainEntity

@Composable
fun WebsiteBlockerDashboard(
    isVpnRunning: Boolean,
    isPermanentProtectionEnabled: Boolean,
    hasVpnPermission: Boolean,
    activeRulesCount: Int,
    blockedTodayCount: Int,
    categories: List<CategoryEntity>,
    domains: List<BlockedDomainEntity>,
    onBackClick: () -> Unit,
    onRequestVpnPermission: () -> Unit,
    onTogglePermanentProtection: (Boolean) -> Unit,
    onToggleCategory: (String, Boolean) -> Unit,
    onOpenCategorySelection: () -> Unit,
    onOpenCustomBlocklist: () -> Unit,
    onOpenSchedules: () -> Unit,
    onOpenAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val displayCategories = listOf(
        "Adult" to listOf("Adult Content", "Adult"),
        "Entertainment" to listOf("Entertainment", "Video Streaming"),
        "Socials" to listOf("Socials", "Social Media"),
        "Gambling" to listOf("Gambling"),
        "Gaming" to listOf("Gaming")
    )
    Column(
        modifier = modifier.fillMaxSize().background(PrimeBackground)
            .statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).testTag("website_blocker_dashboard")
    ) {
        PrimeBackHeader(
            title = "Website blocker",
            subtitle = "Choose what you want to keep out of reach.",
            onBackClick = onBackClick,
            trailing = {
                PrimeGlassIconButton(
                    icon = Icons.Rounded.BarChart,
                    contentDescription = "Analytics",
                    onClick = onOpenAnalytics,
                    containerSize = 44.dp,
                    iconSize = 20.dp
                )
            }
        )
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(PrimeGlassCardBg).padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (isVpnRunning) "PROTECTION ACTIVE" else "PROTECTION INACTIVE", color = if (isVpnRunning) PrimeAccent else PrimeTextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(if (isVpnRunning) "Stay in control." else "Protect your focus.", color = PrimeTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Switch(
                    checked = isPermanentProtectionEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasVpnPermission) onRequestVpnPermission() else onTogglePermanentProtection(enabled)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimeAccent, uncheckedThumbColor = PrimeTextSecondary, uncheckedTrackColor = PrimeSurfaceHigher),
                    modifier = Modifier.testTag("permanent_shield_toggle")
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("ACTIVE RULES", activeRulesCount.toString(), Modifier.weight(1f), onOpenCategorySelection)
                Metric("BLOCKED TODAY", blockedTodayCount.toString(), Modifier.weight(1f), onOpenAnalytics)
            }
            if (!hasVpnPermission) {
                Spacer(Modifier.height(14.dp))
                Text("Allow PRIME to create its on-device VPN connection to enforce your website blocklist.", color = PrimeTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Text("Grant permission", color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(PrimeAccent).clickable(onClick = onRequestVpnPermission).padding(horizontal = 16.dp, vertical = 11.dp))
            }
        }
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(PrimeGlassCardBg).clickable(onClick = onOpenCategorySelection).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = PrimeAccent)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("$activeRulesCount blocked domains", color = PrimeTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Open to see domain names, categories and expiry", color = PrimeTextSecondary, fontSize = 12.sp)
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = PrimeTextTertiary, modifier = Modifier.size(15.dp))
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("Categories", color = PrimeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        displayCategories.forEach { (label, aliases) ->
            val category = categories.firstOrNull { it.name in aliases }
            if (category != null) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp))
                        .background(PrimeGlassCardBg).padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(Color(category.colorHex)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(label, color = PrimeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${domains.count { it.category in aliases }} domains", color = PrimeTextTertiary, fontSize = 12.sp)
                    }
                    Switch(checked = category.isEnabled, onCheckedChange = { onToggleCategory(category.name, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimeAccent, uncheckedThumbColor = PrimeTextSecondary, uncheckedTrackColor = PrimeSurfaceHigher))
                }
            }
        }
        categories.filter { it.isCustom }.forEach { category ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(PrimeGlassCardBg).padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(category.colorHex)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(category.name, color = PrimeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${domains.count { it.category == category.name }} domains", color = PrimeTextTertiary, fontSize = 11.sp)
                }
                Switch(checked = category.isEnabled, onCheckedChange = { onToggleCategory(category.name, it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimeAccent, uncheckedThumbColor = PrimeTextSecondary, uncheckedTrackColor = PrimeSurfaceHigher))
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PrimeGlassCardBg).clickable(onClick = onOpenCategorySelection).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Add, null, tint = PrimeAccent)
            Spacer(Modifier.width(12.dp))
            Text("Create a custom category", color = PrimeTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = PrimeTextTertiary, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.height(22.dp))
        ActionRow("Custom domains", "Add domains to your personal blocklist", Icons.Rounded.Security, onOpenCustomBlocklist)
        Spacer(Modifier.height(10.dp))
        ActionRow("Automated schedules", "Choose days, dates, times and categories", Icons.Rounded.Schedule, onOpenSchedules)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF101010)).clickable(onClick = onClick).padding(13.dp)) {
        Text(label, color = PrimeTextTertiary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(value, color = PrimeTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PrimeGlassCardBg).clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = PrimeAccent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = PrimeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = PrimeTextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = PrimeTextTertiary, modifier = Modifier.size(14.dp))
    }
}
