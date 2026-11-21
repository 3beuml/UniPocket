package com.unipocket.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unipocket.domain.prefs.CourseTextMode
import com.unipocket.domain.prefs.DarkMode
import com.unipocket.domain.prefs.ThemePreset
import com.unipocket.domain.prefs.UiPrefs

/** 配色预设：与 UiPrefs.ThemePreset 一一对应 */
private val ThemePresets: List<Pair<String, ThemePreset>> = listOf(
    "动态" to ThemePreset.DYNAMIC,
    "紫" to ThemePreset.PURPLE,
    "蓝" to ThemePreset.BLUE,
    "青" to ThemePreset.TEAL,
    "绿" to ThemePreset.GREEN,
    "琥珀" to ThemePreset.AMBER,
    "粉" to ThemePreset.PINK,
    "灰" to ThemePreset.GRAPHITE,
)

/**
 * 设置页（无状态）。
 *
 * 只负责展示与回调；偏好的读写由调用方通过 [com.unipocket.domain.PrefsRepository] 完成，
 * 所以这一页可以被任意接入方替换成自己的设置界面，而核心页面只依赖 [SchedulePrefs]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: UiPrefs,
    onDarkModeChange: (DarkMode) -> Unit,
    onThemePresetChange: (ThemePreset) -> Unit,
    onShowWeekendChange: (Boolean) -> Unit,
    onCourseTextModeChange: (CourseTextMode) -> Unit,
    onPickBackground: () -> Unit,
    onClearBackground: () -> Unit,
    onWidgetWeekShowPlaceChange: (Boolean) -> Unit,
    onWidgetWeekShowTimeChange: (Boolean) -> Unit,
    onWidgetWeekendStripChange: (Boolean) -> Unit,
    onBackgroundScrimChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionTitle("外观")

        Text(
            text = "深色模式",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DarkMode.entries.forEach { mode ->
                FilterChip(
                    selected = prefs.darkMode == mode,
                    onClick = { onDarkModeChange(mode) },
                    label = { Text(mode.label()) },
                )
            }
        }

        Text(
            text = "主题色",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ThemePresets.size) { index ->
                val (name, preset) = ThemePresets[index]
                val selected = prefs.themePreset == preset
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(preset.seedArgb ?: 0xFF6750A4.toInt()))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            )
                            .clickable { onThemePresetChange(preset) },
                    )
                    Text(
                        text = name,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = "背景图",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (prefs.backgroundPath != null) "已设置背景图" else "未设置背景图",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "从相册选择一张图片（由 App 负责读取与授权）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onPickBackground) {
                Text(if (prefs.backgroundPath != null) "更换" else "选择")
            }
            if (prefs.backgroundPath != null) {
                TextButton(onClick = onClearBackground) { Text("移除") }
            }
        }
        SwitchRow(
            title = "背景变暗",
            subtitle = "在图片上盖一层遮罩，保证文字清晰",
            checked = prefs.backgroundScrim > 0.01f,
            onCheckedChange = { onBackgroundScrimChange(if (it) 0.4f else 0.01f) },
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SectionTitle("显示")

        SwitchRow(
            title = "显示周末",
            subtitle = "关闭后课表只显示周一到周五",
            checked = prefs.showWeekend,
            onCheckedChange = onShowWeekendChange,
        )
        SwitchRow(
            title = "显示周末",
            subtitle = "关闭后课表只显示周一到周五",
            checked = prefs.showWeekend,
            onCheckedChange = onShowWeekendChange,
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SectionTitle("小组件")

        SwitchRow(
            title = "组件显示教室",
            subtitle = "桌面组件课程块与明细里的教室",
            checked = prefs.widgetWeekShowPlace,
            onCheckedChange = onWidgetWeekShowPlaceChange,
        )
        SwitchRow(
            title = "组件显示时间",
            subtitle = "节次列的时刻、周末明细条的时间",
            checked = prefs.widgetWeekShowTime,
            onCheckedChange = onWidgetWeekShowTimeChange,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = "设置保存在本机，不会上传。",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun DarkMode.label(): String = when (this) {
    DarkMode.SYSTEM -> "跟随系统"
    DarkMode.LIGHT -> "浅色"
    DarkMode.DARK -> "深色"
}
