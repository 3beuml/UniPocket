package com.unipocket.demo

import android.content.Context
import android.content.SharedPreferences
import com.unipocket.domain.prefs.UiPrefsRepository
import com.unipocket.widget.WidgetOptions

/** 从偏好里读小组件显示选项；Provider 与预览页共用 */
fun loadWidgetOptions(context: Context): WidgetOptions {
    val prefs: SharedPreferences = context.getSharedPreferences("unipocket_demo", Context.MODE_PRIVATE)
    fun raw(key: String): String? = prefs.all[key]?.toString()
    return WidgetOptions(
        showRoom = raw(UiPrefsRepository.KEY_WIDGET_WEEK_SHOW_PLACE)?.toBooleanStrictOrNull() ?: true,
        showTime = raw(UiPrefsRepository.KEY_WIDGET_WEEK_SHOW_TIME)?.toBooleanStrictOrNull() ?: true,
    )
}
