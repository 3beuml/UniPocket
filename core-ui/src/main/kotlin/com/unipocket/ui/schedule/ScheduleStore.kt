package com.unipocket.ui.schedule

import com.unipocket.domain.EditableScheduleSource
import com.unipocket.domain.ScheduleSource
import com.unipocket.domain.Semester
import com.unipocket.domain.Session
import com.unipocket.domain.UniPocketException
import com.unipocket.domain.WeekAlignment
import com.unipocket.domain.toUniPocketException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/**
 * 课表状态机：加载、缓存、翻周、错误。**不依赖 Android**，可直接单测。
 *
 * 行为约定：
 * - 首次 [start] 拉全学期数据并缓存；之后翻周只在内存里过滤，不再请求。
 * - [refresh] 强制重拉；失败时**保留已有数据**，只设置 [ScheduleUiState.error]。
 * - 同一时刻只允许一个加载任务，重复调用会被忽略。
 * - 所有底层异常都会归一成 [UniPocketException]。
 *
 * 周次语义：今天的周次由 [WeekAlignment] 从 [Semester.startDate] 推算，
 * 推不出（开学前/寒暑假/没给起始日）落到学期第一周。
 */
class ScheduleStore(
    private val semester: Semester,
    private val source: ScheduleSource,
    private val scope: CoroutineScope,
    /** 注入时钟，便于测试"今天在第几周" */
    private val clock: () -> LocalDate = { LocalDate.now() },
    /**
     * 编辑通道。读取走 [source]（可能是合并后的多个来源），
     * 补录/删除走这里（手动补录源）——两者刻意分开，避免"合并源不可编辑"把编辑静默吞掉。
     */
    private val editableSource: EditableScheduleSource? = null,
) {

    /**
     * 周次显示的合法区间（学期第 1 周..最后一周）。
     * ⚠️ 必须声明在 [_state] 之前：[_state] 的初值走 [initialWeek]，会读这两个值——
     * Kotlin 属性按声明顺序初始化，放在后面读到的是未初始化的 0。
     */
    private val firstWeek: Int = semester.firstWeek ?: 1
    private val lastWeek: Int = semester.lastWeek ?: semester.weekCount

    private val _state = MutableStateFlow(
        ScheduleUiState(
            week = initialWeek(),
            weekCount = semester.weekCount,
            isCurrentWeek = true,
        ),
    )
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    /** 全学期课程缓存；null 表示还没成功加载过 */
    private var allSessions: List<Session>? = null
    private var loadJob: Job? = null

    private fun todayWeek(): Int? =
        WeekAlignment.indexOf(semester.startDate, semester.weekStartOnSunday, semester.weekIndices, clock())

    private fun initialWeek(): Int =
        WeekAlignment.currentWeek(todayWeek(), semester.firstWeek).coerceIn(firstWeek, lastWeek)

    /** 首次进入页面时调用；已有缓存则什么都不做 */
    fun start() {
        if (allSessions == null) load(force = false)
    }

    /** 强制重新拉取（下拉刷新 / 重试） */
    fun refresh() = load(force = true)

    /** 与 [refresh] 同义，给"加载失败 → 重试"按钮用 */
    fun retry() = refresh()

    fun setWeek(week: Int) {
        val target = week.coerceIn(firstWeek, lastWeek)
        _state.update {
            it.copy(
                week = target,
                isCurrentWeek = target == todayWeek(),
                sessions = allSessions?.filter { s -> s.weekIndex == target }.orEmpty(),
            )
        }
        if (allSessions == null) load(force = false)
    }

    fun nextWeek() = setWeek(_state.value.week + 1)

    fun previousWeek() = setWeek(_state.value.week - 1)

    fun goToToday() = setWeek(initialWeek())

    /**
     * 手动补录/编辑。一门课覆盖 N 周 → 传 N 条 [Session]（同 [Session.courseCode]、
     * 各自的 [Session.weekIndex]）。仅在数据源实现 [EditableScheduleSource] 时生效；
     * 写入成功后强制刷新，失败时把错误放进 [ScheduleUiState.error]（不清空已有课表）。
     */
    fun upsertSessions(sessions: List<Session>) = edit { source -> sessions.forEach { source.upsert(it) } }

    /** 删除一条上课记录；语义同 [upsertSessions] */
    fun deleteSession(id: String) = edit { it.delete(id) }

    private fun edit(block: suspend (EditableScheduleSource) -> Unit) {
        val editable = editableSource ?: (source as? EditableScheduleSource) ?: return
        scope.launch {
            try {
                block(editable)
                load(force = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(error = e.toUniPocketException()) }
            }
        }
    }

    private fun load(force: Boolean) {
        if (loadJob?.isActive == true) return
        if (!force && allSessions != null) return

        val hasCache = allSessions != null
        _state.update {
            it.copy(
                isLoading = !hasCache,
                isRefreshing = hasCache,
                error = null,
            )
        }

        loadJob = scope.launch {
            try {
                val sessions = source.loadSessions()
                allSessions = sessions
                val now = Instant.now()
                _state.update {
                    it.copy(
                        sessions = sessions.filter { s -> s.weekIndex == it.week },
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        lastUpdated = now,
                    )
                }
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
}
