package com.fittrack.data.db

import androidx.room.*
import com.fittrack.data.entity.MealRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MealRecordDao {

    @Query("SELECT * FROM meal_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<MealRecord>>

    @Query("SELECT * FROM meal_records WHERE date = :date ORDER BY createdAt DESC")
    fun getRecordsByDate(date: String): Flow<List<MealRecord>>

    @Query("SELECT * FROM meal_records WHERE date = :date AND mealType = :mealType ORDER BY createdAt DESC")
    fun getRecordsByDateAndType(date: String, mealType: String): Flow<List<MealRecord>>

    @Query("SELECT * FROM meal_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, createdAt ASC")
    fun getRecordsBetween(startDate: String, endDate: String): Flow<List<MealRecord>>

    @Query("SELECT * FROM meal_records WHERE id = :id")
    suspend fun getById(id: Long): MealRecord?

    @Query("SELECT COALESCE(SUM(totalCalories), 0) FROM meal_records WHERE date = :date")
    suspend fun getTotalCaloriesByDate(date: String): Double

    @Query("SELECT COALESCE(SUM(totalProtein), 0), COALESCE(SUM(totalCarbs), 0), COALESCE(SUM(totalFat), 0), COALESCE(SUM(totalCalories), 0) FROM meal_records WHERE date = :date")
    suspend fun getDailyNutritionSummary(date: String): DailyNutritionSummary

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MealRecord): Long

    @Update
    suspend fun update(record: MealRecord)

    @Delete
    suspend fun delete(record: MealRecord)

    @Query("DELETE FROM meal_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}

/**
 * 每日营养汇总
 */
data class DailyNutritionSummary(
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val totalCalories: Double
)
