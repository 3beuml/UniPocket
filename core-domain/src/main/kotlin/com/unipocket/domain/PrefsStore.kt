package com.unipocket.domain

import kotlinx.coroutines.flow.Flow

/**
 * 偏好读写接口。框架**不提供实现**，存储方式完全外置：
 * demo 用内存实现，接入方可以用 DataStore / SharedPreferences / 自己的数据库。
 *
 * 值统一用字符串（"true" / "1.5" / "#FF5722" 之类），由调用方负责解析。
 * 这样接口不依赖序列化库，也不会因为新增偏好项而破坏二进制兼容。
 */
interface PrefsStore {
    /** 观察某个键的变化；当前值不存在时发出 null */
    fun observe(key: String): Flow<String?>

    /** 读取一次；不存在返回 null */
    suspend fun read(key: String): String?

    /** 写入；传 null 表示删除 */
    suspend fun write(key: String, value: String?)
}
