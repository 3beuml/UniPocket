package com.unipocket.demo

import android.content.SharedPreferences
import com.unipocket.feature.focus.FocusRecord
import com.unipocket.feature.focus.FocusRecordStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class StoredFocusRecord(
    val startedAtMillis: Long,
    val minutes: Int,
)

/**
 * 专注记录存储的 demo 参考实现：JSON 存 SharedPreferences。
 * 接入方想换 Room / DataStore，实现 [FocusRecordStore] 的两个方法即可。
 */
class SharedPrefsFocusRecordStore(
    private val prefs: SharedPreferences,
) : FocusRecordStore {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadAll(): List<FocusRecord> = runCatching {
        json.decodeFromString<List<StoredFocusRecord>>(prefs.getString(KEY, "[]") ?: "[]")
            .map { FocusRecord(it.startedAtMillis, it.minutes) }
    }.getOrDefault(emptyList())

    override suspend fun append(record: FocusRecord) {
        val all = loadAll() + record
        prefs.edit()
            .putString(KEY, json.encodeToString(all.map { StoredFocusRecord(it.startedAtMillis, it.minutes) }))
            .apply()
    }

    private companion object {
        const val KEY = "focus.records"
    }
}
