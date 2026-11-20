package com.unipocket.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 学期：起始日 + 总周数 + 一周从哪天开始。
 *
 * [startDate] 允许给任意一天，框架会自动归一到"所在周的第一天"
 * （[weekStartOnSunday] 为 false 时是周一，为 true 时是周日）。
 * 这样接入方填"开学那天"即可，不用关心它是不是周首。
 */
data class Semester(
    /** 展示名，如 "2026-2027 第一学期" */
    val name: String,
    /** 学期起始日（第 1 周内任意一天均可） */
    val startDate: LocalDate,
    /** 总周数，至少 1 */
    val weekCount: Int,
    /** 一周从周日还是周一开始。多数国内高校是周一 */
    val weekStartOnSunday: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Semester.name 不能为空" }
        require(weekCount >= 1) { "Semester.weekCount 必须 >= 1" }
    }

    private val weekStartDay: DayOfWeek = if (weekStartOnSunday) DayOfWeek.SUNDAY else DayOfWeek.MONDAY

    /** 第 1 周的第一天（已按 [weekStartOnSunday] 归一） */
    val firstWeekStart: LocalDate by lazy {
        val offset = (startDate.dayOfWeek.value - weekStartDay.value + 7) % 7
        startDate.minusDays(offset.toLong())
    }

    /** 学期最后一周的最后一天 */
    val lastDate: LocalDate get() = firstWeekStart.plusDays((weekCount * 7L) - 1)

    /**
     * 第 [week] 周星期 [dayOfWeek] 的日期。
     * [dayOfWeek] 用 1=周一 … 7=周日；[weekStartOnSunday] 只影响"周"的边界，不改变星期的编号。
     */
    fun dateOf(week: Int, dayOfWeek: Int): LocalDate {
        require(week in 1..weekCount) { "week 必须在 1..$weekCount，给了 $week" }
        require(dayOfWeek in 1..7) { "dayOfWeek 必须在 1..7，给了 $dayOfWeek" }
        val dayOffset = if (weekStartOnSunday) dayOfWeek % 7 else dayOfWeek - 1
        return firstWeekStart.plusDays((week - 1) * 7L + dayOffset)
    }

    /** [date] 属于第几周；不在学期范围内返回 null */
    fun weekOf(date: LocalDate): Int? {
        val days = ChronoUnit.DAYS.between(firstWeekStart, date)
        if (days < 0) return null
        val week = (days / 7).toInt() + 1
        return week.takeIf { it <= weekCount }
    }

    /** 今天在第几周；不在学期内返回 null */
    fun currentWeek(today: LocalDate = LocalDate.now()): Int? = weekOf(today)
}
