package com.fittrack.data.api

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

/**
 * Qwen Chat Completion 响应
 */
data class QwenChatResponse(
    val id: String,
    @SerializedName("object")
    val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<QwenChoice>,
    val usage: QwenUsage
)

/**
 * 响应选项
 */
data class QwenChoice(
    val index: Int,
    val message: QwenResponseMessage,
    @SerializedName("finish_reason")
    val finishReason: String
)

/**
 * 响应消息（兼容文本和 VL 模型）
 * content 可能是 String（文本模型）或 List<ContentPart>（VL 模型）
 */
data class QwenResponseMessage(
    val role: String,
    private val _content: Any? = null // 可能是 String 或 List<ContentPartMap>
) {
    /**
     * 获取文本内容（兼容两种格式）
     */
    fun getTextContent(): String {
        return when (_content) {
            is String -> _content
            is List<*> -> {
                // VL 模型返回的 content 是 List<Map>
                try {
                    (_content as? List<Map<String, Any?>>)?.firstOrNull {
                        it["type"] == "text"
                    }?.get("text") as? String ?: ""
                } catch (e: Exception) {
                    ""
                }
            }
            else -> ""
        }
    }
}

/**
 * QwenResponseMessage 的自定义反序列化器
 */
class QwenResponseMessageDeserializer : JsonDeserializer<QwenResponseMessage> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): QwenResponseMessage {
        val jsonObj = json.asJsonObject
        val role = jsonObj.get("role")?.asString ?: ""

        val contentElement = jsonObj.get("content")
        val content: Any? = when {
            contentElement == null || contentElement.isJsonNull -> null
            contentElement.isJsonPrimitive -> contentElement.asString
            contentElement.isJsonArray -> {
                // VL 模型返回的是数组
                context.deserialize<List<Map<String, Any?>>>(
                    contentElement,
                    List::class.java
                )
            }
            else -> contentElement.toString()
        }

        return QwenResponseMessage(role, content)
    }
}

/**
 * Token 使用统计
 */
data class QwenUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

/**
 * 错误响应
 */
data class QwenErrorResponse(
    val error: QwenError
)

/**
 * 错误详情
 */
data class QwenError(
    val message: String,
    val type: String,
    val param: String?,
    val code: String
)

/**
 * 体态分析解析后的结构化结果
 */
data class ParsedBodyAnalysis(
    val postureScore: Int? = null,
    val bodyFatPercentage: Double? = null,
    val muscleBalance: String? = null,
    val issues: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val postureIssues: List<String> = emptyList(),
    val bodyType: String = "",
    val estimatedBodyFat: Double = 0.0,
    val muscleMass: String = "",
    val recommendations: List<String> = emptyList(),
    val rawResponse: String = "",
    val rawJson: String = ""
)

/**
 * 训练计划解析后的结构化结果
 */
data class ParsedWorkoutPlan(
    val name: String,
    val description: String,
    val goal: String,
    val cycleWeeks: Int,
    val weeklySchedule: List<DailyWorkout>,
    val rawResponse: String
)

/**
 * 每日训练
 */
data class DailyWorkout(
    val dayOfWeek: Int, // 1-7
    val exercises: List<PlannedExercise>,
    val notes: String = ""
)

/**
 * 计划中的动作
 */
data class PlannedExercise(
    val name: String,
    val category: String,
    val sets: Int,
    val reps: String, // 可以是 "10-12" 或 "10,10,8"
    val weight: String = "", // 可以是 "自重" 或 "60kg"
    val restSeconds: Int = 60,
    val notes: String = ""
)

/**
 * 计划调整建议
 */
data class PlanAdjustment(
    val needsAdjustment: Boolean,
    val adjustments: List<AdjustmentItem>,
    val reason: String,
    val rawResponse: String
)

/**
 * 具体调整项
 */
data class AdjustmentItem(
    val type: String, // "weight", "reps", "exercise", "rest", "frequency"
    val target: String, // 动作名称或训练日
    val currentValue: String,
    val newValue: String,
    val reason: String
)
