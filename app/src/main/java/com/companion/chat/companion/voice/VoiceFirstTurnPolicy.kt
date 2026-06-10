package com.companion.chat.companion.voice

sealed class VoiceTranscriptDecision {
    data object AutoSend : VoiceTranscriptDecision()
    data class HoldForUser(val message: String) : VoiceTranscriptDecision()
}

object VoiceFirstTurnPolicy {
    fun evaluateTranscript(
        transcript: String,
        isGenerating: Boolean,
        isEngineReady: Boolean,
        isVoiceFirstReady: Boolean = true
    ): VoiceTranscriptDecision {
        return when {
            transcript.isBlank() -> VoiceTranscriptDecision.HoldForUser("No speech was recognized.")
            isGenerating -> VoiceTranscriptDecision.HoldForUser("A reply is still generating. Please wait.")
            !isEngineReady -> VoiceTranscriptDecision.HoldForUser("The model is not ready. The transcript stayed in the input field.")
            !isVoiceFirstReady -> VoiceTranscriptDecision.HoldForUser("Voice mode is not ready. The transcript stayed in the input field.")
            else -> VoiceTranscriptDecision.AutoSend
        }
    }
}
