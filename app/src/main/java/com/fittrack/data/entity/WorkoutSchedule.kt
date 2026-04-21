package com.fittrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 训练日程实体（存储具体日期的训练计划）
 * @property id 日程ID
 * @property planId 所属计划ID
 * @property scheduledDate 计划训练日期（格式：yyyy-MM-dd）
 * @property originalScheduledDate 原始计划日期（用于逾期判断）
 * @property status 状态：pending（待训练）/completed（已完成）/overdue（已逾期）
 * @property isRescheduled 是否已延期
 * @property createdAt 创建时间
 */
@Entity(tableName = "workout_schedules")
data class WorkoutSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long,
    val scheduledDate: String, // yyyy-MM-dd
    val originalScheduledDate: String, // 原始日期，用于逾期判断
    val status: String = "pending", // pending, completed, overdue
    val isRescheduled: Boolean = false, // 是否已延期
    val createdAt: Long = System.currentTimeMillis()
)
