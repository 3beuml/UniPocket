package com.unipocket.domain

/**
 * 空教室数据来源。可选实现：不实现时，空教室页显示"该校未接入"。
 *
 * 契约只规定"给地点树 + 查某时段空闲教室"，
 * 具体怎么查（教务接口 / 自己排课表反推 / 手工维护）由各校决定。
 */
interface RoomSource {
    val id: String

    /** 完整地点树，层数不限 */
    suspend fun locations(): LocationNode

    /**
     * 查询某周、某天、某几节的空闲教室。
     *
     * @param week 教学周，1 起
     * @param dayOfWeek 1=周一 … 7=周日
     * @param periods 节次区间，如 1..2
     * @return 叶子节点（教室）；具体挂在哪个楼、哪层由 `locations()` 的树决定
     */
    suspend fun freeRooms(week: Int, dayOfWeek: Int, periods: IntRange): List<LocationNode>
}
