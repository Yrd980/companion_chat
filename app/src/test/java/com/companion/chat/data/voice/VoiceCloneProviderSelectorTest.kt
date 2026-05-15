package com.companion.chat.data.voice

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCloneProviderSelectorTest {

    @Test
    fun `本地克隆占位会声明系统 TTS 回退`() = runBlocking {
        val selector = VoiceCloneProviderSelector()

        val result = selector.synthesize(
            provider = VoiceCloneProvider.LOCAL_CLONE_PLACEHOLDER,
            request = VoiceCloneRequest(text = "你好", roleId = "xia")
        ).getOrThrow()

        assertEquals(VoiceCloneProvider.LOCAL_CLONE_PLACEHOLDER, result.provider)
        assertTrue(result.fallbackToSystemTts)
        assertTrue(result.message.contains("尚未接入"))
    }
}
