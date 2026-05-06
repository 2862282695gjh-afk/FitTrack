package com.fittrack.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrack.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 连胜火焰徽章 —— 带脉冲动画
 */
@Composable
fun StreakBadge(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streakPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streakScale"
    )

    Row(
        modifier = modifier
            .background(
                color = FitOrangeContainer,
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🔥",
            modifier = Modifier.scale(scale),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$streakDays",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = FitOrange
            )
        )
    }
}

/**
 * 经验值进度条 —— 带填充动画
 */
@Composable
fun XpProgressBar(
    currentXp: Int,
    maxXp: Int,
    level: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }
    val targetProgress = (currentXp.toFloat() / maxXp).coerceIn(0f, 1f)

    LaunchedEffect(currentXp, maxXp) {
        animatedProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            brush = Brush.linearGradient(GradientYellow),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$level",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = FitOnSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lv.$level",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Text(
                text = "$currentXp / $maxXp XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.value)
                    .height(12.dp)
                    .background(
                        brush = Brush.horizontalGradient(GradientYellow),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

/**
 * 成就徽章 —— 获得时的弹跳入场动画
 */
@Composable
fun AchievementBadge(
    emoji: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Card(
        modifier = modifier.scale(scale.value),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = FitYellowContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 圆形进度指示器 —— 带动画填充
 */
@Composable
fun AnimatedCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 120.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 12.dp,
    color: Color = FitGreen,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress.value },
            modifier = Modifier.fillMaxSize(),
            color = color,
            trackColor = trackColor,
            strokeWidth = strokeWidth
        )
    }
}
