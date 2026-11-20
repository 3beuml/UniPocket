package com.unipocket.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale

/**
 * 背景层：给整个 App 铺一张背景图，并在上面盖一层主题色遮罩。
 *
 * 框架**不负责选图与存储**——[painter] 由接入方自己从相册/文件加载后传进来，
 * 这样框架不会引入图片库、也不会碰文件权限。
 *
 * @param painter 背景图；null 时什么都不画（直接用主题背景）
 * @param dim 是否盖遮罩。默认盖，保证文字在照片上仍可读
 */
@Composable
fun AppBackground(
    painter: Painter?,
    modifier: Modifier = Modifier,
    dim: Boolean = true,
    scrimAlpha: Float = 0.55f,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (dim) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = scrimAlpha)),
                )
            }
        }
        content()
    }
}
