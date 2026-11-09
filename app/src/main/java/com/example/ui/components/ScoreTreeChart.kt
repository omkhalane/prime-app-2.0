package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeAccentSecondary
import com.example.ui.theme.PrimeError
import com.example.ui.theme.PrimeGlassBorder
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeSubtleBorder
import com.example.ui.theme.PrimeSurfaceElevated
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary

/**
 * Score Tree Chart matching Apple-inspired Liquid Glass & HIG principles:
 * - Centered "Score" label and large score with upward indicator (▲)
 * - Horizontal curly bracket tree connector
 * - Three pill nodes: Sleep (Moon), Focus (Hourglass), Rest (Tree) with custom glowing border strokes
 */
@Composable
fun ScoreTreeChart(
    totalScore: Int,
    sleepScore: Int,
    focusScore: Int,
    restScore: Int,
    modifier: Modifier = Modifier,
    hasPermission: Boolean = true,
    errorMessage: String? = null,
    onRequestPermission: (() -> Unit)? = null,
    onChartClick: (() -> Unit)? = null
) {
    val bracketColor = Color(0xFF2C323C)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onChartClick != null) Modifier.clickable { onChartClick() } else Modifier)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // "Score" Label
        Text(
            text = "Score",
            color = PrimeAccent,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Main Score Number with Upward Triangle Arrow (▲) or Permission Banner
        if (!hasPermission) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimeSurfaceElevated)
                    .border(1.dp, PrimeError.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable { onRequestPermission?.invoke() ?: onChartClick?.invoke() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "0",
                    color = PrimeError,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("score_tree_total")
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Permission Required",
                        color = PrimeError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap to enable screen time",
                        color = PrimeTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalScore",
                    color = PrimeAccent,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.testTag("score_tree_total")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "▲",
                    color = PrimeAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Curved Curly Bracket Tree Connector
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 46.dp)
        ) {
            val w = size.width
            val h = size.height

            val leftX = w * 0.16f
            val midX = w * 0.50f
            val rightX = w * 0.84f

            val topY = 2f
            val bottomY = h - 2f

            val path = Path().apply {
                // Left downward hook
                moveTo(leftX, bottomY)
                quadraticTo(leftX, topY + 8f, leftX + 16f, topY + 8f)

                // Left horizontal segment to center bracket cusp
                lineTo(midX - 16f, topY + 8f)
                quadraticTo(midX - 4f, topY + 8f, midX, topY)

                // Right bracket cusp down to horizontal segment
                quadraticTo(midX + 4f, topY + 8f, midX + 16f, topY + 8f)
                lineTo(rightX - 16f, topY + 8f)

                // Right downward hook
                quadraticTo(rightX, topY + 8f, rightX, bottomY)
            }

            drawPath(
                path = path,
                color = bracketColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Three Capsule Pills Row: Sleep, Focus, Rest
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            TreeChartPill(
                score = sleepScore,
                label = "Sleep",
                icon = Icons.Rounded.Bedtime,
                iconTint = PrimeAccent
            )

            TreeChartPill(
                score = focusScore,
                label = "Focus",
                icon = Icons.Rounded.HourglassTop,
                iconTint = PrimeAccent
            )

            TreeChartPill(
                score = restScore,
                label = "Rest",
                icon = Icons.Rounded.Park,
                iconTint = PrimeAccent
            )
        }
    }
}

@Composable
private fun TreeChartPill(
    score: Int,
    label: String,
    icon: ImageVector,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(94.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(PrimeGlassCardBg)
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(PrimeAccent.copy(alpha = 0.5f), PrimeGlassBorder)),
                    RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$score",
                    color = PrimeTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            color = PrimeTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
    }
}
