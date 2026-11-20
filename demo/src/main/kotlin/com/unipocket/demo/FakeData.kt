package com.unipocket.demo

import com.unipocket.domain.Course
import com.unipocket.domain.LocationNode
import com.unipocket.domain.Period
import com.unipocket.domain.RoomSource
import com.unipocket.domain.ScheduleSource
import com.unipocket.domain.SchoolConfig
import com.unipocket.domain.Semester
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

/**
 * demo 的假数据。
 *
 * 外校 fork 后的第一件事就是把这里换掉：
 * 1. 用本校真实的节次时间与学期起始日构造 [config]；
 * 2. 删掉 [courses]，改由自己的 `ScheduleSource` 提供；
 * 3. 把 [FakeRoomSource] 换成真的空教室数据源（可选）。
 *
 * 假数据刻意覆盖了几种边界：周末有课、第 9 周才开课的课、第 17 周才开的课程设计、
 * 周次不连续的"形势与政策"，用来验证周次过滤与网格布局。
 */
object FakeData {

    val config: SchoolConfig = SchoolConfig(
        schoolName = "示例大学",
        periods = listOf(
            Period(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
            Period(2, LocalTime.of(8, 55), LocalTime.of(9, 40)),
            Period(3, LocalTime.of(10, 0), LocalTime.of(10, 45)),
            Period(4, LocalTime.of(10, 55), LocalTime.of(11, 40)),
            Period(5, LocalTime.of(14, 0), LocalTime.of(14, 45)),
            Period(6, LocalTime.of(14, 55), LocalTime.of(15, 40)),
            Period(7, LocalTime.of(16, 0), LocalTime.of(16, 45)),
            Period(8, LocalTime.of(16, 55), LocalTime.of(17, 40)),
            Period(9, LocalTime.of(19, 0), LocalTime.of(19, 45)),
            Period(10, LocalTime.of(19, 55), LocalTime.of(20, 40)),
        ),
        // 2026-09-01 是周二，框架会自动归一到所在周的周一（08-31）
        semester = Semester(
            name = "2026-2027 第一学期",
            startDate = LocalDate.of(2026, 9, 1),
            weekCount = 20,
        ),
    )

    val courses: List<Course> = listOf(
        Course(
            id = "fake-math",
            name = "高等数学",
            teacher = "张伟",
            room = "教1-101",
            weeks = weeksOf(1..16),
            dayOfWeek = 1,
            periods = 1..2,
        ),
        Course(
            id = "fake-physics",
            name = "大学物理",
            teacher = "李娜",
            room = "教2-203",
            weeks = weeksOf(1..16),
            dayOfWeek = 2,
            periods = 3..4,
        ),
        Course(
            id = "fake-programming",
            name = "程序设计基础",
            teacher = "王强",
            room = "计算机楼-302",
            weeks = weeksOf(1..12),
            dayOfWeek = 3,
            periods = 1..2,
        ),
        Course(
            id = "fake-english",
            name = "大学英语",
            teacher = "刘芳",
            room = "外语楼-105",
            weeks = weeksOf(1..16),
            dayOfWeek = 4,
            periods = 3..4,
        ),
        Course(
            id = "fake-datastructure",
            name = "数据结构",
            teacher = "陈静",
            room = "计算机楼-401",
            weeks = weeksOf(1..16),
            dayOfWeek = 5,
            periods = 5..6,
        ),
        Course(
            id = "fake-linear",
            name = "线性代数",
            teacher = "赵磊",
            room = "教1-204",
            weeks = weeksOf(1..8),
            dayOfWeek = 1,
            periods = 3..4,
        ),
        Course(
            id = "fake-pe",
            name = "体育（羽毛球）",
            teacher = "孙鹏",
            room = "体育馆",
            weeks = weeksOf(1..16),
            dayOfWeek = 2,
            periods = 5..6,
        ),
        Course(
            id = "fake-history",
            name = "中国近现代史纲要",
            teacher = "周敏",
            room = "教3-101",
            weeks = weeksOf(1..10),
            dayOfWeek = 4,
            periods = 1..2,
        ),
        Course(
            id = "fake-circuit",
            name = "电路分析基础",
            teacher = "吴涛",
            room = "实验楼-208",
            weeks = weeksOf(9..16),
            dayOfWeek = 3,
            periods = 5..6,
        ),
        Course(
            id = "fake-policy",
            name = "形势与政策",
            teacher = "郑华",
            room = "教3-202",
            weeks = weeksOf(5..8) + weeksOf(11..14),
            dayOfWeek = 5,
            periods = 7..8,
        ),
        Course(
            id = "fake-physics-lab",
            name = "大学物理实验",
            teacher = "李娜",
            room = "实验楼-105",
            weeks = weeksOf(2..15),
            dayOfWeek = 6,
            periods = 1..2,
        ),
        Course(
            id = "fake-project",
            name = "数据结构课程设计",
            teacher = "陈静",
            room = "计算机楼-401",
            weeks = weeksOf(17..20),
            dayOfWeek = 5,
            periods = 5..8,
        ),
    )
}

private fun weeksOf(range: IntRange): Set<Int> = range.toSet()

/**
 * 假数据源：模拟一次网络请求的延迟，方便观察"加载中"状态。
 *
 * 外校接入时，把 [FakeData.courses] 换成自家教务返回的 `List<Course>` 即可——
 * 也可以直接参考这个类写自己的 `ScheduleSource`。
 */
class FakeScheduleSource(
    private val delayMillis: Long = 400,
) : ScheduleSource {

    override val id: String = "fake"

    override suspend fun loadCourses(): List<Course> {
        if (delayMillis > 0) delay(delayMillis)
        return FakeData.courses
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
