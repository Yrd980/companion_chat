package com.companion.chat.ui.language

import android.content.Context
import android.content.SharedPreferences

class AppLanguageRepository(
    private val sharedPreferences: SharedPreferences
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    fun getLanguage(): AppLanguage {
        return AppLanguage.fromStorageKey(sharedPreferences.getString(KEY_LANGUAGE, null))
    }

    fun updateLanguage(language: AppLanguage) {
        sharedPreferences.edit().putString(KEY_LANGUAGE, language.storageKey).apply()
    }

    private companion object {
        const val PREFS_NAME = "app_language"
        const val KEY_LANGUAGE = "language"
    }
}
