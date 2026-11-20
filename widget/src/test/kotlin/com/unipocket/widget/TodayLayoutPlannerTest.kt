package com.unipocket.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayLayoutPlannerTest {

    @Test
    fun `空列表返回空计划`() {
        val plan = TodayLayoutPlanner.plan(reportedHeightDp = 200f, nameLineCounts = emptyList())
        assertTrue(plan.rowHeightsDp.isEmpty())
    }

    @Test
    fun `按自然高累计，装不下少画`() {
        // 上报 140 → 真实 110；减列头 30 与内边距 6 = 74 可用
        // 每行 38：两行 76 > 74 → 只画 1 行
        val plan = TodayLayoutPlanner.plan(
            reportedHeightDp = 140f,
            nameLineCounts = listOf(1, 1, 1),
        )
        assertEquals(1, plan.visibleRows)
        assertTrue(plan.rowHeightsDp[0] >= 38f)
    }

    @Test
    fun `两行课名自然高多 10dp`() {
        val plan = TodayLayoutPlanner.plan(
            reportedHeightDp = 400f,
            nameLineCounts = listOf(1, 2),
        )
        assertEquals(2, plan.visibleRows)
        assertTrue(plan.rowHeightsDp[1] > plan.rowHeightsDp[0] + 8f)
    }

    @Test
    fun `均摊撑高但每行最多加 8dp`() {
        val plan = TodayLayoutPlanner.plan(
            reportedHeightDp = 400f,
            nameLineCounts = listOf(1, 1),
        )
        assertEquals(2, plan.visibleRows)
        assertTrue(plan.rowHeightsDp.all { it <= 38f + 8f + 0.001f })
    }
}
