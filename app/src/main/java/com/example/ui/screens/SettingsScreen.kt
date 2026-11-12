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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.OpalBlack
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalMintLight
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary

@Composable
fun SettingsScreen(
    profile: UserProfile,
    onBackClick: () -> Unit,
    onTryProClick: () -> Unit,
    onUseEmergencyPass: () -> Unit,
    onReplayOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OpalBlack)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Profile header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161616))
                    .border(1.dp, Color(0x26FFFFFF), CircleShape)
                    .clickable { onBackClick() }
                    .testTag("settings_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OpalTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Your profile",
                    color = OpalTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "Personalize your PRIME experience",
                    color = OpalTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Free plan / Pro Card
        Text(
            text = if (profile.isPro) "You're on Prime PRO" else "You're on a Free plan",
            color = OpalTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (profile.isPro) "All features & Hard Mode active" else "Upgrade to focus better with PRO",
            color = OpalTextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // PRO Upgrade Card (Liquid Glass with subtle emerald border)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xD914181F))
                .border(1.dp, Color(0x3300E59A), RoundedCornerShape(26.dp))
                .padding(22.dp)
        ) {
            Column {
                ProFeatureRow(
                    icon = Icons.Default.Shield,
                    title = "Hard mode",
                    desc = "No way out. You can't turn Prime off."
                )
                Spacer(modifier = Modifier.height(14.dp))
                ProFeatureRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Unlimited rules",
                    desc = "Schedule as many recurring shields as you need"
                )
                Spacer(modifier = Modifier.height(14.dp))
                ProFeatureRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Allow only",
                    desc = "Block everything except white-listed essential apps"
                )
                Spacer(modifier = Modifier.height(14.dp))
                ProFeatureRow(
                    icon = Icons.Default.ColorLens,
                    title = "Make it yours",
                    desc = "Pick your favorite Block Screens"
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onTryProClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OpalMintLight,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_try_pro_button")
                ) {
                    Text(
                        text = if (profile.isPro) "PRO Plan Active" else "Try for Free",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Profile Section
        SectionTitle(title = "Profile")
        SettingsCard {
            SettingsItemRow(
                icon = Icons.Default.Person,
                title = "My account",
                subtitle = profile.name
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Personalize Section
        SectionTitle(title = "Personalize")
        SettingsCard {
            SettingsItemRow(
                icon = Icons.Default.AutoAwesome,
                title = "Autofocus",
                subtitle = "Coming soon"
            )
            HorizontalDivider(color = Color(0xFF23272C))
            SettingsItemRow(
                icon = Icons.Default.Shield,
                title = "Block screens",
                subtitle = "Coming soon"
            )
            HorizontalDivider(color = Color(0xFF23272C))
            SettingsItemRow(
                icon = Icons.Default.HourglassTop,
                title = "Waiting room",
                subtitle = "Coming soon"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Support Section
        SectionTitle(title = "Support")
        SettingsCard {
            SettingsItemRow(
                icon = Icons.Default.HelpOutline,
                title = "Help centre",
                subtitle = "Coming soon"
            )
            HorizontalDivider(color = Color(0xFF23272C))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUseEmergencyPass() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = OpalTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Emergency pass",
                        color = OpalTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${profile.emergencyPassesRemaining} remaining",
                    color = OpalMint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Integrations Section
        SectionTitle(title = "Integrations")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                        tint = OpalTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Prime for Mac",
                            color = OpalTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Block apps on your Mac too",
                            color = OpalTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = OpalTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Other Section
        SectionTitle(title = "Other")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReplayOnboarding() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = OpalMint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Replay Onboarding Journey",
                        color = OpalTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Walk through the full 16-step Prime setup",
                        color = OpalTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            HorizontalDivider(color = Color(0xFF23272C))
            SettingsItemRow(
                icon = Icons.Default.ToggleOn,
                title = "Permissions",
                subtitle = "Coming soon"
            )
            HorizontalDivider(color = Color(0xFF23272C))
            SettingsItemRow(
                icon = Icons.Default.Restore,
                title = "Restore purchase",
                subtitle = null
            )
            HorizontalDivider(color = Color(0xFF23272C))
            SettingsItemRow(
                icon = Icons.Default.Info,
                title = "About Prime",
                subtitle = "Version 1.0 (Gemstone Edition)"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Community Forum Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1B1E22))
                .border(1.dp, OpalCardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Community forum · Coming soon",
                    color = OpalTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = OpalTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = OpalTextPrimary,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(OpalCardBg)
            .border(1.dp, OpalCardBorder, RoundedCornerShape(22.dp))
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OpalTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                color = OpalTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = OpalTextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ProFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OpalMint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = OpalTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = OpalTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
