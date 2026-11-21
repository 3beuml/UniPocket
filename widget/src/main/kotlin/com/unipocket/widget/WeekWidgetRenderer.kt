package com.unipocket.widget

import android.app.PendingIntent
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.unipocket.domain.Period
import com.unipocket.domain.Session
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

/** 小组件配色。深浅两套由调用方按系统深色模式选 */
data class WidgetStyle(
    val dark: Boolean = false,
    val accentColor: Int = 0xFF4F6BED.toInt(),
    val blockPalette: List<Int> = DEFAULT_WIDGET_PALETTE,
)

/** 浅色系课程块配色；块内文字固定深色，保证在粉彩底上可读 */
val DEFAULT_WIDGET_PALETTE: List<Int> = listOf(
    0xFFE3F2FD, 0xFFE8F5E9, 0xFFFFF3E0, 0xFFF3E5F5,
    0xFFE0F7FA, 0xFFFFEBEE, 0xFFF1F8E9, 0xFFEDE7F6,
).map { it.toInt() }

private const val BLOCK_TEXT = 0xFF1B1B1F.toInt()
private const val BLOCK_SUB_TEXT = 0x991B1B1F.toInt()

/**
 * 一周课表组件的 RemoteViews 渲染器（**纯布局函数**，不含数据获取）。
 *
 * 设计要点（全部来自真机回归，见仓库 `memory/`）：
 * - 7 列恒显，列头两行（星期 + 日期），今天用主题色；
 * - 行高走 [WeekLayoutPlanner] 的换行感知账本，宁可少画一行也不裁字；
 * - 周末有课时底部补一条明细（窄列放不下的完整信息）；
 * - 尺寸上报先过 −30dp 校准。
 *
 * 同一时段多门课（冲突）时只画第一门——组件空间不足以并排，冲突提示留给 App 内页面。
 */
object WeekWidgetRenderer {

    private val DAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

    /**
     * @param sessions 全学期上课记录（渲染器自己按 [week] 过滤）
     * @param weekMonday 该教学周的周一；null = 列头不显示日期
     */
    fun render(
        context: Context,
        sessions: List<Session>,
        periods: List<Period>,
        week: Int,
        weekMonday: LocalDate?,
        reportedWidthDp: Float,
        reportedHeightDp: Float,
        style: WidgetStyle = WidgetStyle(),
        options: WidgetOptions = WidgetOptions(),
        clickIntent: PendingIntent? = null,
    ): RemoteViews {
        val pkg = context.packageName
        val density = context.resources.displayMetrics.density
        val weekSessions = sessions.filter { it.weekIndex == week }
        val hasWeekend = weekSessions.any { it.weekday >= 6 }

        // 账本要换行感知：先按单列宽估课名行数，再算行高
        val provisionalColumnWidth =
            ((reportedWidthDp - WeekLayoutPlanner.TIME_COLUMN_WIDTH_DP) / 7f).coerceAtLeast(1f)
        val nameLinesByRow = mutableMapOf<Int, Int>()
        for (session in weekSessions) {
            val lines = WidgetMetrics.estimateLines(session.courseName, provisionalColumnWidth, 9f)
            nameLinesByRow[session.startUnit] =
                max(nameLinesByRow[session.startUnit] ?: 1, lines)
        }

        val plan = WeekLayoutPlanner.plan(
            reportedHeightDp = reportedHeightDp,
            widthDp = reportedWidthDp,
            periodCount = periods.size,
            nameLinesByRow = nameLinesByRow,
            hasWeekendStrip = hasWeekend,
        )

        val bg = if (style.dark) 0xFF1C1B1F.toInt() else 0xFFFFFFFF.toInt()
        val text = if (style.dark) 0xFFE6E1E5.toInt() else 0xFF1B1B1F.toInt()
        val sub = if (style.dark) 0xFF938F99.toInt() else 0xFF6F6B75.toInt()

        val root = RemoteViews(pkg, R.layout.unipocket_widget_week)
        root.setInt(R.id.week_root, "setBackgroundColor", bg)
        clickIntent?.let { root.setOnClickPendingIntent(R.id.week_root, it) }
        root.removeAllViews(R.id.week_header)
        root.removeAllViews(R.id.week_body)

        // ── 列头 ──────────────────────────────────────────────────────
        root.addView(R.id.week_header, spacer(pkg, plan.timeColumnWidthDp, plan.headerHeightDp))

        val today = LocalDate.now()
        // 只要列头日期等于今天就高亮——weekMonday 不是本周的周一时自然不会相等
        for (day in 1..7) {
            val cell = RemoteViews(pkg, R.layout.unipocket_widget_header_cell)
            val date = weekMonday?.plusDays((day - 1).toLong())
            val isToday = date == today

            cell.setTextViewText(R.id.header_main, if (isToday) "今" else DAY_LABELS[day - 1])
            cell.setTextColor(R.id.header_main, if (isToday) style.accentColor else text)
            cell.setTextViewText(
                R.id.header_sub,
                date?.let { "${it.monthValue}.${it.dayOfMonth}" }.orEmpty(),
            )
            cell.setTextColor(R.id.header_sub, if (isToday) style.accentColor else sub)
            cell.setViewLayoutWidth(R.id.header_root, plan.columnWidthDp, TypedValue.COMPLEX_UNIT_DIP)
            cell.setViewLayoutHeight(R.id.header_root, plan.headerHeightDp, TypedValue.COMPLEX_UNIT_DIP)
            root.addView(R.id.week_header, cell)
        }

        // ── 时间列 ────────────────────────────────────────────────────
        val timeColumn = RemoteViews(pkg, R.layout.unipocket_widget_day_column)
        for (row in 1..plan.visibleRows) {
            val cell = RemoteViews(pkg, R.layout.unipocket_widget_time_cell)
            val startText = periods.firstOrNull { it.indexNo == row }?.startText
            val label = buildString {
                append(row)
                if (startText != null && options.showTime) {
                    append('\n')
                    append(startText)
                }
            }
            cell.setTextViewText(R.id.time_text, label)
            cell.setTextColor(R.id.time_text, sub)
            cell.setViewLayoutHeight(R.id.time_text, plan.rowHeightsDp[row - 1], TypedValue.COMPLEX_UNIT_DIP)
            cell.setViewLayoutWidth(R.id.time_text, plan.timeColumnWidthDp, TypedValue.COMPLEX_UNIT_DIP)
            timeColumn.addView(R.id.day_root, cell)
        }
        timeColumn.setViewLayoutWidth(R.id.day_root, plan.timeColumnWidthDp, TypedValue.COMPLEX_UNIT_DIP)
        root.addView(R.id.week_body, timeColumn)

        // ── 7 个日列 ──────────────────────────────────────────────────
        for (day in 1..7) {
            val column = RemoteViews(pkg, R.layout.unipocket_widget_day_column)
            val byStart = weekSessions
                .filter { it.weekday == day }
                .sortedBy { it.startUnit }
                .groupBy { it.startUnit }
                .mapValues { (_, list) -> list.first() }

            var row = 1
            while (row <= plan.visibleRows) {
                val session = byStart[row]
                if (session == null) {
                    val empty = spacer(pkg, plan.columnWidthDp, plan.rowHeightsDp[row - 1])
                    column.addView(R.id.day_root, empty)
                    row++
                    continue
                }

                // 跨节次：课程块高度 = 覆盖到的可见行高度之和
                val spanEnd = min(session.endUnit, plan.visibleRows)
                val blockHeight = plan.rowHeightsDp.subList(row - 1, spanEnd).sum()
                column.addView(
                    R.id.day_root,
                    courseCell(pkg, density, session, plan.columnWidthDp, blockHeight, style, options),
                )
                row = spanEnd + 1
            }

            column.setViewLayoutWidth(R.id.day_root, plan.columnWidthDp, TypedValue.COMPLEX_UNIT_DIP)
            root.addView(R.id.week_body, column)
        }

        // ── 周末明细条 ────────────────────────────────────────────────
        val weekendSessions = weekSessions.filter { it.weekday >= 6 }
        if (weekendSessions.isNotEmpty()) {
            root.setViewVisibility(R.id.week_strip, View.VISIBLE)
            root.setTextViewText(
                R.id.week_strip,
                weekendStripText(weekMonday, periods, weekendSessions, options),
            )
            root.setTextColor(R.id.week_strip, sub)
        } else {
            root.setViewVisibility(R.id.week_strip, View.GONE)
        }

        return root
    }

    private fun spacer(pkg: String, widthDp: Float, heightDp: Float): RemoteViews {
        val view = RemoteViews(pkg, R.layout.unipocket_widget_empty_cell)
        view.setViewLayoutWidth(R.id.empty_root, widthDp, TypedValue.COMPLEX_UNIT_DIP)
        view.setViewLayoutHeight(R.id.empty_root, heightDp, TypedValue.COMPLEX_UNIT_DIP)
        return view
    }

    private fun courseCell(
        pkg: String,
        density: Float,
        session: Session,
        widthDp: Float,
        heightDp: Float,
        style: WidgetStyle,
        options: WidgetOptions,
    ): RemoteViews {
        val cell = RemoteViews(pkg, R.layout.unipocket_widget_course_cell)
        // 按课程代码取色：同一门课所有周同色
        val blockColor = style.blockPalette[
            Math.floorMod(session.courseCode.hashCode(), style.blockPalette.size),
        ]
        cell.setInt(R.id.course_root, "setBackgroundColor", blockColor)
        cell.setViewPadding(
            R.id.course_root,
            (2 * density).toInt(),
            (2 * density).toInt(),
            (2 * density).toInt(),
            (2 * density).toInt(),
        )

        cell.setTextViewText(R.id.course_name, session.courseName)
        cell.setTextColor(R.id.course_name, BLOCK_TEXT)

        val room = session.roomText
        if (room.isNullOrBlank() || !options.showRoom) {
            cell.setViewVisibility(R.id.course_room, View.GONE)
        } else {
            cell.setTextViewText(R.id.course_room, room)
            cell.setTextColor(R.id.course_room, BLOCK_SUB_TEXT)
        }

        cell.setViewLayoutWidth(R.id.course_root, widthDp, TypedValue.COMPLEX_UNIT_DIP)
        cell.setViewLayoutHeight(R.id.course_root, heightDp, TypedValue.COMPLEX_UNIT_DIP)
        return cell
    }

    /** 「六 11.7 数据结构 教1-101 10:10-11:50」，最多两条 */
    private fun weekendStripText(
        weekMonday: LocalDate?,
        periods: List<Period>,
        weekendSessions: List<Session>,
        options: WidgetOptions,
    ): String = weekendSessions
        .sortedWith(compareBy({ it.weekday }, { it.startUnit }))
        .take(2)
        .joinToString("   ") { session ->
            val date = weekMonday?.plusDays((session.weekday - 1).toLong())
            val start = periods.firstOrNull { it.indexNo == session.startUnit }?.startText
            val end = periods.firstOrNull { it.indexNo == session.endUnit }?.endText
            listOfNotNull(
                DAY_LABELS.getOrNull(session.weekday - 1),
                date?.let { "${it.monthValue}.${it.dayOfMonth}" },
                session.courseName,
                session.roomText?.takeIf { options.showRoom },
                if (options.showTime && start != null && end != null) "$start-$end" else null,
            ).filter { it.isNotBlank() }.joinToString(" ")
        }
}
