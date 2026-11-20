package com.unipocket.domain

/**
 * 一次上课安排 —— **一个时间段**，不是"一门课"。
 *
 * 同一门课有多个时间段（例如周二 1-2 节 + 周四 3-4 节）时，会产生多条 [Course]。
 * 这样建模的原因：周次、教室、教师在每个时间段都可能不同，拆开之后
 * 课表网格、冲突检测、小组件都只需要处理"一条 = 一格"的简单形状。
 *
 * 各校专有信息（校区、实验课标记、课程性质、学分、开课院系……）一律放进
 * [attributes]，**不要**往这里加学校语义字段。
 */
data class Course(
    /**
     * 稳定标识：同一节课每次加载都必须得到相同的值。
     * 手动补录/编辑、去重、界面局部刷新都依赖它。
     */
    val id: String,
    val name: String,
    /** 教师；多教师时由适配器自行拼接 */
    val teacher: String? = null,
    /** 教室文本；校区信息放 [attributes]，不要拼进来 */
    val room: String? = null,
    /** 上课的教学周，1 起。空集合视为适配器错误 */
    val weeks: Set<Int>,
    /** 星期几：1=周一 … 7=周日（ISO-8601） */
    val dayOfWeek: Int,
    /** 节次区间，如 1..2；非连续节次请拆成多条 Course */
    val periods: IntRange,
    /** 配色种子；默认按课名取，保证同一门课颜色稳定 */
    val colorSeed: Int = name.hashCode(),
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(id.isNotBlank()) { "Course.id 不能为空" }
        require(name.isNotBlank()) { "Course.name 不能为空" }
        require(weeks.isNotEmpty()) { "Course.weeks 不能为空：$name" }
        require(weeks.all { it >= 1 }) { "Course.weeks 必须从 1 开始：$name $weeks" }
        require(dayOfWeek in 1..7) { "Course.dayOfWeek 必须在 1..7：$name 给了 $dayOfWeek" }
        require(!periods.isEmpty()) { "Course.periods 不能为空：$name" }
        require(periods.first >= 1) { "Course.periods 必须从 1 开始：$name $periods" }
    }

    val startPeriod: Int get() = periods.first
    val endPeriod: Int get() = periods.last

    /** 该课是否在本周有课 */
    fun isInWeek(week: Int): Boolean = week in weeks

    /** 详情页展示用的周次串，如 "1-8,10-16" */
    val weeksText: String get() = weeks.sorted().toRangesText()

    /**
     * 合并去重用的身份：忽略 [id] 与 [attributes]。
     * 同一条课程从两个来源拿到时 [id] 可能不同，但内容相同，应该只留一份。
     */
    internal fun dedupKey(): List<Any?> =
        listOf(name, teacher, room, weeks.sorted(), dayOfWeek, startPeriod, endPeriod)
}

/** 只保留本周有课的条目 */
fun List<Course>.inWeek(week: Int): List<Course> = filter { it.isInWeek(week) }

/** [1,2,3,5,6] -> "1-3,5-6"；用于展示与编辑框的周次文本 */
fun weeksToText(weeks: Collection<Int>): String = weeks.sorted().toRangesText()

/**
 * "1-16" / "1,3,5-7" / "2 4 6" → 周次集合；无法解析返回 null。
 *
 * 编辑框的周次自定义输入用它解析：任何非法片段（负数、倒序、非数字）
 * 都整体拒绝，不做"尽量解析"，避免用户以为存上了实际没存上。
 */
fun parseWeeksText(text: String): Set<Int>? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null

    val result = mutableSetOf<Int>()
    for (token in trimmed.split(Regex("[,，、\\s]+"))) {
        if (token.isBlank()) continue
        val match = WEEK_TOKEN.find(token) ?: return null
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[3].ifEmpty { match.groupValues[1] }.toInt()
        if (start < 1 || end < start) return null
        result += start..end
    }
    return result.takeIf { it.isNotEmpty() }
}

private val WEEK_TOKEN = Regex("^(\\d+)(-(\\d+))?$")

/** [1,2,3,5,6] -> "1-3,5-6" */
internal fun List<Int>.toRangesText(): String {
    if (isEmpty()) return ""
    val sorted = sorted().distinct()
    val parts = mutableListOf<String>()
    var start = sorted.first()
    var prev = start
    for (w in sorted.drop(1)) {
        if (w == prev + 1) {
            prev = w
        } else {
            parts += if (start == prev) "$start" else "$start-$prev"
            start = w
            prev = w
        }
    }
    parts += if (start == prev) "$start" else "$start-$prev"
    return parts.joinToString(",")
}
