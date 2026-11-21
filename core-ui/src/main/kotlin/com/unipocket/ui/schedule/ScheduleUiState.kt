package com.unipocket.ui.schedule

import com.unipocket.domain.Session
import com.unipocket.domain.UniPocketException
import java.time.Instant

/**
 * 课表页的完整界面状态。**只读快照**，由 [ScheduleStore] 产出。
 *
 * 设计上把"内容"和"错误"分开：[sessions] 可以是有数据的（缓存），同时 [error] 有值
 * （后台刷新失败）——这样界面能一边显示旧课表，一边提示"刷新失败，点此重试"，
 * 而不是把用户已经看到的课表清空。
 */
data class ScheduleUiState(
    /** 当前周，1 起 */
    val week: Int = 1,
    /** 学期总周数 */
    val weekCount: Int = 1,
    /** 当前周是否就是今天所在的周 */
    val isCurrentWeek: Boolean = true,
    /** 当前周要显示的课程（已按周过滤，每周一条 [Session]） */
    val sessions: List<Session> = emptyList(),
    /** 首次加载中：还没有任何数据可显示 */
    val isLoading: Boolean = false,
    /** 已有数据，正在后台刷新 */
    val isRefreshing: Boolean = false,
    /** 最近一次失败；成功后清空 */
    val error: UniPocketException? = null,
    /** 最近一次成功加载的时间 */
    val lastUpdated: Instant? = null,
) {
    /** 本周确实没课（不是加载中、也不是出错） */
    val isEmpty: Boolean get() = sessions.isEmpty() && !isLoading && error == null
}
