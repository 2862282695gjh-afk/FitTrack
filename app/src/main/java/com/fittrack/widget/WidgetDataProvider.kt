package com.fittrack.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fittrack.data.db.ExerciseDao
import com.fittrack.data.db.FitTrackDatabase
import com.fittrack.data.db.WorkoutPlanDao
import com.fittrack.data.db.WorkoutScheduleDao
import com.fittrack.data.db.WorkoutRecordDao
import com.fittrack.data.entity.WorkoutSchedule
import com.fittrack.data.repository.FunPhrasesRepository
import com.fittrack.data.scheduler.WorkoutScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_data")

/**
 * 小部件数据提供器
 * 负责从数据库获取数据并转换为小部件所需格式
 * 支持日历绑定、逾期检测和俏皮话显示
 */
class WidgetDataProvider(private val context: Context) {

    private val database: FitTrackDatabase
    private val workoutPlanDao: WorkoutPlanDao
    private val exerciseDao: ExerciseDao
    private val workoutRecordDao: WorkoutRecordDao
    private val workoutScheduleDao: WorkoutScheduleDao
    private val scheduler: WorkoutScheduler

    init {
        try {
            database = FitTrackDatabase.getDatabase(context)
            workoutPlanDao = database.workoutPlanDao()
            exerciseDao = database.exerciseDao()
            workoutRecordDao = database.workoutRecordDao()
            workoutScheduleDao = database.workoutScheduleDao()
            scheduler = WorkoutScheduler(context)
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize WidgetDataProvider", e)
        }
    }

    /**
     * DataStore keys
     */
    private object Keys {
        val CACHED_STATUS = intPreferencesKey("cached_status")
        val CACHED_PLAN_NAME = stringPreferencesKey("cached_plan_name")
        val CACHED_EXERCISES = stringPreferencesKey("cached_exercises")
        val CACHED_WEEKLY_COMPLETED = intPreferencesKey("cached_weekly_completed")
        val CACHED_WEEKLY_TOTAL = intPreferencesKey("cached_weekly_total")
        val CACHED_WEEK_START = stringPreferencesKey("cached_week_start")
        val CACHED_TRAINING_DATE = stringPreferencesKey("cached_training_date")
        val CACHED_NEXT_TRAINING = stringPreferencesKey("cached_next_training")
        val CACHED_FUN_PHRASE = stringPreferencesKey("cached_fun_phrase")
        val CACHED_OVERDUE_DAYS = intPreferencesKey("cached_overdue_days")
        val CACHED_EXERCISE_INDEX = intPreferencesKey("cached_exercise_index")
        val LAST_UPDATE_TIME = stringPreferencesKey("last_update_time")
    }

    /**
     * 获取小部件数据
     * 优先使用缓存，缓存失效时从数据库重新获取
     */
    suspend fun getWidgetData(): WidgetData {
        // 先尝试从缓存读取
        val cachedData = getCachedData()

        // 检查缓存是否过期（15分钟）
        val currentTime = System.currentTimeMillis()
        val lastUpdateTime = cachedData?.lastUpdateTime ?: 0
        val isCacheValid = currentTime - lastUpdateTime < 15 * 60 * 1000

        if (isCacheValid && cachedData != null) {
            return cachedData.widgetData
        }

        // 缓存过期或无缓存，从数据库获取
        val freshData = fetchFreshData()
        saveToCache(freshData)
        return freshData
    }

    /**
     * 从数据库获取最新数据
     */
    private suspend fun fetchFreshData(): WidgetData {
        // 先从缓存读取当前的索引，保持用户的滑动状态
        val cachedData = getCachedData()
        val savedIndex = cachedData?.widgetData?.currentExerciseIndex ?: 0

        val today = getTodayDateString()
        val weekStart = getWeekStartDate()
        val weekEnd = getWeekEndDate()

        // 检查并标记逾期训练
        scheduler.checkAndMarkOverdue()

        // 检查是否有逾期训练
        val hasOverdue = scheduler.hasOverdueTraining()
        val overdueSchedules = workoutScheduleDao.getOverdueSchedules().first()

        // 优先显示逾期训练
        if (hasOverdue && overdueSchedules.isNotEmpty()) {
            val overdueSchedule = overdueSchedules.first()
            val overdueDays = calculateOverdueDays(overdueSchedule.originalScheduledDate)

            // 获取逾期训练的动作
            val exercises = exerciseDao
                .getExercisesByPlan(overdueSchedule.planId)
                .first()
                .map { it.name }

            // 限制索引不超过动作数量
            val validIndex = if (exercises.isNotEmpty()) savedIndex.coerceIn(0, exercises.size - 1) else 0

            val phrase = FunPhrasesRepository.getRandomOverduePhrase()

            return WidgetData(
                status = WorkoutStatus.OVERDUE,
                planName = getPlanName(overdueSchedule.planId),
                exerciseNames = exercises,
                weeklyProgress = getWeeklyProgress(weekStart, weekEnd),
                weekStartDate = weekStart,
                trainingDate = formatTrainingDate(),
                nextTrainingDay = "",
                funPhrase = phrase,
                overdueDays = overdueDays,
                currentExerciseIndex = validIndex
            )
        }

        // 检查今天是否有训练
        val hasTrainingToday = scheduler.hasTrainingToday()

        if (!hasTrainingToday) {
            // 没有训练，显示俏皮话
            val phrase = FunPhrasesRepository.getRandomRestDayPhrase()

            return WidgetData(
                status = WorkoutStatus.REST_DAY,
                planName = "",
                exerciseNames = emptyList(),
                weeklyProgress = getWeeklyProgress(weekStart, weekEnd),
                weekStartDate = weekStart,
                trainingDate = formatTrainingDate(),
                nextTrainingDay = "",
                funPhrase = phrase,
                overdueDays = 0,
                currentExerciseIndex = 0
            )
        }

        // 有训练，获取今日训练日程
        val todaySchedules = scheduler.getTodaySchedule()
        if (todaySchedules.isEmpty()) {
            // 异常情况，显示休息日
            val phrase = FunPhrasesRepository.getRandomRestDayPhrase()

            return WidgetData(
                status = WorkoutStatus.REST_DAY,
                planName = "",
                exerciseNames = emptyList(),
                weeklyProgress = getWeeklyProgress(weekStart, weekEnd),
                weekStartDate = weekStart,
                trainingDate = formatTrainingDate(),
                nextTrainingDay = "",
                funPhrase = phrase,
                overdueDays = 0,
                currentExerciseIndex = 0
            )
        }

        val todaySchedule = todaySchedules.first()

        // 判断今日训练状态
        val todayRecords = workoutRecordDao
            .getRecordsBetween(weekStart, weekEnd)
            .first()
            .filter { it.date == today }

        val status = if (todayRecords.isNotEmpty()) {
            WorkoutStatus.COMPLETED
        } else {
            WorkoutStatus.PENDING
        }

        // 获取今日动作
        val exercises = exerciseDao
            .getExercisesByPlan(todaySchedule.planId)
            .first()
            .map { it.name }

        val planName = getPlanName(todaySchedule.planId)

        // 如果是待完成状态，添加激励俏皮话
        val phrase = if (status == WorkoutStatus.PENDING) {
            FunPhrasesRepository.getRandomMotivationalPhrase()
        } else {
            ""
        }

        // 限制索引不超过动作数量
        val validIndex = if (exercises.isNotEmpty()) savedIndex.coerceIn(0, exercises.size - 1) else 0

        // 计算下次训练日
        val nextTrainingDay = findNextTrainingDay()

        return WidgetData(
            status = status,
            planName = planName,
            exerciseNames = exercises,
            weeklyProgress = getWeeklyProgress(weekStart, weekEnd),
            weekStartDate = weekStart,
            trainingDate = formatTrainingDate(),
            nextTrainingDay = nextTrainingDay,
            funPhrase = phrase,
            overdueDays = 0,
            currentExerciseIndex = validIndex
        )
    }

    /**
     * 获取本周训练进度
     */
    private suspend fun getWeeklyProgress(weekStart: String, weekEnd: String): Pair<Int, Int> {
        val weeklyRecords = workoutRecordDao
            .getRecordsBetween(weekStart, weekEnd)
            .first()

        val completed = weeklyRecords.size

        // 从日程表中获取本周计划训练次数
        val weeklySchedules = workoutScheduleDao
            .getSchedulesBetween(weekStart, weekEnd)
            .first()
            .filter { it.status != "completed" }

        val total = weeklySchedules.size

        return Pair(completed, total)
    }

    /**
     * 获取计划名称
     */
    private suspend fun getPlanName(planId: Long): String {
        val plan = workoutPlanDao.getPlanById(planId)
        return plan?.name ?: ""
    }

    /**
     * 计算逾期天数
     */
    private fun calculateOverdueDays(originalDate: String): Int {
        val today = Date()
        val original = parseDate(originalDate)

        val diff = today.time - original.time
        val days = (diff / (24 * 60 * 60 * 1000)).toInt()

        return maxOf(0, days)
    }

    /**
     * 解析日期字符串
     */
    private fun parseDate(dateStr: String): Date {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.parse(dateStr) ?: Date()
    }

    /**
     * 从缓存读取数据
     */
    private suspend fun getCachedData(): CachedWidgetData? {
        val preferences = context.widgetDataStore.data.first()
        val lastUpdateTime = preferences[Keys.LAST_UPDATE_TIME]?.toLongOrNull() ?: return null

        return CachedWidgetData(
            widgetData = WidgetData(
                status = WorkoutStatus.entries[preferences[Keys.CACHED_STATUS] ?: 0],
                planName = preferences[Keys.CACHED_PLAN_NAME] ?: "",
                exerciseNames = (preferences[Keys.CACHED_EXERCISES] ?: "").split(",").filter { it.isNotEmpty() },
                weeklyProgress = Pair(
                    preferences[Keys.CACHED_WEEKLY_COMPLETED] ?: 0,
                    preferences[Keys.CACHED_WEEKLY_TOTAL] ?: 0
                ),
                weekStartDate = preferences[Keys.CACHED_WEEK_START] ?: "",
                trainingDate = preferences[Keys.CACHED_TRAINING_DATE] ?: "",
                nextTrainingDay = preferences[Keys.CACHED_NEXT_TRAINING] ?: "",
                funPhrase = preferences[Keys.CACHED_FUN_PHRASE] ?: "",
                overdueDays = preferences[Keys.CACHED_OVERDUE_DAYS] ?: 0,
                currentExerciseIndex = preferences[Keys.CACHED_EXERCISE_INDEX] ?: 0
            ),
            lastUpdateTime = lastUpdateTime
        )
    }

    /**
     * 保存到缓存
     */
    private suspend fun saveToCache(data: WidgetData) {
        context.widgetDataStore.edit { preferences ->
            preferences[Keys.CACHED_STATUS] = data.status.ordinal
            preferences[Keys.CACHED_PLAN_NAME] = data.planName
            preferences[Keys.CACHED_EXERCISES] = data.exerciseNames.joinToString(",")
            preferences[Keys.CACHED_WEEKLY_COMPLETED] = data.weeklyProgress.first
            preferences[Keys.CACHED_WEEKLY_TOTAL] = data.weeklyProgress.second
            preferences[Keys.CACHED_WEEK_START] = data.weekStartDate
            preferences[Keys.CACHED_TRAINING_DATE] = data.trainingDate
            preferences[Keys.CACHED_NEXT_TRAINING] = data.nextTrainingDay
            preferences[Keys.CACHED_FUN_PHRASE] = data.funPhrase
            preferences[Keys.CACHED_OVERDUE_DAYS] = data.overdueDays
            preferences[Keys.CACHED_EXERCISE_INDEX] = data.currentExerciseIndex
            preferences[Keys.LAST_UPDATE_TIME] = System.currentTimeMillis().toString()
        }
    }

    /**
     * 获取今天的日期字符串（yyyy-MM-dd）
     */
    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * 获取本周开始日期（周一）
     */
    private fun getWeekStartDate(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    /**
     * 获取本周结束日期（周日）
     */
    private fun getWeekEndDate(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    /**
     * 格式化训练日期（如 "3月2日 周日"）
     */
    private fun formatTrainingDate(): String {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> ""
        }
        return "${month}月${day}日 $dayOfWeek"
    }

    /**
     * 查找下次训练日
     */
    private suspend fun findNextTrainingDay(): String {
        val nextSchedule = scheduler.getNextTrainingSchedule()

        if (nextSchedule == null) {
            return ""
        }

        val today = Date()
        val nextDate = parseDate(nextSchedule.scheduledDate)

        val diff = nextDate.time - today.time
        val days = (diff / (24 * 60 * 60 * 1000)).toInt()

        return when (days) {
            1 -> "明天"
            2 -> "后天"
            in 3..7 -> getDayName(nextDate)
            else -> ""
        }
    }

    /**
     * 获取日期对应的周几名称
     */
    private fun getDayName(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> ""
        }
    }

    /**
     * 强制刷新数据
     */
    suspend fun refresh(): WidgetData {
        val freshData = fetchFreshData()
        saveToCache(freshData)
        return freshData
    }

    /**
     * 更新当前动作索引
     */
    suspend fun updateExerciseIndex(index: Int) {
        val cachedData = getCachedData()
        if (cachedData != null) {
            val updatedData = cachedData.widgetData.copy(currentExerciseIndex = index)
            saveToCache(updatedData)
        }
    }
}

/**
 * 缓存的数据结构
 */
private data class CachedWidgetData(
    val widgetData: WidgetData,
    val lastUpdateTime: Long
)
