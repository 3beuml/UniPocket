package com.unipocket.feature.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 番茄钟页（无状态）：计时圆环 + 统计区。
 *
 * 只展示 [FocusState] 与可选的 [FocusStats] 并回调按钮；计时逻辑在 [FocusStore]，
 * 记录落库在 ViewModel。未提供记录存储时 [stats] 为 null，统计区不显示。
 */
@Composable
fun FocusScreen(
    state: FocusState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    stats: FocusStats? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.phaseLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(20.dp))

        Box(contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = state.progress,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                accentColor = MaterialTheme.colorScheme.primary,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.remainingText,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                )
                Text(
                    text = "已完成 ${state.completedWorkSessions} 个番茄",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = if (state.isRunning) onPause else onStart) {
            Text(
                text = when {
                    state.isRunning -> "暂停"
                    state.phase == FocusPhase.WORK -> "开始专注"
                    else -> "开始休息"
                },
            )
        }

        TextButton(onClick = onReset) {
            Text("重置")
        }

        if (state.phase == FocusPhase.WORK && state.cycleLength > 0) {
            Text(
                text = "长休息前 ${state.completedWorkSessions % state.cycleLength}/${state.cycleLength} 个",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        stats?.let { s ->
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Text(
                text = "统计",
                modifier = Modifier.align(Alignment.Start),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "今日 ${s.todayCount} 个 · ${s.todayMinutes} 分钟",
                modifier = Modifier.align(Alignment.Start),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "累计 ${s.totalCount} 个 · ${s.totalMinutes} 分钟",
                modifier = Modifier.align(Alignment.Start),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))
            SevenDayBars(days = s.last7Days)
        }
    }
}

/** 最近 7 天的迷你柱状图：柱高按最大分钟数归一，无记录的天画基线 */
@Composable
private fun SevenDayBars(days: List<DayMinutes>) {
    val maxMinutes = days.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.forEach { day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                val barHeight = if (day.minutes > 0) {
                    (48.dp * day.minutes / maxMinutes).coerceAtLeast(4.dp)
                } else {
                    3.dp
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .background(if (day.minutes > 0) barColor else emptyColor),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${day.date.dayOfMonth}",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    trackColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    Canvas(Modifier.size(240.dp)) {
        val strokeWidth = 14.dp.toPx()
        val inset = strokeWidth / 2f
        val arcSize = Size(
            width = size.width - strokeWidth,
            height = size.height - strokeWidth,
        )
        val topLeft = Offset(inset, inset)

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        if (progress > 0f) {
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}
