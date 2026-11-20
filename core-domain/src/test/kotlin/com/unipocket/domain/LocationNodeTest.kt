package com.unipocket.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationNodeTest {

    private val tree = LocationNode(
        id = "campus",
        name = "主校区",
        level = 0,
        children = listOf(
            LocationNode(
                id = "b1",
                name = "1 号楼",
                level = 1,
                children = listOf(
                    LocationNode(id = "b1-101", name = "101", level = 2),
                    LocationNode(id = "b1-102", name = "102", level = 2),
                ),
            ),
            LocationNode(id = "b2", name = "2 号楼", level = 1),
        ),
    )

    @Test
    fun `flatten 深度优先展开`() {
        assertEquals(
            listOf("campus", "b1", "b1-101", "b1-102", "b2"),
            tree.flatten().map { it.id }.toList(),
        )
    }

    @Test
    fun `find 按 id 查节点`() {
        assertEquals("102", tree.find("b1-102")?.name)
        assertNull(tree.find("nope"))
    }

    @Test
    fun `pathTo 返回完整路径`() {
        assertEquals(
            listOf("campus", "b1", "b1-101"),
            tree.pathTo("b1-101")?.map { it.id },
        )
        assertEquals(listOf("campus"), tree.pathTo("campus")?.map { it.id })
        assertNull(tree.pathTo("nope"))
    }

    @Test
    fun `叶子节点判定`() {
        assertTrue(tree.find("b2")!!.isLeaf)
        assertTrue(!tree.find("b1")!!.isLeaf)
    }
}
