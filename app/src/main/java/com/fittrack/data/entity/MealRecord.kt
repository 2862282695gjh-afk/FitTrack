package com.fittrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 饮食记录实体
 * 用户手动记录的每餐饮食
 *
 * @property id 记录ID
 * @property date 日期（格式：yyyy-MM-dd）
 * @property mealType 餐次类型：breakfast / lunch / dinner / snack
 * @property foodsJson 食物列表 JSON，每项包含 {name, amount, calories, protein, carbs, fat}
 * @property totalCalories 本餐总热量(kcal)
 * @property totalProtein 本餐总蛋白质(g)
 * @property totalCarbs 本餐总碳水(g)
 * @property totalFat 本餐总脂肪(g)
 * @property note 备注
 * @property createdAt 创建时间戳
 */
@Entity(tableName = "meal_records")
data class MealRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // yyyy-MM-dd
    val mealType: String = "lunch", // breakfast, lunch, dinner, snack
    val foodsJson: String = "[]",
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 饮食推荐实体
 * AI 根据用户目标、训练状态生成的推荐方案
 *
 * @property id 推荐ID
 * @property date 日期（格式：yyyy-MM-dd）
 * @property mealType 餐次类型
 * @property goal 健身目标快照（冗余存储，避免用户改目标后历史推荐变味）
 * @property targetCalories 目标热量(kcal)
 * @property targetProtein 目标蛋白质(g)
 * @property targetCarbs 目标碳水(g)
 * @property targetFat 目标脂肪(g)
 * @property adviceJson 推荐食物列表 JSON，每项 {food, amount, calories, protein, carbs, fat, reason}
 * @property summary 推荐摘要（纯文本，1-2 句话）
 * @property createdAt 创建时间戳
 */
@Entity(tableName = "nutrition_advices")
data class NutritionAdvice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // yyyy-MM-dd
    val mealType: String = "lunch",
    val goal: String = "", // 冗余存储当时的健身目标
    val targetCalories: Double = 0.0,
    val targetProtein: Double = 0.0,
    val targetCarbs: Double = 0.0,
    val targetFat: Double = 0.0,
    val adviceJson: String = "[]",
    val summary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
