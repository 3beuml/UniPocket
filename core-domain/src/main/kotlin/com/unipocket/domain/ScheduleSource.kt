package com.unipocket.domain

/**
 * 课表数据来源。外校接入时**只需要实现这个接口**，界面代码不用动。
 *
 * 约定：
 * - 返回整个学期的全部上课安排（每条 Session 对应一周中的某个时段）。
 * - 同一节课多次调用必须返回稳定的 [Session.id]。
 * - 失败时抛 [UniPocketException] 的子类，界面据此统一展示。
 * - 不要求实现缓冲/缓存；框架的 ViewModel 负责"先显示缓存再刷新"。
 */
interface ScheduleSource {
    /** 来源标识，用于日志与多个来源的去重归因，如 "eas" / "gms" / "manual" */
    val id: String

    suspend fun loadSessions(): List<Session>
}

/**
 * 支持增删改的数据源 —— 手动补录一般实现它。
 */
interface EditableScheduleSource : ScheduleSource {
    /** 新增或按 id 覆盖 */
    suspend fun upsert(session: Session)
    /** 删除本来源管理的课 */
    suspend fun delete(id: String)
    /** 该 id 是否由本来源管理 */
    suspend fun manages(id: String): Boolean
}

/**
 * 把多个来源拼成一个。去重规则：课名 + 星期 + 起止节次相同即视为同一节课。
 */
class MergedScheduleSource(vararg sources: ScheduleSource) : ScheduleSource {

    private val sources: List<ScheduleSource> = sources.toList()

    init {
        require(this.sources.isNotEmpty()) { "MergedScheduleSource 至少需要一个来源" }
    }

    override val id: String = this.sources.joinToString("+") { it.id }

    override suspend fun loadSessions(): List<Session> =
        sources
            .flatMap { it.loadSessions() }
            .distinctBy { listOf(it.courseName, it.weekday, it.startUnit, it.endUnit, it.weekIndex) }
}
