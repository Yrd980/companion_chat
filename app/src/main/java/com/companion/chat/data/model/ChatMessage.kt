package com.companion.chat.data.model

import android.net.Uri
import java.util.UUID

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

const val DEFAULT_SESSION_TITLE = "New Chat"
const val DEFAULT_WELCOME_MESSAGE =
    "Hi, I am your AI companion. Tap the microphone to start a voice chat, or type a message."

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val images: List<Uri> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

data class ConversationSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = DEFAULT_SESSION_TITLE,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

fun createWelcomeMessage() = ChatMessage(
    role = MessageRole.ASSISTANT,
    content = DEFAULT_WELCOME_MESSAGE
)

fun createDefaultSession() = ConversationSession(
    title = DEFAULT_SESSION_TITLE,
    messages = listOf(createWelcomeMessage())
)
