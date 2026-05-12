package com.companion.chat.data.engine

import kotlinx.coroutines.flow.Flow

sealed class VoiceOutputState {
    data object Idle : VoiceOutputState()
    data object Speaking : VoiceOutputState()
    data class Error(val message: String) : VoiceOutputState()
}

interface VoiceOutputEngine {
    val state: Flow<VoiceOutputState>

    suspend fun speak(text: String)

    fun stop()

    fun release()
}
