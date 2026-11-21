package com.unipocket.domain

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayItemsTest {

    private val periods = listOf(
        Period(1, "第1节", 800, 845, "MORNING", 0),
        Period(2, "第2节", 855, 940, "MORNING", 0),
        Period(3, "第3节", 1000, 1045, "MORNING", 1),
    )

    /** 周三的课；weekIndex 由调用方先过滤，这里直接给已过滤的列表 */
    private fun session(id: String, weekday: Int, startUnit: Int, endUnit: Int, startTime: String? = null, endTime: String? = null) =
        Session(
            id = id,
            semesterId = 1,
            source = "eas",
            courseCode = id,
            courseName = id,
            weekIndex = 12,
            weekday = weekday,
            startUnit = startUnit,
            endUnit = endUnit,
            date = null,
            startTime = startTime,
            endTime = endTime,
            teacherName = null,
            roomText = null,
            campusText = null,
            color = null,
        )

    @Test
    fun `按星期过滤并按开始时刻排序`() {
        val sessions = listOf(
            session("c", weekday = 3, startUnit = 3, endUnit = 3),
            session("a", weekday = 3, startUnit = 1, endUnit = 2),
            session("b", weekday = 2, startUnit = 1, endUnit = 2), // 别的星期，不进列表
        )
        val items = sessions.todayItems(periods, dayOfWeek = 3, now = LocalTime.of(9, 0))

        assertEquals(listOf("a", "c"), items.map { it.session.id })
        assertEquals(LocalTime.of(8, 0), items[0].start)
        assertEquals(LocalTime.of(10, 45), items[1].end)
    }

    @Test
    fun `三态判定以结束时刻为界`() {
        val sessions = listOf(session("a", weekday = 3, startUnit = 1, endUnit = 2))
        val at = { h: Int, m: Int -> sessions.todayItems(periods, 3, LocalTime.of(h, m)).single().status }

        assertEquals(TodayStatus.UPCOMING, at(7, 59))
        assertEquals(TodayStatus.ONGOING, at(8, 0))
        assertEquals(TodayStatus.FINISHED, at(9, 40))
        assertEquals(TodayStatus.FINISHED, at(9, 41))
    }

    @Test
    fun `节次表查不到时退回 Session 自带时刻`() {
        val sessions = listOf(
            session("a", weekday = 3, startUnit = 9, endUnit = 9, startTime = "19:00", endTime = "19:45"),
        )
        val item = sessions.todayItems(periods, 3, LocalTime.of(19, 20)).single()

        assertEquals(LocalTime.of(19, 0), item.start)
        assertEquals(LocalTime.of(19, 45), item.end)
        assertEquals(TodayStatus.ONGOING, item.status)
    }

    @Test
    fun `时间完全缺失的条目跳过（不画错位置）`() {
        val sessions = listOf(session("a", weekday = 3, startUnit = 9, endUnit = 9))
        assertTrue(sessions.todayItems(periods, 3, LocalTime.of(9, 0)).isEmpty())
    }

    @Test
    fun `nextUpcoming 取第一门未结束的课`() {
        val sessions = listOf(
            session("past", weekday = 3, startUnit = 1, endUnit = 2),
            session("next", weekday = 3, startUnit = 3, endUnit = 3),
        )
        val items = sessions.todayItems(periods, 3, LocalTime.of(9, 50))

        assertEquals("next", items.nextUpcoming()?.session?.id)

        val allOver = sessions.todayItems(periods, 3, LocalTime.of(23, 0))
        assertNull(allOver.nextUpcoming())
    }

    @Test
    fun `相对文案的零分钟兜底`() {
        val sessions = listOf(session("a", weekday = 3, startUnit = 1, endUnit = 2))
        // 9:30 构建（进行中），把 now 拨到结束时刻：还剩 0 分钟时说"马上下课"，不出"还剩 0分钟"
        val item = sessions.todayItems(periods, 3, LocalTime.of(9, 30)).single()
        assertEquals("马上下课", item.relativeText(LocalTime.of(9, 40)))

        // 状态随重建翻转：过了结束时刻重建就是已结束
        val after = sessions.todayItems(periods, 3, LocalTime.of(9, 41)).single()
        assertEquals("已结束", after.relativeText(LocalTime.of(9, 41)))
    }
}

private fun assertTrue(b: Boolean) = org.junit.Assert.assertTrue(b)
