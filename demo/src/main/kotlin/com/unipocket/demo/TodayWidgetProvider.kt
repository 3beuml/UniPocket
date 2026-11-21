package com.unipocket.demo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import com.unipocket.widget.TodayWidgetRenderer
import com.unipocket.widget.WidgetOptions
import com.unipocket.widget.WidgetStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** 「今天」桌面组件：把今天的课列表交给 [TodayWidgetRenderer] */
class TodayWidgetProvider : AppWidgetProvider() {

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
                val semester = FakeData.semester
                val sessions = FakeScheduleSource(delayMillis = 0).loadSessions()
                val week = com.unipocket.domain.WeekAlignment.indexOf(
                    semester.startDate, semester.weekStartOnSunday, semester.weekIndices,
                    LocalDate.now(),
                ) ?: 1

                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    .takeIf { it > 0 }?.toFloat() ?: 250f
                val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                    .takeIf { it > 0 }?.toFloat() ?: 150f

                val dark = (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                val openApp = PendingIntent.getActivity(
                    context,
                    1,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val views = TodayWidgetRenderer.render(
                    context = context,
                    sessions = sessions,
                    periods = FakeData.periods,
                    week = week,
                    date = LocalDate.now(),
                    now = LocalTime.now(),
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
