package com.unipocket.ui.schedule

import org.junit.Assert.assertEquals
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
        colorSeed = 0, week = week, dayOfWeek = day,
        startPeriod = start, endPeriod = end,
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
    fun `expandByWeek 只展开该周有课的条目`() {
        val courses = listOf(
            com.unipocket.domain.Course(
                id = "a", name = "高数",
                weeks = setOf(1, 2, 3), dayOfWeek = 1, periods = 1..2,
            ),
            com.unipocket.domain.Course(
                id = "b", name = "体育",
                weeks = setOf(5), dayOfWeek = 1, periods = 3..4,
            ),
        )
        val week1 = courses.expandByWeek(1)
        assertEquals(1, week1.size)
        assertEquals("a#1", week1[0].id)

        val week5 = courses.expandByWeek(5)
        assertEquals(1, week5.size)
        assertEquals("b#5", week5[0].id)

        val week10 = courses.expandByWeek(10)
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

private fun assertFalse(b: Boolean) = org.junit.Assert.assertFalse(b)
