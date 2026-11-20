package com.unipocket.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseTest {

    private fun course(
        id: String = "c1",
        weeks: Set<Int> = setOf(1, 2, 3),
        dayOfWeek: Int = 1,
        periods: IntRange = 1..2,
    ) = Course(
        id = id,
        name = "高等数学",
        teacher = "张老师",
        room = "教1-101",
        weeks = weeks,
        dayOfWeek = dayOfWeek,
        periods = periods,
    )

    @Test
    fun `周次文本合并为区间`() {
        assertEquals("1-3,5-6", course(weeks = setOf(1, 2, 3, 5, 6)).weeksText)
        assertEquals("2", course(weeks = setOf(2)).weeksText)
        assertEquals("1,3,5", course(weeks = setOf(1, 3, 5)).weeksText)
    }

    @Test
    fun `isInWeek 判断周次成员`() {
        val c = course(weeks = setOf(2, 4))
        assertTrue(c.isInWeek(2))
        assertFalse(c.isInWeek(3))
    }

    @Test
    fun `inWeek 只保留本周有课的条目`() {
        val list = listOf(course(id = "a", weeks = setOf(1)), course(id = "b", weeks = setOf(2)))
        assertEquals(listOf("a"), list.inWeek(1).map { it.id })
    }

    @Test
    fun `非法字段在构造时失败`() {
        assertThrows(IllegalArgumentException::class.java) {
            course(weeks = emptySet())
        }
        assertThrows(IllegalArgumentException::class.java) {
            course(dayOfWeek = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            course(periods = 2..1)
        }
    }
}
