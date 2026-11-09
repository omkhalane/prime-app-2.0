package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BadgeLevel
import com.example.model.BadgeRarity
import com.example.model.PrimeBadge
import com.example.model.PrimeRankTier
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary

/**
 * Custom Shield + Hexagonal Medal Hybrid Badge Component.
 * Supports Level evolution (Bronze, Silver, Gold), Special Rarities (Platinum, Diamond, Cosmic),
 * embossed 3D icon, rim lighting, metallic bevel border, and Level badge (I, II, III).
 */
@Composable
fun PrimeHexagonBadge(
    badge: PrimeBadge,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 96.dp,
    showLabel: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val isUnlocked = badge.isUnlocked
    val isMythic = badge.rarity == BadgeRarity.MYTHIC

    // Cosmic shimmer animation for mythic badges
    val infiniteTransition = rememberInfiniteTransition(label = "badgeShimmer")
    val cosmicOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cosmicShimmer"
    )

    // Palette based on BadgeLevel / Rarity
    val metallicGradients = when {
        !isUnlocked -> listOf(Color(0xFF2C3038), Color(0xFF1E2126), Color(0xFF15181C))
        isMythic -> listOf(
            Color(0xFFFF4081),
            Color(0xFF7C4DFF),
            Color(0xFF00E5FF),
            Color(0xFFFFD700)
        )
        badge.rarity == BadgeRarity.LEGENDARY -> listOf(
            Color(0xFFB388FF),
            Color(0xFF7C4DFF),
            Color(0xFFE040FB),
            Color(0xFF00E5FF)
        )
        badge.rarity == BadgeRarity.EPIC -> listOf(
            Color(0xFFE0F7FA),
            Color(0xFF80DEEA),
            Color(0xFF00BCD4),
            Color(0xFF00838F)
        )
        badge.level == BadgeLevel.LEVEL_III || badge.rarity == BadgeRarity.RARE -> listOf(
            Color(0xFFFFE082),
            Color(0xFFFFD54F),
            Color(0xFFFFB300),
            Color(0xFFFF8F00)
        )
        badge.level == BadgeLevel.LEVEL_II || badge.rarity == BadgeRarity.UNCOMMON -> listOf(
            Color(0xFFECEFF1),
            Color(0xFFCFD8DC),
            Color(0xFF90A4AE),
            Color(0xFF607D8B)
        )
        else -> listOf( // Level I / Common Bronze
            Color(0xFFE6A36E),
            Color(0xFFCD7F32),
            Color(0xFFA0522D),
            Color(0xFF8B4513)
        )
    }

    val glowColor = when {
        !isUnlocked -> Color.Transparent
        isMythic -> Color(0xFFFF4081).copy(alpha = 0.45f)
        badge.rarity == BadgeRarity.LEGENDARY -> Color(0xFFB388FF).copy(alpha = 0.4f)
        badge.rarity == BadgeRarity.EPIC -> Color(0xFF80DEEA).copy(alpha = 0.35f)
        badge.level == BadgeLevel.LEVEL_III -> Color(0xFFFFD54F).copy(alpha = 0.4f)
        badge.level == BadgeLevel.LEVEL_II -> Color(0xFFCFD8DC).copy(alpha = 0.3f)
        else -> Color(0xFFCD7F32).copy(alpha = 0.3f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier.size(badgeSize),
            contentAlignment = Alignment.Center
        ) {
            // Ambient soft glow behind medal
            if (isUnlocked) {
                Box(
                    modifier = Modifier
                        .size(badgeSize * 0.88f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(glowColor, Color.Transparent)
                            )
                        )
                )
            }

            // Hexagonal shield Canvas rendering
            Canvas(modifier = Modifier.size(badgeSize)) {
                val w = size.width
                val h = size.height

                // Custom Hexagonal Shield Geometry
                fun buildShieldPath(inset: Float): Path {
                    val p = Path()
                    val iw = w - inset * 2
                    val ih = h - inset * 2
                    val left = inset
                    val top = inset

                    // 1. Top crest point
                    p.moveTo(left + iw * 0.50f, top + ih * 0.03f)
                    // 2. Top-right shoulder
                    p.lineTo(left + iw * 0.88f, top + ih * 0.14f)
                    // 3. Right vertical waist
                    p.lineTo(left + iw * 0.94f, top + ih * 0.62f)
                    // 4. Bottom shield point
                    p.lineTo(left + iw * 0.50f, top + ih * 0.97f)
                    // 5. Left vertical waist
                    p.lineTo(left + iw * 0.06f, top + ih * 0.62f)
                    // 6. Top-left shoulder
                    p.lineTo(left + iw * 0.12f, top + ih * 0.14f)
                    p.close()
                    return p
                }

                val outerPath = buildShieldPath(inset = 2.dp.toPx())
                val innerPath = buildShieldPath(inset = 5.5.dp.toPx())

                // Draw outer metallic bevel fill
                val metallicBrush = Brush.linearGradient(
                    colors = metallicGradients,
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                drawPath(outerPath, brush = metallicBrush, style = Fill)

                // Draw outer bevel highlight stroke
                val bevelStrokeColor = if (isUnlocked) {
                    Color.White.copy(alpha = 0.45f)
                } else {
                    Color(0xFF3A3F47)
                }
                drawPath(
                    outerPath,
                    color = bevelStrokeColor,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw deep charcoal inner shield body
                val innerCharcoalBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF181B20),
                        Color(0xFF101216),
                        Color(0xFF0B0D10)
                    )
                )
                drawPath(innerPath, brush = innerCharcoalBrush, style = Fill)

                // Inner rim stroke
                val innerRimColor = if (isUnlocked) {
                    metallicGradients.first().copy(alpha = 0.5f)
                } else {
                    Color(0xFF22262D)
                }
                drawPath(
                    innerPath,
                    color = innerRimColor,
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Central 3D Embossed Icon
            val iconVector = getBadgeIconVector(badge.iconType)
            val iconSize = badgeSize * 0.36f
            val iconTint = if (isUnlocked) {
                when {
                    isMythic -> Color(0xFFFF80AB)
                    badge.rarity == BadgeRarity.LEGENDARY -> Color(0xFFD1C4E9)
                    badge.level == BadgeLevel.LEVEL_III -> Color(0xFFFFD54F)
                    badge.level == BadgeLevel.LEVEL_II -> Color(0xFFECEFF1)
                    else -> Color(0xFFE6A36E)
                }
            } else {
                Color(0xFF4A505C)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-badgeSize * 0.05f))
            ) {
                if (isUnlocked) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = badge.name,
                        tint = iconTint,
                        modifier = Modifier.size(iconSize)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF555C68),
                        modifier = Modifier.size(iconSize * 0.85f)
                    )
                }
            }

            // Level Pill indicator (I, II, III, or Rarity) at bottom crest
            val levelText = if (badge.isSpecial) {
                badge.rarity.label.take(3).uppercase()
            } else {
                badge.level.roman
            }

            val levelBadgeColor = when {
                !isUnlocked -> Color(0xFF252930)
                badge.level == BadgeLevel.LEVEL_III -> Color(0xFFFFB300)
                badge.level == BadgeLevel.LEVEL_II -> Color(0xFF90A4AE)
                else -> Color(0xFFCD7F32)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-badgeSize * 0.04f))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1115))
                    .border(
                        1.dp,
                        if (isUnlocked) levelBadgeColor else Color(0xFF333842),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 7.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = levelText,
                    color = if (isUnlocked) levelBadgeColor else Color(0xFF757D8A),
                    fontSize = if (badgeSize > 80.dp) 10.sp else 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = badge.name,
                color = if (isUnlocked) Color(0xFFE1E4EA) else Color(0xFF757D8A),
                fontSize = if (badgeSize > 80.dp) 11.sp else 9.sp,
                fontWeight = if (isUnlocked) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Text(
                text = if (isUnlocked) badge.rarity.label else "Locked",
                color = if (isUnlocked) metallicGradients.first() else Color(0xFF555C68),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getBadgeIconVector(type: String): ImageVector {
    return when (type) {
        "hourglass" -> Icons.Default.HourglassTop
        "phone" -> Icons.Default.PhoneAndroid
        "bolt" -> Icons.Default.Bolt
        "target" -> Icons.Default.TrackChanges
        "self_improvement" -> Icons.Default.SelfImprovement
        "shield" -> Icons.Default.Shield
        "block" -> Icons.Default.Block
        "lock" -> Icons.Default.Lock
        "star" -> Icons.Default.Star
        "timer" -> Icons.Default.Timer
        "park" -> Icons.Default.Park
        "nightlight" -> Icons.Default.Nightlight
        "trophy" -> Icons.Default.WorkspacePremium
        "diamond" -> Icons.Default.Diamond
        else -> Icons.Default.Shield
    }
}

/**
 * Returns the distinctive prestige badge name for each of the 8 PRIME Ranking tiers.
 */
fun getRankBadgeName(rank: PrimeRankTier): String {
    return when (rank) {
        PrimeRankTier.AWAKENING -> "Awakening Guardian"
        PrimeRankTier.EXPLORER -> "Discipline Explorer"
        PrimeRankTier.INITIATE -> "Focus Initiate"
        PrimeRankTier.DISCIPLINED -> "Habit Architect"
        PrimeRankTier.FOCUSED -> "Sovereign Focus"
        PrimeRankTier.ELITE -> "Elite Vanguard"
        PrimeRankTier.MASTER -> "Platinum Grandmaster"
        PrimeRankTier.PRIME -> "PRIME Apex Champion"
    }
}

fun getRankIconVector(rank: PrimeRankTier): ImageVector {
    return when (rank) {
        PrimeRankTier.AWAKENING -> Icons.Default.Nightlight
        PrimeRankTier.EXPLORER -> Icons.Default.TrackChanges
        PrimeRankTier.INITIATE -> Icons.Default.Shield
        PrimeRankTier.DISCIPLINED -> Icons.Default.SelfImprovement
        PrimeRankTier.FOCUSED -> Icons.Default.HourglassTop
        PrimeRankTier.ELITE -> Icons.Default.WorkspacePremium
        PrimeRankTier.MASTER -> Icons.Default.Diamond
        PrimeRankTier.PRIME -> Icons.Default.Bolt
    }
}

fun buildShieldShapePath(width: Float, height: Float, inset: Float): Path {
    val p = Path()
    val iw = width - inset * 2
    val ih = height - inset * 2
    val left = inset
    val top = inset

    p.moveTo(left + iw * 0.50f, top + ih * 0.03f)
    p.lineTo(left + iw * 0.88f, top + ih * 0.14f)
    p.lineTo(left + iw * 0.94f, top + ih * 0.62f)
    p.lineTo(left + iw * 0.50f, top + ih * 0.97f)
    p.lineTo(left + iw * 0.06f, top + ih * 0.62f)
    p.lineTo(left + iw * 0.12f, top + ih * 0.14f)
    p.close()
    return p
}

/**
 * Dynamic Hero Badge component displayed on the Home Screen.
 * Renders the user's active Rank tier medal, rank tier number, award title, details, and badge name.
 */
@Composable
fun PrimeCurrentRankHeroBadge(
    rankTier: PrimeRankTier,
    score: Int,
    hasPermission: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val badgeName = getRankBadgeName(rankTier)
    val rankIcon = getRankIconVector(rankTier)
    val medalSize = 148.dp

    val metallicColors = when (rankTier) {
        PrimeRankTier.AWAKENING -> listOf(Color(0xFF90A4AE), Color(0xFF546E7A), Color(0xFF37474F))
        PrimeRankTier.EXPLORER -> listOf(Color(0xFFE6A36E), Color(0xFFCD7F32), Color(0xFF8B4513))
        PrimeRankTier.INITIATE -> listOf(Color(0xFFFFB74D), Color(0xFFE6953B), Color(0xFFA65B15))
        PrimeRankTier.DISCIPLINED -> listOf(Color(0xFFFFFFFF), Color(0xFFCFD8DC), Color(0xFF78909C))
        PrimeRankTier.FOCUSED -> listOf(Color(0xFFE0F7FA), Color(0xFF80DEEA), Color(0xFF00ACC1))
        PrimeRankTier.ELITE -> listOf(Color(0xFFFFF59D), Color(0xFFFFD54F), Color(0xFFFF8F00))
        PrimeRankTier.MASTER -> listOf(Color(0xFFE0F2F1), Color(0xFF80CBC4), Color(0xFF00897B))
        PrimeRankTier.PRIME -> listOf(Color(0xFFE040FB), Color(0xFF76F2BF), Color(0xFF00E5FF))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Rank Stage Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF141414))
                .border(BorderStroke(1.dp, Color(0xFF262626)), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PrimeAccent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Rank ${rankTier.tierNumber} • ${rankTier.title} (${rankTier.award})",
                color = PrimeTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Large 3D Hexagon Shield Medal
        Box(
            modifier = Modifier.size(medalSize),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Radial Aura Glow
            Box(
                modifier = Modifier
                    .size(medalSize * 1.08f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                rankTier.primaryColor.copy(alpha = 0.28f),
                                rankTier.secondaryColor.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Shield Canvas
            Canvas(modifier = Modifier.size(medalSize)) {
                val w = size.width
                val h = size.height

                val outerPath = buildShieldShapePath(w, h, inset = 3.dp.toPx())
                val innerPath = buildShieldShapePath(w, h, inset = 9.dp.toPx())

                // Draw outer metallic bevel fill
                val metallicBrush = Brush.linearGradient(
                    colors = metallicColors,
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                drawPath(outerPath, brush = metallicBrush, style = Fill)

                // Highlight rim
                drawPath(
                    outerPath,
                    color = Color.White.copy(alpha = 0.5f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Deep charcoal inner shield body
                val innerBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF191C22),
                        Color(0xFF111318),
                        Color(0xFF0A0C0F)
                    )
                )
                drawPath(innerPath, brush = innerBrush, style = Fill)

                // Inner rim stroke
                drawPath(
                    innerPath,
                    color = rankTier.primaryColor.copy(alpha = 0.6f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Central Hero Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-medalSize * 0.05f))
            ) {
                Icon(
                    imageVector = rankIcon,
                    contentDescription = badgeName,
                    tint = rankTier.primaryColor,
                    modifier = Modifier.size(medalSize * 0.38f)
                )
            }

            // Bottom Apex Tier Level Tag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-medalSize * 0.04f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F1116))
                    .border(1.2.dp, rankTier.primaryColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = "TIER ${rankTier.tierNumber}",
                    color = rankTier.primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Prominent Badge Name Pill (below medal)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF161A22))
                .border(1.2.dp, rankTier.primaryColor.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .padding(horizontal = 18.dp, vertical = 7.dp)
        ) {
            Text(
                text = badgeName,
                color = OpalTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Details / Motto
        Text(
            text = rankTier.subtitle,
            color = OpalTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (hasPermission) {
                "Score: $score pts • Tier range: ${rankTier.minScore}–${rankTier.maxScore} pts"
            } else {
                "Permission required to compute score"
            },
            color = if (hasPermission) rankTier.primaryColor.copy(alpha = 0.85f) else Color(0xFFFF8A80),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
