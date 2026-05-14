package com.companion.chat.data.image

import kotlinx.coroutines.flow.StateFlow

interface ImageGenerationEngine {
    val state: StateFlow<ImageGenerationState>

    suspend fun generate(
        prompt: String,
        config: ImageGenerationConfig,
        purpose: ImageGenerationPurpose = ImageGenerationPurpose.CHAT_SCENE
    ): Result<String>
}
