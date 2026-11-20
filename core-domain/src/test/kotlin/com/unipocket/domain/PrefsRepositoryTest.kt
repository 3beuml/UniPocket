package com.unipocket.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsRepositoryTest {

    /**
     * 偏好仓储需要一个长期运行的收集协程。用独立的无约束调度器作用域：
     * 协程立即执行（便于断言），又不会成为 testScope 的子任务把 runTest 卡住。
     */
    private fun TestScope.repository(store: PrefsStore): PrefsRepository =
        PrefsRepository(store, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

    @Test
    fun `空存储时使用默认值`() = runTest {
        val repository = repository(InMemoryPrefsStore())

        repository.start()

        assertEquals(SchedulePrefs(), repository.prefs.value)
    }

    @Test
    fun `读取已存储的值`() = runTest {
        val store = InMemoryPrefsStore()
        store.write(SchedulePrefs.KEY_DARK_MODE, "DARK")
        store.write(SchedulePrefs.KEY_THEME_SEED, "12345")
        store.write(SchedulePrefs.KEY_SHOW_WEEKEND, "false")
        store.write(SchedulePrefs.KEY_SHOW_TEACHER, "true")
        store.write(SchedulePrefs.KEY_SHOW_ROOM, "false")

        val repository = repository(store)
        repository.start()

        val prefs = repository.prefs.value
        assertEquals(DarkMode.DARK, prefs.darkMode)
        assertEquals(12345, prefs.themeSeed)
        assertEquals(false, prefs.showWeekend)
        assertEquals(true, prefs.showTeacher)
        assertEquals(false, prefs.showRoom)
    }

    @Test
    fun `非法值回退默认`() = runTest {
        val store = InMemoryPrefsStore()
        store.write(SchedulePrefs.KEY_DARK_MODE, "PURPLE")
        store.write(SchedulePrefs.KEY_THEME_SEED, "not-a-number")
        store.write(SchedulePrefs.KEY_SHOW_WEEKEND, "maybe")

        val repository = repository(store)
        repository.start()

        assertEquals(SchedulePrefs().darkMode, repository.prefs.value.darkMode)
        assertEquals(SchedulePrefs().themeSeed, repository.prefs.value.themeSeed)
        assertEquals(SchedulePrefs().showWeekend, repository.prefs.value.showWeekend)
    }

    @Test
    fun `写入后状态随之更新`() = runTest {
        val store = InMemoryPrefsStore()
        val repository = repository(store)
        repository.start()

        repository.setDarkMode(DarkMode.LIGHT)
        repository.setShowWeekend(false)

        assertEquals(DarkMode.LIGHT, repository.prefs.value.darkMode)
        assertEquals(false, repository.prefs.value.showWeekend)
        assertEquals("LIGHT", store.read(SchedulePrefs.KEY_DARK_MODE))
    }

    @Test
    fun `外部写入也会反映到状态`() = runTest {
        val store = InMemoryPrefsStore()
        val repository = repository(store)
        repository.start()

        store.write(SchedulePrefs.KEY_SHOW_ROOM, "false")

        assertEquals(false, repository.prefs.value.showRoom)
    }
}
