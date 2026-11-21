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

/** 空教室页的界面状态 */
data class RoomUiState(
    val week: Int = 1,
    val dayOfWeek: Int = 1,
    val periodIndex: Int = 1,
    val periodCount: Int = 1,
    val buildings: List<LocationNode> = emptyList(),
    val selectedBuildingId: String? = null,
    val groups: List<RoomGroup> = emptyList(),
    /** 占用矩阵：room.id → 该教室空闲的节次集合（1..periodCount 的子集） */
    val freeMatrix: Map<String, Set<Int>> = emptyMap(),
    /** 所有已知教室（从地点树叶子展开），按楼分组 */
    val allRooms: List<RoomGroup> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: com.unipocket.domain.UniPocketException? = null,
) {
    val isEmpty: Boolean get() = allRooms.isEmpty() && !isLoading && error == null
}

data class RoomGroup(
    val building: LocationNode?,
    val rooms: List<LocationNode>,
) {
    val title: String get() = building?.name ?: "其他"
}

/**
 * 空教室状态机（ZZUPocket 占用矩阵版）。
 *
 * 查询策略：对每一节（1..periodCount）分别调 [RoomSource.freeRooms]，
 * 构建「教室 → 空闲节次集合」的占用矩阵。这样界面能画出一行一间教室、
 * 一列一节课的占用格子（绿=空闲，红=有课）。
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
    private var allRooms: List<LocationNode> = emptyList()
    private var queryJob: Job? = null

    fun start() {
        if (tree == null) loadTree() else query()
    }

    fun refresh() { if (tree == null) loadTree() else query() }
    fun retry() = refresh()

    fun setDayOfWeek(dayOfWeek: Int) {
        if (dayOfWeek !in 1..7) return
        _state.update { it.copy(dayOfWeek = dayOfWeek) }
        query()
    }

    fun setPeriod(periodIndex: Int) {
        if (periodIndex !in 1..config.periodCount) return
        _state.update { it.copy(periodIndex = periodIndex) }
    }

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
                allRooms = root.flatten().filter { it.isLeaf }.toList()
                _state.update { it.copy(buildings = root.children, isLoading = false) }
                query()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.toUniPocketException()) }
            }
        }
    }

    /** 对每一节分别查询，构建占用矩阵 */
    private fun query() {
        val root = tree ?: return
        queryJob?.cancel()
        val snapshot = _state.value
        _state.update { it.copy(isLoading = it.freeMatrix.isEmpty(), isRefreshing = it.freeMatrix.isNotEmpty(), error = null) }

        queryJob = scope.launch {
            try {
                val matrix = mutableMapOf<String, MutableSet<Int>>()
                // 初始化：所有已知教室
                allRooms.forEach { room -> matrix[room.id] = mutableSetOf() }
                // 每节查一次
                for (p in 1..snapshot.periodCount) {
                    val free = source.freeRooms(snapshot.week, snapshot.dayOfWeek, p..p)
                    free.forEach { room -> matrix[room.id]?.add(p) }
                }
                _state.update {
                    it.copy(freeMatrix = matrix, isLoading = false, isRefreshing = false)
                }
                applyBuildingFilter()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.toUniPocketException()) }
            }
        }
    }

    private fun applyBuildingFilter() {
        val selected = _state.value.selectedBuildingId
        val buildingGroups = groupByBuilding(tree ?: return, allRooms)
        _state.update { it.copy(allRooms = buildingGroups) }
        val visible = if (selected == null) buildingGroups else buildingGroups.filter { it.building?.id == selected }
        _state.update { it.copy(groups = visible) }
    }

    private fun groupByBuilding(root: LocationNode, rooms: List<LocationNode>): List<RoomGroup> {
        val order = root.children.mapIndexed { index, node -> node.id to index }.toMap()
        return rooms
            .groupBy { room -> root.pathTo(room.id)?.getOrNull(1) }
            .map { (building, list) -> RoomGroup(building = building, rooms = list.sortedBy { it.name }) }
            .sortedBy { group -> order[group.building?.id] ?: Int.MAX_VALUE }
    }
}
