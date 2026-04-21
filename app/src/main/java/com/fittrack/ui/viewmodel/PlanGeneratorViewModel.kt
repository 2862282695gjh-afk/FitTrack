package com.fittrack.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.data.api.*
import com.fittrack.data.entity.Exercise
import com.fittrack.data.entity.WorkoutPlan
import com.fittrack.data.repository.FitTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 计划生成状态
 */
sealed class PlanGenerationState {
    object Idle : PlanGenerationState()
    object Loading : PlanGenerationState()
    data class Success(val plan: ParsedWorkoutPlan) : PlanGenerationState()
    data class Error(val message: String) : PlanGenerationState()
}

/**
 * 智能训练计划生成 ViewModel
 */
class PlanGeneratorViewModel(
    private val qwenRepository: QwenRepository,
    private val fitTrackRepository: FitTrackRepository
) : ViewModel() {

    // 生成状态
    private val _generationState = MutableStateFlow<PlanGenerationState>(PlanGenerationState.Idle)
    val generationState: StateFlow<PlanGenerationState> = _generationState.asStateFlow()

    // 用户选择的训练目标
    private val _selectedGoal = MutableStateFlow("综合健身")
    val selectedGoal: StateFlow<String> = _selectedGoal.asStateFlow()

    // 每周训练天数
    private val _weeklyDays = MutableStateFlow(4)
    val weeklyDays: StateFlow<Int> = _weeklyDays.asStateFlow()

    // 每次训练时长（分钟）
    private val _sessionDuration = MutableStateFlow(60)
    val sessionDuration: StateFlow<Int> = _sessionDuration.asStateFlow()

    // 健身经验
    private val _experience = MutableStateFlow("intermediate")
    val experience: StateFlow<String> = _experience.asStateFlow()

    // 可用目标列表
    val goals = listOf(
        "综合健身" to "general",
        "减脂塑形" to "fat_loss",
        "增肌增重" to "muscle_gain",
        "力量提升" to "strength",
        "耐力训练" to "endurance"
    )

    // 可用经验等级
    val experienceLevels = listOf(
        "初学者（<6个月）" to "beginner",
        "中级（6个月-2年）" to "intermediate",
        "高级（>2年）" to "advanced"
    )

    /**
     * 更新训练目标
     */
    fun updateGoal(goal: String) {
        _selectedGoal.value = goal
    }

    /**
     * 更新每周训练天数
     */
    fun updateWeeklyDays(days: Int) {
        _weeklyDays.value = days.coerceIn(1, 7)
    }

    /**
     * 更新训练时长
     */
    fun updateSessionDuration(duration: Int) {
        _sessionDuration.value = duration.coerceIn(20, 120)
    }

    /**
     * 更新经验等级
     */
    fun updateExperience(exp: String) {
        _experience.value = exp
    }

    /**
     * 生成训练计划
     */
    fun generatePlan() {
        viewModelScope.launch {
            try {
                _generationState.value = PlanGenerationState.Loading

                // 构建用户数据
                val userStats = UserBodyStats(
                    height = 170.0, // 可以从用户档案获取
                    weight = 65.0,
                    age = 25,
                    gender = "男",
                    goal = _selectedGoal.value,
                    experience = _experience.value,
                    availableDays = _weeklyDays.value,
                    sessionDuration = _sessionDuration.value
                )

                // 构建一个基本的体态分析（如果没有照片分析）
                val bodyAnalysis = BodyAnalysisResult(
                    postureScore = 70,
                    bodyFatPercentage = 20.0,
                    muscleBalance = "中等",
                    issues = emptyList(),
                    suggestions = emptyList()
                )

                when (val result = qwenRepository.generateWorkoutPlan(bodyAnalysis, userStats)) {
                    is QwenResult.Success -> {
                        val plan = result.data
                        val totalExercises = plan.weeklySchedule.sumOf { it.exercises.size }
                        Log.d("PlanGeneratorVM", "计划生成成功: ${plan.name}, 训练日: ${plan.weeklySchedule.size}, 总动作: $totalExercises")

                        if (totalExercises == 0) {
                            Log.e("PlanGeneratorVM", "警告: 生成的计划没有训练动作！")
                            _generationState.value = PlanGenerationState.Error(
                                "AI 返回的计划没有包含训练动作，请重试。\n原始响应: ${plan.rawResponse.take(200)}..."
                            )
                        } else {
                            _generationState.value = PlanGenerationState.Success(plan)
                        }
                    }
                    is QwenResult.Error -> {
                        _generationState.value = PlanGenerationState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _generationState.value = PlanGenerationState.Error("生成计划时发生错误: ${e.message}")
            }
        }
    }

    /**
     * 保存计划到数据库
     */
    fun savePlan(plan: ParsedWorkoutPlan, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            // 创建 WorkoutPlan
            val workoutPlan = WorkoutPlan(
                name = plan.name,
                description = plan.description,
                goal = mapGoalToInternal(plan.goal),
                reminderDays = "1,3,5",
                reminderTime = "08:00"
            )

            // 保存计划
            val planId = fitTrackRepository.insertPlan(workoutPlan)

            // 保存动作（按训练日分组）
            plan.weeklySchedule.forEach { dailyWorkout ->
                dailyWorkout.exercises.forEachIndexed { index, plannedExercise ->
                    // 解析重量：从字符串中提取数字，如 "60kg" -> 60.0
                    val weightValue = parseWeight(plannedExercise.weight)

                    val exercise = Exercise(
                        planId = planId,
                        name = plannedExercise.name,
                        category = plannedExercise.category,
                        defaultSets = plannedExercise.sets,
                        defaultReps = plannedExercise.reps.toIntOrNull() ?: 10,
                        defaultWeight = weightValue,
                        notes = plannedExercise.notes,
                        dayOfWeek = dailyWorkout.dayOfWeek, // 保存训练日信息
                        orderIndex = index
                    )
                    fitTrackRepository.insertExercise(exercise)
                }
            }

            onComplete(planId)
        }
    }

    /**
     * 映射目标到内部格式
     */
    private fun mapGoalToInternal(goal: String): String {
        return when {
            goal.contains("减脂") || goal.contains("塑形") -> "fat_loss"
            goal.contains("增肌") || goal.contains("增重") -> "muscle_gain"
            goal.contains("力量") -> "strength"
            goal.contains("耐力") -> "endurance"
            else -> "general"
        }
    }

    /**
     * 解析重量字符串
     * 例如："60kg" -> 60.0, "自重" -> 0.0
     */
    private fun parseWeight(weightStr: String): Double {
        if (weightStr.isBlank()) return 0.0
        if (weightStr.contains("自重") || weightStr.contains("bodyweight", ignoreCase = true)) {
            return 0.0
        }

        // 提取数字部分
        val regex = """(\d+\.?\d*)""".toRegex()
        val match = regex.find(weightStr)
        return match?.value?.toDoubleOrNull() ?: 0.0
    }

    /**
     * 重置状态
     */
    fun reset() {
        _generationState.value = PlanGenerationState.Idle
    }

    companion object {
        fun Factory(
            qwenRepository: QwenRepository,
            fitTrackRepository: FitTrackRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlanGeneratorViewModel(qwenRepository, fitTrackRepository) as T
            }
        }
    }
}
