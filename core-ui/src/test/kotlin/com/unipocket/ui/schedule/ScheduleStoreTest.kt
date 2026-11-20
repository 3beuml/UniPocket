package com.unipocket.ui.schedule

import com.unipocket.domain.Course
import com.unipocket.domain.Period
import com.unipocket.domain.ScheduleSource
import com.unipocket.domain.SchoolConfig
import com.unipocket.domain.Semester
import com.unipocket.domain.UniPocketException
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleStoreTest {

    private val config = SchoolConfig(
        schoolName = "测试大学",
        periods = listOf(
            Period(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
            Period(2, LocalTime.of(8, 55), LocalTime.of(9, 40)),
        ),
        semester = Semester(
            name = "测试学期",
            startDate = LocalDate.of(2026, 8, 31), // 周一
            weekCount = 20,
        ),
    )

    /** 第 2 周周二 */
    private val today = LocalDate.of(2026, 9, 8)

    private fun course(id: String, weeks: Set<Int>, day: Int = 2) = Course(
        id = id,
        name = id,
        weeks = weeks,
        dayOfWeek = day,
        periods = 1..2,
    )

    private fun source(vararg courses: Course) = object : ScheduleSource {
        override val id = "test"
        override suspend fun loadCourses(): List<Course> = courses.toList()
    }

    @Test
    fun `首次加载后按当前周过滤`() = runTest {
        val store = ScheduleStore(
            config = config,
            source = source(course("a", setOf(2)), course("b", setOf(3))),
            scope = this,
            clock = { today },
        )

        store.start()
        advanceUntilIdle()

        val state = store.state.value
        assertEquals(2, state.week)
        assertEquals(listOf("a"), state.courses.map { it.id })
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.lastUpdated)
    }

    @Test
    fun `加载失败暴露统一错误类型`() = runTest {
        val failing = object : ScheduleSource {
            override val id = "test"
            override suspend fun loadCourses(): List<Course> =
                throw UniPocketException.Network("连不上教务")
        }
        val store = ScheduleStore(config, failing, this) { today }

        store.start()
        advanceUntilIdle()

        val state = store.state.value
        assertTrue(state.error is UniPocketException.Network)
        assertTrue(state.courses.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `已有缓存时刷新失败保留旧数据`() = runTest {
        var shouldFail = false
        val flaky = object : ScheduleSource {
            override val id = "test"
            override suspend fun loadCourses(): List<Course> {
                if (shouldFail) throw UniPocketException.Network("刷新失败")
                return listOf(course("a", setOf(2)))
            }
        }
        val store = ScheduleStore(config, flaky, this) { today }

        store.start()
        advanceUntilIdle()
        assertEquals(listOf("a"), store.state.value.courses.map { it.id })

        shouldFail = true
        store.refresh()
        advanceUntilIdle()

        val state = store.state.value
        assertEquals(listOf("a"), state.courses.map { it.id })
        assertTrue(state.error is UniPocketException.Network)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `翻周在边界钳制并重新过滤`() = runTest {
        val store = ScheduleStore(
            config = config,
            source = source(course("a", setOf(1)), course("b", setOf(20))),
            scope = this,
            clock = { today },
        )

        store.start()
        advanceUntilIdle()

        store.previousWeek()
        assertEquals(1, store.state.value.week)

        store.setWeek(99)
        assertEquals(20, store.state.value.week)
        assertEquals(listOf("b"), store.state.value.courses.map { it.id })
    }

    @Test
    fun `goToToday 回到当前周`() = runTest {
        val store = ScheduleStore(config, source(), this) { today }

        store.start()
        advanceUntilIdle()

        store.setWeek(10)
        assertFalse(store.state.value.isCurrentWeek)

        store.goToToday()
        assertEquals(2, store.state.value.week)
        assertTrue(store.state.value.isCurrentWeek)
    }

    @Test
    fun `非框架异常也会归一成 Unknown`() = runTest {
        val broken = object : ScheduleSource {
            override val id = "test"
            override suspend fun loadCourses(): List<Course> = throw IllegalStateException("解析炸了")
        }
        val store = ScheduleStore(config, broken, this) { today }

        store.start()
        advanceUntilIdle()

        val error = store.state.value.error
        assertTrue(error is UniPocketException.Unknown)
        assertEquals("解析炸了", error?.message)
    }
}
