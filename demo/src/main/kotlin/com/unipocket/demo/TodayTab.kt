package com.unipocket.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unipocket.ui.schedule.ScheduleViewModel
import com.unipocket.ui.today.TodayScreen
import com.unipocket.ui.today.todayItems
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

/**
 * 今天页：复用官方 [ScheduleViewModel] 的课程数据，只按"今天/当前时刻"重新整理。
 * 每 30 秒刷新一次当前时间，跨节次/下课时状态会自动变。
 */
@Composable
fun TodayTab(modifier: Modifier = Modifier) {
    val config = FakeData.config
    val source = remember { FakeScheduleSource() }
    val viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModel.factory(config, source),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalTime.now()
        }
    }

    val today = LocalDate.now()
    val items = remember(state.courses, now, today) {
        state.courses.todayItems(config, today.dayOfWeek.value, now)
    }

    TodayScreen(
        date = today,
        items = items,
        modifier = modifier,
        isLoading = state.isLoading,
    )
}
