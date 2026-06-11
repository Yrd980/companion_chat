package com.companion.chat.engine

import com.companion.chat.data.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ModelRuntimeLifecycle(
    private val scope: CoroutineScope,
    private val modelConfigRepository: ModelConfigRepository,
    private val inferenceEngineFactory: InferenceEngineFactory,
    private val logger: (String) -> Unit
) {
    private var inferenceEngine = inferenceEngineFactory.create(modelConfigRepository.getConfig().runtime)
    private var stateJob: Job? = null

    val engine: InferenceEngine
        get() = inferenceEngine

    val state: StateFlow<InferenceState>
        get() = inferenceEngine.state

    fun collectState(onState: (InferenceState) -> Unit) {
        stateJob?.cancel()
        stateJob = scope.launch {
            inferenceEngine.state.collectLatest(onState)
        }
    }

    suspend fun initialize(
        baseSystemPrompt: String,
        modelPathOverride: String = ""
    ): ModelRuntimeInitializationResult {
        val requestedConfig = modelConfigRepository.getConfig()
        val actualPath = modelPathOverride.ifBlank {
            modelConfigRepository.resolveModelPath(requestedConfig)
        }
        val engineConfig = modelConfigRepository.toEngineConfig(
            systemPrompt = baseSystemPrompt,
            config = requestedConfig
        ).copy(modelPath = actualPath)

        val previousRuntime = inferenceEngine.getCurrentConfig()?.runtime
        if (engineConfig.runtime != previousRuntime) {
            logger("切换模型运行时: $previousRuntime -> ${engineConfig.runtime}")
            inferenceEngine.release()
            inferenceEngine = inferenceEngineFactory.create(engineConfig.runtime)
        }

        inferenceEngine.initialize(engineConfig)
        val actualBackend = inferenceEngine.getCurrentConfig()?.backend
        if (
            actualBackend != null &&
            actualBackend != requestedConfig.backend &&
            requestedConfig.backend != BackendType.CPU
        ) {
            modelConfigRepository.updateConfig(requestedConfig.copy(backend = actualBackend))
            logger("模型后端已同步为实际可用后端: ${requestedConfig.backend} -> $actualBackend")
        }

        return ModelRuntimeInitializationResult(
            requestedConfig = requestedConfig,
            engineConfig = engineConfig,
            actualBackend = actualBackend,
            runtimeSwitched = engineConfig.runtime != previousRuntime
        )
    }

    suspend fun warmUp(messages: List<ChatMessage>): Boolean {
        return inferenceEngine.warmUp(messages)
    }

    fun cancel() {
        inferenceEngine.cancel()
    }

    fun release() {
        stateJob?.cancel()
        inferenceEngine.release()
    }
}

data class ModelRuntimeInitializationResult(
    val requestedConfig: ModelConfig,
    val engineConfig: EngineConfig,
    val actualBackend: BackendType?,
    val runtimeSwitched: Boolean
)
