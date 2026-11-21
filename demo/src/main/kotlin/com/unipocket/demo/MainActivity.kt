package com.unipocket.demo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unipocket.domain.prefs.UiPrefsRepository
import com.unipocket.ui.AppBackground
import com.unipocket.ui.AppShell
import com.unipocket.ui.AppTab
import com.unipocket.ui.theme.UniPocketTheme

private val demoTabs = listOf(
    AppTab(id = "today", label = "今天"),
    AppTab(id = "schedule", label = "课表"),
    AppTab(id = "rooms", label = "空教室"),
    AppTab(id = "focus", label = "专注"),
    AppTab(id = "settings", label = "设置"),
)

/**
 * demo 入口：偏好存储 → 主题 → 可配置底栏 → 五个页面。
 *
 * 想加自己的页面（电费、一卡通……）只需要往 [demoTabs] 里加一项、
 * 在下面的 `when` 里加一个分支，不用改框架。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiPrefsRepository = remember {
        UiPrefsRepository(
            store = SharedPreferencesPrefsStore(
                context.getSharedPreferences("unipocket_demo", Context.MODE_PRIVATE),
            ),
            scope = scope,
        )
    }
    LaunchedEffect(uiPrefsRepository) {
        uiPrefsRepository.start()
    }
    val prefs by uiPrefsRepository.prefs.collectAsStateWithLifecycle()

    val backgroundPainter = rememberBackgroundPainter(prefs.backgroundPath)
    val hasBackground = backgroundPainter != null

    UniPocketTheme(
        darkMode = prefs.darkMode,
        preset = prefs.themePreset,
        darkContrast = prefs.darkContrast,
        darkTinted = prefs.darkTinted,
    ) {
        // 背景图在 AppShell 之下，Scaffold 需要透明才能透出来
        AppBackground(
            painter = backgroundPainter,
            backgroundOpacity = prefs.backgroundOpacity,
            backgroundBlur = prefs.backgroundBlur,
            backgroundScrim = prefs.backgroundScrim,
        ) {
            var selectedTabId by rememberSaveable { mutableStateOf(demoTabs.first().id) }

            AppShell(
                tabs = demoTabs,
                selectedTabId = selectedTabId,
                onTabSelected = { selectedTabId = it },
                containerColor = if (hasBackground) Color.Transparent
                else MaterialTheme.colorScheme.background,
                topBar = { tab ->
                    TopAppBar(
                        title = { Text(titleFor(tab.id)) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (hasBackground) Color.Transparent
                            else MaterialTheme.colorScheme.surface,
                        ),
                    )
                },
            ) { tab ->
                when (tab.id) {
                    "rooms" -> RoomTab()
                    "focus" -> FocusTab()
                    "settings" -> SettingsTab(uiPrefsRepository)
                    "schedule" -> TimetableTab(prefs = prefs)
                    else -> TodayTab()
                }
            }
        }
    }
}

private fun titleFor(tabId: String): String = when (tabId) {
    "today" -> "今天"
    "rooms" -> "空教室"
    "focus" -> "专注"
    "settings" -> "设置"
    else -> FakeData.config.schoolName
}
