package com.fittrack.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.ui.components.AccentCard
import com.fittrack.ui.components.AnimatedCounter
import com.fittrack.ui.components.GradientCard
import com.fittrack.ui.theme.*
import com.fittrack.ui.viewmodel.FitTrackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: FitTrackViewModel,
    onNavigateBack: () -> Unit
) {
    val weeklyCount by viewModel.weeklyWorkoutCount.collectAsStateWithLifecycle(0)
    val monthlyCount by viewModel.monthlyWorkoutCount.collectAsStateWithLifecycle(0)
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle(emptyList())

    val recentRecords = allRecords.take(7)
    val avgMetabolicPressure = if (recentRecords.isNotEmpty()) {
        recentRecords.map { it.metabolicPressure }.average().toInt()
    } else 0
    val avgMentalPressure = if (recentRecords.isNotEmpty()) {
        recentRecords.map { it.mentalPressure }.average().toInt()
    } else 0
    val needsDeload = recentRecords.any { it.isDeload }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "训练统计",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 统计卡片
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GradientCard(
                        modifier = Modifier.weight(1f),
                        gradientColors = GradientPrimary,
                        contentColor = Color.White,
                        elevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            AnimatedCounter(
                                count = weeklyCount,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                ),
                                suffix = "次"
                            )
                            Text(
                                "本周训练",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                    GradientCard(
                        modifier = Modifier.weight(1f),
                        gradientColors = GradientBlue,
                        contentColor = Color.White,
                        elevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            AnimatedCounter(
                                count = monthlyCount,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                ),
                                suffix = "次"
                            )
                            Text(
                                "本月训练",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }
            }

            // 压力分析卡片
            item {
                val gradientColors = if (needsDeload) GradientOrange else GradientBlue
                GradientCard(
                    gradientColors = gradientColors,
                    contentColor = Color.White,
                    elevation = 6.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (needsDeload) Icons.Default.Warning else Icons.Default.PieChart,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "压力分析",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        PressureBarDuolingo(
                            label = "代谢压力",
                            value = avgMetabolicPressure,
                            color = Color.White
                        )
                        PressureBarDuolingo(
                            label = "精神压力",
                            value = avgMentalPressure,
                            color = Color.White
                        )

                        if (needsDeload) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "建议安排减载训练，降低40-50%的强度",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 周训练概览
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "本周训练概览",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val days = listOf("一", "二", "三", "四", "五", "六", "日")
                            days.forEachIndexed { index, day ->
                                val trained = index == 0 || index == 2 || index == 4
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                color = if (trained)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (trained) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Text(
                                                day,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        day,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (trained)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 月度目标
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "月度目标",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val goal = 12
                        val progress = (monthlyCount.toFloat() / goal).coerceIn(0f, 1f)
                        val animatedProgress = remember { Animatable(0f) }
                        LaunchedEffect(monthlyCount) {
                            animatedProgress.animateTo(
                                targetValue = progress,
                                animationSpec = tween(1000, easing = FastOutSlowInEasing)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(50)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress.value)
                                    .height(20.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            GradientPrimary
                                        ),
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${monthlyCount}次 / ${goal}次",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // 提示信息
            item {
                AccentCard(
                    containerColor = FitBlueContainer,
                    contentColor = FitBlue
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = FitBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "坚持训练，你做得很棒！继续保持每周3-4次的训练频率。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FitBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PressureBarDuolingo(
    label: String,
    value: Int,
    color: Color
) {
    val animatedValue = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animatedValue.animateTo(
            targetValue = value / 100f,
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = color.copy(alpha = 0.9f)
                )
            )
            Text(
                text = "$value/100",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(50)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedValue.value)
                    .height(12.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}
