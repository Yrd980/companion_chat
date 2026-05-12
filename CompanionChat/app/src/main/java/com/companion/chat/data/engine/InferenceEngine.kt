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
    val systemPrompt: String = "",
    val contextSize: Int = 4096,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f
)

object DefaultModelConfig {
    const val ModelFileName = "Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-Q4_K_P.gguf"
    const val ExternalModelsDir = "models"
    const val DefaultSystemPrompt = "你是一个友善的AI助手，请用中文回答用户的问题。默认简洁回答，除非用户明确要求详细展开。"

    const val ContextSize = 2048
    const val MaxTokens = 512
    const val Temperature = 0.7f
    const val TopK = 40
    const val TopP = 0.95f
    const val MaxPromptMessages = 6
}

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
