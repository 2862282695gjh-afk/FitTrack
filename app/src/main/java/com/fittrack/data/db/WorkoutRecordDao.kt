package com.fittrack.data.db

import kotlinx.coroutines.flow.Flow
import androidx.room.*
import com.fittrack.data.entity.WorkoutRecord

@Dao
interface WorkoutRecordDao {

    @Query("SELECT * FROM workout_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<WorkoutRecord>>

    @Query("SELECT * FROM workout_records WHERE planId = :planId ORDER BY date DESC")
    fun getRecordsByPlan(planId: Long): Flow<List<WorkoutRecord>>

    @Query("SELECT * FROM workout_records WHERE date = :date ORDER BY date DESC")
    fun getRecordsByDate(date: String): Flow<List<WorkoutRecord>>

    @Query("SELECT * FROM workout_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getRecordsBetween(startDate: String, endDate: String): Flow<List<WorkoutRecord>>

    @Query("SELECT * FROM workout_records WHERE id = :recordId")
    suspend fun getRecordById(recordId: Long): WorkoutRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: WorkoutRecord): Long

    @Update
    suspend fun updateRecord(record: WorkoutRecord)

    @Delete
    suspend fun deleteRecord(record: WorkoutRecord)

    @Query("SELECT COUNT(*) FROM workout_records WHERE date >= :startDate")
    fun getWorkoutCountSince(startDate: String): Flow<Int>

    @Query("SELECT SUM(totalDuration) FROM workout_records WHERE date >= :startDate")
    fun getTotalDurationSince(startDate: String): Flow<Int?>

    @Query("SELECT * FROM workout_records WHERE planId = :planId ORDER BY date DESC")
    suspend fun getRecordsByPlanOnce(planId: Long): List<WorkoutRecord>

    @Query("DELETE FROM workout_records WHERE planId = :planId")
    suspend fun deleteRecordsByPlan(planId: Long)
}
