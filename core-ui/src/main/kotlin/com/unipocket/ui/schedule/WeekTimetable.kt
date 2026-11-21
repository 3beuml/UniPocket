package com.unipocket.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unipocket.domain.Course
import com.unipocket.domain.SchoolConfig
import com.unipocket.domain.inWeek
import java.time.LocalDate

/** 单节课格子高度。10 节 × 56dp 在 360dp 宽屏上正好一屏半，允许滚动。 */
private val SlotHeight: Dp = 56.dp
private val TimeColumnWidth: Dp = 44.dp

private val DayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 一周课表网格（无状态）。
 *
 * - 7 列恒显（含周末）：周末没课也占位，用户能一眼看到"周末没课"。
 * - 列头两行：大字星期 + 今天标记，避免 7 列挤爆。
 * - 同一时段多门课（冲突）时横向平分格子，不互相遮挡。
 *
 * 只依赖 [SchoolConfig] 与 [Course]，不含任何学校语义，也不读写存储。
 */
@Composable
fun WeekTimetable(
    config: SchoolConfig,
    courses: List<Course>,
    week: Int,
    modifier: Modifier = Modifier,
    showWeekend: Boolean = true,
    showTeacher: Boolean = false,
    showRoom: Boolean = true,
    onCourseClick: (Course) -> Unit = {},
) {
    val weekCourses = remember(courses, week) { courses.inWeek(week) }
    val byDay = remember(weekCourses) { weekCourses.groupBy { it.dayOfWeek } }
    val lanesByDay = remember(byDay) { byDay.mapValues { (_, list) -> computeLanes(list) } }
    val days = if (showWeekend) 1..7 else 1..5

    val today = LocalDate.now()
    val todayWeek = config.semester.weekOf(today)
    val isCurrentWeek = todayWeek == week
    val todayDay = today.dayOfWeek.value

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(TimeColumnWidth))
            days.forEach { day ->
                val label = DayLabels[day - 1]
                val isToday = isCurrentWeek && day == todayDay
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = if (isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isToday) {
                        Text("今天", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(Modifier.fillMaxWidth()) {
                PeriodColumn(config)
                for (day in days) {
                    DayColumn(
                        day = day,
                        periodCount = config.periodCount,
                        courses = byDay[day].orEmpty(),
                        lanes = lanesByDay[day].orEmpty(),
                        showTeacher = showTeacher,
                        showRoom = showRoom,
                        onCourseClick = onCourseClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodColumn(config: SchoolConfig) {
    Column(Modifier.width(TimeColumnWidth)) {
        for (period in config.periods) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SlotHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("${period.index}", fontSize = 12.sp)
                Text(period.startText, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun RowScope.DayColumn(
    day: Int,
    periodCount: Int,
    courses: List<Course>,
    lanes: Map<String, LaneInfo>,
    showTeacher: Boolean,
    showRoom: Boolean,
    onCourseClick: (Course) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .height(SlotHeight * periodCount),
    ) {
        // 每节课的纵向位置按节次算；横向宽度按冲突车道平分
        for (course in courses) {
            val lane = lanes[course.id] ?: LaneInfo(lane = 0, lanes = 1)
            val laneWidth = maxWidth / lane.lanes
            val conflicted = lane.lanes > 1
            Box(
                modifier = Modifier
                    .offset(x = laneWidth * lane.lane, y = SlotHeight * (course.startPeriod - 1))
                    .width(laneWidth)
                    .height(SlotHeight * course.periods.count())
                    .padding(1.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(blockColor(course.colorSeed))
                    // 冲突警示：同一时段被多门课占用时描一圈警示色
                    .border(
                        width = if (conflicted) 2.dp else 0.dp,
                        color = if (conflicted) MaterialTheme.colorScheme.error else Color.Transparent,
                        shape = RoundedCornerShape(4.dp),
                    )
                    .clickable { onCourseClick(course) }
                    .padding(horizontal = 3.dp, vertical = 2.dp),
            ) {
                Column {
                    Text(
                        text = course.name,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = BlockTextColor,
                    )
                    if (showTeacher) {
                        course.teacher?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                fontSize = 7.5f.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = BlockTextColor.copy(alpha = 0.7f),
                            )
                        }
                    }
                    if (showRoom) {
                        course.room?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                fontSize = 7.5f.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = BlockTextColor.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 车道分配结果：[lane] 从 0 起，[lanes] 是该课需要平分的总车道数 */
private data class LaneInfo(val lane: Int, val lanes: Int)

/**
 * 冲突分道：按开始时间排序，能放进已有车道就不新开一条。
 * 冲突的课越多，各自越窄——但不重叠。
 */
private fun computeLanes(courses: List<Course>): Map<String, LaneInfo> {
    if (courses.isEmpty()) return emptyMap()

    val sorted = courses.sortedWith(compareBy({ it.startPeriod }, { it.endPeriod }))
    val lanes = mutableListOf<MutableList<Course>>()
    val laneOf = mutableMapOf<String, Int>()

    for (course in sorted) {
        val free = lanes.indexOfFirst { lane -> lane.none { it.overlaps(course) } }
        val index = if (free >= 0) {
            free
        } else {
            lanes.add(mutableListOf())
            lanes.lastIndex
        }
        lanes[index].add(course)
        laneOf[course.id] = index
    }

    return sorted.associate { course ->
        val overlapping = lanes.count { lane ->
            lane.any { other -> other.id != course.id && other.overlaps(course) }
        }
        course.id to LaneInfo(lane = laneOf.getValue(course.id), lanes = overlapping + 1)
    }
}

private fun Course.overlaps(other: Course): Boolean =
    dayOfWeek == other.dayOfWeek && startPeriod <= other.endPeriod && other.startPeriod <= endPeriod

private val BlockTextColor = Color(0xFF1B1B1F)

/** 8 色调色板；按 colorSeed 取色，保证同一门课颜色稳定 */
private val BlockPalette = listOf(
    Color(0xFFE3F2FD), Color(0xFFE8F5E9), Color(0xFFFFF3E0), Color(0xFFF3E5F5),
    Color(0xFFE0F7FA), Color(0xFFFFEBEE), Color(0xFFF1F8E9), Color(0xFFEDE7F6),
)

private fun blockColor(seed: Int): Color = BlockPalette[Math.floorMod(seed, BlockPalette.size)]
