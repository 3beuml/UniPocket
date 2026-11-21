package com.unipocket.ui.schedule

import com.unipocket.domain.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleBlockTest {

    private fun slot(
        id: String,
        name: String = id,
        day: Int = 1,
        start: Int = 1,
        end: Int = 2,
        week: Int = 1,
    ) = ScheduleSlot(
        id = id, name = name, room = null, teacher = null,
        courseCode = id, week = week, dayOfWeek = day,
        startPeriod = start, endPeriod = end,
        date = null, startTime = null, endTime = null,
        conflictKey = "$id#$week",
    )

    @Test
    fun `普通课按连续节次合并`() {
        val slots = listOf(slot("a", start = 1, end = 4))
        val blocks = scheduleBlocks(slots, maxPeriod = 6)
        assertEquals(1, blocks.size)
        assertEquals(1, blocks[0].startPeriod)
        assertEquals(4, blocks[0].endPeriod)
        assertFalse(blocks[0].isConflict)
    }

    @Test
    fun `不连续的课不合并`() {
        val slots = listOf(slot("a", start = 1, end = 2), slot("b", start = 4, end = 5))
        val blocks = scheduleBlocks(slots, maxPeriod = 6)
        assertEquals(2, blocks.size)
        assertEquals(1, blocks[0].startPeriod)
        assertEquals(4, blocks[1].startPeriod)
    }

    @Test
    fun `重叠课切成三段`() {
        val a = slot("甲", start = 1, end = 4)
        val b = slot("乙", start = 3, end = 6)
        val blocks = scheduleBlocks(listOf(a, b), maxPeriod = 8)

        assertEquals(3, blocks.size)
        assertEquals(listOf("甲"), blocks[0].slots.map { it.id })
        assertEquals(setOf("甲", "乙"), blocks[1].slots.map { it.id }.toSet())
        assertEquals(listOf("乙"), blocks[2].slots.map { it.id })
        assertTrue(blocks[1].isConflict)
        assertFalse(blocks[0].isConflict)
    }

    @Test
    fun `不同天的课不互相影响`() {
        val slots = listOf(
            slot("a", day = 1, start = 1, end = 2),
            slot("b", day = 2, start = 1, end = 2),
        )
        val blocks = scheduleBlocks(slots, maxPeriod = 6)
        assertEquals(2, blocks.size)
        assertEquals(1, blocks[0].dayOfWeek)
        assertEquals(2, blocks[1].dayOfWeek)
    }

    @Test
    fun `expandByWeek 只留下该周有课的记录`() {
        val sessions = listOf(
            session(id = "a", name = "高数", weekIndex = 1, weekday = 1, startUnit = 1, endUnit = 2),
            session(id = "b", name = "体育", weekIndex = 5, weekday = 1, startUnit = 3, endUnit = 4),
        )
        val week1 = sessions.expandByWeek(1)
        assertEquals(1, week1.size)
        assertEquals("a", week1[0].id)
        assertEquals("高数", week1[0].name)

        val week5 = sessions.expandByWeek(5)
        assertEquals(1, week5.size)
        assertEquals("b", week5[0].id)

        val week10 = sessions.expandByWeek(10)
        assertTrue(week10.isEmpty())
    }

    @Test
    fun `冲突选择的 key 稳定`() {
        val a = slot("甲", start = 3, end = 4)
        val b = slot("乙", start = 3, end = 4)
        val blocks = scheduleBlocks(listOf(a, b), maxPeriod = 6)
        val conflictBlock = blocks.first { it.isConflict }
        val key1 = conflictBlock.blockConflictKey
        val key2 = scheduleBlocks(listOf(b, a), maxPeriod = 6)
            .first { it.isConflict }.blockConflictKey
        assertEquals(key1, key2) // 顺序不影响 key
    }
}

private fun session(
    id: String,
    name: String,
    weekIndex: Int,
    weekday: Int,
    startUnit: Int,
    endUnit: Int,
) = Session(
    id = id,
    semesterId = 1,
    source = "eas",
    courseCode = id,
    courseName = name,
    weekIndex = weekIndex,
    weekday = weekday,
    startUnit = startUnit,
    endUnit = endUnit,
    date = null,
    startTime = null,
    endTime = null,
    teacherName = null,
    roomText = null,
    campusText = null,
    color = null,
)
