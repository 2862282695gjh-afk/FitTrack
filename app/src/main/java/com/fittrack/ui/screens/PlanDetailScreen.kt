package com.fittrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fittrack.data.entity.Exercise
import com.fittrack.data.entity.WorkoutPlan
import com.fittrack.ui.viewmodel.FitTrackViewModel
import kotlinx.coroutines.launch

// 星期几的中文映射
private val dayNames = mapOf(
    1 to "周一",
    2 to "周二",
    3 to "周三",
    4 to "周四",
    5 to "周五",
    6 to "周六",
    7 to "周日"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planId: Long,
    viewModel: FitTrackViewModel,
    onNavigateBack: () -> Unit,
    onStartWorkout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val selectedPlan by viewModel.selectedPlan.collectAsState()
    val exercises by viewModel.planExercises.collectAsState()
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(planId) {
        viewModel.selectPlan(planId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedPlan?.name ?: "计划详情") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExerciseDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加动作")
            }
        }
    ) { padding ->
        selectedPlan?.let { currentPlan ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 计划信息
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (currentPlan.description.isNotEmpty()) {
                                Text(
                                    currentPlan.description,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(getGoalLabel(currentPlan.goal)) }
                                )
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text("${currentPlan.cycleDays}天周期") }
                                )
                            }
                        }
                    }
                }

                // 开始训练按钮
                item {
                    Button(
                        onClick = {
                            viewModel.startWorkout(currentPlan)
                            onStartWorkout()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开始训练")
                    }
                }

                // 动作列表标题
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "训练动作",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${exercises.size} 个动作",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (exercises.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "还没有添加训练动作",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { showAddExerciseDialog = true }) {
                                    Text("添加第一个动作")
                                }
                            }
                        }
                    }
                } else {
                    // 按训练日分组显示动作
                    val groupedExercises = exercises.groupBy { it.dayOfWeek }.toSortedMap()

                    groupedExercises.forEach { (dayOfWeek, dayExercises) ->
                        // 训练日标题
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        dayNames[dayOfWeek] ?: "第${dayOfWeek}天",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "${dayExercises.size} 个动作",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // 该训练日的动作列表
                        items(dayExercises.sortedBy { it.orderIndex }, key = { it.id }) { exercise ->
                            ExerciseItem(
                                exercise = exercise,
                                onDelete = { viewModel.deleteExercise(exercise) }
                            )
                        }
                    }
                }
            }
        }
    }

    // 添加动作对话框
    if (showAddExerciseDialog) {
        AddExerciseDialog(
            planId = planId,
            onDismiss = { showAddExerciseDialog = false },
            onAdd = { name, sets, reps, weight, duration, category ->
                viewModel.addExercise(
                    planId = planId,
                    name = name,
                    sets = sets,
                    reps = reps,
                    weight = weight,
                    duration = duration,
                    category = category
                )
                showAddExerciseDialog = false
            }
        )
    }
}

@Composable
fun ExerciseItem(
    exercise: Exercise,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (exercise.notes.isNotEmpty()) {
                    Text(
                        exercise.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (exercise.defaultSets > 0 && exercise.defaultReps > 0) {
                        Text(
                            "${exercise.defaultSets}组 x ${exercise.defaultReps}次",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (exercise.defaultWeight > 0) {
                        Text(
                            "${exercise.defaultWeight}kg",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (exercise.defaultDuration > 0) {
                        Text(
                            "${exercise.defaultDuration}分钟",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除动作") },
            text = { Text("确定要删除「${exercise.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    planId: Long,
    onDismiss: () -> Unit,
    onAdd: (name: String, sets: Int, reps: Int, weight: Double, duration: Int, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("other") }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var weight by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }

    val categories = listOf(
        "chest" to "胸部",
        "back" to "背部",
        "legs" to "腿部",
        "shoulders" to "肩部",
        "arms" to "手臂",
        "core" to "核心",
        "cardio" to "有氧",
        "other" to "其他"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加训练动作") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("动作名称 *") },
                    placeholder = { Text("例如：深蹲、卧推") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("分类", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { (value, label) ->
                        FilterChip(
                            selected = category == value,
                            onClick = { category = value },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it.filter { c -> c.isDigit() } },
                        label = { Text("组数") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { c -> c.isDigit() } },
                        label = { Text("次数") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("重量(kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it.filter { c -> c.isDigit() } },
                        label = { Text("时长(分钟)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            name,
                            sets.toIntOrNull() ?: 3,
                            reps.toIntOrNull() ?: 10,
                            weight.toDoubleOrNull() ?: 0.0,
                            duration.toIntOrNull() ?: 0,
                            category
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
