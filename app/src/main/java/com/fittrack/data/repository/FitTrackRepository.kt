package com.fittrack.data.repository

import com.fittrack.data.db.*
import com.fittrack.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * 健身追踪仓库
 * 统一管理所有数据访问
 */
class FitTrackRepository(
    private val workoutPlanDao: WorkoutPlanDao,
    private val exerciseDao: ExerciseDao,
    private val workoutRecordDao: WorkoutRecordDao,
    private val exerciseRecordDao: ExerciseRecordDao,
    private val userProfileDao: UserProfileDao? = null
) {

    // ==================== 健身计划相关 ====================

    fun getAllPlans(): Flow<List<WorkoutPlan>> = workoutPlanDao.getAllPlans()

    fun getActivePlans(): Flow<List<WorkoutPlan>> = workoutPlanDao.getActivePlans()

    suspend fun getPlanById(planId: Long): WorkoutPlan? = workoutPlanDao.getPlanById(planId)

    suspend fun insertPlan(plan: WorkoutPlan): Long = workoutPlanDao.insertPlan(plan)

    suspend fun updatePlan(plan: WorkoutPlan) = workoutPlanDao.updatePlan(plan)

    suspend fun deletePlan(plan: WorkoutPlan) {
        // 删除计划时同时删除关联的训练记录、动作记录和动作
        // 1. 先获取该计划的所有训练记录
        val records = workoutRecordDao.getRecordsByPlanOnce(plan.id)
        // 2. 删除每条训练记录关联的动作记录
        records.forEach { record ->
            exerciseRecordDao.deleteExerciseRecordsByWorkout(record.id)
        }
        // 3. 删除该计划的所有训练记录
        workoutRecordDao.deleteRecordsByPlan(plan.id)
        // 4. 删除该计划的所有动作
        exerciseDao.deleteExercisesByPlan(plan.id)
        // 5. 最后删除计划本身
        workoutPlanDao.deletePlanById(plan.id)
    }

    suspend fun getExercisesForPlan(planId: Long): List<Exercise> = exerciseDao.getExercisesForPlan(planId)

    // ==================== 训练动作相关 ====================

    fun getExercisesByPlan(planId: Long): Flow<List<Exercise>> = exerciseDao.getExercisesByPlan(planId)

    suspend fun getExerciseById(exerciseId: Long): Exercise? = exerciseDao.getExerciseById(exerciseId)

    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insertExercise(exercise)

    suspend fun insertExercises(exercises: List<Exercise>) = exerciseDao.insertExercises(exercises)

    suspend fun updateExercise(exercise: Exercise) = exerciseDao.updateExercise(exercise)

    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.deleteExercise(exercise)

    // ==================== 训练记录相关 ====================

    fun getAllRecords(): Flow<List<WorkoutRecord>> = workoutRecordDao.getAllRecords()

    fun getRecordsByPlan(planId: Long): Flow<List<WorkoutRecord>> = workoutRecordDao.getRecordsByPlan(planId)

    fun getRecordsForPlan(planId: Long): Flow<List<WorkoutRecord>> = workoutRecordDao.getRecordsByPlan(planId)

    fun getRecordsByDate(date: String): Flow<List<WorkoutRecord>> = workoutRecordDao.getRecordsByDate(date)

    fun getRecordsBetween(startDate: String, endDate: String): Flow<List<WorkoutRecord>> =
        workoutRecordDao.getRecordsBetween(startDate, endDate)

    suspend fun getRecordById(recordId: Long): WorkoutRecord? = workoutRecordDao.getRecordById(recordId)

    suspend fun insertRecord(record: WorkoutRecord): Long = workoutRecordDao.insertRecord(record)

    suspend fun insertWorkoutRecord(record: WorkoutRecord): Long = workoutRecordDao.insertRecord(record)

    suspend fun updateRecord(record: WorkoutRecord) = workoutRecordDao.updateRecord(record)

    suspend fun deleteRecord(record: WorkoutRecord) {
        // 删除记录时同时删除关联的动作记录
        exerciseRecordDao.deleteExerciseRecordsByWorkout(record.id)
        workoutRecordDao.deleteRecord(record)
    }

    // ==================== 统计相关 ====================

    fun getWorkoutCountSince(startDate: String): Flow<Int> = workoutRecordDao.getWorkoutCountSince(startDate)

    fun getTotalDurationSince(startDate: String): Flow<Int?> = workoutRecordDao.getTotalDurationSince(startDate)

    // ==================== 动作记录相关 ====================

    fun getExerciseRecordsByWorkout(recordId: Long): Flow<List<ExerciseRecord>> =
        exerciseRecordDao.getExerciseRecordsByWorkout(recordId)

    suspend fun getRecentRecordsForExercise(exerciseId: Long, limit: Int = 10): List<ExerciseRecord> =
        exerciseRecordDao.getRecentRecordsForExercise(exerciseId, limit)

    suspend fun insertExerciseRecord(record: ExerciseRecord): Long = exerciseRecordDao.insertExerciseRecord(record)

    suspend fun insertExerciseRecords(records: List<ExerciseRecord>) = exerciseRecordDao.insertExerciseRecords(records)

    suspend fun deleteExerciseRecord(record: ExerciseRecord) = exerciseRecordDao.deleteExerciseRecord(record)

    // ==================== 用户档案相关 ====================

    /**
     * 获取用户档案
     */
    fun getProfile() = userProfileDao?.getProfileFlow()

    /**
     * 获取用户档案（一次性）
     */
    suspend fun getProfileOnce(): UserProfile? = userProfileDao?.getProfile()

    /**
     * 保存用户档案
     */
    suspend fun saveProfile(profile: UserProfile): Long {
        return userProfileDao?.insertOrUpdateProfile(profile) ?: 0L
    }

    /**
     * 更新身体参数
     */
    suspend fun updateBodyStats(profileId: Long, heightCm: Double, weightKg: Double, targetWeightKg: Double) {
        userProfileDao?.updateBodyStats(profileId, heightCm, weightKg, targetWeightKg)
    }

    /**
     * 更新照片路径
     */
    suspend fun updatePhotoPaths(profileId: Long, frontPath: String, sidePath: String, backPath: String) {
        userProfileDao?.updatePhotoPaths(profileId, frontPath, sidePath, backPath)
    }

    /**
     * 更新 AI 分析结果
     */
    suspend fun updateBodyAnalysis(profileId: Long, analysisJson: String) {
        userProfileDao?.updateBodyAnalysis(profileId, analysisJson)
    }

    // ==================== 体重记录相关 ====================

    /**
     * 获取所有体重记录
     */
    fun getAllWeightRecords() = userProfileDao?.getAllWeightRecords()

    /**
     * 获取最近体重记录
     */
    fun getRecentWeightRecords(limit: Int = 30) = userProfileDao?.getRecentWeightRecords(limit)

    /**
     * 添加体重记录
     */
    suspend fun insertWeightRecord(record: WeightRecord): Long {
        return userProfileDao?.insertWeightRecord(record) ?: 0L
    }

    /**
     * 删除体重记录
     */
    suspend fun deleteWeightRecord(record: WeightRecord) {
        userProfileDao?.deleteWeightRecord(record)
    }

    // ==================== 身体测量相关 ====================

    /**
     * 获取所有身体测量记录
     */
    fun getAllMeasurements() = userProfileDao?.getAllMeasurements()

    /**
     * 获取最近测量记录
     */
    fun getRecentMeasurements(limit: Int = 10) = userProfileDao?.getRecentMeasurements(limit)

    /**
     * 添加测量记录
     */
    suspend fun insertMeasurement(measurement: BodyMeasurement): Long {
        return userProfileDao?.insertMeasurement(measurement) ?: 0L
    }

    /**
     * 删除测量记录
     */
    suspend fun deleteMeasurement(measurement: BodyMeasurement) {
        userProfileDao?.deleteMeasurement(measurement)
    }

    // ==================== 辅助方法 ====================

    companion object {
        fun getTodayDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun getWeekStartDate(): String {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(calendar.time)
        }

        fun getMonthStartDate(): String {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(calendar.time)
        }
    }
}
