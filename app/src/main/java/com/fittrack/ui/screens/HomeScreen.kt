package com.fittrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fittrack.data.entity.WorkoutPlan
import com.fittrack.data.entity.WorkoutRecord
import com.fittrack.ui.components.*
import com.fittrack.ui.navigation.STAGGER_DELAY
import com.fittrack.ui.navigation.listItemEnter
import com.fittrack.ui.theme.*
import com.fittrack.ui.viewmodel.FitTrackViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FitTrackViewModel,
    onNavigateToPlanList: () -> Unit,
    onNavigateToPlanDetail: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToPlanGenerator: () -> Unit = {}
) {
    val allPlans by viewModel.allPlans.collectAsState(initial = emptyList())
    val weeklyCount by viewModel.weeklyWorkoutCount.collectAsState(initial = 0)
    val allRecords by viewModel.allRecords.collectAsState(initial = emptyList())

    val contentVisible = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { contentVisible.targetState = true }

    // 计算周进度
    val weekTarget = 5
    val progressPct = (weeklyCount.toFloat() / weekTarget).coerceIn(0f, 1f)
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progressPct) {
        animatedProgress.animateTo(
            targetValue = progressPct,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    Scaffold(
        floatingActionButton = {
            // AI 教练 FAB — Lovable 风格：发光脉冲 + 浮动
            val fabInteraction = remember { MutableInteractionSource() }
            val fabPressed by fabInteraction.collectIsPressedAsState()
            val fabScale by animateFloatAsState(
                targetValue = if (fabPressed) 0.88f else 1f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
                label = "fabScale"
            )

            val infiniteTransition = rememberInfiniteTransition(label = "fabPulse")
            val glowScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowPulse"
            )

            SmallFloatingActionButton(
                onClick = onNavigateToChat,
                modifier = Modifier
                    .scale(fabScale)
                    .scale(glowScale),
                containerColor = FitGreen,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "AI 教练")
            }
        },
        bottomBar = { /* 由 NavHost 管理 */ }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(FitBackground),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ═══════════════════════════════════════════════════
            // Hero 区域 — Lovable 风格渐变头部
            // ═══════════════════════════════════════════════════
            item {
                AnimatedVisibility(
                    visibleState = contentVisible,
                    enter = listItemEnter(delayMillis = 0)
                ) {
                    HeroSection(weeklyCount = weeklyCount)
                }
            }

            // ═══════════════════════════════════════════════════
            // 统计卡片双栏 — Lovable 轻量 StatCard
            // ═══════════════════════════════════════════════════
            item {
                AnimatedVisibility(
                    visibleState = contentVisible,
                    enter = listItemEnter(delayMillis = STAGGER_DELAY)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LovableStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.FitnessCenter,
                            label = "本周训练",
                            value = "$weeklyCount",
                            sub = "次 / 目标 $weekTarget 次"
                        )
                        LovableStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.TrendingUp,
                            label = "完成率",
                            value = "${(progressPct * 100).toInt()}%",
                            sub = "本周进度"
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // 周训练计划进度 — Lovable 风格 7 天方块
            // ═══════════════════════════════════════════════════
            item {
                AnimatedVisibility(
                    visibleState = contentVisible,
                    enter = listItemEnter(delayMillis = STAGGER_DELAY * 2)
                ) {
                    WeekProgressSection(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        completedDays = weeklyCount,
                        weekTarget = weekTarget,
                        animatedProgress = animatedProgress.value
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // AI 生成计划入口 — Lovable 风格全宽渐变按钮
            // ═══════════════════════════════════════════════════
            item {
                AnimatedVisibility(
                    visibleState = contentVisible,
                    enter = listItemEnter(delayMillis = STAGGER_DELAY * 3)
                ) {
                    AIGenerateButton(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        onClick = onNavigateToPlanGenerator
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // 训练记录列表
            // ═══════════════════════════════════════════════════
            item {
                AnimatedVisibility(
                    visibleState = contentVisible,
                    enter = listItemEnter(delayMillis = STAGGER_DELAY * 4)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "最近训练",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            if (allRecords.isNotEmpty()) {
                                TextButton(onClick = onNavigateToStatistics) {
                                    Text("查看全部", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            val recentRecords = allRecords.take(5)
            if (recentRecords.isEmpty()) {
                item {
                    AnimatedVisibility(
                        visibleState = contentVisible,
                        enter = listItemEnter(delayMillis = STAGGER_DELAY * 5)
                    ) {
                        EmptyTrainingCard(modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            } else {
                items(recentRecords, key = { it.id }) { record ->
                    AnimatedVisibility(
                        visibleState = contentVisible,
                        enter = listItemEnter(delayMillis = STAGGER_DELAY * 5)
                    ) {
                        WeeklyRecordCard(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            record = record
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // 我的计划列表
            // ═══════════════════════════════════════════════════
            item {
                AnimatedVisibility(
                    visibleState = contentVisible,
                    enter = listItemEnter(delayMillis = STAGGER_DELAY * 6)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "我的计划",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        TextButton(onClick = onNavigateToPlanList) {
                            Text("查看全部 →", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (allPlans.isEmpty()) {
                item {
                    AnimatedVisibility(
                        visibleState = contentVisible,
                        enter = listItemEnter(delayMillis = STAGGER_DELAY * 7)
                    ) {
                        EmptyPlanCard(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            onNavigate = onNavigateToPlanList
                        )
                    }
                }
            } else {
                items(allPlans.take(3), key = { it.id }) { plan ->
                    AnimatedVisibility(
                        visibleState = contentVisible,
                        enter = listItemEnter(delayMillis = STAGGER_DELAY * 7)
                    ) {
                        LovablePlanCard(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            plan = plan,
                            onClick = { onNavigateToPlanDetail(plan.id) },
                            onStartWorkout = { onStartWorkout(plan.id) }
                        )
                    }
                }
            }

            // 底部留白
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Lovable 风格子组件
// ═══════════════════════════════════════════════════════════════

/**
 * Hero 区域 — 渐变头部 + 日期 + 问候语 + 浮动装饰
 */
@Composable
private fun HeroSection(weeklyCount: Int) {
    val today = remember {
        val d = Date()
        val weekdays = listOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
        val sdf = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
        "${sdf.format(d)} · ${weekdays[d.day]}"
    }

    val message = when {
        weeklyCount >= 5 -> "本周目标达成！太强了！"
        weeklyCount >= 3 -> "状态火热，继续保持！"
        weeklyCount > 0 -> "今天也要燃烧吧！"
        else -> "开始你的健身之旅吧！"
    }

    // 浮动动画 state
    val floatTransition1 = rememberInfiniteTransition(label = "heroFloat1")
    val floatOffset1 by floatTransition1.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroFloat1"
    )
    val floatTransition2 = rememberInfiniteTransition(label = "heroFloat2")
    val floatOffset2 by floatTransition2.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroFloat2"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(Brush.linearGradient(GradientHero))
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        // 装饰浮动圆形 — Lovable blob 风格
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-20).dp, y = (-20).dp)
                .size(120.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .graphicsLayer { translationY = floatOffset1 }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 20.dp)
                .size(90.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                .graphicsLayer { translationY = floatOffset2 }
        )

        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    today,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                StreakBadgeCompact(streakDays = weeklyCount)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "坚持下去，更好的自己正在路上",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/** 紧凑版连胜徽章（Hero 区内使用） */
@Composable
private fun StreakBadgeCompact(streakDays: Int) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🔥",
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$streakDays",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        )
    }
}

/**
 * Lovable 风格 StatCard — 白底 + border + 图标 + 数值 + 标签
 */
@Composable
private fun LovableStatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    sub: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 4.dp,
        animationSpec = tween(200),
        label = "statElevation"
    )

    Card(
        modifier = modifier.scale(if (isPressed) 0.97f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FitSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(1.dp, FitOutline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = FitGreen.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = FitGreen
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 周训练计划进度 — 7 天方块 + 渐变进度条
 */
@Composable
private fun WeekProgressSection(
    modifier: Modifier = Modifier,
    completedDays: Int,
    weekTarget: Int,
    animatedProgress: Float
) {
    val todayIndex = remember {
        val cal = Calendar.getInstance()
        (cal.get(Calendar.DAY_OF_WEEK) + 6) % 7 // 周一=0, 周日=6
    }
    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FitSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, FitOutline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = FitGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "本周训练计划",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    "$completedDays/$weekTarget 完成",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 渐变进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(FitOutline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(GradientPrimary))
                        .drawWithContent {
                            drawContent()
                            // Shimmer overlay
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.3f),
                                        Color.Transparent
                                    ),
                                    startX = -size.width + (System.currentTimeMillis() % 3000) / 3000f * size.width * 3,
                                    endX = (System.currentTimeMillis() % 3000) / 3000f * size.width * 3
                                )
                            )
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7 天方块
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dayLabels.forEachIndexed { index, label ->
                    val isCompleted = index < completedDays
                    val isToday = index == todayIndex

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isCompleted) {
                                    Modifier.background(Brush.horizontalGradient(GradientPrimary))
                                } else if (isToday) {
                                    Modifier
                                        .border(2.dp, FitGreen, RoundedCornerShape(8.dp))
                                        .background(FitGreen.copy(alpha = 0.1f))
                                } else {
                                    Modifier.background(FitOutline)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        } else {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) FitGreen
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * AI 智能生成计划 — 全宽渐变按钮 + glow 脉冲
 */
@Composable
private fun AIGenerateButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "aiBtnScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "aiPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val aiFloatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(GradientPrimary))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标 + 标题
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .graphicsLayer { translationY = aiFloatOffset },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            "AI 智能生成计划",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            "根据你的状态智能匹配训练",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Lovable 风格训练记录卡片 — 轻量展开收起式
 */
@Composable
private fun WeeklyRecordCard(
    modifier: Modifier = Modifier,
    record: WorkoutRecord
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .scale(if (isPressed) 0.98f else 1f),
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FitSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, FitOutline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.linearGradient(GradientPrimary),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            record.date,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            "${record.totalDuration}分钟 · 感受 ${record.feeling}/5",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 展开详情
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = FitOutline)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(text = "代谢 ${record.metabolicPressure}", color = FitGreen)
                        StatusChip(text = "精神 ${record.mentalPressure}", color = FitBlue)
                        if (record.isDeload) {
                            StatusChip(text = "减载", color = FitOrange)
                        }
                    }
                    if (record.aiSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = FitGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "AI 建议",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FitGreen
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            record.aiSummary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lovable 风格计划卡片 — 展开收起式 + 强度标签
 */
@Composable
private fun LovablePlanCard(
    modifier: Modifier = Modifier,
    plan: WorkoutPlan,
    onClick: () -> Unit,
    onStartWorkout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "planCardScale"
    )

    val intensityColor = when (plan.goal) {
        "muscle_gain" -> Triple(FitGreen, FitGreenContainer, FitGreen)
        "fat_loss" -> Triple(FitBlue, FitBlueContainer, FitBlue)
        "strength" -> Triple(FitOrange, FitOrangeContainer, FitOrange)
        else -> Triple(FitGreen, FitGreenContainer, FitGreen)
    }
    val intensityLabel = getGoalLabel(plan.goal)
    val intensityBg = intensityColor.second
    val intensityFg = intensityColor.first

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FitSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, FitOutline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(GradientPrimary),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))

                // 标题 + 标签
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            plan.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // 强度标签
                        Box(
                            modifier = Modifier
                                .background(intensityBg, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                intensityLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = intensityFg,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                    if (plan.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            plan.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 展开详情
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(if (expanded) "收起" else "详情")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 开始训练
                val startInteraction = remember { MutableInteractionSource() }
                val startPressed by startInteraction.collectIsPressedAsState()
                val startScale by animateFloatAsState(
                    targetValue = if (startPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
                    label = "startScale"
                )
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier
                        .weight(1f)
                        .scale(startScale),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FitGreen
                    ),
                    interactionSource = startInteraction
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("开始")
                }
            }
        }
    }
}

/**
 * 空训练记录卡片
 */
@Composable
private fun EmptyTrainingCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FitSurface
        ),
        border = BorderStroke(1.dp, FitOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🏋️", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "本周还没有训练记录",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "动起来，第一课就开始！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 空计划卡片
 */
@Composable
private fun EmptyPlanCard(modifier: Modifier = Modifier, onNavigate: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FitSurface
        ),
        border = BorderStroke(1.dp, FitOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📋", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "还没有训练计划",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            BouncyButton(
                onClick = onNavigate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FitGreen
                )
            ) {
                Text("创建计划")
            }
        }
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 6 -> "夜深了 🌙"
        hour < 9 -> "早上好 ☀️"
        hour < 12 -> "上午好 💪"
        hour < 14 -> "中午好 🍽️"
        hour < 18 -> "下午好 ⚡"
        else -> "晚上好 🌆"
    }
}
