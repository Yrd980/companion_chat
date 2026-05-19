package com.companion.chat.context

data class ContextSettings(
    val retainedRounds: Int = 10,
    val compressionBuffer: Int = 10,
    val summaryMaxChars: Int = 200,
    val summaryTimeoutMillis: Long = 60_000L
) {
    val compressionThreshold: Int
        get() = retainedRounds * 2 + compressionBuffer
}
