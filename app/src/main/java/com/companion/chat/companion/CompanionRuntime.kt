package com.companion.chat.companion

import com.companion.chat.context.ContextManager
import com.companion.chat.context.ContextSettings
import com.companion.chat.context.PromptAssembler
import com.companion.chat.engine.InferenceEngine
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.MessageRole
import com.companion.chat.memory.MemoryPromptBuilder
import com.companion.chat.data.memory.MemoryRepository
import com.companion.chat.identity.RoleCardPromptBuilder
import com.companion.chat.identity.RoleCardRepository
import com.companion.chat.data.preferences.PreferenceRepository
import com.companion.chat.capability.SkillRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CompanionRuntime(
    private val roleCardRepository: RoleCardRepository,
    private val skillRepository: SkillRepository,
    private val preferenceRepository: PreferenceRepository? = null,
    private val memoryRepository: MemoryRepository? = null,
    private val contextManager: ContextManager? = null,
    private val inferenceEngineProvider: () -> InferenceEngine? = { null },
    private val postTurnLearning: CompanionPostTurnLearning? = null,
    private val promptAssembler: PromptAssembler = PromptAssembler(),
    private val memoryPromptBuilder: MemoryPromptBuilder = MemoryPromptBuilder(),
    private val roleCardPromptBuilder: RoleCardPromptBuilder = RoleCardPromptBuilder(),
    private val defaultBasePrompt: String = DEFAULT_BASE_PROMPT
) {

    suspend fun refreshBasePrompt(): String {
        val rolePrompt = roleCardPromptBuilder.build(roleCardRepository.getActiveRoleCard())
        val skillPrompt = skillRepository.getActiveSkill()?.systemPrompt?.trim().orEmpty()

        return buildList {
            add(defaultBasePrompt)
            if (rolePrompt.isNotBlank()) {
                add(rolePrompt)
            }
            if (skillPrompt.isNotBlank()) {
                add(skillPrompt)
            }
        }.joinToString(separator = "\n\n")
    }

    suspend fun activateRoleCardAndRefreshPrompt(roleCardId: Long): String {
        roleCardRepository.activateRoleCard(roleCardId)
        return refreshBasePrompt()
    }

    suspend fun activateSkillAndRefreshPrompt(skillId: Long): String {
        skillRepository.activateSkill(skillId)
        return refreshBasePrompt()
    }

    suspend fun prepareTurnContext(userInput: String): CompanionTurnContext {
        val query = userInput.trim()
        return try {
            val confirmedPreferences = preferenceRepository?.getConfirmedPreferences().orEmpty()
            val repository = memoryRepository
            val persistentMemories = repository?.getPersistentMemories().orEmpty()
            val relevantMemories = repository?.retrieveRelevantMemories(userInput).orEmpty()
            CompanionTurnContext(
                query = query,
                userPreferences = buildConfirmedPreferencePrompt(
                    confirmedPreferences.map { it.content }
                ),
                persistentMemoryPrompt = memoryPromptBuilder.buildPersistent(persistentMemories),
                memoryPrompt = memoryPromptBuilder.build(relevantMemories),
                confirmedPreferenceCount = confirmedPreferences.size,
                persistentMemoryCount = persistentMemories.size,
                retrievedMemoryCount = relevantMemories.size
            )
        } catch (error: Exception) {
            CompanionTurnContext(
                query = query,
                preparationError = error.message ?: error.javaClass.simpleName
            )
        }
    }

    suspend fun rebuildConversationWithContext(
        stableMessages: List<ChatMessage>,
        baseSystemPrompt: String,
        settings: ContextSettings,
        turnContext: CompanionTurnContext = CompanionTurnContext(),
        forceRebuild: Boolean = false
    ): CompanionRebuildResult {
        val manager = contextManager ?: return CompanionRebuildResult.skipped()
        val engine = inferenceEngineProvider() ?: return CompanionRebuildResult.skipped()
        val shouldInjectContext = turnContext.userPreferences.isNotBlank() ||
            turnContext.persistentMemoryPrompt.isNotBlank() ||
            turnContext.memoryPrompt.isNotBlank()

        if (!forceRebuild && !manager.shouldCompress(stableMessages, settings) && !shouldInjectContext) {
            return CompanionRebuildResult.skipped()
        }

        val contextWindow = manager.buildContext(
            messages = stableMessages,
            systemPrompt = baseSystemPrompt,
            userPreferences = turnContext.userPreferences,
            persistentMemoryPrompt = turnContext.persistentMemoryPrompt,
            memoryPrompt = turnContext.memoryPrompt,
            settings = settings
        )
        val rebuildSucceeded = engine.rebuildConversation(contextWindow.systemPrompt)
        if (!rebuildSucceeded) {
            return CompanionRebuildResult(
                rebuildAttempted = true,
                rebuildSucceeded = false,
                replaySucceeded = null,
                fallbackSucceeded = null,
                recentMessageCount = contextWindow.recentMessages.size,
                historySummaryEmpty = contextWindow.historySummary.isBlank(),
                preferenceInjected = contextWindow.userPreferences.isNotBlank(),
                persistentMemoryInjected = contextWindow.persistentMemoryPrompt.isNotBlank(),
                memoryInjected = contextWindow.memoryPrompt.isNotBlank()
            )
        }

        val replaySucceeded = engine.replayMessages(contextWindow.recentMessages)
        if (replaySucceeded) {
            return CompanionRebuildResult(
                rebuildAttempted = true,
                rebuildSucceeded = true,
                replaySucceeded = true,
                fallbackSucceeded = null,
                recentMessageCount = contextWindow.recentMessages.size,
                historySummaryEmpty = contextWindow.historySummary.isBlank(),
                preferenceInjected = contextWindow.userPreferences.isNotBlank(),
                persistentMemoryInjected = contextWindow.persistentMemoryPrompt.isNotBlank(),
                memoryInjected = contextWindow.memoryPrompt.isNotBlank()
            )
        }

        val fallbackPrompt = promptAssembler.assemble(
            baseSystemPrompt = contextWindow.systemPrompt,
            userPreferences = "",
            persistentMemoryPrompt = "",
            memoryPrompt = "",
            historySummary = "",
            recentConversationSnippet = buildRecentConversationSnippet(contextWindow.recentMessages)
        )
        val fallbackSucceeded = engine.rebuildConversationWithFallbackContext(fallbackPrompt)
        return CompanionRebuildResult(
            rebuildAttempted = true,
            rebuildSucceeded = true,
            replaySucceeded = false,
            fallbackSucceeded = fallbackSucceeded,
            recentMessageCount = contextWindow.recentMessages.size,
            historySummaryEmpty = contextWindow.historySummary.isBlank(),
            preferenceInjected = contextWindow.userPreferences.isNotBlank(),
            persistentMemoryInjected = contextWindow.persistentMemoryPrompt.isNotBlank(),
            memoryInjected = contextWindow.memoryPrompt.isNotBlank()
        )
    }

    private fun buildRecentConversationSnippet(messages: List<ChatMessage>): String {
        return messages.mapNotNull { message ->
            val content = message.content.trim()
            if (content.isBlank()) {
                return@mapNotNull null
            }

            val roleLabel = when (message.role) {
                MessageRole.USER -> "用户"
                MessageRole.ASSISTANT -> "助手"
                MessageRole.SYSTEM -> "系统"
            }
            "$roleLabel：${content.take(80)}"
        }.joinToString(separator = "\n")
    }

    private fun buildConfirmedPreferencePrompt(confirmedPreferences: List<String>): String {
        if (confirmedPreferences.isEmpty()) {
            return ""
        }
        return buildString {
            appendLine("关于当前用户的已知信息（请自然地融入对话，不要刻意提及你知道这些）：")
            confirmedPreferences.forEach { preference ->
                appendLine("- $preference")
            }
        }.trim()
    }

    fun onTurnFinished(
        sessionIdProvider: () -> String,
        messagesProvider: () -> List<ChatMessage>
    ) {
        postTurnLearning?.scheduleAfterIdle(
            sessionIdProvider = sessionIdProvider,
            messagesProvider = messagesProvider
        )
    }

    fun onConversationBoundary(
        reason: String,
        sessionId: String,
        messages: List<ChatMessage>
    ) {
        postTurnLearning?.triggerNow(
            reason = reason,
            sessionId = sessionId,
            messages = messages
        )
    }

    fun cancelPostTurnLearning() {
        postTurnLearning?.cancelRunningSummary()
    }

    fun release() {
        postTurnLearning?.release()
    }

    suspend fun rebuildBasePromptForPromptChange(baseSystemPrompt: String): CompanionBasePromptRebuildResult {
        val engine = inferenceEngineProvider()
            ?: return CompanionBasePromptRebuildResult(rebuildAttempted = false, rebuildSucceeded = null)
        val rebuildSucceeded = engine.rebuildConversation(baseSystemPrompt)
        return CompanionBasePromptRebuildResult(
            rebuildAttempted = true,
            rebuildSucceeded = rebuildSucceeded
        )
    }

    fun runTurn(
        messages: List<ChatMessage>,
        baseSystemPrompt: String,
        settings: ContextSettings,
        userInput: String
    ): Flow<CompanionTurnEvent> = flow {
        val engine = inferenceEngineProvider() ?: return@flow
        val stableMessages = messages.filterNot { it.isStreaming }
        val turnContext = prepareTurnContext(userInput)
        emit(CompanionTurnEvent.ContextPrepared(turnContext))
        val rebuildResult = rebuildConversationWithContext(
            stableMessages = stableMessages,
            baseSystemPrompt = baseSystemPrompt,
            settings = settings,
            turnContext = turnContext,
            forceRebuild = false
        )
        emit(CompanionTurnEvent.ContextRebuildCompleted(rebuildResult, stableMessages.size))
        try {
            engine.sendMessageStream(messages).collect { token ->
                emit(CompanionTurnEvent.AssistantToken(token))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(CompanionTurnEvent.TurnFailed(e.message ?: e.javaClass.simpleName))
        }
    }

    companion object {
        const val DEFAULT_BASE_PROMPT =
            "你是 Anime Companion 的本地私密陪伴智能体。默认使用中文，像长期熟悉用户的伙伴一样自然回应：亲近但不过界，温柔但不说教，记得对话中的连续性与用户已经确认的偏好。你的记忆描述始终以用户为归属，不把用户的信息说成自己的经历。回答应简洁、有情绪承接，除非用户明确需要步骤或分析，否则少用训诫式建议。"
    }
}

sealed class CompanionTurnEvent {
    data class ContextPrepared(val context: CompanionTurnContext) : CompanionTurnEvent()

    data class ContextRebuildCompleted(
        val result: CompanionRebuildResult,
        val stableMessageCount: Int
    ) : CompanionTurnEvent()

    data class AssistantToken(val token: String) : CompanionTurnEvent()

    data class TurnFailed(val message: String) : CompanionTurnEvent()
}

interface CompanionPostTurnLearning {
    fun scheduleAfterIdle(
        sessionIdProvider: () -> String,
        messagesProvider: () -> List<ChatMessage>
    )

    fun triggerNow(
        reason: String,
        sessionId: String,
        messages: List<ChatMessage>
    )

    fun cancelRunningSummary()

    fun release()
}

data class CompanionTurnContext(
    val query: String = "",
    val userPreferences: String = "",
    val persistentMemoryPrompt: String = "",
    val memoryPrompt: String = "",
    val confirmedPreferenceCount: Int = 0,
    val persistentMemoryCount: Int = 0,
    val retrievedMemoryCount: Int = 0,
    val preparationError: String = ""
) {
    val hasConfirmedPreferences: Boolean
        get() = userPreferences.isNotBlank()

    val hasPersistentMemories: Boolean
        get() = persistentMemoryPrompt.isNotBlank()

    val hasRetrievedMemories: Boolean
        get() = memoryPrompt.isNotBlank()
}

data class CompanionBasePromptRebuildResult(
    val rebuildAttempted: Boolean,
    val rebuildSucceeded: Boolean?
)

data class CompanionRebuildResult(
    val rebuildAttempted: Boolean,
    val rebuildSucceeded: Boolean?,
    val replaySucceeded: Boolean?,
    val fallbackSucceeded: Boolean?,
    val recentMessageCount: Int = 0,
    val historySummaryEmpty: Boolean = true,
    val preferenceInjected: Boolean = false,
    val persistentMemoryInjected: Boolean = false,
    val memoryInjected: Boolean = false
) {
    companion object {
        fun skipped(): CompanionRebuildResult {
            return CompanionRebuildResult(
                rebuildAttempted = false,
                rebuildSucceeded = null,
                replaySucceeded = null,
                fallbackSucceeded = null
            )
        }
    }
}
