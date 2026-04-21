package com.fittrack.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fittrack.MainActivity
import com.fittrack.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FitTrack 桌面小部件
 * 支持俏皮话显示、逾期提醒、动作翻滚
 */
class FitTrackWidget : GlanceAppWidget() {

    // 使用 Single 尺寸模式
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 获取数据，使用空数据作为后备
        val data = withContext(Dispatchers.IO) {
            try {
                val provider = WidgetDataProvider(context)
                provider.getWidgetData()
            } catch (e: Exception) {
                // 出错时返回空数据，显示默认的休息日界面
                WidgetData(
                    status = WorkoutStatus.REST_DAY,
                    funPhrase = "小部件初始化中..."
                )
            }
        }

        provideContent {
            GlanceTheme {
                WidgetContent(data)
            }
        }
    }

    companion object {
        /**
         * 更新所有小部件
         */
        suspend fun updateAll(context: Context) {
            FitTrackWidget().updateAll(context)
        }
    }
}

/**
 * 小部件内容
 */
@Composable
private fun WidgetContent(data: WidgetData) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(getBackgroundColor(data.status))
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        when (data.status) {
            WorkoutStatus.REST_DAY -> RestDayContent(data)
            WorkoutStatus.PENDING -> PendingContent(data)
            WorkoutStatus.COMPLETED -> CompletedContent(data)
            WorkoutStatus.OVERDUE -> OverdueContent(data)
        }
    }
}

/**
 * 休息日内容 - 显示俏皮话
 */
@Composable
private fun RestDayContent(data: WidgetData) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 休息图标
        Text(
            text = "😴",
            style = TextStyle(fontSize = 32.sp)
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        // 状态
        Text(
            text = data.getStatusText(),
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurface
            )
        )

        Spacer(modifier = GlanceModifier.height(12.dp))

        // 俏皮话
        Text(
            text = data.funPhrase,
            style = TextStyle(
                fontSize = 12.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
    }
}

/**
 * 待完成内容 - 显示训练信息，支持翻滚
 */
@Composable
private fun PendingContent(data: WidgetData) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start
    ) {
        // 顶部状态栏
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态图标
            Text(
                text = data.getStatusEmoji(),
                style = TextStyle(fontSize = 24.sp)
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.Start
            ) {
                // 状态文字
                Text(
                    text = data.getStatusText(),
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    )
                )

                // 计划名称
                if (data.planName.isNotEmpty()) {
                    Text(
                        text = data.planName,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }

            // 翻滚按钮
            if (data.canScrollExercises()) {
                Row(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 上翻
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_up),
                        contentDescription = "上翻",
                        modifier = GlanceModifier
                            .size(20.dp)
                            .clickable(
                                actionRunCallback<ScrollUpAction>()
                            )
                    )

                    Spacer(modifier = GlanceModifier.width(4.dp))

                    // 下翻
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_down),
                        contentDescription = "下翻",
                        modifier = GlanceModifier
                            .size(20.dp)
                            .clickable(
                                actionRunCallback<ScrollDownAction>()
                            )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // 当前动作
        if (data.getCurrentExercise().isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏋️",
                    style = TextStyle(fontSize = 16.sp)
                )

                Spacer(modifier = GlanceModifier.width(4.dp))

                Text(
                    text = data.getCurrentExercise(),
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurface
                    )
                )

                if (data.canScrollExercises()) {
                    Spacer(modifier = GlanceModifier.width(4.dp))

                    Text(
                        text = data.getExerciseIndicator(),
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // 俏皮话
        if (data.funPhrase.isNotEmpty()) {
            Text(
                text = data.funPhrase,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * 已完成内容
 */
@Composable
private fun CompletedContent(data: WidgetData) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start
    ) {
        // 顶部状态栏
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 完成图标
            Text(
                text = "✅",
                style = TextStyle(fontSize = 24.sp)
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.Start
            ) {
                // 状态文字
                Text(
                    text = data.getStatusText(),
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    )
                )

                // 计划名称
                if (data.planName.isNotEmpty()) {
                    Text(
                        text = data.planName,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }

            // 周进度
            if (data.weeklyProgress.second > 0) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${data.weeklyProgress.first}/${data.weeklyProgress.second}",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    // 进度条
                    val progressWidth = (data.getProgressPercentage() * 40).toInt().dp.coerceAtLeast(4.dp)
                    Spacer(
                        modifier = GlanceModifier
                            .width(progressWidth)
                            .height(4.dp)
                            .background(Color(0xFF4CAF50))
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // 完成动作预览
        if (data.exerciseNames.isNotEmpty()) {
            Text(
                text = "已完成：${data.getExercisePreview()}",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // 次次训练
        if (data.nextTrainingDay.isNotEmpty()) {
            Text(
                text = "下次训练：${data.nextTrainingDay}",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * 逾期内容 - 显示逾期提醒
 */
@Composable
private fun OverdueContent(data: WidgetData) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFFFFEBEE)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            // 顶部状态栏
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 逾期图标
                Text(
                    text = "😱",
                    style = TextStyle(fontSize = 24.sp)
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    // 状态文字
                    Text(
                        text = data.getStatusText(),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFFD32F2F))
                        )
                    )

                    // 计划名称
                    if (data.planName.isNotEmpty()) {
                        Text(
                            text = data.planName,
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // 逾期俏皮话
            if (data.funPhrase.isNotEmpty()) {
                Text(
                    text = data.funPhrase,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(Color(0xFFD32F2F))
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // 逾期动作
            if (data.getCurrentExercise().isNotEmpty()) {
                Text(
                    text = "待补：${data.getCurrentExercise()}",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }
    }
}

/**
 * 获取背景颜色
 */
private fun getBackgroundColor(status: WorkoutStatus): ColorProvider {
    return when (status) {
        WorkoutStatus.REST_DAY -> ColorProvider(Color(0xFFF5F5F5))
        WorkoutStatus.PENDING -> ColorProvider(Color(0xFFE3F2FD))
        WorkoutStatus.COMPLETED -> ColorProvider(Color(0xFFE8F5E9))
        WorkoutStatus.OVERDUE -> ColorProvider(Color(0xFFFFEBEE))
    }
}

/**
 * 上翻 Action
 */
class ScrollUpAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val provider = WidgetDataProvider(context)
        val data = provider.getWidgetData()

        if (data.canScrollExercises()) {
            var idx = data.currentExerciseIndex - 1
            if (idx < 0) idx = data.exerciseNames.size - 1

            // 更新数据中的索引
            provider.updateExerciseIndex(idx)

            // 更新小部件
            FitTrackWidget.updateAll(context)
        }
    }
}

/**
 * 下翻 Action
 */
class ScrollDownAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val provider = WidgetDataProvider(context)
        val data = provider.getWidgetData()

        if (data.canScrollExercises()) {
            var idx = data.currentExerciseIndex + 1
            if (idx >= data.exerciseNames.size) idx = 0

            // 更新数据中的索引
            provider.updateExerciseIndex(idx)

            // 更新小部件
            FitTrackWidget.updateAll(context)
        }
    }
}

/**
 * 小部件接收器
 */
class FitTrackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FitTrackWidget()
}
