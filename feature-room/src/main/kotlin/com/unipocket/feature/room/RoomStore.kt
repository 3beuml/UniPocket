package com.unipocket.feature.room

import com.unipocket.domain.LocationNode
import com.unipocket.domain.RoomSource
import com.unipocket.domain.SchoolConfig
import com.unipocket.domain.toUniPocketException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 空教室状态机：地点树只拉一次，筛选条件变化时重新查询空闲教室。
 *
 * 行为约定：
 * - 切节次/星期会**取消上一次查询**，只保留最新结果（避免慢响应覆盖新条件）。
 * - 切楼栋不重新请求，只在已有结果里过滤。
 * - 查询失败**保留上一次结果**并暴露错误，界面同时显示结果与错误条。
 * - 不依赖 Android，可直接单测。
 */
class RoomStore(
    private val config: SchoolConfig,
    private val source: RoomSource,
    private val scope: CoroutineScope,
    private val clock: () -> LocalDate = { LocalDate.now() },
) {

    private val today: LocalDate get() = clock()

    private val _state = MutableStateFlow(
        RoomUiState(
            week = config.semester.weekOf(today) ?: 1,
            dayOfWeek = today.dayOfWeek.value,
            periodIndex = 1,
            periodCount = config.periodCount,
        ),
    )
    val state: StateFlow<RoomUiState> = _state.asStateFlow()

    private var tree: LocationNode? = null
    /** 未经楼栋过滤的完整结果 */
    private var allGroups: List<RoomGroup> = emptyList()
    private var queryJob: Job? = null

    /** 首次进入页面时调用 */
    fun start() {
        if (tree == null) loadTree() else query()
    }

    fun refresh() {
        if (tree == null) loadTree() else query()
    }

    fun retry() = refresh()

    fun setDayOfWeek(dayOfWeek: Int) {
        if (dayOfWeek !in 1..7) return
        _state.update { it.copy(dayOfWeek = dayOfWeek) }
        query()
    }

    fun setPeriod(periodIndex: Int) {
        if (periodIndex !in 1..config.periodCount) return
        _state.update { it.copy(periodIndex = periodIndex) }
        query()
    }

    /** 只在本地过滤，不重新请求 */
    fun setBuilding(buildingId: String?) {
        _state.update { it.copy(selectedBuildingId = buildingId) }
        applyBuildingFilter()
    }

    private fun loadTree() {
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            try {
                val root = source.locations()
                tree = root
                _state.update { it.copy(buildings = root.children, isLoading = false) }
                query()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.toUniPocketException()) }
            }
        }
    }

    private fun query() {
        val root = tree ?: return
        queryJob?.cancel()

        val snapshot = _state.value
        _state.update {
            it.copy(
                isLoading = it.groups.isEmpty(),
                isRefreshing = it.groups.isNotEmpty(),
                error = null,
            )
        }

        queryJob = scope.launch {
            try {
                val rooms = source.freeRooms(
                    week = snapshot.week,
                    dayOfWeek = snapshot.dayOfWeek,
                    periods = snapshot.periodIndex..snapshot.periodIndex,
                )
                allGroups = groupByBuilding(root, rooms)
                applyBuildingFilter()
                _state.update { it.copy(isLoading = false, isRefreshing = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.toUniPocketException(),
                    )
                }
            }
        }
    }

    private fun applyBuildingFilter() {
        val selected = _state.value.selectedBuildingId
        val visible = if (selected == null) {
            allGroups
        } else {
            allGroups.filter { it.building?.id == selected }
        }
        _state.update { it.copy(groups = visible) }
    }

    /** 按地点树第一层（楼栋）分组；不依赖房间 id 的命名规则 */
    private fun groupByBuilding(root: LocationNode, rooms: List<LocationNode>): List<RoomGroup> {
        val order = root.children.mapIndexed { index, node -> node.id to index }.toMap()

        return rooms
            .groupBy { room -> root.pathTo(room.id)?.getOrNull(1) }
            .map { (building, list) ->
                RoomGroup(
                    building = building,
                    rooms = list.sortedBy { it.name },
                )
            }
            .sortedBy { group -> order[group.building?.id] ?: Int.MAX_VALUE }
    }
}
