package com.companion.chat.engine

import com.companion.chat.data.engine.VoiceOutputConfig
import com.companion.chat.data.engine.VoiceOutputEngine
import com.companion.chat.data.engine.VoiceOutputMode
import com.companion.chat.data.engine.VoiceOutputState
import com.companion.chat.data.role.RoleCardRepository
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
            null
        }

        if (cloneResult?.fallbackToSystemTts == false && !cloneResult.audioUri.isNullOrBlank()) {
            localAudioPlaybackEngine.play(cloneResult.audioUri)
        } else {
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
