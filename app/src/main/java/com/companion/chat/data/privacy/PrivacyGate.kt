package com.companion.chat.data.privacy

class PrivacyGate(
    private val settingsProvider: () -> PrivacySettings
) {
    fun evaluate(request: PrivacyGateRequest): PrivacyGateDecision {
        val settings = settingsProvider()
        if (settings.localOnlyMode) {
            return PrivacyGateDecision.Denied(
                reason = "Local-only mode is enabled. ${request.reason} cannot send ${request.dataType.label} to ${request.destination}."
            )
        }

        val allowedByPurpose = when (request.dataType) {
            PrivacyDataType.Audio -> settings.allowCloudAsr
            PrivacyDataType.VoiceCloneText -> settings.allowHttpVoiceClone
            PrivacyDataType.ImagePrompt -> settings.allowHttpImageGeneration
            PrivacyDataType.LlmPrompt -> settings.allowCloudLlm
        }
        return if (allowedByPurpose) {
            PrivacyGateDecision.Allowed
        } else {
            PrivacyGateDecision.Denied(
                reason = "${request.reason} is not enabled for ${request.destination}. Use ${request.localAlternative} or enable the matching privacy setting."
            )
        }
    }

    fun requireAllowed(request: PrivacyGateRequest) {
        when (val decision = evaluate(request)) {
            PrivacyGateDecision.Allowed -> Unit
            is PrivacyGateDecision.Denied -> throw PrivacyGateDeniedException(decision.reason)
        }
    }
}

data class PrivacyGateRequest(
    val dataType: PrivacyDataType,
    val destination: String,
    val reason: String,
    val localAlternative: String
)

enum class PrivacyDataType(val label: String) {
    Audio("audio"),
    VoiceCloneText("voice text"),
    ImagePrompt("image prompt"),
    LlmPrompt("LLM prompt")
}

sealed class PrivacyGateDecision {
    data object Allowed : PrivacyGateDecision()
    data class Denied(val reason: String) : PrivacyGateDecision()
}

class PrivacyGateDeniedException(message: String) : IllegalStateException(message)

object PrivacyGateDefaults {
    fun denyRemoteByDefault(): PrivacyGate {
        return PrivacyGate(
            settingsProvider = {
                PrivacySettings(
                    localOnlyMode = true
                )
            }
        )
    }
}
