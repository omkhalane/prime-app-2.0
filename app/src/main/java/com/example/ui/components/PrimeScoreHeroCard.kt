package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PrimeScoreBreakdown
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeAccentDark
import com.example.ui.theme.PrimeAccentGlow
import com.example.ui.theme.PrimeAccentSecondary
import com.example.ui.theme.PrimeEmeraldGradient
import com.example.ui.theme.PrimeGlassBorder
import com.example.ui.theme.PrimeGlassBorderHighlight
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeSubtleBorder
import com.example.ui.theme.PrimeSurfaceBorderBrush
import com.example.ui.theme.PrimeSurfaceElevated
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary
import com.example.ui.theme.PrimeWarning
import kotlin.math.roundToInt

@Composable
fun PrimeScoreHeroCard(
    scoreBreakdown: PrimeScoreBreakdown,
    onOpenFormulaDialog: () -> Unit,
    onOpenTargetSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalScore = scoreBreakdown.totalScore
    val rollingAvg = scoreBreakdown.rolling7DayAverage
    val rankTier = scoreBreakdown.currentRank

    val animatedScoreFraction by animateFloatAsState(
        targetValue = (totalScore / 1000f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 850),
        label = "scoreFraction"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(PrimeGlassCardBg)
            .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), RoundedCornerShape(26.dp))
            .padding(20.dp)
            .testTag("prime_score_hero_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Top Header: Tag & Subtitle
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRIME SCORE",
                        color = PrimeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                }

                // Quick Target Tuning Glass Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimeSurfaceHigher)
                        .border(BorderStroke(1.dp, PrimeGlassBorder), RoundedCornerShape(12.dp))
                        .clickable { onOpenTargetSettings() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Targets",
                        tint = PrimeTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Targets",
                        color = PrimeTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Daily Digital Discipline · 0–1,000 points",
                color = PrimeTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Big Score Row + Rolling Average Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$totalScore",
                        color = PrimeAccent,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.2).sp,
                        modifier = Modifier.testTag("prime_total_score_text")
                    )
                    Text(
                        text = " / 1,000",
                        color = PrimeTextTertiary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }

                // 7-day average glass badge
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimeSurfaceHigher)
                            .border(BorderStroke(1.dp, PrimeGlassBorder), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "7-Day Avg: ",
                                color = PrimeTextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$rollingAvg",
                                color = PrimeTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (totalScore >= rollingAvg) "▲ +${totalScore - rollingAvg} today" else "▼ ${totalScore - rollingAvg} today",
                        color = if (totalScore >= rollingAvg) PrimeAccent else PrimeWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Overall Progress Bar out of 1000
            LinearProgressIndicator(
                progress = { animatedScoreFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PrimeAccent,
                trackColor = PrimeSurfaceHigher,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current Rank Tier Glass Emblem
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PrimeSurfaceHigher)
                    .border(
                        BorderStroke(1.dp, rankTier.primaryColor.copy(alpha = 0.4f)),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onOpenFormulaDialog() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(rankTier.primaryColor.copy(alpha = 0.16f))
                                .border(
                                    BorderStroke(1.dp, rankTier.primaryColor.copy(alpha = 0.45f)),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WorkspacePremium,
                                contentDescription = rankTier.title,
                                tint = rankTier.primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Rank ${rankTier.tierNumber} · ${rankTier.title}",
                                    color = PrimeTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(rankTier.primaryColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = rankTier.award,
                                        color = rankTier.primaryColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = rankTier.subtitle,
                                color = PrimeTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = "Details",
                        tint = PrimeTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // The 3 Prime Discipline Factors
            Text(
                text = "DISCIPLINE FACTORS",
                color = PrimeTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Factor 1: Screen-time control (300 pts)
            DisciplineFactorRow(
                title = "Screen-time control",
                earnedPoints = scoreBreakdown.screenTimeScore.roundToInt(),
                maxPoints = 300,
                progress = (scoreBreakdown.screenTimeScore / 300f).coerceIn(0f, 1f),
                statsText = "${scoreBreakdown.actualScreenMinutes}m used · Target ≤ ${scoreBreakdown.targetScreenMinutes}m",
                icon = Icons.Rounded.PhoneAndroid,
                accentColor = Color(0xFF64B5F6)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Factor 2: Focus time (350 pts)
            DisciplineFactorRow(
                title = "Focus time",
                earnedPoints = scoreBreakdown.focusScore.roundToInt(),
                maxPoints = 350,
                progress = (scoreBreakdown.focusScore / 350f).coerceIn(0f, 1f),
                statsText = "${scoreBreakdown.actualFocusMinutes}m verified · Target ${scoreBreakdown.targetFocusMinutes}m",
                icon = Icons.Rounded.HourglassTop,
                accentColor = PrimeAccent
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Factor 3: Blocking discipline (350 pts)
            DisciplineFactorRow(
                title = "Blocking discipline",
                earnedPoints = scoreBreakdown.blockingScore.roundToInt(),
                maxPoints = 350,
                progress = (scoreBreakdown.blockingScore / 350f).coerceIn(0f, 1f),
                statsText = "${scoreBreakdown.actualBlockingMinutes}m protected · Target ${scoreBreakdown.targetBlockingMinutes}m",
                icon = Icons.Rounded.Block,
                accentColor = PrimeWarning
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom tap to view formula & details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenFormulaDialog() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = PrimeAccent.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tap to view PRIME formula & score rules",
                    color = PrimeAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DisciplineFactorRow(
    title: String,
    earnedPoints: Int,
    maxPoints: Int,
    progress: Float,
    statsText: String,
    icon: ImageVector,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PrimeSurfaceElevated)
            .border(BorderStroke(1.dp, PrimeSubtleBorder), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = PrimeTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$earnedPoints",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " / $maxPoints",
                        color = PrimeTextTertiary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = PrimeSurfaceHigher,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statsText,
                color = PrimeTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
