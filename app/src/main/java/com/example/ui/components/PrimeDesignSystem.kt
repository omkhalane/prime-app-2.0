package com.example.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeSubtleBorder
import com.example.ui.theme.PrimeSurfaceElevated
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary

object PrimeSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val pageHorizontal = 20.dp
    val pageBottom = 28.dp
}

object PrimeCorners {
    val control = 12.dp
    val card = 18.dp
    val hero = 24.dp
    val pill = 100.dp
}

@Composable
fun PrimePageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PrimeTextPrimary,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = PrimeTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun PrimeSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PrimeTextPrimary,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = PrimeTextTertiary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun PrimeMetricCard(
    label: String,
    value: String,
    detail: String? = null,
    modifier: Modifier = Modifier,
    accent: Color = PrimeAccent,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(PrimeCorners.card))
            .background(PrimeSurfaceElevated)
            .border(1.dp, PrimeSubtleBorder, RoundedCornerShape(PrimeCorners.card))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = PrimeTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            color = PrimeTextPrimary,
            fontSize = 24.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp
        )
        if (!detail.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(text = detail, color = PrimeTextTertiary, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun PrimeStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PrimeAccent
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(PrimeCorners.pill))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.24f), RoundedCornerShape(PrimeCorners.pill))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(7.dp))
        Text(text, color = color, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PrimeBackHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrimeGlassIconButton(
            icon = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBackClick,
            containerSize = 44.dp,
            iconSize = 20.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PrimeTextPrimary,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.25).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = PrimeTextTertiary, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}
