package com.fittrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.ui.navigation.FitTrackNavHost
import com.fittrack.ui.theme.FitTrackTheme
import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        var pendingCrashLog: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查是否有上次的崩溃日志
        pendingCrashLog = try {
            val crashFile = File(filesDir, "crash_log.txt")
            if (crashFile.exists()) {
                val log = crashFile.readText().trim()
                // 读取后清空，避免重复显示
                crashFile.writeText("")
                log.ifBlank { null }
            } else null
        } catch (_: Exception) { null }

        enableEdgeToEdge()
        setContent {
            FitTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FitTrackNavHost()

                    // 崩溃日志弹窗
                    val crashLog = pendingCrashLog
                    if (crashLog != null) {
                        var showDialog by remember { mutableStateOf(true) }
                        if (showDialog) {
                            AlertDialog(
                                onDismissRequest = { showDialog = false },
                                title = { Text("上次崩溃日志") },
                                text = {
                                    Text(
                                        text = crashLog.take(2000),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 16.sp,
                                        maxLines = 20
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showDialog = false }) {
                                        Text("关闭")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
