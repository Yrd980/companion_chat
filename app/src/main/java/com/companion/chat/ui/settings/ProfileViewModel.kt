package com.companion.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.chat.companion.readiness.CompanionReadinessSnapshot
import com.companion.chat.companion.readiness.CompanionReadinessRepository
import com.companion.chat.data.export.DataExportRepository
import com.companion.chat.data.export.LocalDataDeleteScope
import com.companion.chat.data.plan.PlanState
import com.companion.chat.data.plan.PlanRepository
import com.companion.chat.data.privacy.PrivacySettings
import com.companion.chat.data.privacy.PrivacySettingsRepository
import com.companion.chat.data.profile.UserProfile
import com.companion.chat.data.profile.UserProfileRepository
import com.companion.chat.data.timeline.TimelineEventType
import com.companion.chat.data.timeline.TimelineEventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val planState: PlanState = PlanState(),
    val privacySettings: PrivacySettings = PrivacySettings(),
    val readinessSnapshot: CompanionReadinessSnapshot? = null,
    val exportStatusMessage: String = "",
    val pendingDeleteScope: LocalDataDeleteScope? = null,
    val message: String = "",
    val retainedRounds: Int = 12
)

class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val privacySettingsRepository: PrivacySettingsRepository,
    private val planRepository: PlanRepository,
    private val dataExportRepository: DataExportRepository,
    private val timelineEventRepository: TimelineEventRepository,
    private val readinessRepository: CompanionReadinessRepository,
    private val retainedRoundsProvider: () -> Int = { 12 }
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                profile = userProfileRepository.getProfile(),
                planState = planRepository.getPlanState(),
                privacySettings = privacySettingsRepository.getSettings(),
                readinessSnapshot = readinessRepository.getSnapshot(),
                retainedRounds = retainedRoundsProvider()
            )
        }
    }

    fun updateDisplayName(displayName: String) {
        userProfileRepository.updateDisplayName(displayName)
        refresh()
    }

    fun updateAvatarUri(avatarUri: String) {
        userProfileRepository.updateAvatarUri(avatarUri)
        refresh()
    }

    fun updateEmergencyContact(name: String, phone: String) {
        viewModelScope.launch {
            userProfileRepository.updateEmergencyContact(name, phone)
            timelineEventRepository.add(
                type = TimelineEventType.SETUP_CHANGED,
                title = "Emergency contact updated",
                detail = "Emergency contact settings changed locally."
            )
            refresh()
        }
    }

    fun updatePrivacySettings(settings: PrivacySettings) {
        viewModelScope.launch {
            privacySettingsRepository.updateSettings(settings)
            timelineEventRepository.add(
                type = TimelineEventType.PRIVACY_CHANGED,
                title = "Privacy settings updated",
                detail = privacySummary(privacySettingsRepository.getSettings())
            )
            refresh()
        }
    }

    fun exportLocalData() {
        viewModelScope.launch {
            runCatching { dataExportRepository.exportAll() }
                .onSuccess { path ->
                    timelineEventRepository.add(
                        type = TimelineEventType.DATA_EXPORTED,
                        title = "Local data exported",
                        detail = path
                    )
                    _uiState.update {
                        it.copy(
                            exportStatusMessage = path,
                            message = "Export complete: $path"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(message = error.message ?: "Export failed")
                    }
                }
        }
    }

    fun requestDeleteLocalData(scope: LocalDataDeleteScope) {
        _uiState.update {
            it.copy(
                pendingDeleteScope = scope,
                message = ""
            )
        }
    }

    fun confirmDeleteLocalData() {
        val scope = _uiState.value.pendingDeleteScope ?: return
        viewModelScope.launch {
            runCatching { dataExportRepository.deleteLocalData(scope) }
                .onSuccess { count ->
                    timelineEventRepository.add(
                        type = TimelineEventType.LOCAL_DATA_DELETED,
                        title = "Local data deleted",
                        detail = "${scope.displayName()} deleted $count local records."
                    )
                    _uiState.update {
                        it.copy(
                            pendingDeleteScope = null,
                            message = "${scope.displayName()} deleted locally."
                        )
                    }
                    refresh()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(message = error.message ?: "Delete failed")
                    }
                }
        }
    }

    fun cancelDeleteLocalData() {
        _uiState.update { it.copy(pendingDeleteScope = null) }
    }

    private fun privacySummary(settings: PrivacySettings): String {
        return if (settings.localOnlyMode) {
            "Local-only mode enabled. Cloud, analytics, and sharing are off."
        } else {
            "Local-only mode disabled. Optional cloud settings are controlled individually."
        }
    }
}

fun LocalDataDeleteScope.displayName(): String {
    return when (this) {
        LocalDataDeleteScope.MEMORIES -> "Memories"
        LocalDataDeleteScope.CONVERSATIONS -> "Conversations"
        LocalDataDeleteScope.ROLE_CARDS -> "Role cards"
        LocalDataDeleteScope.ALL_LOCAL_USER_DATA -> "All local user data"
    }
}
