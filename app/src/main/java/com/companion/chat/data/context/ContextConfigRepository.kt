package com.companion.chat.data.context

import android.content.Context

class ContextConfigRepository(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getSettings(): ContextSettings {
        return ContextSettings(
            retainedRounds = sharedPreferences.getInt(KEY_RETAINED_ROUNDS, DEFAULT_SETTINGS.retainedRounds),
            compressionBuffer = sharedPreferences.getInt(
                KEY_COMPRESSION_BUFFER,
                DEFAULT_SETTINGS.compressionBuffer
            ),
            summaryMaxChars = sharedPreferences.getInt(
                KEY_SUMMARY_MAX_CHARS,
                DEFAULT_SETTINGS.summaryMaxChars
            ),
            summaryTimeoutMillis = sharedPreferences.getLong(
                KEY_SUMMARY_TIMEOUT_MILLIS,
                DEFAULT_SETTINGS.summaryTimeoutMillis
            )
        )
    }

    fun updateSettings(settings: ContextSettings) {
        sharedPreferences.edit()
            .putInt(KEY_RETAINED_ROUNDS, settings.retainedRounds)
            .putInt(KEY_COMPRESSION_BUFFER, settings.compressionBuffer)
            .putInt(KEY_SUMMARY_MAX_CHARS, settings.summaryMaxChars)
            .putLong(KEY_SUMMARY_TIMEOUT_MILLIS, settings.summaryTimeoutMillis)
            .apply()
    }

    fun updateRetainedRounds(retainedRounds: Int) {
        val currentSettings = getSettings()
        updateSettings(
            currentSettings.copy(retainedRounds = retainedRounds.coerceIn(MIN_RETAINED_ROUNDS, MAX_RETAINED_ROUNDS))
        )
    }

    companion object {
        const val MIN_RETAINED_ROUNDS = 3
        const val MAX_RETAINED_ROUNDS = 20
        private const val PREFS_NAME = "context_settings"
        private const val KEY_RETAINED_ROUNDS = "retained_rounds"
        private const val KEY_COMPRESSION_BUFFER = "compression_buffer"
        private const val KEY_SUMMARY_MAX_CHARS = "summary_max_chars"
        private const val KEY_SUMMARY_TIMEOUT_MILLIS = "summary_timeout_millis"

        val DEFAULT_SETTINGS = ContextSettings()
    }
}
