package com.companion.chat.data.privacy

import android.content.Context
import android.content.SharedPreferences

data class PrivacySettings(
    val localOnlyMode: Boolean = true,
    val allowCloudAsr: Boolean = false,
    val allowHttpVoiceClone: Boolean = false,
    val allowHttpImageGeneration: Boolean = false,
    val allowAnalytics: Boolean = false,
    val allowPartnerSharing: Boolean = false
)

class PrivacySettingsRepository(
    private val sharedPreferences: SharedPreferences
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    fun getSettings(): PrivacySettings {
        return PrivacySettings(
            localOnlyMode = sharedPreferences.getBoolean(KEY_LOCAL_ONLY_MODE, true),
            allowCloudAsr = sharedPreferences.getBoolean(KEY_ALLOW_CLOUD_ASR, false),
            allowHttpVoiceClone = sharedPreferences.getBoolean(KEY_ALLOW_HTTP_VOICE_CLONE, false),
            allowHttpImageGeneration = sharedPreferences.getBoolean(KEY_ALLOW_HTTP_IMAGE_GENERATION, false),
            allowAnalytics = sharedPreferences.getBoolean(KEY_ALLOW_ANALYTICS, false),
            allowPartnerSharing = sharedPreferences.getBoolean(KEY_ALLOW_PARTNER_SHARING, false)
        ).withLocalOnlyGuardrails()
    }

    fun updateSettings(settings: PrivacySettings) {
        val normalized = settings.withLocalOnlyGuardrails()
        sharedPreferences.edit()
            .putBoolean(KEY_LOCAL_ONLY_MODE, normalized.localOnlyMode)
            .putBoolean(KEY_ALLOW_CLOUD_ASR, normalized.allowCloudAsr)
            .putBoolean(KEY_ALLOW_HTTP_VOICE_CLONE, normalized.allowHttpVoiceClone)
            .putBoolean(KEY_ALLOW_HTTP_IMAGE_GENERATION, normalized.allowHttpImageGeneration)
            .putBoolean(KEY_ALLOW_ANALYTICS, normalized.allowAnalytics)
            .putBoolean(KEY_ALLOW_PARTNER_SHARING, normalized.allowPartnerSharing)
            .apply()
    }

    private fun PrivacySettings.withLocalOnlyGuardrails(): PrivacySettings {
        return if (!localOnlyMode) {
            this
        } else {
            copy(
                allowCloudAsr = false,
                allowHttpVoiceClone = false,
                allowHttpImageGeneration = false,
                allowAnalytics = false,
                allowPartnerSharing = false
            )
        }
    }

    private companion object {
        const val PREFS_NAME = "privacy_settings"
        const val KEY_LOCAL_ONLY_MODE = "local_only_mode"
        const val KEY_ALLOW_CLOUD_ASR = "allow_cloud_asr"
        const val KEY_ALLOW_HTTP_VOICE_CLONE = "allow_http_voice_clone"
        const val KEY_ALLOW_HTTP_IMAGE_GENERATION = "allow_http_image_generation"
        const val KEY_ALLOW_ANALYTICS = "allow_analytics"
        const val KEY_ALLOW_PARTNER_SHARING = "allow_partner_sharing"
    }
}
