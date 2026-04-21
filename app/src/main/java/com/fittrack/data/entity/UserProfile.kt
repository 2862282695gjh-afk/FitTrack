package com.fittrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户档案实体
 * 存储用户的身体参数和基本信息
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 基本信息
    val name: String = "",
    val gender: String = "", // "male", "female", "other"
    val age: Int = 0,

    // 身体参数
    val heightCm: Double = 0.0,      // 身高 (厘米)
    val weightKg: Double = 0.0,      // 体重 (公斤)
    val targetWeightKg: Double = 0.0, // 目标体重

    // 健身目标
    val fitnessGoal: String = "", // "lose_weight", "gain_muscle", "maintain", "improve_health"

    // 健身经验
    val experienceLevel: String = "", // "beginner", "intermediate", "advanced"

    // 可用训练时间 (每周分钟数)
    val weeklyAvailableMinutes: Int = 0,

    // 健康问题/限制
    val healthIssues: String = "",

    // 照片路径 (用于 AI 体态分析)
    val frontPhotoPath: String = "",
    val sidePhotoPath: String = "",
    val backPhotoPath: String = "",

    // AI 分析结果 (JSON 格式存储)
    val bodyAnalysisJson: String = "",

    // 时间戳
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 计算 BMI
     */
    fun calculateBMI(): Double {
        if (heightCm <= 0 || weightKg <= 0) return 0.0
        val heightM = heightCm / 100
        return weightKg / (heightM * heightM)
    }

    /**
     * 获取 BMI 分类
     */
    fun getBMICategory(): String {
        val bmi = calculateBMI()
        return when {
            bmi < 18.5 -> "偏瘦"
            bmi < 24 -> "正常"
            bmi < 28 -> "偏胖"
            else -> "肥胖"
        }
    }

    /**
     * 是否有照片可用于分析
     */
    fun hasPhotos(): Boolean {
        return frontPhotoPath.isNotEmpty() ||
                sidePhotoPath.isNotEmpty() ||
                backPhotoPath.isNotEmpty()
    }

    /**
     * 是否基本信息已填写完整
     */
    fun isBasicInfoComplete(): Boolean {
        return heightCm > 0 && weightKg > 0 && age > 0 && gender.isNotEmpty()
    }
}

/**
 * 体重记录实体
 * 用于追踪体重变化
 */
@Entity(tableName = "weight_record")
data class WeightRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val weightKg: Double,
    val recordedAt: Long = System.currentTimeMillis(),
    val note: String = ""
)

/**
 * 身体测量记录实体
 * 用于追踪围度变化
 */
@Entity(tableName = "body_measurement")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val chestCm: Double = 0.0,       // 胸围
    val waistCm: Double = 0.0,       // 腰围
    val hipCm: Double = 0.0,         // 臀围
    val leftArmCm: Double = 0.0,     // 左臂围
    val rightArmCm: Double = 0.0,    // 右臂围
    val leftThighCm: Double = 0.0,   // 左大腿围
    val rightThighCm: Double = 0.0,  // 右大腿围

    val measuredAt: Long = System.currentTimeMillis(),
    val note: String = ""
)
