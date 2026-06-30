package com.companion.chat.memory

class RuleBasedMemoryExtractor : MemoryExtractor {

    override fun extract(userMessage: String, sessionId: String): List<ExtractedMemory> {
        val normalizedMessage = userMessage.trim()
        if (normalizedMessage.isEmpty()) {
            return emptyList()
        }

        return splitToClauses(normalizedMessage)
            .flatMap { clause -> extractFromClause(clause) }
            .distinctBy { "${it.category}|${it.content}" }
    }

    private fun extractFromClause(clause: String): List<ExtractedMemory> {
        return listOfNotNull(
            extractName(clause),
            extractPreference(clause),
            extractDislike(clause),
            extractLocation(clause),
            extractHabit(clause),
            extractSelfDescription(clause),
            extractResponseStyle(clause),
            extractAvoidance(clause)
        )
    }

    private fun extractName(message: String): ExtractedMemory? {
        val value = listOf(
            Regex("^记住我叫(.+)$"),
            Regex("^我叫(.+)$"),
            Regex("^my name is (.+)$", RegexOption.IGNORE_CASE),
            Regex("^call me (.+)$", RegexOption.IGNORE_CASE),
            Regex("^i'?m (.+)$", RegexOption.IGNORE_CASE)
        ).firstNotNullOfOrNull { pattern ->
            pattern.matchEntire(message)?.groupValues?.get(1)
        }?.sanitizeValue()

        return value?.let {
            createMemory(
                content = "User's name is $it",
                category = CATEGORY_FACT
            )
        }
    }

    private fun extractPreference(message: String): ExtractedMemory? {
        val value = listOf(
            Regex("^(?:我)?(?:很)?喜欢(.+)$"),
            Regex("^i (?:really )?like (.+)$", RegexOption.IGNORE_CASE),
            Regex("^i love (.+)$", RegexOption.IGNORE_CASE),
            Regex("^i enjoy (.+)$", RegexOption.IGNORE_CASE)
        ).firstNotNullOfOrNull { pattern ->
            pattern.matchEntire(message)?.groupValues?.get(1)
        }?.sanitizeValue()

        return value?.let {
            createMemory(
                content = "User likes $it",
                category = CATEGORY_PREFERENCE
            )
        }
    }

    private fun extractDislike(message: String): ExtractedMemory? {
        val cnMatch = Regex("^(?:我)?(不喜欢|讨厌|不爱)(.+)$")
            .matchEntire(message)
        if (cnMatch != null) {
            val prefix = cnMatch.groupValues[1]
            val value = cnMatch.groupValues[2].sanitizeValue()
            if (value != null) {
                return createMemory(content = "用户$prefix$value", category = CATEGORY_PREFERENCE)
            }
        }

        val enMatch = Regex("^i (?:don'?t|do not) (?:like|enjoy) (.+)$", RegexOption.IGNORE_CASE)
            .matchEntire(message)
        if (enMatch != null) {
            val value = enMatch.groupValues[1].sanitizeValue()
            if (value != null) {
                return createMemory(content = "User dislikes $value", category = CATEGORY_PREFERENCE)
            }
        }

        val enHateMatch = Regex("^i hate (.+)$", RegexOption.IGNORE_CASE)
            .matchEntire(message)
        if (enHateMatch != null) {
            val value = enHateMatch.groupValues[1].sanitizeValue()
            if (value != null) {
                return createMemory(content = "User hates $value", category = CATEGORY_PREFERENCE)
            }
        }

        return null
    }

    private fun extractLocation(message: String): ExtractedMemory? {
        val value = listOf(
            Regex("^我住在(.+)$"),
            Regex("^i live in (.+)$", RegexOption.IGNORE_CASE),
            Regex("^i'?m from (.+)$", RegexOption.IGNORE_CASE),
            Regex("^i reside in (.+)$", RegexOption.IGNORE_CASE)
        ).firstNotNullOfOrNull { pattern ->
            pattern.matchEntire(message)?.groupValues?.get(1)
        }?.sanitizeValue()

        return value?.let {
            createMemory(
                content = "User lives in $it",
                category = CATEGORY_FACT
            )
        }
    }

    private fun extractHabit(message: String): ExtractedMemory? {
        val cnMatch = Regex("^我(一般|通常|经常|平时|常常)(.+)$")
            .matchEntire(message)
        if (cnMatch != null) {
            val prefix = cnMatch.groupValues[1]
            val value = cnMatch.groupValues[2].sanitizeValue()
            if (value != null) {
                return createMemory(
                    content = "用户$prefix$value",
                    category = if (looksLikeTimeHabit("$prefix$value")) CATEGORY_TIME else CATEGORY_OTHER
                )
            }
        }

        val enMatch = Regex("^i (?:usually|often|always|generally) (.+)$", RegexOption.IGNORE_CASE)
            .matchEntire(message)
        if (enMatch != null) {
            val value = enMatch.groupValues[1].sanitizeValue()
            if (value != null) {
                return createMemory(
                    content = "User usually $value",
                    category = if (looksLikeTimeHabitEn(value)) CATEGORY_TIME else CATEGORY_OTHER
                )
            }
        }

        return null
    }

    private fun extractSelfDescription(message: String): ExtractedMemory? {
        val cnPrefixed = Regex("^我(比较|很|挺|有点)(.+)$")
            .matchEntire(message)
            ?.let { match -> "${match.groupValues[1]}${match.groupValues[2]}" }
            ?.sanitizeValue()

        if (cnPrefixed != null) {
            return createMemory(content = "用户$cnPrefixed", category = CATEGORY_OTHER)
        }

        val cnIdentity = Regex("^我是个(.+?)(的人)?$")
            .matchEntire(message)
            ?.groupValues?.get(1)
            ?.sanitizeValue()

        if (cnIdentity != null) {
            return createMemory(content = "用户是个${cnIdentity}的人", category = CATEGORY_OTHER)
        }

        val enIdentity = Regex("^i'?m (?:a |an )?(.+?)( person)?$", RegexOption.IGNORE_CASE)
            .matchEntire(message)
            ?.groupValues?.get(1)
            ?.sanitizeValue()

        if (enIdentity != null) {
            return createMemory(content = "User is $enIdentity", category = CATEGORY_OTHER)
        }

        return null
    }

    private fun extractResponseStyle(message: String): ExtractedMemory? {
        val cnRequested = listOf(
            Regex("^(?:以后)?请(?:尽量)?(.+)$"),
            Regex("^希望你(.+)$")
        ).firstNotNullOfOrNull { pattern ->
            pattern.matchEntire(message)?.groupValues?.get(1)
        }?.sanitizeValue()

        if (cnRequested != null) {
            return createMemory(content = "用户偏好$cnRequested", category = CATEGORY_PREFERENCE)
        }

        val enRequested = listOf(
            Regex("^please (.+)$", RegexOption.IGNORE_CASE),
            Regex("^can you (.+)$", RegexOption.IGNORE_CASE),
            Regex("^i'?d like you to (.+)$", RegexOption.IGNORE_CASE)
        ).firstNotNullOfOrNull { pattern ->
            pattern.matchEntire(message)?.groupValues?.get(1)
        }?.sanitizeValue()

        if (enRequested != null) {
            return createMemory(content = "User prefers: $enRequested", category = CATEGORY_PREFERENCE)
        }

        val cnAvoid = Regex("^(?:回答时)?(?:别|不要)(太.+)$")
            .matchEntire(message)
            ?.groupValues?.get(1)
            ?.sanitizeValue()

        if (cnAvoid != null) {
            return createMemory(content = "用户不喜欢$cnAvoid", category = CATEGORY_PREFERENCE)
        }

        val enAvoid = Regex("^don'?t be (?:too |so )?(.+)$", RegexOption.IGNORE_CASE)
            .matchEntire(message)
            ?.groupValues?.get(1)
            ?.sanitizeValue()

        if (enAvoid != null) {
            return createMemory(content = "User doesn't want: $enAvoid", category = CATEGORY_PREFERENCE)
        }

        return null
    }

    private fun extractAvoidance(message: String): ExtractedMemory? {
        val cnValue = Regex("^不要再说(.+?)(了)?$")
            .matchEntire(message)
            ?.groupValues?.get(1)
            ?.sanitizeValue()

        if (cnValue != null) {
            return createMemory(content = "不要再说$cnValue", category = CATEGORY_PREFERENCE)
        }

        val enValue = Regex("^stop (?:saying|talking about|mentioning) (.+)$", RegexOption.IGNORE_CASE)
            .matchEntire(message)
            ?.groupValues?.get(1)
            ?.sanitizeValue()

        if (enValue != null) {
            return createMemory(content = "Stop mentioning $enValue", category = CATEGORY_PREFERENCE)
        }

        return null
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

    private fun splitToClauses(message: String): List<String> {
        return message
            .split('，', ',', '。', ';', '；', '\n')
            .map { it.trim() }
            .map(::normalizeClause)
            .filter { it.isNotEmpty() }
    }

    private fun normalizeClause(clause: String): String {
        return clause
            .removePrefix("也")
            .removePrefix("而且")
            .removePrefix("另外")
            .trim()
    }

    private fun looksLikeTimeHabit(content: String): Boolean {
        return TIME_HINTS.any { hint -> content.contains(hint) }
    }

    private fun looksLikeTimeHabitEn(content: String): Boolean {
        val lower = content.lowercase()
        return TIME_HINTS_EN.any { hint -> lower.contains(hint) }
    }

    companion object {
        private const val CATEGORY_FACT = "fact"
        private const val CATEGORY_PREFERENCE = "preference"
        private const val CATEGORY_TIME = "time"
        private const val CATEGORY_OTHER = "other"
        private const val LAYER_SHORT_TERM = "short_term"
        private const val SOURCE_RULE_EXTRACTOR = "rule_extractor"
        private val TIME_HINTS = listOf(
            "早上", "上午", "中午", "下午", "晚上", "凌晨",
            "点", "周末", "周一", "周二", "周三", "周四", "周五",
            "每天", "每日", "睡前", "起床后"
        )
        private val TIME_HINTS_EN = listOf(
            "morning", "afternoon", "evening", "night",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "daily", "every day", "weekend", "before bed", "after waking"
        )
    }
}
