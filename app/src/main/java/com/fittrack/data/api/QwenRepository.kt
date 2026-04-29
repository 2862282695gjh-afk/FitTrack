package com.fittrack.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.fittrack.data.storage.SettingsManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Qwen API 结果封装
 */
sealed class QwenResult<out T> {
    data class Success<T>(val data: T) : QwenResult<T>()
    data class Error(val message: String, val code: String? = null) : QwenResult<Nothing>()
}

/**
 * Qwen API 仓库
 * 统一管理所有 Qwen API 调用
 */
class QwenRepository(private val settingsManager: SettingsManager) {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(QwenResponseMessage::class.java, QwenResponseMessageDeserializer())
        .create()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    // 缓存的 baseUrl，用于检测配置变更
    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var _apiService: QwenApiService? = null

    // 动态获取 apiService，如果 baseUrl 变化则重新创建
    private val apiService: QwenApiService
        get() = synchronized(this) {
            val baseUrl = settingsManager.apiBaseUrl.ifEmpty { QwenApiService.BASE_URL }
            val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            if (_apiService == null || cachedBaseUrl != normalizedBaseUrl) {
                cachedBaseUrl = normalizedBaseUrl
                _apiService = Retrofit.Builder()
                    .baseUrl(normalizedBaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
                    .create(QwenApiService::class.java)
            }
            _apiService!!
        }

    private val apiKey: String?
        get() = settingsManager.apiKey

    private val authHeader: String?
        get() = apiKey?.let { "Bearer $it" }

    /**
     * 检查 API Key 是否已配置
     */
    val hasApiKey: Boolean
        get() = !apiKey.isNullOrBlank()

    /**
     * 发送文本对话请求
     */
    suspend fun chat(
        messages: List<QwenMessage>,
        model: String = QwenApiService.MODEL_QWEN_PLUS,
        temperature: Double = 0.7
    ): QwenResult<String> = withContext(Dispatchers.IO) {
        val auth = authHeader
        if (auth.isNullOrBlank()) {
            return@withContext QwenResult.Error("API Key 未配置")
        }

        val request = QwenChatRequest(
            model = model,
            messages = messages,
            temperature = temperature
        )

        try {
            val response = apiService.chatCompletion(auth, request)
            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices.firstOrNull()?.message?.getTextContent()
                if (content != null && content.isNotEmpty()) {
                    QwenResult.Success(content)
                } else {
                    QwenResult.Error("响应内容为空")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                QwenResult.Error("请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            QwenResult.Error("网络错误: ${e.message}")
        }
    }

    /**
     * 发送流式文本对话请求
     * 返回 Flow，每次 emit 一个文本片段
     */
    fun chatStream(
        messages: List<QwenMessage>,
        model: String = QwenApiService.MODEL_QWEN_PLUS,
        temperature: Double = 0.7
    ): Flow<String> = flow {
        val auth = authHeader
        if (auth.isNullOrBlank()) {
            emit("[ERROR]API Key 未配置")
            return@flow
        }

        val request = QwenChatRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            stream = true  // 启用流式
        )

        try {
            val response = apiService.chatCompletionStream(auth, request)
            if (response.isSuccessful && response.body() != null) {
                val reader = BufferedReader(InputStreamReader(response.body()!!.byteStream()))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue

                    // SSE 格式：data: {...}
                    if (currentLine.startsWith("data:")) {
                        val jsonStr = currentLine.removePrefix("data:").trim()

                        // 检查是否是结束标记
                        if (jsonStr == "[DONE]") {
                            break
                        }

                        try {
                            val json = gson.fromJson<JsonObject>(jsonStr, object : TypeToken<JsonObject>() {}.type)
                            val delta = json.getAsJsonArray("choices")
                                ?.firstOrNull()?.asJsonObject
                                ?.getAsJsonObject("delta")
                            val content = delta?.getAsJsonPrimitive("content")?.asString

                            if (!content.isNullOrBlank()) {
                                emit(content)
                            }
                        } catch (e: Exception) {
                            // 忽略解析错误，继续处理下一行
                            Log.d(TAG, "解析流式响应失败: ${e.message}")
                        }
                    }
                }

                reader.close()
            } else {
                val errorBody = response.errorBody()?.string()
                emit("[ERROR]请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "流式请求错误: ${e.message}", e)
            emit("[ERROR]网络错误: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 发送带图片的对话请求（使用视觉模型）
     * @param userPrompt 用户输入的提示
     * @param imageBase64 图片的 Base64 编码（不含前缀）
     * @param systemPrompt 系统提示
     * @param model 视觉模型，默认 qwen-vl-plus
     */
    suspend fun chatWithImage(
        userPrompt: String,
        imageBase64: String,
        systemPrompt: String,
        model: String = QwenApiService.MODEL_QWEN_VL_PLUS
    ): QwenResult<String> = withContext(Dispatchers.IO) {
        val auth = authHeader
        if (auth.isNullOrBlank()) {
            return@withContext QwenResult.Error("API Key 未配置")
        }

        // 构建视觉请求
        val request = QwenVLRequest(
            model = model,
            messages = listOf(
                QwenVLMessage(
                    role = "system",
                    content = listOf(QwenContentPart(type = "text", text = systemPrompt))
                ),
                QwenVLMessage(
                    role = "user",
                    content = listOf(
                        QwenContentPart(
                            type = "image_url",
                            imageUrl = QwenImageUrl(url = "data:image/jpeg;base64,$imageBase64")
                        ),
                        QwenContentPart(type = "text", text = userPrompt)
                    )
                )
            ),
            temperature = 0.7,
            maxTokens = 2048
        )

        try {
            val response = apiService.vlCompletion(auth, request)
            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices.firstOrNull()?.message?.getTextContent()
                if (content != null && content.isNotEmpty()) {
                    QwenResult.Success(content)
                } else {
                    QwenResult.Error("响应内容为空")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                QwenResult.Error("请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            QwenResult.Error("网络错误: ${e.message}")
        }
    }

    /**
     * 发送带多张图片的对话请求（用于视频帧分析）
     * @param userPrompt 用户输入的提示
     * @param imageBase64List 图片的 Base64 编码列表（不含前缀）
     * @param systemPrompt 系统提示
     * @param model 视觉模型，默认 qwen-vl-plus
     */
    suspend fun chatWithImages(
        userPrompt: String,
        imageBase64List: List<String>,
        systemPrompt: String,
        model: String = QwenApiService.MODEL_QWEN_VL_PLUS
    ): QwenResult<String> = withContext(Dispatchers.IO) {
        val auth = authHeader
        if (auth.isNullOrBlank()) {
            return@withContext QwenResult.Error("API Key 未配置")
        }

        // 构建图片内容部分
        val imageParts = imageBase64List.map { base64 ->
            QwenContentPart(
                type = "image_url",
                imageUrl = QwenImageUrl(url = "data:image/jpeg;base64,$base64")
            )
        }

        // 构建文本部分
        val textPart = QwenContentPart(type = "text", text = userPrompt)

        // 组合所有内容
        val userContent = imageParts + textPart

        val request = QwenVLRequest(
            model = model,
            messages = listOf(
                QwenVLMessage(
                    role = "system",
                    content = listOf(QwenContentPart(type = "text", text = systemPrompt))
                ),
                QwenVLMessage(
                    role = "user",
                    content = userContent
                )
            ),
            temperature = 0.7,
            maxTokens = 4096  // 多帧可能需要更多 token
        )

        try {
            val response = apiService.vlCompletion(auth, request)
            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices.firstOrNull()?.message?.getTextContent()
                if (content != null && content.isNotEmpty()) {
                    QwenResult.Success(content)
                } else {
                    QwenResult.Error("响应内容为空")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                QwenResult.Error("请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            QwenResult.Error("网络错误: ${e.message}")
        }
    }

    /**
     * 分析体态（通过多张图像）
     * @param images 图片列表，每个元素包含 type（front/side/back）和 File
     * @param userStats 用户身体数据
     */
    suspend fun analyzeBodyImages(
        images: List<Pair<String, File>>,
        userStats: UserBodyStats
    ): QwenResult<ParsedBodyAnalysis> = withContext(Dispatchers.IO) {
        val auth = authHeader
        if (auth.isNullOrBlank()) {
            return@withContext QwenResult.Error("API Key 未配置")
        }

        try {
            // 将所有图片转换为 Base64
            val base64Images = images.mapNotNull { (type, file) ->
                val base64 = encodeImageToBase64(file)
                if (base64 != null) type to base64 else null
            }

            if (base64Images.isEmpty()) {
                return@withContext QwenResult.Error("所有图片读取失败")
            }

            val request = QwenRequestBuilder.buildBodyAnalysisRequestMulti(base64Images, userStats)
            val response = apiService.vlCompletion(auth, request)

            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices.firstOrNull()?.message?.getTextContent()
                if (content != null && content.isNotEmpty()) {
                    QwenResult.Success(parseBodyAnalysisResponse(content))
                } else {
                    QwenResult.Error("响应内容为空")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                QwenResult.Error("请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            QwenResult.Error("分析失败: ${e.message}")
        }
    }

    /**
     * 分析体态（通过图像）
     * @param imageFile 图像文件
     * @param userStats 用户身体数据
     */
    suspend fun analyzeBodyImage(
        imageFile: File,
        userStats: UserBodyStats
    ): QwenResult<ParsedBodyAnalysis> = withContext(Dispatchers.IO) {
        val auth = authHeader
        if (auth.isNullOrBlank()) {
            return@withContext QwenResult.Error("API Key 未配置")
        }

        try {
            // 将图片转换为 Base64
            val base64 = encodeImageToBase64(imageFile)
            if (base64 == null) {
                return@withContext QwenResult.Error("图片读取失败")
            }

            val request = QwenRequestBuilder.buildBodyAnalysisRequest(base64, userStats)
            val response = apiService.vlCompletion(auth, request)

            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices.firstOrNull()?.message?.getTextContent()
                if (content != null && content.isNotEmpty()) {
                    QwenResult.Success(parseBodyAnalysisResponse(content))
                } else {
                    QwenResult.Error("响应内容为空")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                QwenResult.Error("请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            QwenResult.Error("分析失败: ${e.message}")
        }
    }

    /**
     * 分析体态（通过 Base64 图像数据）
     */
    suspend fun analyzeBodyBase64(
        imageBase64: String,
        userStats: UserBodyStats
    ): QwenResult<ParsedBodyAnalysis> = withContext(Dispatchers.IO) {
        val auth = authHeader
        if (auth.isNullOrBlank()) {
            return@withContext QwenResult.Error("API Key 未配置")
        }

        try {
            val request = QwenRequestBuilder.buildBodyAnalysisRequest(imageBase64, userStats)
            val response = apiService.vlCompletion(auth, request)

            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices.firstOrNull()?.message?.getTextContent()
                if (content != null && content.isNotEmpty()) {
                    QwenResult.Success(parseBodyAnalysisResponse(content))
                } else {
                    QwenResult.Error("响应内容为空")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                QwenResult.Error("请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            QwenResult.Error("分析失败: ${e.message}")
        }
    }

    /**
     * 生成健身计划
     */
    suspend fun generateWorkoutPlan(
        bodyAnalysis: BodyAnalysisResult,
        userStats: UserBodyStats
    ): QwenResult<ParsedWorkoutPlan> = withContext(Dispatchers.IO) {
        val auth = authHeader
        if (auth.isNullOrBlank()) {
            return@withContext QwenResult.Error("API Key 未配置")
        }

        try {
            val request = QwenRequestBuilder.buildPlanGenerationRequest(bodyAnalysis, userStats)
            val response = apiService.chatCompletion(auth, request)

            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices.firstOrNull()?.message?.getTextContent()
                if (content != null && content.isNotEmpty()) {
                    QwenResult.Success(parseWorkoutPlanResponse(content))
                } else {
                    QwenResult.Error("响应内容为空")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                QwenResult.Error("请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            QwenResult.Error("生成计划失败: ${e.message}")
        }
    }

    /**
     * 分析并建议计划调整
     */
    suspend fun analyzePlanAdjustment(
        currentPlan: String,
        recentProgress: String,
        issues: String = ""
    ): QwenResult<PlanAdjustment> = withContext(Dispatchers.IO) {
        val auth = authHeader
        if (auth.isNullOrBlank()) {
            return@withContext QwenResult.Error("API Key 未配置")
        }

        try {
            val request = QwenRequestBuilder.buildPlanAdjustmentRequest(
                currentPlan, recentProgress, issues
            )
            val response = apiService.chatCompletion(auth, request)

            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices.firstOrNull()?.message?.getTextContent()
                if (content != null && content.isNotEmpty()) {
                    QwenResult.Success(parsePlanAdjustmentResponse(content))
                } else {
                    QwenResult.Error("响应内容为空")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                QwenResult.Error("请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            QwenResult.Error("分析失败: ${e.message}")
        }
    }

    /**
     * 生成减载训练计划
     */
    suspend fun generateDeloadPlan(
        normalPlan: String,
        userStats: UserBodyStats
    ): QwenResult<ParsedWorkoutPlan> = withContext(Dispatchers.IO) {
        val auth = authHeader
        if (auth.isNullOrBlank()) {
            return@withContext QwenResult.Error("API Key 未配置")
        }

        try {
            val request = QwenRequestBuilder.buildDeloadPlanRequest(normalPlan, userStats)
            val response = apiService.chatCompletion(auth, request)

            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices.firstOrNull()?.message?.getTextContent()
                if (content != null && content.isNotEmpty()) {
                    QwenResult.Success(parseWorkoutPlanResponse(content))
                } else {
                    QwenResult.Error("响应内容为空")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                QwenResult.Error("请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            QwenResult.Error("生成减载计划失败: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "QwenRepository"

        /** 图片最大边长（px），超过此尺寸会等比缩小 */
        private const val MAX_IMAGE_DIMENSION = 1024

        @Volatile
        private var INSTANCE: QwenRepository? = null

        fun getInstance(settingsManager: SettingsManager): QwenRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QwenRepository(settingsManager).also { INSTANCE = it }
            }
        }

        fun resetInstance() {
            synchronized(this) { INSTANCE = null }
        }
    }

    /**
     * 将图片文件编码为 Base64
     * 大图会先采样缩小到 MAX_IMAGE_DIMENSION 以内，避免 OOM
     */
    private fun encodeImageToBase64(imageFile: File): String? {
        return try {
            // 1. 读取原始尺寸
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imageFile.absolutePath, options)

            // 2. 计算采样率
            val (width, height) = options.outWidth to options.outHeight
            var inSampleSize = 1
            while (width / inSampleSize > MAX_IMAGE_DIMENSION ||
                   height / inSampleSize > MAX_IMAGE_DIMENSION) {
                inSampleSize *= 2
            }

            // 3. 采样解码
            val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val sampledBitmap = BitmapFactory.decodeFile(imageFile.absolutePath, decodeOptions)
                ?: return null

            // 4. 精确缩放到目标尺寸以内
            val bitmap = if (sampledBitmap.width > MAX_IMAGE_DIMENSION ||
                              sampledBitmap.height > MAX_IMAGE_DIMENSION) {
                val scale = MAX_IMAGE_DIMENSION.toFloat() / maxOf(sampledBitmap.width, sampledBitmap.height)
                Bitmap.createScaledBitmap(
                    sampledBitmap,
                    (sampledBitmap.width * scale).toInt(),
                    (sampledBitmap.height * scale).toInt(),
                    true
                ).also { if (it != sampledBitmap) sampledBitmap.recycle() }
            } else {
                sampledBitmap
            }

            val outputStream = ByteArrayOutputStream()

            // 根据文件类型选择压缩格式
            val format = when (imageFile.extension.lowercase()) {
                "png" -> Bitmap.CompressFormat.PNG
                else -> Bitmap.CompressFormat.JPEG
            }

            bitmap.compress(format, 85, outputStream)
            bitmap.recycle()
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "图片编码失败: ${e.message}")
            null
        }
    }

    /**
     * 解析体态分析响应
     */
    private fun parseBodyAnalysisResponse(content: String): ParsedBodyAnalysis {
        return try {
            // 尝试从响应中提取 JSON
            val jsonStr = extractJson(content)
            val json = gson.fromJson<JsonObject>(jsonStr, object : TypeToken<JsonObject>() {}.type)

            ParsedBodyAnalysis(
                postureScore = json.getAsJsonPrimitive("postureScore")?.asInt,
                bodyFatPercentage = json.getAsJsonPrimitive("bodyFatPercentage")?.asDouble,
                muscleBalance = json.getAsJsonPrimitive("muscleBalance")?.asString,
                issues = json.getAsJsonArray("issues")?.map { it.asString } ?: emptyList(),
                suggestions = json.getAsJsonArray("suggestions")?.map { it.asString } ?: emptyList(),
                postureIssues = json.getAsJsonArray("postureIssues")?.map { it.asString } ?: emptyList(),
                bodyType = json.getAsJsonPrimitive("bodyType")?.asString ?: "",
                estimatedBodyFat = json.getAsJsonPrimitive("estimatedBodyFat")?.asDouble ?: 0.0,
                muscleMass = json.getAsJsonPrimitive("muscleMass")?.asString ?: "",
                recommendations = json.getAsJsonArray("recommendations")?.map { it.asString } ?: emptyList(),
                rawResponse = content,
                rawJson = jsonStr
            )
        } catch (e: Exception) {
            // 如果 JSON 解析失败，返回原始响应
            ParsedBodyAnalysis(
                rawResponse = content,
                rawJson = content
            )
        }
    }

    /**
     * 解析训练计划响应
     */
    private fun parseWorkoutPlanResponse(content: String): ParsedWorkoutPlan {
        Log.d(TAG, "=== 开始解析训练计划 ===")
        Log.d(TAG, "AI 原始响应: $content")

        return try {
            val jsonStr = extractJson(content)
            Log.d(TAG, "提取的 JSON: $jsonStr")

            val json = gson.fromJson<JsonObject>(jsonStr, object : TypeToken<JsonObject>() {}.type)

            val weeklySchedule = json.getAsJsonArray("weeklySchedule")?.mapIndexed { dayIndex, dayJson ->
                val dayObj = dayJson.asJsonObject
                // 获取 dayOfWeek，如果无效则使用索引+1
                val rawDayOfWeek = dayObj.getAsJsonPrimitive("dayOfWeek")?.asInt ?: (dayIndex + 1)
                // 验证 dayOfWeek 在有效范围内（1-7）
                val dayOfWeek = if (rawDayOfWeek in 1..7) rawDayOfWeek else (dayIndex + 1)

                val exercises = dayObj.getAsJsonArray("exercises")?.map { exJson ->
                    val exObj = exJson.asJsonObject
                    PlannedExercise(
                        name = exObj.getAsJsonPrimitive("name")?.asString ?: "",
                        category = exObj.getAsJsonPrimitive("category")?.asString ?: "",
                        sets = exObj.getAsJsonPrimitive("sets")?.asInt ?: 3,
                        reps = exObj.getAsJsonPrimitive("reps")?.asString ?: "10",
                        weight = exObj.getAsJsonPrimitive("weight")?.asString ?: "",
                        restSeconds = exObj.getAsJsonPrimitive("restSeconds")?.asInt ?: 60,
                        notes = exObj.getAsJsonPrimitive("notes")?.asString ?: ""
                    )
                } ?: emptyList()
                Log.d(TAG, "训练日[原始=$rawDayOfWeek, 修正后=$dayOfWeek]: 包含 ${exercises.size} 个动作")
                DailyWorkout(
                    dayOfWeek = dayOfWeek,
                    exercises = exercises,
                    notes = dayObj.getAsJsonPrimitive("notes")?.asString ?: ""
                )
            } ?: emptyList()

            // 检查是否有重复的训练日，如果有则进行修正
            val dayOfWeekSet = mutableSetOf<Int>()
            val correctedSchedule = weeklySchedule.mapIndexed { index, dailyWorkout ->
                var dayOfWeek = dailyWorkout.dayOfWeek
                // 如果这个训练日已经存在，找一个未使用的训练日
                while (dayOfWeekSet.contains(dayOfWeek)) {
                    Log.w(TAG, "检测到重复的 dayOfWeek=$dayOfWeek，尝试修正")
                    dayOfWeek = (dayOfWeek % 7) + 1
                }
                dayOfWeekSet.add(dayOfWeek)
                if (dayOfWeek != dailyWorkout.dayOfWeek) {
                    dailyWorkout.copy(dayOfWeek = dayOfWeek)
                } else {
                    dailyWorkout
                }
            }

            Log.d(TAG, "解析完成: 共 ${correctedSchedule.size} 个训练日, 总动作数: ${correctedSchedule.sumOf { it.exercises.size }}")
            correctedSchedule.forEach {
                Log.d(TAG, "  - 第${it.dayOfWeek}天: ${it.exercises.map { e -> e.name }.joinToString()}")
            }

            ParsedWorkoutPlan(
                name = json.getAsJsonPrimitive("name")?.asString ?: "训练计划",
                description = json.getAsJsonPrimitive("description")?.asString ?: "",
                goal = json.getAsJsonPrimitive("goal")?.asString ?: "",
                cycleWeeks = json.getAsJsonPrimitive("cycleWeeks")?.asInt ?: 4,
                weeklySchedule = correctedSchedule,
                rawResponse = content
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析训练计划失败: ${e.message}", e)
            ParsedWorkoutPlan(
                name = "训练计划",
                description = "",
                goal = "",
                cycleWeeks = 4,
                weeklySchedule = emptyList(),
                rawResponse = content
            )
        }
    }

    /**
     * 解析计划调整响应
     */
    private fun parsePlanAdjustmentResponse(content: String): PlanAdjustment {
        return try {
            val jsonStr = extractJson(content)
            val json = gson.fromJson<JsonObject>(jsonStr, object : TypeToken<JsonObject>() {}.type)

            val adjustments = json.getAsJsonArray("adjustments")?.map { adjJson ->
                val adjObj = adjJson.asJsonObject
                AdjustmentItem(
                    type = adjObj.getAsJsonPrimitive("type")?.asString ?: "",
                    target = adjObj.getAsJsonPrimitive("target")?.asString ?: "",
                    currentValue = adjObj.getAsJsonPrimitive("currentValue")?.asString ?: "",
                    newValue = adjObj.getAsJsonPrimitive("newValue")?.asString ?: "",
                    reason = adjObj.getAsJsonPrimitive("reason")?.asString ?: ""
                )
            } ?: emptyList()

            PlanAdjustment(
                needsAdjustment = json.getAsJsonPrimitive("needsAdjustment")?.asBoolean ?: false,
                adjustments = adjustments,
                reason = json.getAsJsonPrimitive("reason")?.asString ?: "",
                rawResponse = content
            )
        } catch (e: Exception) {
            PlanAdjustment(
                needsAdjustment = false,
                adjustments = emptyList(),
                reason = "",
                rawResponse = content
            )
        }
    }

    /**
     * 从响应中提取 JSON 字符串
     */
    private fun extractJson(content: String): String {
        // 1. 尝试从 markdown 代码块中提取
        val codeBlockRegex = """```(?:json)?\s*\n?([\s\S]*?)```""".toRegex()
        val codeBlockMatch = codeBlockRegex.findAll(content).firstOrNull {
            it.groupValues[1].trim().startsWith("{")
        }
        if (codeBlockMatch != null) {
            return codeBlockMatch.groupValues[1].trim()
        }

        // 2. 括号匹配：找到最外层完整的 JSON 对象
        var depth = 0
        var startIndex = -1
        var endIndex = -1
        var inString = false
        var escaped = false

        for (i in content.indices) {
            val c = content[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\' && inString) {
                escaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (inString) continue

            when (c) {
                '{' -> {
                    if (depth == 0) startIndex = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0) {
                        endIndex = i
                        break
                    }
                }
            }
        }

        return if (startIndex != -1 && endIndex != -1) {
            content.substring(startIndex, endIndex + 1)
        } else {
            content
        }
    }

}
