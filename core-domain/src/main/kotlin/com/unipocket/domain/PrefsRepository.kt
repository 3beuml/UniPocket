package com.unipocket.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 把 [PrefsStore] 的字符串键值整理成类型安全的 [SchedulePrefs]。
 *
 * - [start] 之后持续观察存储变化（包括外部写入），[prefs] 始终是最新快照。
 * - 写入走 [PrefsStore.write]，不在这里另建缓存，避免两处状态不一致。
 * - 存储里缺失或非法的值一律回退默认值。
 *
 * 生命周期由调用方决定：App 级建一个即可，不要每个页面各建一个。
 */
class PrefsRepository(
    private val store: PrefsStore,
    private val scope: CoroutineScope,
) {

    private val _prefs = MutableStateFlow(SchedulePrefs())
    val prefs: StateFlow<SchedulePrefs> = _prefs.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            combine(SchedulePrefs.KEYS.map { store.observe(it) }) { values ->
                SchedulePrefs(
                    darkMode = values[0].toDarkMode(),
                    themeSeed = values[1]?.toIntOrNull() ?: SchedulePrefs.DEFAULT_SEED,
                    showWeekend = values[2]?.toBooleanStrictOrNull() ?: true,
                    showTeacher = values[3]?.toBooleanStrictOrNull() ?: false,
                    showRoom = values[4]?.toBooleanStrictOrNull() ?: true,
                    backgroundUri = values[5]?.takeIf { it.isNotBlank() },
                    backgroundDim = values[6]?.toBooleanStrictOrNull() ?: true,
                )
            }.collect { _prefs.value = it }
        }
    }

    fun setDarkMode(mode: DarkMode) = write(SchedulePrefs.KEY_DARK_MODE, mode.name)

    fun setThemeSeed(argb: Int) = write(SchedulePrefs.KEY_THEME_SEED, argb.toString())

    fun setShowWeekend(show: Boolean) = write(SchedulePrefs.KEY_SHOW_WEEKEND, show.toString())

    fun setShowTeacher(show: Boolean) = write(SchedulePrefs.KEY_SHOW_TEACHER, show.toString())

    fun setShowRoom(show: Boolean) = write(SchedulePrefs.KEY_SHOW_ROOM, show.toString())

    fun setBackgroundUri(uri: String?) = write(SchedulePrefs.KEY_BACKGROUND_URI, uri)

    fun setBackgroundDim(dim: Boolean) = write(SchedulePrefs.KEY_BACKGROUND_DIM, dim.toString())

    private fun write(key: String, value: String?) {
        scope.launch { store.write(key, value) }
    }

    private fun String?.toDarkMode(): DarkMode =
        DarkMode.entries.firstOrNull { it.name == this } ?: DarkMode.SYSTEM
}
