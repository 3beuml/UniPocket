package com.unipocket.widget

import android.app.PendingIntent
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.unipocket.domain.Course
import com.unipocket.domain.SchoolConfig
import com.unipocket.domain.TodayStatus
import com.unipocket.domain.inWeek
import com.unipocket.domain.todayItems
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 「今天」桌面组件：今天剩下的课按时间排成列表。
 *
 * 与一周组件共用同一套口径：尺寸先减 30dp、按课名行数记账、装不下少画不裁字。
 * 进行中的课用主题色强调，已结束的课弱化。
 */
object TodayWidgetRenderer {

    private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val DAY_LABELS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    private const val TIME_COLUMN_DP = 42f
    private const val HORIZONTAL_CHROME_DP = 17f

    fun render(
        context: Context,
        config: SchoolConfig,
        courses: List<Course>,
        week: Int,
        date: LocalDate,
        now: LocalTime,
        reportedWidthDp: Float,
        reportedHeightDp: Float,
        style: WidgetStyle = WidgetStyle(),
        options: WidgetOptions = WidgetOptions(),
        clickIntent: PendingIntent? = null,
    ): RemoteViews {
        val pkg = context.packageName
        // 先按周过滤，再取当天——否则其它教学周的课会漏进来
        val items = courses.inWeek(week).todayItems(config, date.dayOfWeek.value, now)
        val nameWidth = (reportedWidthDp - TIME_COLUMN_DP - HORIZONTAL_CHROME_DP).coerceAtLeast(40f)
        val lineCounts = items.map {
            WidgetMetrics.estimateLines(it.course.name, nameWidth, 10f)
        }
        val plan = TodayLayoutPlanner.plan(reportedHeightDp, lineCounts)

        val bg = if (style.dark) 0xFF1C1B1F.toInt() else 0xFFFFFFFF.toInt()
        val text = if (style.dark) 0xFFE6E1E5.toInt() else 0xFF1B1B1F.toInt()
        val sub = if (style.dark) 0xFF938F99.toInt() else 0xFF6F6B75.toInt()

        val root = RemoteViews(pkg, R.layout.unipocket_widget_today)
        root.setInt(R.id.today_root, "setBackgroundColor", bg)
        clickIntent?.let { root.setOnClickPendingIntent(R.id.today_root, it) }
        root.removeAllViews(R.id.today_rows)

        val weekday = DAY_LABELS.getOrElse(date.dayOfWeek.value - 1) { "" }
        val dateText = "${date.monthValue}.${date.dayOfMonth}"
        val header = if (items.isEmpty()) {
            "今天 $dateText $weekday"
        } else {
            "今天 $dateText $weekday · ${items.size} 节"
        }
        root.setTextViewText(R.id.today_header, header)
        root.setTextColor(R.id.today_header, text)

        if (items.isEmpty()) {
            root.setViewVisibility(R.id.today_empty, View.VISIBLE)
            root.setTextViewText(R.id.today_empty, "今天没有课程")
            root.setTextColor(R.id.today_empty, sub)
            return root
        }

        root.setViewVisibility(R.id.today_empty, View.GONE)

        items.take(plan.visibleRows).forEachIndexed { index, item ->
            val row = RemoteViews(pkg, R.layout.unipocket_widget_today_row)
            val height = plan.rowHeightsDp[index]

            val nameColor = when (item.status) {
                TodayStatus.ONGOING -> style.accentColor
                TodayStatus.UPCOMING -> text
                TodayStatus.FINISHED -> sub
            }
            val blockColor = style.blockPalette[
                Math.floorMod(item.course.colorSeed, style.blockPalette.size)
            ]

            row.setViewLayoutWidth(R.id.today_row_root, reportedWidthDp, TypedValue.COMPLEX_UNIT_DIP)
            row.setViewLayoutHeight(R.id.today_row_root, height, TypedValue.COMPLEX_UNIT_DIP)

            row.setViewLayoutHeight(
                R.id.today_row_bar,
                (height - 4f).coerceAtLeast(1f),
                TypedValue.COMPLEX_UNIT_DIP,
            )
            row.setInt(R.id.today_row_bar, "setBackgroundColor", blockColor)

            row.setTextViewText(R.id.today_row_time, item.start.format(HH_MM))
            row.setTextColor(R.id.today_row_time, nameColor)

            row.setViewLayoutWidth(
                R.id.today_row_content,
                nameWidth,
                TypedValue.COMPLEX_UNIT_DIP,
            )
            row.setTextViewText(R.id.today_row_name, item.course.name)
            row.setTextColor(R.id.today_row_name, nameColor)

            val detail = listOfNotNull(
                item.course.room?.takeIf { it.isNotBlank() && options.showRoom },
                item.statusLabel,
            ).joinToString(" · ")
            if (detail.isBlank()) {
                row.setViewVisibility(R.id.today_row_detail, View.GONE)
            } else {
                row.setViewVisibility(R.id.today_row_detail, View.VISIBLE)
                row.setTextViewText(R.id.today_row_detail, detail)
                row.setTextColor(R.id.today_row_detail, sub)
            }

            root.addView(R.id.today_rows, row)
        }

        return root
    }
}
