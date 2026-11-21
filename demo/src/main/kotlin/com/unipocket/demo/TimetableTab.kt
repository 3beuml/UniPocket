package com.unipocket.demo

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.unipocket.domain.prefs.CourseTextMode
import com.unipocket.ui.common.UniPocketErrorBanner
import com.unipocket.ui.schedule.CourseEditorDialog
import com.unipocket.ui.schedule.ScheduleViewModel
import com.unipocket.ui.schedule.WeekHeader
import com.unipocket.ui.schedule.WeekTimetable

/**
 * 课表页：教务来源（假数据）+ 手动补录来源合并，经官方 [ScheduleViewModel] 加载。
 *
 * FAB 打开补录弹窗；点手动补录的课可以编辑/删除。外校接入时把
 * [FakeScheduleSource] 换成自己的教务来源即可，补录部分不用动。
 */
@Composable
fun TimetableTab(prefs: UiPrefs, modifier: Modifier = Modifier) {
    val config = FakeData.config
    val context = LocalContext.current
    val manualSource = remember {
        ManualCourseSource(context.getSharedPreferences("unipocket_demo", Context.MODE_PRIVATE))
    }
    val source = remember { MergedScheduleSource(FakeScheduleSource(), manualSource) }
    // key 必须唯一：今天页也用 ScheduleViewModel，不给 key 会拿到同一个缓存实例
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
            WeekTimetable(
                config = config,
                courses = state.courses,
                week = state.week,
                modifier = Modifier.fillMaxSize(),
                showWeekend = prefs.showWeekend,
                
                showRoom = prefs.courseTextMode == CourseTextMode.NAME_ROOM,
                onCourseClick = { selectedCourse = it },
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
                onClick = {
                    editorTarget = null
                    editorOpen = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Text(text = "＋", fontSize = 22.sp)
            }
        }
    }

    selectedCourse?.let { course ->
        LaunchedEffect(course.id) {
            manageable = manualSource.manages(course.id)
        }
        CourseDetailDialog(
            course = course,
            manageable = manageable,
            onDismiss = { selectedCourse = null },
            onEdit = {
                selectedCourse = null
                editorTarget = course
                editorOpen = true
            },
            onDelete = {
                selectedCourse = null
                confirmDelete = course
            },
        )
    }

    if (editorOpen) {
        CourseEditorDialog(
            config = config,
            initial = editorTarget,
            onSave = { course ->
                editorOpen = false
                viewModel.upsertCourse(course)
            },
            onDismiss = { editorOpen = false },
        )
    }

    confirmDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除课程") },
            text = { Text("确定删除「${course.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    viewModel.deleteCourse(course.id)
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("取消") }
            },
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
                    text = "时间：${weekdayText(course.dayOfWeek)} 第${course.startPeriod}-${course.endPeriod}节",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("周次：${course.weeksText}", style = MaterialTheme.typography.bodyMedium)
                if (course.attributes.isNotEmpty()) {
                    Text("备注：${course.attributes.entries.joinToString("，") { "${it.key}=${it.value}" }}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        dismissButton = {
            if (manageable) {
                Row {
                    TextButton(onClick = onEdit) { Text("编辑") }
                    TextButton(onClick = onDelete) { Text("删除") }
                }
            }
        },
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

private fun weekdayText(dayOfWeek: Int): String =
    listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").getOrElse(dayOfWeek - 1) { "周$dayOfWeek" }
