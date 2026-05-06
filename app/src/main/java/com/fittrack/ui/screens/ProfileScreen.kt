package com.fittrack.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fittrack.ui.viewmodel.ProfileEditState
import com.fittrack.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 当前选中的照片类型
    var selectedPhotoType by remember { mutableStateOf("front") }

    // 照片选择器 - 使用 OpenDocument 替代 GetContent 以获得更好的兼容性
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // 持久化 URI 权限，以便后续访问
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setPhotoUri(selectedPhotoType, it)
        }
    }

    // 备用的照片选择器（使用 GetContent 作为备选方案）
    val fallbackPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setPhotoUri(selectedPhotoType, it) }
    }

    // 显示消息 SnackBar
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "我的档案",
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
                actions = {
                    IconButton(
                        onClick = { viewModel.saveProfile() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "保存")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 基本信息
                BasicInfoSection(
                    editState = uiState.editState,
                    onStateChange = { viewModel.updateEditState(it) }
                )

                // 健身目标
                FitnessGoalSection(
                    editState = uiState.editState,
                    onStateChange = { viewModel.updateEditState(it) }
                )

                // 体态照片
                BodyPhotoSection(
                    frontPhotoUri = uiState.frontPhotoUri,
                    sidePhotoUri = uiState.sidePhotoUri,
                    backPhotoUri = uiState.backPhotoUri,
                    existingFrontPath = uiState.profile?.frontPhotoPath,
                    existingSidePath = uiState.profile?.sidePhotoPath,
                    existingBackPath = uiState.profile?.backPhotoPath,
                    onSelectPhoto = { type ->
                        selectedPhotoType = type
                        // 直接启动图片选择器，不需要存储权限
                        // 对于 Android 13+ 使用 OpenDocument，对于老版本使用 GetContent
                        try {
                            photoPickerLauncher.launch(arrayOf("image/*"))
                        } catch (e: Exception) {
                            // 如果 OpenDocument 失败，回退到 GetContent
                            fallbackPhotoPickerLauncher.launch("image/*")
                        }
                    },
                    onAnalyze = { viewModel.analyzeBodyPhoto() },
                    isAnalyzing = uiState.isAnalyzing,
                    bodyAnalysis = uiState.bodyAnalysis
                )

                // 智能计划生成
                if (uiState.bodyAnalysis != null) {
                    AiPlanSection(
                        isGenerating = uiState.isGeneratingPlan,
                        generatedPlanJson = uiState.generatedPlanJson,
                        onGeneratePlan = { viewModel.generateWorkoutPlan() }
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun BasicInfoSection(
    editState: ProfileEditState,
    onStateChange: (ProfileEditState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "基本信息",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // 姓名
            OutlinedTextField(
                value = editState.name,
                onValueChange = { onStateChange(editState.copy(name = it)) },
                label = { Text("姓名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 性别
                OutlinedTextField(
                    value = editState.gender,
                    onValueChange = { onStateChange(editState.copy(gender = it)) },
                    label = { Text("性别") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            if (editState.gender == "男") Icons.Default.Male else Icons.Default.Female,
                            contentDescription = null
                        )
                    },
                    placeholder = { Text("男/女") }
                )

                // 年龄
                OutlinedTextField(
                    value = editState.age,
                    onValueChange = { onStateChange(editState.copy(age = it)) },
                    label = { Text("年龄") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Cake, contentDescription = null)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 身高
                OutlinedTextField(
                    value = editState.heightCm,
                    onValueChange = { onStateChange(editState.copy(heightCm = it)) },
                    label = { Text("身高 (cm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Icon(Icons.Default.Height, contentDescription = null)
                    }
                )

                // 体重
                OutlinedTextField(
                    value = editState.weightKg,
                    onValueChange = { onStateChange(editState.copy(weightKg = it)) },
                    label = { Text("体重 (kg)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Icon(Icons.Default.MonitorWeight, contentDescription = null)
                    }
                )
            }

            // 目标体重
            OutlinedTextField(
                value = editState.targetWeightKg,
                onValueChange = { onStateChange(editState.copy(targetWeightKg = it)) },
                label = { Text("目标体重 (kg)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = {
                    Icon(Icons.Default.TrackChanges, contentDescription = null)
                }
            )
        }
    }
}

@Composable
private fun FitnessGoalSection(
    editState: ProfileEditState,
    onStateChange: (ProfileEditState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "健身目标",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // 健身目标选择
            var goalExpanded by remember { mutableStateOf(false) }
            val goalOptions = listOf("减脂", "增肌", "塑形", "保持健康", "提升体能")

            ExposedDropdownMenuBox(
                expanded = goalExpanded,
                onExpandedChange = { goalExpanded = it }
            ) {
                OutlinedTextField(
                    value = editState.fitnessGoal,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("健身目标") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    leadingIcon = {
                        Icon(Icons.Default.Flag, contentDescription = null)
                    }
                )
                ExposedDropdownMenu(
                    expanded = goalExpanded,
                    onDismissRequest = { goalExpanded = false }
                ) {
                    goalOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onStateChange(editState.copy(fitnessGoal = option))
                                goalExpanded = false
                            }
                        )
                    }
                }
            }

            // 健身经验
            var expExpanded by remember { mutableStateOf(false) }
            val expOptions = listOf("新手", "初级", "中级", "高级", "专业")

            ExposedDropdownMenuBox(
                expanded = expExpanded,
                onExpandedChange = { expExpanded = it }
            ) {
                OutlinedTextField(
                    value = editState.experienceLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("健身经验") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    leadingIcon = {
                        Icon(Icons.Default.TrendingUp, contentDescription = null)
                    }
                )
                ExposedDropdownMenu(
                    expanded = expExpanded,
                    onDismissRequest = { expExpanded = false }
                ) {
                    expOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onStateChange(editState.copy(experienceLevel = option))
                                expExpanded = false
                            }
                        )
                    }
                }
            }

            // 每周可用时间
            OutlinedTextField(
                value = editState.weeklyMinutes,
                onValueChange = { onStateChange(editState.copy(weeklyMinutes = it)) },
                label = { Text("每周可用时间 (分钟)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                }
            )

            // 健康问题
            OutlinedTextField(
                value = editState.healthIssues,
                onValueChange = { onStateChange(editState.copy(healthIssues = it)) },
                label = { Text("健康问题（如有）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                leadingIcon = {
                    Icon(Icons.Default.Healing, contentDescription = null)
                },
                placeholder = { Text("请描述任何可能影响训练的健康问题") }
            )
        }
    }
}

@Composable
private fun BodyPhotoSection(
    frontPhotoUri: Uri?,
    sidePhotoUri: Uri?,
    backPhotoUri: Uri?,
    existingFrontPath: String?,
    existingSidePath: String?,
    existingBackPath: String?,
    onSelectPhoto: (String) -> Unit,
    onAnalyze: () -> Unit,
    isAnalyzing: Boolean,
    bodyAnalysis: com.fittrack.data.api.ParsedBodyAnalysis?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "体态照片",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "上传正面照片进行体态分析（可选上传侧面和背面照片获得更全面的分析）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 照片行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhotoSlot(
                    label = "正面",
                    photoUri = frontPhotoUri,
                    existingPath = existingFrontPath,
                    onClick = { onSelectPhoto("front") },
                    modifier = Modifier.weight(1f)
                )
                PhotoSlot(
                    label = "侧面",
                    photoUri = sidePhotoUri,
                    existingPath = existingSidePath,
                    onClick = { onSelectPhoto("side") },
                    modifier = Modifier.weight(1f)
                )
                PhotoSlot(
                    label = "背面",
                    photoUri = backPhotoUri,
                    existingPath = existingBackPath,
                    onClick = { onSelectPhoto("back") },
                    modifier = Modifier.weight(1f)
                )
            }

            // AI 分析按钮
            Button(
                onClick = onAnalyze,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI 分析中...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI 体态分析")
                }
            }

            // 分析结果
            bodyAnalysis?.let { analysis ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AI 分析结果",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // 体态评分（主要或备用）
                        val score = analysis.postureScore ?: analysis.estimatedBodyFat.toInt()
                        if (analysis.postureScore != null || analysis.estimatedBodyFat > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("体态评分", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    "${analysis.postureScore ?: analysis.estimatedBodyFat.toInt()} / 100",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // 体脂率（主要或备用）
                        val bodyFat = analysis.bodyFatPercentage ?: analysis.estimatedBodyFat
                        if (bodyFat > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("预估体脂率", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${String.format("%.1f", bodyFat)}%", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        // 肌肉平衡或肌肉量
                        (analysis.muscleBalance ?: analysis.muscleMass)?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                "肌肉状态: $it",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // 体型
                        if (analysis.bodyType.isNotBlank()) {
                            Text(
                                "体型: ${analysis.bodyType}",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // 问题列表
                        analysis.issues.takeIf { it.isNotEmpty() }?.let { issues ->
                            Text(
                                "发现问题:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            issues.forEach { issue ->
                                Text(
                                    "• $issue",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // 姿势问题（备用）
                        analysis.postureIssues.takeIf { it.isNotEmpty() && analysis.issues.isEmpty() }?.let { issues ->
                            Text(
                                "体态问题:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            issues.forEach { issue ->
                                Text(
                                    "• $issue",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // 建议列表
                        analysis.suggestions.takeIf { it.isNotEmpty() }?.let { suggestions ->
                            Text(
                                "建议:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            suggestions.forEach { suggestion ->
                                Text(
                                    "• $suggestion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // 推荐列表（备用）
                        analysis.recommendations.takeIf { it.isNotEmpty() && analysis.suggestions.isEmpty() }?.let { recommendations ->
                            Text(
                                "推荐:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            recommendations.forEach { recommendation ->
                                Text(
                                    "• $recommendation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoSlot(
    label: String,
    photoUri: Uri?,
    existingPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPhoto = photoUri != null || !existingPath.isNullOrBlank()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(3f / 4f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (hasPhoto) 0.dp else 1.dp,
                    color = if (hasPhoto) Color.Transparent else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            when {
                photoUri != null -> {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                !existingPath.isNullOrBlank() -> {
                    AsyncImage(
                        model = existingPath,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiPlanSection(
    isGenerating: Boolean,
    generatedPlanJson: String?,
    onGeneratePlan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "智能训练计划",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "根据您的体态分析结果和个人信息，AI 将为您生成个性化的训练计划",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 生成计划按钮
            Button(
                onClick = onGeneratePlan,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI 生成中...")
                } else {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成训练计划")
                }
            }

            // 显示生成的计划
            generatedPlanJson?.let { planJson ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "训练计划已生成",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = "计划详情请查看「计划」页面或咨询 AI 教练",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
