package com.unipocket.feature.focus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusStoreTest {

    /** 推进虚拟时间，并执行恰好落在该时刻的任务（advanceTimeBy 是开区间） */
    private fun TestScope.advance(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    private val config = FocusConfig(
        workMillis = 60_000,
        shortBreakMillis = 30_000,
        longBreakMillis = 120_000,
        cyclesBeforeLongBreak = 2,
    )

    /** 无约束调度器 + 虚拟时钟：立即执行，且不被 runTest 等待 */
    private fun TestScope.newStore(): FocusStore {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        return FocusStore(config, scope) { testScheduler.currentTime }
    }

    @Test
    fun `开始后倒计时递减`() = runTest {
        val store = newStore()

        store.start()
        advance(10_000)

        val state = store.state.value
        assertTrue(state.isRunning)
        assertEquals(50_000, state.remainingMillis)
        assertEquals("00:50", state.remainingText)
    }

    @Test
    fun `暂停后剩余时间不再变化`() = runTest {
        val store = newStore()

        store.start()
        advance(10_000)
        store.pause()
        val paused = store.state.value.remainingMillis

        advance(30_000)

        assertFalse(store.state.value.isRunning)
        assertEquals(paused, store.state.value.remainingMillis)
    }

    @Test
    fun `专注结束自动进入短休息`() = runTest {
        val store = newStore()

        store.start()
        advance(60_000)

        val state = store.state.value
        assertEquals(FocusPhase.SHORT_BREAK, state.phase)
        assertTrue(state.isRunning)
        assertEquals(1, state.completedWorkSessions)
        assertEquals(30_000, state.remainingMillis)
    }

    @Test
    fun `休息结束停在专注阶段，等待手动开始`() = runTest {
        val store = newStore()

        store.start()
        advance(60_000) // 专注结束 → 短休息
        advance(30_000) // 休息结束

        val state = store.state.value
        assertEquals(FocusPhase.WORK, state.phase)
        assertFalse(state.isRunning)
        assertEquals(60_000, state.remainingMillis)
    }

    @Test
    fun `每两个番茄后进入长休息`() = runTest {
        val store = newStore()

        // 第 1 个番茄
        store.start()
        advance(60_000)
        assertEquals(FocusPhase.SHORT_BREAK, store.state.value.phase)
        advance(30_000) // 短休息结束

        // 第 2 个番茄
        store.start()
        advance(60_000)

        val state = store.state.value
        assertEquals(FocusPhase.LONG_BREAK, state.phase)
        assertEquals(2, state.completedWorkSessions)
        assertEquals(120_000, state.remainingMillis)
    }

    @Test
    fun `专注完成发出 WorkCompleted 事件`() = runTest {
        val store = newStore()
        val events = mutableListOf<FocusEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.events.collect { events += it }
        }

        store.start()
        advance(60_000)

        assertEquals(1, events.size)
        val event = events.single() as FocusEvent.WorkCompleted
        assertEquals(60_000L, event.durationMillis)
        assertEquals(0L, event.startedAtMillis)
    }

    @Test
    fun `重置回到当前阶段的完整时长`() = runTest {
        val store = newStore()

        store.start()
        advance(20_000)
        store.reset()

        val state = store.state.value
        assertFalse(state.isRunning)
        assertEquals(60_000, state.remainingMillis)
        assertEquals(0f, state.progress)
    }
}
