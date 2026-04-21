package com.fittrack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.data.api.DailyWorkout
import com.fittrack.data.api.ParsedWorkoutPlan
import com.fittrack.data.api.PlannedExercise
import com.fittrack.ui.viewmodel.PlanGenerationState
import com.fittrack.ui.viewmodel.PlanGeneratorViewModel

/**
 * 智能训练计划生成界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanGeneratorScreen(
    viewModel: PlanGeneratorViewModel,
    onNavigateBack: () -> Unit,
    onPlanSaved: (Long) -> Unit
) {
    val generationState by viewModel.generationState.collectAsState()
    val selectedGoal by viewModel.selectedGoal.collectAsState()
    val weeklyDays by viewModel.weeklyDays.collectAsState()
    val sessionDuration by viewModel.sessionDuration.collectAsState()
    val experience by viewModel.experience.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI 智能生成计划")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = generationState) {
                is PlanGenerationState.Idle -> {
                    // 配置界面
                    PlanConfigSection(
                        selectedGoal = selectedGoal,
                        goals = viewModel.goals,
                        onGoalChange = { viewModel.updateGoal(it) },
                        weeklyDays = weeklyDays,
                        onWeeklyDaysChange = { viewModel.updateWeeklyDays(it) },
                        sessionDuration = sessionDuration,
                        onSessionDurationChange = { viewModel.updateSessionDuration(it) },
                        experience = experience,
                        experienceLevels = viewModel.experienceLevels,
                        onExperienceChange = { viewModel.updateExperience(it) },
                        onGenerate = { viewModel.generatePlan() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is PlanGenerationState.Loading -> {
                    // 加载界面
                    LoadingSection(
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is PlanGenerationState.Success -> {
                    // 结果展示界面
                    PlanResultSection(
                        plan = state.plan,
                        onSave = {
                            viewModel.savePlan(state.plan) { planId ->
                                onPlanSaved(planId)
                            }
                        },
                        onRegenerate = { viewModel.reset() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is PlanGenerationState.Error -> {
                    // 错误界面
                    ErrorSection(
                        message = state.message,
                        onRetry = { viewModel.generatePlan() },
                        onBack = { viewModel.reset() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * 计划配置区域
 */
@Composable
fun PlanConfigSection(
    selectedGoal: String,
    goals: List<Pair<String, String>>,
    onGoalChange: (String) -> Unit,
    weeklyDays: Int,
    onWeeklyDaysChange: (Int) -> Unit,
    sessionDuration: Int,
    onSessionDurationChange: (Int) -> Unit,
    experience: String,
    experienceLevels: List<Pair<String, String>>,
    onExperienceChange: (String) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 说明卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI 将根据你的目标和经验，为你生成个性化的训练计划",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // 训练目标选择
        ConfigSection(title = "训练目标", icon = Icons.Default.Flag) {
            goals.forEach { (label, value) ->
                FilterChip(
                    selected = selectedGoal == label,
                    onClick = { onGoalChange(label) },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // 健身经验
        ConfigSection(title = "健身经验", icon = Icons.Default.Timeline) {
            experienceLevels.forEach { (label, value) ->
                FilterChip(
                    selected = experience == value,
                    onClick = { onExperienceChange(value) },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // 每周训练天数
        ConfigSection(title = "每周训练天数", icon = Icons.Default.CalendarMonth) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { onWeeklyDaysChange(weeklyDays - 1) },
                    enabled = weeklyDays > 1
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "减少")
                }
                Text(
                    text = "$weeklyDays 天",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onWeeklyDaysChange(weeklyDays + 1) },
                    enabled = weeklyDays < 7
                ) {
                    Icon(Icons.Default.Add, contentDescription = "增加")
                }
            }
        }

        // 每次训练时长
        ConfigSection(title = "每次训练时长", icon = Icons.Default.Timer) {
            Column {
                Slider(
                    value = sessionDuration.toFloat(),
                    onValueChange = { onSessionDurationChange(it.toInt()) },
                    valueRange = 20f..120f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "$sessionDuration 分钟",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 生成按钮
        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "生成训练计划",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * 配置区域通用组件
 */
@Composable
fun ConfigSection(
    title: String,
    icon: ImageVector,
    content: @Composable RowScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * 加载界面
 */
@Composable
fun LoadingSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "AI 正在为你生成专属计划...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "这可能需要几秒钟",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * 计划结果展示
 */
@Composable
fun PlanResultSection(
    plan: ParsedWorkoutPlan,
    onSave: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 计划头部
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (plan.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("目标: ${plan.goal}") },
                        icon = {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text("周期: ${plan.cycleWeeks}周") },
                        icon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // 每日计划
        plan.weeklySchedule.forEach { dailyWorkout ->
            DailyWorkoutCard(
                dailyWorkout = dailyWorkout,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRegenerate,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("重新生成")
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存计划")
            }
        }
    }
}

/**
 * 每日训练卡片
 */
@Composable
fun DailyWorkoutCard(
    dailyWorkout: DailyWorkout,
    modifier: Modifier = Modifier
) {
    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 日期标题
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayNames.getOrElse(dailyWorkout.dayOfWeek - 1) { "第${dailyWorkout.dayOfWeek}天" },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${dailyWorkout.exercises.size} 个动作",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (dailyWorkout.notes.isNotBlank()) {
                Text(
                    text = dailyWorkout.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 动作列表
            dailyWorkout.exercises.forEachIndexed { index, exercise ->
                ExerciseItem(
                    exercise = exercise,
                    index = index + 1,
                    modifier = Modifier.fillMaxWidth()
                )
                if (index < dailyWorkout.exercises.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

/**
 * 动作项
 */
@Composable
fun ExerciseItem(
    exercise: PlannedExercise,
    index: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 序号
        Box(
            modifier = Modifier
                .size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${exercise.sets}组 × ${exercise.reps}次",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (exercise.weight.isNotBlank()) {
                    Text(
                        text = exercise.weight,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (exercise.notes.isNotBlank()) {
                Text(
                    text = exercise.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // 类别标签
        SuggestionChip(
            onClick = {},
            label = { Text(exercise.category, fontSize = 10.sp) },
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * 错误界面
 */
@Composable
fun ErrorSection(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "生成失败",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        SelectionContainer {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("返回修改")
            }
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}
