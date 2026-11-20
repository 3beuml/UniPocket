package com.unipocket.ui.room

import com.unipocket.domain.LocationNode
import com.unipocket.domain.UniPocketException

/**
 * 空教室页的界面状态。**只读快照**，由 [RoomStore] 产出。
 */
data class RoomUiState(
    /** 查询的教学周，1 起 */
    val week: Int = 1,
    /** 查询的星期，1=周一 … 7=周日 */
    val dayOfWeek: Int = 1,
    /** 查询的节次（单节） */
    val periodIndex: Int = 1,
    /** 节次总数，用于渲染选择器 */
    val periodCount: Int = 1,
    /** 可选楼栋（地点树第一层） */
    val buildings: List<LocationNode> = emptyList(),
    /** 选中楼栋；null = 全部 */
    val selectedBuildingId: String? = null,
    /** 按楼栋分组的结果 */
    val groups: List<RoomGroup> = emptyList(),
    /** 首次加载中（还没有任何结果） */
    val isLoading: Boolean = false,
    /** 已有结果，正在重新查询 */
    val isRefreshing: Boolean = false,
    val error: UniPocketException? = null,
) {
    val isEmpty: Boolean get() = groups.isEmpty() && !isLoading && error == null
}

/** 一栋楼及其空闲教室 */
data class RoomGroup(
    val building: LocationNode?,
    val rooms: List<LocationNode>,
) {
    /** 该组展示标题 */
    val title: String get() = building?.name ?: "其他"
}
