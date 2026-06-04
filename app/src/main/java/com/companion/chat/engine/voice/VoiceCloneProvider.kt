package com.companion.chat.engine.voice

enum class VoiceCloneProvider {
    SYSTEM_TTS,
    HTTP_CLONE,
    MOSS_TTS_NANO
}

data class VoiceCloneRequest(
    val text: String,
    val referenceAudioUri: String = "",
    val roleId: String = "",
    val displayName: String = ""
)

data class VoiceCloneResult(
    val provider: VoiceCloneProvider,
    val audioUri: String? = null,
    val fallbackToSystemTts: Boolean = false,
    val message: String = ""
)

interface VoiceCloneEngine {
    suspend fun synthesize(request: VoiceCloneRequest): Result<VoiceCloneResult>
}
