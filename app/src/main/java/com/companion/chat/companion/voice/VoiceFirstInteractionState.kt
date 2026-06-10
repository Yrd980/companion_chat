package com.companion.chat.companion.voice

data class VoiceFirstInteractionState(
    val isStarting: Boolean = false,
    val isListening: Boolean = false,
    val isWarmedUp: Boolean = false,
    val isSpeaking: Boolean = false,
    val isAutoSending: Boolean = false,
    val inputError: String = "",
    val lastTranscript: String = "",
    val inputPreview: String = "",
    val showPermissionDialog: Boolean = false
) {
    val isInputActive: Boolean
        get() = isStarting || isListening

    val shouldShowInputPreview: Boolean
        get() = isInputActive || isAutoSending || inputPreview.isNotBlank()

    val inputPlaceholder: String
        get() = when {
            isStarting -> "Starting voice recognition..."
            isListening -> "Listening..."
            isAutoSending -> "Sending voice..."
            else -> "Type a message..."
        }
}
