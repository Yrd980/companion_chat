package com.companion.chat.engine.voice.role

import com.companion.chat.engine.voice.MossTtsNanoModelStatus
import com.companion.chat.engine.voice.VoiceCloneConfigRepository
import com.companion.chat.engine.voice.VoiceCloneEngine
import com.companion.chat.engine.voice.VoiceCloneProvider
import com.companion.chat.engine.voice.VoiceCloneRequest
import com.companion.chat.engine.voice.VoiceCloneResult

class RoleVoiceCloneRouter(
    private val voiceCloneConfigRepository: VoiceCloneConfigRepository,
    private val httpCloneEngine: VoiceCloneEngine,
    private val mossTtsNanoEngine: VoiceCloneEngine
) {
    suspend fun synthesize(
        text: String,
        profile: RoleVoiceProfile
    ): RoleVoiceCloneRouteResult {
        val request = VoiceCloneRequest(
            text = text,
            referenceAudioUri = profile.referenceAudioUri,
            roleId = profile.roleId,
            displayName = profile.displayName
        )
        val failures = mutableListOf<String>()

        for (provider in candidateProviders()) {
            val result = synthesizeWith(provider, request).getOrElse { error ->
                VoiceCloneResult(
                    provider = provider,
                    fallbackToSystemTts = true,
                    message = error.message ?: "${provider.name} 语音克隆失败"
                )
            }

            if (!result.fallbackToSystemTts && !result.audioUri.isNullOrBlank()) {
                return RoleVoiceCloneRouteResult.Generated(result)
            }
            result.message.takeIf { it.isNotBlank() }?.let { failures += it }
        }

        return RoleVoiceCloneRouteResult.FallbackToSystemTts(
            failures.joinToString("；").ifBlank { "未配置可用的角色语音克隆后端" }
        )
    }

    private fun candidateProviders(): List<VoiceCloneProvider> {
        val config = voiceCloneConfigRepository.getConfig()
        return buildList {
            if (config.isHttpCloneConfigured) {
                add(VoiceCloneProvider.HTTP_CLONE)
            }
            if (voiceCloneConfigRepository.getMossModelStatus(config) is MossTtsNanoModelStatus.Ready) {
                add(VoiceCloneProvider.MOSS_TTS_NANO)
            }
        }
    }

    private suspend fun synthesizeWith(
        provider: VoiceCloneProvider,
        request: VoiceCloneRequest
    ): Result<VoiceCloneResult> {
        return when (provider) {
            VoiceCloneProvider.HTTP_CLONE -> httpCloneEngine.synthesize(request)
            VoiceCloneProvider.MOSS_TTS_NANO -> mossTtsNanoEngine.synthesize(request)
            VoiceCloneProvider.SYSTEM_TTS -> Result.success(
                VoiceCloneResult(
                    provider = provider,
                    fallbackToSystemTts = true,
                    message = "使用系统 TTS"
                )
            )
        }
    }
}

sealed class RoleVoiceCloneRouteResult {
    data class Generated(val result: VoiceCloneResult) : RoleVoiceCloneRouteResult()
    data class FallbackToSystemTts(val message: String = "") : RoleVoiceCloneRouteResult()
}
