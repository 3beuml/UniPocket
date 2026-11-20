package com.unipocket.domain

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** 今天某节课相对当前时刻的状态 */
enum class TodayStatus { ONGOING, UPCOMING, FINISHED }

/** 今天时间轴上的一条：课程 + 真实起止时刻 + 状态 */
data class TodayItem(
    val course: Course,
    val start: LocalTime,
    val end: LocalTime,
    val status: TodayStatus,
) {
    /** "08:00-09:40" */
    val timeText: String get() = "${start.format(HH_MM)}-${end.format(HH_MM)}"

    val statusLabel: String
        get() = when (status) {
            TodayStatus.ONGOING -> "进行中"
            TodayStatus.UPCOMING -> "即将开始"
            TodayStatus.FINISHED -> "已结束"
        }

    private companion object {
        val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

/**
 * 把某一天的课程整理成今天时间轴。
 *
 * - 只保留 [dayOfWeek] 当天的课；
 * - 节次在 [SchoolConfig] 里查不到起止时间的条目会被跳过（数据不完整时不画错位置）；
 * - 按开始时刻排序；
 * - [now] 决定每条的 [TodayStatus]：结束时刻之前算"进行中"，之后算"已结束"。
 */
fun List<Course>.todayItems(
    config: SchoolConfig,
    dayOfWeek: Int,
    now: LocalTime,
): List<TodayItem> =
    filter { it.dayOfWeek == dayOfWeek }
        .mapNotNull { course ->
            val start = config.period(course.startPeriod)?.start ?: return@mapNotNull null
            val end = config.period(course.endPeriod)?.end ?: return@mapNotNull null
            val status = when {
                now >= end -> TodayStatus.FINISHED
                now >= start -> TodayStatus.ONGOING
                else -> TodayStatus.UPCOMING
            }
            TodayItem(course = course, start = start, end = end, status = status)
        }
        .sortedBy { it.start }
