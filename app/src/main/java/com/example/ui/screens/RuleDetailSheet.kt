package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalDanger
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalSurfaceDark
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleDetailSheet(
    rule: BlockRule,
    apps: List<BlockedAppItem>,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onTogglePause: () -> Unit,
    onDeleteRule: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val blockedApps = apps.filter { it.id in rule.blockedAppIds }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OpalSurfaceDark,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A3F45))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close Button top left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22262B))
                        .clickable { onDismiss() }
                        .testTag("rule_detail_close_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = OpalTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badge: Calendar -> Shield
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF22262B))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = OpalMint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "›",
                    color = OpalTextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = OpalMint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rule Title
            Text(
                text = rule.title,
                color = OpalTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Apps preview summary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Block ",
                    color = OpalTextSecondary,
                    fontSize = 14.sp
                )
                StackedAppIcons(items = blockedApps, maxVisible = 2, iconSize = 20.dp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Starts in pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF22262B))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (rule.isPaused) "Paused" else rule.startsInText,
                    color = if (rule.isPaused) OpalDanger else OpalTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Info rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InfoRow(label = "During this time", value = "${rule.fromTime} – ${rule.toTime}")
                HorizontalDivider(color = OpalCardBorder)
                InfoRow(label = "On these days", value = rule.daysLabel)
                HorizontalDivider(color = OpalCardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Block apps",
                        color = OpalTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StackedAppIcons(items = blockedApps, maxVisible = 2, iconSize = 18.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        val appsCount = blockedApps.count { !it.isWebsite }
                        val webCount = blockedApps.count { it.isWebsite }
                        val summaryText = when {
                            appsCount > 0 && webCount > 0 -> "$appsCount apps, $webCount websites"
                            appsCount > 0 -> "$appsCount apps"
                            webCount > 0 -> "$webCount websites"
                            else -> "All blocked items"
                        }
                        Text(
                            text = summaryText,
                            color = OpalTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                HorizontalDivider(color = OpalCardBorder)
                InfoRow(label = "Breaks allowed", value = if (rule.allowBreaks) "Yes" else "No")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // "Edit shield" button (White Pill with Black Text)
            Button(
                onClick = onEditClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("rule_detail_edit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit shield",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit shield",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pause / Resume shield button
            Row(
                modifier = Modifier
                    .clickable { onTogglePause() }
                    .padding(8.dp)
                    .testTag("rule_detail_pause_button"),
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

            if (rule.isCustom && onDeleteRule != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clickable { onDeleteRule(rule.id) }
                        .padding(8.dp)
                        .testTag("rule_detail_delete_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete shield",
                        tint = OpalDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete custom shield",
                        color = OpalDanger,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = OpalTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            color = OpalTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
