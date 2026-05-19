package com.companion.chat.memory

data class ExtractedMemory(
    val content: String,
    val category: String,
    val layer: String,
    val source: String,
    val expiresAt: Long? = null
)
