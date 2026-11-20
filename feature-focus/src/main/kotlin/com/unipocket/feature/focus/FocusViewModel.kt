package com.unipocket.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.StateFlow

/** 官方番茄钟 ViewModel：把 [FocusStore] 绑到页面生命周期上 */
class FocusViewModel(
    config: FocusConfig = FocusConfig(),
) : ViewModel() {

    private val store = FocusStore(config, viewModelScope)

    val state: StateFlow<FocusState> = store.state

    fun start() = store.start()

    fun pause() = store.pause()

    fun reset() = store.reset()

    companion object {
        fun factory(config: FocusConfig = FocusConfig()): ViewModelProvider.Factory = viewModelFactory {
            initializer { FocusViewModel(config) }
        }
    }
}
