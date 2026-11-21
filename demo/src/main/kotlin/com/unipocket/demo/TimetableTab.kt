package com.unipocket.demo

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unipocket.core.ScheduleText
import com.unipocket.domain.MergedScheduleSource
import com.unipocket.domain.Session
import com.unipocket.domain.prefs.UiPrefs
import com.unipocket.ui.common.UniPocketErrorBanner
import com.unipocket.ui.schedule.CourseEditorDialog
import com.unipocket.ui.schedule.ScheduleGrid
import com.unipocket.ui.schedule.ScheduleSlot
import com.unipocket.ui.schedule.ScheduleViewModel
import com.unipocket.ui.schedule.WeekHeader
import com.unipocket.ui.schedule.expandByWeek
import java.time.LocalDate

@Composable
fun TimetableTab(prefs: UiPrefs, modifier: Modifier = Modifier) {
    val semester = FakeData.semester
    val periods = FakeData.periods
    val context = LocalContext.current
    val manualSource = remember {
        ManualCourseSource(context.getSharedPreferences("unipocket_demo", Context.MODE_PRIVATE))
    }
    val source = remember { MergedScheduleSource(FakeScheduleSource(), manualSource) }
    val viewModel: ScheduleViewModel = viewModel(
        key = "timetable",
        factory = ScheduleViewModel.factory(semester, source, editableSource = manualSource),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var selectedSlot by remember { mutableStateOf<ScheduleSlot?>(null) }
    /** 点中的课在全学期的全部周记录（假数据直接查 FakeData，手动课查手动源） */
    var selectedSiblings by remember { mutableStateOf<List<Session>>(emptyList()) }
    var manageable by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<ScheduleSlot?>(null) }
    var confirmDelete by remember { mutableStateOf<ScheduleSlot?>(null) }

    Column(modifier.fillMaxSize()) {
        WeekHeader(
            week = state.week,
            weekCount = state.weekCount,
            isCurrentWeek = state.isCurrentWeek,
            onPrevious = viewModel::previousWeek,
            onNext = viewModel::nextWeek,
            onToday = viewModel::goToToday,
        )

        state.error?.let { error ->
            UniPocketErrorBanner(error = error, onRetry = viewModel::retry)
        }

        Box(Modifier.weight(1f)) {
            ScheduleGrid(
                slots = state.sessions.expandByWeek(state.week),
                periods = periods,
                prefs = prefs,
                weekMonday = remember(state.week) {
                    ScheduleText.mondayOf(semester.startDate, state.week)
                },
                conflictChoices = emptyList(),
                week = state.week,
                onBlockClick = { block ->
                    selectedSlot = block.displaySlot
                },
            )

            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.isEmpty -> Text(
                    text = "本周没有课程",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FloatingActionButton(
                onClick = { editorTarget = null; editorOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Text(text = "＋", fontSize = 22.sp)
            }
        }
    }

    // 选中格子后，查这门课在全学期的周记录与可编辑性
    LaunchedEffect(selectedSlot?.id) {
        val slot = selectedSlot ?: return@LaunchedEffect
        manageable = manualSource.manages(slot.id)
        selectedSiblings = if (manageable) {
            manualSource.loadSessions().filter {
                it.courseCode == slot.courseCode &&
                    it.weekday == slot.dayOfWeek &&
                    it.startUnit == slot.startPeriod
            }
        } else {
            FakeData.sessions.filter {
                it.courseCode == slot.courseCode &&
                    it.weekday == slot.dayOfWeek &&
                    it.startUnit == slot.startPeriod
            }
        }
    }

    selectedSlot?.let { slot ->
        CourseDetailDialog(
            slot = slot,
            weeks = selectedSiblings.map { it.weekIndex },
            manageable = manageable,
            onDismiss = { selectedSlot = null },
            onEdit = { selectedSlot = null; editorTarget = slot; editorOpen = true },
            onDelete = { selectedSlot = null; confirmDelete = slot },
        )
    }

    if (editorOpen) {
        CourseEditorDialog(
            weeks = semester.weekIndices,
            periodCount = periods.size,
            semesterId = semester.id,
            initial = editorTarget?.let { slot -> selectedSiblings.takeIf { it.isNotEmpty() && manageable } },
            onSave = { editorOpen = false; viewModel.upsertSessions(it) },
            onDismiss = { editorOpen = false },
        )
    }

    confirmDelete?.let { slot ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除课程") },
            text = { Text("确定删除「${slot.name}」的全部周次吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    // 新模型一门课 = 多条周记录，逐条删；load 有单飞保护不会抖
                    selectedSiblings.forEach { viewModel.deleteSession(it.id) }
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun CourseDetailDialog(
    slot: ScheduleSlot,
    weeks: List<Int>,
    manageable: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(slot.name) },
        text = {
            Column {
                slot.teacher?.let { Text("教师：$it", style = MaterialTheme.typography.bodyMedium) }
                slot.room?.let { Text("教室：$it", style = MaterialTheme.typography.bodyMedium) }
                Text(
                    text = "时间：${ScheduleText.weekdayName(slot.dayOfWeek)} 第${slot.startPeriod}-${slot.endPeriod}节",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "周次：${ScheduleText.compressWeeks(weeks)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        dismissButton = {
            if (manageable) {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除") }
            }
        },
    )
}
