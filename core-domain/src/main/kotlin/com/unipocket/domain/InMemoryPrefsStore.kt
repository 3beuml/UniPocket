package com.unipocket.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * [PrefsStore] 的内存实现：进程退出即丢失。
 *
 * 用途：demo、单测、以及"先跑起来再换存储"的接入初期。
 * 真实 App 请换成 DataStore / SharedPreferences 实现（demo 里有一份可参考的写法）。
 */
class InMemoryPrefsStore : PrefsStore {

    private val values = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observe(key: String): Flow<String?> =
        values.map { it[key] }.distinctUntilChanged()

    override suspend fun read(key: String): String? = values.value[key]

    override suspend fun write(key: String, value: String?) {
        values.update { current ->
            if (value == null) current - key else current + (key to value)
        }
    }
}
