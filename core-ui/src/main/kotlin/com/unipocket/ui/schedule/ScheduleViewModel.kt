package com.unipocket.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.unipocket.domain.Course
import com.unipocket.domain.EditableScheduleSource
import com.unipocket.domain.ScheduleSource
import com.unipocket.domain.SchoolConfig
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
 *     factory = ScheduleViewModel.factory(config, MyScheduleSource()),
 * )
 * val state by vm.state.collectAsStateWithLifecycle()
 * ```
 */
class ScheduleViewModel(
    config: SchoolConfig,
    source: ScheduleSource,
    clock: () -> LocalDate = { LocalDate.now() },
    editableSource: EditableScheduleSource? = null,
) : ViewModel() {

    private val store = ScheduleStore(config, source, viewModelScope, clock, editableSource)

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
    fun upsertCourse(course: Course) = store.upsertCourse(course)

    /** 删除一节课；数据源不支持时为 no-op */
    fun deleteCourse(id: String) = store.deleteCourse(id)

    companion object {
        fun factory(
            config: SchoolConfig,
            source: ScheduleSource,
            clock: () -> LocalDate = { LocalDate.now() },
            editableSource: EditableScheduleSource? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ScheduleViewModel(config, source, clock, editableSource) }
        }
    }
}
