package com.companion.chat.engine

import android.util.Log
import com.companion.chat.engine.voice.role.RoleVoiceCloneRouteResult
import com.companion.chat.engine.voice.role.RoleVoiceCloneRouter
import com.companion.chat.engine.voice.role.RoleVoiceProfileResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoleAwareVoiceOutputEngine(
    private val fallbackEngine: VoiceOutputEngine,
    private val profileResolver: RoleVoiceProfileResolver,
    private val cloneRouter: RoleVoiceCloneRouter? = null,
    private val localAudioPlaybackEngine: GeneratedAudioPlayer? = null
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
        val profile = profileResolver.resolve(config)
        val roleConfig = profile.toOutputConfig()

        if (roleConfig.mode != VoiceOutputMode.CLONE || cloneRouter == null || localAudioPlaybackEngine == null) {
            fallbackEngine.speak(text, roleConfig.copy(mode = VoiceOutputMode.SYSTEM_TTS))
            return
        }

        when (val cloneResult = cloneRouter.synthesize(text, profile)) {
            is RoleVoiceCloneRouteResult.Generated -> {
                safeLog("语音克隆成功，播放生成音频: ${cloneResult.result.message}")
                localAudioPlaybackEngine.play(cloneResult.result.audioUri.orEmpty())
            }
            is RoleVoiceCloneRouteResult.FallbackToSystemTts -> {
                safeLog("语音克隆不可用，回退系统 TTS: ${cloneResult.message}", warning = true)
                fallbackEngine.speak(text, roleConfig.copy(mode = VoiceOutputMode.SYSTEM_TTS))
            }
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
