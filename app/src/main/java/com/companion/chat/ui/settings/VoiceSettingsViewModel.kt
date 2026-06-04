package com.companion.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.chat.engine.VoiceOutputEngine
import com.companion.chat.engine.VoiceOutputState
import com.companion.chat.engine.voice.CloudAsrConfig
import com.companion.chat.engine.voice.CloudAsrConfigRepository
import com.companion.chat.engine.voice.LocalSenseVoiceModelStatus
import com.companion.chat.engine.voice.MossTtsNanoModelStatus
import com.companion.chat.engine.voice.VoiceCloneConfig
import com.companion.chat.engine.voice.VoiceCloneConfigRepository
import com.companion.chat.engine.voice.VoiceCloneTestRepository
import com.companion.chat.engine.voice.VoiceInputConfig
import com.companion.chat.engine.voice.VoiceInputConfigRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VoiceSettingsUiState(
    val voiceInputConfig: VoiceInputConfig,
    val localModelStatus: LocalSenseVoiceModelStatus,
    val cloudAsrConfig: CloudAsrConfig,
    val voiceCloneConfig: VoiceCloneConfig,
    val mossModelStatus: MossTtsNanoModelStatus
)

class VoiceSettingsViewModel(
    private val voiceInputConfigRepository: VoiceInputConfigRepository,
    private val cloudAsrConfigRepository: CloudAsrConfigRepository,
    private val voiceCloneConfigRepository: VoiceCloneConfigRepository,
    private val voiceOutputEngine: VoiceOutputEngine,
    private val voiceCloneTestRepository: VoiceCloneTestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildUiState())
    val uiState: StateFlow<VoiceSettingsUiState> = _uiState.asStateFlow()
    val voiceOutputState: StateFlow<VoiceOutputState> = voiceOutputEngine.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VoiceOutputState.Idle
    )

    fun refresh() {
        _uiState.update { buildUiState() }
    }

    fun speakSweetGirlSample() {
        viewModelScope.launch {
            voiceOutputEngine.speak(
                text = "我在这里。今天就用甜一点的声音陪你聊。",
                config = voiceCloneTestRepository.sweetGirlConfig()
            )
        }
    }

    fun stopSpeaking() {
        voiceOutputEngine.stop()
    }

    private fun buildUiState(): VoiceSettingsUiState {
        val voiceInputConfig = voiceInputConfigRepository.getConfig()
        val cloudAsrConfig = cloudAsrConfigRepository.getConfig()
        val voiceCloneConfig = voiceCloneConfigRepository.getConfig()
        return VoiceSettingsUiState(
            voiceInputConfig = voiceInputConfig,
            localModelStatus = voiceInputConfigRepository.getLocalSenseVoiceModelStatus(voiceInputConfig),
            cloudAsrConfig = cloudAsrConfig,
            voiceCloneConfig = voiceCloneConfig,
            mossModelStatus = voiceCloneConfigRepository.getMossModelStatus(voiceCloneConfig)
        )
    }
}
