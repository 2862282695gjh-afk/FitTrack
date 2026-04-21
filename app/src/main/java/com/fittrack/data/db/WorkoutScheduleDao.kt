package com.fittrack.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fittrack.data.entity.WorkoutSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutScheduleDao {

    /**
     * 插入训练日程
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: WorkoutSchedule): Long

    /**
     * 批量插入训练日程
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<WorkoutSchedule>)

    /**
     * 更新训练日程
     */
    @Update
    suspend fun update(schedule: WorkoutSchedule)

    /**
     * 根据ID获取训练日程
     */
    @Query("SELECT * FROM workout_schedules WHERE id = :scheduleId")
    suspend fun getScheduleById(scheduleId: Long): WorkoutSchedule?

    /**
     * 根据ID删除训练日程
     */
    @Query("DELETE FROM workout_schedules WHERE id = :scheduleId")
    suspend fun deleteById(scheduleId: Long)

    /**
     * 根据计划ID删除所有训练日程
     */
    @Query("DELETE FROM workout_schedules WHERE planId = :planId")
    suspend fun deleteByPlanId(planId: Long)

    /**
     * 根据计划ID获取所有训练日程（按日期排序）
     */
    @Query("SELECT * FROM workout_schedules WHERE planId = :planId ORDER BY scheduledDate ASC")
    fun getSchedulesByPlan(planId: Long): Flow<List<WorkoutSchedule>>

    /**
     * 获取指定日期的训练日程
     */
    @Query("SELECT * FROM workout_schedules WHERE scheduledDate = :date ORDER BY id ASC")
    fun getSchedulesByDate(date: String): Flow<List<WorkoutSchedule>>

    /**
     * 获取指定日期的训练日程（同步）
     */
    @Query("SELECT * FROM workout_schedules WHERE scheduledDate = :date ORDER BY id ASC")
    suspend fun getSchedulesByDateSync(date: String): List<WorkoutSchedule>

    /**
     * 获取所有逾期训练日程
     */
    @Query("SELECT * FROM workout_schedules WHERE status = 'overdue' ORDER BY scheduledDate ASC")
    fun getOverdueSchedules(): Flow<List<WorkoutSchedule>>

    /**
     * 获取所有逾期训练日程（同步）
     */
    @Query("SELECT * FROM workout_schedules WHERE status = 'overdue' ORDER BY scheduledDate ASC")
    suspend fun getOverdueSchedulesSync(): List<WorkoutSchedule>

    /**
     * 获取待完成训练日程（从今天开始）
     */
    @Query("SELECT * FROM workout_schedules WHERE status = 'pending' AND scheduledDate >= :date ORDER BY scheduledDate ASC LIMIT 1")
    suspend fun getNextPendingSchedule(date: String): WorkoutSchedule?

    /**
     * 获取所有待完成训练日程
     */
    @Query("SELECT * FROM workout_schedules WHERE status = 'pending' ORDER BY scheduledDate ASC")
    fun getPendingSchedules(): Flow<List<WorkoutSchedule>>

    /**
     * 更新训练日程状态
     */
    @Query("UPDATE workout_schedules SET status = :status WHERE id = :scheduleId")
    suspend fun updateStatus(scheduleId: Long, status: String)

    /**
     * 将指定日期的待完成训练标记为逾期
     */
    @Query("UPDATE workout_schedules SET status = 'overdue' WHERE scheduledDate < :date AND status = 'pending'")
    suspend fun markAsOverdue(date: String)

    /**
     * 延期训练日程到新日期
     */
    @Query("UPDATE workout_schedules SET scheduledDate = :newDate, isRescheduled = 1 WHERE id = :scheduleId")
    suspend fun reschedule(scheduleId: Long, newDate: String)

    /**
     * 获取日期范围内的训练日程
     */
    @Query("SELECT * FROM workout_schedules WHERE scheduledDate BETWEEN :startDate AND :endDate ORDER BY scheduledDate ASC")
    fun getSchedulesBetween(startDate: String, endDate: String): Flow<List<WorkoutSchedule>>

    /**
     * 检查指定日期是否有训练
     */
    @Query("SELECT COUNT(*) FROM workout_schedules WHERE scheduledDate = :date")
    suspend fun hasScheduleOnDate(date: String): Int

    /**
     * 获取最近未完成训练的最早日期
     */
    @Query("SELECT MIN(scheduledDate) FROM workout_schedules WHERE status = 'pending' AND scheduledDate >= :date")
    suspend fun getNextTrainingDate(date: String): String?
}
