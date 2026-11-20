package com.unipocket.domain

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 一节课的时间信息。由接入方在 [SchoolConfig] 里填写。
 *
 * 学校专有信息（"第几大节"、上午/下午/晚上、是否连堂）走 [attributes]，
 * 框架只依赖 [index] 与起止时间。
 */
data class Period(
    /** 节次序号，从 1 开始，必须连续 */
    val index: Int,
    val start: LocalTime,
    val end: LocalTime,
    /** 可选的显示名，如 "第1节"；为空时界面用序号 */
    val name: String? = null,
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(index >= 1) { "Period.index 必须 >= 1" }
        require(start < end) { "Period 起止时间非法：$index $start-$end" }
    }

    val startText: String get() = start.format(HH_MM)
    val endText: String get() = end.format(HH_MM)

    /** "08:00-08:45" */
    val rangeText: String get() = "$startText-$endText"

    private companion object {
        val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

/**
 * 一所学校的静态配置：节次表 + 学期 + 展示名。
 *
 * 外校接入只需要在 demo 里替换这个对象（以及 [ScheduleSource]），界面代码不用改。
 */
data class SchoolConfig(
    /** 学校名，仅用于界面标题与日志 */
    val schoolName: String,
    /** 节次表，按 [Period.index] 升序；必须从 1 开始且连续 */
    val periods: List<Period>,
    val semester: Semester,
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(schoolName.isNotBlank()) { "SchoolConfig.schoolName 不能为空" }
        require(periods.isNotEmpty()) { "SchoolConfig.periods 不能为空" }
        val indices = periods.map { it.index }.sorted()
        require(indices == (1..indices.size).toList()) {
            "SchoolConfig.periods 必须从 1 开始连续编号，实际为 $indices"
        }
        require(periods.all { it.start < it.end }) { "SchoolConfig.periods 存在非法起止时间" }
    }

    /** 节次数 */
    val periodCount: Int get() = periods.size

    /** 按序号取节次；不存在返回 null */
    fun period(index: Int): Period? = periods.firstOrNull { it.index == index }

    /** 某一节开始/结束时刻；越界返回 null */
    fun timeOf(periodIndex: Int, start: Boolean): LocalTime? =
        period(periodIndex)?.let { if (start) it.start else it.end }
}
