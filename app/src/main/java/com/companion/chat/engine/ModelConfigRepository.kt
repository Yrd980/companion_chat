package com.companion.chat.engine

import android.content.Context
import java.io.File

data class ModelConfig(
    val runtime: ModelRuntime = ModelRuntime.LLAMA_CPP_GGUF,
    val modelPath: String = "",
    val backend: BackendType = BackendType.CPU,
    val contextSize: Int = DefaultModelConfig.DefaultContextSize,
    val maxTokens: Int = DefaultModelConfig.DefaultMaxTokens,
    val temperature: Float = DefaultModelConfig.DefaultTemperature,
    val topK: Int = DefaultModelConfig.DefaultTopK,
    val topP: Float = DefaultModelConfig.DefaultTopP,
    val cloudBaseUrl: String = "",
    val cloudApiKey: String = "",
    val cloudModelName: String = ""
)

data class LocalLmPackageStatus(
    val runtime: ModelRuntime,
    val modelPath: String,
    val modelFileStatus: LocalLmFileStatus,
    val mmprojPath: String,
    val mmprojFileStatus: LocalLmFileStatus
) {
    val isModelReady: Boolean
        get() = modelFileStatus is LocalLmFileStatus.Ready

    val isMmprojRelevant: Boolean
        get() = runtime == ModelRuntime.LLAMA_CPP_GGUF
}

sealed class LocalLmFileStatus {
    data class Ready(val byteCount: Long) : LocalLmFileStatus()
    data object Missing : LocalLmFileStatus()
    data object Unreadable : LocalLmFileStatus()
    data object Empty : LocalLmFileStatus()
    data object NotRequired : LocalLmFileStatus()
}

class ModelConfigRepository(
    context: Context
) {
    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConfig(): ModelConfig {
        val runtime = sharedPreferences.getString(KEY_RUNTIME, null)
            ?.let { value -> runCatching { ModelRuntime.valueOf(value) }.getOrNull() }
            ?: ModelRuntime.LLAMA_CPP_GGUF
        val backend = sharedPreferences.getString(KEY_BACKEND, null)
            ?.let { value -> runCatching { BackendType.valueOf(value) }.getOrNull() }
            ?: BackendType.CPU
        val modelPath = sharedPreferences.getString(KEY_MODEL_PATH, null)
            ?.trim()
            .orEmpty()

        return ModelConfig(
            runtime = runtime,
            modelPath = modelPath,
            backend = backend,
            contextSize = sharedPreferences.getInt(KEY_CONTEXT_SIZE, DefaultModelConfig.DefaultContextSize),
            maxTokens = sharedPreferences.getInt(KEY_MAX_TOKENS, DefaultModelConfig.DefaultMaxTokens),
            temperature = sharedPreferences.getFloat(KEY_TEMPERATURE, DefaultModelConfig.DefaultTemperature),
            topK = sharedPreferences.getInt(KEY_TOP_K, DefaultModelConfig.DefaultTopK),
            topP = sharedPreferences.getFloat(KEY_TOP_P, DefaultModelConfig.DefaultTopP),
            cloudBaseUrl = sharedPreferences.getString(KEY_CLOUD_BASE_URL, "").orEmpty().trim(),
            cloudApiKey = sharedPreferences.getString(KEY_CLOUD_API_KEY, "").orEmpty().trim(),
            cloudModelName = sharedPreferences.getString(KEY_CLOUD_MODEL_NAME, "").orEmpty().trim()
        ).normalized()
    }

    fun updateConfig(config: ModelConfig) {
        val normalized = config.normalized()
        sharedPreferences.edit()
            .putString(KEY_RUNTIME, normalized.runtime.name)
            .putString(KEY_MODEL_PATH, normalized.modelPath)
            .putString(KEY_BACKEND, normalized.backend.name)
            .putInt(KEY_CONTEXT_SIZE, normalized.contextSize)
            .putInt(KEY_MAX_TOKENS, normalized.maxTokens)
            .putFloat(KEY_TEMPERATURE, normalized.temperature)
            .putInt(KEY_TOP_K, normalized.topK)
            .putFloat(KEY_TOP_P, normalized.topP)
            .putString(KEY_CLOUD_BASE_URL, normalized.cloudBaseUrl)
            .putString(KEY_CLOUD_API_KEY, normalized.cloudApiKey)
            .putString(KEY_CLOUD_MODEL_NAME, normalized.cloudModelName)
            .apply()
    }

    fun resolveModelPath(config: ModelConfig = getConfig()): String {
        if (config.runtime == ModelRuntime.CLOUD_MIMO) return ""
        val explicitPath = config.modelPath.trim()
        if (explicitPath.isNotBlank()) return explicitPath

        val fileName = when (config.runtime) {
            ModelRuntime.LLAMA_CPP_GGUF -> DefaultModelConfig.GgufModelFileName
            ModelRuntime.LITERT_LM -> DefaultModelConfig.LiteRtModelFileName
            ModelRuntime.CLOUD_MIMO -> return ""
        }
        val externalDir = appContext.getExternalFilesDir(DefaultModelConfig.ExternalModelsDir)
        return if (externalDir != null) {
            File(externalDir, fileName).absolutePath
        } else {
            File(File(appContext.filesDir, DefaultModelConfig.ExternalModelsDir), fileName).absolutePath
        }
    }

    fun resolveMmprojPath(): String {
        val externalDir = appContext.getExternalFilesDir(DefaultModelConfig.ExternalModelsDir)
        return if (externalDir != null) {
            File(externalDir, DefaultModelConfig.GgufMmprojFileName).absolutePath
        } else {
            File(File(appContext.filesDir, DefaultModelConfig.ExternalModelsDir), DefaultModelConfig.GgufMmprojFileName).absolutePath
        }
    }

    fun getLocalLmPackageStatus(config: ModelConfig = getConfig()): LocalLmPackageStatus {
        val normalized = config.normalized()
        if (normalized.runtime == ModelRuntime.CLOUD_MIMO) {
            return LocalLmPackageStatus(
                runtime = normalized.runtime,
                modelPath = "",
                modelFileStatus = LocalLmFileStatus.NotRequired,
                mmprojPath = "",
                mmprojFileStatus = LocalLmFileStatus.NotRequired
            )
        }
        val modelPath = resolveModelPath(normalized)
        val mmprojPath = if (normalized.runtime == ModelRuntime.LLAMA_CPP_GGUF) {
            resolveMmprojPath()
        } else {
            ""
        }
        return LocalLmPackageStatus(
            runtime = normalized.runtime,
            modelPath = modelPath,
            modelFileStatus = inspectModelFile(modelPath),
            mmprojPath = mmprojPath,
            mmprojFileStatus = if (mmprojPath.isBlank()) {
                LocalLmFileStatus.NotRequired
            } else {
                inspectModelFile(mmprojPath)
            }
        )
    }

    fun toEngineConfig(
        systemPrompt: String,
        config: ModelConfig = getConfig()
    ): EngineConfig {
        val normalized = config.normalized()
        return EngineConfig(
            modelPath = if (normalized.runtime == ModelRuntime.CLOUD_MIMO) "" else resolveModelPath(normalized),
            mmprojPath = if (normalized.runtime == ModelRuntime.LLAMA_CPP_GGUF) resolveMmprojPath() else "",
            runtime = normalized.runtime,
            backend = normalized.backend,
            contextSize = normalized.contextSize,
            maxTokens = normalized.maxTokens,
            temperature = normalized.temperature,
            topK = normalized.topK,
            topP = normalized.topP,
            systemPrompt = systemPrompt,
            cloudBaseUrl = normalized.cloudBaseUrl,
            cloudApiKey = normalized.cloudApiKey,
            cloudModelName = normalized.cloudModelName
        )
    }

    private fun ModelConfig.normalized(): ModelConfig {
        return copy(
            modelPath = modelPath.trim(),
            contextSize = contextSize.coerceIn(512, 32768),
            maxTokens = maxTokens.coerceIn(1, 1024),
            temperature = temperature.coerceIn(0.0f, 2.0f),
            topK = topK.coerceIn(1, 200),
            topP = topP.coerceIn(0.01f, 1.0f)
        )
    }

    private fun inspectModelFile(path: String): LocalLmFileStatus {
        val file = File(path)
        return when {
            !file.exists() -> LocalLmFileStatus.Missing
            !file.canRead() -> LocalLmFileStatus.Unreadable
            file.length() <= 0L -> LocalLmFileStatus.Empty
            else -> LocalLmFileStatus.Ready(file.length())
        }
    }

    companion object {
        private const val PREFS_NAME = "model_config"
        private const val KEY_RUNTIME = "runtime"
        private const val KEY_MODEL_PATH = "model_path"
        private const val KEY_BACKEND = "backend"
        private const val KEY_CONTEXT_SIZE = "context_size"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_TOP_K = "top_k"
        private const val KEY_TOP_P = "top_p"
        private const val KEY_CLOUD_BASE_URL = "cloud_base_url"
        private const val KEY_CLOUD_API_KEY = "cloud_api_key"
        private const val KEY_CLOUD_MODEL_NAME = "cloud_model_name"
    }
}
