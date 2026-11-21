package com.unipocket.feature.focus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 番茄钟的三段：专注 / 短休息 / 长休息 */
enum class FocusPhase { WORK, SHORT_BREAK, LONG_BREAK }

/**
 * 时长配置。默认是经典番茄工作法：25 分钟专注、5 分钟短休息、
 * 每 4 个番茄后 15 分钟长休息。
 */
data class FocusConfig(
    val workMillis: Long = 25 * 60_000L,
    val shortBreakMillis: Long = 5 * 60_000L,
    val longBreakMillis: Long = 15 * 60_000L,
    val cyclesBeforeLongBreak: Int = 4,
)

/** 番茄钟界面状态 */
data class FocusState(
    val phase: FocusPhase = FocusPhase.WORK,
    val durationMillis: Long = 25 * 60_000L,
    val remainingMillis: Long = 25 * 60_000L,
    val isRunning: Boolean = false,
    /** 已完成的专注个数（跨多个轮次累计） */
    val completedWorkSessions: Int = 0,
    /** 每多少个番茄进入长休息 */
    val cycleLength: Int = 4,
) {
    /** 0f = 刚开始，1f = 走完 */
    val progress: Float
        get() = if (durationMillis <= 0) 0f
        else (1f - remainingMillis.toFloat() / durationMillis).coerceIn(0f, 1f)

    /** "24:59" */
    val remainingText: String
        get() {
            val totalSeconds = (remainingMillis + 999) / 1000
            return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
        }

    val phaseLabel: String
        get() = when (phase) {
            FocusPhase.WORK -> "专注"
            FocusPhase.SHORT_BREAK -> "短休息"
            FocusPhase.LONG_BREAK -> "长休息"
        }
}

/**
 * 番茄钟状态机（纯 Kotlin，可单测）。
 *
 * 行为：
 * - 专注结束 → 自动开始休息（短/长由番茄计数决定）。
 * - 休息结束 → 停在"专注"等用户手动开始，不擅自进入下一轮。
 * - 暂停保留剩余时间；重置回到当前阶段的完整时长。
 * - [clock] 可注入，测试里用虚拟时间推进。
 */
class FocusStore(
    private val config: FocusConfig = FocusConfig(),
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    private val _state = MutableStateFlow(
        FocusState(
            phase = FocusPhase.WORK,
            durationMillis = config.workMillis,
            remainingMillis = config.workMillis,
            cycleLength = config.cyclesBeforeLongBreak,
        ),
    )
    val state: StateFlow<FocusState> = _state.asStateFlow()

    /** 阶段完成事件（目前只有专注完成）；缓冲 8 个足够，消费方是统计落库 */
    private val _events = MutableSharedFlow<FocusEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<FocusEvent> = _events.asSharedFlow()

    /** 当前阶段结束的绝对时刻；暂停/停止时为 null */
    private var deadline: Long? = null
    /** 当前阶段开始时刻，用于专注完成时记录归属 */
    private var phaseStartedAt: Long? = null
    private var ticker: Job? = null

    fun start() {
        if (_state.value.isRunning) return
        val now = clock()
        deadline = now + _state.value.remainingMillis
        if (_state.value.phase == FocusPhase.WORK) phaseStartedAt = now
        _state.update { it.copy(isRunning = true) }
        ensureTicker()
    }

    fun pause() {
        if (!_state.value.isRunning) return
        val remaining = remainingNow()
        deadline = null
        _state.update { it.copy(isRunning = false, remainingMillis = remaining) }
    }

    /** 重置当前阶段（不改变已完成计数） */
    fun reset() {
        deadline = null
        ticker?.cancel()
        ticker = null
        val duration = durationOf(_state.value.phase)
        _state.update {
            it.copy(
                isRunning = false,
                durationMillis = duration,
                remainingMillis = duration,
            )
        }
    }

    private fun ensureTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                if (!_state.value.isRunning) break

                val target = deadline ?: break
                val remaining = (target - clock()).coerceAtLeast(0)
                if (remaining > 0) {
                    _state.update { it.copy(remainingMillis = remaining) }
                } else {
                    completePhase()
                }
            }
        }
    }

    private fun completePhase() {
        val current = _state.value
        when (current.phase) {
            FocusPhase.WORK -> {
                val completed = current.completedWorkSessions + 1
                val next = if (current.cycleLength > 0 && completed % current.cycleLength == 0) {
                    FocusPhase.LONG_BREAK
                } else {
                    FocusPhase.SHORT_BREAK
                }
                _events.tryEmit(
                    FocusEvent.WorkCompleted(
                        startedAtMillis = phaseStartedAt ?: (clock() - config.workMillis),
                        durationMillis = config.workMillis,
                    ),
                )
                enterPhase(next, completed, autoStart = true)
            }

            FocusPhase.SHORT_BREAK, FocusPhase.LONG_BREAK ->
                enterPhase(FocusPhase.WORK, current.completedWorkSessions, autoStart = false)
        }
    }

    private fun enterPhase(phase: FocusPhase, completed: Int, autoStart: Boolean) {
        val duration = durationOf(phase)
        deadline = if (autoStart) clock() + duration else null
        phaseStartedAt = if (autoStart) clock() else null
        _state.update {
            it.copy(
                phase = phase,
                durationMillis = duration,
                remainingMillis = duration,
                isRunning = autoStart,
                completedWorkSessions = completed,
            )
        }
    }

    private fun durationOf(phase: FocusPhase): Long = when (phase) {
        FocusPhase.WORK -> config.workMillis
        FocusPhase.SHORT_BREAK -> config.shortBreakMillis
        FocusPhase.LONG_BREAK -> config.longBreakMillis
    }

    private fun remainingNow(): Long =
        deadline?.let { (it - clock()).coerceAtLeast(0) } ?: _state.value.remainingMillis

    private companion object {
        const val TICK_MILLIS = 250L
    }
}
