package com.fittrack.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.fittrack.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 训练提醒管理器
 * 使用 WorkManager 实现定时提醒功能
 */
class ReminderManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        const val CHANNEL_ID = "workout_reminder_channel"
        const val CHANNEL_NAME = "训练提醒"
        const val WORK_PREFIX = "workout_reminder_"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "训练日提醒通知"
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * 设置训练提醒
     * @param planId 计划ID
     * @param planName 计划名称
     * @param reminderDays 提醒日期（1-7 对应周一到周日，逗号分隔）
     * @param reminderTime 提醒时间（格式：HH:mm）
     */
    fun scheduleReminder(
        planId: Long,
        planName: String,
        reminderDays: String,
        reminderTime: String
    ) {
        // 取消旧的提醒
        cancelReminder(planId)

        if (reminderDays.isEmpty()) return

        val days = reminderDays.split(",").mapNotNull { it.trim().toIntOrNull() }
        val timeParts = reminderTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        // 为每个训练日设置提醒
        days.forEach { day ->
            scheduleReminderForDay(planId, planName, day, hour, minute)
        }
    }

    private fun scheduleReminderForDay(
        planId: Long,
        planName: String,
        dayOfWeek: Int, // 1-7 (Calendar.MONDAY to Calendar.SUNDAY)
        hour: Int,
        minute: Int
    ) {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 如果目标时间已过，设置为下周
        if (targetTime.before(currentTime)) {
            targetTime.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        val inputData = workDataOf(
            "planId" to planId,
            "planName" to planName
        )

        val workRequest = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(
            7, TimeUnit.DAYS // 每周重复
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "${WORK_PREFIX}${planId}_$dayOfWeek",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * 取消训练提醒
     */
    fun cancelReminder(planId: Long) {
        // 取消该计划的所有提醒（周一到周日）
        for (day in 1..7) {
            workManager.cancelUniqueWork("${WORK_PREFIX}${planId}_$day")
        }
    }
}

/**
 * 训练提醒 Worker
 */
class WorkoutReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val planName = inputData.getString("planName") ?: return Result.failure()

        showNotification(planName)

        return Result.success()
    }

    private fun showNotification(planName: String) {
        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, ReminderManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("该训练了！")
            .setContentText("今天是「$planName」的训练日，准备好开始了吗？")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
