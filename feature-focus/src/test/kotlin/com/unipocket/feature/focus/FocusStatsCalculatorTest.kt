package com.unipocket.feature.focus

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusStatsCalculatorTest {

    private val utc: ZoneId = ZoneOffset.UTC
    private val today: LocalDate = LocalDate.of(2026, 11, 21)

    /** 当天中午 12 点（UTC）的毫秒数，避免时区边界抖动 */
    private fun millis(date: LocalDate, hour: Int = 12): Long =
        date.atStartOfDay(utc).toInstant().plusSeconds(hour * 3600L).toEpochMilli()

    private fun record(date: LocalDate, minutes: Int, hour: Int = 12) =
        FocusRecord(startedAtMillis = millis(date, hour), minutes = minutes)

    @Test
    fun `空记录给出全零与补零的 7 天`() {
        val stats = FocusStatsCalculator.compute(emptyList(), today, utc)

        assertEquals(0, stats.todayCount)
        assertEquals(0, stats.totalMinutes)
        assertEquals(7, stats.last7Days.size)
        assertEquals(today, stats.last7Days.last().date)
        assertTrue(stats.last7Days.all { it.minutes == 0 })
    }

    @Test
    fun `今日与累计分别聚合`() {
        val records = listOf(
            record(today, 25),
            record(today, 25),
            record(today.minusDays(1), 25),
        )
        val stats = FocusStatsCalculator.compute(records, today, utc)

        assertEquals(2, stats.todayCount)
        assertEquals(50, stats.todayMinutes)
        assertEquals(3, stats.totalCount)
        assertEquals(75, stats.totalMinutes)
    }

    @Test
    fun `7 天窗口按日期归桶且旧数据不计入`() {
        val records = listOf(
            record(today, 25),
            record(today.minusDays(6), 50),
            record(today.minusDays(7), 999), // 窗口外
        )
        val stats = FocusStatsCalculator.compute(records, today, utc)

        assertEquals(50, stats.last7Days.first().minutes)
        assertEquals(25, stats.last7Days.last().minutes)
        assertEquals(1074, stats.totalMinutes) // 累计不受窗口限制（25+50+999）
    }

    @Test
    fun `days 参数控制窗口长度`() {
        val stats = FocusStatsCalculator.compute(emptyList(), today, utc, days = 3)
        assertEquals(3, stats.last7Days.size)
    }
}
