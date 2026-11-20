package com.unipocket.feature.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.unipocket.domain.RoomSource
import com.unipocket.domain.SchoolConfig
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * 官方空教室 ViewModel：把 [RoomStore] 绑到页面生命周期上。
 *
 * 未实现 [RoomSource] 的接入方可以不使用本页面；使用了也只需提供一个实现。
 */
class RoomViewModel(
    config: SchoolConfig,
    source: RoomSource,
    clock: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val store = RoomStore(config, source, viewModelScope, clock)

    val state: StateFlow<RoomUiState> = store.state

    init {
        store.start()
    }

    fun refresh() = store.refresh()

    fun retry() = store.retry()

    fun setDayOfWeek(dayOfWeek: Int) = store.setDayOfWeek(dayOfWeek)

    fun setPeriod(periodIndex: Int) = store.setPeriod(periodIndex)

    fun setBuilding(buildingId: String?) = store.setBuilding(buildingId)

    companion object {
        fun factory(
            config: SchoolConfig,
            source: RoomSource,
            clock: () -> LocalDate = { LocalDate.now() },
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { RoomViewModel(config, source, clock) }
        }
    }
}
