package com.companion.chat.data.engine

import com.companion.chat.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

enum class BackendType {
    CPU,
    GPU
}

data class EngineConfig(
    val modelPath: String,
    val backend: BackendType = BackendType.CPU,
    val maxTokens: Int = 2048,
    val systemPrompt: String = ""
)

sealed class InferenceState {
    data object Idle : InferenceState()
    data object Initializing : InferenceState()
    data object Ready : InferenceState()
    data class Generating(val partialText: String = "") : InferenceState()
    data class Error(val message: String) : InferenceState()
}

interface InferenceEngine {
    val state: Flow<InferenceState>

    suspend fun initialize(config: EngineConfig)

    fun sendMessageStream(
        messages: List<ChatMessage>
    ): Flow<String>

    fun cancel()

    fun release()
}
