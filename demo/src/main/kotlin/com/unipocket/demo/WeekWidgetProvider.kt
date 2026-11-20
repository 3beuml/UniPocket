package com.unipocket.demo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import com.unipocket.widget.WeekWidgetRenderer
import com.unipocket.widget.WidgetOptions
import com.unipocket.widget.WidgetStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 一周课表桌面组件。
 *
 * 组件只负责「取数据 → 调渲染器 → 交给系统」；布局与行高账本全在 `widget` 模块里。
 * 外校接入时把数据来源换成自己的 `ScheduleSource` 即可。
 */
class WeekWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { update(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        update(context, appWidgetManager, appWidgetId)
    }

    private fun update(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val config = FakeData.config
                val courses = FakeScheduleSource(delayMillis = 0).loadCourses()
                val week = config.semester.currentWeek() ?: 1

                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    .takeIf { it > 0 }?.toFloat() ?: 300f
                val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                    .takeIf { it > 0 }?.toFloat() ?: 300f

                val dark = (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                val openApp = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val views = WeekWidgetRenderer.render(
                    context = context,
                    config = config,
                    courses = courses,
                    week = week,
                    reportedWidthDp = widthDp,
                    reportedHeightDp = heightDp,
                    style = WidgetStyle(dark = dark),
                    options = loadWidgetOptions(context),
                    clickIntent = openApp,
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
