package com.unipocket.ui.schedule

import com.unipocket.domain.Course
import com.unipocket.domain.ScheduleSource
import com.unipocket.domain.SchoolConfig
import com.unipocket.domain.UniPocketException
import com.unipocket.domain.inWeek
import com.unipocket.ui.common.toUniPocketException
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
 */
class ScheduleStore(
    private val config: SchoolConfig,
    private val source: ScheduleSource,
    private val scope: CoroutineScope,
    /** 注入时钟，便于测试"今天在第几周" */
    private val clock: () -> LocalDate = { LocalDate.now() },
) {

    private val _state = MutableStateFlow(
        ScheduleUiState(
            week = initialWeek(),
            weekCount = config.semester.weekCount,
            isCurrentWeek = true,
        ),
    )
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    /** 全学期课程缓存；null 表示还没成功加载过 */
    private var allCourses: List<Course>? = null
    private var loadJob: Job? = null

    private fun todayWeek(): Int? = config.semester.weekOf(clock())

    private fun initialWeek(): Int =
        todayWeek()?.coerceIn(1, config.semester.weekCount) ?: 1

    /** 首次进入页面时调用；已有缓存则什么都不做 */
    fun start() {
        if (allCourses == null) load(force = false)
    }

    /** 强制重新拉取（下拉刷新 / 重试） */
    fun refresh() = load(force = true)

    /** 与 [refresh] 同义，给"加载失败 → 重试"按钮用 */
    fun retry() = refresh()

    fun setWeek(week: Int) {
        val target = week.coerceIn(1, config.semester.weekCount)
        _state.update {
            it.copy(
                week = target,
                isCurrentWeek = target == todayWeek(),
                courses = allCourses?.inWeek(target).orEmpty(),
            )
        }
        if (allCourses == null) load(force = false)
    }

    fun nextWeek() = setWeek(_state.value.week + 1)

    fun previousWeek() = setWeek(_state.value.week - 1)

    fun goToToday() = setWeek(initialWeek())

    private fun load(force: Boolean) {
        if (loadJob?.isActive == true) return
        if (!force && allCourses != null) return

        val hasCache = allCourses != null
        _state.update {
            it.copy(
                isLoading = !hasCache,
                isRefreshing = hasCache,
                error = null,
            )
        }

        loadJob = scope.launch {
            try {
                val courses = source.loadCourses()
                allCourses = courses
                val now = Instant.now()
                _state.update {
                    it.copy(
                        courses = courses.inWeek(it.week),
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
