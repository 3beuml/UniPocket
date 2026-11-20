package com.unipocket.demo

import android.content.SharedPreferences
import com.unipocket.domain.Course
import com.unipocket.domain.EditableScheduleSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class StoredCourse(
    val id: String,
    val name: String,
    val teacher: String?,
    val room: String?,
    val weeks: List<Int>,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
)

private fun Course.toStored() = StoredCourse(
    id = id,
    name = name,
    teacher = teacher,
    room = room,
    weeks = weeks.sorted(),
    dayOfWeek = dayOfWeek,
    startPeriod = startPeriod,
    endPeriod = endPeriod,
)

private fun StoredCourse.toCourse() = Course(
    id = id,
    name = name,
    teacher = teacher,
    room = room,
    weeks = weeks.toSet(),
    dayOfWeek = dayOfWeek,
    periods = startPeriod..endPeriod,
)

/**
 * 手动补录数据源 —— [EditableScheduleSource] 的 demo 参考实现。
 *
 * 持久化用 JSON 存进 SharedPreferences。真实接入方可以换成 Room / DataStore，
 * 只要实现同样三个方法，界面代码一行都不用改。
 */
class ManualCourseSource(
    private val prefs: SharedPreferences,
) : EditableScheduleSource {

    override val id: String = "manual"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadCourses(): List<Course> =
        decode(prefs.getString(KEY, null))

    override suspend fun upsert(course: Course) {
        persist(loadCourses().filter { it.id != course.id } + course)
    }

    override suspend fun delete(id: String) {
        persist(loadCourses().filter { it.id != id })
    }

    override suspend fun manages(id: String): Boolean =
        loadCourses().any { it.id == id }

    private fun persist(courses: List<Course>) {
        prefs.edit().putString(KEY, json.encodeToString(courses.map { it.toStored() })).apply()
    }

    private fun decode(payload: String?): List<Course> = runCatching {
        json.decodeFromString<List<StoredCourse>>(payload ?: "[]").map { it.toCourse() }
    }.getOrDefault(emptyList())

    private companion object {
        const val KEY = "manual.courses"
    }
}
