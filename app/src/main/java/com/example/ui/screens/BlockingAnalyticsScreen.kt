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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
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
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimeGlassIconButton
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeError
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary
import com.example.vpn.db.BlockedWebsiteEventEntity
import com.example.vpn.db.CategoryAttemptCount
import com.example.vpn.db.DomainAttemptCount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BlockingAnalyticsScreen(
    totalBlockedCount: Int,
    todayBlockedCount: Int,
    uniqueDomainsCount: Int,
    topBlockedDomains: List<DomainAttemptCount>,
    categoryDistribution: List<CategoryAttemptCount>,
    recentEvents: List<BlockedWebsiteEventEntity>,
    onBackClick: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val timeFormat = SimpleDateFormat("h:mm a • MMM d", Locale.US)

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
            .testTag("blocking_analytics_screen")
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
                text = "Blocking Analytics",
                color = PrimeTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            PrimeGlassIconButton(
                icon = Icons.Rounded.DeleteOutline,
                contentDescription = "Clear History",
                onClick = onClearHistory,
                containerSize = 40.dp,
                iconSize = 20.dp,
                tint = PrimeError.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Metric Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Text(
                    text = "TOTAL ATTEMPTS BLOCKED",
                    color = PrimeTextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$totalBlockedCount",
                        color = PrimeTextPrimary,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "($todayBlockedCount today)",
                        color = PrimeAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Unique Sites Protected", color = PrimeTextSecondary, fontSize = 12.sp)
                        Text("$uniqueDomainsCount domains", color = PrimeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Privacy Guarantee", color = PrimeTextSecondary, fontSize = 12.sp)
                        Text("100% On-Device", color = PrimeAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Top Blocked Domains Card
        Text(
            text = "Most Frequently Blocked Websites",
            color = PrimeTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (topBlockedDomains.isNotEmpty()) {
            val maxCount = (topBlockedDomains.firstOrNull()?.count ?: 1).coerceAtLeast(1)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topBlockedDomains.forEach { item ->
                        val ratio = (item.count.toFloat() / maxCount.toFloat()).coerceIn(0.08f, 1f)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.domain,
                                    color = PrimeTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${item.count} attempts",
                                    color = PrimeAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(PrimeSurfaceHigher)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(PrimeAccent)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF121212))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No blocked attempts recorded yet. As you browse, blocked attempts will appear here.",
                    color = PrimeTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Category Breakdown
        if (categoryDistribution.isNotEmpty()) {
            Text(
                text = "Attempts by Category",
                color = PrimeTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    categoryDistribution.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PrimeAccent)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = cat.category,
                                    color = PrimeTextPrimary,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "${cat.count} blocks",
                                color = PrimeTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Recent Blocked Events Timeline
        Text(
            text = "Recent Blocked Attempts (Live Log)",
            color = PrimeTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (recentEvents.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentEvents.take(15).forEach { event ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF141414))
                            .border(BorderStroke(1.dp, Color(0xFF222222)), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = event.domain,
                                    color = PrimeTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = event.category + " • " + event.blockingReason,
                                    color = PrimeTextTertiary,
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = timeFormat.format(Date(event.timestamp)),
                                color = PrimeTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF121212))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Live timeline will display DNS requests intercepted by PRIME.",
                    color = PrimeTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
