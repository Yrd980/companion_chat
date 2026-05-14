package com.companion.chat.engine

import com.companion.chat.data.engine.VoiceOutputConfig
import com.companion.chat.data.engine.VoiceOutputEngine
import com.companion.chat.data.engine.VoiceOutputMode
import com.companion.chat.data.engine.VoiceOutputState
import com.companion.chat.data.role.RoleCardRepository
import kotlinx.coroutines.flow.Flow

class RoleAwareVoiceOutputEngine(
    private val fallbackEngine: VoiceOutputEngine,
    private val roleCardRepository: RoleCardRepository
) : VoiceOutputEngine {

    override val state: Flow<VoiceOutputState> = fallbackEngine.state

    override suspend fun speak(text: String, config: VoiceOutputConfig) {
        val activeRole = roleCardRepository.getActiveRoleCard()
        val roleConfig = activeRole?.let {
            VoiceOutputConfig(
                mode = runCatching { VoiceOutputMode.valueOf(it.voiceMode) }
                    .getOrDefault(VoiceOutputMode.SYSTEM_TTS),
                referenceAudioUri = it.voiceProfileUri,
                displayName = it.voiceDisplayName
            )
        } ?: config

        // Voice cloning is intentionally an extension point in stage 6.
        // Until a clone backend is bound, route every mode through Android TTS.
        fallbackEngine.speak(text, roleConfig.copy(mode = VoiceOutputMode.SYSTEM_TTS))
    }

    override fun stop() {
        fallbackEngine.stop()
    }

    override fun release() {
        fallbackEngine.release()
    }
}
