package com.unipocket.widget

/** 小组件的显示选项，由偏好（[com.unipocket.domain.SchedulePrefs]）驱动 */
data class WidgetOptions(
    /** 是否显示教室 */
    val showRoom: Boolean = true,
    /** 是否显示时间段（节次列时刻、明细条时间） */
    val showTime: Boolean = true,
)
