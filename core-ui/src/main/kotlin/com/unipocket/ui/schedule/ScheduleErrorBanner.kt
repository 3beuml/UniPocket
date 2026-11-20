package com.unipocket.ui.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unipocket.domain.UniPocketException

/**
 * 统一的课表错误条。**已有数据时**它会出现在课表上方，而不是替掉课表。
 *
 * 四类错误对应四种行动：网络→重试；登录失效→重新登录；格式异常→反馈/等待；
 * 未知→重试。具体文案集中在这里，接入方不用各写一套。
 */
@Composable
fun ScheduleErrorBanner(
    error: UniPocketException,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (error) {
        is UniPocketException.Network -> "网络不可用，请检查连接后重试"
        is UniPocketException.AuthExpired -> "登录状态已失效，请重新登录"
        is UniPocketException.DataFormat -> "课表数据格式异常，教务系统可能已改版"
        is UniPocketException.Unknown -> "课表加载失败"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium)
                val detail = error.message
                if (!detail.isNullOrBlank() && detail != title) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onRetry) {
                Text("重试", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}
