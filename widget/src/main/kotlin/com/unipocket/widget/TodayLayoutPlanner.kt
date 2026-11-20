package com.unipocket.widget

import kotlin.math.max
import kotlin.math.min

/** 今天组件的排版结果：每行高度与列头高度 */
data class TodayPlan(
    val rowHeightsDp: List<Float>,
    val headerHeightDp: Float = TodayLayoutPlanner.HEADER_HEIGHT_DP,
) {
    val visibleRows: Int get() = rowHeightsDp.size
}

/**
 * 今天组件的行高账本。
 *
 * 与一周组件同一套口径：尺寸先减 30dp 虚高、按课名真实行数记自然高、
 * 装不下整行就不画（宁少一行不裁字）、剩余空间均摊且封顶。
 */
object TodayLayoutPlanner {

    const val HEADER_HEIGHT_DP = 30f
    const val ROW_BASE_DP = 38f
    const val ROW_EXTRA_LINE_DP = 10f
    const val MAX_STRETCH_DP = 8f
    const val VERTICAL_PADDING_DP = 6f

    fun plan(
        reportedHeightDp: Float,
        nameLineCounts: List<Int>,
        headerHeightDp: Float = HEADER_HEIGHT_DP,
    ): TodayPlan {
        if (nameLineCounts.isEmpty()) return TodayPlan(emptyList(), headerHeightDp)

        val available = WidgetMetrics.correctedHeight(reportedHeightDp) -
            headerHeightDp - VERTICAL_PADDING_DP
        val usable = max(available, 0f)

        val naturals = nameLineCounts.map { lines ->
            ROW_BASE_DP + (lines - 1).coerceAtLeast(0) * ROW_EXTRA_LINE_DP
        }

        val visible = ArrayList<Float>()
        var accumulated = 0f
        for (height in naturals) {
            if (visible.isNotEmpty() && accumulated + height > usable) break
            accumulated += height
            visible += height
        }
        if (visible.isEmpty()) visible += naturals.first()

        val extra = min((usable - accumulated).coerceAtLeast(0f) / visible.size, MAX_STRETCH_DP)
        return TodayPlan(visible.map { it + extra }, headerHeightDp)
    }
}
