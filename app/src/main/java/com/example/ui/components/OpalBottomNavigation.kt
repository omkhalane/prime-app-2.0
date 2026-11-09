package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavigationTab
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeAccentDark
import com.example.ui.theme.PrimeAccentSecondary
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeGlassBackground
import com.example.ui.theme.PrimeGlassBorderHighlight
import com.example.ui.theme.PrimeSurfaceBorderBrush
import com.example.ui.theme.PrimeSurfaceElevated
import com.example.ui.theme.PrimeSurfaceHigher
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary

/**
 * Apple-inspired Floating Liquid Glass Navigation Capsule.
 * Translucent dark graphite background, thin specular border,
 * smooth spring-animated active indicator and refined icons.
 */
@Composable
fun OpalBottomNavigation(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
            .border(BorderStroke(1.dp, Color(0xFF1A1A1A)))
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingNavItem(
                title = "Home",
                activeIcon = Icons.Rounded.RadioButtonChecked,
                inactiveIcon = Icons.Outlined.RadioButtonUnchecked,
                isSelected = currentTab == NavigationTab.HOME,
                onClick = { onTabSelected(NavigationTab.HOME) },
                testTag = "nav_home_tab",
                modifier = Modifier.weight(1f)
            )
            FloatingNavItem(
                title = "Shields",
                activeIcon = Icons.Rounded.Shield,
                inactiveIcon = Icons.Outlined.Shield,
                isSelected = currentTab == NavigationTab.MY_APPS,
                onClick = { onTabSelected(NavigationTab.MY_APPS) },
                testTag = "nav_shields_tab",
                modifier = Modifier.weight(1f)
            )
            FloatingNavItem(
                title = "Timer",
                activeIcon = Icons.Rounded.Timer,
                inactiveIcon = Icons.Outlined.HourglassEmpty,
                isSelected = currentTab == NavigationTab.TIMER,
                onClick = { onTabSelected(NavigationTab.TIMER) },
                testTag = "nav_timer_tab",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FloatingNavItem(
    title: String,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "nav_press_scale"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) PrimeAccent else PrimeTextTertiary,
        label = "nav_text_color"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "nav_pill_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(vertical = 4.dp)
    ) {
        // Active Indicator Capsule
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(
                    if (isSelected) PrimeAccentDark.copy(alpha = 0.85f * pillAlpha) else Color.Transparent
                )
                .then(
                    if (isSelected) {
                        Modifier.border(
                            BorderStroke(1.dp, PrimeAccent.copy(alpha = 0.35f)),
                            RoundedCornerShape(17.dp)
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) activeIcon else inactiveIcon,
                contentDescription = title,
                tint = if (isSelected) PrimeAccent else PrimeTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = title,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
    }
}
