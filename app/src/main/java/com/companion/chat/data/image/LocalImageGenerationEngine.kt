package com.companion.chat.data.image

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        when (val status = DreamLiteModelPackage.inspect(config.localModelPath)) {
            DreamLiteModelStatus.Ready -> Unit
            DreamLiteModelStatus.DirectoryNotConfigured -> {
                val error = "DreamLite 模型目录未配置，已完成端侧接入框架"
                _state.value = ImageGenerationState.Error(error)
                return Result.failure(IllegalStateException(error))
            }
            is DreamLiteModelStatus.InvalidConfig -> {
                val error = "DreamLite 配置无效：${status.message}"
                _state.value = ImageGenerationState.Error(error)
                return Result.failure(IllegalStateException(error))
            }
            is DreamLiteModelStatus.MissingFiles -> {
                val error = "DreamLite 模型尚未准备，已完成端侧接入框架。缺失：${status.fileNames.joinToString()}"
                _state.value = ImageGenerationState.Error(error)
                return Result.failure(IllegalStateException(error))
            }
        }
        val error = "DreamLite 模型尚未准备，已完成端侧接入框架。等待官方权重/端侧包后启用真实出图"
        _state.value = ImageGenerationState.Error(error)
        return Result.failure(UnsupportedOperationException(error))
    }
}
