package com.companion.chat.companion.readiness

import com.companion.chat.engine.LocalLmFileStatus
import com.companion.chat.engine.ModelConfigRepository
import com.companion.chat.engine.ModelRuntime
import com.companion.chat.engine.NetworkEndpointPolicy
import com.companion.chat.engine.image.ImageGenerationConfigRepository
import com.companion.chat.engine.image.ImageProviderReadinessLevel
import com.companion.chat.engine.voice.CloudAsrConfigRepository
import com.companion.chat.engine.voice.LocalSenseVoiceModelStatus
import com.companion.chat.engine.voice.MossTtsNanoModelStatus
import com.companion.chat.engine.voice.VoiceCloneConfigRepository
import com.companion.chat.engine.voice.VoiceInputBackend
import com.companion.chat.engine.voice.VoiceInputConfigRepository

enum class CompanionCapability {
    LLM,
    ASR,
    TTS,
    IMAGE
}

enum class CompanionReadinessLevel {
    READY,
    DEGRADED,
    NOT_READY
}

data class CapabilityReadiness(
    val capability: CompanionCapability,
    val level: CompanionReadinessLevel,
    val provider: String,
    val summary: String,
    val detail: String = ""
) {
    val isUsable: Boolean
        get() = level != CompanionReadinessLevel.NOT_READY
}

data class CompanionReadinessSnapshot(
    val llm: CapabilityReadiness,
    val asr: CapabilityReadiness,
    val tts: CapabilityReadiness,
    val image: CapabilityReadiness
) {
    val capabilities: List<CapabilityReadiness>
        get() = listOf(llm, asr, tts, image)

    val isReadyForVoiceFirstTurn: Boolean
        get() = llm.level == CompanionReadinessLevel.READY && asr.isUsable && tts.isUsable
}

class CompanionReadinessRepository(
    private val modelConfigRepository: ModelConfigRepository,
    private val voiceInputConfigRepository: VoiceInputConfigRepository,
    private val cloudAsrConfigRepository: CloudAsrConfigRepository,
    private val voiceCloneConfigRepository: VoiceCloneConfigRepository,
    private val imageGenerationConfigRepository: ImageGenerationConfigRepository
) {
    fun getSnapshot(): CompanionReadinessSnapshot {
        return CompanionReadinessSnapshot(
            llm = llmReadiness(),
            asr = asrReadiness(),
            tts = ttsReadiness(),
            image = imageReadiness()
        )
    }

    private fun llmReadiness(): CapabilityReadiness {
        val config = modelConfigRepository.getConfig()
        if (config.runtime == ModelRuntime.CLOUD_MIMO) {
            val hasApiKey = config.cloudApiKey.isNotBlank()
            val baseUrl = config.cloudBaseUrl.ifBlank { "https://token-plan-cn.xiaomimimo.com/v1" }
            return if (hasApiKey) {
                CapabilityReadiness(
                    capability = CompanionCapability.LLM,
                    level = CompanionReadinessLevel.READY,
                    provider = "Xiaomi MiMo Cloud",
                    summary = "Cloud LLM is configured",
                    detail = "$baseUrl (${config.cloudModelName.ifBlank { "mimo-v2.5-pro" }})"
                )
            } else {
                CapabilityReadiness(
                    capability = CompanionCapability.LLM,
                    level = CompanionReadinessLevel.NOT_READY,
                    provider = "Xiaomi MiMo Cloud",
                    summary = "Cloud API key is not configured"
                )
            }
        }
        val status = modelConfigRepository.getLocalLmPackageStatus(config)
        return if (status.isModelReady) {
            CapabilityReadiness(
                capability = CompanionCapability.LLM,
                level = CompanionReadinessLevel.READY,
                provider = config.runtime.displayName(),
                summary = "Text model package is ready",
                detail = status.modelPath
            )
        } else {
            CapabilityReadiness(
                capability = CompanionCapability.LLM,
                level = CompanionReadinessLevel.NOT_READY,
                provider = config.runtime.displayName(),
                summary = "Text model ${status.modelFileStatus.displayName()}",
                detail = status.modelPath
            )
        }
    }

    private fun asrReadiness(): CapabilityReadiness {
        val config = voiceInputConfigRepository.getConfig()
        return when (config.backend) {
            VoiceInputBackend.LOCAL_SENSEVOICE -> {
                val status = voiceInputConfigRepository.getLocalSenseVoiceModelStatus(config)
                if (status == LocalSenseVoiceModelStatus.Ready) {
                    CapabilityReadiness(
                        capability = CompanionCapability.ASR,
                        level = CompanionReadinessLevel.READY,
                        provider = "Local SenseVoice",
                        summary = "Speech recognition model is ready",
                        detail = config.localSenseVoiceModelDirectory
                    )
                } else {
                    CapabilityReadiness(
                        capability = CompanionCapability.ASR,
                        level = CompanionReadinessLevel.NOT_READY,
                        provider = "Local SenseVoice",
                        summary = status.displayName(),
                        detail = config.localSenseVoiceModelDirectory
                    )
                }
            }
            VoiceInputBackend.CLOUD_HTTP_ASR -> {
                val cloudConfig = cloudAsrConfigRepository.getConfig()
                val endpointError = cloudConfig.baseUrl.endpointError("Cloud ASR")
                if (!cloudConfig.isConfigured) {
                    CapabilityReadiness(
                        capability = CompanionCapability.ASR,
                        level = CompanionReadinessLevel.NOT_READY,
                        provider = "Cloud HTTP ASR",
                        summary = "Cloud ASR base URL is not configured"
                    )
                } else if (endpointError == null) {
                    CapabilityReadiness(
                        capability = CompanionCapability.ASR,
                        level = CompanionReadinessLevel.READY,
                        provider = "Cloud HTTP ASR",
                        summary = "Cloud speech recognition is configured",
                        detail = cloudConfig.baseUrl
                    )
                } else {
                    CapabilityReadiness(
                        capability = CompanionCapability.ASR,
                        level = CompanionReadinessLevel.NOT_READY,
                        provider = "Cloud HTTP ASR",
                        summary = endpointError.message ?: "Cloud ASR endpoint is unavailable",
                        detail = cloudConfig.baseUrl
                    )
                }
            }
        }
    }

    private fun ttsReadiness(): CapabilityReadiness {
        val config = voiceCloneConfigRepository.getConfig()
        val mossStatus = voiceCloneConfigRepository.getMossModelStatus(config)
        val httpCloneEndpointError = config.httpCloneBaseUrl.endpointError("HTTP voice clone")
        return when {
            config.isHttpCloneConfigured && httpCloneEndpointError == null -> CapabilityReadiness(
                capability = CompanionCapability.TTS,
                level = CompanionReadinessLevel.READY,
                provider = "HTTP clone + system TTS",
                summary = "HTTP voice clone is configured",
                detail = config.httpCloneBaseUrl
            )
            mossStatus == MossTtsNanoModelStatus.Ready -> CapabilityReadiness(
                capability = CompanionCapability.TTS,
                level = CompanionReadinessLevel.READY,
                provider = "Local MOSS clone + system TTS",
                summary = "MOSS voice clone model is ready",
                detail = config.mossModelDirectory
            )
            config.isHttpCloneConfigured -> CapabilityReadiness(
                capability = CompanionCapability.TTS,
                level = CompanionReadinessLevel.DEGRADED,
                provider = "System TTS",
                summary = "HTTP clone endpoint is unavailable. System TTS can fall back.",
                detail = httpCloneEndpointError?.message.orEmpty()
            )
            else -> CapabilityReadiness(
                capability = CompanionCapability.TTS,
                level = CompanionReadinessLevel.DEGRADED,
                provider = "System TTS",
                summary = "Voice clone is not ready. System TTS can fall back.",
                detail = mossStatus.displayName()
            )
        }
    }

    private fun imageReadiness(): CapabilityReadiness {
        val readiness = imageGenerationConfigRepository.getProviderReadiness()
        return CapabilityReadiness(
            capability = CompanionCapability.IMAGE,
            level = when (readiness.level) {
                ImageProviderReadinessLevel.READY -> CompanionReadinessLevel.READY
                ImageProviderReadinessLevel.NOT_READY -> CompanionReadinessLevel.NOT_READY
            },
            provider = readiness.provider.displayName(),
            summary = readiness.summary,
            detail = readiness.detail
        )
    }
}

private fun ModelRuntime.displayName(): String {
    return when (this) {
        ModelRuntime.LLAMA_CPP_GGUF -> "llama.cpp GGUF"
        ModelRuntime.LITERT_LM -> "LiteRT-LM"
        ModelRuntime.CLOUD_MIMO -> "Xiaomi MiMo Cloud"
    }
}

private fun LocalLmFileStatus.displayName(): String {
    return when (this) {
        is LocalLmFileStatus.Ready -> "is ready"
        LocalLmFileStatus.Missing -> "is missing"
        LocalLmFileStatus.Unreadable -> "is unreadable"
        LocalLmFileStatus.Empty -> "is empty"
        LocalLmFileStatus.NotRequired -> "is not required"
    }
}

private fun LocalSenseVoiceModelStatus.displayName(): String {
    return when (this) {
        LocalSenseVoiceModelStatus.Ready -> "Speech recognition model is ready"
        LocalSenseVoiceModelStatus.DirectoryNotConfigured -> "Local SenseVoice model is not configured"
        is LocalSenseVoiceModelStatus.MissingFiles -> "Speech recognition files are missing: ${fileNames.joinToString()}"
    }
}

private fun MossTtsNanoModelStatus.displayName(): String {
    return when (this) {
        MossTtsNanoModelStatus.Ready -> "MOSS voice clone model is ready"
        MossTtsNanoModelStatus.DirectoryNotConfigured -> "moss-tts-nano model is not configured"
        is MossTtsNanoModelStatus.InvalidConfig -> "MOSS config is invalid: $message"
        is MossTtsNanoModelStatus.MissingFiles -> "MOSS files are missing: ${fileNames.joinToString()}"
    }
}

private fun String.endpointError(label: String): Throwable? {
    if (isBlank()) return null
    return runCatching {
        NetworkEndpointPolicy.requireHttpsOrLoopback(this, label)
    }.exceptionOrNull()
}

private fun com.companion.chat.engine.image.ImageGenerationProvider.displayName(): String {
    return when (this) {
        com.companion.chat.engine.image.ImageGenerationProvider.HTTP -> "HTTP image generation"
        com.companion.chat.engine.image.ImageGenerationProvider.LOCAL_DREAMLITE -> "Local DreamLite"
        com.companion.chat.engine.image.ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP -> {
            "Local Stable Diffusion"
        }
    }
}
