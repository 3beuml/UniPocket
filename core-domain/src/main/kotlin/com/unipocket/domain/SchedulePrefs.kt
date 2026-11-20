package com.unipocket.domain

/** 深色模式策略 */
enum class DarkMode {
    /** 跟随系统 */
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * 框架内置的用户偏好。**只放普适项**：学校专有的显示差异由接入方自己在
 * [PrefsStore] 里存别的键，不要往这里加学校语义字段。
 *
 * 存的是字符串键值（见 [PrefsStore]），所以每一项都有稳定的 key；
 * 读取时任何非法值都回退到默认值，不会因为旧版本残留数据崩溃。
 */
data class SchedulePrefs(
    val darkMode: DarkMode = DarkMode.SYSTEM,
    /** 主题种子色（ARGB）。用它生成整套 Material 3 配色 */
    val themeSeed: Int = DEFAULT_SEED,
    /** 课表网格是否显示周末两列 */
    val showWeekend: Boolean = true,
    /** 课程块里是否显示教师 */
    val showTeacher: Boolean = false,
    /** 课程块里是否显示教室 */
    val showRoom: Boolean = true,
) {
    companion object {
        /** 默认种子色：偏蓝的靛色，浅色/深色下对比都够 */
        val DEFAULT_SEED: Int = 0xFF4F6BED.toInt()

        const val KEY_DARK_MODE = "appearance.darkMode"
        const val KEY_THEME_SEED = "appearance.themeSeed"
        const val KEY_SHOW_WEEKEND = "display.showWeekend"
        const val KEY_SHOW_TEACHER = "display.showTeacher"
        const val KEY_SHOW_ROOM = "display.showRoom"

        /** 顺序必须与 [PrefsRepository] 的 combine 一致 */
        val KEYS: List<String> = listOf(
            KEY_DARK_MODE,
            KEY_THEME_SEED,
            KEY_SHOW_WEEKEND,
            KEY_SHOW_TEACHER,
            KEY_SHOW_ROOM,
        )
    }
}
