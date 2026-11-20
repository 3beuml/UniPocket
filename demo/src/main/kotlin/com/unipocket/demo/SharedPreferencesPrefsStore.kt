package com.unipocket.demo

import android.content.SharedPreferences
import com.unipocket.domain.PrefsStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Collections

/**
 * [PrefsStore] 的 SharedPreferences 实现 —— 给接入方抄的参考实现。
 *
 * 框架本身不绑定任何存储：想换成 DataStore，实现同样三个方法即可。
 *
 * 注意：SharedPreferences 内部用弱引用保存监听器，所以这里必须自己持有强引用，
 * 否则监听器会被 GC、`observe` 之后收不到变化。
 */
class SharedPreferencesPrefsStore(
    private val prefs: SharedPreferences,
) : PrefsStore {

    private val activeListeners = Collections.synchronizedSet(
        mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>(),
    )

    override fun observe(key: String): Flow<String?> = callbackFlow {
        trySend(prefs.getString(key, null))

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) trySend(prefs.getString(key, null))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    override suspend fun read(key: String): String? = prefs.getString(key, null)

    override suspend fun write(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }
}
