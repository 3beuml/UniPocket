package com.unipocket.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.unipocket.domain.prefs.BlockBorder
import com.unipocket.domain.prefs.CourseColorMode
import com.unipocket.domain.prefs.CourseTextMode
import com.unipocket.domain.prefs.GridDensity
import com.unipocket.domain.prefs.UiPrefs
import com.unipocket.domain.prefs.UiPrefsRepository
import com.unipocket.ui.components.GlassCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unipocket.ui.nav.DOCK_CLEARANCE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    repository: UiPrefsRepository,
    backgroundPainter: Painter?,
    onBack: () -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs by repository.prefs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课程表设置") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = DOCK_CLEARANCE),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            SectionCard("显示") {
                SwitchRow(
                    title = "显示周六周日",
                    hint = "关掉后只显示周一到周五，每列更宽、课名更完整",
                    checked = prefs.showWeekend,
                    onCheckedChange = repository::setShowWeekend,
                )
                ChoiceRow(
                    title = "课程块描边",
                    hint = "虚线便于分辨相邻且颜色接近的课程块",
                    options = listOf(
                        BlockBorder.NONE to "无",
                        BlockBorder.SOLID to "实线",
                        BlockBorder.DASHED to "虚线",
                    ),
                    selected = prefs.blockBorder,
                    onSelect = repository::setBlockBorder,
                )
                ChoiceRow(
                    title = "课程格内容",
                    hint = "老师不放在格子里（点开课程详情就能看到）",
                    options = listOf(
                        CourseTextMode.NAME to "课名",
                        CourseTextMode.NAME_ROOM to "课名+地点",
                    ),
                    selected = prefs.courseTextMode,
                    onSelect = repository::setCourseTextMode,
                )
                ChoiceRow(
                    title = "格子密度",
                    hint = "标准档把课名字号提到 11sp",
                    options = listOf(
                        GridDensity.COMPACT to "紧凑",
                        GridDensity.STANDARD to "标准",
                        GridDensity.COMFORTABLE to "舒适",
                    ),
                    selected = prefs.density,
                    onSelect = repository::setDensity,
                )
            }

            SectionCard("配色") {
                ChoiceRow(
                    title = "课程配色",
                    options = listOf(
                        CourseColorMode.COLORFUL to "多彩",
                        CourseColorMode.SINGLE to "单色",
                    ),
                    selected = prefs.colorMode,
                    onSelect = repository::setColorMode,
                )
            }

            SectionCard("正在上的课") {
                SwitchRow(
                    title = "描边高亮",
                    hint = "给当前正在上的那节课加一圈描边",
                    checked = prefs.liveOutlineEnabled,
                    onCheckedChange = repository::setLiveOutline,
                )
                if (prefs.liveOutlineEnabled) {
                    ChoiceRow(
                        title = "描边宽度",
                        options = listOf(1 to "细", 2 to "标准", 3 to "粗"),
                        selected = prefs.liveOutlineWidth,
                        onSelect = repository::setLiveOutlineWidth,
                    )
                }
            }

            SectionCard("课表背景") {
                BackgroundSection(prefs, repository, backgroundPainter, onPickImage)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BackgroundSection(
    prefs: UiPrefs,
    repository: UiPrefsRepository,
    backgroundPainter: Painter?,
    onPickImage: () -> Unit,
) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (backgroundPainter != null) {
            Image(
                painter = backgroundPainter,
                contentDescription = "当前的课表背景",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
            )
        } else {
            Text(
                text = "当前用的是默认背景。选一张图作为课表壁纸。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPickImage) {
                Text(if (backgroundPainter == null) "选择图片" else "换一张")
            }
        }

        if (backgroundPainter != null) {
            WallpaperSlider(
                label = "壁纸浓淡",
                value = prefs.backgroundOpacity,
                range = 0.15f..0.95f,
                display = "${(prefs.backgroundOpacity * 100).toInt()}%",
                hint = "图片自身的不透明度。",
                onChange = repository::setBackgroundOpacity,
            )
            WallpaperSlider(
                label = "模糊",
                value = prefs.backgroundBlur,
                range = 0f..20f,
                display = if (prefs.backgroundBlur < 0.5f) "清晰" else "${prefs.backgroundBlur.toInt()}",
                hint = "模糊一点，照片和文字更容易共存。",
                onChange = repository::setBackgroundBlur,
            )
            WallpaperSlider(
                label = "底色浓度",
                value = prefs.backgroundScrim,
                range = 0f..0.9f,
                display = "${(prefs.backgroundScrim * 100).toInt()}%",
                hint = "图片上那层主题底色。课名看不清就往右调。",
                onChange = repository::setBackgroundScrim,
            )
            OutlinedButton(
                onClick = { repository.clearBackground() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("恢复默认壁纸") }
        }
    }
}

@Composable
private fun WallpaperSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    hint: String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = display,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    hint: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            hint?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    hint: String? = null,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        hint?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { (value, label) ->
                androidx.compose.material3.FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                )
            }
        }
    }
}
