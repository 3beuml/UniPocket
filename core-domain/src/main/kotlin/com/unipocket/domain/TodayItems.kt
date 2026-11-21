package com.unipocket.domain

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** 今天某节课相对当前时刻的状态 */
enum class TodayStatus { ONGOING, UPCOMING, FINISHED }

/**
 * 离 [target] 还有多少分钟（同一天内，向后看）；[now] 晚于 target 时为负。
 * 只用于展示层文案，不做跨天折算。
 */
fun minutesBetween(now: LocalTime, target: LocalTime): Long =
    ChronoUnit.MINUTES.between(now, target)

/** 人类可读的时长："45分钟" / "2小时5分" / "2小时" */
fun humanMinutes(totalMinutes: Long): String = when {
    totalMinutes >= 60 -> {
        val hours = totalMinutes / 60
        val rest = totalMinutes % 60
        if (rest == 0L) "${hours}小时" else "${hours}小时${rest}分"
    }
    else -> "${totalMinutes}分钟"
}

/** 今天时间轴上的一条：上课记录 + 真实起止时刻 + 状态 */
data class TodayItem(
    val session: Session,
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

    /**
     * 相对当前时刻的文案："还剩 1小时20分" / "25分钟后开始" / "已结束"。
     * 0 分钟时分别说 "马上下课" / "马上开始"，避免出现 "还剩 0分钟"。
     */
    fun relativeText(now: LocalTime): String = when (status) {
        TodayStatus.ONGOING -> {
            val rest = minutesBetween(now, end)
            if (rest <= 0) "马上下课" else "还剩 ${humanMinutes(rest)}"
        }
        TodayStatus.UPCOMING -> {
            val wait = minutesBetween(now, start)
            if (wait <= 0) "马上开始" else "${humanMinutes(wait)}后开始"
        }
        TodayStatus.FINISHED -> "已结束"
    }

    private companion object {
        val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

/**
 * 把某一天的课程整理成今天时间轴。
 *
 * - 只保留 [dayOfWeek] 当天的课；**调用方先按教学周过滤**
 *   （`sessions.filter { it.weekIndex == week }`），否则其它周的课会漏进来；
 * - 有 `date` 的记录（调休/补课）时间仍按节次表解析——调休课的"今天"归属由调用方按日期判断；
 * - 节次在 [periods] 里查不到、[Session] 也没带起止时刻的条目会被跳过（数据不完整时不画错位置）；
 * - 按开始时刻排序；
 * - [now] 决定每条的 [TodayStatus]：结束时刻之前算"进行中"，之后算"已结束"。
 */
fun List<Session>.todayItems(
    periods: List<Period>,
    dayOfWeek: Int,
    now: LocalTime,
): List<TodayItem> =
    filter { it.weekday == dayOfWeek }
        .mapNotNull { session ->
            val start = resolveTime(periods, session.startUnit, session.startTime)
                ?: return@mapNotNull null
            val end = resolveTime(periods, session.endUnit, session.endTime)
                ?: return@mapNotNull null
            val status = when {
                now >= end -> TodayStatus.FINISHED
                now >= start -> TodayStatus.ONGOING
                else -> TodayStatus.UPCOMING
            }
            TodayItem(session = session, start = start, end = end, status = status)
        }
        .sortedBy { it.start }

/**
 * 「下一节课」卡片槽的挑选规则：第一门**还没结束**的课；
 * 全部结束时返回 null（此时卡片不显示，时间轴照常列出）。
 */
fun List<TodayItem>.nextUpcoming(): TodayItem? = firstOrNull { it.status != TodayStatus.FINISHED }

/** 节次表查得到就按表（`800` = 08:00），查不到退回 Session 自带的 "08:00" 文本 */
private fun resolveTime(periods: List<Period>, unit: Int, fallback: String?): LocalTime? {
    val period = periods.firstOrNull { it.indexNo == unit }
    if (period != null) return LocalTime.of(period.startTime / 100, period.startTime % 100)
    return fallback?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
}
