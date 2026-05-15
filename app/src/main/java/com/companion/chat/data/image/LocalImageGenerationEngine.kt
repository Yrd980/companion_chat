package com.companion.chat.data.image

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class LocalImageGenerationEngine : ImageGenerationEngine {

    private val _state = MutableStateFlow<ImageGenerationState>(ImageGenerationState.Idle)
    override val state: StateFlow<ImageGenerationState> = _state.asStateFlow()

    override suspend fun generate(
        prompt: String,
        config: ImageGenerationConfig,
        purpose: ImageGenerationPurpose
    ): Result<String> {
        return generate(
            request = ImageGenerationRequest(prompt = prompt, purpose = purpose),
            config = config
        )
    }

    override suspend fun generate(
        request: ImageGenerationRequest,
        config: ImageGenerationConfig
    ): Result<String> {
        val modelPath = config.localModelPath.trim()
        if (modelPath.isBlank()) {
            val error = "本地 DreamLite 模型路径未配置"
            _state.value = ImageGenerationState.Error(error)
            return Result.failure(IllegalStateException(error))
        }
        if (!File(modelPath).exists()) {
            val error = "本地 DreamLite 模型不存在: $modelPath"
            _state.value = ImageGenerationState.Error(error)
            return Result.failure(IllegalStateException(error))
        }
        val error = "本地 DreamLite 推理尚未接入"
        _state.value = ImageGenerationState.Error(error)
        return Result.failure(UnsupportedOperationException(error))
    }
}
