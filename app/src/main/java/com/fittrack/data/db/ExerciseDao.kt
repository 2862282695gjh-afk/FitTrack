package com.fittrack.data.db

import kotlinx.coroutines.flow.Flow
import androidx.room.*
import com.fittrack.data.entity.Exercise

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE planId = :planId ORDER BY orderIndex ASC")
    fun getExercisesByPlan(planId: Long): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE planId = :planId ORDER BY orderIndex ASC")
    suspend fun getExercisesForPlan(planId: Long): List<Exercise>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: Long): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("DELETE FROM exercises WHERE planId = :planId")
    suspend fun deleteExercisesByPlan(planId: Long)
}
