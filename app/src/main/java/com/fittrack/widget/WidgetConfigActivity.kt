package com.fittrack.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.data.db.FitTrackDatabase
import com.fittrack.data.entity.WorkoutPlan
import com.fittrack.ui.theme.FitTrackTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 小部件配置 Activity
 * 允许用户选择要显示的训练计划
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取小部件 ID
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // 如果 ID 无效，直接退出
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            FitTrackTheme {
                val viewModelFactory = WidgetConfigViewModelFactory(applicationContext)
                WidgetConfigScreen(
                    onPlanSelected = { planId ->
                        // 保存选择的计划 ID（可选功能）
                        // 更新小部件
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        // 立即更新小部件
                        lifecycleScope.launch {
                            try {
                                // 刷新数据缓存
                                val provider = WidgetDataProvider(applicationContext)
                                provider.refresh()
                                // 更新所有小部件
                                FitTrackWidget().updateAll(applicationContext)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            finish()
                        }
                    },
                    onCancel = {
                        // 取消配置
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_CANCELED, resultValue)
                        finish()
                    },
                    viewModelFactory = viewModelFactory
                )
            }
        }
    }
}

@Composable
fun WidgetConfigScreen(
    onPlanSelected: (Long) -> Unit,
    onCancel: () -> Unit,
    viewModelFactory: WidgetConfigViewModelFactory
) {
    val viewModel: WidgetConfigViewModel = viewModel(factory = viewModelFactory)
    var plans by remember { mutableStateOf<List<WorkoutPlan>>(emptyList()) }
    var selectedPlanId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        plans = viewModel.loadPlans()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择训练计划") },
                actions = {
                    TextButton(onClick = onCancel) {
                        Text("取消")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (plans.isEmpty()) {
                // 没有计划
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("还没有训练计划")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onCancel) {
                            Text("返回")
                        }
                    }
                }
            } else {
                // 计划列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "选择一个训练计划显示在小部件上",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    items(plans) { plan ->
                        PlanCard(
                            plan = plan,
                            isSelected = selectedPlanId == plan.id,
                            onClick = {
                                selectedPlanId = plan.id
                                onPlanSelected(plan.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: WorkoutPlan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (plan.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "训练日：${plan.reminderDays}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 配置页面的 ViewModel
 */
class WidgetConfigViewModel(
    private val context: Context
) : ViewModel() {
    private val database = FitTrackDatabase.getDatabase(context)

    suspend fun loadPlans(): List<WorkoutPlan> {
        return database.workoutPlanDao().getAllPlans().first()
    }
}

/**
 * ViewModel 工厂类
 */
class WidgetConfigViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WidgetConfigViewModel::class.java)) {
            return WidgetConfigViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
