package com.unipocket.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * 背景层：给整个 App 铺一张背景图，并按壁纸三参数调节观感。
 *
 * 框架**不负责选图与存储**——[painter] 由接入方自己从相册/文件加载后传进来，
 * 这样框架不会引入图片库、也不会碰文件权限。
 *
 * 观感三参数（与 ZZUPocket 口径一致）：
 *  - [backgroundOpacity]：图片自身的不透明度（越低越淡）；
 *  - [backgroundBlur]：模糊半径（dp），0 = 完全清晰；
 *  - [backgroundScrim]：图片上那层主题底色的强度，越大字越清楚。
 */
@Composable
fun AppBackground(
    painter: Painter?,
    modifier: Modifier = Modifier,
    backgroundOpacity: Float = 0.7f,
    backgroundBlur: Float = 6f,
    backgroundScrim: Float = 0.4f,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        if (painter != null) {
            // 图片垫一层主题底色（1 - 不透明度），避免透明处直接露出下层
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 1f - backgroundOpacity)),
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(backgroundBlur.dp),
                contentScale = ContentScale.Crop,
                alpha = backgroundOpacity,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = backgroundScrim)),
            )
        }
        content()
    }
}
