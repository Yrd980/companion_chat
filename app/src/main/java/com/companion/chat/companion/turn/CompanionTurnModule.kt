package com.companion.chat.companion.turn

import android.net.Uri
import com.companion.chat.companion.CompanionRebuildResult
import com.companion.chat.context.ContextSettings
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.ConversationSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CompanionTurnModule {
    val snapshot: StateFlow<CompanionTurnSnapshot>
    val currentBaseSystemPrompt: String
    val currentContextSettings: ContextSettings

    suspend fun start()

    fun submit(request: CompanionTurnRequest): Flow<CompanionTurnEvent>

    suspend fun createSession()

    suspend fun startRoleConversation(roleCardId: Long)

    suspend fun openSession(sessionId: String)

    suspend fun renameSession(sessionId: String, title: String)

    suspend fun deleteSession(sessionId: String)

    suspend fun activateRoleCard(roleId: Long)

    suspend fun activateSkill(skillId: Long)

    fun onAppBackgrounded()

    fun cancelActiveTurn()

    fun release()
}

data class CompanionTurnSnapshot(
    val sessions: List<ConversationSession> = emptyList(),
    val currentSessionId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val assistantAvatarImageUri: String = "",
    val isGenerating: Boolean = false,
    val isConversationWarmingUp: Boolean = false
)

data class CompanionTurnRequest(
    val text: String,
    val images: List<Uri> = emptyList(),
    val delivery: CompanionTurnDelivery = CompanionTurnDelivery.TextOnly
)

sealed class CompanionTurnDelivery {
    data object TextOnly : CompanionTurnDelivery()
    data object VoiceFirst : CompanionTurnDelivery()
}

sealed class CompanionTurnEvent {
    data class Accepted(val voiceFirst: Boolean) : CompanionTurnEvent()
    data class AssistantToken(val token: String) : CompanionTurnEvent()
    data class ContextRebuildCompleted(
        val reason: String,
        val result: CompanionRebuildResult,
        val stableMessageCount: Int,
        val compressionThreshold: Int
    ) : CompanionTurnEvent()
    data class Rejected(val reason: CompanionTurnRejectReason, val message: String) : CompanionTurnEvent()
    data class Failed(val message: String) : CompanionTurnEvent()
    data object Completed : CompanionTurnEvent()
}

enum class CompanionTurnRejectReason {
    BlankInput,
    AlreadyGenerating,
    EngineNotReady
}
