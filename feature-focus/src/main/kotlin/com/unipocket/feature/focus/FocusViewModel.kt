package com.unipocket.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 官方番茄钟 ViewModel：状态机 + 完成事件落统计。
 *
 * 传 [recordStore]（`FocusRecordStore` 实现）即可获得统计；不传时 `stats` 恒为 null，
 * 界面不显示统计区。计时本身不依赖存储——杀进程会丢当前计时，但已完成的记录在存储里。
 */
class FocusViewModel(
    config: FocusConfig = FocusConfig(),
    private val recordStore: FocusRecordStore? = null,
) : ViewModel() {

    private val store = FocusStore(config, viewModelScope)

    val state: StateFlow<FocusState> = store.state

    private val _stats = MutableStateFlow<FocusStats?>(null)
    val stats: StateFlow<FocusStats?> = _stats.asStateFlow()

    init {
        // 完成事件 → 落库 → 重算统计；事件缓冲在 store 侧，这里慢一点也不丢
        viewModelScope.launch {
            store.events.collect { event ->
                if (event is FocusEvent.WorkCompleted && recordStore != null) {
                    recordStore.append(
                        FocusRecord(
                            startedAtMillis = event.startedAtMillis,
                            minutes = (event.durationMillis / 60_000L).coerceAtLeast(1).toInt(),
                        ),
                    )
                }
                refreshStats()
            }
        }
        refreshStats()
    }

    private fun refreshStats() {
        val store0 = recordStore ?: return
        viewModelScope.launch {
            _stats.value = FocusStatsCalculator.compute(store0.loadAll(), LocalDate.now())
        }
    }

    fun start() = store.start()

    fun pause() = store.pause()

    fun reset() = store.reset()

    companion object {
        fun factory(
            config: FocusConfig = FocusConfig(),
            recordStore: FocusRecordStore? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { FocusViewModel(config, recordStore) }
        }
    }
}
