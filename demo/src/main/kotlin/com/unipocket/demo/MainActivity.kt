package com.unipocket.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unipocket.domain.Course
import com.unipocket.ui.UniPocketTheme
import com.unipocket.ui.schedule.WeekHeader
import com.unipocket.ui.schedule.WeekTimetable

/**
 * 假数据课表页 —— 外校 fork 的起点。
 *
 * 除了「换成自己的数据」，这里刻意保持最小：只有一个课表页 + 课程详情弹窗。
 * 设置、番茄钟、空教室等页面后续从既有实现搬进来。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniPocketTheme {
                TimetableScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableScreen() {
    val config = FakeData.config
    val semester = config.semester
    val currentWeek = remember { semester.currentWeek() ?: 1 }
    var week by rememberSaveable { mutableStateOf(currentWeek) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(config.schoolName) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            WeekHeader(
                week = week,
                weekCount = semester.weekCount,
                isCurrentWeek = week == currentWeek,
                onPrevious = { if (week > 1) week-- },
                onNext = { if (week < semester.weekCount) week++ },
                onToday = { week = currentWeek },
            )
            WeekTimetable(
                config = config,
                courses = FakeData.courses,
                week = week,
                modifier = Modifier.weight(1f),
                onCourseClick = { selectedCourse = it },
            )
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
