package com.companion.chat.data.image

data class ImageGenerationConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val requestTemplate: String = DEFAULT_REQUEST_TEMPLATE,
    val responseImageFieldPath: String = DEFAULT_RESPONSE_FIELD_PATH,
    val timeoutMillis: Int = 60_000
) {
    companion object {
        const val DEFAULT_REQUEST_TEMPLATE =
            """{"model":"{{model}}","prompt":"{{prompt}}","size":"1024x1024"}"""
        const val DEFAULT_RESPONSE_FIELD_PATH = "data.0.url"
    }
}

sealed class ImageGenerationState {
    data object Idle : ImageGenerationState()
    data object Generating : ImageGenerationState()
    data class Success(val imageUri: String) : ImageGenerationState()
    data class Error(val message: String) : ImageGenerationState()
}

enum class ImageGenerationPurpose {
    ROLE_AVATAR,
    ROLE_GALLERY,
    CHAT_SCENE
}
