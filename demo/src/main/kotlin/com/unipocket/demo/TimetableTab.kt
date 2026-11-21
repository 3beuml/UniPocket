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
import com.unipocket.domain.Course
import com.unipocket.domain.MergedScheduleSource
import com.unipocket.domain.prefs.UiPrefs
import com.unipocket.ui.common.UniPocketErrorBanner
import com.unipocket.ui.schedule.CourseEditorDialog
import com.unipocket.ui.schedule.ScheduleGrid
import com.unipocket.ui.schedule.ScheduleViewModel
import com.unipocket.ui.schedule.WeekHeader
import com.unipocket.ui.schedule.expandByWeek

@Composable
fun TimetableTab(prefs: UiPrefs, modifier: Modifier = Modifier) {
    val config = FakeData.config
    val context = LocalContext.current
    val manualSource = remember {
        ManualCourseSource(context.getSharedPreferences("unipocket_demo", Context.MODE_PRIVATE))
    }
    val source = remember { MergedScheduleSource(FakeScheduleSource(), manualSource) }
    val viewModel: ScheduleViewModel = viewModel(
        key = "timetable",
        factory = ScheduleViewModel.factory(config, source, editableSource = manualSource),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var manageable by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<Course?>(null) }
    var confirmDelete by remember { mutableStateOf<Course?>(null) }

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
                slots = state.courses.expandByWeek(state.week),
                periods = config.periods,
                prefs = prefs,
                weekMonday = remember(state.week) {
                    config.semester.firstWeekStart.plusDays((state.week - 1) * 7L)
                },
                conflictChoices = emptyList(),
                week = state.week,
                onBlockClick = { block ->
                    val slot = block.displaySlot
                    selectedCourse = Course(
                        id = slot.id.split("#").first(),
                        name = slot.name,
                        teacher = slot.teacher,
                        room = slot.room,
                        weeks = setOf(slot.week),
                        dayOfWeek = slot.dayOfWeek,
                        periods = slot.startPeriod..slot.endPeriod,
                    )
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

    selectedCourse?.let { course ->
        LaunchedEffect(course.id) { manageable = manualSource.manages(course.id) }
        CourseDetailDialog(
            course = course,
            manageable = manageable,
            onDismiss = { selectedCourse = null },
            onEdit = { selectedCourse = null; editorTarget = course; editorOpen = true },
            onDelete = { selectedCourse = null; confirmDelete = course },
        )
    }

    if (editorOpen) {
        CourseEditorDialog(
            config = config,
            initial = editorTarget,
            onSave = { editorOpen = false; viewModel.upsertCourse(it) },
            onDismiss = { editorOpen = false },
        )
    }

    confirmDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除课程") },
            text = { Text("确定删除「${course.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = null; viewModel.deleteCourse(course.id) }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun CourseDetailDialog(
    course: Course,
    manageable: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(course.name) },
        text = {
            Column {
                course.teacher?.let { Text("教师：$it", style = MaterialTheme.typography.bodyMedium) }
                course.room?.let { Text("教室：$it", style = MaterialTheme.typography.bodyMedium) }
                Text(
                    text = "时间：周${course.dayOfWeek} 第${course.startPeriod}-${course.endPeriod}节",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("周次：${course.weeksText}", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        dismissButton = {
            if (manageable) {
                TextButton(onClick = onEdit) { Text("编辑") }
            }
        },
    )
}
