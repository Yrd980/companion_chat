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
        val status = modelConfigRepository.getLocalLmPackageStatus(config)
        return if (status.isModelReady) {
            CapabilityReadiness(
                capability = CompanionCapability.LLM,
                level = CompanionReadinessLevel.READY,
                provider = config.runtime.displayName(),
                summary = "文本模型包已就绪",
                detail = status.modelPath
            )
        } else {
            CapabilityReadiness(
                capability = CompanionCapability.LLM,
                level = CompanionReadinessLevel.NOT_READY,
                provider = config.runtime.displayName(),
                summary = "文本模型${status.modelFileStatus.displayName()}",
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
                        provider = "本地 SenseVoice",
                        summary = "语音识别模型已就绪",
                        detail = config.localSenseVoiceModelDirectory
                    )
                } else {
                    CapabilityReadiness(
                        capability = CompanionCapability.ASR,
                        level = CompanionReadinessLevel.NOT_READY,
                        provider = "本地 SenseVoice",
                        summary = status.displayName(),
                        detail = config.localSenseVoiceModelDirectory
                    )
                }
            }
            VoiceInputBackend.CLOUD_HTTP_ASR -> {
                val cloudConfig = cloudAsrConfigRepository.getConfig()
                val endpointError = cloudConfig.baseUrl.endpointError("云 ASR")
                if (!cloudConfig.isConfigured) {
                    CapabilityReadiness(
                        capability = CompanionCapability.ASR,
                        level = CompanionReadinessLevel.NOT_READY,
                        provider = "云 HTTP ASR",
                        summary = "云 ASR Base URL 未配置"
                    )
                } else if (endpointError == null) {
                    CapabilityReadiness(
                        capability = CompanionCapability.ASR,
                        level = CompanionReadinessLevel.READY,
                        provider = "云 HTTP ASR",
                        summary = "云端语音识别已配置",
                        detail = cloudConfig.baseUrl
                    )
                } else {
                    CapabilityReadiness(
                        capability = CompanionCapability.ASR,
                        level = CompanionReadinessLevel.NOT_READY,
                        provider = "云 HTTP ASR",
                        summary = endpointError.message ?: "云 ASR endpoint 不可用",
                        detail = cloudConfig.baseUrl
                    )
                }
            }
        }
    }

    private fun ttsReadiness(): CapabilityReadiness {
        val config = voiceCloneConfigRepository.getConfig()
        val mossStatus = voiceCloneConfigRepository.getMossModelStatus(config)
        val httpCloneEndpointError = config.httpCloneBaseUrl.endpointError("HTTP 语音克隆")
        return when {
            config.isHttpCloneConfigured && httpCloneEndpointError == null -> CapabilityReadiness(
                capability = CompanionCapability.TTS,
                level = CompanionReadinessLevel.READY,
                provider = "HTTP 克隆 + 系统 TTS",
                summary = "HTTP 语音克隆已配置",
                detail = config.httpCloneBaseUrl
            )
            mossStatus == MossTtsNanoModelStatus.Ready -> CapabilityReadiness(
                capability = CompanionCapability.TTS,
                level = CompanionReadinessLevel.READY,
                provider = "MOSS 本地克隆 + 系统 TTS",
                summary = "MOSS 语音克隆模型已就绪",
                detail = config.mossModelDirectory
            )
            config.isHttpCloneConfigured -> CapabilityReadiness(
                capability = CompanionCapability.TTS,
                level = CompanionReadinessLevel.DEGRADED,
                provider = "系统 TTS",
                summary = "HTTP 克隆 endpoint 不可用，系统 TTS 可回退",
                detail = httpCloneEndpointError?.message.orEmpty()
            )
            else -> CapabilityReadiness(
                capability = CompanionCapability.TTS,
                level = CompanionReadinessLevel.DEGRADED,
                provider = "系统 TTS",
                summary = "克隆未就绪，系统 TTS 可回退",
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
    }
}

private fun LocalLmFileStatus.displayName(): String {
    return when (this) {
        is LocalLmFileStatus.Ready -> "已就绪"
        LocalLmFileStatus.Missing -> "缺失"
        LocalLmFileStatus.Unreadable -> "不可读取"
        LocalLmFileStatus.Empty -> "文件为空"
        LocalLmFileStatus.NotRequired -> "不需要"
    }
}

private fun LocalSenseVoiceModelStatus.displayName(): String {
    return when (this) {
        LocalSenseVoiceModelStatus.Ready -> "语音识别模型已就绪"
        LocalSenseVoiceModelStatus.DirectoryNotConfigured -> "本地 SenseVoice 模型未配置"
        is LocalSenseVoiceModelStatus.MissingFiles -> "语音识别文件缺失：${fileNames.joinToString()}"
    }
}

private fun MossTtsNanoModelStatus.displayName(): String {
    return when (this) {
        MossTtsNanoModelStatus.Ready -> "MOSS 语音克隆模型已就绪"
        MossTtsNanoModelStatus.DirectoryNotConfigured -> "moss-tts-nano 模型未配置"
        is MossTtsNanoModelStatus.InvalidConfig -> "MOSS 配置无效：$message"
        is MossTtsNanoModelStatus.MissingFiles -> "MOSS 文件缺失：${fileNames.joinToString()}"
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
        com.companion.chat.engine.image.ImageGenerationProvider.HTTP -> "HTTP 图片生成"
        com.companion.chat.engine.image.ImageGenerationProvider.LOCAL_DREAMLITE -> "本地 DreamLite"
        com.companion.chat.engine.image.ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP -> {
            "本地 Stable Diffusion"
        }
    }
}
