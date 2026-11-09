package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeAccentDark
import com.example.ui.theme.PrimeAccentGlow
import com.example.ui.theme.PrimeAccentSecondary
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeBackgroundSecondary
import com.example.ui.theme.PrimeEmeraldDarkGradient
import com.example.ui.theme.PrimeEmeraldGradient
import com.example.ui.theme.PrimeGlassBackground
import com.example.ui.theme.PrimeGlassBorder
import com.example.ui.theme.PrimeGlassBorderHighlight
import com.example.ui.theme.PrimeGlassCardBg
import com.example.ui.theme.PrimeGlassGradient
import com.example.ui.theme.PrimeSubtleBorder
import com.example.ui.theme.PrimeSurface
import com.example.ui.theme.PrimeSurfaceBorderBrush
import com.example.ui.theme.PrimeSurfaceElevated
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextQuaternary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary

/**
 * Liquid Glass Container
 * Provides a translucent dark graphite surface with a subtle 1dp specular stroke
 * and ambient lighting highlight.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = PrimeGlassCardBg,
    borderColor: Color = PrimeGlassBorder,
    borderBrush: Brush? = PrimeSurfaceBorderBrush,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.985f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "glass_card_press"
    )

    val baseModifier = modifier
        .scale(scale)
        .clip(shape)
        .background(backgroundColor)
        .then(
            if (borderBrush != null) {
                Modifier.border(BorderStroke(1.dp, borderBrush), shape)
            } else {
                Modifier.border(BorderStroke(1.dp, borderColor), shape)
            }
        )
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
            } else Modifier
        )
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .padding(contentPadding)

    Box(modifier = baseModifier) {
        content()
    }
}

/**
 * Standard Surface Card (Material B)
 * For dashboard content, analytics, and organized groups.
 */
@Composable
fun PrimeCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = PrimeSurfaceElevated,
    borderColor: Color = PrimeSubtleBorder,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.985f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "prime_card_press"
    )

    val baseModifier = modifier
        .scale(scale)
        .clip(shape)
        .background(backgroundColor)
        .border(BorderStroke(1.dp, borderColor), shape)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
            } else Modifier
        )
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .padding(contentPadding)

    Box(modifier = baseModifier) {
        content()
    }
}

/**
 * Primary Emerald Action Button (Material D)
 * Spring-based press scaling, vibrant gradient, dark high-contrast typography.
 */
@Composable
fun PrimePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    testTag: String = "prime_primary_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "primary_btn_press"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (enabled) PrimeEmeraldGradient else Brush.linearGradient(listOf(PrimeSurfaceHigher, PrimeSurfaceHigher))
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) PrimeBackground else PrimeTextTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) PrimeBackground else PrimeTextTertiary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * Glass / Secondary Action Button
 * Apple-style frosted glass button with subtle specular border.
 */
@Composable
fun PrimeGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    height: Dp = 50.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    testTag: String = "prime_glass_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "glass_btn_press"
    )

    val textColor = when {
        !enabled -> PrimeTextTertiary
        isDestructive -> com.example.ui.theme.PrimeError
        else -> PrimeTextPrimary
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .fillMaxWidth()
            .clip(shape)
            .background(PrimeGlassCardBg)
            .border(BorderStroke(1.dp, PrimeSurfaceBorderBrush), shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp
            )
        }
    }
}

/**
 * Minimalist Glass Icon Button (48dp minimum touch target)
 */
@Composable
fun PrimeGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = PrimeTextPrimary,
    containerSize: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = CircleShape,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "icon_btn_press"
    )

    Box(
        modifier = modifier
            .size(containerSize)
            .scale(scale)
            .clip(shape)
            .background(PrimeGlassCardBg)
            .border(BorderStroke(1.dp, PrimeGlassBorder), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * iOS-style Segmented Control with smooth sliding indicator pill
 */
@Composable
fun <T> PrimeSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PrimeSurfaceElevated)
            .border(BorderStroke(1.dp, PrimeSubtleBorder), RoundedCornerShape(14.dp))
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.98f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                    label = "segment_scale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .scale(animatedScale)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (isSelected) PrimeSurfaceHigher else Color.Transparent
                        )
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    BorderStroke(1.dp, PrimeGlassBorderHighlight),
                                    RoundedCornerShape(11.dp)
                                )
                            } else Modifier
                        )
                        .clickable { onItemSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item),
                        color = if (isSelected) PrimeTextPrimary else PrimeTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * iOS-style Fluid Switch / Toggle with emerald glow
 */
@Composable
fun PrimeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "prime_toggle"
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 22f else 2f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "toggle_thumb"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (checked) PrimeAccent else PrimeSurfaceHigher)
            .border(
                BorderStroke(
                    1.dp,
                    if (checked) PrimeAccentSecondary.copy(alpha = 0.4f) else PrimeSubtleBorder
                ),
                RoundedCornerShape(15.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .testTag(testTag)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) PrimeBackground else PrimeTextPrimary)
        )
    }
}

/**
 * Circular Precision Progress Ring with Neon Emerald Gradient and Glow
 */
@Composable
fun PrimeProgressRing(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = PrimeSurfaceHigher,
    gradient: Brush = PrimeEmeraldGradient
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "ring_progress"
    )

    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidth.toPx()
        val diameter = size.minDimension - strokeWidthPx
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f
        )
        val arcSize = Size(diameter, diameter)

        // Background Track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )

        // Progress Sweep
        if (animatedProgress > 0.005f) {
            drawArc(
                brush = gradient,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Metric Card for Screen Time, Focus Time, and Streaks
 */
@Composable
fun PrimeMetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = PrimeAccent,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimeSurfaceHigher),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimeTextSecondary
                    )
                }

                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                color = PrimeTextPrimary,
                fontWeight = FontWeight.Bold
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimeTextTertiary
                )
            }
        }
    }
}

/**
 * Apple Sheet Header with grabber indicator and clean title
 */
@Composable
fun PrimeSheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 12.dp)
    ) {
        // Drag grabber bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PrimeGlassBorderHighlight)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = PrimeTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimeTextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (actionButton != null) {
                    actionButton()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                PrimeGlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close",
                    onClick = onClose,
                    containerSize = 36.dp,
                    iconSize = 18.dp
                )
            }
        }
    }
}

/**
 * Apple-style Section Header with Optional Action
 */
@Composable
fun PrimeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = PrimeTextSecondary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )

        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                color = PrimeAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Serene Apple-style Empty State
 */
@Composable
fun PrimeEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(PrimeSurfaceHigher)
                .border(BorderStroke(1.dp, PrimeGlassBorder), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimeTextSecondary,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = PrimeTextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = PrimeTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            PrimeGlassButton(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier.width(180.dp),
                height = 42.dp
            )
        }
    }
}
