package com.fittrack.data.db

import androidx.room.*
import com.fittrack.data.entity.NutritionAdvice
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionAdviceDao {

    @Query("SELECT * FROM nutrition_advices ORDER BY createdAt DESC")
    fun getAllAdvices(): Flow<List<NutritionAdvice>>

    @Query("SELECT * FROM nutrition_advices WHERE date = :date ORDER BY mealType ASC")
    fun getAdvicesByDate(date: String): Flow<List<NutritionAdvice>>

    @Query("SELECT * FROM nutrition_advices WHERE date = :date AND mealType = :mealType ORDER BY createdAt DESC LIMIT 1")
    suspend fun getAdviceByDateAndType(date: String, mealType: String): NutritionAdvice?

    @Query("SELECT * FROM nutrition_advices WHERE date = :date ORDER BY mealType ASC LIMIT 1")
    suspend fun getLatestAdviceForDate(date: String): NutritionAdvice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(advice: NutritionAdvice): Long

    @Update
    suspend fun update(advice: NutritionAdvice)

    @Query("DELETE FROM nutrition_advices WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM nutrition_advices WHERE date = :date AND mealType = :mealType")
    suspend fun deleteByDateAndType(date: String, mealType: String)
}
