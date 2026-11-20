package com.unipocket.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScheduleSourceTest {

    private fun course(id: String) = Course(
        id = id,
        name = "大学物理",
        teacher = "李老师",
        room = "教2-202",
        weeks = setOf(1, 2, 3),
        dayOfWeek = 2,
        periods = 3..4,
    )

    private fun source(id: String, vararg courses: Course) = object : ScheduleSource {
        override val id = id
        override suspend fun loadCourses(): List<Course> = courses.toList()
    }

    @Test
    fun `内容相同但 id 不同的课只保留一份，且优先先出现的来源`() = runTest {
        val merged = MergedScheduleSource(
            source("eas", course("eas-1")),
            source("manual", course("manual-1")),
        ).loadCourses()

        assertEquals(1, merged.size)
        assertEquals("eas-1", merged.single().id)
    }

    @Test
    fun `周次或教室不同的课不会被误合并`() = runTest {
        val other = course("manual-2").copy(weeks = setOf(4))
        val merged = MergedScheduleSource(
            source("eas", course("eas-1")),
            source("manual", other),
        ).loadCourses()

        assertEquals(2, merged.size)
    }

    @Test
    fun `合并来源的 id 由各来源拼接`() {
        val merged = MergedScheduleSource(source("eas"), source("gms"))
        assertEquals("eas+gms", merged.id)
    }

    @Test
    fun `没有来源时构造失败`() {
        assertThrows(IllegalArgumentException::class.java) {
            MergedScheduleSource()
        }
    }
}
