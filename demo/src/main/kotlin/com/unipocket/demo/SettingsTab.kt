package com.unipocket.demo

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unipocket.domain.PrefsRepository
import com.unipocket.feature.settings.SettingsScreen

/** 设置页：把偏好仓储接到框架提供的设置界面上 */
@Composable
fun SettingsTab(repository: PrefsRepository, modifier: Modifier = Modifier) {
    val prefs by repository.prefs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 用 OpenDocument（而不是照片选择器）：拿到长期可读的授权，重启后背景图仍在
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            repository.setBackgroundUri(uri.toString())
        }
    }

    SettingsScreen(
        prefs = prefs,
        onDarkModeChange = repository::setDarkMode,
        onThemeSeedChange = repository::setThemeSeed,
        onShowWeekendChange = repository::setShowWeekend,
        onShowTeacherChange = repository::setShowTeacher,
        onShowRoomChange = repository::setShowRoom,
        onPickBackground = { pickImage.launch(arrayOf("image/*")) },
        onClearBackground = { repository.setBackgroundUri(null) },
        onToggleBackgroundDim = repository::setBackgroundDim,
        onWidgetShowRoomChange = repository::setWidgetShowRoom,
        onWidgetShowTimeChange = repository::setWidgetShowTime,
        modifier = modifier,
    )
}
