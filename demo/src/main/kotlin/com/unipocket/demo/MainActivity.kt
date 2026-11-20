package com.unipocket.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import com.unipocket.ui.AppShell
import com.unipocket.ui.AppTab
import com.unipocket.ui.UniPocketTheme

private val demoTabs = listOf(
    AppTab(id = "schedule", label = "课表"),
    AppTab(id = "rooms", label = "空教室"),
)

/**
 * demo 入口：可配置底栏 + 两个页面。
 *
 * 想加自己的页面（电费、一卡通……）只需要往 [demoTabs] 里加一项、
 * 在下面的 `when` 里加一个分支，不用改框架。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniPocketTheme {
                DemoApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoApp() {
    var selectedTabId by rememberSaveable { mutableStateOf(demoTabs.first().id) }

    AppShell(
        tabs = demoTabs,
        selectedTabId = selectedTabId,
        onTabSelected = { selectedTabId = it },
        topBar = { tab ->
            TopAppBar(
                title = {
                    Text(if (tab.id == "rooms") "空教室" else FakeData.config.schoolName)
                },
            )
        },
    ) { tab ->
        when (tab.id) {
            "rooms" -> RoomTab()
            else -> TimetableTab()
        }
    }
}
