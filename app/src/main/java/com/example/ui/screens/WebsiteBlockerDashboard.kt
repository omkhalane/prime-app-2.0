package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.rounded.Public
import com.example.util.PermissionHelper
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimeGlassIconButton
import com.example.ui.components.PrimePrimaryButton
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeError
import com.example.ui.theme.PrimeGlassBorder
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeSurfaceBorderBrush
import com.example.ui.theme.PrimeSurfaceElevated
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary
import com.example.vpn.db.CategoryEntity

@Composable
fun WebsiteBlockerDashboard(
    isVpnRunning: Boolean,
    isPermanentProtectionEnabled: Boolean,
    hasVpnPermission: Boolean,
    activeRulesCount: Int,
    blockedTodayCount: Int,
    categories: List<CategoryEntity>,
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
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isAccessibilityGranted = remember(isVpnRunning) {
        PermissionHelper.isAccessibilityPermissionGranted(context)
    }

    BackHandler {
        onBackClick()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PrimeBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .testTag("website_blocker_dashboard")
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.prime_logo),
                    contentDescription = "Prime Logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WEBSITE SHIELD",
                    color = PrimeAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            PrimeGlassIconButton(
                icon = Icons.Rounded.BarChart,
                contentDescription = "Analytics",
                onClick = onOpenAnalytics,
                containerSize = 40.dp,
                iconSize = 20.dp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Card: VPN Shield Status
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isVpnRunning) PrimeAccent else Color(0xFF64748B))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isVpnRunning) "SHIELD ACTIVE" else "SHIELD INACTIVE",
                                color = if (isVpnRunning) PrimeAccent else PrimeTextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isVpnRunning) "Websites Blocked" else "Protection Off",
                            color = PrimeTextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Master Toggle
                    Switch(
                        checked = isPermanentProtectionEnabled,
                        onCheckedChange = { checked ->
                            if (checked && !hasVpnPermission) {
                                onRequestVpnPermission()
                            } else {
                                onTogglePermanentProtection(checked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = PrimeAccent,
                            uncheckedThumbColor = PrimeTextSecondary,
                            uncheckedTrackColor = PrimeSurfaceHigher
                        ),
                        modifier = Modifier.testTag("permanent_shield_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Dual-layer protection blocks distracting websites in real time across Google Chrome, Samsung Internet, Firefox, Edge, Brave, and other mobile browsers.",
                    color = PrimeTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Dual Status Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. In-Browser Shield
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAccessibilityGranted) Color(0x1F00E59A) else Color(0x1FE6953B))
                            .border(
                                1.dp,
                                if (isAccessibilityGranted) PrimeAccent.copy(alpha = 0.4f) else Color(0xFFE6953B).copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = if (isAccessibilityGranted) PrimeAccent else Color(0xFFE6953B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Browser Shield",
                                    color = PrimeTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAccessibilityGranted) "Active (In-Browser)" else "Setup Required",
                                color = if (isAccessibilityGranted) PrimeAccent else Color(0xFFE6953B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Network VPN Shield
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isVpnRunning) Color(0x1F00E59A) else Color(0x1F64748B))
                            .border(
                                1.dp,
                                if (isVpnRunning) PrimeAccent.copy(alpha = 0.4f) else Color(0xFF64748B).copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.VpnKey,
                                    contentDescription = null,
                                    tint = if (isVpnRunning) PrimeAccent else PrimeTextTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Network Filter",
                                    color = PrimeTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isVpnRunning) "Active (DNS Filter)" else "Inactive",
                                color = if (isVpnRunning) PrimeAccent else PrimeTextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // In-Browser Shield Setup Prompt if not granted
                if (!isAccessibilityGranted) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF132322))
                            .border(BorderStroke(1.dp, PrimeAccent.copy(alpha = 0.4f)), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = "Browser Shield",
                                    tint = PrimeAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enable In-Browser Shield",
                                    color = PrimeAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enables real-time URL detection and instant blocking inside Google Chrome, Samsung Internet, Firefox, Edge, and all mobile browsers.",
                                color = PrimeTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            PrimePrimaryButton(
                                text = "Authorize Browser Shield",
                                onClick = {
                                    PermissionHelper.requestAccessibilityPermission(context)
                                },
                                height = 38.dp
                            )
                        }
                    }
                }

                if (!hasVpnPermission) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF261E10))
                            .border(BorderStroke(1.dp, Color(0xFFE6953B).copy(alpha = 0.5f)), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.VpnKey,
                                    contentDescription = "VPN Permission",
                                    tint = Color(0xFFE6953B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Network VPN Filter Setup",
                                    color = Color(0xFFE6953B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Android uses a local on-device VPN interface to filter website requests without routing traffic to external servers.",
                                color = PrimeTextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            PrimePrimaryButton(
                                text = "Authorize VPN Filter",
                                onClick = onRequestVpnPermission,
                                height = 38.dp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2 Metrics Cards in a Row: Blocked Today & Active Rules
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                onClick = onOpenAnalytics
            ) {
                Column {
                    Text(
                        text = "BLOCKED TODAY",
                        color = PrimeTextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$blockedTodayCount",
                        color = PrimeTextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Attempts intercepted",
                        color = PrimeAccent,
                        fontSize = 11.sp
                    )
                }
            }

            GlassCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                onClick = onOpenCategorySelection
            ) {
                Column {
                    Text(
                        text = "ACTIVE RULES",
                        color = PrimeTextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$activeRulesCount",
                        color = PrimeTextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Domains & categories",
                        color = PrimeTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Navigation Buttons
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Select Websites & Categories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCategorySelection() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimeSurfaceHigher),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = "Websites",
                                tint = PrimeAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Website Categories & Blocklists",
                                color = PrimeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Social media, streaming, news, adult, gambling",
                                color = PrimeTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = "Open",
                        tint = PrimeTextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF222222))
                )

                // Custom Domains Blocklist
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCustomBlocklist() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimeSurfaceHigher),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Custom",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Custom Web Domains",
                                color = PrimeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Add, edit, or permanently block specific URLs",
                                color = PrimeTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = "Open",
                        tint = PrimeTextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF222222))
                )

                // Blocking Schedules
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSchedules() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimeSurfaceHigher),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = "Schedules",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Automated Schedules",
                                color = PrimeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Set time windows and active days of week",
                                color = PrimeTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = "Open",
                        tint = PrimeTextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Category Toggles Section
        Text(
            text = "Categories",
            color = PrimeTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        categories.take(6).forEach { category ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimeGlassCardBg)
                    .border(BorderStroke(1.dp, Color(0xFF1E1E1E)), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(category.colorHex))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = category.name,
                                color = PrimeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = category.description,
                                color = PrimeTextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = category.isEnabled,
                        onCheckedChange = { onToggleCategory(category.name, it) },
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

        Spacer(modifier = Modifier.height(20.dp))

        // Explanation & Limitations Notice
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111111))
                .border(BorderStroke(1.dp, Color(0xFF222222)), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Info",
                        tint = PrimeTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Local-First DNS Filtering",
                        color = PrimeTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "PRIME runs 100% locally on your device with no remote servers, logs, or external accounts. It filters standard DNS queries from Chrome, Edge, Brave, Firefox, and system apps. Note: Apps using encrypted DNS (Private DNS/DoH) bypass standard DNS filters.",
                    color = PrimeTextTertiary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
