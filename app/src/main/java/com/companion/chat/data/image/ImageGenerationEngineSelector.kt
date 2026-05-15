package com.companion.chat.data.image

class ImageGenerationEngineSelector(
    private val httpEngine: ImageGenerationEngine,
    private val localEngine: ImageGenerationEngine
) {
    suspend fun generate(
        request: ImageGenerationRequest,
        config: ImageGenerationConfig
    ): Result<String> {
        val provider = chooseProvider(config)
        return when (provider) {
            ImageGenerationProvider.HTTP -> httpEngine.generate(request, config.copy(provider = provider))
            ImageGenerationProvider.LOCAL_DREAMLITE -> localEngine.generate(request, config.copy(provider = provider))
        }
    }

    fun chooseProvider(config: ImageGenerationConfig): ImageGenerationProvider {
        if (config.provider == ImageGenerationProvider.LOCAL_DREAMLITE) {
            return ImageGenerationProvider.LOCAL_DREAMLITE
        }
        return if (config.baseUrl.isNotBlank()) ImageGenerationProvider.HTTP else config.provider
    }
}
