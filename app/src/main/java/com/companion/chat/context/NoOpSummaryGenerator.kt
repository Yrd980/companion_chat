package com.companion.chat.context

import com.companion.chat.data.model.ChatMessage

class NoOpSummaryGenerator : SummaryGenerator {

    override suspend fun summarize(
        messages: List<ChatMessage>,
        settings: ContextSettings
    ): String {
        return ""
    }
}
