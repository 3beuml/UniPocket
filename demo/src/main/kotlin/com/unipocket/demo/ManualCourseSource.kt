package com.unipocket.demo

import android.content.SharedPreferences
import com.unipocket.domain.EditableScheduleSource
import com.unipocket.domain.Session
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** [Session] 的可序列化镜像（Session 本体是纯模型，不绑序列化） */
@Serializable
private data class StoredSession(
    val id: String,
    val semesterId: Int,
    val source: String,
    val courseCode: String,
    val courseName: String,
    val weekIndex: Int,
    val weekday: Int,
    val startUnit: Int,
    val endUnit: Int,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val teacherName: String? = null,
    val roomText: String? = null,
    val campusText: String? = null,
    val color: String? = null,
    val experiment: Boolean = false,
)

private fun Session.toStored() = StoredSession(
    id = id,
    semesterId = semesterId,
    source = source,
    courseCode = courseCode,
    courseName = courseName,
    weekIndex = weekIndex,
    weekday = weekday,
    startUnit = startUnit,
    endUnit = endUnit,
    date = date,
    startTime = startTime,
    endTime = endTime,
    teacherName = teacherName,
    roomText = roomText,
    campusText = campusText,
    color = color,
    experiment = experiment,
)

private fun StoredSession.toSession() = Session(
    id = id,
    semesterId = semesterId,
    source = source,
    courseCode = courseCode,
    courseName = courseName,
    weekIndex = weekIndex,
    weekday = weekday,
    startUnit = startUnit,
    endUnit = endUnit,
    date = date,
    startTime = startTime,
    endTime = endTime,
    teacherName = teacherName,
    roomText = roomText,
    campusText = campusText,
    color = color,
    experiment = experiment,
)

/**
 * 手动补录数据源 —— [EditableScheduleSource] 的 demo 参考实现。
 *
 * 新模型一条 [Session] 只对应一个教学周，所以这里按「周记录」逐条存取；
 * 一门课覆盖 N 周就是 N 条记录（同 courseCode、各自 weekIndex）。
 * 持久化用 JSON 存进 SharedPreferences。真实接入方可以换成 Room / DataStore，
 * 只要实现同样三个方法，界面代码一行都不用改。
 */
class ManualCourseSource(
    private val prefs: SharedPreferences,
) : EditableScheduleSource {

    override val id: String = "manual"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadSessions(): List<Session> =
        decode(prefs.getString(KEY, null))

    override suspend fun upsert(session: Session) {
        persist(loadSessions().filter { it.id != session.id } + session)
    }

    override suspend fun delete(id: String) {
        persist(loadSessions().filter { it.id != id })
    }

    override suspend fun manages(id: String): Boolean =
        loadSessions().any { it.id == id }

    private fun persist(sessions: List<Session>) {
        prefs.edit().putString(KEY, json.encodeToString(sessions.map { it.toStored() })).apply()
    }

    private fun decode(payload: String?): List<Session> = runCatching {
        json.decodeFromString<List<StoredSession>>(payload ?: "[]").map { it.toSession() }
    }.getOrDefault(emptyList())

    private companion object {
        const val KEY = "manual.sessions"
    }
}
