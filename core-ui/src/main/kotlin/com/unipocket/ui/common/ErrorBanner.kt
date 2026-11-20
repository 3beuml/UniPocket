package com.unipocket.ui.common

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
 * 统一错误条。【已有数据时】它出现在内容上方，而不是替掉内容。
 *
 * @param subject 出错的对象名，用于文案，如"课表""空教室数据"
 */
@Composable
fun UniPocketErrorBanner(
    error: UniPocketException,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    subject: String = "课表",
) {
    val title = when (error) {
        is UniPocketException.Network -> "网络不可用，请检查连接后重试"
        is UniPocketException.AuthExpired -> "登录状态已失效，请重新登录"
        is UniPocketException.DataFormat -> "${subject}数据格式异常，教务系统可能已改版"
        is UniPocketException.Unknown -> "${subject}加载失败"
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
