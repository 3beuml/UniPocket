package com.unipocket.ui.schedule

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unipocket.domain.Session
import java.util.UUID

private enum class WeeksMode { ALL, ODD, EVEN, CUSTOM }

private val DAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 补录/编辑课程的弹窗（无状态壳 + 内部表单状态）。
 *
 * 新模型一条 [Session] 只对应一个教学周，所以"一门课"在这里是表单 + 周次集合，
 * 保存时展开成每周一条的 [Session] 列表交给调用方逐条 upsert。
 * 周次提供「每周 / 单周 / 双周 / 自定义」四档；自定义按 `1-8,10-16` 解析（[weeksFromText]），
 * 非法输入整体拒绝。编辑时已存在的周沿用原 id（保住冲突选择记忆），新周生成新 id。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditorDialog(
    /** 可选教学周（如 [com.unipocket.domain.Semester.weekIndices]），决定"每周"档的范围 */
    weeks: List<Int>,
    periodCount: Int,
    /** 新建记录挂在哪个学期下 */
    semesterId: Int,
    /** 编辑时传同一门课（同 courseCode）的全部周记录；null = 新增 */
    initial: List<Session>?,
    onSave: (List<Session>) -> Unit,
    onDismiss: () -> Unit,
) {
    val head = initial?.firstOrNull()
    var name by remember { mutableStateOf(head?.courseName.orEmpty()) }
    var teacher by remember { mutableStateOf(head?.teacherName.orEmpty()) }
    var room by remember { mutableStateOf(head?.roomText.orEmpty()) }
    var day by remember { mutableStateOf(head?.weekday ?: 1) }
    var start by remember { mutableStateOf(head?.startUnit ?: 1) }
    var end by remember { mutableStateOf(head?.endUnit ?: minOf(2, periodCount)) }

    val allWeeks = remember { weeks.filter { it > 0 }.distinct().sorted() }
    val oddWeeks = remember { allWeeks.filter { it % 2 == 1 } }
    val evenWeeks = remember { allWeeks.filter { it % 2 == 0 } }
    val initialWeeks = remember { initial.orEmpty().map { it.weekIndex }.distinct().sorted() }
    var weeksMode by remember {
        mutableStateOf(
            when {
                initialWeeks.isEmpty() -> WeeksMode.ALL
                initialWeeks == allWeeks -> WeeksMode.ALL
                initialWeeks == oddWeeks -> WeeksMode.ODD
                initialWeeks == evenWeeks -> WeeksMode.EVEN
                else -> WeeksMode.CUSTOM
            },
        )
    }
    var customText by remember {
        mutableStateOf(if (weeksMode == WeeksMode.CUSTOM) weeksToText(initialWeeks) else "")
    }

    val selectedWeeks: List<Int> = when (weeksMode) {
        WeeksMode.ALL -> allWeeks
        WeeksMode.ODD -> oddWeeks
        WeeksMode.EVEN -> evenWeeks
        WeeksMode.CUSTOM -> weeksFromText(customText).filter { it in allWeeks }
    }

    val invalidWeeks = weeksMode == WeeksMode.CUSTOM && customText.isNotBlank() && selectedWeeks.isEmpty()
    val invalidRange = start > end
    val valid = name.isNotBlank() && selectedWeeks.isNotEmpty() && !invalidRange

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "补录课程" else "编辑课程") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("课名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("教师") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("教室") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text("星期", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DAY_LABELS.forEachIndexed { index, label ->
                        val value = index + 1
                        FilterChip(
                            selected = day == value,
                            onClick = { day = value },
                            label = { Text(label) },
                        )
                    }
                }

                Text("起始节次", style = MaterialTheme.typography.labelMedium)
                PeriodChips(
                    selected = start,
                    onSelect = {
                        start = it
                        if (end < it) end = it
                    },
                    periodCount = periodCount,
                )

                Text("结束节次", style = MaterialTheme.typography.labelMedium)
                PeriodChips(
                    selected = end,
                    onSelect = { end = it },
                    periodCount = periodCount,
                )

                Text("周次", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WeeksMode.entries.forEach { mode ->
                        FilterChip(
                            selected = weeksMode == mode,
                            onClick = {
                                weeksMode = mode
                                if (mode == WeeksMode.CUSTOM && customText.isBlank() && initialWeeks.isNotEmpty()) {
                                    customText = weeksToText(initialWeeks)
                                }
                            },
                            label = {
                                Text(
                                    when (mode) {
                                        WeeksMode.ALL -> "每周"
                                        WeeksMode.ODD -> "单周"
                                        WeeksMode.EVEN -> "双周"
                                        WeeksMode.CUSTOM -> "自定义"
                                    },
                                )
                            },
                        )
                    }
                }
                if (weeksMode == WeeksMode.CUSTOM) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = { Text("如 1-8,10-16") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (invalidWeeks) {
                    Text(
                        text = "周次格式无法识别",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (invalidRange) {
                    Text(
                        text = "结束节次不能早于起始节次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val oldById = initial.orEmpty().associateBy { it.weekIndex }
                    onSave(
                        selectedWeeks.map { week ->
                            val previous = oldById[week]
                            previous?.copy(
                                courseName = name.trim(),
                                teacherName = teacher.trim().takeIf { it.isNotBlank() },
                                roomText = room.trim().takeIf { it.isNotBlank() },
                                weekday = day,
                                startUnit = start,
                                endUnit = end,
                            ) ?: Session(
                                id = "manual-${UUID.randomUUID()}-w$week",
                                semesterId = semesterId,
                                source = "custom",
                                courseCode = name.trim(),
                                courseName = name.trim(),
                                weekIndex = week,
                                weekday = day,
                                startUnit = start,
                                endUnit = end,
                                date = null,
                                startTime = null,
                                endTime = null,
                                teacherName = teacher.trim().takeIf { it.isNotBlank() },
                                roomText = room.trim().takeIf { it.isNotBlank() },
                                campusText = null,
                                color = null,
                            )
                        },
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** "1-8,10-16" → [1..8, 10..16]；识别不出返回空表 */
internal fun weeksFromText(text: String): List<Int> {
    val weeks = mutableListOf<Int>()
    for (part in text.split(',', '，', ' ')) {
        val trimmed = part.trim()
        if (trimmed.isEmpty()) continue
        val range = Regex("^(\\d+)[-–](\\d+)$").find(trimmed)
        if (range != null) {
            val from = range.groupValues[1].toIntOrNull() ?: return emptyList()
            val to = range.groupValues[2].toIntOrNull() ?: return emptyList()
            if (from > to || to - from > 200) return emptyList()
            weeks += (from..to).toList()
        } else {
            weeks += trimmed.toIntOrNull() ?: return emptyList()
        }
    }
    return weeks.distinct().sorted()
}

/** [1,2,3,6,8] → "1-3,6,8"（回填输入框用；与 [weeksFromText] 互逆） */
internal fun weeksToText(weeks: List<Int>): String {
    val w = weeks.filter { it > 0 }.distinct().sorted()
    if (w.isEmpty()) return ""
    val parts = mutableListOf<String>()
    var start = w.first()
    var prev = start
    for (cur in w.drop(1)) {
        if (cur == prev + 1) {
            prev = cur
            continue
        }
        parts += if (start == prev) "$start" else "$start-$prev"
        start = cur
        prev = cur
    }
    parts += if (start == prev) "$start" else "$start-$prev"
    return parts.joinToString(",")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodChips(
    selected: Int,
    onSelect: (Int) -> Unit,
    periodCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (index in 1..periodCount) {
            FilterChip(
                selected = selected == index,
                onClick = { onSelect(index) },
                label = { Text("$index") },
            )
        }
    }
}
