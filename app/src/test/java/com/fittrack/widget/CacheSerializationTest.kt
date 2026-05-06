package com.fittrack.widget

import org.junit.Assert.*
import org.junit.Test

/**
 * 缓存序列化 Bug 复现测试
 * 验证 WidgetDataProvider 中逗号分隔符序列化存在的问题
 *
 * 作者：小花 (kohana) — 品控测试
 *
 * BUG #1: 练习名含逗号时，joinToString(",") + split(",") 会错误拆分
 * BUG #2: WorkoutStatus ordinal 越界时 IndexOutOfBoundsException
 * BUG #3: 缓存中练习名为空字符串时 split 后产生多余空元素
 */
class CacheSerializationTest {

    // ==================== BUG #1: 逗号分隔符序列化崩溃 ====================

    /**
     * BUG 复现：练习名包含逗号时，序列化后反序列化结果错误
     *
     * WidgetDataProvider.saveToCache() 使用 joinToString(",")
     * WidgetDataProvider.getCachedData() 使用 split(",")
     * 如果练习名包含逗号（如 "卧推, 杠铃"），反序列化后会变成 ["卧推", " 杠铃"]
     */
    @Test
    fun `BUG 练习名含逗号时序列化拆分错误`() {
        val exercises = listOf("卧推, 杠铃", "深蹲", "硬拉 10x3")

        // 模拟 WidgetDataProvider 的序列化逻辑
        val serialized = exercises.joinToString(",")
        val deserialized = serialized.split(",").filter { it.isNotEmpty() }

        // 预期：["卧推, 杠铃", "深蹲", "硬拉 10x3"]
        // 实际：["卧推", " 杠铃", "深蹲", "硬拉 10x3"] — 多出一个元素！
        assertEquals(
            "练习名含逗号后拆分结果长度应该不变",
            3,
            deserialized.size
        )
    }

    @Test
    fun `BUG 含逗号练习名拆分后内容错乱`() {
        val exercises = listOf("卧推, 杠铃", "深蹲")

        val serialized = exercises.joinToString(",")
        val deserialized = serialized.split(",").filter { it.isNotEmpty() }

        // 第一个元素应该是 "卧推, 杠铃"，但实际被拆成了 "卧推"
        assertEquals(
            "卧推, 杠铃",
            deserialized[0]
        )
    }

    @Test
    fun `BUG 多个练习名含逗号时更严重`() {
        val exercises = listOf("卧推, 杠铃", "深蹲, 自重", "硬拉")

        val serialized = exercises.joinToString(",")
        val deserialized = serialized.split(",").filter { it.isNotEmpty() }

        // 预期 3 个，实际 5 个
        assertEquals(
            "多个逗号练习名拆分后长度应该不变",
            3,
            deserialized.size
        )
    }

    /**
     * 修复方案验证：使用空字符作为分隔符应该能正确处理含逗号的练习名
     */
    @Test
    fun `FIX 使用空字符分隔符正确序列化含逗号练习名`() {
        val exercises = listOf("卧推, 杠铃", "深蹲", "硬拉 10x3")

        // 使用 \u0000 (null character) 作为分隔符
        val serialized = exercises.joinToString("\u0000")
        val deserialized = serialized.split("\u0000")

        assertEquals(3, deserialized.size)
        assertEquals("卧推, 杠铃", deserialized[0])
        assertEquals("深蹲", deserialized[1])
        assertEquals("硬拉 10x3", deserialized[2])
    }

    /**
     * 修复方案验证：使用 JSON 序列化也能正确处理
     */
    @Test
    fun `FIX 使用 JSON 格式序列化含逗号练习名`() {
        val exercises = listOf("卧推, 杠铃", "深蹲", "硬拉 10x3")

        // 简单 JSON 数组格式
        val serialized = "[\"${exercises.joinToString("\",\"") { it.replace("\"", "\\\"") }}\"]"
        // 手动解析简化版（实际应用中用 Gson/Jackson）
        val content = serialized
            .removePrefix("[")
            .removeSuffix("]")
            .split("\",\"")
            .map { it.removeSurrounding("\"") }

        assertEquals(3, content.size)
        assertEquals("卧推, 杠铃", content[0])
    }

    // ==================== BUG #2: WorkoutStatus ordinal 越界 ====================

    /**
     * BUG 复现：如果枚举顺序变化，缓存的 ordinal 可能越界
     *
     * WidgetDataProvider.getCachedData() 直接使用 ordinal 索引：
     *   WorkoutStatus.entries[preferences[Keys.CACHED_STATUS] ?: 0]
     * 如果未来新增状态或调整顺序，缓存的旧 ordinal 会导致崩溃
     */
    @Test(expected = ArrayIndexOutOfBoundsException::class)
    fun `BUG WorkoutStatus ordinal 越界时崩溃`() {
        // 模拟缓存中存了一个过期的 ordinal（比如旧版本有5个状态，现在只有4个）
        val cachedOrdinal = 99 // 越界值

        // 这行代码会抛出 ArrayIndexOutOfBoundsException
        WorkoutStatus.entries[cachedOrdinal]
    }

    @Test
    fun `BUG WorkoutStatus ordinal 等于 size 时也崩溃`() {
        val size = WorkoutStatus.entries.size
        try {
            // ordinal 最大有效值是 size - 1
            WorkoutStatus.entries[size]
            fail("应该抛出 ArrayIndexOutOfBoundsException")
        } catch (e: ArrayIndexOutOfBoundsException) {
            // 预期行为
        }
    }

    /**
     * 修复方案验证：使用 getOrNull + fallback
     */
    @Test
    fun `FIX 使用 getOrNull 避免 ordinal 越界`() {
        val cachedOrdinal = 99

        val status = WorkoutStatus.entries.getOrNull(cachedOrdinal) ?: WorkoutStatus.REST_DAY

        assertEquals(WorkoutStatus.REST_DAY, status)
    }

    // ==================== 边界条件 ====================

    @Test
    fun `序列化空练习列表`() {
        val exercises = emptyList<String>()
        val serialized = exercises.joinToString(",")
        val deserialized = serialized.split(",").filter { it.isNotEmpty() }

        assertTrue("空列表序列化后应该为空", deserialized.isEmpty())
    }

    @Test
    fun `序列化单个练习名`() {
        val exercises = listOf("深蹲")
        val serialized = exercises.joinToString(",")
        val deserialized = serialized.split(",").filter { it.isNotEmpty() }

        assertEquals(1, deserialized.size)
        assertEquals("深蹲", deserialized[0])
    }
}
