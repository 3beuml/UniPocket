package com.unipocket.ui.schedule

import com.unipocket.domain.EditableScheduleSource
import com.unipocket.domain.Period
import com.unipocket.domain.ScheduleSource
import com.unipocket.domain.Semester
import com.unipocket.domain.Session
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

class ScheduleStoreTest {

    private val semester = Semester(
        id = 1,
        nameZh = "测试学期",
        schoolYear = "2026-2027",
        startDate = LocalDate.of(2026, 8, 31), // 周一
        endDate = null,
        weekStartOnSunday = false,
        weekIndices = (1..20).toList(),
        isCurrent = true,
    )

    /** 第 2 周周二 */
    private val today = LocalDate.of(2026, 9, 8)

    private fun session(id: String, weekIndex: Int, weekday: Int = 2) = Session(
        id = id,
        semesterId = 1,
        source = "eas",
        courseCode = id,
        courseName = id,
        weekIndex = weekIndex,
        weekday = weekday,
        startUnit = 1,
        endUnit = 2,
        date = null,
        startTime = null,
        endTime = null,
        teacherName = null,
        roomText = null,
        campusText = null,
        color = null,
    )

    private fun source(vararg sessions: Session) = object : ScheduleSource {
        override val id = "test"
        override suspend fun loadSessions(): List<Session> = sessions.toList()
    }

    @Test
    fun `首次加载后按当前周过滤`() = runTest {
        val store = ScheduleStore(
            semester = semester,
            source = source(session("a", 2), session("b", 3)),
            scope = this,
            clock = { today },
        )

        store.start()
        advanceUntilIdle()

        val state = store.state.value
        assertEquals(2, state.week)
        assertEquals(listOf("a"), state.sessions.map { it.id })
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.lastUpdated)
    }

    @Test
    fun `加载失败暴露统一错误类型`() = runTest {
        val failing = object : ScheduleSource {
            override val id = "test"
            override suspend fun loadSessions(): List<Session> =
                throw UniPocketException.Network("连不上教务")
        }
        val store = ScheduleStore(semester, failing, this, clock = { today })

        store.start()
        advanceUntilIdle()

        val state = store.state.value
        assertTrue(state.error is UniPocketException.Network)
        assertTrue(state.sessions.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `已有缓存时刷新失败保留旧数据`() = runTest {
        var shouldFail = false
        val flaky = object : ScheduleSource {
            override val id = "test"
            override suspend fun loadSessions(): List<Session> {
                if (shouldFail) throw UniPocketException.Network("刷新失败")
                return listOf(session("a", 2))
            }
        }
        val store = ScheduleStore(semester, flaky, this, clock = { today })

        store.start()
        advanceUntilIdle()
        assertEquals(listOf("a"), store.state.value.sessions.map { it.id })

        shouldFail = true
        store.refresh()
        advanceUntilIdle()

        val state = store.state.value
        assertEquals(listOf("a"), state.sessions.map { it.id })
        assertTrue(state.error is UniPocketException.Network)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `翻周在边界钳制并重新过滤`() = runTest {
        val store = ScheduleStore(
            semester = semester,
            source = source(session("a", 1), session("b", 20)),
            scope = this,
            clock = { today },
        )

        store.start()
        advanceUntilIdle()

        store.previousWeek()
        assertEquals(1, store.state.value.week)

        store.setWeek(99)
        assertEquals(20, store.state.value.week)
        assertEquals(listOf("b"), store.state.value.sessions.map { it.id })
    }

    @Test
    fun `goToToday 回到当前周`() = runTest {
        val store = ScheduleStore(semester, source(), this, clock = { today })

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
            override suspend fun loadSessions(): List<Session> = throw IllegalStateException("解析炸了")
        }
        val store = ScheduleStore(semester, broken, this, clock = { today })

        store.start()
        advanceUntilIdle()

        val error = store.state.value.error
        assertTrue(error is UniPocketException.Unknown)
        assertEquals("解析炸了", error?.message)
    }

    private class EditableSource : EditableScheduleSource {
        val stored = mutableListOf<Session>()
        override val id = "manual"
        override suspend fun loadSessions(): List<Session> = stored.toList()
        override suspend fun upsert(session: Session) {
            stored.removeAll { it.id == session.id }
            stored += session
        }
        override suspend fun delete(id: String) {
            stored.removeAll { it.id == id }
        }
        override suspend fun manages(id: String): Boolean = stored.any { it.id == id }
    }

    @Test
    fun `补录后自动刷新并出现在当前周`() = runTest {
        val store = ScheduleStore(semester, EditableSource(), this, clock = { today })
        store.start()
        advanceUntilIdle()
        assertTrue(store.state.value.sessions.isEmpty())

        store.upsertSessions(listOf(session("m1", 2)))
        advanceUntilIdle()

        assertEquals(listOf("m1"), store.state.value.sessions.map { it.id })
    }

    @Test
    fun `补录多周会按周过滤显示`() = runTest {
        val store = ScheduleStore(semester, EditableSource(), this, clock = { today })
        store.start()
        advanceUntilIdle()

        store.upsertSessions(listOf(session("m1", 2), session("m1b", 3)))
        advanceUntilIdle()

        assertEquals(listOf("m1"), store.state.value.sessions.map { it.id })
        store.setWeek(3)
        assertEquals(listOf("m1b"), store.state.value.sessions.map { it.id })
    }

    @Test
    fun `删除后刷新移除`() = runTest {
        val store = ScheduleStore(semester, EditableSource(), this, clock = { today })
        store.start()
        advanceUntilIdle()

        store.upsertSessions(listOf(session("m1", 2)))
        advanceUntilIdle()
        store.deleteSession("m1")
        advanceUntilIdle()

        assertTrue(store.state.value.sessions.isEmpty())
    }

    @Test
    fun `数据源不支持编辑时编辑调用被忽略`() = runTest {
        val store = ScheduleStore(semester, source(session("a", 2)), this, clock = { today })
        store.start()
        advanceUntilIdle()

        store.upsertSessions(listOf(session("x", 2)))
        advanceUntilIdle()

        assertEquals(listOf("a"), store.state.value.sessions.map { it.id })
    }
}
