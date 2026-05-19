package com.companion.chat.engine

import kotlinx.coroutines.flow.Flow

sealed class VoiceOutputState {
    data object Idle : VoiceOutputState()
    data object Speaking : VoiceOutputState()
    data class Error(val message: String) : VoiceOutputState()
}

enum class VoiceOutputMode {
    SYSTEM_TTS,
    CLONE
}

data class VoiceOutputConfig(
    val mode: VoiceOutputMode = VoiceOutputMode.SYSTEM_TTS,
    val referenceAudioUri: String = "",
    val displayName: String = ""
)

interface VoiceOutputEngine {
    val state: Flow<VoiceOutputState>

    suspend fun speak(text: String, config: VoiceOutputConfig = VoiceOutputConfig())

    fun stop()

    fun release()
}
