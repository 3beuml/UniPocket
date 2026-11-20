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
import com.unipocket.domain.Course
import com.unipocket.domain.SchoolConfig
import com.unipocket.domain.parseWeeksText
import com.unipocket.domain.weeksToText
import java.util.UUID

private enum class WeeksMode { ALL, ODD, EVEN, CUSTOM }

private val DAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 补录/编辑课程的弹窗（无状态壳 + 内部表单状态）。
 *
 * 周次提供「每周 / 单周 / 双周 / 自定义」四档；自定义用 [parseWeeksText] 解析，
 * 非法输入整体拒绝。保存时生成稳定 id（新课时 `manual-<uuid>`，编辑保留原 id）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditorDialog(
    config: SchoolConfig,
    initial: Course?,
    onSave: (Course) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var teacher by remember { mutableStateOf(initial?.teacher.orEmpty()) }
    var room by remember { mutableStateOf(initial?.room.orEmpty()) }
    var day by remember { mutableStateOf(initial?.dayOfWeek ?: 1) }
    var start by remember { mutableStateOf(initial?.startPeriod ?: 1) }
    var end by remember { mutableStateOf(initial?.endPeriod ?: minOf(2, config.periodCount)) }

    val allWeeks = remember { (1..config.semester.weekCount).toSet() }
    val oddWeeks = remember { allWeeks.filter { it % 2 == 1 }.toSet() }
    val evenWeeks = remember { allWeeks.filter { it % 2 == 0 }.toSet() }
    var weeksMode by remember {
        mutableStateOf(
            when {
                initial == null -> WeeksMode.ALL
                initial.weeks == allWeeks -> WeeksMode.ALL
                initial.weeks == oddWeeks -> WeeksMode.ODD
                initial.weeks == evenWeeks -> WeeksMode.EVEN
                else -> WeeksMode.CUSTOM
            },
        )
    }
    var customText by remember {
        mutableStateOf(
            if (weeksMode == WeeksMode.CUSTOM && initial != null) weeksToText(initial.weeks) else "",
        )
    }

    val weeks: Set<Int> = when (weeksMode) {
        WeeksMode.ALL -> allWeeks
        WeeksMode.ODD -> oddWeeks
        WeeksMode.EVEN -> evenWeeks
        WeeksMode.CUSTOM -> parseWeeksText(customText) ?: emptySet()
    }

    val invalidWeeks = weeksMode == WeeksMode.CUSTOM && customText.isNotBlank() && weeks.isEmpty()
    val invalidRange = start > end
    val valid = name.isNotBlank() && weeks.isNotEmpty() && !invalidRange

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
                    periodCount = config.periodCount,
                )

                Text("结束节次", style = MaterialTheme.typography.labelMedium)
                PeriodChips(
                    selected = end,
                    onSelect = { end = it },
                    periodCount = config.periodCount,
                )

                Text("周次", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WeeksMode.entries.forEach { mode ->
                        FilterChip(
                            selected = weeksMode == mode,
                            onClick = {
                                weeksMode = mode
                                if (mode == WeeksMode.CUSTOM && customText.isBlank() && initial != null) {
                                    customText = weeksToText(initial.weeks)
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
                    onSave(
                        Course(
                            id = initial?.id ?: "manual-${UUID.randomUUID()}",
                            name = name.trim(),
                            teacher = teacher.trim().takeIf { it.isNotBlank() },
                            room = room.trim().takeIf { it.isNotBlank() },
                            weeks = weeks,
                            dayOfWeek = day,
                            periods = start..end,
                        ),
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
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
