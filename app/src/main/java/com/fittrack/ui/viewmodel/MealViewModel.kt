package com.fittrack.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.data.api.NutritionAdviceRequest
import com.fittrack.data.api.NutritionAdvisor
import com.fittrack.data.api.QwenRepository
import com.fittrack.data.api.QwenResult
import com.fittrack.data.entity.MealRecord
import com.fittrack.data.entity.NutritionAdvice
import com.fittrack.data.repository.MealRepository
import com.fittrack.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class NutritionAdviceUiState(
    val isLoading: Boolean = false,
    val advice: NutritionAdvice? = null,
    val error: String? = null
)

class MealViewModel(
    private val mealRepository: MealRepository,
    private val userProfileRepository: UserProfileRepository,
    private val qwenRepository: QwenRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MealViewModel"

        fun getTodayDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun getCurrentMealType(): String {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when {
                hour < 10 -> "breakfast"
                hour < 14 -> "lunch"
                hour < 20 -> "dinner"
                else -> "snack"
            }
        }
    }

    private val nutritionAdvisor = NutritionAdvisor(qwenRepository)

    // 今日日期
    private val _today = MutableStateFlow(getTodayDate())
    val today: StateFlow<String> = _today.asStateFlow()

    // 今日饮食记录
    val todayMealRecords: StateFlow<List<MealRecord>> = _today
        .flatMapLatest { date -> mealRepository.getMealRecordsByDate(date) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 今日营养汇总
    private val _dailySummary = MutableStateFlow(DailySummary())
    val dailySummary: StateFlow<DailySummary> = _dailySummary.asStateFlow()

    // 今日各餐推荐
    private val _advicesByDate = MutableStateFlow<Map<String, NutritionAdvice>>(emptyMap())
    val advicesByDate: StateFlow<Map<String, NutritionAdvice>> = _advicesByDate.asStateFlow()

    // 推荐加载状态
    private val _adviceStates = MutableStateFlow<Map<String, NutritionAdviceUiState>>(emptyMap())
    val adviceStates: StateFlow<Map<String, NutritionAdviceUiState>> = _adviceStates.asStateFlow()

    // 当前饭点（根据当前时间判断）
    val currentMealType: String
        get() = getCurrentMealType()

    init {
        // 监听今日记录变化，更新营养汇总
        viewModelScope.launch {
            todayMealRecords.collect { records ->
                updateDailySummary(records)
            }
        }

        // 刷新今日推荐
        refreshAdvices()
    }

    // ==================== 饮食记录操作 ====================

    fun addMealRecord(
        date: String,
        mealType: String,
        foodsJson: String,
        totalCalories: Double,
        totalProtein: Double,
        totalCarbs: Double,
        totalFat: Double,
        note: String = ""
    ) {
        viewModelScope.launch {
            val record = MealRecord(
                date = date,
                mealType = mealType,
                foodsJson = foodsJson,
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat,
                note = note
            )
            mealRepository.insertMealRecord(record)
        }
    }

    fun updateMealRecord(record: MealRecord) {
        viewModelScope.launch {
            mealRepository.updateMealRecord(record)
        }
    }

    fun deleteMealRecord(record: MealRecord) {
        viewModelScope.launch {
            mealRepository.deleteMealRecord(record)
        }
    }

    // ==================== AI 饮食推荐 ====================

    /**
     * 刷新指定日期的所有推荐
     */
    fun refreshAdvices(date: String? = null) {
        val targetDate = date ?: _today.value
        val mealTypes = listOf("breakfast", "lunch", "dinner", "snack")
        mealTypes.forEach { mealType ->
            loadOrGenerateAdvice(targetDate, mealType)
        }
    }

    /**
     * 请求生成指定餐次的推荐
     */
    fun requestAdvice(date: String, mealType: String) {
        loadOrGenerateAdvice(date, mealType, forceRefresh = true)
    }

    private fun loadOrGenerateAdvice(date: String, mealType: String, forceRefresh: Boolean = false) {
        val key = "$date:$mealType"
        viewModelScope.launch {
            // 先尝试从数据库加载
            if (!forceRefresh) {
                val cached = mealRepository.getAdviceByDateAndType(date, mealType)
                if (cached != null) {
                    _advicesByDate.update { it + (key to cached) }
                    _adviceStates.update { it + (key to NutritionAdviceUiState(advice = cached)) }
                    return@launch
                }
            }

            // 标记加载中
            _adviceStates.update { it + (key to NutritionAdviceUiState(isLoading = true)) }

            // 获取用户档案
            val profile = userProfileRepository.getProfile()
            if (profile == null) {
                _adviceStates.update { it + (key to NutritionAdviceUiState(error = "请先完善个人档案")) }
                return@launch
            }

            // 构建请求
            val todaySummary = _dailySummary.value
            val intensity = determineWorkoutIntensity(date)

            val request = NutritionAdviceRequest(
                date = date,
                mealType = mealType,
                fitnessGoal = profile.fitnessGoal,
                weightKg = profile.weightKg,
                heightCm = profile.heightCm,
                age = profile.age,
                gender = profile.gender,
                alreadyEatenCalories = if (mealType == "breakfast") 0.0 else todaySummary.totalCalories,
                todayWorkoutDuration = 0, // TODO: 从 workout records 获取
                todayWorkoutIntensity = intensity,
                healthIssues = profile.healthIssues
            )

            // 调用 AI 生成推荐
            when (val result = nutritionAdvisor.generateAdvice(request)) {
                is QwenResult.Success -> {
                    val parsed = result.data
                    val advice = NutritionAdvice(
                        date = date,
                        mealType = mealType,
                        goal = profile.fitnessGoal,
                        targetCalories = parsed.targetCalories,
                        targetProtein = parsed.targetProtein,
                        targetCarbs = parsed.targetCarbs,
                        targetFat = parsed.targetFat,
                        adviceJson = serializeFoods(parsed.foods),
                        summary = parsed.summary
                    )
                    mealRepository.insertAdvice(advice)
                    _advicesByDate.update { it + (key to advice) }
                    _adviceStates.update { it + (key to NutritionAdviceUiState(advice = advice)) }
                }
                is QwenResult.Error -> {
                    Log.e(TAG, "生成饮食推荐失败: ${result.message}")
                    _adviceStates.update { it + (key to NutritionAdviceUiState(error = result.message)) }
                }
            }
        }
    }

    // ==================== 内部方法 ====================

    private suspend fun updateDailySummary(records: List<MealRecord>) {
        val summary = if (records.isEmpty()) {
            DailySummary()
        } else {
            mealRepository.getDailyNutritionSummary(_today.value).let {
                DailySummary(
                    totalCalories = it.totalCalories,
                    totalProtein = it.totalProtein,
                    totalCarbs = it.totalCarbs,
                    totalFat = it.totalFat
                )
            }
        }
        _dailySummary.value = summary
    }

    private fun determineWorkoutIntensity(date: String): String {
        // TODO: 查询当日训练记录，根据 duration 和 feeling 判断强度
        // 暂时返回 "rest"，后续接入 workout records
        return "rest"
    }

    private fun serializeFoods(foods: List<com.fittrack.data.api.AdviceFoodItem>): String {
        return try {
            com.google.gson.Gson().toJson(foods)
        } catch (e: Exception) {
            "[]"
        }
    }

    class Factory(
        private val mealRepository: MealRepository,
        private val userProfileRepository: UserProfileRepository,
        private val qwenRepository: QwenRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MealViewModel::class.java)) {
                return MealViewModel(mealRepository, userProfileRepository, qwenRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * 每日营养汇总 UI 数据
 */
data class DailySummary(
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0
)
