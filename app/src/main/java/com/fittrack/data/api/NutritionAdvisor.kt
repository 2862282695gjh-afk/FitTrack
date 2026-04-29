package com.fittrack.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

/**
 * AI 饮食推荐参数
 */
data class NutritionAdviceRequest(
    val date: String,
    val mealType: String, // breakfast, lunch, dinner, snack
    val fitnessGoal: String, // lose_weight, gain_muscle, maintain, improve_health
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val gender: String,
    val alreadyEatenCalories: Double = 0.0, // 今日已摄入热量
    val todayWorkoutDuration: Int = 0, // 今日训练时长(分钟)
    val todayWorkoutIntensity: String = "rest", // rest, light, moderate, intense
    val healthIssues: String = ""
)

/**
 * 推荐食物条目
 */
data class AdviceFoodItem(
    val food: String,
    val amount: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val reason: String = ""
)

/**
 * AI 饮食推荐结果（解析后）
 */
data class ParsedNutritionAdvice(
    val targetCalories: Double,
    val targetProtein: Double,
    val targetCarbs: Double,
    val targetFat: Double,
    val foods: List<AdviceFoodItem>,
    val summary: String = "",
    val rawResponse: String
)

/**
 * 饮食推荐服务
 * 通过 Qwen API 根据用户状态生成个性化饮食建议
 */
class NutritionAdvisor(private val qwenRepository: QwenRepository) {

    companion object {
        private const val TAG = "NutritionAdvisor"

        /** 每日基础代谢率简易计算（Mifflin-St Jeor） */
        fun estimateBMR(weightKg: Double, heightCm: Double, age: Int, gender: String): Double {
            return if (gender == "female") {
                10.0 * weightKg + 6.25 * heightCm - 5.0 * age - 161.0
            } else {
                10.0 * weightKg + 6.25 * heightCm - 5.0 * age + 5.0
            }
        }

        /** 根据目标和训练强度计算每日目标热量 */
        fun calculateTargetCalories(
            bmr: Double,
            goal: String,
            workoutDuration: Int,
            intensity: String
        ): Double {
            // 基础活动系数（久坐）
            var tdee = bmr * 1.2

            // 训练消耗追加
            val activityMultiplier = when (intensity) {
                "intense" -> 1.725
                "moderate" -> 1.55
                "light" -> 1.375
                else -> 1.0
            }
            if (workoutDuration > 0) {
                // 将训练日的活动系数按训练时长比例混入
                val trainingFraction = (workoutDuration / 60.0).coerceIn(0.0, 1.0)
                tdee = bmr * 1.2 * (1.0 - trainingFraction) + bmr * activityMultiplier * trainingFraction
            }

            // 根据目标调整
            return when (goal) {
                "lose_weight" -> (tdee * 0.8).coerceAtLeast(bmr * 1.0) // 减脂：-20% 但不低于 BMR
                "gain_muscle" -> tdee * 1.15 // 增肌：+15%
                "improve_health" -> tdee * 1.0 // 维持健康
                else -> tdee * 1.0 // maintain
            }
        }
    }

    private val gson = Gson()

    /**
     * 生成一餐的饮食推荐
     */
    suspend fun generateAdvice(request: NutritionAdviceRequest): QwenResult<ParsedNutritionAdvice> {
        if (!qwenRepository.hasApiKey) {
            return QwenResult.Error("API Key 未配置")
        }

        val bmr = estimateBMR(request.weightKg, request.heightCm, request.age, request.gender)
        val dailyTarget = calculateTargetCalories(
            bmr, request.fitnessGoal,
            request.todayWorkoutDuration, request.todayWorkoutIntensity
        )

        // 该餐分配比例
        val mealRatio = when (request.mealType) {
            "breakfast" -> 0.30
            "lunch" -> 0.40
            "dinner" -> 0.25
            "snack" -> 0.05
            else -> 0.30
        }

        val mealTargetCalories = (dailyTarget * mealRatio - request.alreadyEatenCalories)
            .coerceAtLeast(200.0)

        val systemPrompt = buildSystemPrompt()
        val userPrompt = buildUserPrompt(request, dailyTarget, mealTargetCalories, mealRatio)

        return when (val result = qwenRepository.chat(
            messages = listOf(
                QwenMessage(role = "system", content = systemPrompt),
                QwenMessage(role = "user", content = userPrompt)
            ),
            temperature = 0.6
        )) {
            is QwenResult.Success -> QwenResult.Success(
                parseResponse(result.data, mealTargetCalories)
            )
            is QwenResult.Error -> result
        }
    }

    private fun buildSystemPrompt(): String {
        return """
            你是一位专业的运动营养师。根据用户的健身目标、身体数据和当日训练情况，为用户生成一餐的饮食推荐。

            【严格要求】
            1. 推荐的食物要日常可得，不要推荐罕见的补剂或难以获取的食材
            2. 每餐推荐 3-6 种食物/菜品，荤素搭配合理
            3. 重量/份量要具体（如 "鸡胸肉 150g"、"糙米饭 200g"）
            4. 必须严格按 JSON 格式返回，不要添加任何额外文字

            【返回 JSON 格式】
            {
              "targetCalories": 550,
              "targetProtein": 35,
              "targetCarbs": 65,
              "targetFat": 12,
              "foods": [
                {
                  "food": "鸡胸肉",
                  "amount": "150g",
                  "calories": 165,
                  "protein": 31,
                  "carbs": 0,
                  "fat": 3.6,
                  "reason": "高蛋白低脂，增肌必备"
                },
                {
                  "food": "糙米饭",
                  "amount": "200g",
                  "calories": 232,
                  "protein": 5,
                  "carbs": 48,
                  "fat": 1.8,
                  "reason": "复合碳水，提供持续能量"
                }
              ],
              "summary": "本餐以高蛋白为主，搭配复合碳水，适合训练后恢复"
            }

            注意：
            - targetCalories/Protein/Carbs/Fat 是本餐的目标营养素（单位分别为 kcal/g/g/g）
            - foods 中每项必须包含 food, amount, calories, protein, carbs, fat
            - reason 是一句话推荐理由
            - summary 是本餐推荐的一句话总结
        """.trimIndent()
    }

    private fun buildUserPrompt(
        request: NutritionAdviceRequest,
        dailyTarget: Double,
        mealTargetCalories: Double,
        mealRatio: Double
    ): String {
        val goalText = when (request.fitnessGoal) {
            "lose_weight" -> "减脂"
            "gain_muscle" -> "增肌"
            "improve_health" -> "改善健康"
            else -> "维持体重"
        }
        val mealText = when (request.mealType) {
            "breakfast" -> "早餐"
            "lunch" -> "午餐"
            "dinner" -> "晚餐"
            "snack" -> "加餐"
            else -> "午餐"
        }
        val intensityText = when (request.todayWorkoutIntensity) {
            "intense" -> "高强度"
            "moderate" -> "中等强度"
            "light" -> "低强度"
            else -> "休息日"
        }

        val sb = StringBuilder()
        sb.appendLine("请为用户推荐「$mealText」的饮食方案。")
        sb.appendLine()
        sb.appendLine("## 用户信息")
        sb.appendLine("- 性别：${request.gender}")
        sb.appendLine("- 年龄：${request.age}岁")
        sb.appendLine("- 身高：${request.heightCm}cm")
        sb.appendLine("- 体重：${request.weightKg}kg")
        sb.appendLine("- 健身目标：$goalText")
        if (request.healthIssues.isNotBlank()) {
            sb.appendLine("- 健康问题：${request.healthIssues}")
        }
        sb.appendLine()
        sb.appendLine("## 今日训练情况")
        sb.appendLine("- 训练时长：${request.todayWorkoutDuration}分钟")
        sb.appendLine("- 训练强度：$intensityText")
        sb.appendLine()
        sb.appendLine("## 营养目标")
        sb.appendLine("- 每日目标热量：${dailyTarget.toInt()} kcal")
        sb.appendLine("- 本餐(${mealText})占比：${(mealRatio * 100).toInt()}%")
        sb.appendLine("- 本餐目标热量：${mealTargetCalories.toInt()} kcal")
        if (request.alreadyEatenCalories > 0) {
            sb.appendLine("- 今日已摄入：${request.alreadyEatenCalories.toInt()} kcal")
        }
        sb.appendLine()
        sb.appendLine("请根据以上信息推荐合适的${mealText}方案。")

        return sb.toString()
    }

    private fun parseResponse(content: String, fallbackCalories: Double): ParsedNutritionAdvice {
        return try {
            val jsonStr = extractJson(content)
            val json = gson.fromJson<JsonObject>(jsonStr, object : TypeToken<JsonObject>() {}.type)

            val foods = json.getAsJsonArray("foods")?.map { elem ->
                val obj = elem.asJsonObject
                AdviceFoodItem(
                    food = obj.getAsJsonPrimitive("food")?.asString ?: "",
                    amount = obj.getAsJsonPrimitive("amount")?.asString ?: "",
                    calories = obj.getAsJsonPrimitive("calories")?.asDouble ?: 0.0,
                    protein = obj.getAsJsonPrimitive("protein")?.asDouble ?: 0.0,
                    carbs = obj.getAsJsonPrimitive("carbs")?.asDouble ?: 0.0,
                    fat = obj.getAsJsonPrimitive("fat")?.asDouble ?: 0.0,
                    reason = obj.getAsJsonPrimitive("reason")?.asString ?: ""
                )
            } ?: emptyList()

            ParsedNutritionAdvice(
                targetCalories = json.getAsJsonPrimitive("targetCalories")?.asDouble ?: fallbackCalories,
                targetProtein = json.getAsJsonPrimitive("targetProtein")?.asDouble ?: 0.0,
                targetCarbs = json.getAsJsonPrimitive("targetCarbs")?.asDouble ?: 0.0,
                targetFat = json.getAsJsonPrimitive("targetFat")?.asDouble ?: 0.0,
                foods = foods,
                summary = json.getAsJsonPrimitive("summary")?.asString ?: "",
                rawResponse = content
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析饮食推荐失败: ${e.message}", e)
            ParsedNutritionAdvice(
                targetCalories = fallbackCalories,
                targetProtein = 0.0,
                targetCarbs = 0.0,
                targetFat = 0.0,
                foods = emptyList(),
                summary = "",
                rawResponse = content
            )
        }
    }

    private fun extractJson(content: String): String {
        val startIndex = content.indexOf('{')
        val endIndex = content.lastIndexOf('}')
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            content.substring(startIndex, endIndex + 1)
        } else {
            content
        }
    }
}
