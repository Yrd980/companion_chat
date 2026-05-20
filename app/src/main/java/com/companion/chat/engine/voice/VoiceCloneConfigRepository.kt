package com.companion.chat.engine.voice

import android.content.Context
import android.content.SharedPreferences

class VoiceCloneConfigRepository(
    private val sharedPreferences: SharedPreferences,
    private val defaultMossModelDirectoryProvider: () -> String = { "" }
) {
    constructor(context: Context) : this(
        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        defaultMossModelDirectoryProvider = {
            context.applicationContext.getExternalFilesDir(MossTtsNanoModelPackage.DEFAULT_MODEL_RELATIVE_DIRECTORY)
                ?.absolutePath
                .orEmpty()
        }
    )

    fun getConfig(): VoiceCloneConfig {
        return VoiceCloneConfig(
            mossModelDirectory = sharedPreferences.getString(KEY_MOSS_MODEL_DIRECTORY, null)
                ?.trim()
                .orEmpty()
                .ifBlank { defaultMossModelDirectoryProvider().trim() },
            httpCloneBaseUrl = sharedPreferences.getString(KEY_HTTP_CLONE_BASE_URL, "")
                .orEmpty()
                .trim(),
            httpCloneApiKey = sharedPreferences.getString(KEY_HTTP_CLONE_API_KEY, "")
                .orEmpty()
                .trim(),
            httpCloneVoice = sharedPreferences.getString(KEY_HTTP_CLONE_VOICE, "custom")
                .orEmpty()
                .trim()
                .ifBlank { "custom" },
            httpCloneTimeoutMillis = sharedPreferences.getInt(
                KEY_HTTP_CLONE_TIMEOUT_MILLIS,
                VoiceCloneConfig.DEFAULT_TIMEOUT_MILLIS
            ).coerceIn(VoiceCloneConfig.MIN_TIMEOUT_MILLIS, VoiceCloneConfig.MAX_TIMEOUT_MILLIS)
        )
    }

    fun updateConfig(config: VoiceCloneConfig) {
        sharedPreferences.edit()
            .putString(KEY_MOSS_MODEL_DIRECTORY, config.mossModelDirectory.trim())
            .putString(KEY_HTTP_CLONE_BASE_URL, config.httpCloneBaseUrl.trim())
            .putString(KEY_HTTP_CLONE_API_KEY, config.httpCloneApiKey.trim())
            .putString(KEY_HTTP_CLONE_VOICE, config.httpCloneVoice.trim().ifBlank { "custom" })
            .putInt(
                KEY_HTTP_CLONE_TIMEOUT_MILLIS,
                config.httpCloneTimeoutMillis.coerceIn(
                    VoiceCloneConfig.MIN_TIMEOUT_MILLIS,
                    VoiceCloneConfig.MAX_TIMEOUT_MILLIS
                )
            )
            .apply()
    }

    fun getMossModelStatus(config: VoiceCloneConfig = getConfig()): MossTtsNanoModelStatus {
        return MossTtsNanoModelPackage.inspect(config.mossModelDirectory)
    }

    companion object {
        const val PREFS_NAME = "voice_clone_config"
        private const val KEY_MOSS_MODEL_DIRECTORY = "moss_model_directory"
        private const val KEY_HTTP_CLONE_BASE_URL = "http_clone_base_url"
        private const val KEY_HTTP_CLONE_API_KEY = "http_clone_api_key"
        private const val KEY_HTTP_CLONE_VOICE = "http_clone_voice"
        private const val KEY_HTTP_CLONE_TIMEOUT_MILLIS = "http_clone_timeout_millis"
    }
}

data class VoiceCloneConfig(
    val mossModelDirectory: String = "",
    val httpCloneBaseUrl: String = "",
    val httpCloneApiKey: String = "",
    val httpCloneVoice: String = "custom",
    val httpCloneTimeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS
) {
    val isHttpCloneConfigured: Boolean = httpCloneBaseUrl.isNotBlank()

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 120_000
        const val MIN_TIMEOUT_MILLIS = 5_000
        const val MAX_TIMEOUT_MILLIS = 300_000
    }
}
