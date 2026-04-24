package com.fittrack.data.repository

import com.fittrack.data.db.MealRecordDao
import com.fittrack.data.db.NutritionAdviceDao
import com.fittrack.data.db.DailyNutritionSummary
import com.fittrack.data.entity.MealRecord
import com.fittrack.data.entity.NutritionAdvice
import kotlinx.coroutines.flow.Flow

/**
 * 饮食数据仓库
 * 统一管理饮食记录和营养推荐的数据访问
 */
class MealRepository(
    private val mealRecordDao: MealRecordDao,
    private val nutritionAdviceDao: NutritionAdviceDao
) {

    // ==================== 饮食记录 ====================

    fun getAllMealRecords(): Flow<List<MealRecord>> = mealRecordDao.getAllRecords()

    fun getMealRecordsByDate(date: String): Flow<List<MealRecord>> =
        mealRecordDao.getRecordsByDate(date)

    fun getMealRecordsByDateAndType(date: String, mealType: String): Flow<List<MealRecord>> =
        mealRecordDao.getRecordsByDateAndType(date, mealType)

    fun getMealRecordsBetween(startDate: String, endDate: String): Flow<List<MealRecord>> =
        mealRecordDao.getRecordsBetween(startDate, endDate)

    suspend fun getMealRecordById(id: Long): MealRecord? = mealRecordDao.getById(id)

    suspend fun insertMealRecord(record: MealRecord): Long = mealRecordDao.insert(record)

    suspend fun updateMealRecord(record: MealRecord) = mealRecordDao.update(record)

    suspend fun deleteMealRecord(record: MealRecord) = mealRecordDao.delete(record)

    suspend fun deleteMealRecordById(id: Long) = mealRecordDao.deleteById(id)

    // ==================== 营养统计 ====================

    suspend fun getTotalCaloriesByDate(date: String): Double =
        mealRecordDao.getTotalCaloriesByDate(date)

    suspend fun getDailyNutritionSummary(date: String): DailyNutritionSummary =
        mealRecordDao.getDailyNutritionSummary(date)

    // ==================== 营养推荐 ====================

    fun getAllAdvices(): Flow<List<NutritionAdvice>> = nutritionAdviceDao.getAllAdvices()

    fun getAdvicesByDate(date: String): Flow<List<NutritionAdvice>> =
        nutritionAdviceDao.getAdvicesByDate(date)

    suspend fun getAdviceByDateAndType(date: String, mealType: String): NutritionAdvice? =
        nutritionAdviceDao.getAdviceByDateAndType(date, mealType)

    suspend fun getLatestAdviceForDate(date: String): NutritionAdvice? =
        nutritionAdviceDao.getLatestAdviceForDate(date)

    suspend fun insertAdvice(advice: NutritionAdvice): Long = nutritionAdviceDao.insert(advice)

    suspend fun updateAdvice(advice: NutritionAdvice) = nutritionAdviceDao.update(advice)

    suspend fun deleteAdvicesByDate(date: String) = nutritionAdviceDao.deleteByDate(date)

    suspend fun deleteAdviceByDateAndType(date: String, mealType: String) =
        nutritionAdviceDao.deleteByDateAndType(date, mealType)
}
