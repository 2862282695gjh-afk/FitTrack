package com.fittrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    // 计算压力指标
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
                title = { Text("训练统计") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 统计卡片
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "本周训练",
                        value = "${weeklyCount}次",
                        icon = Icons.Default.FitnessCenter
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "本月训练",
                        value = "${monthlyCount}次",
                        icon = Icons.Default.CalendarMonth
                    )
                }
            }

            // 压力分析卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (needsDeload)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (needsDeload) Icons.Default.Warning else Icons.Default.PieChart,
                                contentDescription = null,
                                tint = if (needsDeload)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "压力分析",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (needsDeload)
                                    MaterialTheme.colorScheme.onErrorContainer
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // 代谢压力
                        PressureBar(
                            label = "代谢压力",
                            value = avgMetabolicPressure,
                            color = if (avgMetabolicPressure >= 80)
                                MaterialTheme.colorScheme.error
                            else if (avgMetabolicPressure >= 60)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.primary
                        )

                        // 精神压力
                        PressureBar(
                            label = "精神压力",
                            value = avgMentalPressure,
                            color = if (avgMentalPressure >= 80)
                                MaterialTheme.colorScheme.error
                            else if (avgMentalPressure >= 60)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.primary
                        )

                        // 减载建议
                        if (needsDeload) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "建议安排减载训练，降低40-50%的强度",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onError
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "本周训练概览",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // 简单的周视图
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val days = listOf("一", "二", "三", "四", "五", "六", "日")
                            days.forEachIndexed { index, day ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // 这里可以根据实际训练记录显示
                                        if (index == 0 || index == 2 || index == 4) {
                                            // 示例：周一、三、五训练
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Circle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outlineVariant,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        day,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 目标进度
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "月度目标",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // 假设目标为每月12次训练
                        val goal = 12
                        val progress = (monthlyCount.toFloat() / goal).coerceIn(0f, 1f)

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 提示信息
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "坚持训练，你做得很棒！继续保持每周3-4次的训练频率。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PressureBar(
    label: String,
    value: Int,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "$value/100",
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = color
        )
    }
}
