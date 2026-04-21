package com.fittrack.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 小部件更新 Worker
 * 使用 WorkManager 定期更新小部件数据
 */
class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // 先初始化数据库，确保已经准备好
                val database = com.fittrack.data.db.FitTrackDatabase.getDatabase(context)

                // 刷新数据缓存
                val provider = WidgetDataProvider(context)
                provider.refresh()
                // 更新所有小部件
                FitTrackWidget().updateAll(context)
                Result.success()
            } catch (e: Exception) {
                // 数据库未初始化或其他错误，不更新小部件但不影响应用
                e.printStackTrace()
                // 返回 success 避免重试
                Result.success()
            }
        }
    }

    companion object {
        const val WORK_NAME = "widget_update_worker"
    }
}

/**
 * 调度小部件更新任务
 */
fun scheduleWidgetUpdate(context: Context) {
    androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WidgetUpdateWorker.WORK_NAME,
        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
        androidx.work.PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = java.util.concurrent.TimeUnit.MINUTES
        ).build()
    )
}

/**
 * 取消小部件更新任务
 */
fun cancelWidgetUpdate(context: Context) {
    androidx.work.WorkManager.getInstance(context).cancelUniqueWork(WidgetUpdateWorker.WORK_NAME)
}
