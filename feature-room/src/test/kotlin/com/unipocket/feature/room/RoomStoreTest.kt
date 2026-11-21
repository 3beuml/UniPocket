package com.unipocket.feature.room

import com.unipocket.domain.LocationNode
import com.unipocket.domain.Period
import com.unipocket.domain.RoomSource
import com.unipocket.domain.Semester
import com.unipocket.domain.UniPocketException
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RoomStoreTest {

    private val periods = listOf(
        Period(1, "第1节", 800, 845, "MORNING", 0),
        Period(2, "第2节", 855, 940, "MORNING", 0),
        Period(3, "第3节", 1000, 1045, "MORNING", 1),
        Period(4, "第4节", 1055, 1140, "MORNING", 1),
    )

    private val semester = Semester(
        id = 1,
        nameZh = "测试学期",
        schoolYear = "2026-2027",
        startDate = LocalDate.of(2026, 8, 31),
        endDate = null,
        weekStartOnSunday = false,
        weekIndices = (1..20).toList(),
        isCurrent = true,
    )

    /** 第 2 周周二 */
    private val today = LocalDate.of(2026, 9, 8)

    private val tree = LocationNode(
        id = "campus",
        name = "主校区",
        level = 0,
        children = listOf(
            LocationNode(
                id = "b1",
                name = "1 号楼",
                level = 1,
                children = listOf(
                    LocationNode("b1-101", "101", 2),
                    LocationNode("b1-102", "102", 2),
                ),
            ),
            LocationNode(
                id = "b2",
                name = "2 号楼",
                level = 1,
                children = listOf(LocationNode("b2-201", "201", 2)),
            ),
        ),
    )

    private class FakeSource(
        private val tree: LocationNode,
        var rooms: List<LocationNode> = tree.flatten().filter { it.isLeaf }.toList(),
        var fail: Boolean = false,
    ) : RoomSource {
        override val id: String = "test"
        val queries = mutableListOf<Triple<Int, Int, IntRange>>()

        override suspend fun locations(): LocationNode = tree

        override suspend fun freeRooms(week: Int, dayOfWeek: Int, periods: IntRange): List<LocationNode> {
            queries += Triple(week, dayOfWeek, periods)
            if (fail) throw UniPocketException.Network("查不到")
            return rooms
        }
    }

    @Test
    fun `默认按今天星期与第 1 节查询，并按楼栋分组`() = runTest {
        val store = RoomStore(periods, semester, FakeSource(tree), this) { today }

        store.start()
        advanceUntilIdle()

        val state = store.state.value
        assertEquals(2, state.dayOfWeek)
        assertEquals(1, state.periodIndex)
        assertEquals(listOf("1 号楼", "2 号楼"), state.groups.map { it.title })
        assertEquals(2, state.groups[0].rooms.size)
        assertEquals(listOf("101", "102"), state.groups[0].rooms.map { it.name })
        assertFalse(state.isLoading)
    }

    @Test
    fun `切换楼栋只在本地过滤，不重新请求`() = runTest {
        val source = FakeSource(tree)
        val store = RoomStore(periods, semester, source, this) { today }

        store.start()
        advanceUntilIdle()
        val queriesAfterLoad = source.queries.size

        store.setBuilding("b2")
        advanceUntilIdle()

        assertEquals(queriesAfterLoad, source.queries.size)
        assertEquals(listOf("2 号楼"), store.state.value.groups.map { it.title })
    }

    @Test
    fun `setPeriod 更新筛选节次（矩阵已包含全节次数据无需重查）`() = runTest {
        val source = FakeSource(tree)
        val store = RoomStore(periods, semester, source, this) { today }

        store.start()
        advanceUntilIdle()
        val queriesAfterLoad = source.queries.size

        store.setPeriod(3)

        assertEquals(3, store.state.value.periodIndex)
        assertEquals(queriesAfterLoad, source.queries.size)
    }

    @Test
    fun `查询失败暴露错误`() = runTest {
        val source = FakeSource(tree).apply { fail = true }
        val store = RoomStore(periods, semester, source, this) { today }

        store.start()
        advanceUntilIdle()

        val state = store.state.value
        assertTrue(state.error is UniPocketException.Network)
        assertFalse(state.isLoading)
    }

    @Test
    fun `占用矩阵包含所有节次的空闲信息`() = runTest {
        val source = FakeSource(tree).apply { rooms = tree.flatten().filter { it.isLeaf }.toList() }
        val store = RoomStore(periods, semester, source, this) { today }

        store.start()
        advanceUntilIdle()

        // 所有教室都在矩阵里
        assertEquals(3, store.state.value.freeMatrix.size)
        // 每间教室的空闲节次都在 1..4 范围内
        store.state.value.freeMatrix.values.forEach { periods ->
            assertTrue(periods.all { it in 1..4 })
        }
    }

    @Test
    fun `越界的星期与节次被忽略`() = runTest {
        val source = FakeSource(tree)
        val store = RoomStore(periods, semester, source, this) { today }

        store.start()
        advanceUntilIdle()
        val before = store.state.value

        store.setDayOfWeek(0)
        store.setPeriod(99)

        assertEquals(before.dayOfWeek, store.state.value.dayOfWeek)
        assertEquals(before.periodIndex, store.state.value.periodIndex)
    }
}
