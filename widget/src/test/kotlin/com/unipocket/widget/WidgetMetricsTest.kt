package com.unipocket.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetMetricsTest {

    @Test
    fun `上报高度减去 30dp 虚高`() {
        assertEquals(135f, WidgetMetrics.correctedHeight(165f))
        assertEquals(80f, WidgetMetrics.correctedHeight(60f), 0.001f)
    }

    @Test
    fun `CJK 课名按 1em 估算行数`() {
        // 4 个汉字 × 9sp = 36dp，在 43dp 列宽里放得下
        assertEquals(1, WidgetMetrics.estimateLines("高等数学", 43f, 9f))
        // 14 个汉字 = 126dp → 3 行，但 maxLines=2
        assertEquals(2, WidgetMetrics.estimateLines("大学生职业生涯规划与就业指导", 43f, 9f))
    }

    @Test
    fun `ASCII 按 055em 估算`() {
        // 10 个 ASCII × 9 × 0.55 = 49.5dp，40dp 列宽 → 2 行
        assertEquals(2, WidgetMetrics.estimateLines("Algorithm3", 40f, 9f))
    }

    @Test
    fun `自然高随课名行数增加`() {
        assertEquals(44f, WidgetMetrics.naturalRowHeight(1), 0.001f)
        assertEquals(56f, WidgetMetrics.naturalRowHeight(2), 0.001f)
    }

    @Test
    fun `行高账本按自然高累计，装不下就少画一行`() {
        // 上报 142 → 真实 112；减列头 34 与内边距 8 = 70 可用
        // 每行自然高 44：两行 88 > 70 → 只画 1 行
        val plan = WeekLayoutPlanner.plan(
            reportedHeightDp = 142f,
            widthDp = 300f,
            periodCount = 5,
            nameLinesByRow = emptyMap(),
        )
        assertEquals(1, plan.visibleRows)
        assertTrue(plan.rowHeightsDp[0] >= 44f)
    }

    @Test
    fun `剩余空间按行均摊且每行最多加 12dp`() {
        // 上报 300 → 真实 270；减 34+8 = 228 可用
        // 10 行自然高 44 = 440 > 228 → 只放 5 行（220），剩 8 均摊 1.6
        val plan = WeekLayoutPlanner.plan(
            reportedHeightDp = 300f,
            widthDp = 300f,
            periodCount = 10,
            nameLinesByRow = emptyMap(),
        )
        assertEquals(5, plan.visibleRows)
        assertTrue(plan.rowHeightsDp.all { it in 44f..56f })

        // 超大组件：每行最多加 12
        val big = WeekLayoutPlanner.plan(
            reportedHeightDp = 1000f,
            widthDp = 300f,
            periodCount = 2,
            nameLinesByRow = emptyMap(),
        )
        assertTrue(big.rowHeightsDp.all { it <= 44f + 12f + 0.001f })
    }

    @Test
    fun `换行感知：两行课名的行自然高更大`() {
        val plan = WeekLayoutPlanner.plan(
            reportedHeightDp = 400f,
            widthDp = 300f,
            periodCount = 3,
            nameLinesByRow = mapOf(2 to 2),
        )
        assertEquals(3, plan.visibleRows)
        assertTrue(plan.rowHeightsDp[1] > plan.rowHeightsDp[0])
    }

    @Test
    fun `极端小尺寸也保留一行`() {
        val plan = WeekLayoutPlanner.plan(
            reportedHeightDp = 80f,
            widthDp = 200f,
            periodCount = 10,
            nameLinesByRow = emptyMap(),
        )
        assertEquals(1, plan.visibleRows)
    }

    @Test
    fun `列宽按 7 列均分`() {
        val plan = WeekLayoutPlanner.plan(
            reportedHeightDp = 300f,
            widthDp = 302f,
            periodCount = 2,
            nameLinesByRow = emptyMap(),
        )
        // (302 - 26) / 7 = 39.43
        assertEquals(39.43f, plan.columnWidthDp, 0.01f)
    }
}
