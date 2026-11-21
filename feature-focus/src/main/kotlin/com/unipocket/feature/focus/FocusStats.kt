package com.unipocket.feature.focus

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 聚合后的专注统计 */
data class FocusStats(
    /** 今日完成个数 */
    val todayCount: Int,
    /** 今日专注分钟数 */
    val todayMinutes: Int,
    /** 累计个数 */
    val totalCount: Int,
    /** 累计分钟数 */
    val totalMinutes: Int,
    /** 最近 [days] 天（含今天，旧 → 新）；没记录的天为 0 分钟，柱状图不缺列 */
    val last7Days: List<DayMinutes>,
) {
    val days: Int get() = last7Days.size
}

/** 一天的专注分钟数 */
data class DayMinutes(val date: LocalDate, val minutes: Int)

/**
 * 专注统计的**纯计算**：记录 → [FocusStats]。
 *
 * 归属规则：一条记录按其开始时刻（设备时区）落在哪一天就算哪一天；
 * 跨零点的专注归属开始那天——统计口径宁可简单可预期。
 */
object FocusStatsCalculator {

    fun compute(
        records: List<FocusRecord>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        days: Int = 7,
    ): FocusStats {
        require(days >= 1) { "days 至少为 1" }

        fun dayOf(record: FocusRecord): LocalDate =
            Instant.ofEpochMilli(record.startedAtMillis).atZone(zone).toLocalDate()

        val todayRecords = records.filter { dayOf(it) == today }
        val lastDays = (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DayMinutes(
                date = date,
                minutes = records.filter { dayOf(it) == date }.sumOf { it.minutes },
            )
        }

        return FocusStats(
            todayCount = todayRecords.size,
            todayMinutes = todayRecords.sumOf { it.minutes },
            totalCount = records.size,
            totalMinutes = records.sumOf { it.minutes },
            last7Days = lastDays,
        )
    }
}
