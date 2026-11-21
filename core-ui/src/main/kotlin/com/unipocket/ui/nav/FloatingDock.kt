package com.unipocket.ui.nav

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.unipocket.ui.components.LocalShowDescriptions
import com.unipocket.ui.theme.rememberEffectiveDark

/**
 * 底栏目的地配置：填充/线性图标成对，选中态用 filled，未选中用 outlined（M3 惯例）。
 *
 * @param id 稳定标识，用于记住选中项
 * @param label 无障碍描述
 * @param icon 选中态图标（filled）
 * @param outlinedIcon 未选中态图标（outlined）
 */
data class DockDestination(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector,
)

/**
 * 悬浮胶囊 dock 为「滚动内容」预留下的底部余量：
 * 胶囊 56 + 上下边距 16 = 72，再加 16 呼吸。
 * dock 是覆盖层后内容全高滚动（从胶囊底下穿过），各滚动容器把这个值挂到
 * contentPadding 上，**最后一条内容才能完整滚出胶囊**。
 */
val DOCK_CLEARANCE = 88.dp

/**
 * 全应用导航壳（从 ZZUPocket `AppNav` 移植）。
 *
 * 与标准 Scaffold bottomBar 的核心区别：dock 是**覆盖层**，不是 bottomBar——
 * 内容全高铺满、滚动时从胶囊底下穿过，才是真悬浮。
 *
 * 壁纸层铺满整屏（包括 dock 下面），dock 胶囊的透明度随壁纸模式变化。
 *
 * @param destinations 底栏目的地列表（接入方自行配置，可隐藏任意 tab）
 * @param selectedTabId 当前选中的 tab id
 * @param onTabSelected tab 点击回调
 * @param darkMode 深浅色偏好（影响 dock 配色口径）
 * @param wallpaperPainter 背景图 painter；null = 无壁纸
 * @param wallpaperScrim 壁纸底色浓度
 * @param showTabInDock 各 tab 是否显示在 dock 里（如可隐藏「专注」格）
 * @param content 页面内容；接收当前选中的 [DockDestination]
 */
@Composable
fun UniPocketNavHost(
    destinations: List<DockDestination>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit,
    darkMode: com.unipocket.domain.prefs.DarkMode,
    modifier: Modifier = Modifier,
    wallpaperPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    wallpaperScrim: Float = 0.4f,
    wallpaperNavBar: Boolean = true,
    content: @Composable (destination: DockDestination) -> Unit,
) {
    val showDock = destinations.any { it.id == selectedTabId }
    val dockDark = rememberEffectiveDark(darkMode)
    val wallpaperActive = wallpaperPainter != null

    // dock 胶囊透明度：壁纸模式尊重「透明导航栏」偏好（更透），其余页面 0.5
    val dockAlpha = if (wallpaperActive && wallpaperNavBar) 0.35f else 0.5f
    val dockColor = when {
        wallpaperActive && wallpaperNavBar -> MaterialTheme.colorScheme.surface.copy(alpha = dockAlpha)
        dockDark -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface.copy(alpha = dockAlpha)
    }
    val dockElevation = when {
        wallpaperActive && wallpaperNavBar -> 6.dp
        dockDark -> 1.dp
        else -> 6.dp
    }

    CompositionLocalProvider(LocalShowDescriptions provides true) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // ── 壁纸图层：铺满整屏，包括 dock 下面 ────────────────────────
            if (wallpaperPainter != null) {
                androidx.compose.foundation.Image(
                    painter = wallpaperPainter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.7f,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = wallpaperScrim)),
                )
            }

            // ── 页面内容：全高铺满，从 dock 底下穿过 ──────────────────────
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) { _ ->
                val selected = destinations.firstOrNull { it.id == selectedTabId } ?: destinations.first()
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    content(selected)
                }
            }

            // ── 悬浮胶囊 dock：覆盖层 ──────────────────────────────────────
            if (showDock) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = dockColor,
                        tonalElevation = dockElevation,
                        shadowElevation = 0.dp,
                        border = BorderStroke(
                            0.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                ),
                            ),
                        ),
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = dockElevation,
                                shape = RoundedCornerShape(28.dp),
                                clip = false,
                            ),
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            destinations.forEach { destination ->
                                val isSelected = destination.id == selectedTabId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clickable { onTabSelected(destination.id) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    !isSelected -> Color.Transparent
                                                    dockDark -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                                },
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        androidx.compose.material3.Icon(
                                            if (isSelected) destination.icon else destination.outlinedIcon,
                                            contentDescription = destination.label,
                                            tint = when {
                                                !isSelected -> MaterialTheme.colorScheme.onSurfaceVariant
                                                dockDark -> MaterialTheme.colorScheme.onPrimaryContainer
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.size(23.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
