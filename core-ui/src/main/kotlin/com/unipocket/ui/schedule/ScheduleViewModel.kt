package com.unipocket.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.unipocket.domain.EditableScheduleSource
import com.unipocket.domain.ScheduleSource
import com.unipocket.domain.Semester
import com.unipocket.domain.Session
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * 官方课表 ViewModel：把 [ScheduleStore] 绑到页面生命周期上。
 *
 * 页面销毁时 `viewModelScope` 自动取消，进行中的请求随之中断。
 * 用 [factory] 创建，接入方不需要自己写 Factory：
 *
 * ```kotlin
 * val vm: ScheduleViewModel = viewModel(
 *     factory = ScheduleViewModel.factory(semester, MyScheduleSource()),
 * )
 * val state by vm.state.collectAsStateWithLifecycle()
 * ```
 */
class ScheduleViewModel(
    semester: Semester,
    source: ScheduleSource,
    clock: () -> LocalDate = { LocalDate.now() },
    editableSource: EditableScheduleSource? = null,
) : ViewModel() {

    private val store = ScheduleStore(semester, source, viewModelScope, clock, editableSource)

    val state: StateFlow<ScheduleUiState> = store.state

    init {
        store.start()
    }

    fun refresh() = store.refresh()

    fun retry() = store.retry()

    fun setWeek(week: Int) = store.setWeek(week)

    fun nextWeek() = store.nextWeek()

    fun previousWeek() = store.previousWeek()

    fun goToToday() = store.goToToday()

    /** 手动补录/编辑；数据源不支持时为 no-op */
    fun upsertSessions(sessions: List<Session>) = store.upsertSessions(sessions)

    /** 删除一条上课记录；数据源不支持时为 no-op */
    fun deleteSession(id: String) = store.deleteSession(id)

    companion object {
        fun factory(
            semester: Semester,
            source: ScheduleSource,
            clock: () -> LocalDate = { LocalDate.now() },
            editableSource: EditableScheduleSource? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ScheduleViewModel(semester, source, clock, editableSource) }
        }
    }
}
