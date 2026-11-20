package com.unipocket.widget

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * 小组件的尺寸与文字测量口径。
 *
 * 两个来自真机的教训（见仓库 `memory/`）：
 * 1. **所有尺寸上报通道都会虚高约 30dp**（AppWidgetManager 的 MIN/MAX 与 API31 的 SIZES 都一样），
 *    不能拿上报值直接当画布，必须先减 30。
 * 2. **行高账本必须换行感知**：课名换两行时自然高会多出一行，按"一行"记账会导致底部裁字。
 */
object WidgetMetrics {

    /** 实测虚高量（dp），任何上报通道都要减掉 */
    const val SIZE_INFLATION_DP = 30f

    /** 单行课名的自然高（dp） */
    const val ROW_BASE_DP = 44f

    /** 课名每多一行增加的自然高（dp） */
    const val ROW_EXTRA_LINE_DP = 12f

    /** 撑高时每行最多再加多少（dp） */
    const val ROW_MAX_STRETCH_DP = 12f

    /** 上报高度 → 真实可用高度 */
    fun correctedHeight(reportedHeightDp: Float): Float =
        max(reportedHeightDp - SIZE_INFLATION_DP, 80f)

    /**
     * 估算一段文字在给定列宽里占几行。
     *
     * 口径：CJK 字符按 1em、ASCII 按 0.55em 计宽；结果夹在 1..[maxLines]。
     * 这是**估算**，不是排版引擎——目的是让账本不至于把两行课名记成一行。
     */
    fun estimateLines(
        text: String,
        columnWidthDp: Float,
        fontSizeSp: Float,
        maxLines: Int = 2,
    ): Int {
        if (text.isEmpty() || columnWidthDp <= 0f) return 1

        var width = 0f
        for (ch in text) {
            width += if (ch.code <= 0x7F) fontSizeSp * 0.55f else fontSizeSp
        }
        val lines = ceil(width / columnWidthDp).toInt()
        return lines.coerceIn(1, maxLines)
    }

    /** 一行课位的自然高（按课名行数） */
    fun naturalRowHeight(nameLines: Int): Float =
        ROW_BASE_DP + (nameLines - 1).coerceAtLeast(0) * ROW_EXTRA_LINE_DP
}

/** 一周课表组件的排版结果 */
data class WeekPlan(
    /** 单列宽度（dp），已扣除时间列 */
    val columnWidthDp: Float,
    /** 每个可见节次行的高度（dp） */
    val rowHeightsDp: List<Float>,
    /** 时间列宽度（dp） */
    val timeColumnWidthDp: Float,
    /** 列头高度（dp） */
    val headerHeightDp: Float,
) {
    val visibleRows: Int get() = rowHeightsDp.size
    val totalRowsHeightDp: Float get() = rowHeightsDp.sum()
}

/**
 * 一周课表的行高账本。
 *
 * 规则（来自真机回归）：
 * - 按每行**真实自然高**累计；装不下整行就不画（宁少一行，不裁字），至少保留 1 行。
 * - 剩余空间**按行均摊**撑高，但每行不超过自然高 + 12dp。
 * - 永不把行高压到自然高以下——账本说放得下、视图实际溢出，正是 4×2 裁字事故的根因。
 */
object WeekLayoutPlanner {

    const val TIME_COLUMN_WIDTH_DP = 26f
    const val HEADER_HEIGHT_DP = 34f
    const val VERTICAL_PADDING_DP = 8f

    /**
     * @param reportedHeightDp 系统上报的组件高度（会先减 30dp）
     * @param nameLinesByRow 每个节次行里最长的课名行数（key = 节次，从 1 开始）
     * @param hasWeekendStrip 是否要给底部周末明细条留高度
     */
    fun plan(
        reportedHeightDp: Float,
        widthDp: Float,
        periodCount: Int,
        nameLinesByRow: Map<Int, Int>,
        hasWeekendStrip: Boolean = false,
        timeColumnWidthDp: Float = TIME_COLUMN_WIDTH_DP,
        headerHeightDp: Float = HEADER_HEIGHT_DP,
        verticalPaddingDp: Float = VERTICAL_PADDING_DP,
    ): WeekPlan {
        require(periodCount >= 1) { "periodCount 至少为 1" }

        val available = WidgetMetrics.correctedHeight(reportedHeightDp) -
            headerHeightDp - verticalPaddingDp - if (hasWeekendStrip) 30f else 0f
        val usable = max(available, 0f)

        val naturals = (1..periodCount).map { row ->
            WidgetMetrics.naturalRowHeight(nameLinesByRow[row] ?: 1)
        }

        val visible = ArrayList<Int>(periodCount)
        var accumulated = 0f
        for (index in naturals.indices) {
            val height = naturals[index]
            if (visible.isNotEmpty() && accumulated + height > usable) break
            accumulated += height
            visible += index
        }
        // 一个都放不下也必须留一行（极端小尺寸），保证界面有个可读的格子
        if (visible.isEmpty()) visible += 0

        val extraPerRow = if (visible.isEmpty()) {
            0f
        } else {
            min((usable - accumulated).coerceAtLeast(0f) / visible.size, WidgetMetrics.ROW_MAX_STRETCH_DP)
        }

        val heights = visible.map { naturals[it] + extraPerRow }
        val columnWidth = max((widthDp - timeColumnWidthDp) / 7f, 1f)

        return WeekPlan(
            columnWidthDp = columnWidth,
            rowHeightsDp = heights,
            timeColumnWidthDp = timeColumnWidthDp,
            headerHeightDp = headerHeightDp,
        )
    }
}
