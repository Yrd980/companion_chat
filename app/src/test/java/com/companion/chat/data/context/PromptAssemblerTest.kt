package com.companion.chat.data.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptAssemblerTest {

    @Test
    fun `只有基础prompt时不拼空段落`() {
        val prompt = PromptAssembler().assemble(
            baseSystemPrompt = "你是一个友善的AI助手。",
            userPreferences = "",
            historySummary = ""
        )

        assertEquals("你是一个友善的AI助手。", prompt)
    }

    @Test
    fun `基础prompt偏好摘要顺序正确`() {
        val prompt = PromptAssembler().assemble(
            baseSystemPrompt = "基础提示词",
            userPreferences = "用户喜欢简洁回答",
            historySummary = "用户刚刚在讨论 Kotlin 项目。"
        )

        assertEquals(
            "基础提示词\n\n用户喜欢简洁回答\n\n之前对话的摘要：\n用户刚刚在讨论 Kotlin 项目。",
            prompt
        )
    }

    @Test
    fun `摘要为空时不出现摘要标题`() {
        val prompt = PromptAssembler().assemble(
            baseSystemPrompt = "基础提示词",
            userPreferences = "用户偏好",
            historySummary = ""
        )

        assertFalse(prompt.contains("之前对话的摘要"))
    }

    @Test
    fun `历史摘要与最近片段同时存在时顺序正确`() {
        val prompt = PromptAssembler().assemble(
            baseSystemPrompt = "基础提示词",
            userPreferences = "用户偏好",
            historySummary = "更早历史摘要",
            recentConversationSnippet = "用户：最近问题\n助手：最近回答"
        )

        assertEquals(
            "基础提示词\n\n用户偏好\n\n之前对话的摘要：\n更早历史摘要\n\n最近几轮对话片段：\n用户：最近问题\n助手：最近回答",
            prompt
        )
    }

    @Test
    fun `最近片段为空时不出现最近片段标题`() {
        val prompt = PromptAssembler().assemble(
            baseSystemPrompt = "基础提示词",
            userPreferences = "",
            historySummary = "历史摘要",
            recentConversationSnippet = ""
        )

        assertFalse(prompt.contains("最近几轮对话片段"))
    }

    @Test
    fun `仅有最近片段时不拼空摘要标题`() {
        val prompt = PromptAssembler().assemble(
            baseSystemPrompt = "基础提示词",
            userPreferences = "",
            historySummary = "",
            recentConversationSnippet = "用户：你好"
        )

        assertFalse(prompt.contains("之前对话的摘要"))
        assertTrue(prompt.contains("最近几轮对话片段"))
    }
}
