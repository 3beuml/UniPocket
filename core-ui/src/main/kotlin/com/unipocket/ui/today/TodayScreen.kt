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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unipocket.domain.Course
import com.unipocket.domain.TodayItem
import com.unipocket.domain.TodayStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 今天页（无状态）：把今天剩下的课排成时间轴。
 *
 * 课间空档刻意不画——只在有课时出现，避免制造"必须填满"的噪音。
 */
@Composable
fun TodayScreen(
    date: LocalDate,
    items: List<TodayItem>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onCourseClick: (Course) -> Unit = {},
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
                    items(items, key = { it.course.id }) { item ->
                        TodayRow(item = item, onClick = { onCourseClick(item.course) })
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
                text = item.course.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.status == TodayStatus.ONGOING) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            val detail = listOfNotNull(item.course.room, item.course.teacher)
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

private fun weekdayName(date: LocalDate): String =
    listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        .getOrElse(date.dayOfWeek.value - 1) { "" }
