package com.unipocket.feature.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unipocket.domain.SchoolConfig
import com.unipocket.ui.common.UniPocketErrorBanner

private val DayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 空教室页（无状态）。
 *
 * 三个筛选维度：星期、节次（单节）、楼栋；结果按楼栋分组。
 * 数据全部来自 [RoomUiState]，页面自己不请求、不持有状态。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    config: SchoolConfig,
    state: RoomUiState,
    modifier: Modifier = Modifier,
    onDaySelected: (Int) -> Unit = {},
    onPeriodSelected: (Int) -> Unit = {},
    onBuildingSelected: (String?) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Column(modifier) {
        state.error?.let { error ->
            UniPocketErrorBanner(
                error = error,
                onRetry = onRetry,
                subject = "空教室数据",
            )
        }

        FilterRow {
            items(7) { index ->
                val day = index + 1
                FilterChip(
                    selected = state.dayOfWeek == day,
                    onClick = { onDaySelected(day) },
                    label = { Text(DayLabels[index]) },
                )
            }
        }

        FilterRow {
            items(state.periodCount) { index ->
                val period = index + 1
                FilterChip(
                    selected = state.periodIndex == period,
                    onClick = { onPeriodSelected(period) },
                    label = { Text("第 $period 节") },
                )
            }
        }

        FilterRow {
            item {
                FilterChip(
                    selected = state.selectedBuildingId == null,
                    onClick = { onBuildingSelected(null) },
                    label = { Text("全部") },
                )
            }
            items(state.buildings, key = { it.id }) { building ->
                FilterChip(
                    selected = state.selectedBuildingId == building.id,
                    onClick = { onBuildingSelected(building.id) },
                    label = { Text(building.name) },
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(Modifier.weight(1f)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.isEmpty -> Text(
                    text = "该时段没有空闲教室",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> RoomList(groups = state.groups)
            }

            if (state.isRefreshing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RoomList(groups: List<RoomGroup>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        groups.forEach { group ->
            item(key = "building-${group.building?.id ?: "other"}") {
                Text(
                    text = "${group.title} · ${group.rooms.size} 间",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(group.rooms, key = { it.id }) { room ->
                Text(
                    text = room.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item(key = "divider-${group.building?.id ?: "other"}") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun FilterRow(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
