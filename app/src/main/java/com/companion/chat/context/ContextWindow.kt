package com.companion.chat.context

import com.companion.chat.data.model.ChatMessage

data class ContextWindow(
    val systemPrompt: String,
    val userPreferences: String,
    val persistentMemoryPrompt: String,
    val memoryPrompt: String,
    val oneTurnMemoryPrompt: String,
    val historySummary: String,
    val recentMessages: List<ChatMessage>,
    val currentMessage: ChatMessage
)
