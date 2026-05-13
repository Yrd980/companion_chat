package com.companion.chat.data.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedMemoryExtractorTest {

    private val extractor = RuleBasedMemoryExtractor()

    @Test
    fun `记住我叫小明提取事实记忆`() {
        val memories = extractor.extract(
            userMessage = "记住我叫小明",
            sessionId = "session-1"
        )

        assertEquals(1, memories.size)
        assertEquals("用户叫小明", memories.first().content)
        assertEquals("fact", memories.first().category)
    }

    @Test
    fun `我喜欢吃火锅提取偏好记忆`() {
        val memories = extractor.extract(
            userMessage = "我喜欢吃火锅",
            sessionId = "session-1"
        )

        assertEquals(1, memories.size)
        assertEquals("用户喜欢吃火锅", memories.first().content)
        assertEquals("preference", memories.first().category)
    }

    @Test
    fun `我住在北京提取事实记忆`() {
        val memories = extractor.extract(
            userMessage = "我住在北京",
            sessionId = "session-1"
        )

        assertEquals(1, memories.size)
        assertEquals("用户住在北京", memories.first().content)
        assertEquals("fact", memories.first().category)
    }

    @Test
    fun `不要再说这个了提取偏好记忆`() {
        val memories = extractor.extract(
            userMessage = "不要再说这个了",
            sessionId = "session-1"
        )

        assertEquals(1, memories.size)
        assertEquals("不要再说这个", memories.first().content)
        assertEquals("preference", memories.first().category)
    }

    @Test
    fun `普通消息不提取记忆`() {
        val memories = extractor.extract(
            userMessage = "今天天气不错",
            sessionId = "session-1"
        )

        assertTrue(memories.isEmpty())
    }
}
