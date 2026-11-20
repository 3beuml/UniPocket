package com.unipocket.demo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.unipocket.domain.DarkMode
import com.unipocket.domain.PrefsRepository
import com.unipocket.ui.AppShell
import com.unipocket.ui.AppTab
import com.unipocket.ui.UniPocketTheme

private val demoTabs = listOf(
    AppTab(id = "schedule", label = "课表"),
    AppTab(id = "rooms", label = "空教室"),
    AppTab(id = "focus", label = "专注"),
    AppTab(id = "settings", label = "设置"),
)

/**
 * demo 入口：偏好存储 → 主题 → 可配置底栏 → 三个页面。
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
    val prefsRepository = remember {
        PrefsRepository(
            store = SharedPreferencesPrefsStore(
                context.getSharedPreferences("unipocket_demo", Context.MODE_PRIVATE),
            ),
            scope = scope,
        )
    }
    LaunchedEffect(prefsRepository) {
        prefsRepository.start()
    }
    val prefs by prefsRepository.prefs.collectAsStateWithLifecycle()

    val darkTheme = when (prefs.darkMode) {
        DarkMode.SYSTEM -> isSystemInDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }

    UniPocketTheme(
        darkTheme = darkTheme,
        seedColor = Color(prefs.themeSeed),
    ) {
        var selectedTabId by rememberSaveable { mutableStateOf(demoTabs.first().id) }

        AppShell(
            tabs = demoTabs,
            selectedTabId = selectedTabId,
            onTabSelected = { selectedTabId = it },
            topBar = { tab ->
                TopAppBar(title = { Text(titleFor(tab.id)) })
            },
        ) { tab ->
            when (tab.id) {
                "rooms" -> RoomTab()
                "focus" -> FocusTab()
                "settings" -> SettingsTab(prefsRepository)
                else -> TimetableTab(prefs = prefs)
            }
        }
    }
}

private fun titleFor(tabId: String): String = when (tabId) {
    "rooms" -> "空教室"
    "focus" -> "专注"
    "settings" -> "设置"
    else -> FakeData.config.schoolName
}
