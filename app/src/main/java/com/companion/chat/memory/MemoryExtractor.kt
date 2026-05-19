package com.companion.chat.memory

interface MemoryExtractor {
    fun extract(userMessage: String, sessionId: String): List<ExtractedMemory>
}
