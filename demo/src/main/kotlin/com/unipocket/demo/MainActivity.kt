package com.unipocket.demo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unipocket.domain.prefs.UiPrefsRepository
import com.unipocket.ui.nav.DockDestination
import com.unipocket.ui.nav.UniPocketNavHost
import com.unipocket.ui.theme.rememberEffectiveDark

private val demoDestinations = listOf(
    DockDestination("today", "首页", Icons.Filled.Today, Icons.Outlined.Today),
    DockDestination("schedule", "课表", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    DockDestination("rooms", "空教室", Icons.Filled.Apps, Icons.Outlined.Apps),
    DockDestination("focus", "专注", Icons.Filled.Timer, Icons.Outlined.Timer),
    DockDestination("settings", "设置", Icons.Filled.Settings, Icons.Outlined.Settings),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DemoApp() }
    }
}

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
    LaunchedEffect(uiPrefsRepository) { uiPrefsRepository.start() }
    val prefs by uiPrefsRepository.prefs.collectAsStateWithLifecycle()

    val backgroundPainter = rememberBackgroundPainter(prefs.backgroundPath)
    var selectedTabId by rememberSaveable { mutableStateOf(demoDestinations.first().id) }

    UniPocketNavHost(
        destinations = demoDestinations,
        selectedTabId = selectedTabId,
        onTabSelected = { selectedTabId = it },
        darkMode = prefs.darkMode,
        wallpaperPainter = backgroundPainter,
        wallpaperScrim = prefs.backgroundScrim,
        wallpaperNavBar = prefs.wallpaperNavBar,
        content = { destination ->
            when (destination.id) {
                "rooms" -> RoomTab()
                "focus" -> FocusTab()
                "settings" -> SettingsTab(uiPrefsRepository)
                "schedule" -> TimetableTab(prefs = prefs)
                else -> TodayTab()
            }
        },
    )
}
