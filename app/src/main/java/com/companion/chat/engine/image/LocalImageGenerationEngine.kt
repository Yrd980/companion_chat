package com.companion.chat.engine.image

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LocalImageGenerationEngine(
    context: Context? = null
) : ImageGenerationEngine {

    private val imageFileStore = context?.let { ImageFileStore(it) }
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
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext when (config.provider) {
            ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP -> generateStableDiffusion(request, config)
            else -> generateDreamLite(config)
        }
    }

    private fun generateDreamLite(config: ImageGenerationConfig): Result<String> {
        val providerConfig = config.localProviderConfig(ImageGenerationProvider.LOCAL_DREAMLITE)
        when (val status = DreamLiteModelPackage.inspect(providerConfig.modelPath)) {
            is DreamLiteModelStatus.Ready -> Unit
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
                val error = "DreamLite 模型包文件缺失：${status.fileNames.joinToString()}"
                _state.value = ImageGenerationState.Error(error)
                return Result.failure(IllegalStateException(error))
            }
        }
        val error = "DreamLite 模型包已就绪，但 Android 端真实推理运行时尚未接入"
        _state.value = ImageGenerationState.Error(error)
        return Result.failure(UnsupportedOperationException(error))
    }

    private fun generateStableDiffusion(
        request: ImageGenerationRequest,
        config: ImageGenerationConfig
    ): Result<String> {
        val store = imageFileStore
        if (store == null) {
            val error = "本地 Stable Diffusion 需要 Android Context 才能保存图片"
            _state.value = ImageGenerationState.Error(error)
            return Result.failure(IllegalStateException(error))
        }
        if (request.prompt.isBlank()) {
            val error = "本地 Stable Diffusion 提示词不能为空"
            _state.value = ImageGenerationState.Error(error)
            return Result.failure(IllegalArgumentException(error))
        }

        val providerConfig = config.localProviderConfig(ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP)
        val runtimeConfig = when (val status = StableDiffusionModelPackage.inspect(providerConfig.modelPath)) {
            is StableDiffusionModelStatus.Ready -> status.config
            StableDiffusionModelStatus.DirectoryNotConfigured -> {
                val error = "Stable Diffusion 模型目录未配置"
                _state.value = ImageGenerationState.Error(error)
                return Result.failure(IllegalStateException(error))
            }
            is StableDiffusionModelStatus.InvalidConfig -> {
                val error = "Stable Diffusion 配置无效：${status.message}"
                _state.value = ImageGenerationState.Error(error)
                return Result.failure(IllegalStateException(error))
            }
            is StableDiffusionModelStatus.MissingFiles -> {
                val error = "Stable Diffusion 模型文件缺失：${status.fileNames.joinToString()}"
                _state.value = ImageGenerationState.Error(error)
                return Result.failure(IllegalStateException(error))
            }
        }

        _state.value = ImageGenerationState.Generating
        return runCatching {
            val pngBytes = StableDiffusionNative.generateTxt2ImgPng(
                modelPath = runtimeConfig.modelPath,
                vaePath = runtimeConfig.vaePath,
                taesdPath = runtimeConfig.taesdPath,
                loraPaths = runtimeConfig.loraPaths.toTypedArray(),
                prompt = request.prompt,
                negativePrompt = request.negativePrompt,
                width = providerConfig.width.takeIf { it > 0 } ?: runtimeConfig.defaultWidth,
                height = providerConfig.height.takeIf { it > 0 } ?: runtimeConfig.defaultHeight,
                steps = stableDiffusionSteps(request, providerConfig, runtimeConfig),
                cfgScale = providerConfig.cfgScale.takeIf { it >= 0f } ?: runtimeConfig.defaultCfgScale,
                seed = stableDiffusionSeed(request, providerConfig, runtimeConfig),
                useVulkan = providerConfig.useVulkan && runtimeConfig.useVulkan
            )
            val uri = store.saveBytes(pngBytes, request.purpose)
            _state.value = ImageGenerationState.Success(uri)
            uri
        }.onFailure { error ->
            _state.value = ImageGenerationState.Error(error.message ?: "本地 Stable Diffusion 出图失败")
        }
    }

    internal fun stableDiffusionSteps(
        request: ImageGenerationRequest,
        config: LocalImageProviderConfig,
        runtimeConfig: StableDiffusionRuntimeConfig
    ): Int {
        return when {
            request.steps > 0 -> request.steps
            config.steps > 0 -> config.steps
            else -> runtimeConfig.defaultSteps
        }.coerceIn(1, 50)
    }

    internal fun stableDiffusionSeed(
        request: ImageGenerationRequest,
        config: LocalImageProviderConfig,
        runtimeConfig: StableDiffusionRuntimeConfig
    ): Long {
        return request.seed ?: config.seed ?: runtimeConfig.defaultSeed ?: -1L
    }
}
