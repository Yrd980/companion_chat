package com.companion.chat.data.memory

class RuleBasedMemoryExtractor : MemoryExtractor {

    override fun extract(userMessage: String, sessionId: String): List<ExtractedMemory> {
        val normalizedMessage = userMessage.trim()
        if (normalizedMessage.isEmpty()) {
            return emptyList()
        }

        return listOfNotNull(
            extractName(normalizedMessage),
            extractPreference(normalizedMessage),
            extractLocation(normalizedMessage),
            extractAvoidance(normalizedMessage)
        ).take(1)
    }

    private fun extractName(message: String): ExtractedMemory? {
        val value = listOf(
            Regex("^记住我叫(.+)$"),
            Regex("^我叫(.+)$")
        ).firstNotNullOfOrNull { pattern ->
            pattern.matchEntire(message)?.groupValues?.get(1)
        }?.sanitizeValue()

        return value?.let {
            createMemory(
                content = "用户叫$it",
                category = CATEGORY_FACT
            )
        }
    }

    private fun extractPreference(message: String): ExtractedMemory? {
        val value = Regex("^我喜欢(.+)$")
            .matchEntire(message)
            ?.groupValues
            ?.get(1)
            ?.sanitizeValue()

        return value?.let {
            createMemory(
                content = "用户喜欢$it",
                category = CATEGORY_PREFERENCE
            )
        }
    }

    private fun extractLocation(message: String): ExtractedMemory? {
        val value = Regex("^我住在(.+)$")
            .matchEntire(message)
            ?.groupValues
            ?.get(1)
            ?.sanitizeValue()

        return value?.let {
            createMemory(
                content = "用户住在$it",
                category = CATEGORY_FACT
            )
        }
    }

    private fun extractAvoidance(message: String): ExtractedMemory? {
        val value = Regex("^不要再说(.+?)(了)?$")
            .matchEntire(message)
            ?.groupValues
            ?.get(1)
            ?.sanitizeValue()

        return value?.let {
            createMemory(
                content = "不要再说$it",
                category = CATEGORY_PREFERENCE
            )
        }
    }

    private fun createMemory(content: String, category: String): ExtractedMemory {
        return ExtractedMemory(
            content = content,
            category = category,
            layer = LAYER_SHORT_TERM,
            source = SOURCE_RULE_EXTRACTOR
        )
    }

    private fun String.sanitizeValue(): String? {
        val sanitized = trim().trimEnd('。', '！', '？', '.', '!', '?')
        return sanitized.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val CATEGORY_FACT = "fact"
        private const val CATEGORY_PREFERENCE = "preference"
        private const val LAYER_SHORT_TERM = "short_term"
        private const val SOURCE_RULE_EXTRACTOR = "rule_extractor"
    }
}
