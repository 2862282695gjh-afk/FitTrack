package com.fittrack.data.api

import com.google.gson.annotations.SerializedName

/**
 * Qwen Chat Completion 请求
 * 用于文本对话
 */
data class QwenChatRequest(
    val model: String = "qwen-plus",
    val messages: List<QwenMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int = 2048,
    val stream: Boolean = false
)

/**
 * Qwen VL (Vision Language) 请求
 * 用于图像分析
 */
data class QwenVLRequest(
    val model: String = "qwen-vl-plus",
    val messages: List<QwenVLMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int = 2048,
    val stream: Boolean = false
)

/**
 * 文本消息
 */
data class QwenMessage(
    val role: String, // system, user, assistant
    val content: String
)

/**
 * VL 消息（支持图像）
 */
data class QwenVLMessage(
    val role: String, // system, user, assistant
    val content: List<QwenContentPart>
)

/**
 * VL 内容部分（文本或图像）
 */
data class QwenContentPart(
    val type: String, // text, image_url
    val text: String? = null,
    @SerializedName("image_url")
    val imageUrl: QwenImageUrl? = null
)

/**
 * 图像 URL
 */
data class QwenImageUrl(
    val url: String // 可以是 http URL 或 base64 data URL
)

/**
 * 健身计划生成请求的便捷构建器
 */
object QwenRequestBuilder {

    /**
     * 构建体态分析请求（支持多张照片）
     * @param images 图片列表，每个元素包含 type（front/side/back）和 base64 数据
     * @param userStats 用户身体数据
     */
    fun buildBodyAnalysisRequestMulti(
        images: List<Pair<String, String>>, // (type, base64)
        userStats: UserBodyStats
    ): QwenVLRequest {
        val photoCount = images.size
        val photoTypes = images.map { it.first }

        val systemPrompt = """
            你是一位专业的健身教练和体态分析师。请根据用户的照片和身体数据分析用户的体态状况。
            用户上传了 $photoCount 张照片：${photoTypes.joinToString("、")}。

            分析内容包括：
            1. 体态评估（是否有圆肩、驼背、骨盆前倾等问题）
            2. 体型分类（内胚型、中胚型、外胚型）
            3. 体脂率估算
            4. 肌肉量评估
            5. 改进建议

            请严格按照以下 JSON 格式返回分析结果，不要添加任何额外文字：
            {
              "postureScore": 75,
              "bodyFatPercentage": 18.5,
              "muscleBalance": "上肢略弱于下肢",
              "issues": ["轻微圆肩", "骨盆轻微前倾"],
              "suggestions": ["加强背部训练", "核心稳定性练习"],
              "bodyType": "中胚型",
              "estimatedBodyFat": 18.5,
              "muscleMass": "中等",
              "recommendations": ["增加蛋白质摄入", "每周进行3-4次力量训练"]
            }

            注意：
            - postureScore 是 0-100 的整数，100 表示完美体态
            - bodyFatPercentage 和 estimatedBodyFat 是预估值（如 18.5）
            - issues 和 suggestions 必须是字符串数组
            - 所有字段都必须填写
        """.trimIndent()

        val userPrompt = """
            用户身体数据：
            - 身高: ${userStats.height}cm
            - 体重: ${userStats.weight}kg
            - 年龄: ${userStats.age}岁
            - 性别: ${userStats.gender}
            - 健身目标: ${userStats.goal}
            - 健身经验: ${userStats.experience}

            请综合分析这些照片中用户的体态状况。
        """.trimIndent()

        // 构建图片内容部分
        val imageParts = images.map { (_, base64) ->
            QwenContentPart(
                type = "image_url",
                imageUrl = QwenImageUrl(url = "data:image/jpeg;base64,$base64")
            )
        }

        return QwenVLRequest(
            model = "qwen-vl-plus",
            messages = listOf(
                QwenVLMessage(
                    role = "system",
                    content = listOf(QwenContentPart(type = "text", text = systemPrompt))
                ),
                QwenVLMessage(
                    role = "user",
                    content = imageParts + listOf(QwenContentPart(type = "text", text = userPrompt))
                )
            )
        )
    }

    /**
     * 构建体态分析请求
     * @param imageBase64 Base64 编码的图像数据（不含 data:image/xxx;base64, 前缀）
     * @param userStats 用户身体数据
     */
    fun buildBodyAnalysisRequest(
        imageBase64: String,
        userStats: UserBodyStats
    ): QwenVLRequest {
        val systemPrompt = """
            你是一位专业的健身教练和体态分析师。请根据用户的照片和身体数据分析用户的体态状况。

            分析内容包括：
            1. 体态评估（是否有圆肩、驼背、骨盆前倾等问题）
            2. 体型分类（内胚型、中胚型、外胚型）
            3. 体脂率估算
            4. 肌肉量评估
            5. 改进建议

            请严格按照以下 JSON 格式返回分析结果，不要添加任何额外文字：
            {
              "postureScore": 75,
              "bodyFatPercentage": 18.5,
              "muscleBalance": "上肢略弱于下肢",
              "issues": ["轻微圆肩", "骨盆轻微前倾"],
              "suggestions": ["加强背部训练", "核心稳定性练习"],
              "bodyType": "中胚型",
              "estimatedBodyFat": 18.5,
              "muscleMass": "中等",
              "recommendations": ["增加蛋白质摄入", "每周进行3-4次力量训练"]
            }

            注意：
            - postureScore 是 0-100 的整数，100 表示完美体态
            - bodyFatPercentage 和 estimatedBodyFat 是预估值（如 18.5）
            - issues 和 suggestions 必须是字符串数组
            - 所有字段都必须填写
        """.trimIndent()

        val userPrompt = """
            用户身体数据：
            - 身高: ${userStats.height}cm
            - 体重: ${userStats.weight}kg
            - 年龄: ${userStats.age}岁
            - 性别: ${userStats.gender}
            - 健身目标: ${userStats.goal}
            - 健身经验: ${userStats.experience}

            请分析照片中用户的体态状况。
        """.trimIndent()

        return QwenVLRequest(
            model = "qwen-vl-plus",
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
            )
        )
    }

    /**
     * 构建健身计划生成请求
     */
    fun buildPlanGenerationRequest(
        bodyAnalysis: BodyAnalysisResult,
        userStats: UserBodyStats
    ): QwenChatRequest {
        val systemPrompt = """
            你是一位专业的健身教练。根据用户的体态分析结果和身体数据，制定个性化的健身计划。

            【绝对要求 - 必须严格遵守】
            1. 每个训练日(exercises数组)必须包含4-8个具体动作
            2. 每个动作必须包含：name(中文名), category, sets(组数), reps(次数), weight(重量)
            3. 重量格式："60kg"、"自重"、"40%1RM" 等，不能为空
            4. category 必须是以下之一：chest, back, legs, shoulders, arms, core, cardio
            5. 动作名称要具体，例如："杠铃卧推"、"哑铃飞鸟"、"高位下拉"，不能是模糊描述

            【训练安排原则】
            - 根据用户每周可用训练天数，安排相应数量的训练日
            - 每个训练日针对特定肌群（如：胸+三头、背+二头、腿部、肩+腹）
            - 每次训练时长控制在用户指定范围内

            【休息日安排原则 - 必须严格遵守】
            - 不要把训练日安排在连续的天数！
            - 训练日之间必须至少间隔一天休息，让肌肉恢复
            - 例如：一周训练4天，应该安排在 周一(1)、周三(3)、周五(5)、周六(6)，而不是周一到周四
            - 可以在周末安排连续训练（如周六和周日）
            - 示例安排：
              * 3天/周：周一(1)、周三(3)、周五(5) 或 周一(1)、周四(4)、周六(6)
              * 4天/周：周一(1)、周三(3)、周五(5)、周六(6) 或 周二(2)、周四(4)、周六(6)、周日(7)
              * 5天/周：周一(1)、周二(2)、周四(4)、周五(5)、周日(7) 或 周一(1)、周三(3)、周四(4)、周六(6)、周日(7)

            请严格按照以下 JSON 格式返回，不要添加任何额外文字：
            {
              "name": "计划名称（如：4周增肌塑形计划）",
              "description": "计划描述（50字以内）",
              "goal": "训练目标",
              "cycleWeeks": 4,
              "weeklySchedule": [
                {
                  "dayOfWeek": 1,
                  "notes": "胸部+三头肌训练日",
                  "exercises": [
                    {"name": "杠铃卧推", "category": "chest", "sets": 4, "reps": "8-10", "weight": "60kg", "restSeconds": 90, "notes": "核心复合动作"},
                    {"name": "哑铃飞鸟", "category": "chest", "sets": 3, "reps": "12", "weight": "15kg", "restSeconds": 60, "notes": "胸肌拉伸"},
                    {"name": "绳索下压", "category": "arms", "sets": 3, "reps": "12-15", "weight": "25kg", "restSeconds": 60, "notes": "三头肌收尾"}
                  ]
                },
                {
                  "dayOfWeek": 3,
                  "notes": "背部+二头肌训练日",
                  "exercises": [
                    {"name": "引体向上", "category": "back", "sets": 4, "reps": "8-10", "weight": "自重", "restSeconds": 90, "notes": "背部宽度"},
                    {"name": "高位下拉", "category": "back", "sets": 3, "reps": "12", "weight": "50kg", "restSeconds": 60, "notes": "背阔肌"}
                  ]
                },
                {
                  "dayOfWeek": 5,
                  "notes": "腿部训练日",
                  "exercises": [
                    {"name": "深蹲", "category": "legs", "sets": 4, "reps": "8-10", "weight": "80kg", "restSeconds": 120, "notes": "腿部核心"},
                    {"name": "腿举", "category": "legs", "sets": 3, "reps": "12", "weight": "100kg", "restSeconds": 60, "notes": "股四头肌"}
                  ]
                }
              ]
            }

            注意：dayOfWeek 必须正确设置！1=周一，2=周二，3=周三，4=周四，5=周五，6=周六，7=周日

            再次强调：exercises 数组不能为空！每个训练日必须有具体动作！
        """.trimIndent()

        val userPrompt = """
            用户体态分析：
            - 体态评分: ${bodyAnalysis.postureScore}
            - 体脂率: ${bodyAnalysis.bodyFatPercentage}%
            - 肌肉平衡: ${bodyAnalysis.muscleBalance}
            - 体态问题: ${bodyAnalysis.issues.joinToString()}
            - 改进建议: ${bodyAnalysis.suggestions.joinToString()}

            用户身体数据：
            - 身高: ${userStats.height}cm
            - 体重: ${userStats.weight}kg
            - 年龄: ${userStats.age}岁
            - 性别: ${userStats.gender}
            - 健身目标: ${userStats.goal}
            - 健身经验: ${userStats.experience}
            - 每周可用训练天数: ${userStats.availableDays}天
            - 每次训练时长: ${userStats.sessionDuration}分钟

            请为用户制定健身计划。务必确保每个动作都有具体的重量建议！
        """.trimIndent()

        return QwenChatRequest(
            model = "qwen-plus",
            messages = listOf(
                QwenMessage(role = "system", content = systemPrompt),
                QwenMessage(role = "user", content = userPrompt)
            )
        )
    }

    /**
     * 构建计划调整请求
     */
    fun buildPlanAdjustmentRequest(
        currentPlan: String,
        recentProgress: String,
        issues: String
    ): QwenChatRequest {
        val systemPrompt = """
            你是一位专业的健身教练。根据用户最近的训练记录和反馈，分析是否需要调整训练计划。
            如果需要调整，请给出具体的调整建议。
        """.trimIndent()

        val userPrompt = """
            当前训练计划：
            $currentPlan

            最近训练记录：
            $recentProgress

            遇到的问题：
            $issues

            请分析并给出调整建议。
        """.trimIndent()

        return QwenChatRequest(
            model = "qwen-plus",
            messages = listOf(
                QwenMessage(role = "system", content = systemPrompt),
                QwenMessage(role = "user", content = userPrompt)
            )
        )
    }

    /**
     * 构建减载训练计划请求
     */
    fun buildDeloadPlanRequest(
        normalPlan: String,
        userStats: UserBodyStats
    ): QwenChatRequest {
        val systemPrompt = """
            你是一位专业的健身教练。用户需要减载训练来恢复和调整。

            减载训练原则：
            1. 训练强度降低至正常的 40-60%
            2. 减少组数（例如：4组 → 2-3组）
            3. 保持动作模式，专注于技术
            4. 每组不要达到力竭
            5. 缩短训练时长

            重要要求：
            1. 每个动作必须包含具体的组数、次数和重量建议
            2. 重量必须是正常训练的 40-60%
            3. 重量必须以"kg"为单位标注（如：30kg, 自重）
            4. 确保每个训练日有完整的动作列表

            请以 JSON 格式返回减载训练计划。
        """.trimIndent()

        val userPrompt = """
            用户身体数据：
            - 身高: ${userStats.height}cm
            - 体重: ${userStats.weight}kg
            - 年龄: ${userStats.age}岁
            - 性别: ${userStats.gender}
            - 健身目标: ${userStats.goal}
            - 健身经验: ${userStats.experience}

            正常训练计划：
            $normalPlan

            请为用户制定减载训练计划（1-2周）。注意：所有重量应该是正常训练的 40-60%！
        """.trimIndent()

        return QwenChatRequest(
            model = "qwen-plus",
            messages = listOf(
                QwenMessage(role = "system", content = systemPrompt),
                QwenMessage(role = "user", content = userPrompt)
            )
        )
    }
}

/**
 * 用户身体数据
 */
data class UserBodyStats(
    val height: Double,
    val weight: Double,
    val age: Int,
    val gender: String,
    val goal: String,
    val experience: String,
    val availableDays: Int = 4,
    val sessionDuration: Int = 60,
    val weeklyMinutes: Int = 0,
    val healthIssues: String = ""
)

/**
 * 体态分析结果
 */
data class BodyAnalysisResult(
    val postureScore: Int = 0,
    val bodyFatPercentage: Double = 0.0,
    val muscleBalance: String = "",
    val issues: List<String> = emptyList(),
    val suggestions: List<String> = emptyList()
)
