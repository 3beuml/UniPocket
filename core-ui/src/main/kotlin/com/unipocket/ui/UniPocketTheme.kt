package com.unipocket.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.unipocket.ui.theme.MaterialYouScheme

/**
 * 框架统一主题入口。
 *
 * @param darkTheme 是否深色；接入方根据用户偏好计算后传入
 * @param seedColor 主题种子色；传 null 用 Material 默认配色，
 *   传任意颜色则用 Material You 算法生成整套配色
 */
@Composable
fun UniPocketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seedColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(darkTheme, seedColor) {
        if (seedColor == null) {
            if (darkTheme) darkColorScheme() else lightColorScheme()
        } else {
            MaterialYouScheme.fromSeed(seedColor.toArgb(), darkTheme)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
