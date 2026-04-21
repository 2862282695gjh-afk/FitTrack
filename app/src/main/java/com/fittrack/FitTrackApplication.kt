package com.fittrack

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.fittrack.reminder.ReminderManager
import com.fittrack.widget.scheduleWidgetUpdate
import kotlin.system.exitProcess

class FitTrackApplication : Application() {

    private val crashHandler = Thread.UncaughtExceptionHandler { thread, throwable ->
        // 捕获并记录崩溃
        throwable.printStackTrace()

        // 如果是小部件相关的崩溃，不影响应用启动
        if (throwable is RuntimeException && throwable.stackTraceToString().contains("widget")) {
            // 静默处理，不影响应用正常启动
            return@UncaughtExceptionHandler
        }

        // 其他崩溃交给默认处理
        defaultHandler?.uncaughtException(thread, throwable) ?: run {
            exitProcess(1)
        }
    }

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()

        // 设置全局异常处理器
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)

        // 创建通知渠道
        try {
            ReminderManager.createNotificationChannel(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 延迟调度小部件更新任务（等待数据库初始化完成）
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                scheduleWidgetUpdate(this)
            } catch (e: Exception) {
                e.printStackTrace()
                // 小部件更新失败不影响应用正常运行
            }
        }, 2000)
    }
}
