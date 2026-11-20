package com.unipocket.domain

/**
 * 通用位置树节点。层数不限，用 [level] 表示深度（0 = 根，通常是校区）。
 *
 * 之所以做成通用树而不是固定"校区→楼→层→房间"：各校层级差异大，
 * 有的到楼为止，有的细分到房间甚至座位（7 级）。
 *
 * @property id 适配器内部唯一标识，同一棵树内不得重复
 * @property level 深度，从 0 开始；应与实际嵌套深度一致，便于界面缩进
 */
data class LocationNode(
    val id: String,
    val name: String,
    val level: Int,
    val children: List<LocationNode> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(id.isNotBlank()) { "LocationNode.id 不能为空" }
        require(name.isNotBlank()) { "LocationNode.name 不能为空" }
        require(level >= 0) { "LocationNode.level 必须 >= 0" }
    }

    /** 没有子节点即为叶子（通常是教室本身） */
    val isLeaf: Boolean get() = children.isEmpty()

    /** 深度优先展开为序列 */
    fun flatten(): Sequence<LocationNode> = sequence {
        yield(this@LocationNode)
        for (child in children) yieldAll(child.flatten())
    }

    /** 在整棵树里按 id 查找 */
    fun find(id: String): LocationNode? = flatten().firstOrNull { it.id == id }

    /**
     * 从根到 [targetId] 的路径（含首尾）；找不到返回 null。
     * 空教室页的"教学楼 / 楼层"面包屑靠它。
     */
    fun pathTo(targetId: String): List<LocationNode>? {
        if (id == targetId) return listOf(this)
        for (child in children) {
            val sub = child.pathTo(targetId)
            if (sub != null) return listOf(this) + sub
        }
        return null
    }
}
