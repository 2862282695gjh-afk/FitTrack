package com.fittrack.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.data.api.QwenMessage
import com.fittrack.data.api.QwenRepository
import com.fittrack.data.api.QwenResult
import com.fittrack.data.entity.Exercise
import com.fittrack.data.entity.ExerciseRecord
import com.fittrack.data.entity.WorkoutPlan
import com.fittrack.data.entity.WorkoutRecord
import com.fittrack.data.repository.FitTrackRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class WorkoutSession(
    val planId: Long = 0,
    val planName: String = "",
    val exercises: List<Exercise> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val exerciseRecords: Map<Long, ExerciseRecordData> = emptyMap(),
    val startTime: Long = System.currentTimeMillis()
)

data class ExerciseRecordData(
    val sets: Int = 0,
    val reps: String = "",
    val weights: String = ""
)

class FitTrackViewModel(
    val repository: FitTrackRepository,
    private val qwenRepository: QwenRepository? = null
) : ViewModel() {

    // 所有健身计划
    val allPlans: StateFlow<List<WorkoutPlan>> = repository.getAllPlans()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 所有训练记录
    val allRecords: StateFlow<List<WorkoutRecord>> = repository.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 当前选中的计划
    private val _selectedPlan = MutableStateFlow<WorkoutPlan?>(null)
    val selectedPlan: StateFlow<WorkoutPlan?> = _selectedPlan.asStateFlow()

    // 当前计划的动作列表
    private val _planExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val planExercises: StateFlow<List<Exercise>> = _planExercises.asStateFlow()

    // 当前训练会话
    private val _workoutSession = MutableStateFlow<WorkoutSession?>(null)
    val workoutSession: StateFlow<WorkoutSession?> = _workoutSession.asStateFlow()

    // 本周训练次数
    val weeklyWorkoutCount: StateFlow<Int> = allRecords.map { records ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val weekStart = sdf.format(calendar.time)
        records.count { it.date >= weekStart }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // 本月训练次数
    val monthlyWorkoutCount: StateFlow<Int> = allRecords.map { records ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = sdf.format(calendar.time)
        records.count { it.date >= monthStart }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // 选择计划
    fun selectPlan(planId: Long) {
        viewModelScope.launch {
            val plan = repository.getPlanById(planId)
            _selectedPlan.value = plan
            plan?.let {
                _planExercises.value = repository.getExercisesForPlan(planId)
            }
        }
    }

    // 创建新计划
    fun createPlan(name: String, goal: String, notes: String, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val plan = WorkoutPlan(
                name = name,
                description = notes,
                goal = goal,
                createdAt = System.currentTimeMillis()
            )
            val id = repository.insertPlan(plan)
            onComplete(id)
        }
    }

    // 保存计划（用于 AddPlanScreen）
    fun savePlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            repository.insertPlan(plan)
        }
    }

    // 更新计划
    fun updatePlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            repository.updatePlan(plan)
        }
    }

    // 删除计划
    fun deletePlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            repository.deletePlan(plan)
        }
    }

    // 添加动作到计划
    fun addExercise(
        planId: Long,
        name: String,
        sets: Int,
        reps: Int,
        weight: Double,
        duration: Int,
        category: String = "other"
    ) {
        viewModelScope.launch {
            val exercise = Exercise(
                planId = planId,
                name = name,
                category = category,
                defaultSets = sets,
                defaultReps = reps,
                defaultWeight = weight,
                defaultDuration = duration
            )
            repository.insertExercise(exercise)
            _planExercises.value = repository.getExercisesForPlan(planId)
        }
    }

    // 删除动作
    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
            _selectedPlan.value?.let {
                _planExercises.value = repository.getExercisesForPlan(it.id)
            }
        }
    }

    // 开始训练
    fun startWorkout(plan: WorkoutPlan) {
        viewModelScope.launch {
            // 获取今天的星期几（1-7，周一到周日）
            val calendar = Calendar.getInstance()
            val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            // Calendar.DAY_OF_WEEK 返回 1（周日）到 7（周六）
            // 转换为 1（周一）到 7（周日）
            val convertedDayOfWeek = if (todayDayOfWeek == Calendar.SUNDAY) 7 else todayDayOfWeek - 1

            // 获取计划的所有动作，然后过滤出今天的动作
            val allExercises = repository.getExercisesForPlan(plan.id)
            val todayExercises = allExercises.filter { it.dayOfWeek == convertedDayOfWeek }

            // 如果今天没有安排动作，显示所有动作（兼容旧数据）
            val exercises = if (todayExercises.isEmpty()) allExercises else todayExercises

            val records = exercises.associate { exercise ->
                exercise.id to ExerciseRecordData()
            }
            _workoutSession.value = WorkoutSession(
                planId = plan.id,
                planName = plan.name,
                exercises = exercises,
                currentExerciseIndex = 0,
                exerciseRecords = records
            )
        }
    }

    // 更新动作记录
    fun updateExerciseRecord(exerciseId: Long, sets: Int, reps: String, weights: String) {
        _workoutSession.value?.let { session ->
            val updatedRecords = session.exerciseRecords.toMutableMap()
            updatedRecords[exerciseId] = ExerciseRecordData(sets, reps, weights)
            _workoutSession.value = session.copy(exerciseRecords = updatedRecords)
        }
    }

    // 下一个动作
    fun nextExercise() {
        _workoutSession.value?.let { session ->
            if (session.currentExerciseIndex < session.exercises.size - 1) {
                _workoutSession.value = session.copy(currentExerciseIndex = session.currentExerciseIndex + 1)
            }
        }
    }

    // 上一个动作
    fun previousExercise() {
        _workoutSession.value?.let { session ->
            if (session.currentExerciseIndex > 0) {
                _workoutSession.value = session.copy(currentExerciseIndex = session.currentExerciseIndex - 1)
            }
        }
    }

    // 完成训练
    fun finishWorkout(
        rating: Int,
        notes: String,
        sleepQuality: Int = 3,
        appetite: Int = 3,
        energyLevel: Int = 3,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _workoutSession.value?.let { session ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = sdf.format(Date())

                // 获取最近训练记录用于压力分析
                val recentRecords = try {
                    repository.getAllRecords()
                        .first()
                        .take(7)
                } catch (e: Exception) {
                    emptyList()
                }

                // 构建当前训练的动作记录列表
                val currentExerciseRecords = mutableListOf<ExerciseRecord>()
                session.exercises.forEach { exercise ->
                    val recordData = session.exerciseRecords[exercise.id] ?: ExerciseRecordData()
                    val exerciseRecord = ExerciseRecord(
                        recordId = 0L, // 临时ID，插入后会更新
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        sets = recordData.sets,
                        reps = recordData.reps,
                        weights = recordData.weights
                    )
                    currentExerciseRecords.add(exerciseRecord)
                }

                // 计算压力水平
                val pressureAnalysis = try {
                    com.fittrack.data.analyzer.PressureAnalyzer.analyzePressure(
                        WorkoutRecord(
                            planId = session.planId,
                            date = today,
                            startTime = session.startTime,
                            endTime = System.currentTimeMillis(),
                            totalDuration = ((System.currentTimeMillis() - session.startTime) / 60000).toInt(),
                            feeling = rating,
                            sleepQuality = sleepQuality,
                            appetite = appetite,
                            energyLevel = energyLevel
                        ),
                        recentRecords,
                        currentExerciseRecords
                    )
                } catch (e: Exception) {
                    null
                }

                // 生成 AI 训练总结
                val aiSummary = try {
                    generateWorkoutSummary(
                        session = session,
                        exerciseRecords = currentExerciseRecords,
                        rating = rating,
                        sleepQuality = sleepQuality,
                        appetite = appetite,
                        energyLevel = energyLevel,
                        duration = ((System.currentTimeMillis() - session.startTime) / 60000).toInt(),
                        pressureAnalysis = pressureAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("FitTrackViewModel", "生成训练总结失败", e)
                    ""
                }

                // 创建训练记录
                val workoutRecord = WorkoutRecord(
                    planId = session.planId,
                    date = today,
                    startTime = session.startTime,
                    endTime = System.currentTimeMillis(),
                    totalDuration = ((System.currentTimeMillis() - session.startTime) / 60000).toInt(),
                    feeling = rating,
                    notes = notes,
                    sleepQuality = sleepQuality,
                    appetite = appetite,
                    energyLevel = energyLevel,
                    metabolicPressure = pressureAnalysis?.metabolicPressure ?: 0,
                    mentalPressure = pressureAnalysis?.mentalPressure ?: 0,
                    isDeload = pressureAnalysis?.needsDeload ?: false,
                    aiSummary = aiSummary
                )
                val recordId = repository.insertWorkoutRecord(workoutRecord)

                // 保存每个动作的记录
                currentExerciseRecords.forEach { exerciseRecord ->
                    val recordWithId = exerciseRecord.copy(recordId = recordId)
                    repository.insertExerciseRecord(recordWithId)
                }

                _workoutSession.value = null
                onComplete()
            }
        }
    }

    /**
     * 生成训练总结
     */
    private suspend fun generateWorkoutSummary(
        session: WorkoutSession,
        exerciseRecords: List<ExerciseRecord>,
        rating: Int,
        sleepQuality: Int,
        appetite: Int,
        energyLevel: Int,
        duration: Int,
        pressureAnalysis: com.fittrack.data.analyzer.PressureAnalysis?
    ): String {
        if (qwenRepository == null) return ""

        val prompt = buildWorkoutSummaryPrompt(
            session = session,
            exerciseRecords = exerciseRecords,
            rating = rating,
            sleepQuality = sleepQuality,
            appetite = appetite,
            energyLevel = energyLevel,
            duration = duration,
            pressureAnalysis = pressureAnalysis
        )

        val messages = listOf(
            QwenMessage(role = "system", content = """
                你是一位专业的健身教练。请根据用户刚才完成的训练情况，生成一份简洁的训练总结。

                请使用 Markdown 格式，包含以下几个部分：
                1. **训练表现**：评价本次训练的亮点
                2. **改进建议**：下次训练可以改进的地方
                3. **恢复建议**：饮食、休息等方面的建议

                注意：
                - 语言简洁友好，每部分 2-3 条要点
                - 使用中文回复
                - 不要过于啰嗦，控制在 150 字以内
            """.trimIndent()),
            QwenMessage(role = "user", content = prompt)
        )

        return when (val result = qwenRepository.chat(messages)) {
            is QwenResult.Success -> result.data
            is QwenResult.Error -> ""
        }
    }

    /**
     * 构建训练总结提示
     */
    private fun buildWorkoutSummaryPrompt(
        session: WorkoutSession,
        exerciseRecords: List<ExerciseRecord>,
        rating: Int,
        sleepQuality: Int,
        appetite: Int,
        energyLevel: Int,
        duration: Int,
        pressureAnalysis: com.fittrack.data.analyzer.PressureAnalysis?
    ): String {
        val sb = StringBuilder()

        sb.append("我刚完成了训练，请帮我生成总结。\n\n")
        sb.append("## 训练概况\n")
        sb.append("- 计划名称：${session.planName}\n")
        sb.append("- 训练时长：${duration}分钟\n")
        sb.append("- 训练感受：${rating}/5\n")
        sb.append("- 睡眠质量：${sleepQuality}/5\n")
        sb.append("- 食欲：${appetite}/5\n")
        sb.append("- 能量水平：${energyLevel}/5\n\n")

        sb.append("## 完成的动作\n")
        exerciseRecords.forEach { record ->
            if (record.sets > 0) {
                sb.append("- ${record.exerciseName}: ${record.sets}组")
                if (record.reps.isNotBlank()) sb.append(" x ${record.reps}次")
                if (record.weights.isNotBlank()) sb.append(" @ ${record.weights}kg")
                sb.append("\n")
            }
        }

        if (pressureAnalysis != null) {
            sb.append("\n## 身体状态分析\n")
            sb.append("- 代谢压力：${pressureAnalysis.metabolicPressure}/100\n")
            sb.append("- 精神压力：${pressureAnalysis.mentalPressure}/100\n")
            if (pressureAnalysis.needsDeload) {
                sb.append("- 建议：身体需要恢复，建议下次减载训练\n")
            }
        }

        return sb.toString()
    }

    // 取消训练
    fun cancelWorkout() {
        _workoutSession.value = null
    }

    // 获取计划的训练记录
    fun getRecordsForPlan(planId: Long): Flow<List<WorkoutRecord>> {
        return repository.getRecordsForPlan(planId)
    }

    class Factory(
        private val repository: FitTrackRepository,
        private val qwenRepository: QwenRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FitTrackViewModel::class.java)) {
                return FitTrackViewModel(repository, qwenRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
