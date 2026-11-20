package com.unipocket.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeeksTextTest {

    @Test
    fun `解析区间与枚举`() {
        assertEquals(setOf(1, 2, 3), parseWeeksText("1-3"))
        assertEquals(setOf(1, 3, 5, 6, 7), parseWeeksText("1,3,5-7"))
        assertEquals(setOf(2, 4, 6), parseWeeksText("2 4 6"))
        assertEquals(setOf(10), parseWeeksText("10"))
        assertEquals(setOf(2, 4), parseWeeksText("2、4"))
    }

    @Test
    fun `非法输入整体拒绝`() {
        assertNull(parseWeeksText(""))
        assertNull(parseWeeksText("   "))
        assertNull(parseWeeksText("abc"))
        assertNull(parseWeeksText("1,3,x"))
        assertNull(parseWeeksText("5-2"))
        assertNull(parseWeeksText("0-3"))
    }

    @Test
    fun `周次文本往返一致`() {
        val weeks = setOf(1, 2, 3, 5, 6)
        assertEquals(weeks, parseWeeksText(weeksToText(weeks)))
        assertEquals("1-3,5-6", weeksToText(weeks))
    }
}
