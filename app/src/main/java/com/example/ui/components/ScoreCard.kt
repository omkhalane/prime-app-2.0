package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PrimeRankTier
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary

@Composable
fun ScoreCard(
    totalScore: Int,
    sleepScore: Int,
    focusScore: Int,
    restScore: Int,
    modifier: Modifier = Modifier
) {
    val tier = PrimeRankTier.fromScore(totalScore)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PRIME SCORE",
            color = OpalTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$totalScore",
                color = OpalMint,
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = " / 1,000",
                color = OpalTextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 7.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Rank Tier Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(tier.primaryColor.copy(alpha = 0.15f))
                .border(1.dp, tier.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = tier.primaryColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Rank ${tier.tierNumber} · ${tier.title} (${tier.award})",
                color = tier.primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Three PRIME discipline factors: Screen (300), Focus (350), Blocking (350)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScorePill(
                label = "Screen Control",
                score = sleepScore,
                maxPoints = 300,
                icon = Icons.Default.PhoneAndroid,
                tint = Color(0xFF64B5F6)
            )
            ScorePill(
                label = "Focus Time",
                score = focusScore,
                maxPoints = 350,
                icon = Icons.Default.HourglassTop,
                tint = Color(0xFF81C784)
            )
            ScorePill(
                label = "Distraction Block",
                score = restScore,
                maxPoints = 350,
                icon = Icons.Default.Block,
                tint = Color(0xFFFFB74D)
            )
        }
    }
}

@Composable
private fun ScorePill(
    label: String,
    score: Int,
    maxPoints: Int,
    icon: ImageVector,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(OpalCardBg.copy(alpha = 0.85f))
                .border(1.dp, OpalCardBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "$score",
                    color = OpalTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/$maxPoints",
                    color = OpalTextSecondary,
                    fontSize = 10.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = OpalTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
