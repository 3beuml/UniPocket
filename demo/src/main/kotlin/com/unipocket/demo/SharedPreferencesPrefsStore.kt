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
        trySend(currentValue(key))

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) trySend(currentValue(key))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    override suspend fun read(key: String): String? = currentValue(key)

    /**
     * 用 [SharedPreferences.getAll] 而不是 getString：
     * getString 遇到类型不匹配的历史数据（外部工具写入的 boolean 等）会直接抛
     * ClassCastException，这里宁可把 1 当成 "1"，也不能让 App 崩在启动页。
     */
    private fun currentValue(key: String): String? = prefs.all[key]?.toString()

    override suspend fun write(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }
}
