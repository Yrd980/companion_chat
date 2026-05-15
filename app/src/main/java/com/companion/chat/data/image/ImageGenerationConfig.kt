package com.companion.chat.data.image

data class ImageGenerationConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val provider: ImageGenerationProvider = ImageGenerationProvider.HTTP,
    val localModelPath: String = "",
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

enum class ImageGenerationProvider {
    HTTP,
    LOCAL_DREAMLITE
}

data class ImageGenerationRequest(
    val prompt: String,
    val negativePrompt: String = "",
    val size: String = "1024x1024",
    val seed: Long? = null,
    val steps: Int = 24,
    val roleId: String = "",
    val purpose: ImageGenerationPurpose = ImageGenerationPurpose.CHAT_SCENE
)

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
