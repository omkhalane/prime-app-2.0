package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavigationTab
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeSubtleBorder
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextTertiary

/**
 * Five-destination navigation with consistent iconography, 48dp+ targets,
 * clear selected state and a quiet divider instead of a floating glass capsule.
 */
@Composable
fun OpalBottomNavigation(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PrimeBackground)
            .padding(top = 1.dp)
            .background(Color(0xFF141414))
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("Home", NavigationTab.HOME, Icons.Rounded.Home, Icons.Outlined.Home, currentTab, onTabSelected, "nav_home_tab")
        NavItem("Focus", NavigationTab.TIMER, Icons.Rounded.Timer, Icons.Outlined.Timer, currentTab, onTabSelected, "nav_focus_tab")
        NavItem("Shields", NavigationTab.MY_APPS, Icons.Rounded.Shield, Icons.Outlined.Shield, currentTab, onTabSelected, "nav_shields_tab")
        NavItem("Insights", NavigationTab.INSIGHTS, Icons.Rounded.BarChart, Icons.Outlined.BarChart, currentTab, onTabSelected, "nav_insights_tab")
        NavItem("You", NavigationTab.PROFILE, Icons.Rounded.Person, Icons.Outlined.Person, currentTab, onTabSelected, "nav_profile_tab")
    }
}

@Composable
private fun NavItem(
    label: String,
    tab: NavigationTab,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    tag: String
) {
    val selected = currentTab == tab
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "nav_item_scale"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) PrimeAccent else PrimeTextTertiary,
        label = "nav_item_tint"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = { onTabSelected(tab) }
            )
            .semantics { this.selected = selected }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) activeIcon else inactiveIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (selected) PrimeTextPrimary else PrimeTextTertiary,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
