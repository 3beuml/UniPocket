package com.unipocket.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SemesterTest {

    private val mondaySemester = Semester(
        name = "2026-2027 第一学期",
        startDate = LocalDate.of(2026, 9, 2), // 周三，故意不给周首
        weekCount = 20,
    )

    @Test
    fun `startDate 会归一到所在周的周一`() {
        assertEquals(LocalDate.of(2026, 8, 31), mondaySemester.firstWeekStart)
    }

    @Test
    fun `周一制下 dateOf 按周与星期计算`() {
        assertEquals(LocalDate.of(2026, 8, 31), mondaySemester.dateOf(1, 1))
        assertEquals(LocalDate.of(2026, 9, 2), mondaySemester.dateOf(1, 3))
        assertEquals(LocalDate.of(2026, 9, 6), mondaySemester.dateOf(1, 7)) // 第1周周日
        assertEquals(LocalDate.of(2026, 9, 7), mondaySemester.dateOf(2, 1))
        assertEquals(LocalDate.of(2027, 1, 17), mondaySemester.dateOf(20, 7))
    }

    @Test
    fun `weekOf 边界正确且学期外返回 null`() {
        assertEquals(1, mondaySemester.weekOf(LocalDate.of(2026, 8, 31)))
        assertEquals(1, mondaySemester.weekOf(LocalDate.of(2026, 9, 6))) // 周日仍属第1周
        assertEquals(2, mondaySemester.weekOf(LocalDate.of(2026, 9, 7)))
        assertNull(mondaySemester.weekOf(LocalDate.of(2026, 8, 30)))
        assertNull(mondaySemester.weekOf(LocalDate.of(2026, 8, 31).plusDays(20L * 7)))
    }

    @Test
    fun `lastDate 是第 weekCount 周的最后一天`() {
        assertEquals(LocalDate.of(2026, 8, 31).plusDays(20L * 7 - 1), mondaySemester.lastDate)
    }

    @Test
    fun `周日制下星期编号不变但周边界前移`() {
        val sundaySemester = mondaySemester.copy(weekStartOnSunday = true)
        assertEquals(LocalDate.of(2026, 8, 30), sundaySemester.firstWeekStart)
        assertEquals(LocalDate.of(2026, 8, 30), sundaySemester.dateOf(1, 7))
        assertEquals(LocalDate.of(2026, 8, 31), sundaySemester.dateOf(1, 1))
        assertEquals(1, sundaySemester.weekOf(LocalDate.of(2026, 8, 30)))
        assertEquals(2, sundaySemester.weekOf(LocalDate.of(2026, 9, 6)))
        assertNull(sundaySemester.weekOf(LocalDate.of(2026, 8, 29)))
    }
}
