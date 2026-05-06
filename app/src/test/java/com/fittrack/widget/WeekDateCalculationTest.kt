package com.fittrack.widget

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期计算 Bug 复现测试
 * 验证 WidgetDataProvider 中周日期计算的周日边界问题
 *
 * 作者：小花 (kohana) — 品控测试
 *
 * BUG: getWeekStartDate() 和 getWeekEndDate() 使用 Calendar.set(DAY_OF_WEEK, ...)
 * 在周日调用时，Calendar 的行为会导致日期跳到下一周
 *
 * 例如：如果今天是 2025-01-05（周日），
 *   calendar.set(DAY_OF_WEEK, MONDAY) 会设置到 2025-01-06（下周一）
 *   而不是 2024-12-30（本周一）
 */
class WeekDateCalculationTest {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // ==================== BUG: 周一计算在周日跳到下周 ====================

    /**
     * BUG 复现：周日晚上的本周开始日期会跳到下周一
     *
     * 模拟 WidgetDataProvider.getWeekStartDate() 的逻辑
     */
    @Test
    fun `BUG getWeekStartDate 在周日返回下周一而不是本周一`() {
        // 构造一个周日：2025-01-05 是周日
        val sunday = createCalendar(2025, Calendar.JANUARY, 5)

        // 当前 WidgetDataProvider 的 buggy 逻辑
        val buggyStart = getWeekStartDateBuggy(sunday)

        // 2025-01-05 是周日，本周一应该是 2024-12-30
        val expectedStart = "2024-12-30"

        // buggy 版本会返回 "2025-01-06"（下周一）
        assertEquals(
            "周日时本周一开始日期应该是 $expectedStart，但 buggy 版本返回了 $buggyStart",
            expectedStart,
            buggyStart
        )
    }

    /**
     * BUG 复现：周日的本周结束日期也会出错
     */
    @Test
    fun `BUG getWeekEndDate 在周日返回下周日而不是本周日`() {
        val sunday = createCalendar(2025, Calendar.JANUARY, 5)

        val buggyEnd = getWeekEndDateBuggy(sunday)

        // 本周日就是今天 2025-01-05
        val expectedEnd = "2025-01-05"

        assertEquals(
            "周日时本周日结束日期应该是 $expectedEnd，但 buggy 版本返回了 $buggyEnd",
            expectedEnd,
            buggyEnd
        )
    }

    /**
     * 验证非周日时当前逻辑是否正确
     */
    @Test
    fun `getWeekStartDate 在周三正确返回本周一`() {
        val wednesday = createCalendar(2025, Calendar.JANUARY, 1) // 2025-01-01 是周三

        val start = getWeekStartDateBuggy(wednesday)

        // 2025-01-01 周三 → 本周一 2024-12-30
        assertEquals("2024-12-30", start)
    }

    /**
     * 验证周一时当前逻辑正确
     */
    @Test
    fun `getWeekStartDate 在周一正确返回当天`() {
        val monday = createCalendar(2024, Calendar.DECEMBER, 30) // 2024-12-30 是周一

        val start = getWeekStartDateBuggy(monday)

        assertEquals("2024-12-30", start)
    }

    /**
     * 验证周六时当前逻辑正确
     */
    @Test
    fun `getWeekStartDate 在周六正确返回本周一`() {
        val saturday = createCalendar(2025, Calendar.JANUARY, 4) // 2025-01-04 是周六

        val start = getWeekStartDateBuggy(saturday)

        assertEquals("2024-12-30", start)
    }

    // ==================== 修复方案验证 ====================

    /**
     * 修复方案：使用 getAdjustedToPreviousWeekStart
     */
    @Test
    fun `FIX 周日时正确返回本周一`() {
        val sunday = createCalendar(2025, Calendar.JANUARY, 5)

        val start = getWeekStartDateFixed(sunday)

        assertEquals("2024-12-30", start)
    }

    @Test
    fun `FIX 非周日时修复版仍然正确`() {
        val wednesday = createCalendar(2025, Calendar.JANUARY, 1)

        val start = getWeekStartDateFixed(wednesday)

        assertEquals("2024-12-30", start)
    }

    @Test
    fun `FIX 周一时修复版仍然正确`() {
        val monday = createCalendar(2024, Calendar.DECEMBER, 30)

        val start = getWeekStartDateFixed(monday)

        assertEquals("2024-12-30", start)
    }

    /**
     * 修复方案：正确的周末日期计算
     */
    @Test
    fun `FIX 周日时正确返回本周日作为结束日期`() {
        val sunday = createCalendar(2025, Calendar.JANUARY, 5)

        val end = getWeekEndDateFixed(sunday)

        assertEquals("2025-01-05", end)
    }

    @Test
    fun `FIX 周一时正确返回本周日作为结束日期`() {
        val monday = createCalendar(2024, Calendar.DECEMBER, 30)

        val end = getWeekEndDateFixed(monday)

        assertEquals("2025-01-05", end)
    }

    // ==================== 辅助方法：模拟 buggy 逻辑 ====================

    /**
     * 模拟 WidgetDataProvider.getWeekStartDate() 的当前 buggy 逻辑
     */
    private fun getWeekStartDateBuggy(reference: Calendar): String {
        val cal = reference.clone() as Calendar
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return sdf.format(cal.time)
    }

    /**
     * 模拟 WidgetDataProvider.getWeekEndDate() 的当前 buggy 逻辑
     */
    private fun getWeekEndDateBuggy(reference: Calendar): String {
        val cal = reference.clone() as Calendar
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        return sdf.format(cal.time)
    }

    /**
     * 修复方案：正确获取本周一
     * 先设置到 MONDAY，如果结果大于今天，回退一周
     */
    private fun getWeekStartDateFixed(reference: Calendar): String {
        val cal = reference.clone() as Calendar
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        // 如果设置后日期比参考日期大（说明跳到了下周），回退一周
        if (cal.after(reference)) {
            cal.add(Calendar.WEEK_OF_YEAR, -1)
        }
        return sdf.format(cal.time)
    }

    /**
     * 修复方案：正确获取本周日
     * 先设置到 SUNDAY，如果结果小于今天（说明跳到了上周），前进一周
     */
    private fun getWeekEndDateFixed(reference: Calendar): String {
        val cal = reference.clone() as Calendar
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        // 如果设置后日期比参考日期小（说明跳到了上周），前进一周
        if (cal.before(reference)) {
            cal.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return sdf.format(cal.time)
    }

    private fun createCalendar(year: Int, month: Int, day: Int): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 12) // 设置正午避免跨日问题
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }
}
