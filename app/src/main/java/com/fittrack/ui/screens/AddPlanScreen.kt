package com.fittrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fittrack.data.entity.WorkoutPlan
import com.fittrack.ui.viewmodel.FitTrackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlanScreen(
    viewModel: FitTrackViewModel,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedGoal by remember { mutableStateOf("general") }
    var cycleDays by remember { mutableStateOf("7") }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf("08:00") }

    val goals = listOf(
        "general" to "综合训练",
        "muscle_gain" to "增肌",
        "fat_loss" to "减脂",
        "strength" to "力量",
        "endurance" to "耐力"
    )

    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建计划") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 计划名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("计划名称 *") },
                placeholder = { Text("例如：增肌计划、减脂计划") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 计划描述
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("计划描述") },
                placeholder = { Text("简单描述一下这个计划的目标") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // 训练目标
            Text(
                "训练目标",
                style = MaterialTheme.typography.titleSmall
            )
           goals.forEach { (value, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGoal == value,
                        onClick = { selectedGoal = value }
                    )
                    Text(label)
                }
            }

            HorizontalDivider()

            // 周期设置
            OutlinedTextField(
                value = cycleDays,
                onValueChange = { cycleDays = it.filter { c -> c.isDigit() } },
                label = { Text("计划周期（天）") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            HorizontalDivider()

            // 提醒设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("训练提醒", style = MaterialTheme.typography.titleSmall)
                    Text("开启后会在训练日提醒你", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it }
                )
            }

            if (reminderEnabled) {
                OutlinedTextField(
                    value = reminderTime,
                    onValueChange = { },
                    label = { Text("提醒时间") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "选择时间")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 保存按钮
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val plan = WorkoutPlan(
                            name = name,
                            description = description,
                            goal = selectedGoal,
                            cycleDays = cycleDays.toIntOrNull() ?: 7,
                            reminderTime = reminderTime,
                            isActive = true
                        )
                        viewModel.savePlan(plan)
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存计划")
            }
        }
    }

    if (showTimePicker) {
        val timeParts = reminderTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        reminderTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
