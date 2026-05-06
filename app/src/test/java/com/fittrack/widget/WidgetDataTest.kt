package com.fittrack.widget

import org.junit.Assert.*
import org.junit.Test

/**
 * WidgetData 单元测试
 * 验证数据模型的边界条件和逻辑正确性
 *
 * 作者：小花 (kohana) — 品控测试
 */
class WidgetDataTest {

    // ==================== getCurrentExercise ====================

    @Test
    fun `getCurrentExercise 空列表返回空字符串`() {
        val data = WidgetData(exerciseNames = emptyList(), currentExerciseIndex = 0)
        assertEquals("", data.getCurrentExercise())
    }

    @Test
    fun `getCurrentExercise 正常索引返回对应动作`() {
        val data = WidgetData(
            exerciseNames = listOf("深蹲", "卧推", "硬拉"),
            currentExerciseIndex = 1
        )
        assertEquals("卧推", data.getCurrentExercise())
    }

    @Test
    fun `getCurrentExercise 负索引被钳制到第一个`() {
        val data = WidgetData(
            exerciseNames = listOf("深蹲", "卧推"),
            currentExerciseIndex = -5
        )
        assertEquals("深蹲", data.getCurrentExercise())
    }

    @Test
    fun `getCurrentExercise 超大索引被钳制到最后一个`() {
        val data = WidgetData(
            exerciseNames = listOf("深蹲", "卧推"),
            currentExerciseIndex = 999
        )
        assertEquals("卧推", data.getCurrentExercise())
    }

    @Test
    fun `getCurrentExercise 索引等于列表大小被钳制`() {
        val data = WidgetData(
            exerciseNames = listOf("深蹲"),
            currentExerciseIndex = 1 // size == 1, 有效范围 [0, 0]
        )
        assertEquals("深蹲", data.getCurrentExercise())
    }

    // ==================== getExercisePreview ====================

    @Test
    fun `getExercisePreview 空列表返回空字符串`() {
        val data = WidgetData(exerciseNames = emptyList())
        assertEquals("", data.getExercisePreview())
    }

    @Test
    fun `getExercisePreview 最多显示3个动作`() {
        val data = WidgetData(
            exerciseNames = listOf("深蹲", "卧推", "硬拉", "划船", "弯举")
        )
        val preview = data.getExercisePreview()
        assertEquals("深蹲、卧推、硬拉", preview)
    }

    @Test
    fun `getExercisePreview 少于3个全部显示`() {
        val data = WidgetData(exerciseNames = listOf("深蹲", "卧推"))
        assertEquals("深蹲、卧推", data.getExercisePreview())
    }

    // ==================== canScrollExercises ====================

    @Test
    fun `canScrollExercises 单个动作不可翻滚`() {
        val data = WidgetData(exerciseNames = listOf("深蹲"))
        assertFalse(data.canScrollExercises())
    }

    @Test
    fun `canScrollExercises 多个动作可翻滚`() {
        val data = WidgetData(exerciseNames = listOf("深蹲", "卧推"))
        assertTrue(data.canScrollExercises())
    }

    @Test
    fun `canScrollExercises 空列表不可翻滚`() {
        val data = WidgetData(exerciseNames = emptyList())
        assertFalse(data.canScrollExercises())
    }

    // ==================== getExerciseIndicator ====================

    @Test
    fun `getExerciseIndicator 空列表返回空字符串`() {
        val data = WidgetData(exerciseNames = emptyList())
        assertEquals("", data.getExerciseIndicator())
    }

    @Test
    fun `getExerciseIndicator 正确显示当前位置`() {
        val data = WidgetData(
            exerciseNames = listOf("深蹲", "卧推", "硬拉"),
            currentExerciseIndex = 1
        )
        assertEquals("2/3", data.getExerciseIndicator())
    }

    @Test
    fun `getExerciseIndicator 第一个动作为 1_N`() {
        val data = WidgetData(
            exerciseNames = listOf("深蹲", "卧推", "硬拉"),
            currentExerciseIndex = 0
        )
        assertEquals("1/3", data.getExerciseIndicator())
    }

    @Test
    fun `getExerciseIndicator 越界索引被钳制`() {
        val data = WidgetData(
            exerciseNames = listOf("深蹲", "卧推"),
            currentExerciseIndex = 99
        )
        assertEquals("2/2", data.getExerciseIndicator())
    }

    // ==================== getProgressPercentage ====================

    @Test
    fun `getProgressPercentage 全部完成返回100pct`() {
        val data = WidgetData(weeklyProgress = Pair(5, 5))
        assertEquals(1.0f, data.getProgressPercentage(), 0.001f)
    }

    @Test
    fun `getProgressPercentage 部分完成`() {
        val data = WidgetData(weeklyProgress = Pair(3, 5))
        assertEquals(0.6f, data.getProgressPercentage(), 0.001f)
    }

    @Test
    fun `getProgressPercentage 零完成`() {
        val data = WidgetData(weeklyProgress = Pair(0, 5))
        assertEquals(0f, data.getProgressPercentage(), 0.001f)
    }

    @Test
    fun `getProgressPercentage 总数为零返回零`() {
        val data = WidgetData(weeklyProgress = Pair(0, 0))
        assertEquals(0f, data.getProgressPercentage(), 0.001f)
    }

    /**
     * BUG 复现：completed > total 时百分比超过 100%
     * 这个 bug 是因为 getWeeklyProgress 的 total 过滤掉了 completed 项
     * 导致可能出现 (5, 2) 这样的荒谬数据
     */
    @Test
    fun `BUG getProgressPercentage completed 超过 total 时超过 100pct`() {
        // 模拟 getWeeklyProgress 返回的荒谬数据
        val buggyData = WidgetData(weeklyProgress = Pair(5, 2))
        val percentage = buggyData.getProgressPercentage()
        // 这不应该发生！但当前代码会返回 2.5 (250%)
        assertTrue(
            "BUG: 进度百分比不应该超过 100%, 但实际为 ${percentage * 100}%",
            percentage > 1.0f
        )
    }

    // ==================== hasWorkoutData ====================

    @Test
    fun `hasWorkoutData 有计划名和动作返回 true`() {
        val data = WidgetData(
            planName = "增肌计划",
            exerciseNames = listOf("深蹲")
        )
        assertTrue(data.hasWorkoutData())
    }

    @Test
    fun `hasWorkoutData 缺少计划名返回 false`() {
        val data = WidgetData(
            planName = "",
            exerciseNames = listOf("深蹲")
        )
        assertFalse(data.hasWorkoutData())
    }

    @Test
    fun `hasWorkoutData 缺少动作返回 false`() {
        val data = WidgetData(
            planName = "增肌计划",
            exerciseNames = emptyList()
        )
        assertFalse(data.hasWorkoutData())
    }

    // ==================== getStatusText ====================

    @Test
    fun `getStatusText 各状态正确文字`() {
        assertEquals("今日休息", WidgetData(status = WorkoutStatus.REST_DAY).getStatusText())
        assertEquals("待完成", WidgetData(status = WorkoutStatus.PENDING).getStatusText())
        assertEquals("已完成", WidgetData(status = WorkoutStatus.COMPLETED).getStatusText())
        assertEquals("已逾期 3 天", WidgetData(status = WorkoutStatus.OVERDUE, overdueDays = 3).getStatusText())
    }

    @Test
    fun `getStatusText 逾期0天也显示逾期`() {
        assertEquals("已逾期 0 天", WidgetData(status = WorkoutStatus.OVERDUE, overdueDays = 0).getStatusText())
    }

    // ==================== getStatusEmoji ====================

    @Test
    fun `getStatusEmoji 各状态正确 emoji`() {
        assertEquals("😴", WidgetData(status = WorkoutStatus.REST_DAY).getStatusEmoji())
        assertEquals("⚠️", WidgetData(status = WorkoutStatus.PENDING).getStatusEmoji())
        assertEquals("✅", WidgetData(status = WorkoutStatus.COMPLETED).getStatusEmoji())
        assertEquals("😱", WidgetData(status = WorkoutStatus.OVERDUE).getStatusEmoji())
    }

    // ==================== EmptyWidgetData ====================

    @Test
    fun `EmptyWidgetData 默认为 REST_DAY 状态`() {
        assertEquals(WorkoutStatus.REST_DAY, EmptyWidgetData.status)
        assertTrue(EmptyWidgetData.exerciseNames.isEmpty())
        assertEquals(0, EmptyWidgetData.currentExerciseIndex)
    }
}
