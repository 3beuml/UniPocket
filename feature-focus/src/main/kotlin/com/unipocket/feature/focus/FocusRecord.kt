package com.unipocket.feature.focus

/**
 * 一条完成的专注记录。休息不计入统计。
 *
 * [startedAtMillis] 用 Epoch 毫秒而不是 LocalDate：归属哪一天由统计方按
 * 设备时区折算（跨零点的专注归属开始那天）。
 */
data class FocusRecord(
    /** 本次专注开始时刻（Epoch 毫秒），同时充当记录 id */
    val startedAtMillis: Long,
    /** 专注时长（分钟），用于统计与柱状图 */
    val minutes: Int,
)

/**
 * 专注记录的存储接口。**框架不提供持久化实现**：
 * demo 用 SharedPreferences + JSON 作参考实现，接入方可换 Room / DataStore。
 *
 * 约定：[append] 允许重复 id（同毫秒开始两次几乎不可能，不做去重）。
 */
interface FocusRecordStore {
    suspend fun loadAll(): List<FocusRecord>
    suspend fun append(record: FocusRecord)
}

/** 测试与"先跑起来"用的内存实现 */
class InMemoryFocusRecordStore : FocusRecordStore {

    private val records = mutableListOf<FocusRecord>()

    override suspend fun loadAll(): List<FocusRecord> = records.toList()

    override suspend fun append(record: FocusRecord) {
        records += record
    }
}
