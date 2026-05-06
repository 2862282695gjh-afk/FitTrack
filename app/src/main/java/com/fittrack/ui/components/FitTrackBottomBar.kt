package com.fittrack.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fittrack.ui.navigation.Screen

/**
 * FitTrack 统一底部导航栏 —— Duolingo 风格弹跳选中动画
 */
@Composable
fun FitTrackBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItemData("首页", Icons.Default.Home, Screen.Home.route),
        NavItemData("计划", Icons.Default.List, Screen.PlanList.route),
        NavItemData("统计", Icons.Default.BarChart, Screen.Statistics.route),
        NavItemData("我的", Icons.Default.Person, Screen.Profile.route),
        NavItemData("设置", Icons.Default.Settings, Screen.Settings.route)
    )

    NavigationBar(
        modifier = modifier,
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = when {
                    isPressed -> 0.85f
                    selected -> 1.15f
                    else -> 1f
                },
                animationSpec = spring(
                    dampingRatio = 0.5f,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "navIconScale"
            )

            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.scale(scale)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                interactionSource = interactionSource,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

private data class NavItemData(
    val label: String,
    val icon: ImageVector,
    val route: String
)
