package com.unipocket.demo

import com.unipocket.domain.LocationNode
import com.unipocket.domain.Period
import com.unipocket.domain.RoomSource
import com.unipocket.domain.ScheduleSource
import com.unipocket.domain.Semester
import com.unipocket.domain.Session
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * demo 的假数据。
 *
 * 外校 fork 后的第一件事就是把这里换掉：
 * 1. 用本校真实的节次时间与学期参数构造 [periods] / [semester]；
 * 2. 删掉 [sessions]，改由自己的 `ScheduleSource` 提供；
 * 3. 把 [FakeRoomSource] 换成真的空教室数据源（可选）。
 *
 * 假数据刻意覆盖了几种边界：周末有课、第 9 周才开课的课、第 17 周才开的课程设计、
 * 周次不连续的"形势与政策"，用来验证周次过滤与网格布局。
 * 每门课按新模型展开成「每周一条 [Session]」。
 */
object FakeData {

    val periods: List<Period> = listOf(
        Period(1, "第1节", 800, 845, "MORNING", 0),
        Period(2, "第2节", 855, 940, "MORNING", 0),
        Period(3, "第3节", 1000, 1045, "MORNING", 1),
        Period(4, "第4节", 1055, 1140, "MORNING", 1),
        Period(5, "第5节", 1400, 1445, "AFTERNOON", 0),
        Period(6, "第6节", 1455, 1540, "AFTERNOON", 0),
        Period(7, "第7节", 1600, 1645, "AFTERNOON", 1),
        Period(8, "第8节", 1655, 1740, "AFTERNOON", 1),
        Period(9, "第9节", 1900, 1945, "EVENING", 0),
        Period(10, "第10节", 1955, 2040, "EVENING", 0),
    )

    // 2026-09-01 是周二，框架会自动归一到所在周的周一（08-31）
    val semester: Semester = Semester(
        id = 1,
        nameZh = "2026-2027-1",
        schoolYear = "2026-2027",
        startDate = LocalDate.of(2026, 9, 1),
        endDate = null,
        weekStartOnSunday = false,
        weekIndices = (1..20).toList(),
        isCurrent = true,
    )

    /** 教学周总数（各处筛选用） */
    const val WEEK_COUNT = 20

    private data class CourseDef(
        val code: String,
        val name: String,
        val teacher: String,
        val room: String,
        val weeks: List<Int>,
        val weekday: Int,
        val startUnit: Int,
        val endUnit: Int,
        val experiment: Boolean = false,
    )

    private val courseDefs: List<CourseDef> = listOf(
        CourseDef("MATH101", "高等数学", "张伟", "教1-101", (1..16).toList(), 1, 1, 2),
        CourseDef("PHYS101", "大学物理", "李娜", "教2-203", (1..16).toList(), 2, 3, 4),
        CourseDef("CS102", "程序设计基础", "王强", "计算机楼-302", (1..12).toList(), 3, 1, 2),
        CourseDef("ENG101", "大学英语", "刘芳", "外语楼-105", (1..16).toList(), 4, 3, 4),
        CourseDef("CS201", "数据结构", "陈静", "计算机楼-401", (1..16).toList(), 5, 5, 6),
        CourseDef("MATH201", "线性代数", "赵磊", "教1-204", (1..8).toList(), 1, 3, 4),
        CourseDef("PE101", "体育（羽毛球）", "孙鹏", "体育馆", (1..16).toList(), 2, 5, 6),
        CourseDef("HIS101", "中国近现代史纲要", "周敏", "教3-101", (1..10).toList(), 4, 1, 2),
        CourseDef("EE210", "电路分析基础", "吴涛", "实验楼-208", (9..16).toList(), 3, 5, 6),
        CourseDef("GEN010", "形势与政策", "郑华", "教3-202", (5..8).toList() + (11..14).toList(), 5, 7, 8),
        CourseDef("PHYS110", "大学物理实验", "李娜", "实验楼-105", (2..15).toList(), 6, 1, 2, experiment = true),
        CourseDef("CS299", "数据结构课程设计", "陈静", "计算机楼-401", (17..20).toList(), 5, 5, 8),
    )

    /** 每门课展开成每周一条 Session（新模型口径） */
    val sessions: List<Session> = courseDefs.flatMap { def ->
        def.weeks.map { week ->
            Session(
                id = "fake-${def.code.lowercase()}-w$week",
                semesterId = semester.id,
                source = "eas",
                courseCode = def.code,
                courseName = def.name,
                weekIndex = week,
                weekday = def.weekday,
                startUnit = def.startUnit,
                endUnit = def.endUnit,
                date = null,
                startTime = null,
                endTime = null,
                teacherName = def.teacher,
                roomText = def.room,
                campusText = null,
                color = null,
                experiment = def.experiment,
            )
        }
    }
}

/**
 * 假数据源：模拟一次网络请求的延迟，方便观察"加载中"状态。
 *
 * 外校接入时，把 [FakeData.sessions] 换成自家教务返回的 `List<Session>` 即可——
 * 也可以直接参考这个类写自己的 `ScheduleSource`。
 */
class FakeScheduleSource(
    private val delayMillis: Long = 400,
) : ScheduleSource {

    override val id: String = "fake"

    override suspend fun loadSessions(): List<Session> {
        if (delayMillis > 0) delay(delayMillis)
        return FakeData.sessions
    }
}

/** 假地点树：主校区 → 3 栋楼 → 若干教室 */
object FakeCampus {

    private fun rooms(buildingId: String, vararg names: String): List<LocationNode> =
        names.map { LocationNode(id = "$buildingId-$it", name = it, level = 2) }

    val root: LocationNode = LocationNode(
        id = "campus-main",
        name = "主校区",
        level = 0,
        children = listOf(
            LocationNode(
                id = "building-1",
                name = "1 号楼",
                level = 1,
                children = rooms("b1", "101", "102", "103", "104", "201", "202"),
            ),
            LocationNode(
                id = "building-2",
                name = "2 号楼",
                level = 1,
                children = rooms("b2", "101", "102", "201", "202", "301"),
            ),
            LocationNode(
                id = "building-lab",
                name = "实验楼",
                level = 1,
                children = rooms("lab", "A101", "A102", "A201"),
            ),
        ),
    )
}

/**
 * 假空教室数据源：用"教室号 + 星期 + 节次"做个确定性取模，
 * 让每次查询有大约七成教室空闲，方便肉眼核对分组与筛选。
 */
class FakeRoomSource(
    private val delayMillis: Long = 250,
) : RoomSource {

    override val id: String = "fake-rooms"

    override suspend fun locations(): LocationNode = FakeCampus.root

    override suspend fun freeRooms(week: Int, dayOfWeek: Int, periods: IntRange): List<LocationNode> {
        if (delayMillis > 0) delay(delayMillis)
        return FakeCampus.root.flatten()
            .filter { it.isLeaf }
            .filter { room ->
                val number = room.name.filter { it.isDigit() }.toIntOrNull() ?: 0
                (number + dayOfWeek + periods.first) % 4 != 0
            }
            .toList()
    }
}
