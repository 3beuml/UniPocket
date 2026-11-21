package com.unipocket.feature.focus

/** 番茄钟阶段完成事件。UI/存储层据此落统计，不侵入状态机本身。 */
sealed interface FocusEvent {
    /** 一次专注完成；[startedAtMillis] 为该次专注的开始时刻 */
    data class WorkCompleted(
        val startedAtMillis: Long,
        val durationMillis: Long,
    ) : FocusEvent
}
