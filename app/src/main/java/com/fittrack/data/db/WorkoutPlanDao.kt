package com.fittrack.data.db

import kotlinx.coroutines.flow.Flow
import androidx.room.*
import com.fittrack.data.entity.WorkoutPlan

@Dao
interface WorkoutPlanDao {

    @Query("SELECT * FROM workout_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_plans WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActivePlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_plans WHERE id = :planId")
    suspend fun getPlanById(planId: Long): WorkoutPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlan): Long

    @Update
    suspend fun updatePlan(plan: WorkoutPlan)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlan)

    @Query("DELETE FROM workout_plans WHERE id = :planId")
    suspend fun deletePlanById(planId: Long)

    @Query("SELECT * FROM workout_plans WHERE reminderDays != ''")
    suspend fun getPlansWithReminder(): List<WorkoutPlan>
}
