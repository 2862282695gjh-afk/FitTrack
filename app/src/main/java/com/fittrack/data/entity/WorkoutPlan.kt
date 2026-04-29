package com.fittrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 健身计划实体
 * @property id 计划ID
 * @property name 计划名称（如：增肌计划、减脂计划）
 * @property description 计划描述
 * @property goal 训练目标（增肌/减脂/力量/耐力）
 * @property cycleDays 计划周期（天数）
 * @property reminderDays 提醒日期（如 "1,3,5" 表示周一三五）
 * @property reminderTime 提醒时间（如 "07:00"）
 * @property isActive 是否激活
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
@Entity(tableName = "workout_plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val goal: String = "general", // general, muscle_gain, fat_loss, strength, endurance
    val cycleDays: Int = 7,
    val reminderDays: String = "1,3,5", // Monday, Wednesday, Friday
    val reminderTime: String = "07:00",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 训练动作实体
 * @property id 动作ID
 * @property planId 所属计划ID
 * @property name 动作名称（如：深蹲、卧推）
 * @property category 动作分类（胸/背/腿/肩/手臂/核心/有氧）
 * @property defaultSets 默认组数
 * @property defaultReps 默认次数
 * @property defaultWeight 默认重量(kg)
 * @property defaultDuration 默认时长(分钟)
 * @property restIntervalSeconds 组间歇时间(秒)，默认 90 秒
 * @property notes 备注
 * @property dayOfWeek 训练日（1-7 表示周一到周日）
 * @property orderIndex 排序索引
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long,
    val name: String,
    val category: String = "other", // chest, back, legs, shoulders, arms, core, cardio, other
    val defaultSets: Int = 3,
    val defaultReps: Int = 10,
    val defaultWeight: Double = 0.0,
    val defaultDuration: Int = 0, // minutes, for cardio
    val restIntervalSeconds: Int = 90,
    val notes: String = "",
    val dayOfWeek: Int = 1, // 1-7 for Monday-Sunday
    val orderIndex: Int = 0
)

/**
 * 训练记录实体
 * @property id 记录ID
 * @property planId 计划ID
 * @property date 训练日期（格式：yyyy-MM-dd）
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property totalDuration 总时长(分钟)
 * @property notes 训练笔记
 * @property feeling 训练感受评分（1-5）
 * @property sleepQuality 睡眠质量（1-5）
 * @property appetite 食欲（1-5）
 * @property energyLevel 能量水平（1-5）
 * @property metabolicPressure 代谢压力（0-100，自动计算）
 * @property mentalPressure 精神压力（0-100，自动计算）
 * @property isDeload 是否为减载训练
 * @property createdAt 创建时间
 */
@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long,
    val date: String, // yyyy-MM-dd
    val startTime: Long = 0,
    val endTime: Long = 0,
    val totalDuration: Int = 0, // minutes
    val notes: String = "",
    val feeling: Int = 3, // 1-5
    val sleepQuality: Int = 3, // 1-5
    val appetite: Int = 3, // 1-5
    val energyLevel: Int = 3, // 1-5
    val metabolicPressure: Int = 0, // 0-100
    val mentalPressure: Int = 0, // 0-100
    val isDeload: Boolean = false,
    val aiSummary: String = "", // AI 生成的训练总结
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 动作记录实体（每次训练中每个动作的详细记录）
 * @property id 记录ID
 * @property recordId 训练记录ID
 * @property exerciseId 动作ID
 * @property exerciseName 动作名称（冗余存储，方便查询）
 * @property sets 完成的组数
 * @property reps 每组次数（逗号分隔，如 "10,10,8"）
 * @property weights 每组重量（逗号分隔，如 "60,65,70"）
 * @property duration 时长(分钟，用于有氧)
 * @property notes 备注
 */
@Entity(tableName = "exercise_records")
data class ExerciseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val sets: Int = 0,
    val reps: String = "", // comma separated
    val weights: String = "", // comma separated
    val duration: Int = 0,
    val notes: String = ""
)
