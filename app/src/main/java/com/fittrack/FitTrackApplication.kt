package com.fittrack

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.fittrack.reminder.ReminderManager
import com.fittrack.widget.scheduleWidgetUpdate
import java.io.File
import java.io.FileWriter
import kotlin.system.exitProcess

class FitTrackApplication : Application() {

    private val crashHandler = Thread.UncaughtExceptionHandler { thread, throwable ->
        // 捕获并记录崩溃
        val stackTrace = throwable.stackTraceToString()
        throwable.printStackTrace()

        // 写入崩溃日志文件
        try {
            val crashLog = File(filesDir, "crash_log.txt")
            FileWriter(crashLog, true).use { writer ->
                writer.appendLine("=== Crash ${System.currentTimeMillis()} ===")
                writer.appendLine(stackTrace)
                writer.appendLine()
            }
        } catch (_: Throwable) {}

        // 如果是小部件相关的崩溃，不影响应用启动
        if (throwable is RuntimeException && stackTrace.contains("widget")) {
            return@UncaughtExceptionHandler
        }

        // 在主线程弹出 Toast 显示崩溃信息（方便无 adb 排查）
        Handler(Looper.getMainLooper()).post {
            try {
                val shortMsg = stackTrace.lines().firstOrNull { it.contains("Exception") || it.contains("Error") }
                    ?.take(200) ?: throwable.javaClass.simpleName
                Toast.makeText(this, "崩溃: $shortMsg", Toast.LENGTH_LONG).show()
            } catch (_: Throwable) {}
        }

        // 交给默认处理
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
        // 使用协程避免阻塞主线程，增加异常隔离
        Thread {
            try {
                Thread.sleep(2000)
                scheduleWidgetUpdate(this)
            } catch (e: Throwable) {
                // 小部件更新失败不影响应用正常运行
            }
        }.start()
    }
}
