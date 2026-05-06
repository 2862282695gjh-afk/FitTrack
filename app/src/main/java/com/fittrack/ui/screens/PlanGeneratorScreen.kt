package com.fittrack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.data.api.DailyWorkout
import com.fittrack.data.api.ParsedWorkoutPlan
import com.fittrack.data.api.PlannedExercise
import com.fittrack.ui.theme.*
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

    // 当成功时自动保存（用 hasSaved 防止重复触发）
    var hasSaved by remember { mutableStateOf(false) }

    LaunchedEffect(generationState) {
        if (generationState is PlanGenerationState.Success && !hasSaved) {
            hasSaved = true
            val plan = (generationState as PlanGenerationState.Success).plan
            viewModel.savePlan(plan) { planId ->
                onPlanSaved(planId)
            }
        }
    }

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
                    PlanConfigSection(
                        selectedGoal = selectedGoal,
                        onGoalChange = { viewModel.updateGoal(it) },
                        weeklyDays = weeklyDays,
                        onWeeklyDaysChange = { viewModel.updateWeeklyDays(it) },
                        sessionDuration = sessionDuration,
                        onSessionDurationChange = { viewModel.updateSessionDuration(it) },
                        experience = experience,
                        onExperienceChange = { viewModel.updateExperience(it) },
                        onGenerate = { viewModel.generatePlan() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is PlanGenerationState.Loading -> {
                    LoadingSection(
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is PlanGenerationState.Success -> {
                    // 自动保存中...显示短暂过渡
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "计划已生成，正在保存...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is PlanGenerationState.Error -> {
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

// ═══════════════════════════════════════════════════════════════
// 训练目标数据（含描述）
// ═══════════════════════════════════════════════════════════════

private data class GoalOption(
    val label: String,
    val value: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

private val goalOptions = listOf(
    GoalOption(
        label = "综合健身",
        value = "综合健身",
        description = "全面提升体能，适合刚开始系统锻炼的你",
        icon = Icons.Default.FitnessCenter,
        color = FitGreen
    ),
    GoalOption(
        label = "减脂塑形",
        value = "减脂塑形",
        description = "降低体脂率、雕刻线条，适合想瘦身的你",
        icon = Icons.Default.LocalFireDepartment,
        color = FitOrange
    ),
    GoalOption(
        label = "增肌增重",
        value = "增肌增重",
        description = "增加肌肉量、提升围度，适合偏瘦的你",
        icon = Icons.Default.ArrowUpward,
        color = FitBlue
    ),
    GoalOption(
        label = "力量提升",
        value = "力量提升",
        description = "提高绝对力量，适合有基础的进阶训练者",
        icon = Icons.Default.EmojiEvents,
        color = FitYellow
    ),
    GoalOption(
        label = "耐力训练",
        value = "耐力训练",
        description = "增强心肺功能，适合跑步/运动爱好者",
        icon = Icons.Default.DirectionsBike,
        color = FitPurple
    )
)

// ═══════════════════════════════════════════════════════════════
// 训练经验等级数据
// ═══════════════════════════════════════════════════════════════

private data class ExperienceOption(
    val label: String,
    val value: String,
    val description: String,
    val icon: ImageVector
)

private val experienceOptions = listOf(
    ExperienceOption(
        label = "初学者",
        value = "beginner",
        description = "健身不到 6 个月",
        icon = Icons.Default.ChildCare
    ),
    ExperienceOption(
        label = "中级",
        value = "intermediate",
        description = "健身 6 个月 ~ 2 年",
        icon = Icons.Default.Eco
    ),
    ExperienceOption(
        label = "高级",
        value = "advanced",
        description = "健身超过 2 年",
        icon = Icons.Default.Park
    )
)

// ═══════════════════════════════════════════════════════════════
// 配置区域
// ═══════════════════════════════════════════════════════════════

/**
 * 计划配置区域
 */
@Composable
fun PlanConfigSection(
    selectedGoal: String,
    onGoalChange: (String) -> Unit,
    weeklyDays: Int,
    onWeeklyDaysChange: (Int) -> Unit,
    sessionDuration: Int,
    onSessionDurationChange: (Int) -> Unit,
    experience: String,
    onExperienceChange: (String) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 说明卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
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

        // ── 训练目标（2 列网格）──
        SectionTitle(title = "选择训练目标", icon = Icons.Default.Flag)

        goalOptions.forEach { goal ->
            val isSelected = selectedGoal == goal.label
            GoalCard(
                goal = goal,
                isSelected = isSelected,
                onClick = { onGoalChange(goal.label) }
            )
        }

        // ── 健身经验（3 列横排）──
        SectionTitle(title = "健身经验", icon = Icons.Default.Timeline)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            experienceOptions.forEach { exp ->
                val isSelected = experience == exp.value
                ExperienceChip(
                    option = exp,
                    isSelected = isSelected,
                    onClick = { onExperienceChange(exp.value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── 每周训练天数 ──
        SectionTitle(title = "每周训练天数", icon = Icons.Default.CalendarMonth)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
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
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 80.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { onWeeklyDaysChange(weeklyDays + 1) },
                enabled = weeklyDays < 7
            ) {
                Icon(Icons.Default.Add, contentDescription = "增加")
            }
        }

        // ── 每次训练时长 ──
        SectionTitle(title = "每次训练时长", icon = Icons.Default.Timer)

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

        // ── 生成按钮 ──
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
 * 段落标题
 */
@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
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
}

/**
 * 目标卡片 — 全宽选择卡片
 */
@Composable
private fun GoalCard(
    goal: GoalOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) goal.color else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200),
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) goal.color.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(200),
        label = "bg"
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(goal.color.copy(alpha = if (isSelected) 0.15f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = goal.icon,
                    contentDescription = null,
                    tint = if (isSelected) goal.color else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) goal.color else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 选中指示器
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = goal.color,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * 经验等级选择 — 紧凑竖向卡片
 */
@Composable
private fun ExperienceChip(
    option: ExperienceOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200),
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(200),
        label = "bg"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = option.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 加载界面 — 多步骤管线进度
// ═══════════════════════════════════════════════════════════════

private data class PipelineStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val thinkingHint: String
)

private val pipelineSteps = listOf(
    PipelineStep(
        title = "构建用户画像",
        description = "分析你的健身目标和经验水平",
        icon = Icons.Default.PersonSearch,
        thinkingHint = "正在根据训练目标评估身体状态..."
    ),
    PipelineStep(
        title = "分析运动偏好",
        description = "匹配合适的动作组合和训练强度",
        icon = Icons.Default.Analytics,
        thinkingHint = "正在筛选最优动作组合..."
    ),
    PipelineStep(
        title = "制定训练计划",
        description = "编排每周训练日程和动作安排",
        icon = Icons.Default.EditCalendar,
        thinkingHint = "正在优化训练频率和恢复周期..."
    ),
    PipelineStep(
        title = "优化计划细节",
        description = "调整组数、次数和重量建议",
        icon = Icons.Default.Tune,
        thinkingHint = "正在细化每组训练参数..."
    )
)

@Composable
fun LoadingSection(modifier: Modifier = Modifier) {
    // 自动推进管线步骤（每步约 3 秒）
    var currentStep by remember { mutableIntStateOf(0) }
    val infiniteTransition = rememberInfiniteTransition()

    // 进度条动画
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stepProgress"
    )

    // 自动切换步骤
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3500)
            if (currentStep < pipelineSteps.lastIndex) {
                currentStep++
            } else {
                // 循环最后一步
                currentStep = pipelineSteps.lastIndex
            }
        }
    }

    // 打字机效果的思考文字
    val currentThinking = pipelineSteps[currentStep].thinkingHint
    var displayedThinking by remember(currentStep) { mutableStateOf("") }
    val thinkingAnimatable = remember { Animatable(0f) }

    LaunchedEffect(currentStep) {
        displayedThinking = ""
        thinkingAnimatable.snapTo(0f)
        thinkingAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(3000, easing = LinearEasing)
        )
    }

    LaunchedEffect(thinkingAnimatable.value, currentStep) {
        val fullText = currentThinking
        val charCount = (thinkingAnimatable.value * fullText.length).toInt()
        if (charCount <= fullText.length) {
            displayedThinking = fullText.take(charCount)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 顶部标题
        Text(
            text = "AI 正在为你生成专属计划",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        // 整体进度条
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "总体进度",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${((currentStep + 1).toFloat() / pipelineSteps.size * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (currentStep + progress) / pipelineSteps.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                strokeCap = StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // 管线步骤列表
        pipelineSteps.forEachIndexed { index, step ->
            val isActive = index == currentStep
            val isCompleted = index < currentStep

            PipelineStepItem(
                step = step,
                isActive = isActive,
                isCompleted = isCompleted
            )
        }

        // AI 思考过程面板
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = FitPurpleContainer.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, FitPurple.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = FitPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI 思考过程",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FitPurple
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = displayedThinking,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 底部提示
        Text(
            text = "请耐心等待，AI 正在精心为你打造最佳训练方案",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 文字记忆变量名修正（Compose 需要记住的是 displayedThinking）
@Composable
private fun PipelineStepItem(
    step: PipelineStep,
    isActive: Boolean,
    isCompleted: Boolean
) {
    val iconColor = when {
        isCompleted -> MaterialTheme.colorScheme.primary
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            } else if (isActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${pipelineSteps.indexOf(step) + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 文字
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isActive || isCompleted) 1f else 0.4f
                )
            )
        }

        // 状态标签
        if (isCompleted) {
            SuggestionChip(
                onClick = {},
                label = { Text("完成", fontSize = 11.sp) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.primary
                )
            )
        } else if (isActive) {
            SuggestionChip(
                onClick = {},
                label = { Text("进行中", fontSize = 11.sp) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    labelColor = MaterialTheme.colorScheme.tertiary
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 错误界面
// ═══════════════════════════════════════════════════════════════

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
