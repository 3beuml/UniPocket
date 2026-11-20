package com.unipocket.feature.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 番茄钟页（无状态）。
 *
 * 只展示 [FocusState] 并回调按钮；计时逻辑在 [FocusStore]，页面不持有任何时间状态。
 */
@Composable
fun FocusScreen(
    state: FocusState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = state.phaseLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(24.dp))

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

        Spacer(Modifier.height(32.dp))

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
        val arcSize = androidx.compose.ui.geometry.Size(
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
