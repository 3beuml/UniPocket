package com.unipocket.ui.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 周次翻页头（无状态）。
 *
 * 翻页边界由调用方用 [onPrevious] / [onNext] 的 enabled 逻辑表达；
 * 这里只管显示与点击回调，不持有"当前第几周"。
 */
@Composable
fun WeekHeader(
    week: Int,
    weekCount: Int,
    isCurrentWeek: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious, enabled = week > 1) {
            Text("‹", fontSize = 22.sp)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "第 $week 周",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (isCurrentWeek) "本周" else "共 $weekCount 周",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onNext, enabled = week < weekCount) {
            Text("›", fontSize = 22.sp)
        }
        TextButton(onClick = onToday, enabled = !isCurrentWeek) {
            Text("今天", fontSize = 12.sp)
        }
    }
}
