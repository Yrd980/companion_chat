package com.companion.chat.engine

import android.util.Log
import com.companion.chat.engine.VoiceOutputConfig
import com.companion.chat.engine.VoiceOutputEngine
import com.companion.chat.engine.VoiceOutputMode
import com.companion.chat.engine.VoiceOutputState
import com.companion.chat.identity.RoleCardRepository
import com.companion.chat.data.voice.VoiceCloneEngine
import com.companion.chat.data.voice.VoiceCloneRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoleAwareVoiceOutputEngine(
    private val fallbackEngine: VoiceOutputEngine,
    private val roleCardRepository: RoleCardRepository?,
    private val cloneEngine: VoiceCloneEngine? = null,
    private val localAudioPlaybackEngine: GeneratedAudioPlayer? = null,
    private val activeRoleConfigProvider: (suspend () -> VoiceOutputConfig?)? = null
) : VoiceOutputEngine {
    private companion object {
        const val TAG = "RoleAwareVoiceOutput"

        fun safeLog(message: String, warning: Boolean = false) {
            runCatching {
                if (warning) {
                    Log.w(TAG, message)
                } else {
                    Log.i(TAG, message)
                }
            }
        }
    }

    override val state: Flow<VoiceOutputState> = if (localAudioPlaybackEngine == null) {
        fallbackEngine.state
    } else {
        fallbackEngine.state.combine(localAudioPlaybackEngine.state) { fallbackState, playbackState ->
            when {
                playbackState is VoiceOutputState.Error -> playbackState
                fallbackState is VoiceOutputState.Error -> fallbackState
                playbackState is VoiceOutputState.Speaking -> playbackState
                fallbackState is VoiceOutputState.Speaking -> fallbackState
                else -> VoiceOutputState.Idle
            }
        }
    }

    override suspend fun speak(text: String, config: VoiceOutputConfig) {
        val roleConfig = activeRoleConfigProvider?.invoke()
            ?: roleCardRepository?.getActiveRoleCard()?.let {
                VoiceOutputConfig(
                    mode = runCatching { VoiceOutputMode.valueOf(it.voiceMode) }
                        .getOrDefault(VoiceOutputMode.SYSTEM_TTS),
                    referenceAudioUri = it.voiceProfileUri,
                    displayName = it.voiceDisplayName
                )
            }
            ?: config

        if (roleConfig.mode != VoiceOutputMode.CLONE || cloneEngine == null || localAudioPlaybackEngine == null) {
            fallbackEngine.speak(text, roleConfig.copy(mode = VoiceOutputMode.SYSTEM_TTS))
            return
        }

        val cloneResult = cloneEngine.synthesize(
            VoiceCloneRequest(
                text = text,
                referenceAudioUri = roleConfig.referenceAudioUri,
                displayName = roleConfig.displayName
            )
        ).getOrElse {
            safeLog("语音克隆异常，回退系统 TTS: ${it.message}", warning = true)
            null
        }

        if (cloneResult?.fallbackToSystemTts == false && !cloneResult.audioUri.isNullOrBlank()) {
            safeLog("语音克隆成功，播放生成音频: ${cloneResult.message}")
            localAudioPlaybackEngine.play(cloneResult.audioUri)
        } else {
            safeLog("语音克隆不可用，回退系统 TTS: ${cloneResult?.message.orEmpty()}")
            fallbackEngine.speak(text, roleConfig.copy(mode = VoiceOutputMode.SYSTEM_TTS))
        }
    }

    override fun stop() {
        localAudioPlaybackEngine?.stop()
        fallbackEngine.stop()
    }

    override fun release() {
        localAudioPlaybackEngine?.release()
        fallbackEngine.release()
    }
}
