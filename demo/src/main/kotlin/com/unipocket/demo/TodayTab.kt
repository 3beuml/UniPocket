package com.unipocket.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unipocket.domain.TodayStatus
import com.unipocket.domain.todayItems
import com.unipocket.domain.prefs.UiPrefs
import com.unipocket.ui.components.GlassCard
import com.unipocket.ui.nav.DOCK_CLEARANCE
import com.unipocket.ui.schedule.ScheduleViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val HH_MM = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayTab(prefs: UiPrefs, modifier: Modifier = Modifier) {
    val config = FakeData.config
    val source = remember { FakeScheduleSource() }
    val viewModel: ScheduleViewModel = viewModel(
        key = "today",
        factory = ScheduleViewModel.factory(config, source),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); now = LocalTime.now() }
    }

    val today = LocalDate.now()
    val items = remember(state.courses, now, today) {
        state.courses.todayItems(config, today.dayOfWeek.value, now)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("首页") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
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

            // ── 今天的时间轴 GlassCard ─────────────────────────────────
            GlassCard(modifier = Modifier.fillMaxWidth().clickable { }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CalendarMonth, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("今天", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "第 ${config.semester.currentWeek() ?: 1} 周",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${today.monthValue}.${today.dayOfMonth}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }

                    if (state.isLoading) {
                        Text("读取中…", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (items.isEmpty()) {
                        Text("今天没有课，也没有专注记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        HorizontalDivider(Modifier.padding(top = 2.dp))
                        items.forEach { item ->
                            TimelineRow(item, now)
                        }
                    }
                }
            }

            // ── 空教室查询入口 GlassCard ────────────────────────────────
            GlassCard(modifier = Modifier.fillMaxWidth().clickable { }) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.MeetingRoom, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("空教室查询", style = MaterialTheme.typography.titleMedium)
                        Text("按楼看每间教室哪节有课",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(item: com.unipocket.domain.TodayItem, now: LocalTime) {
    val accent = when (item.status) {
        TodayStatus.ONGOING -> MaterialTheme.colorScheme.primary
        TodayStatus.UPCOMING -> MaterialTheme.colorScheme.secondary
        TodayStatus.FINISHED -> MaterialTheme.colorScheme.outlineVariant
    }
    val alpha = if (item.status == TodayStatus.FINISHED) 0.55f else 1f

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = item.start.format(HH_MM),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            modifier = Modifier.width(46.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = item.course.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (item.status == TodayStatus.ONGOING) FontWeight.Medium else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = listOfNotNull(
                item.course.room?.takeIf { it.isNotBlank() },
                item.course.teacher?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = item.statusLabel,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
    }
}
