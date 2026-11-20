package com.unipocket.domain

/**
 * 支持增删改的数据源 —— 手动补录一般实现它。
 *
 * 约定：
 * - [upsert] 按 [Course.id] 覆盖：新课程由接入方生成稳定 id（如 "manual-uuid"）。
 * - [delete] 只删除本来源管理的课；对教务课应返回而不报错。
 * - 持久化完全归实现者：框架不定义表、不碰数据库。
 * - 教务课的"改教室/改时间"不走这里——那需要覆盖层概念，v0.1 明确不做；
 *   教务变化应由教务来源自身刷新。
 */
interface EditableScheduleSource : ScheduleSource {

    /** 新增或按 id 覆盖 */
    suspend fun upsert(course: Course)

    /** 删除本来源管理的课 */
    suspend fun delete(id: String)

    /** 该 id 是否由本来源管理；详情页据此决定显示"编辑/删除"还是只读 */
    suspend fun manages(id: String): Boolean
}
