package com.unipocket.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

/**
 * 一个底栏 Tab。
 *
 * @property id 稳定标识，用于记住选中项
 * @property label 底栏文字
 * @property icon 图标内容；不传时用 [label] 首字兜底（骨架期足够，接入方可换成 Material 图标）
 */
data class AppTab(
    val id: String,
    val label: String,
    val icon: (@Composable () -> Unit)? = null,
)

/**
 * 可配置外壳：底栏 + 内容区。页面顺序、数量、命名全由调用方决定。
 *
 * 接入方只需要给一组 [AppTab]，不必改框架代码：
 *
 * ```kotlin
 * AppShell(tabs = listOf(AppTab("schedule", "课表"), AppTab("rooms", "空教室")), …)
 * ```
 */
@Composable
fun AppShell(
    tabs: List<AppTab>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable (AppTab) -> Unit = {},
    content: @Composable (tab: AppTab) -> Unit,
) {
    require(tabs.isNotEmpty()) { "AppShell 至少需要一个 Tab" }
    val selected = tabs.firstOrNull { it.id == selectedTabId } ?: tabs.first()

    Scaffold(
        modifier = modifier,
        topBar = { topBar(selected) },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = tab.id == selected.id,
                        onClick = { onTabSelected(tab.id) },
                        icon = {
                            if (tab.icon != null) {
                                tab.icon()
                            } else {
                                Text(
                                    text = tab.label.take(1),
                                    fontSize = 15.sp,
                                )
                            }
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            content(selected)
        }
    }
}
