package com.fittrack.data.scheduler

import android.content.Context
import com.fittrack.data.db.ExerciseDao
import com.fittrack.data.db.FitTrackDatabase
import com.fittrack.data.db.WorkoutPlanDao
import com.fittrack.data.db.WorkoutScheduleDao
import com.fittrack.data.entity.Exercise
import com.fittrack.data.entity.WorkoutSchedule
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * 训练计划管理器
 * 负责训练日程的生成、延期和逾期检测
 */
class WorkoutScheduler(private val context: Context) {

    private val database = FitTrackDatabase.getDatabase(context)
    private val workoutPlanDao: WorkoutPlanDao = database.workoutPlanDao()
    private val exerciseDao: ExerciseDao = database.exerciseDao()
    private val workoutScheduleDao: WorkoutScheduleDao = database.workoutScheduleDao()

    /**
     * 日期格式
     */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 为指定计划生成训练日程
     * @param planId 计划ID
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param weeks 生成的周数
     */
    suspend fun generateScheduleForPlan(planId: Long, startDate: String, weeks: Int = 4) {
        val plan = workoutPlanDao.getPlanById(planId)
        if (plan == null || !plan.isActive) return

        val planDays = parseTrainingDays(plan.reminderDays)
        if (planDays.isEmpty()) return

        val startCal = parseDate(startDate)
        val schedules = mutableListOf<WorkoutSchedule>()

        // 生成指定周数的日程
        val calendar = Calendar.getInstance()
        calendar.time = startCal

        for (week in 0 until weeks) {
            for (day in planDays) {
                val scheduledDate = calculateDate(calendar, week, day)
                val schedule = WorkoutSchedule(
                    planId = planId,
                    scheduledDate = scheduledDate,
                    originalScheduledDate = scheduledDate,
                    status = "pending",
                    isRescheduled = false
                )
                schedules.add(schedule)
            }
        }

        // 删除旧的日程并插入新的
        workoutScheduleDao.deleteByPlanId(planId)
        workoutScheduleDao.insertAll(schedules)
    }

    /**
     * 检查并标记逾期的训练
     * @return 标记为逾期的日程数量
     */
    suspend fun checkAndMarkOverdue(): Int {
        val today = dateFormat.format(Date())
        workoutScheduleDao.markAsOverdue(today)

        // 获取所有逾期日程
        val overdueSchedules = workoutScheduleDao.getOverdueSchedulesSync()
        return overdueSchedules.size
    }

    /**
     * 自动延期逾期训练到下一个可用日期
     * @return 延期的日程数量
     */
    suspend fun autoRescheduleOverdue(): Int {
        val overdueSchedules = workoutScheduleDao.getOverdueSchedulesSync()
        var rescheduledCount = 0

        for (schedule in overdueSchedules) {
            val nextAvailableDate = findNextAvailableDate(schedule.planId)
            if (nextAvailableDate != null) {
                workoutScheduleDao.reschedule(schedule.id, nextAvailableDate)
                // 保持逾期状态，让用户知道这次训练是逾期的
                rescheduledCount++
            }
        }

        return rescheduledCount
    }

    /**
     * 延期指定训练日程
     * @param scheduleId 日程ID
     * @param daysToDelay 延期的天数
     */
    suspend fun reschedule(scheduleId: Long, daysToDelay: Int = 1) {
        val schedule = workoutScheduleDao.getScheduleById(scheduleId) ?: return

        val cal = Calendar.getInstance()
        cal.time = parseDate(schedule.scheduledDate)
        cal.add(Calendar.DAY_OF_MONTH, daysToDelay)
        val newDate = dateFormat.format(cal.time)

        workoutScheduleDao.reschedule(scheduleId, newDate)
    }

    /**
     * 完成训练
     * @param scheduleId 日程ID
     */
    suspend fun completeWorkout(scheduleId: Long) {
        workoutScheduleDao.updateStatus(scheduleId, "completed")
    }

    /**
     * 获取今天的训练日程
     */
    suspend fun getTodaySchedule(): List<WorkoutSchedule> {
        val today = dateFormat.format(Date())
        return workoutScheduleDao.getSchedulesByDateSync(today)
    }

    /**
     * 获取今天的训练动作（支持多计划）
     */
    suspend fun getTodayExercises(): List<Exercise> {
        val todaySchedules = getTodaySchedule()
        if (todaySchedules.isEmpty()) return emptyList()

        // 收集所有不同计划的今日动作
        val planIds = todaySchedules.map { it.planId }.distinct()
        val allExercises = mutableListOf<Exercise>()
        for (planId in planIds) {
            allExercises.addAll(exerciseDao.getExercisesByPlan(planId).first())
        }
        return allExercises
    }

    /**
     * 获取下一个待训练日程
     */
    suspend fun getNextTrainingSchedule(): WorkoutSchedule? {
        val today = dateFormat.format(Date())
        return workoutScheduleDao.getNextPendingSchedule(today)
    }

    /**
     * 检查今天是否有训练
     */
    suspend fun hasTrainingToday(): Boolean {
        val today = dateFormat.format(Date())
        return workoutScheduleDao.hasScheduleOnDate(today) > 0
    }

    /**
     * 检查今天是否有逾期训练需要显示
     */
    suspend fun hasOverdueTraining(): Boolean {
        val overdueSchedules = workoutScheduleDao.getOverdueSchedulesSync()
        return overdueSchedules.isNotEmpty()
    }

    /**
     * 解析训练日字符串
     * 例如 "1,3,5" 解析为 [1, 3, 5]
     */
    private fun parseTrainingDays(reminderDays: String): List<Int> {
        return reminderDays
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .distinct()
            .sorted()
    }

    /**
     * 解析日期字符串
     */
    private fun parseDate(dateStr: String): Date {
        return dateFormat.parse(dateStr) ?: Date()
    }

    /**
     * 计算指定周数和周几的日期
     */
    private fun calculateDate(baseCalendar: Calendar, weekOffset: Int, dayOfWeek: Int): String {
        val calendar = baseCalendar.clone() as Calendar

        // 设置到该周的第一天（周一）
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        // 计算目标日期
        calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)
        calendar.add(Calendar.DAY_OF_MONTH, dayOfWeek - 1) // 周一是1，需要-1

        return dateFormat.format(calendar.time)
    }

    /**
     * 查找下一个可用的训练日期（没有训练的日期）
     */
    private suspend fun findNextAvailableDate(planId: Long): String? {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1) // 从明天开始

        // 查找接下来7天内没有训练的日期
        for (i in 0 until 7) {
            val date = dateFormat.format(calendar.time)
            val count = workoutScheduleDao.hasScheduleOnDate(date)

            if (count == 0) {
                return date
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return null
    }

    /**
     * 刷新指定计划的日程（重新生成）
     */
    suspend fun refreshSchedule(planId: Long, weeks: Int = 4) {
        val today = dateFormat.format(Date())
        generateScheduleForPlan(planId, today, weeks)
    }
}
