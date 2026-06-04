package com.companion.chat.ui.settings

import androidx.lifecycle.ViewModel
import com.companion.chat.companion.readiness.CompanionReadinessRepository
import com.companion.chat.companion.readiness.CompanionReadinessSnapshot
import com.companion.chat.context.ContextConfigRepository
import com.companion.chat.engine.BackendType
import com.companion.chat.engine.LocalLmPackageStatus
import com.companion.chat.engine.ModelConfig
import com.companion.chat.engine.ModelConfigRepository
import com.companion.chat.engine.ModelRuntime
import com.companion.chat.engine.image.DreamLiteModelStatus
import com.companion.chat.engine.image.ImageGenerationConfig
import com.companion.chat.engine.image.ImageGenerationConfigRepository
import com.companion.chat.engine.image.ImageGenerationProvider
import com.companion.chat.engine.image.ImageProviderReadiness
import com.companion.chat.engine.image.StableDiffusionModelStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ModelConfigUiState(
    val retainedRounds: Int,
    val modelConfig: ModelConfig,
    val imageConfig: ImageGenerationConfig,
    val dreamLiteModelStatus: DreamLiteModelStatus,
    val stableDiffusionModelStatus: StableDiffusionModelStatus,
    val localLmPackageStatus: LocalLmPackageStatus,
    val imageProviderReadiness: ImageProviderReadiness,
    val readinessSnapshot: CompanionReadinessSnapshot
)

class ModelConfigViewModel(
    private val modelConfigRepository: ModelConfigRepository,
    private val contextConfigRepository: ContextConfigRepository,
    private val imageConfigRepository: ImageGenerationConfigRepository,
    private val readinessRepository: CompanionReadinessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildUiState())
    val uiState: StateFlow<ModelConfigUiState> = _uiState.asStateFlow()

    fun setRuntime(runtime: ModelRuntime) {
        updateModelConfig(_uiState.value.modelConfig.copy(runtime = runtime, modelPath = ""))
    }

    fun setBackend(backend: BackendType) {
        updateModelConfig(_uiState.value.modelConfig.copy(backend = backend))
    }

    fun updateModelPath(path: String) {
        updateModelConfig(_uiState.value.modelConfig.copy(modelPath = path))
    }

    fun updateContextSize(value: String) {
        updateModelConfig(_uiState.value.modelConfig.copy(contextSize = value.toIntOrNull() ?: return))
    }

    fun updateMaxTokens(value: String) {
        updateModelConfig(_uiState.value.modelConfig.copy(maxTokens = value.toIntOrNull() ?: return))
    }

    fun updateTemperature(value: String) {
        updateModelConfig(_uiState.value.modelConfig.copy(temperature = value.toFloatOrNull() ?: return))
    }

    fun updateTopK(value: String) {
        updateModelConfig(_uiState.value.modelConfig.copy(topK = value.toIntOrNull() ?: return))
    }

    fun updateTopP(value: String) {
        updateModelConfig(_uiState.value.modelConfig.copy(topP = value.toFloatOrNull() ?: return))
    }

    fun updateRetainedRounds(rounds: Int) {
        contextConfigRepository.updateRetainedRounds(rounds)
        refresh()
    }

    fun setImageProvider(provider: ImageGenerationProvider) {
        updateImageConfig(_uiState.value.imageConfig.copy(provider = provider))
    }

    fun updateLocalModelPath(path: String) {
        updateImageConfig(_uiState.value.imageConfig.copy(localModelPath = path))
    }

    fun updateLocalWidth(value: String) {
        updateImageConfig(_uiState.value.imageConfig.copy(localWidth = value.toIntOrNull() ?: return))
    }

    fun updateLocalHeight(value: String) {
        updateImageConfig(_uiState.value.imageConfig.copy(localHeight = value.toIntOrNull() ?: return))
    }

    fun updateLocalSteps(value: String) {
        updateImageConfig(_uiState.value.imageConfig.copy(localSteps = value.toIntOrNull() ?: return))
    }

    fun updateLocalCfgScale(value: String) {
        updateImageConfig(_uiState.value.imageConfig.copy(localCfgScale = value.toFloatOrNull() ?: return))
    }

    fun updateLocalSeed(value: String) {
        updateImageConfig(_uiState.value.imageConfig.copy(localSeed = value.toLongOrNull()))
    }

    fun setLocalUseVulkan(enabled: Boolean) {
        updateImageConfig(_uiState.value.imageConfig.copy(localUseVulkan = enabled))
    }

    fun updateImageBaseUrl(baseUrl: String) {
        updateImageConfig(_uiState.value.imageConfig.copy(baseUrl = baseUrl))
    }

    fun updateImageApiKey(apiKey: String) {
        updateImageConfig(_uiState.value.imageConfig.copy(apiKey = apiKey))
    }

    fun updateImageModel(model: String) {
        updateImageConfig(_uiState.value.imageConfig.copy(model = model))
    }

    fun updateRequestTemplate(template: String) {
        updateImageConfig(
            _uiState.value.imageConfig.copy(
                requestTemplate = template.ifBlank { ImageGenerationConfig.DEFAULT_REQUEST_TEMPLATE }
            )
        )
    }

    fun updateResponseImageFieldPath(path: String) {
        updateImageConfig(
            _uiState.value.imageConfig.copy(
                responseImageFieldPath = path.ifBlank { ImageGenerationConfig.DEFAULT_RESPONSE_FIELD_PATH }
            )
        )
    }

    fun updateTimeoutMillis(value: String) {
        updateImageConfig(_uiState.value.imageConfig.copy(timeoutMillis = value.toIntOrNull() ?: return))
    }

    private fun updateModelConfig(config: ModelConfig) {
        modelConfigRepository.updateConfig(config)
        refresh()
    }

    private fun updateImageConfig(config: ImageGenerationConfig) {
        imageConfigRepository.updateConfig(config)
        refresh()
    }

    private fun refresh() {
        _uiState.update { buildUiState() }
    }

    private fun buildUiState(): ModelConfigUiState {
        val modelConfig = modelConfigRepository.getConfig()
        val imageConfig = imageConfigRepository.getConfig()
        return ModelConfigUiState(
            retainedRounds = contextConfigRepository.getSettings().retainedRounds,
            modelConfig = modelConfig,
            imageConfig = imageConfig,
            dreamLiteModelStatus = imageConfigRepository.getDreamLiteModelStatus(imageConfig),
            stableDiffusionModelStatus = imageConfigRepository.getStableDiffusionModelStatus(imageConfig),
            localLmPackageStatus = modelConfigRepository.getLocalLmPackageStatus(modelConfig),
            imageProviderReadiness = imageConfigRepository.getProviderReadiness(imageConfig),
            readinessSnapshot = readinessRepository.getSnapshot()
        )
    }
}
