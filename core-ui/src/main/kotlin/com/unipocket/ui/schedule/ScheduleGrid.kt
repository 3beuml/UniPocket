package com.unipocket.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unipocket.domain.Period
import com.unipocket.domain.prefs.BlockBorder
import com.unipocket.domain.prefs.CourseTextMode
import com.unipocket.domain.prefs.CourseColorMode
import com.unipocket.domain.prefs.UiPrefs
import com.unipocket.ui.nav.DOCK_CLEARANCE
import com.unipocket.ui.theme.rememberEffectiveDark
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val WEEKDAY_HEADER_HEIGHT = 36.dp
private val UNIT_COL_WIDTH = 34.dp
private val HH_MM: java.time.format.DateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

/**
 * 一周课表网格（从 ZZUPocket `ScheduleScreen.ScheduleGrid` 移植）。
 *
 * 渲染特征（全部来自 ZZUPocket 长期打磨）：
 * - 表头固定不滚（周几 + 日期始终可见）
 * - 虚线网格线（横竖各一组，用 drawBehind 画）
 * - 课程块按实际占用切段（重叠自动分三段，见 [scheduleBlocks]）
 * - 正在上的课描主题色边（借鉴 Sked 的 liveCourseOutline）
 * - 课程块描边按偏好：实线 / 虚线 / 不画
 * - 课名字号按密度档自动调，长课名自动缩小
 * - 地点超长时「保头保尾」缩写（楼名…房号）
 * - 冲突格子显示「冲突」占位 + 各课名列表
 */
@Composable
fun ScheduleGrid(
    slots: List<ScheduleSlot>,
    periods: List<Period>,
    prefs: UiPrefs,
    weekMonday: LocalDate?,
    conflictChoices: List<ConflictDisplayChoice>,
    week: Int,
    onBlockClick: (ScheduleBlock) -> Unit,
) {
    val days = 1..prefs.dayCount
    val cellH = prefs.cellHeightDp.dp
    val gap = if (prefs.blockBorder == BlockBorder.NONE) 0.75.dp else 1.dp
    val dark = rememberEffectiveDark(prefs.darkMode)
    val today = LocalDate.now()
    val now = LocalTime.now()
    val labelShadow = wallpaperShadow(prefs.backgroundPath != null, MaterialTheme.colorScheme.surface)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxSize()) {
        // ── 表头：固定不滚 ─────────────────────────────────────────────
        Row(Modifier.fillMaxWidth().height(WEEKDAY_HEADER_HEIGHT)) {
            Spacer(Modifier.width(UNIT_COL_WIDTH))
            for (d in days) {
                val date = weekMonday?.plusDays((d - 1).toLong())
                val isToday = date != null && date == today
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = weekdayName(d),
                        style = MaterialTheme.typography.labelMedium.copy(shadow = labelShadow),
                        color = labelColor,
                    )
                    if (date != null) {
                        Text(
                            text = dayLabel(date),
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary else labelColor,
                            style = LocalTextStyle.current.copy(shadow = labelShadow),
                        )
                    }
                }
            }
        }

        // ── 可滚动网格区 ───────────────────────────────────────────────
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            val gridLine = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            Row(
                modifier = Modifier.fillMaxWidth().drawBehind {
                    val unitW = UNIT_COL_WIDTH.toPx()
                    val rowH = cellH.toPx()
                    val gapPx = gap.toPx()
                    val stroke = 0.7.dp.toPx()
                    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                    for (i in 0 until 10) {
                        val y = (rowH + gapPx) * i
                        drawLine(gridLine, Offset(0f, y), Offset(size.width, y), stroke, pathEffect = dash)
                    }
                    drawLine(gridLine, Offset(0f, size.height), Offset(size.width, size.height), stroke, pathEffect = dash)
                    val dayCount = days.count()
                    val dayW = (size.width - unitW) / dayCount
                    for (i in 0..dayCount) {
                        val x = unitW + dayW * i
                        drawLine(gridLine, Offset(x, 0f), Offset(x, size.height), stroke, pathEffect = dash)
                    }
                },
            ) {
                PeriodColumn(periods, cellH, gap, labelShadow, labelColor, showTime = prefs.widgetShowTime)

                for (day in days) {
                    DayColumn(
                        modifier = Modifier.weight(1f),
                        day = day,
                        slots = slots,
                        periods = periods,
                        prefs = prefs,
                        cellH = cellH,
                        gap = gap,
                        dark = dark,
                        today = today,
                        now = now,
                        weekMonday = weekMonday,
                        conflictChoices = conflictChoices,
                        onBlockClick = onBlockClick,
                    )
                }
            }
            Spacer(Modifier.height(DOCK_CLEARANCE))
        }
    }
}

@Composable
private fun PeriodColumn(
    periods: List<Period>,
    cellH: Dp,
    gap: Dp,
    labelShadow: Shadow?,
    labelColor: Color,
    showTime: Boolean,
) {
    Column(
        modifier = Modifier.width(UNIT_COL_WIDTH),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        for (unit in 1..periods.size) {
            Column(
                modifier = Modifier.fillMaxWidth().height(cellH),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "$unit",
                    style = MaterialTheme.typography.labelMedium.copy(shadow = labelShadow),
                )
                periods.firstOrNull { it.index == unit }?.let {
                    if (showTime) {
                        Text(
                            text = "${it.startText}-${it.endText}",
                            fontSize = 7.sp,
                            lineHeight = 9.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = labelColor,
                            style = LocalTextStyle.current.copy(shadow = labelShadow),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    modifier: Modifier,
    day: Int,
    slots: List<ScheduleSlot>,
    periods: List<Period>,
    prefs: UiPrefs,
    cellH: Dp,
    gap: Dp,
    dark: Boolean,
    today: LocalDate,
    now: LocalTime,
    weekMonday: LocalDate?,
    conflictChoices: List<ConflictDisplayChoice>,
    onBlockClick: (ScheduleBlock) -> Unit,
) {
    val corner = RoundedCornerShape(6.dp)
    val daySlots = remember(slots, day, conflictChoices) {
        scheduleBlocks(slots.filter { it.dayOfWeek == day }, conflictChoices, maxPeriod = periods.size)
            .associateBy { it.startPeriod }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        var period = 1
        while (period <= periods.size) {
            val block = daySlots[period]
            if (block == null) {
                Box(Modifier.fillMaxWidth().height(cellH))
                period += 1
                continue
            }
            val slot = block.displaySlot
            val span = block.endPeriod - block.startPeriod + 1
            CourseBlock(
                slot = slot,
                span = span,
                height = cellH * span + gap * (span - 1),
                prefs = prefs,
                corner = corner,
                periods = periods,
                dark = dark,
                live = prefs.liveOutlineEnabled &&
                    (if (block.showPlaceholder) block.slots else listOf(slot)).any {
                        isLiveNow(it, periods, today, now, weekMonday)
                    },
                conflict = block.takeIf { it.isConflict },
                onClick = { onBlockClick(block) },
            )
            period += span
        }
    }
}

@Composable
private fun CourseBlock(
    slot: ScheduleSlot,
    span: Int,
    height: Dp,
    prefs: UiPrefs,
    corner: androidx.compose.ui.graphics.Shape,
    periods: List<Period>,
    dark: Boolean,
    live: Boolean,
    conflict: ScheduleBlock? = null,
    onClick: (ScheduleSlot) -> Unit,
) {
    val placeholder = conflict?.showPlaceholder == true
    val bg = when {
        placeholder -> MaterialTheme.colorScheme.errorContainer
        prefs.colorMode == CourseColorMode.SINGLE -> MaterialTheme.colorScheme.secondaryContainer
        else -> courseColor(slot.id, dark)
    }
    val decoration = when {
        placeholder -> Modifier.border(
            (if (live) maxOf(2, prefs.liveOutlineWidth) else 2).dp,
            MaterialTheme.colorScheme.error,
            corner,
        )
        live -> Modifier.border(prefs.liveOutlineWidth.dp, MaterialTheme.colorScheme.primary, corner)
        prefs.blockBorder == BlockBorder.SOLID ->
            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, corner)
        prefs.blockBorder == BlockBorder.DASHED ->
            Modifier.dashedBorder(MaterialTheme.colorScheme.outline)
        else -> Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(bg, corner)
            .then(decoration)
            .clickable { onClick(slot) },
    ) {
        if (!placeholder && conflict != null) {
            Text(
                text = "冲",
                fontSize = 7.sp,
                lineHeight = 8.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 1.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                    .padding(horizontal = 2.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 2.dp).align(Alignment.Center),
        ) {
            val nameFont = (prefs.courseFontSp + fontAdjust(slot.name))
                .coerceAtLeast(prefs.courseFontSp - 2)
            if (placeholder) {
                ConflictPlaceholderContent(conflict!!, prefs)
            } else Text(
                text = slot.name,
                fontSize = nameFont.sp,
                lineHeight = (nameFont + 2).sp,
                maxLines = if (span >= 2) 4 else 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
            )
            if (!placeholder && prefs.courseTextMode != CourseTextMode.NAME) {
                val roomText = abbreviatePlace(slot.room, maxWeighted = if (span >= 2) 26 else 12) ?: slot.room
                val roomFont = (prefs.roomFontSp + fontAdjust(roomText.orEmpty()))
                    .coerceAtLeast(prefs.roomFontSp - 1)
                roomText?.let {
                    Text(
                        text = it,
                        fontSize = roomFont.sp,
                        lineHeight = (roomFont + 1.5f).sp,
                        maxLines = if (span >= 2) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictPlaceholderContent(conflict: ScheduleBlock, prefs: UiPrefs) {
    Text(
        text = "时间冲突",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
    Text(
        text = conflict.slots.joinToString("\n") { it.name },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 2.dp),
    )
}

// ── 渲染工具函数（从 ZZUPocket core/ScheduleText.kt 移植）────────────────

internal fun weekdayName(weekday: Int): String =
    listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").getOrElse(weekday - 1) { "周$weekday" }

internal fun dayLabel(date: LocalDate): String {
    val m = date.monthValue.toString().padStart(2, '0')
    val d = date.dayOfMonth.toString().padStart(2, '0')
    return "$m-$d"
}

/** 长课名自动缩小：按加权重（CJK=1, ASCII=0.5）判断 */
internal fun fontAdjust(text: String): Int {
    var w = 0f
    for (ch in text) w += if (ch.code > 0x2E7F) 1f else 0.5f
    return when {
        w >= 18f -> -2
        w >= 13f -> -1
        else -> 0
    }
}

/** 地点超长时「保头保尾」缩写（楼名…房号） */
internal fun abbreviatePlace(text: String?, maxWeighted: Int = 14, tailKeep: Int = 5): String? {
    val t = text?.trim().orEmpty()
    if (t.isEmpty()) return null
    var weight = 0f
    for (ch in t) weight += if (ch.code > 0x2E7F) 1f else 0.5f
    if (weight <= maxWeighted) return t
    val tail = Regex("[0-9A-Za-z-]{1,${tailKeep}}$").find(t)?.value.orEmpty()
    val tailW = tail.sumOf { if (it.code > 0x2E7F) 1.0 else 0.5 }.toFloat()
    val headBudget = ((maxWeighted - tailW - 1f).coerceAtLeast(2f))
    val head = StringBuilder()
    var used = 0f
    for (ch in t) {
        val w = if (ch.code > 0x2E7F) 1f else 0.5f
        if (used + w > headBudget) break
        head.append(ch)
        used += w
    }
    return head.toString().trim() + "…" + tail
}

/** 按课程 id 取色：色相由哈希决定，明度按深浅色模式调整 */
internal fun courseColor(code: String, dark: Boolean): Color {
    val hue = (((code.hashCode() % 360) + 360) % 360).toFloat()
    return if (dark) {
        Color.hsl(hue, 0.35f, 0.26f)
    } else {
        Color.hsl(hue, 0.55f, 0.88f)
    }
}

/** 圆角虚线描边（Compose 没有内置虚线，用 drawBehind + dashPathEffect 画） */
internal fun Modifier.dashedBorder(
    color: Color,
    dash: Dp = 4.dp,
    gap: Dp = 3.dp,
    strokeWidth: Dp = 1.dp,
    radius: Dp = 6.dp,
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())),
        ),
    )
}

/** 正在上课判定：该课的日期是今天且当前时刻在起止范围内 */
internal fun isLiveNow(
    slot: ScheduleSlot,
    periods: List<Period>,
    today: LocalDate,
    now: LocalTime,
    weekMonday: LocalDate?,
): Boolean {
    val slotDate = weekMonday?.plusDays((slot.dayOfWeek - 1).toLong()) ?: return false
    if (slotDate != today) return false
    val start = periods.firstOrNull { it.index == slot.startPeriod }?.start ?: return false
    val end = periods.firstOrNull { it.index == slot.endPeriod }?.end ?: return false
    return !now.isBefore(start) && !now.isAfter(end)
}

internal fun wallpaperShadow(enabled: Boolean, surface: Color): Shadow? =
    if (enabled) Shadow(color = surface.copy(alpha = 0.6f), blurRadius = 4f) else null
