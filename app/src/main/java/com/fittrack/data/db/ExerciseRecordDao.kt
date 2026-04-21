package com.fittrack.data.db

import kotlinx.coroutines.flow.Flow
import androidx.room.*
import com.fittrack.data.entity.ExerciseRecord

@Dao
interface ExerciseRecordDao {

    @Query("SELECT * FROM exercise_records WHERE recordId = :recordId ORDER BY id ASC")
    fun getExerciseRecordsByWorkout(recordId: Long): Flow<List<ExerciseRecord>>

    @Query("SELECT * FROM exercise_records WHERE exerciseId = :exerciseId ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentRecordsForExercise(exerciseId: Long, limit: Int): List<ExerciseRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseRecord(record: ExerciseRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseRecords(records: List<ExerciseRecord>)

    @Update
    suspend fun updateExerciseRecord(record: ExerciseRecord)

    @Delete
    suspend fun deleteExerciseRecord(record: ExerciseRecord)

    @Query("DELETE FROM exercise_records WHERE recordId = :recordId")
    suspend fun deleteExerciseRecordsByWorkout(recordId: Long)
}
