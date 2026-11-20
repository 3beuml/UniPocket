package com.unipocket.demo

import android.content.Context
import android.content.SharedPreferences
import com.unipocket.domain.SchedulePrefs
import com.unipocket.widget.WidgetOptions

/** 从偏好里读小组件显示选项；Provider 与预览页共用 */
fun loadWidgetOptions(context: Context): WidgetOptions {
    val prefs: SharedPreferences = context.getSharedPreferences("unipocket_demo", Context.MODE_PRIVATE)
    return WidgetOptions(
        showRoom = prefs.all[SchedulePrefs.KEY_WIDGET_SHOW_ROOM]?.toString()
            ?.toBooleanStrictOrNull() ?: true,
        showTime = prefs.all[SchedulePrefs.KEY_WIDGET_SHOW_TIME]?.toString()
            ?.toBooleanStrictOrNull() ?: true,
    )
}
