package com.unipocket.feature.room

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unipocket.domain.Period
import com.unipocket.ui.common.UniPocketErrorBanner
import com.unipocket.ui.nav.DOCK_CLEARANCE

private val FREE_BG = Color(0xFFC8E6C9)
private val BUSY_BG = Color(0xFFFFCDD2)

@Composable
fun RoomScreen(
    periods: List<Period>,
    state: RoomUiState,
    modifier: Modifier = Modifier,
    onDaySelected: (Int) -> Unit = {},
    onBuildingSelected: (String?) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Column(modifier) {
        state.error?.let { error ->
            UniPocketErrorBanner(error = error, onRetry = onRetry, subject = "空教室数据")
        }

        LazyRow(Modifier.fillMaxWidth().padding(start = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(7) { index ->
                val day = index + 1
                FilterChip(selected = state.dayOfWeek == day, onClick = { onDaySelected(day) },
                    label = { Text(listOf("一","二","三","四","五","六","日")[index]) })
            }
        }

        LazyRow(Modifier.fillMaxWidth().padding(start = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(selected = state.selectedBuildingId == null, onClick = { onBuildingSelected(null) }, label = { Text("全部") }) }
            items(state.buildings, key = { it.id }) { building ->
                FilterChip(selected = state.selectedBuildingId == building.id, onClick = { onBuildingSelected(building.id) }, label = { Text(building.name) })
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (state.isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth())

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.allRooms.isEmpty() && !state.isLoading -> Text(
                    text = "没有教室数据", modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> OccupancyGrid(periods, state)
            }
        }
    }
}

@Composable
private fun OccupancyGrid(periods: List<Period>, state: RoomUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = DOCK_CLEARANCE),
    ) {
        state.groups.forEach { group ->
            item(key = "h-${group.building?.id ?: "o"}") {
                Text("${group.title} · ${group.rooms.size} 间",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            item(key = "p-${group.building?.id ?: "o"}") { PeriodHeaders(periods) }
            items(group.rooms, key = { it.id }) { room ->
                OccupancyRow(room, state.freeMatrix[room.id] ?: emptySet(), periods)
            }
            item(key = "d-${group.building?.id ?: "o"}") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun PeriodHeaders(periods: List<Period>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        androidx.compose.foundation.layout.Spacer(Modifier.width(56.dp))
        periods.forEach { p ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${p.indexNo}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(p.startText, fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

@Composable
private fun OccupancyRow(room: com.unipocket.domain.LocationNode, freePeriods: Set<Int>, periods: List<Period>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f))
        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(room.name, Modifier.width(56.dp).padding(start = 4.dp), fontSize = 11.sp, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        periods.forEach { p ->
            val free = p.indexNo in freePeriods
            Box(Modifier.weight(1f).padding(horizontal = 1.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (free) FREE_BG else BUSY_BG), contentAlignment = Alignment.Center) {
                    Text(if (free) "空" else "课", fontSize = 8.sp,
                        color = if (free) Color(0xFF2E7D32) else Color(0xFFC62828))
                }
            }
        }
    }
}
