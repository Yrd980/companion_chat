package com.companion.chat.ui.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(val storageKey: String) {
    ENGLISH("en"),
    CHINESE("zh");

    companion object {
        fun fromStorageKey(value: String?): AppLanguage {
            return entries.firstOrNull { it.storageKey == value } ?: ENGLISH
        }
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }

@Composable
fun uiText(english: String, chinese: String): String {
    return uiText(LocalAppLanguage.current, english, chinese)
}

fun uiText(language: AppLanguage, english: String, chinese: String): String {
    return when (language) {
        AppLanguage.ENGLISH -> english
        AppLanguage.CHINESE -> chinese
    }
}
