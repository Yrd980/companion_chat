package com.companion.chat.data.context

class PromptAssembler {

    fun assemble(
        baseSystemPrompt: String,
        userPreferences: String,
        historySummary: String,
        recentConversationSnippet: String = ""
    ): String {
        val sections = mutableListOf<String>()

        if (baseSystemPrompt.isNotBlank()) {
            sections += baseSystemPrompt.trim()
        }
        if (userPreferences.isNotBlank()) {
            sections += userPreferences.trim()
        }
        if (historySummary.isNotBlank()) {
            sections += "之前对话的摘要：\n${historySummary.trim()}"
        }
        if (recentConversationSnippet.isNotBlank()) {
            sections += "最近几轮对话片段：\n${recentConversationSnippet.trim()}"
        }

        return sections.joinToString(separator = "\n\n")
    }
}
