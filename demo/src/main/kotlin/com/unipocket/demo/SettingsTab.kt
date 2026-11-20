package com.unipocket.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unipocket.domain.PrefsRepository
import com.unipocket.feature.settings.SettingsScreen

/** 设置页：把偏好仓储接到框架提供的设置界面上 */
@Composable
fun SettingsTab(repository: PrefsRepository, modifier: Modifier = Modifier) {
    val prefs by repository.prefs.collectAsStateWithLifecycle()

    SettingsScreen(
        prefs = prefs,
        onDarkModeChange = repository::setDarkMode,
        onThemeSeedChange = repository::setThemeSeed,
        onShowWeekendChange = repository::setShowWeekend,
        onShowTeacherChange = repository::setShowTeacher,
        onShowRoomChange = repository::setShowRoom,
        modifier = modifier,
    )
}
