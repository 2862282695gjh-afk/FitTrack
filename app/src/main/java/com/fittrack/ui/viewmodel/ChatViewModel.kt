package com.fittrack.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.ai.tools.MultimodalAnalysisTool
import com.fittrack.data.api.QwenMessage
import com.fittrack.data.api.QwenRepository
import com.fittrack.data.api.QwenResult
import com.fittrack.data.db.ChatDao
import com.fittrack.data.entity.Exercise
import com.fittrack.data.entity.UserProfile
import com.fittrack.data.entity.WorkoutPlan
import com.fittrack.data.entity.WorkoutRecord
import com.fittrack.data.repository.FitTrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 用户意图枚举
 */
enum class UserIntent {
    MEDIA_ANALYSIS,      // 媒体分析（图片/视频）
    FITNESS_CONSULT,     // 健身咨询
    DATA_QUERY,          // 数据查询
    CHITCHAT             // 闲聊
}

/**
 * 聊天消息数据类（UI 层）
 */
data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val hasImage: Boolean = false,
    val imageUri: Uri? = null
)

/**
 * AI 教练对话 ViewModel
 */
class ChatViewModel(
    private val qwenRepository: QwenRepository,
    private val fitTrackRepository: FitTrackRepository,
    private val chatDao: ChatDao
) : ViewModel() {

    // 聊天消息列表
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // 输入框文本
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // 是否正在加载
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 用户档案（用于上下文）
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // 用户训练计划
    private val _activePlans = MutableStateFlow<List<WorkoutPlan>>(emptyList())

    // 用户训练记录（最近）
    private val _recentRecords = MutableStateFlow<List<WorkoutRecord>>(emptyList())

    // 训练动作详情
    private val _planExercises = MutableStateFlow<Map<Long, List<Exercise>>>(emptyMap())

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 选中的媒体
    private val _selectedMediaUri = MutableStateFlow<Uri?>(null)
    val selectedMediaUri: StateFlow<Uri?> = _selectedMediaUri.asStateFlow()

    // 选中的媒体类型
    private val _selectedMediaType = MutableStateFlow<String?>(null)
    val selectedMediaType: StateFlow<String?> = _selectedMediaType.asStateFlow()

    // 对话历史（用于 API 上下文）
    private val conversationHistory = mutableListOf<QwenMessage>()

    // 会话列表
    private val _sessions = MutableStateFlow<List<com.fittrack.data.entity.ChatSession>>(emptyList())
    val sessions: StateFlow<List<com.fittrack.data.entity.ChatSession>> = _sessions.asStateFlow()

    // 当前会话 ID
    private var currentSessionId: Long = 0

    // 是否显示会话列表
    private val _showSessionList = MutableStateFlow(false)
    val showSessionList: StateFlow<Boolean> = _showSessionList.asStateFlow()

    init {
        loadSessions()
        loadUserData()
    }

    /**
     * 加载会话列表
     */
    private fun loadSessions() {
        viewModelScope.launch {
            chatDao.getAllSessions().collect { sessionList ->
                _sessions.value = sessionList
                // 如果有会话，选择最新的
                if (sessionList.isNotEmpty() && currentSessionId == 0L) {
                    selectSession(sessionList.first().id)
                } else if (sessionList.isEmpty()) {
                    // 没有会话，创建新会话
                    createNewSession()
                }
            }
        }
    }

    /**
     * 切换会话列表显示
     */
    fun toggleSessionList() {
        _showSessionList.value = !_showSessionList.value
    }

    /**
     * 隐藏会话列表
     */
    fun hideSessionList() {
        _showSessionList.value = false
    }

    /**
     * 创建新会话
     */
    fun createNewSession() {
        viewModelScope.launch {
            val session = com.fittrack.data.entity.ChatSession(
                title = "新对话",
                summary = "",
                lastMessageAt = System.currentTimeMillis()
            )
            val id = chatDao.insertSession(session)
            currentSessionId = id
            _messages.value = emptyList()
            conversationHistory.clear()
            addWelcomeMessage()
            _showSessionList.value = false
        }
    }

    /**
     * 选择会话
     */
    fun selectSession(sessionId: Long) {
        if (currentSessionId == sessionId) {
            _showSessionList.value = false
            return
        }
        currentSessionId = sessionId
        loadSessionMessages(sessionId)
        _showSessionList.value = false
    }

    /**
     * 加载会话消息
     */
    private fun loadSessionMessages(sessionId: Long) {
        viewModelScope.launch {
            chatDao.getMessagesBySession(sessionId).collect { messages ->
                if (messages.isEmpty()) {
                    addWelcomeMessage()
                } else {
                    _messages.value = messages.map { entity ->
                        ChatMessage(
                            id = entity.id,
                            content = entity.content,
                            isFromUser = entity.role == "user",
                            timestamp = entity.createdAt
                        )
                    }
                    // 同时加载到对话历史（用于 API 上下文）
                    conversationHistory.clear()
                    messages.forEach { entity ->
                        conversationHistory.add(
                            QwenMessage(role = entity.role, content = entity.content)
                        )
                    }
                }
            }
        }
    }

    /**
     * 删除会话
     */
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            val session = _sessions.value.find { it.id == sessionId }
            if (session != null) {
                chatDao.deleteSession(session)
                // 删除会话的消息
                chatDao.getMessagesBySession(sessionId).collect { messages ->
                    messages.forEach { chatDao.deleteMessage(it) }
                }
            }
            // 如果删除的是当前会话，切换到其他会话或创建新会话
            if (currentSessionId == sessionId) {
                val remainingSessions = _sessions.value.filter { it.id != sessionId }
                if (remainingSessions.isNotEmpty()) {
                    selectSession(remainingSessions.first().id)
                } else {
                    createNewSession()
                }
            }
        }
    }

    /**
     * 加载聊天历史记录（兼容旧版本）
     */
    private fun loadChatHistory() {
        // 不再使用，由 loadSessions 和 selectSession 处理
    }

    /**
     * 保存消息到数据库
     */
    private suspend fun saveMessageToDb(role: String, content: String) {
        val entity = com.fittrack.data.entity.ChatMessage(
            sessionId = currentSessionId,
            role = role,
            content = content,
            messageType = "text",
            createdAt = System.currentTimeMillis()
        )
        chatDao.insertMessage(entity)

        // 更新会话的最后消息时间
        chatDao.updateSessionLastMessage(currentSessionId, System.currentTimeMillis())

        // 如果是第一条用户消息，更新会话标题
        val sessionCount = chatDao.getMessageCount()
        if (sessionCount <= 2) {
            val title = content.take(20) + if (content.length > 20) "..." else ""
            chatDao.updateSessionTitle(currentSessionId, title)
        }
    }

    /**
     * 加载用户数据（档案、计划、记录）
     */
    private fun loadUserData() {
        loadUserProfile()
        loadActivePlans()
        loadRecentRecords()
    }

    /**
     * 加载用户档案
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            fitTrackRepository.getProfile()?.collect { profile ->
                _userProfile.value = profile
            }
        }
    }

    /**
     * 加载活跃的训练计划
     */
    private fun loadActivePlans() {
        viewModelScope.launch {
            fitTrackRepository.getActivePlans().collect { plans ->
                _activePlans.value = plans
                // 加载每个计划的动作详情（在 IO 线程中执行）
                plans.forEach { plan ->
                    try {
                        val exercises = withContext(Dispatchers.IO) {
                            fitTrackRepository.getExercisesForPlan(plan.id)
                        }
                        _planExercises.value = _planExercises.value + (plan.id to exercises)
                    } catch (e: Exception) {
                        // 忽略单个计划加载失败
                    }
                }
            }
        }
    }

    /**
     * 加载最近的训练记录
     */
    private fun loadRecentRecords() {
        viewModelScope.launch {
            // 获取最近 7 天的训练记录
            val weekStart = FitTrackRepository.getWeekStartDate()
            fitTrackRepository.getRecordsBetween(weekStart, FitTrackRepository.getTodayDate())
                .collect { records ->
                    _recentRecords.value = records
                }
        }
    }

    /**
     * 添加欢迎消息
     */
    private fun addWelcomeMessage() {
        val welcomeMessage = buildWelcomeMessage()
        _messages.value = listOf(
            ChatMessage(
                content = welcomeMessage,
                isFromUser = false
            )
        )
    }

    /**
     * 构建欢迎消息
     */
    private fun buildWelcomeMessage(): String {
        val profile = _userProfile.value
        return if (profile != null && profile.name.isNotBlank()) {
            "你好，${profile.name}！我是你的 AI 健身教练。根据你的${getGoalDescription(profile.fitnessGoal)}目标，我会为你提供专业的健身建议。\n\n" +
                    "你可以问我：\n" +
                    "- 训练计划和动作建议\n" +
                    "- 饮食和营养问题\n" +
                    "- 健身姿势纠正\n" +
                    "- 训练进度分析\n" +
                    "- 任何健身相关的问题"
        } else {
            "你好！我是你的 AI 健身教练。我可以帮助你：\n\n" +
                    "- 制定个性化训练计划\n" +
                    "- 解答健身相关问题\n" +
                    "- 提供饮食和营养建议\n" +
                    "- 分析训练进度\n\n" +
                    "建议先完善个人档案，这样我能给你更精准的建议！有什么我可以帮助你的吗？"
        }
    }

    /**
     * 获取目标描述
     */
    private fun getGoalDescription(goal: String): String {
        return when (goal) {
            "lose_fat" -> "减脂"
            "build_muscle" -> "增肌"
            "shape" -> "塑形"
            "maintain" -> "保持健康"
            "improve_endurance" -> "提升耐力"
            else -> "健身"
        }
    }

    /**
     * 更新输入文本
     */
    fun updateInputText(text: String) {
        _inputText.value = text
    }

    /**
     * 发送消息
     */
    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank() || _isLoading.value) return

        // 添加用户消息
        val userMessage = ChatMessage(
            content = text,
            isFromUser = true
        )
        _messages.value = _messages.value + userMessage
        _inputText.value = ""

        // 添加到对话历史
        conversationHistory.add(
            QwenMessage(role = "user", content = text)
        )

        // 保存用户消息到数据库
        viewModelScope.launch {
            saveMessageToDb("user", text)
        }

        // 发送 AI 请求
        sendAiRequest()
    }

    /**
     * 设置选中的媒体
     */
    fun setMedia(uri: Uri?, type: String?) {
        _selectedMediaUri.value = uri
        _selectedMediaType.value = type
    }

    /**
     * 发送带媒体的消息
     */
    fun sendMediaMessage(context: Context) {
        val uri = _selectedMediaUri.value ?: return
        val mediaType = _selectedMediaType.value ?: "image"
        val text = _inputText.value.trim()

        if (_isLoading.value) return

        // 添加用户消息
        val userMessage = ChatMessage(
            content = if (text.isNotBlank()) text else "请分析这张${if (mediaType == "video") "视频" else "图片"}",
            isFromUser = true,
            hasImage = true,
            imageUri = uri
        )
        _messages.value = _messages.value + userMessage

        // 清空输入
        _inputText.value = ""
        _selectedMediaUri.value = null
        _selectedMediaType.value = null

        // 发送带图片的 AI 请求
        sendMediaAiRequest(context, uri, mediaType, text)
    }

    /**
     * 发送带媒体的 AI 请求
     * 使用 MultimodalAnalysisTool 进行统一的多模态分析
     */
    private fun sendMediaAiRequest(context: Context, uri: Uri, mediaType: String, userText: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // 添加加载中的消息
            val loadingMessage = ChatMessage(
                content = if (mediaType == "video") "正在提取视频帧并分析..." else "正在分析图片...",
                isFromUser = false,
                isLoading = true
            )
            _messages.value = _messages.value + loadingMessage

            // 创建多模态分析工具
            val tool = MultimodalAnalysisTool(qwenRepository)
            val systemPrompt = buildSystemPrompt()

            val result = if (mediaType == "video") {
                // 视频：提取多帧并分析
                val frames = withContext(Dispatchers.IO) {
                    extractVideoFrames(context, uri, maxFrames = 10)
                }

                if (frames.isEmpty()) {
                    Result.failure(Exception("无法从视频中提取帧"))
                } else {
                    Log.d("ChatViewModel", "视频提取了 ${frames.size} 帧，开始分析...")
                    tool.analyzeVideo(frames, userText, systemPrompt)
                }
            } else {
                // 图片：单图分析
                val base64 = withContext(Dispatchers.IO) {
                    imageUriToBase64(context, uri)
                }

                if (base64.isNullOrBlank()) {
                    Result.failure(Exception("无法读取图片"))
                } else {
                    tool.analyzeImage(base64, userText, systemPrompt)
                }
            }

            // 移除加载消息
            _messages.value = _messages.value.filter { !it.isLoading }

            when {
                result.isSuccess -> {
                    val responseContent = result.getOrNull() ?: "分析完成"

                    // 添加 AI 回复
                    val aiMessage = ChatMessage(
                        content = responseContent,
                        isFromUser = false
                    )
                    _messages.value = _messages.value + aiMessage

                    // 添加到对话历史
                    val userHistoryContent = "[发送了${if (mediaType == "video") "视频" else "图片"}] ${userText.ifBlank { "请分析" }}"
                    conversationHistory.add(
                        QwenMessage(role = "user", content = userHistoryContent)
                    )
                    conversationHistory.add(
                        QwenMessage(role = "assistant", content = responseContent)
                    )

                    // 保存到数据库
                    saveMessageToDb("user", userHistoryContent)
                    saveMessageToDb("assistant", responseContent)
                }
                result.isFailure -> {
                    val errorMsg = result.exceptionOrNull()?.message ?: "分析失败"

                    // 添加错误消息
                    val errorMessage = ChatMessage(
                        content = "抱歉，分析失败：$errorMsg。请稍后再试。",
                        isFromUser = false
                    )
                    _messages.value = _messages.value + errorMessage
                    _error.value = errorMsg
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * 将图片 Uri 转为 Base64
     */
    private fun imageUriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从视频中提取关键帧并转为 Base64（兼容旧逻辑，只返回第一帧）
     */
    private fun videoUriToBase64(context: Context, uri: Uri): String? {
        val frames = extractVideoFrames(context, uri, maxFrames = 1)
        return frames.firstOrNull()
    }

    /**
     * 从视频中每秒提取一帧，返回 Base64 列表
     * @param maxFrames 最大帧数限制，避免请求过大（默认 10 帧）
     */
    private fun extractVideoFrames(context: Context, uri: Uri, maxFrames: Int = 10): List<String> {
        val frames = mutableListOf<String>()
        val retriever = android.media.MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, uri)

            // 获取视频时长（毫秒）
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            if (durationMs <= 0) {
                // 无法获取时长，尝试提取中间一帧
                val bitmap = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    val scaledBitmap = scaleBitmap(bitmap, 512)
                    val base64 = bitmapToBase64(scaledBitmap)
                    frames.add(base64)
                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle()
                    }
                    bitmap.recycle()
                }
                return frames
            }

            // 计算需要提取的帧数（每秒一帧）
            val videoSeconds = (durationMs / 1000).toInt().coerceAtLeast(1)
            val frameCount = minOf(videoSeconds, maxFrames)

            // 每秒提取一帧
            for (second in 0 until frameCount) {
                val frameTimeUs = second * 1_000_000L // 转换为微秒
                val bitmap = retriever.getFrameAtTime(frameTimeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                if (bitmap != null) {
                    // 缩放图片以减小传输大小
                    val scaledBitmap = scaleBitmap(bitmap, 512)
                    val base64 = bitmapToBase64(scaledBitmap)
                    frames.add(base64)

                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle()
                    }
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            // 错误时返回空列表
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // 忽略释放错误
            }
        }

        return frames
    }

    /**
     * 缩放图片
     */
    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        return if (width > maxSize || height > maxSize) {
            val scale = maxSize.toFloat() / maxOf(width, height)
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    /**
     * 将 Bitmap 转为 Base64
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * 发送 AI 请求（流式）
     */
    private fun sendAiRequest() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // 添加流式消息占位（用于逐步更新内容）
            val streamingMessageId = System.currentTimeMillis()
            val streamingMessage = ChatMessage(
                id = streamingMessageId,
                content = "",
                isFromUser = false,
                isLoading = true  // 标记为加载中，显示打字效果
            )
            _messages.value = _messages.value + streamingMessage

            // 构建系统提示
            val systemPrompt = buildSystemPrompt()
            val messagesWithSystem = listOf(
                QwenMessage(role = "system", content = systemPrompt)
            ) + conversationHistory

            // 用于收集完整响应
            val fullResponse = StringBuilder()

            try {
                // 使用流式 API
                qwenRepository.chatStream(messagesWithSystem).collect { chunk ->
                    if (chunk.startsWith("[ERROR]")) {
                        // 处理错误
                        val errorMsg = chunk.removePrefix("[ERROR]")
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == streamingMessageId) {
                                msg.copy(
                                    content = "抱歉，出现了一些问题：$errorMsg。请稍后再试。",
                                    isLoading = false
                                )
                            } else msg
                        }
                        _error.value = errorMsg
                    } else {
                        // 累积内容并更新 UI
                        fullResponse.append(chunk)
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == streamingMessageId) {
                                msg.copy(
                                    content = fullResponse.toString(),
                                    isLoading = false  // 有内容后取消加载状态
                                )
                            } else msg
                        }
                    }
                }

                // 流式完成，保存到对话历史
                if (fullResponse.isNotEmpty()) {
                    conversationHistory.add(
                        QwenMessage(role = "assistant", content = fullResponse.toString())
                    )
                    // 保存 AI 回复到数据库
                    saveMessageToDb("assistant", fullResponse.toString())
                }

            } catch (e: Exception) {
                // 处理异常
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == streamingMessageId) {
                        msg.copy(
                            content = "抱歉，出现了一些问题：${e.message}。请稍后再试。",
                            isLoading = false
                        )
                    } else msg
                }
                _error.value = e.message
            }

            _isLoading.value = false
        }
    }

    /**
     * 构建系统提示
     */
    private fun buildSystemPrompt(): String {
        return try {
            buildSystemPromptInternal()
        } catch (e: Exception) {
            // 如果出现任何异常，返回基础提示词
            "你是一位专业、友好、有耐心的 AI 健身教练。请用简洁、易懂的语言回答用户的问题。"
        }
    }

    /**
     * 构建系统提示（内部实现）
     */
    private fun buildSystemPromptInternal(): String {
        val profile = _userProfile.value
        val activePlans = _activePlans.value
        val recentRecords = _recentRecords.value
        val planExercises = _planExercises.value
        val sb = StringBuilder()

        sb.appendLine("你是一位专业、友好、有耐心的 AI 健身教练。")
        sb.appendLine("请用简洁、易懂的语言回答用户的问题。")
        sb.appendLine("如果用户问的问题与健身无关，请友善地引导话题。")
        sb.appendLine()
        sb.appendLine("## 输出格式要求")
        sb.appendLine("请使用 Markdown 格式组织你的回答，使其更易读：")
        sb.appendLine("- 使用 **粗体** 强调重要内容")
        sb.appendLine("- 使用 `代码` 标记专业术语或动作名称")
        sb.appendLine("- 使用列表（- 或 1. 2. 3.）展示步骤或要点")
        sb.appendLine("- 使用 ### 小标题 分隔不同部分")
        sb.appendLine("- 使用 ``` 代码块 ``` 展示训练计划或动作步骤")
        sb.appendLine("- 使用合适的分段，不要一大段文字堆在一起")
        sb.appendLine()

        // 针对不同问题的处理指南
        sb.appendLine("## 问题类型处理指南")
        sb.appendLine()
        sb.appendLine("### 如何热身")
        sb.appendLine("当用户询问热身相关问题时：")
        sb.appendLine("1. 首先查看用户的训练计划，了解今天或近期要做什么训练")
        sb.appendLine("2. 根据训练类型（如胸部训练、腿部训练、全身训练等）推荐针对性的热身动作")
        sb.appendLine("3. 热身应包括：有氧预热（5-10分钟）、动态拉伸、目标肌群激活")
        sb.appendLine("4. 考虑用户的健身经验水平，初学者需要更详细的指导")
        sb.appendLine("5. 如果用户有健康问题，提供针对性的注意事项")
        sb.appendLine("6. 根据用户最近的训练频率和疲劳程度，调整热身强度建议")
        sb.appendLine()
        sb.appendLine("### 热量估算")
        sb.appendLine("当用户询问热量相关问题时：")
        sb.appendLine("1. 先询问用户最近的饮食情况（吃了什么、大概分量）")
        sb.appendLine("2. 根据用户描述大致估算热量摄入")
        sb.appendLine("3. 结合用户的健身目标（减脂/增肌/塑形）、体重、训练情况给出饮食建议")
        sb.appendLine("4. 如果用户想减脂，建议适量热量缺口；增肌则建议适量盈余")
        sb.appendLine("5. 提供健康食物选择建议，不推荐极端节食")
        sb.appendLine()
        sb.appendLine("### 动作细节")
        sb.appendLine("当用户询问动作细节时：")
        sb.appendLine("1. 详细说明动作的标准姿势和执行步骤")
        sb.appendLine("2. 指出常见错误和如何避免")
        sb.appendLine("3. 说明该动作主要锻炼的肌肉群")
        sb.appendLine("4. 根据用户的经验水平，推荐合适的组数和次数")
        sb.appendLine("5. 如果用户有健康问题，提醒注意事项或替代动作")
        sb.appendLine()

        if (profile != null) {
            sb.appendLine("## 用户信息")
            if (profile.name.isNotBlank()) {
                sb.appendLine("- 姓名: ${profile.name}")
            }
            if (profile.age > 0) {
                sb.appendLine("- 年龄: ${profile.age}岁")
            }
            if (profile.gender.isNotBlank()) {
                sb.appendLine("- 性别: ${getGenderText(profile.gender)}")
            }
            if (profile.heightCm > 0) {
                sb.appendLine("- 身高: ${profile.heightCm}cm")
            }
            if (profile.weightKg > 0) {
                sb.appendLine("- 体重: ${profile.weightKg}kg")
            }
            if (profile.targetWeightKg > 0) {
                sb.appendLine("- 目标体重: ${profile.targetWeightKg}kg")
            }
            if (profile.fitnessGoal.isNotBlank()) {
                sb.appendLine("- 健身目标: ${getGoalDescription(profile.fitnessGoal)}")
            }
            if (profile.experienceLevel.isNotBlank()) {
                sb.appendLine("- 健身经验: ${getExperienceText(profile.experienceLevel)}")
            }
            if (profile.weeklyAvailableMinutes > 0) {
                sb.appendLine("- 每周可用时间: ${profile.weeklyAvailableMinutes}分钟")
            }
            if (profile.healthIssues.isNotBlank()) {
                sb.appendLine("- 健康问题: ${profile.healthIssues}")
            }
            sb.appendLine()
        }

        // 添加训练计划信息
        if (activePlans.isNotEmpty()) {
            sb.appendLine("## 用户的训练计划")
            activePlans.forEach { plan ->
                sb.appendLine("### ${plan.name}")
                if (plan.description.isNotBlank()) {
                    sb.appendLine("描述: ${plan.description}")
                }
                val exercises = planExercises[plan.id] ?: emptyList()
                if (exercises.isNotEmpty()) {
                    sb.appendLine("包含动作:")
                    exercises.take(10).forEach { exercise ->
                        sb.appendLine("  - ${exercise.name} (${exercise.defaultSets}组 x ${exercise.defaultReps}次)")
                    }
                    if (exercises.size > 10) {
                        sb.appendLine("  - ... 还有 ${exercises.size - 10} 个动作")
                    }
                }
                sb.appendLine()
            }
        }

        // 添加最近训练记录
        if (recentRecords.isNotEmpty()) {
            sb.appendLine("## 最近训练记录（本周）")
            recentRecords.take(5).forEach { record ->
                try {
                    val exerciseName = planExercises[record.planId]?.firstOrNull()?.name ?: "训练"
                    val duration = record.totalDuration
                    sb.appendLine("- ${record.date}: $exerciseName (时长: ${duration}分钟)")
                } catch (e: Exception) {
                    sb.appendLine("- ${record.date}: 训练")
                }
            }
            if (recentRecords.size > 5) {
                sb.appendLine("- ... 还有 ${recentRecords.size - 5} 条记录")
            }
            sb.appendLine("本周共训练 ${recentRecords.size} 次")
            sb.appendLine()
        }

        if (profile != null || activePlans.isNotEmpty()) {
            sb.appendLine("请根据用户的个人情况和训练计划提供针对性的建议。")
        }

        return sb.toString()
    }

    /**
     * 获取性别文本
     */
    private fun getGenderText(gender: String): String {
        return when (gender) {
            "male" -> "男"
            "female" -> "女"
            else -> "未设置"
        }
    }

    /**
     * 获取经验文本
     */
    private fun getExperienceText(experience: String): String {
        return when (experience) {
            "beginner" -> "初学者（<6个月）"
            "intermediate" -> "中级（6个月-2年）"
            "advanced" -> "高级（>2年）"
            else -> "未设置"
        }
    }

    /**
     * 清空对话
     */
    fun clearChat() {
        viewModelScope.launch {
            // 清空数据库
            chatDao.deleteAllMessages()
        }
        // 清空内存中的对话历史
        conversationHistory.clear()
        // 添加欢迎消息
        addWelcomeMessage()
    }

    /**
     * 重试最后一条消息
     */
    fun retryLastMessage() {
        if (conversationHistory.isEmpty()) return

        // 移除最后一条 AI 消息（如果存在）
        val lastMessage = conversationHistory.last()
        if (lastMessage.role == "assistant") {
            conversationHistory.removeLast()
        }

        // 重新发送
        sendAiRequest()
    }

    /**
     * 快速提问
     */
    fun quickQuestion(question: String) {
        if (question == "训练建议") {
            // 生成基于用户数据的训练建议提示
            val prompt = buildTrainingSuggestionPrompt()
            _inputText.value = prompt
        } else {
            _inputText.value = question
        }
        sendMessage()
    }

    /**
     * 构建训练建议提示
     */
    private fun buildTrainingSuggestionPrompt(): String {
        val recentRecords = _recentRecords.value
        val profile = _userProfile.value
        val sb = StringBuilder()

        sb.append("请根据我最近的训练情况，提供针对性的训练建议。\n\n")

        // 用户基本信息
        if (profile != null) {
            sb.append("## 我的基本信息\n")
            if (profile.name.isNotBlank()) sb.append("- 姓名: ${profile.name}\n")
            if (profile.age > 0) sb.append("- 年龄: ${profile.age}岁\n")
            if (profile.fitnessGoal.isNotBlank()) sb.append("- 健身目标: ${getGoalDescription(profile.fitnessGoal)}\n")
            sb.append("\n")
        }

        // 最近训练情况
        if (recentRecords.isNotEmpty()) {
            sb.append("## 最近训练记录（${recentRecords.size}次）\n")
            recentRecords.take(5).forEach { record ->
                sb.append("- ${record.date}: ")
                sb.append("感受${record.feeling}/5, ")
                sb.append("睡眠${record.sleepQuality}/5, ")
                sb.append("能量${record.energyLevel}/5")
                if (record.isDeload) sb.append(" [减载日]")
                sb.append("\n")
            }

            // 巻加压力指数分析
            val avgMetabolic = if (recentRecords.isNotEmpty()) {
                recentRecords.map { it.metabolicPressure }.average().toInt()
            } else 0

            val avgMental = if (recentRecords.isNotEmpty()) {
                recentRecords.map { it.mentalPressure }.average().toInt()
            } else 0

            val needsDeload = recentRecords.any { it.isDeload }

            sb.append("\n## 健康指标分析\n")
            sb.append("- 平均代谢压力: $avgMetabolic/100\n")
            sb.append("- 平均精神压力: $avgMental/100\n")
            sb.append("- 疲劳状态: ${if (needsDeload) "需要减载" else "正常"}\n")

            // 根据压力情况给出建议请求
            sb.append("\n## 请帮我分析\n")
            if (needsDeload) {
                sb.append("检测到我的疲劳状态较高，请建议：\n")
                sb.append("1. 今天是否应该进行减载训练\n")
                sb.append("2. 如果训练，应该做什么类型的运动\n")
                sb.append("3. 如何调整训练强度帮助恢复\n")
            } else if (avgMetabolic > 60 || avgMental > 60) {
                sb.append("我的压力指数偏高，请建议：\n")
                sb.append("1. 今天的训练强度应该如何调整\n")
                sb.append("2. 需要注意什么来避免过度训练\n")
                sb.append("3. 有什么恢复建议\n")
            } else {
                sb.append("请建议：\n")
                sb.append("1. 根据我最近的训练频率，今天的训练安排\n")
                sb.append("2. 有哪些可以改进的地方\n")
                sb.append("3. 下一步训练重点\n")
            }
        } else {
            sb.append("## 还没有训练记录\n")
            sb.append("请帮我制定一个起步训练计划，考虑到：\n")
            sb.append("1. 作为新手的训练强度建议\n")
            sb.append("2. 第一次训练应该做什么\n")
            sb.append("3. 如何循序渐进地增加训练量\n")
        }

        return sb.toString()
    }

    companion object {
        fun Factory(
            qwenRepository: QwenRepository,
            fitTrackRepository: FitTrackRepository,
            chatDao: ChatDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(qwenRepository, fitTrackRepository, chatDao) as T
            }
        }
    }
}
