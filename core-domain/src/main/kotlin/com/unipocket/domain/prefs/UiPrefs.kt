package com.unipocket.domain.prefs

import com.unipocket.domain.PrefsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** 课程格里显示哪些内容（格子不显示老师：点开详情能看到，格子太小放不下） */
enum class CourseTextMode {
    NAME,
    NAME_ROOM,

    /** 已废弃：早期允许显示老师，读偏好时自动降级为 NAME_ROOM */
    @Deprecated("课表格子不再显示老师")
    NAME_TEACHER_ROOM,
}

/** 格子密度：影响行高与字号 */
enum class GridDensity { COMPACT, STANDARD, COMFORTABLE }

/** 课程配色：多彩（按课程取色）/ 单色（统一主题色） */
enum class CourseColorMode { COLORFUL, SINGLE }

/**
 * 课程块描边。虚线用于分开相邻、颜色接近的课程块；
 * 原「显示网格线」开关已合并进来（实线形式）。
 */
enum class BlockBorder { NONE, SOLID, DASHED }

/** 深浅色：跟随系统 / 始终浅色 / 始终深色 */
enum class DarkMode { SYSTEM, LIGHT, DARK }

/** 深色对比度三档（对标系统「对比度」设置），只在深色下生效 */
enum class DarkContrast { STANDARD, MEDIUM, HIGH }

/**
 * 全应用配色预设。[DYNAMIC] = 跟随系统壁纸动态取色；
 * 其余每项一个种子色，由 Material 3 算法算出整套浅/深配色。
 */
enum class ThemePreset(val seedArgb: Int?) {
    DYNAMIC(null),
    PURPLE(0xFF6750A4.toInt()),
    BLUE(0xFF1A73E8.toInt()),
    TEAL(0xFF00838F.toInt()),
    GREEN(0xFF2E7D32.toInt()),
    AMBER(0xFFE8710A.toInt()),
    PINK(0xFFD81B60.toInt()),
    GRAPHITE(0xFF546E7A.toInt()),
}

/**
 * 全应用界面显示偏好（模型）。
 *
 * 只收**通用显示项**；学校/应用专属的偏好（电价、房间名、更新屏蔽、首页模块顺序）
 * 留在各自的 App 里，不进框架。存储通过 [PrefsStore]（字符串键值）外置，
 * 键名与 ZZUPocket 的 DataStore 保持一致，方便对齐语义。
 */
data class UiPrefs(
    /** 是否显示周六周日（关掉就是工作日 5 列，列宽更宽、字更大） */
    val showWeekend: Boolean = true,
    val blockBorder: BlockBorder = BlockBorder.DASHED,
    val courseTextMode: CourseTextMode = CourseTextMode.NAME_ROOM,
    val density: GridDensity = GridDensity.STANDARD,
    /** "正在上的课"描边高亮（借鉴 Sked 的 liveCourseOutline） */
    val liveOutlineEnabled: Boolean = true,
    val liveOutlineWidth: Int = 2,
    val colorMode: CourseColorMode = CourseColorMode.COLORFUL,
    /** 手动指定入学年；为空则不过滤 */
    val enrollmentYearOverride: Int? = null,
    /** 自定义课表背景图；null = 用默认背景 */
    val backgroundPath: String? = null,
    /** 壁纸浓淡：图片自身的不透明度 */
    val backgroundOpacity: Float = 0.7f,
    /** 壁纸模糊半径（dp），0 = 完全清晰 */
    val backgroundBlur: Float = 6f,
    /**
     * 底色浓度：图片上那层主题底色的强度。与 [backgroundOpacity] 是乘法关系——
     * 70% × 40% 大约让照片保留四成强度，壁纸看得出来、正文也读得清。
     */
    val backgroundScrim: Float = 0.4f,
    /** 课表页的底部导航栏也一起透明（只有课表 tab 生效） */
    val wallpaperNavBar: Boolean = true,
    /** 每次换图 +1，用于让内存位图缓存失效 */
    val backgroundVersion: Int = 0,
    /** 全应用配色预设（默认跟随系统壁纸动态取色） */
    val themePreset: ThemePreset = ThemePreset.DYNAMIC,
    /** 深浅色（默认跟随系统） */
    val darkMode: DarkMode = DarkMode.SYSTEM,
    /** 深色对比度（只在深色下生效，默认标准 = 系统原生观感） */
    val darkContrast: DarkContrast = DarkContrast.STANDARD,
    /** 深色保留颜色（深色表面带主色色相、明度上抬），只在深色下生效 */
    val darkTinted: Boolean = false,
    /**
     * 「显示描述」（默认开）。关掉后界面只留一行短提示，
     * 隐藏原理、例外、错误原因这类补充信息。
     */
    val showDescriptions: Boolean = true,
    /** dock 上是否保留「专注」这一格 */
    val showFocusInDock: Boolean = true,
    /** 小组件时间显示为节次（「第3-4节」）；false（默认）= 时刻（10:10-11:50） */
    val widgetTimeAsPeriod: Boolean = false,
    /** 小组件行样式：auto / compact / mini / stacked（各组件按自身阶梯解释） */
    val widgetRowMode: String = "auto",
    /** 小组件行内显示地点（2×2 / 4×2 / 周课表通用） */
    val widgetShowPlace: Boolean = true,
    /** 小组件行内显示时间段 */
    val widgetShowTime: Boolean = true,
    /** 周课表（4×5）格子内显示地点 */
    val widgetWeekShowPlace: Boolean = true,
    /** 周课表格子内显示开始时刻 */
    val widgetWeekShowTime: Boolean = true,
    /** 周课表底部的周末明细条（周六/日有课时出现） */
    val widgetWeekendStrip: Boolean = true,
    /** 2×2 下一节课的字号档：auto 或档位 label */
    val widgetFontTier: String = "auto",
    /** 2×2 表头样式：HEADER_CLASSIC / HEADER_COMPACT */
    val widgetHeaderStyle: String = "classic",
    /** 4×2 今明课表行样式：auto / stacked / mini */
    val widgetTwoDayStyle: String = "auto",
) {
    val dayCount: Int get() = if (showWeekend) 7 else 5

    val cellHeightDp: Int
        get() = when (density) {
            GridDensity.COMPACT -> 48
            GridDensity.STANDARD -> 54
            GridDensity.COMFORTABLE -> 64
        }

    val courseFontSp: Float
        get() = when (density) {
            GridDensity.COMPACT -> 9.5f
            GridDensity.STANDARD -> 11f
            GridDensity.COMFORTABLE -> 12.5f
        }

    val roomFontSp: Float
        get() = when (density) {
            GridDensity.COMPACT -> 8f
            GridDensity.STANDARD -> 9f
            GridDensity.COMFORTABLE -> 10f
        }

    companion object {
        const val WIDGET_ROW_MODE_AUTO = "auto"
        const val WIDGET_ROW_MODE_COMPACT = "compact"
        const val WIDGET_ROW_MODE_STACKED = "stacked"
        const val WIDGET_ROW_MODE_MINI = "mini"
        const val WIDGET_COMPACT_TIER_AUTO = "auto"
        const val WIDGET_HEADER_CLASSIC = "classic"
        const val WIDGET_HEADER_COMPACT = "compact"
    }
}

/**
 * [UiPrefs] 的读写仓储：读走 [PrefsStore.observe] 组合出的实时流，
 * 写走 [PrefsStore.write]。存储实现外置（demo：SharedPreferences；ZZUPocket：DataStore）。
 *
 * 键名与 ZZUPocket DataStore 完全一致，语义（默认值、旧值降级）也一致，
 * 两个 App 的偏好可以逐键对照迁移。
 */
class UiPrefsRepository(
    private val store: PrefsStore,
    private val scope: CoroutineScope,
) {

    private val _prefs = MutableStateFlow(UiPrefs())

    /** 实时偏好流；[start] 后持续更新（含外部写入） */
    val prefs: StateFlow<UiPrefs> = _prefs.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(KEYS.map { store.observe(it) }) { values ->
                values.toUiPrefs()
            }.collect { _prefs.value = it }
        }
    }

    // ── 外观 ────────────────────────────────────────────────────────

    fun setThemePreset(v: ThemePreset) = write(KEY_THEME_PRESET, v.name)
    fun setDarkMode(v: DarkMode) = write(KEY_DARK_MODE, v.name)
    fun setDarkContrast(v: DarkContrast) = write(KEY_DARK_CONTRAST, v.name)
    fun setDarkTinted(v: Boolean) = write(KEY_DARK_TINTED, v.toString())

    /** 换背景图：路径 + 版本号一起写，位图缓存随之失效 */
    fun setBackground(path: String) {
        scope.launch {
            store.write(KEY_BG_PATH, path)
            val next = (store.read(KEY_BG_VERSION)?.toIntOrNull() ?: 0) + 1
            store.write(KEY_BG_VERSION, next.toString())
        }
    }

    fun setBackgroundOpacity(v: Float) = write(KEY_BG_OPACITY, v.coerceIn(0.15f, 0.95f).toString())
    fun setBackgroundBlur(v: Float) = write(KEY_BG_BLUR, v.coerceIn(0f, 20f).toString())
    fun setBackgroundScrim(v: Float) = write(KEY_BG_SCRIM, v.coerceIn(0f, 0.9f).toString())
    fun setWallpaperNavBar(v: Boolean) = write(KEY_BG_NAVBAR, v.toString())

    /** 恢复默认壁纸：清掉图片与全部调节项，版本号照旧 +1 */
    fun clearBackground() {
        scope.launch {
            store.write(KEY_BG_PATH, null)
            store.write(KEY_BG_OPACITY, null)
            store.write(KEY_BG_BLUR, null)
            store.write(KEY_BG_SCRIM, null)
            val next = (store.read(KEY_BG_VERSION)?.toIntOrNull() ?: 0) + 1
            store.write(KEY_BG_VERSION, next.toString())
        }
    }

    // ── 显示 ────────────────────────────────────────────────────────

    fun setShowWeekend(v: Boolean) = write(KEY_SHOW_WEEKEND, v.toString())
    fun setBlockBorder(v: BlockBorder) = write(KEY_BLOCK_BORDER, v.name)
    fun setCourseTextMode(v: CourseTextMode) = write(KEY_TEXT_MODE, v.name)
    fun setDensity(v: GridDensity) = write(KEY_DENSITY, v.name)
    fun setLiveOutline(v: Boolean) = write(KEY_LIVE_OUTLINE, v.toString())
    fun setLiveOutlineWidth(v: Int) = write(KEY_LIVE_OUTLINE_WIDTH, v.toString())
    fun setColorMode(v: CourseColorMode) = write(KEY_COLOR_MODE, v.name)
    fun setEnrollmentYear(v: Int?) = write(KEY_ENROLL_YEAR, v?.toString())
    fun setShowDescriptions(v: Boolean) = write(KEY_SHOW_DESCRIPTIONS, v.toString())
    fun setShowFocusInDock(v: Boolean) = write(KEY_SHOW_FOCUS_IN_DOCK, v.toString())

    // ── 小组件 ──────────────────────────────────────────────────────

    fun setWidgetTimeAsPeriod(v: Boolean) = write(KEY_WIDGET_TIME_AS_PERIOD, v.toString())
    fun setWidgetRowMode(v: String) = write(KEY_WIDGET_ROW_MODE, v)
    fun setWidgetShowPlace(v: Boolean) = write(KEY_WIDGET_SHOW_PLACE, v.toString())
    fun setWidgetShowTime(v: Boolean) = write(KEY_WIDGET_SHOW_TIME, v.toString())
    fun setWidgetWeekShowPlace(v: Boolean) = write(KEY_WIDGET_WEEK_SHOW_PLACE, v.toString())
    fun setWidgetWeekShowTime(v: Boolean) = write(KEY_WIDGET_WEEK_SHOW_TIME, v.toString())
    fun setWidgetWeekendStrip(v: Boolean) = write(KEY_WIDGET_WEEKEND_STRIP, v.toString())
    fun setWidgetFontTier(v: String) = write(KEY_WIDGET_FONT_TIER, v)
    fun setWidgetHeaderStyle(v: String) = write(KEY_WIDGET_HEADER_STYLE, v)
    fun setWidgetTwoDayStyle(v: String) = write(KEY_WIDGET_TWO_DAY_STYLE, v)

    private fun write(key: String, value: String?) {
        scope.launch { store.write(key, value) }
    }

    private fun Array<String?>.toUiPrefs(): UiPrefs = UiPrefs(
        showWeekend = this[0]?.toBooleanStrictOrNull() ?: true,
        blockBorder = enumOr(this[1], BlockBorder.DASHED),
        courseTextMode = this[2]?.let { runCatching { CourseTextMode.valueOf(it) }.getOrNull() }
            ?.takeIf { it != CourseTextMode.NAME_TEACHER_ROOM } // 老设置自动降级
            ?: CourseTextMode.NAME_ROOM,
        density = enumOr(this[3], GridDensity.STANDARD),
        liveOutlineEnabled = this[4]?.toBooleanStrictOrNull() ?: true,
        liveOutlineWidth = this[5]?.toIntOrNull() ?: 2,
        colorMode = enumOr(this[6], CourseColorMode.COLORFUL),
        enrollmentYearOverride = this[7]?.toIntOrNull()?.takeIf { it > 0 },
        backgroundPath = this[8],
        backgroundOpacity = this[9]?.toFloatOrNull() ?: 0.7f,
        backgroundBlur = this[10]?.toFloatOrNull() ?: 6f,
        backgroundScrim = this[11]?.toFloatOrNull() ?: 0.4f,
        wallpaperNavBar = this[12]?.toBooleanStrictOrNull() ?: true,
        backgroundVersion = this[13]?.toIntOrNull() ?: 0,
        themePreset = enumOr(this[14], ThemePreset.DYNAMIC),
        darkMode = enumOr(this[15], DarkMode.SYSTEM),
        darkContrast = enumOr(this[16], DarkContrast.STANDARD),
        darkTinted = this[17]?.toBooleanStrictOrNull() ?: false,
        showDescriptions = this[18]?.toBooleanStrictOrNull() ?: true,
        showFocusInDock = this[19]?.toBooleanStrictOrNull() ?: true,
        widgetTimeAsPeriod = this[20]?.toBooleanStrictOrNull() ?: false,
        widgetRowMode = this[21]?.takeIf { it.isNotBlank() } ?: "auto",
        widgetShowPlace = this[22]?.toBooleanStrictOrNull() ?: true,
        widgetShowTime = this[23]?.toBooleanStrictOrNull() ?: true,
        widgetWeekShowPlace = this[24]?.toBooleanStrictOrNull() ?: true,
        widgetWeekShowTime = this[25]?.toBooleanStrictOrNull() ?: true,
        widgetWeekendStrip = this[26]?.toBooleanStrictOrNull() ?: true,
        widgetFontTier = this[27]?.takeIf { it.isNotBlank() } ?: "auto",
        widgetHeaderStyle = this[28]?.takeIf { it.isNotBlank() } ?: "classic",
        widgetTwoDayStyle = this[29]?.takeIf { it.isNotBlank() } ?: "auto",
    )

    private inline fun <reified T : Enum<T>> enumOr(raw: String?, default: T): T =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    companion object {
        val KEYS = listOf(
            KEY_SHOW_WEEKEND, KEY_BLOCK_BORDER, KEY_TEXT_MODE, KEY_DENSITY,
            KEY_LIVE_OUTLINE, KEY_LIVE_OUTLINE_WIDTH, KEY_COLOR_MODE, KEY_ENROLL_YEAR,
            KEY_BG_PATH, KEY_BG_OPACITY, KEY_BG_BLUR, KEY_BG_SCRIM,
            KEY_BG_NAVBAR, KEY_BG_VERSION, KEY_THEME_PRESET, KEY_DARK_MODE,
            KEY_DARK_CONTRAST, KEY_DARK_TINTED, KEY_SHOW_DESCRIPTIONS, KEY_SHOW_FOCUS_IN_DOCK,
            KEY_WIDGET_TIME_AS_PERIOD, KEY_WIDGET_ROW_MODE, KEY_WIDGET_SHOW_PLACE, KEY_WIDGET_SHOW_TIME,
            KEY_WIDGET_WEEK_SHOW_PLACE, KEY_WIDGET_WEEK_SHOW_TIME, KEY_WIDGET_WEEKEND_STRIP,
            KEY_WIDGET_FONT_TIER, KEY_WIDGET_HEADER_STYLE, KEY_WIDGET_TWO_DAY_STYLE,
        )

        // 键名与 ZZUPocket DataStore 一致，便于两个 App 的偏好逐键对照迁移
        const val KEY_SHOW_WEEKEND = "show_weekend"
        const val KEY_BLOCK_BORDER = "block_border"
        const val KEY_TEXT_MODE = "course_text_mode"
        const val KEY_DENSITY = "grid_density"
        const val KEY_LIVE_OUTLINE = "live_outline"
        const val KEY_LIVE_OUTLINE_WIDTH = "live_outline_width"
        const val KEY_COLOR_MODE = "color_mode"
        const val KEY_ENROLL_YEAR = "enrollment_year"
        const val KEY_BG_PATH = "grid_bg_path"
        const val KEY_BG_OPACITY = "grid_bg_opacity"
        const val KEY_BG_BLUR = "grid_bg_blur"
        const val KEY_BG_SCRIM = "grid_bg_scrim"
        const val KEY_BG_NAVBAR = "grid_bg_navbar"
        const val KEY_BG_VERSION = "grid_bg_version"
        const val KEY_THEME_PRESET = "theme_preset"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_DARK_CONTRAST = "dark_contrast"
        const val KEY_DARK_TINTED = "dark_tinted"
        const val KEY_SHOW_DESCRIPTIONS = "show_descriptions"
        const val KEY_SHOW_FOCUS_IN_DOCK = "show_focus_in_dock"
        const val KEY_WIDGET_TIME_AS_PERIOD = "widget_time_as_period"
        const val KEY_WIDGET_ROW_MODE = "widget_row_mode"
        const val KEY_WIDGET_SHOW_PLACE = "widget_show_place"
        const val KEY_WIDGET_SHOW_TIME = "widget_show_time"
        const val KEY_WIDGET_WEEK_SHOW_PLACE = "widget_week_show_place"
        const val KEY_WIDGET_WEEK_SHOW_TIME = "widget_week_show_time"
        const val KEY_WIDGET_WEEKEND_STRIP = "widget_weekend_strip"
        const val KEY_WIDGET_FONT_TIER = "widget_font_tier"
        const val KEY_WIDGET_HEADER_STYLE = "widget_header_style"
        const val KEY_WIDGET_TWO_DAY_STYLE = "widget_two_day_style"
    }
}
