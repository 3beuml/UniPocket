package com.unipocket.domain

/**
 * 课表数据来源。外校接入时**只需要实现这个接口**，界面代码不用动。
 *
 * 约定：
 * - 返回整个学期的全部上课安排（不按周过滤；按周取用 [inWeek]）。
 * - 同一节课多次调用必须返回稳定的 [Course.id]。
 * - 失败时抛 [UniPocketException] 的子类，界面据此统一展示。
 * - 不要求实现缓冲/缓存；框架的 ViewModel 负责"先显示缓存再刷新"。
 */
interface ScheduleSource {
    /** 来源标识，用于日志与多个来源的去重归因，如 "eas" / "gms" / "manual" */
    val id: String

    suspend fun loadCourses(): List<Course>
}

/**
 * 把多个来源拼成一个。
 *
 * 去重规则见 [Course.dedupKey]：课名、教师、教室、周次、星期、节次全部相同即视为同一节课，
 * 保留**先出现的那个来源**的版本。任一来源抛异常则整体失败——避免"部分数据看起来正常、
 * 实际缺了一半"更难排查。
 */
class MergedScheduleSource(vararg sources: ScheduleSource) : ScheduleSource {

    private val sources: List<ScheduleSource> = sources.toList()

    init {
        require(this.sources.isNotEmpty()) { "MergedScheduleSource 至少需要一个来源" }
    }

    override val id: String = this.sources.joinToString("+") { it.id }

    override suspend fun loadCourses(): List<Course> =
        sources
            .flatMap { it.loadCourses() }
            .distinctBy { it.dedupKey() }
}
