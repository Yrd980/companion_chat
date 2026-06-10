package com.companion.chat.data.setup

import android.content.Context
import android.content.SharedPreferences
import com.companion.chat.companion.readiness.CompanionReadinessLevel
import com.companion.chat.companion.readiness.CompanionReadinessRepository
import com.companion.chat.data.profile.UserProfileRepository

data class SetupState(
    val profileComplete: Boolean = false,
    val microphonePermissionGranted: Boolean = false,
    val textModelReady: Boolean = false,
    val voiceReady: Boolean = false,
    val imageReady: Boolean = false,
    val privacyReviewed: Boolean = false
) {
    val isComplete: Boolean
        get() = profileComplete &&
            microphonePermissionGranted &&
            textModelReady &&
            voiceReady &&
            imageReady &&
            privacyReviewed
}

class SetupRepository(
    private val sharedPreferences: SharedPreferences,
    private val userProfileRepository: UserProfileRepository,
    private val readinessRepository: CompanionReadinessRepository
) {
    constructor(
        context: Context,
        userProfileRepository: UserProfileRepository,
        readinessRepository: CompanionReadinessRepository
    ) : this(
        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        userProfileRepository = userProfileRepository,
        readinessRepository = readinessRepository
    )

    fun getSetupState(): SetupState {
        val profile = userProfileRepository.getProfile()
        val readiness = readinessRepository.getSnapshot()
        return SetupState(
            profileComplete = profile.displayName.isNotBlank(),
            microphonePermissionGranted = sharedPreferences.getBoolean(KEY_MICROPHONE_PERMISSION_GRANTED, false),
            textModelReady = readiness.llm.level == CompanionReadinessLevel.READY,
            voiceReady = readiness.asr.isUsable && readiness.tts.isUsable,
            imageReady = readiness.image.level == CompanionReadinessLevel.READY,
            privacyReviewed = sharedPreferences.getBoolean(KEY_PRIVACY_REVIEWED, false)
        )
    }

    fun updateMicrophonePermission(granted: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_MICROPHONE_PERMISSION_GRANTED, granted)
            .apply()
    }

    fun markPrivacyReviewed(reviewed: Boolean = true) {
        sharedPreferences.edit()
            .putBoolean(KEY_PRIVACY_REVIEWED, reviewed)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "setup_state"
        const val KEY_MICROPHONE_PERMISSION_GRANTED = "microphone_permission_granted"
        const val KEY_PRIVACY_REVIEWED = "privacy_reviewed"
    }
}
