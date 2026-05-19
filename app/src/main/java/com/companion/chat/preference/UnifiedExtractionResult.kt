package com.companion.chat.preference

import com.companion.chat.memory.ExtractedMemory

data class UnifiedExtractionResult(
    val memories: List<ExtractedMemory> = emptyList(),
    val userPreferences: List<ExtractedPreference> = emptyList()
)
