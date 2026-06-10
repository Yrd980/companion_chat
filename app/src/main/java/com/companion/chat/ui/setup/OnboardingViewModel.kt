package com.companion.chat.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.chat.data.setup.SetupRepository
import com.companion.chat.data.timeline.TimelineEventType
import com.companion.chat.data.timeline.TimelineEventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupStepUiState(
    val id: String,
    val title: String,
    val status: SetupStatus,
    val detail: String,
    val actionLabel: String,
    val routeHint: String
)

enum class SetupStatus {
    READY,
    REQUIRED,
    OPTIONAL,
    SKIPPED,
    NEEDS_ATTENTION
}

data class OnboardingUiState(
    val steps: List<SetupStepUiState> = emptyList(),
    val isComplete: Boolean = false,
    val message: String = ""
)

class OnboardingViewModel(
    private val setupRepository: SetupRepository,
    private val timelineEventRepository: TimelineEventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val setupState = setupRepository.getSetupState()
        _uiState.value = OnboardingUiState(
            steps = listOf(
                SetupStepUiState(
                    id = STEP_PROFILE,
                    title = "Local Profile",
                    status = if (setupState.profileComplete) SetupStatus.READY else SetupStatus.REQUIRED,
                    detail = if (setupState.profileComplete) {
                        "Profile is ready for local companion use."
                    } else {
                        "Add a display name before daily use."
                    },
                    actionLabel = if (setupState.profileComplete) "Review" else "Edit",
                    routeHint = ROUTE_PROFILE
                ),
                SetupStepUiState(
                    id = STEP_MICROPHONE,
                    title = "Microphone Permission",
                    status = if (setupState.microphonePermissionGranted) SetupStatus.READY else SetupStatus.OPTIONAL,
                    detail = if (setupState.microphonePermissionGranted) {
                        "Voice capture has been reviewed on this device."
                    } else {
                        "Text chat works now. Review microphone access before voice capture."
                    },
                    actionLabel = if (setupState.microphonePermissionGranted) "Mark pending" else "Mark reviewed",
                    routeHint = ROUTE_VOICE
                ),
                SetupStepUiState(
                    id = STEP_TEXT_MODEL,
                    title = "Text Model",
                    status = if (setupState.textModelReady) SetupStatus.READY else SetupStatus.NEEDS_ATTENTION,
                    detail = if (setupState.textModelReady) {
                        "Local text model package is ready."
                    } else {
                        "Choose a local model path or continue with degraded text capability."
                    },
                    actionLabel = "Model settings",
                    routeHint = ROUTE_MODEL
                ),
                SetupStepUiState(
                    id = STEP_VOICE_INPUT,
                    title = "Voice Input",
                    status = if (setupState.voiceReady) SetupStatus.READY else SetupStatus.NEEDS_ATTENTION,
                    detail = if (setupState.voiceReady) {
                        "Speech recognition and voice output are usable."
                    } else {
                        "Configure local voice input or keep text as the reliable fallback."
                    },
                    actionLabel = "Voice settings",
                    routeHint = ROUTE_VOICE
                ),
                SetupStepUiState(
                    id = STEP_VOICE_OUTPUT,
                    title = "Voice Output",
                    status = if (setupState.voiceReady) SetupStatus.READY else SetupStatus.OPTIONAL,
                    detail = if (setupState.voiceReady) {
                        "Voice output can respond during companion turns."
                    } else {
                        "System TTS can remain as fallback while voice clone models are missing."
                    },
                    actionLabel = "Voice settings",
                    routeHint = ROUTE_VOICE
                ),
                SetupStepUiState(
                    id = STEP_IMAGE,
                    title = "Image Generation",
                    status = if (setupState.imageReady) SetupStatus.READY else SetupStatus.OPTIONAL,
                    detail = if (setupState.imageReady) {
                        "Local or configured image generation is ready."
                    } else {
                        "Image generation is optional and can be configured later."
                    },
                    actionLabel = "Model settings",
                    routeHint = ROUTE_MODEL
                ),
                SetupStepUiState(
                    id = STEP_PRIVACY,
                    title = "Privacy Review",
                    status = if (setupState.privacyReviewed) SetupStatus.READY else SetupStatus.REQUIRED,
                    detail = if (setupState.privacyReviewed) {
                        "Local-only defaults and opt-in cloud controls have been reviewed."
                    } else {
                        "Review local-only mode before capture or generation."
                    },
                    actionLabel = if (setupState.privacyReviewed) "Review again" else "Mark reviewed",
                    routeHint = ROUTE_PRIVACY
                )
            ),
            isComplete = setupState.isComplete
        )
    }

    fun toggleMicrophoneReviewed() {
        val current = setupRepository.getSetupState().microphonePermissionGranted
        setupRepository.updateMicrophonePermission(!current)
        recordSetupEvent(
            title = "Microphone setup changed",
            detail = if (current) "Microphone review was marked pending." else "Microphone review was marked complete."
        )
        refresh()
    }

    fun markPrivacyReviewed() {
        setupRepository.markPrivacyReviewed(true)
        recordSetupEvent(
            title = "Privacy setup reviewed",
            detail = "Local privacy defaults were reviewed from setup."
        )
        refresh()
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = "") }
    }

    private fun recordSetupEvent(title: String, detail: String) {
        viewModelScope.launch {
            timelineEventRepository.add(
                type = TimelineEventType.SETUP_CHANGED,
                title = title,
                detail = detail
            )
        }
    }

    companion object {
        const val STEP_PROFILE = "profile"
        const val STEP_MICROPHONE = "microphone"
        const val STEP_TEXT_MODEL = "text_model"
        const val STEP_VOICE_INPUT = "voice_input"
        const val STEP_VOICE_OUTPUT = "voice_output"
        const val STEP_IMAGE = "image"
        const val STEP_PRIVACY = "privacy"

        const val ROUTE_PROFILE = "settings"
        const val ROUTE_MODEL = "settings/model"
        const val ROUTE_VOICE = "settings/voice"
        const val ROUTE_PRIVACY = "settings"
        const val ROUTE_LANGUAGE = "settings/language"
    }
}
