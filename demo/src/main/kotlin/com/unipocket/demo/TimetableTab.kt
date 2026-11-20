package com.unipocket.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unipocket.domain.Course
import com.unipocket.domain.SchedulePrefs
import com.unipocket.ui.common.UniPocketErrorBanner
import com.unipocket.ui.schedule.ScheduleViewModel
import com.unipocket.ui.schedule.WeekHeader
import com.unipocket.ui.schedule.WeekTimetable

/**
 * 课表页：数据经由官方 [ScheduleViewModel] 加载。
 * 换数据只需要换一个 `ScheduleSource`，这个文件其余部分不用动。
 */
@Composable
fun TimetableTab(prefs: SchedulePrefs, modifier: Modifier = Modifier) {
    val config = FakeData.config
    val source = remember { FakeScheduleSource() }
    val viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModel.factory(config, source),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

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
                showTeacher = prefs.showTeacher,
                showRoom = prefs.showRoom,
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
        }
    }

    selectedCourse?.let { course ->
        CourseDetailDialog(course = course, onDismiss = { selectedCourse = null })
    }
}

@Composable
private fun CourseDetailDialog(course: Course, onDismiss: () -> Unit) {
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
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

private fun weekdayText(dayOfWeek: Int): String =
    listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").getOrElse(dayOfWeek - 1) { "周$dayOfWeek" }
