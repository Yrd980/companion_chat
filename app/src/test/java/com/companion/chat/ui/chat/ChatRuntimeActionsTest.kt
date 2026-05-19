package com.companion.chat.ui.chat

import androidx.sqlite.db.SupportSQLiteQuery
import com.companion.chat.companion.CompanionPostTurnLearning
import com.companion.chat.companion.CompanionRuntime
import com.companion.chat.data.local.dao.MemoryDao
import com.companion.chat.data.local.dao.RoleCardDao
import com.companion.chat.data.local.dao.SkillDao
import com.companion.chat.data.local.entity.Memory
import com.companion.chat.data.local.entity.RoleCard
import com.companion.chat.data.local.entity.Skill
import com.companion.chat.data.memory.MemoryRepository
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.MessageRole
import com.companion.chat.identity.RoleCardRepository
import com.companion.chat.capability.SkillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRuntimeActionsTest {

    @Test
    fun `auto learning off stores rule based memories before generation`() = runBlocking {
        val insertedMemories = mutableListOf<Memory>()
        val actions = createActions(
            memoryDao = FakeMemoryDao(insertedMemories),
            autoPreferenceLearningEnabled = false
        )

        val count = actions.storeRuleBasedMemoriesBeforeGeneration(
            userMessage = ChatMessage(role = MessageRole.USER, content = "我喜欢吃火锅"),
            sessionId = "session-1"
        )

        assertEquals(1, count)
        assertEquals("用户喜欢吃火锅", insertedMemories.single().content)
        assertEquals("rule_extractor", insertedMemories.single().source)
        assertEquals("session-1", insertedMemories.single().sessionId)
    }

    @Test
    fun `auto learning on skips rule based memory write before generation`() = runBlocking {
        val insertedMemories = mutableListOf<Memory>()
        val actions = createActions(
            memoryDao = FakeMemoryDao(insertedMemories),
            autoPreferenceLearningEnabled = true
        )

        val count = actions.storeRuleBasedMemoriesBeforeGeneration(
            userMessage = ChatMessage(role = MessageRole.USER, content = "我喜欢吃火锅"),
            sessionId = "session-1"
        )

        assertEquals(0, count)
        assertTrue(insertedMemories.isEmpty())
    }

    @Test
    fun `boundary actions forward old session and messages to runtime learning`() {
        val learner = FakePostTurnLearning()
        val actions = createActions(
            memoryDao = FakeMemoryDao(),
            autoPreferenceLearningEnabled = true,
            postTurnLearning = learner
        )
        val messages = listOf(ChatMessage(role = MessageRole.USER, content = "旧会话消息"))
        val reasons = listOf("新建会话前", "角色对话前", "切换会话", "应用进入后台")

        reasons.forEach { reason ->
            actions.triggerConversationBoundary(
                reason = reason,
                sessionId = "old-session",
                messages = messages
            )
        }

        assertEquals(
            reasons.map { reason -> Boundary(reason, "old-session", messages) },
            learner.boundaries
        )
    }

    private fun createActions(
        memoryDao: FakeMemoryDao,
        autoPreferenceLearningEnabled: Boolean,
        postTurnLearning: CompanionPostTurnLearning? = null
    ): ChatRuntimeActions {
        return ChatRuntimeActions(
            companionRuntime = CompanionRuntime(
                roleCardRepository = RoleCardRepository(FakeRoleCardDao()),
                skillRepository = SkillRepository(FakeSkillDao()),
                postTurnLearning = postTurnLearning
            ),
            memoryRepository = MemoryRepository(
                memoryDao = memoryDao,
                nowProvider = { 1_700_000_000_000L }
            ),
            autoPreferenceLearningEnabledProvider = { autoPreferenceLearningEnabled }
        )
    }

    private class FakeMemoryDao(
        private val memories: MutableList<Memory> = mutableListOf()
    ) : MemoryDao {
        private var nextId = 1L

        override suspend fun insert(memory: Memory): Long {
            val inserted = memory.copy(id = nextId++)
            memories += inserted
            return inserted.id
        }

        override suspend fun insertAll(memories: List<Memory>): List<Long> = memories.map { insert(it) }

        override suspend fun update(memory: Memory) = Unit

        override suspend fun delete(memory: Memory) = Unit

        override suspend fun getAll(): List<Memory> = memories

        override fun observeAll(): Flow<List<Memory>> = emptyFlow()

        override suspend fun getByLayer(layer: String): List<Memory> = memories.filter { it.layer == layer }

        override suspend fun getPersistentMemories(): List<Memory> = memories.filter { it.layer == "long_term" }

        override suspend fun getByCategory(category: String): List<Memory> =
            memories.filter { it.category == category }

        override suspend fun findExactMatch(category: String, content: String): Memory? =
            memories.firstOrNull { it.category == category && it.content == content }

        override suspend fun searchByFTS(query: SupportSQLiteQuery): List<Memory> = emptyList()

        override suspend fun incrementReference(id: Long): Int = 0

        override suspend fun promoteToLongTerm(id: Long, now: Long): Int = 0

        override suspend fun cleanupExpiredShortTerm(now: Long): Int = 0

        override suspend fun getPromotableShortTerm(): List<Memory> = emptyList()
    }

    private class FakeRoleCardDao : RoleCardDao {
        override suspend fun insert(roleCard: RoleCard): Long = roleCard.id
        override suspend fun update(roleCard: RoleCard) = Unit
        override suspend fun delete(roleCard: RoleCard) = Unit
        override suspend fun getAll(): List<RoleCard> = emptyList()
        override suspend fun getActive(): RoleCard? = null
        override suspend fun getById(id: Long): RoleCard? = null
        override suspend fun deactivateAll(): Int = 0
        override suspend fun activate(id: Long, now: Long): Int = 0
    }

    private class FakeSkillDao : SkillDao {
        override suspend fun insert(skill: Skill): Long = skill.id
        override suspend fun insertAll(skills: List<Skill>): List<Long> = skills.map { it.id }
        override suspend fun update(skill: Skill) = Unit
        override suspend fun delete(skill: Skill) = Unit
        override suspend fun getAll(): List<Skill> = emptyList()
        override suspend fun getActive(): Skill? = null
        override suspend fun getById(id: Long): Skill? = null
        override suspend fun deactivateAll(): Int = 0
        override suspend fun activate(id: Long, now: Long): Int = 0
    }

    private class FakePostTurnLearning : CompanionPostTurnLearning {
        val boundaries = mutableListOf<Boundary>()

        override fun scheduleAfterIdle(
            sessionIdProvider: () -> String,
            messagesProvider: () -> List<ChatMessage>
        ) = Unit

        override fun triggerNow(
            reason: String,
            sessionId: String,
            messages: List<ChatMessage>
        ) {
            boundaries += Boundary(reason, sessionId, messages)
        }

        override fun cancelRunningSummary() = Unit

        override fun release() = Unit
    }

    private data class Boundary(
        val reason: String,
        val sessionId: String,
        val messages: List<ChatMessage>
    )
}
