package com.companion.chat.data.role

import com.companion.chat.data.local.dao.RoleCardDao
import com.companion.chat.data.local.entity.RoleCard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleCardRepositoryTest {

    @Test
    fun `创建角色卡时会规范化名称和核心人设`() = runBlocking {
        val dao = FakeRoleCardDao()
        val repository = RoleCardRepository(dao, nowProvider = { 10L })

        repository.createRoleCard(
            name = "  小夏  ",
            description = "  温柔陪伴  ",
            avatar = "",
            persona = "  温柔可靠的陪伴者  ",
            speakingStyle = "  轻松自然  ",
            background = "",
            rules = "",
            taboos = "",
            openingMessage = "",
            exampleDialogue = ""
        )

        val created = dao.roleCards.single()
        assertEquals("小夏", created.name)
        assertEquals("温柔可靠的陪伴者", created.persona)
        assertEquals("person", created.avatar)
        assertFalse(created.isBuiltIn)
    }

    @Test
    fun `激活角色卡时只保留一个激活项`() = runBlocking {
        val dao = FakeRoleCardDao(
            mutableListOf(
                roleCard(id = 1L, name = "角色A", isActive = true),
                roleCard(id = 2L, name = "角色B", isActive = false)
            )
        )
        val repository = RoleCardRepository(dao, nowProvider = { 20L })

        repository.activateRoleCard(2L)

        assertEquals(listOf(2L), dao.roleCards.filter { it.isActive }.map { it.id })
        assertEquals(20L, dao.roleCards.first { it.id == 2L }.updatedAt)
    }

    @Test
    fun `删除内置角色卡会被拦截`() = runBlocking {
        val dao = FakeRoleCardDao(
            mutableListOf(
                roleCard(id = 1L, name = "内置角色", isBuiltIn = true)
            )
        )
        val repository = RoleCardRepository(dao)

        var thrown = false
        try {
            repository.deleteRoleCard(1L)
        } catch (_: IllegalStateException) {
            thrown = true
        }

        assertTrue(thrown)
        assertEquals(1, dao.roleCards.size)
    }

    private fun roleCard(
        id: Long,
        name: String,
        isBuiltIn: Boolean = false,
        isActive: Boolean = false
    ) = RoleCard(
        id = id,
        name = name,
        description = "",
        avatar = "person",
        persona = "默认人设",
        speakingStyle = "",
        background = "",
        rules = "",
        taboos = "",
        openingMessage = "",
        exampleDialogue = "",
        isBuiltIn = isBuiltIn,
        isActive = isActive,
        createdAt = 0L,
        updatedAt = 0L
    )

    private class FakeRoleCardDao(
        val roleCards: MutableList<RoleCard> = mutableListOf()
    ) : RoleCardDao {

        private var nextId = (roleCards.maxOfOrNull { it.id } ?: 0L) + 1L

        override suspend fun insert(roleCard: RoleCard): Long {
            val inserted = roleCard.copy(id = nextId++)
            roleCards += inserted
            return inserted.id
        }

        override suspend fun update(roleCard: RoleCard) {
            val index = roleCards.indexOfFirst { it.id == roleCard.id }
            if (index >= 0) {
                roleCards[index] = roleCard
            }
        }

        override suspend fun delete(roleCard: RoleCard) {
            roleCards.removeAll { it.id == roleCard.id }
        }

        override suspend fun getAll(): List<RoleCard> =
            roleCards.sortedWith(compareByDescending<RoleCard> { it.isActive }.thenByDescending { it.updatedAt })

        override suspend fun getActive(): RoleCard? = roleCards.firstOrNull { it.isActive }

        override suspend fun getById(id: Long): RoleCard? = roleCards.firstOrNull { it.id == id }

        override suspend fun deactivateAll(): Int {
            roleCards.replaceAll { it.copy(isActive = false) }
            return roleCards.size
        }

        override suspend fun activate(id: Long, now: Long): Int {
            val index = roleCards.indexOfFirst { it.id == id }
            if (index < 0) {
                return 0
            }
            roleCards[index] = roleCards[index].copy(isActive = true, updatedAt = now)
            return 1
        }
    }
}
