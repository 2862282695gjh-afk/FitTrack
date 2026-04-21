package com.fittrack.widget

/**
 * 训练状态枚举
 */
enum class WorkoutStatus {
    REST_DAY,       // 休息日 - 无训练，显示俏皮话
    PENDING,        // 待完成（训练日但未完成）
    COMPLETED,      // 已完成
    OVERDUE         // 训练已逾期
}

/**
 * 小部件数据模型
 * @property status 今日训练状态
 * @property planName 计划名称
 * @property exerciseNames 训练动作名称列表
 * @property weeklyProgress 本周训练进度（已完成天数/计划天数）
 * @property weekStartDate 本周开始日期
 * @property trainingDate 训练日期（如 "3月2日 周日"）
 * @property nextTrainingDay 下次训练日（如 "明天"）
 * @property funPhrase 俏皮话（无训练时显示）
 * @property overdueDays 逾期天数
 * @property currentExerciseIndex 当前显示的动作索引（用于翻滚）
 */
data class WidgetData(
    val status: WorkoutStatus = WorkoutStatus.REST_DAY,
    val planName: String = "",
    val exerciseNames: List<String> = emptyList(),
    val weeklyProgress: Pair<Int, Int> = Pair(0, 0), // (completed, total)
    val weekStartDate: String = "",
    val trainingDate: String = "",
    val nextTrainingDay: String = "",
    val funPhrase: String = "",
    val overdueDays: Int = 0,
    val currentExerciseIndex: Int = 0
) {
    /**
     * 获取状态图标
     */
    fun getStatusEmoji(): String {
        return when (status) {
            WorkoutStatus.REST_DAY -> "😴"
            WorkoutStatus.PENDING -> "⚠️"
            WorkoutStatus.COMPLETED -> "✅"
            WorkoutStatus.OVERDUE -> "😱"
        }
    }

    /**
     * 获取状态文字
     */
    fun getStatusText(): String {
        return when (status) {
            WorkoutStatus.REST_DAY -> "今日休息"
            WorkoutStatus.PENDING -> "待完成"
            WorkoutStatus.COMPLETED -> "已完成"
            WorkoutStatus.OVERDUE -> "已逾期 $overdueDays 天"
        }
    }

    /**
     * 获取当前显示的动作（根据索引）
     */
    fun getCurrentExercise(): String {
        if (exerciseNames.isEmpty()) return ""
        val index = currentExerciseIndex.coerceIn(0, exerciseNames.size - 1)
        return exerciseNames[index]
    }

    /**
     * 获取训练动作预览（最多3个）
     */
    fun getExercisePreview(): String {
        return exerciseNames.take(3).joinToString("、")
    }

    /**
     * 是否有训练数据
     */
    fun hasWorkoutData(): Boolean {
        return planName.isNotEmpty() && exerciseNames.isNotEmpty()
    }

    /**
     * 是否可以翻滚查看动作
     */
    fun canScrollExercises(): Boolean {
        return exerciseNames.size > 1
    }

    /**
     * 获取动作翻滚指示器（如 "1/3"）
     */
    fun getExerciseIndicator(): String {
        if (exerciseNames.isEmpty()) return ""
        val index = currentExerciseIndex.coerceIn(0, exerciseNames.size - 1)
        return "${index + 1}/${exerciseNames.size}"
    }

    /**
     * 获取本周进度百分比
     */
    fun getProgressPercentage(): Float {
        val (completed, total) = weeklyProgress
        return if (total > 0) {
            completed.toFloat() / total.toFloat()
        } else {
            0f
        }
    }
}

/**
 * 空数据
 */
val EmptyWidgetData = WidgetData()
