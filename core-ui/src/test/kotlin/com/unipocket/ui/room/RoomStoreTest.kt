package com.unipocket.ui.room

import com.unipocket.domain.LocationNode
import com.unipocket.domain.Period
import com.unipocket.domain.RoomSource
import com.unipocket.domain.SchoolConfig
import com.unipocket.domain.Semester
import com.unipocket.domain.UniPocketException
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RoomStoreTest {

    private val config = SchoolConfig(
        schoolName = "测试大学",
        periods = listOf(
            Period(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
            Period(2, LocalTime.of(8, 55), LocalTime.of(9, 40)),
            Period(3, LocalTime.of(10, 0), LocalTime.of(10, 45)),
            Period(4, LocalTime.of(10, 55), LocalTime.of(11, 40)),
        ),
        semester = Semester("测试学期", LocalDate.of(2026, 8, 31), 20),
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
        val store = RoomStore(config, FakeSource(tree), this) { today }

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
        val store = RoomStore(config, source, this) { today }

        store.start()
        advanceUntilIdle()
        val queriesAfterLoad = source.queries.size

        store.setBuilding("b2")
        advanceUntilIdle()

        assertEquals(queriesAfterLoad, source.queries.size)
        assertEquals(listOf("2 号楼"), store.state.value.groups.map { it.title })
    }

    @Test
    fun `切换节次会带上新的节次重新查询`() = runTest {
        val source = FakeSource(tree)
        val store = RoomStore(config, source, this) { today }

        store.start()
        advanceUntilIdle()

        store.setPeriod(3)
        advanceUntilIdle()

        assertEquals(3..3, source.queries.last().third)
        assertEquals(3, store.state.value.periodIndex)
    }

    @Test
    fun `查询失败保留旧结果并暴露错误`() = runTest {
        val source = FakeSource(tree)
        val store = RoomStore(config, source, this) { today }

        store.start()
        advanceUntilIdle()
        assertEquals(2, store.state.value.groups.size)

        source.fail = true
        store.setPeriod(2)
        advanceUntilIdle()

        val state = store.state.value
        assertEquals(2, state.groups.size)
        assertTrue(state.error is UniPocketException.Network)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `没有空闲教室时 isEmpty 为真`() = runTest {
        val source = FakeSource(tree).apply { rooms = emptyList() }
        val store = RoomStore(config, source, this) { today }

        store.start()
        advanceUntilIdle()

        assertTrue(store.state.value.isEmpty)
    }

    @Test
    fun `越界的星期与节次被忽略`() = runTest {
        val source = FakeSource(tree)
        val store = RoomStore(config, source, this) { today }

        store.start()
        advanceUntilIdle()
        val before = store.state.value

        store.setDayOfWeek(0)
        store.setPeriod(99)

        assertEquals(before.dayOfWeek, store.state.value.dayOfWeek)
        assertEquals(before.periodIndex, store.state.value.periodIndex)
    }
}
