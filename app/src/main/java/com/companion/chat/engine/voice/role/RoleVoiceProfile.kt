package com.companion.chat.engine.voice.role

import com.companion.chat.data.local.entity.RoleCard
import com.companion.chat.engine.VoiceOutputConfig
import com.companion.chat.engine.VoiceOutputMode
import com.companion.chat.identity.RoleCardRepository

data class RoleVoiceProfile(
    val mode: VoiceOutputMode = VoiceOutputMode.SYSTEM_TTS,
    val referenceAudioUri: String = "",
    val displayName: String = "",
    val roleId: String = ""
) {
    fun toOutputConfig(): VoiceOutputConfig {
        return VoiceOutputConfig(
            mode = mode,
            referenceAudioUri = referenceAudioUri,
            displayName = displayName
        )
    }
}

class RoleVoiceProfileResolver(
    private val roleCardRepository: RoleCardRepository?,
    private val activeRoleConfigProvider: (suspend () -> VoiceOutputConfig?)? = null
) {
    suspend fun resolve(requestedConfig: VoiceOutputConfig): RoleVoiceProfile {
        return requestedConfig
            .takeIf { it.isExplicit() }
            ?.toRoleVoiceProfile()
            ?: activeRoleConfigProvider?.invoke()?.toRoleVoiceProfile()
            ?: roleCardRepository?.getActiveRoleCard()?.toRoleVoiceProfile()
            ?: requestedConfig.toRoleVoiceProfile()
    }

    private fun VoiceOutputConfig.isExplicit(): Boolean {
        return mode != VoiceOutputMode.SYSTEM_TTS ||
            referenceAudioUri.isNotBlank() ||
            displayName.isNotBlank()
    }

    private fun VoiceOutputConfig.toRoleVoiceProfile(): RoleVoiceProfile {
        return RoleVoiceProfile(
            mode = mode,
            referenceAudioUri = referenceAudioUri.trim(),
            displayName = displayName.trim()
        )
    }

    private fun RoleCard.toRoleVoiceProfile(): RoleVoiceProfile {
        return RoleVoiceProfile(
            mode = runCatching { VoiceOutputMode.valueOf(voiceMode.trim()) }
                .getOrDefault(VoiceOutputMode.SYSTEM_TTS),
            referenceAudioUri = voiceProfileUri.trim(),
            displayName = voiceDisplayName.trim(),
            roleId = id.takeIf { it > 0 }?.toString().orEmpty()
        )
    }
}
