package com.companion.chat.data.profile

import android.content.Context
import android.content.SharedPreferences

data class UserProfile(
    val displayName: String = "You",
    val avatarUri: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = ""
)

class UserProfileRepository(
    private val sharedPreferences: SharedPreferences
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    fun getProfile(): UserProfile {
        return UserProfile(
            displayName = sharedPreferences.getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME)
                ?.trim()
                ?.ifBlank { DEFAULT_DISPLAY_NAME }
                ?: DEFAULT_DISPLAY_NAME,
            avatarUri = sharedPreferences.getString(KEY_AVATAR_URI, "").orEmpty(),
            emergencyContactName = sharedPreferences.getString(KEY_EMERGENCY_CONTACT_NAME, "").orEmpty(),
            emergencyContactPhone = sharedPreferences.getString(KEY_EMERGENCY_CONTACT_PHONE, "").orEmpty()
        )
    }

    fun updateProfile(profile: UserProfile) {
        sharedPreferences.edit()
            .putString(KEY_DISPLAY_NAME, profile.displayName.trim().ifBlank { DEFAULT_DISPLAY_NAME })
            .putString(KEY_AVATAR_URI, profile.avatarUri.trim())
            .putString(KEY_EMERGENCY_CONTACT_NAME, profile.emergencyContactName.trim())
            .putString(KEY_EMERGENCY_CONTACT_PHONE, profile.emergencyContactPhone.trim())
            .apply()
    }

    fun updateDisplayName(displayName: String) {
        updateProfile(getProfile().copy(displayName = displayName))
    }

    fun updateAvatarUri(avatarUri: String) {
        updateProfile(getProfile().copy(avatarUri = avatarUri))
    }

    fun updateEmergencyContact(name: String, phone: String) {
        updateProfile(
            getProfile().copy(
                emergencyContactName = name,
                emergencyContactPhone = phone
            )
        )
    }

    private companion object {
        const val PREFS_NAME = "user_profile"
        const val DEFAULT_DISPLAY_NAME = "You"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_AVATAR_URI = "avatar_uri"
        const val KEY_EMERGENCY_CONTACT_NAME = "emergency_contact_name"
        const val KEY_EMERGENCY_CONTACT_PHONE = "emergency_contact_phone"
    }
}
