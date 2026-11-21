package com.unipocket.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unipocket.domain.Session
import com.unipocket.domain.TodayItem
import com.unipocket.domain.TodayStatus
import com.unipocket.domain.nextUpcoming
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 今天页（无状态）：「下一节课」卡片槽 + 把今天剩下的课排成时间轴。
 *
 * 课间空档刻意不画——只在有课时出现，避免制造"必须填满"的噪音。
 */
@Composable
fun TodayScreen(
    date: LocalDate,
    items: List<TodayItem>,
    now: LocalTime,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onCourseClick: (Session) -> Unit = {},
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date.format(DATE_FORMAT),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = weekdayName(date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 卡片槽：第一门还没结束的课；全天结束时不显示，时间轴照常
        items.nextUpcoming()?.let { next ->
            NextCourseCard(
                item = next,
                now = now,
                onClick = { onCourseClick(next.session) },
            )
        }

        Box(Modifier.weight(1f)) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                items.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "今天没有课程",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "好好休息，或者去番茄钟专注一会儿",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.session.id }) { item ->
                        TodayRow(item = item, onClick = { onCourseClick(item.session) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayRow(item: TodayItem, onClick: () -> Unit) {
    val accent = when (item.status) {
        TodayStatus.ONGOING -> MaterialTheme.colorScheme.primary
        TodayStatus.UPCOMING -> MaterialTheme.colorScheme.secondary
        TodayStatus.FINISHED -> MaterialTheme.colorScheme.outlineVariant
    }
    val contentAlpha = if (item.status == TodayStatus.FINISHED) 0.55f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.start.format(HH_MM),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            Text(
                text = item.end.format(HH_MM),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            )
        }

        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = item.session.courseName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.status == TodayStatus.ONGOING) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            val detail = listOfNotNull(item.session.roomText, item.session.teacherName)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
        }

        Text(
            text = item.statusLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (item.status == TodayStatus.FINISHED) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                accent
            },
        )
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")
private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 「下一节课」卡片槽：状态 + 倒计时 + 课名 + 详情，点击进课程详情 */
@Composable
private fun NextCourseCard(
    item: TodayItem,
    now: LocalTime,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "${item.statusLabel} · ${item.relativeText(now)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = item.session.courseName,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            val detail = listOfNotNull(
                item.timeText,
                item.session.roomText?.takeIf { it.isNotBlank() },
                item.session.teacherName?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

private fun weekdayName(date: LocalDate): String =
    listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        .getOrElse(date.dayOfWeek.value - 1) { "" }
