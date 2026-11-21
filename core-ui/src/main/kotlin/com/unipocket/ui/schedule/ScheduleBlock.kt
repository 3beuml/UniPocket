package com.unipocket.ui.schedule

import com.unipocket.domain.Session
import java.security.MessageDigest

/**
 * 渲染用的单周时段。由 [Session] 过滤目标周而来——
 * 新模型一条 Session 本来就只对应一个教学周，不再需要多周展开。
 */
data class ScheduleSlot(
    val id: String,
    val name: String,
    val room: String?,
    val teacher: String?,
    /** 稳定的课程身份（配色用，同一门课所有周同色） */
    val courseCode: String,
    val week: Int,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    /** 教务给的真实日期（ISO，调休课靠它）；null = 常规排课 */
    val date: String?,
    /** 节次表查不到时的兜底时刻文本（"08:00"）；null = 没有兜底 */
    val startTime: String?,
    val endTime: String?,
    /** 稳定 key，用于冲突选择记忆 */
    val conflictKey: String,
)

/**
 * 挑出目标教学周的 [ScheduleSlot] 列表。
 *
 * `Session.weekIndex` 是单值（每周一条记录），所以"展开"退化成一次过滤。
 */
fun List<Session>.expandByWeek(week: Int): List<ScheduleSlot> =
    filter { it.weekIndex == week }
        .map { session ->
            ScheduleSlot(
                id = session.id,
                name = session.courseName,
                room = session.roomText,
                teacher = session.teacherName,
                courseCode = session.courseCode,
                week = week,
                dayOfWeek = session.weekday,
                startPeriod = session.startUnit,
                endPeriod = session.endUnit,
                date = session.date,
                startTime = session.startTime,
                endTime = session.endTime,
                conflictKey = conflictDisplayKey(session),
            )
        }

/** 冲突选择的稳定 key：同一门课不同时段/不同来源允许按周做不同选择 */
fun conflictDisplayKey(session: Session): String = stableKey(
    listOf(
        session.id,
        session.weekday.toString(),
        session.startUnit.toString(),
        session.endUnit.toString(),
        session.weekIndex.toString(),
    ),
)

/**
 * 冲突显示的用户选择：哪个冲突格子显示哪门课。
 * semesterId 对应 UniPocket 单学期模型用 0 代替（多学期由外层控制）。
 */
data class ConflictDisplayChoice(
    val conflictKey: String,
    val week: Int,
    val slotId: String,
)

/**
 * 一段连续节次。slots 保留该段全部课程（含用户选择不显示的）。
 */
data class ScheduleBlock(
    val startPeriod: Int,
    val endPeriod: Int,
    val slots: List<ScheduleSlot>,
    val selectedSlotId: String? = null,
) {
    val week: Int get() = slots.first().week
    val dayOfWeek: Int get() = slots.first().dayOfWeek
    val isConflict: Boolean get() = slots.size > 1
    val showPlaceholder: Boolean get() = isConflict && selectedSlotId == null
    val displaySlot: ScheduleSlot get() = slots.firstOrNull { it.id == selectedSlotId } ?: slots.first()

    /** 不含课名/教室等显示文本；改名/同步不影响选择记忆 */
    val blockConflictKey: String
        get() = stableKey(
            listOf("conflict-v1", dayOfWeek.toString(), startPeriod.toString(), endPeriod.toString()) +
                slots.map { it.conflictKey }.sorted(),
        )
}

/**
 * 按当天每一节的实际占用切段，只有占用集合不变的相邻节次才合并。
 * 例如甲 1–4、乙 3–6 → 甲 1–2 / 冲突 3–4 / 乙 5–6，不能整段跳过乙。
 *
 * 这是从 ZZUPocket `scheduleBlocks()` 移植的算法，数据源从 `Session` 换成 [ScheduleSlot]。
 */
fun scheduleBlocks(
    slots: List<ScheduleSlot>,
    choices: List<ConflictDisplayChoice> = emptyList(),
    maxPeriod: Int = 10,
): List<ScheduleBlock> {
    val byChoice = choices.associateBy { Pair(it.conflictKey, it.week) }
    return slots
        .filter { it.dayOfWeek in 1..7 && it.startPeriod > 0 && it.endPeriod >= it.startPeriod }
        .groupBy { it.dayOfWeek }
        .flatMap { (_, day) ->
            val ordered = day.distinctBy { it.id }.sortedWith(
                compareBy({ it.startPeriod }, { it.name }, { it.endPeriod }, { it.id }),
            )
            val blocks = mutableListOf<ScheduleBlock>()
            for (period in 1..maxPeriod) {
                val active = ordered.filter { period in it.startPeriod..it.endPeriod }
                if (active.isEmpty()) continue
                val previous = blocks.lastOrNull()
                if (previous != null && previous.endPeriod == period - 1 && previous.slots == active) {
                    blocks[blocks.lastIndex] = previous.copy(endPeriod = period)
                } else {
                    blocks += ScheduleBlock(period, period, active)
                }
            }
            blocks.map { block ->
                val choice = if (block.isConflict) {
                    byChoice[Pair(block.blockConflictKey, block.week)]
                } else null
                block.copy(selectedSlotId = choice?.slotId)
            }
        }
        .sortedWith(compareBy({ it.dayOfWeek }, { it.startPeriod }))
}

private fun stableKey(parts: List<String>): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(parts.joinToString("") { "${it.length}:$it}" }.toByteArray(Charsets.UTF_8))
    val hex = "0123456789abcdef"
    return buildString(digest.size * 2) {
        digest.forEach { byte ->
            val value = byte.toInt() and 0xff
            append(hex[value ushr 4])
            append(hex[value and 15])
        }
    }
}
