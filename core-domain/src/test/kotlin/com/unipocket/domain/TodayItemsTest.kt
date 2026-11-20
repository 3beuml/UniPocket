package com.unipocket.domain

import com.unipocket.domain.Period
import com.unipocket.domain.Semester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TodayItemsTest {

    private val config = SchoolConfig(
        schoolName = "测试大学",
        periods = listOf(
            Period(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
            Period(2, LocalTime.of(8, 55), LocalTime.of(9, 40)),
            Period(3, LocalTime.of(10, 0), LocalTime.of(10, 45)),
        ),
        semester = Semester("测试学期", LocalDate.of(2026, 8, 31), 20),
    )

    private fun course(id: String, day: Int, periods: IntRange) = Course(
        id = id,
        name = id,
        weeks = setOf(1),
        dayOfWeek = day,
        periods = periods,
    )

    private val fridayCourses = listOf(
        course("A", day = 5, periods = 1..2),
        course("B", day = 5, periods = 3..3),
        course("other-day", day = 4, periods = 1..2),
    )

    @Test
    fun `只保留当天课程并按开始时间排序`() {
        val items = fridayCourses.todayItems(config, dayOfWeek = 5, now = LocalTime.of(7, 0))

        assertEquals(listOf("A", "B"), items.map { it.course.id })
        assertTrue(items.all { it.status == TodayStatus.UPCOMING })
    }

    @Test
    fun `进行中与已结束按当前时刻判定`() {
        val ongoing = fridayCourses.todayItems(config, 5, LocalTime.of(9, 0))
        assertEquals(TodayStatus.ONGOING, ongoing.first { it.course.id == "A" }.status)
        assertEquals(TodayStatus.UPCOMING, ongoing.first { it.course.id == "B" }.status)

        val finished = fridayCourses.todayItems(config, 5, LocalTime.of(11, 0))
        assertTrue(finished.all { it.status == TodayStatus.FINISHED })
    }

    @Test
    fun `课程结束时刻算已结束`() {
        // A 的第二段 09:40 结束
        val items = fridayCourses.todayItems(config, 5, LocalTime.of(9, 40))
        assertEquals(TodayStatus.FINISHED, items.first { it.course.id == "A" }.status)
    }

    @Test
    fun `节次配置缺失的课被跳过`() {
        val broken = listOf(course("bad", day = 5, periods = 9..10))
        val items = broken.todayItems(config, 5, LocalTime.of(8, 0))
        assertTrue(items.isEmpty())
    }

    @Test
    fun `时间文本用真实节次时刻`() {
        val items = fridayCourses.todayItems(config, 5, LocalTime.of(7, 0))
        assertEquals("08:00-09:40", items.first().timeText)
    }
}
