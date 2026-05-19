package com.companion.chat.ui.chat

import com.companion.chat.companion.CompanionRuntime
import com.companion.chat.data.memory.MemoryRepository
import com.companion.chat.data.model.ChatMessage

class ChatRuntimeActions(
    private val companionRuntime: CompanionRuntime,
    private val memoryRepository: MemoryRepository,
    private val autoPreferenceLearningEnabledProvider: () -> Boolean
) {

    suspend fun storeRuleBasedMemoriesBeforeGeneration(
        userMessage: ChatMessage,
        sessionId: String
    ): Int {
        if (autoPreferenceLearningEnabledProvider()) {
            return 0
        }
        val content = userMessage.content.trim()
        if (content.isBlank() || sessionId.isBlank()) {
            return 0
        }
        return memoryRepository.extractAndStoreMemories(
            userMessage = content,
            sessionId = sessionId
        ).size
    }

    fun triggerConversationBoundary(
        reason: String,
        sessionId: String,
        messages: List<ChatMessage>
    ) {
        companionRuntime.onConversationBoundary(
            reason = reason,
            sessionId = sessionId,
            messages = messages
        )
    }
}
