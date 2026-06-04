package com.companion.chat.engine.image

data class ImageGenerationConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val provider: ImageGenerationProvider = ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP,
    val localModelPath: String = "",
    val localWidth: Int = 512,
    val localHeight: Int = 512,
    val localSteps: Int = 4,
    val localCfgScale: Float = 1.0f,
    val localSeed: Long? = null,
    val localUseVulkan: Boolean = true,
    val requestTemplate: String = DEFAULT_REQUEST_TEMPLATE,
    val responseImageFieldPath: String = DEFAULT_RESPONSE_FIELD_PATH,
    val timeoutMillis: Int = 60_000
) {
    val httpProviderConfig: HttpImageProviderConfig
        get() = HttpImageProviderConfig(
            endpoint = baseUrl.trim(),
            apiKey = apiKey.trim(),
            model = model.trim(),
            requestTemplate = requestTemplate.ifBlank { DEFAULT_REQUEST_TEMPLATE },
            responseImageFieldPath = responseImageFieldPath.ifBlank { DEFAULT_RESPONSE_FIELD_PATH },
            timeoutMillis = timeoutMillis
        )

    fun localProviderConfig(
        provider: ImageGenerationProvider = this.provider
    ): LocalImageProviderConfig {
        val localProvider = if (provider.isLocal) {
            provider
        } else {
            ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP
        }
        return LocalImageProviderConfig(
            provider = localProvider,
            modelPath = localModelPath.trim(),
            width = localWidth,
            height = localHeight,
            steps = localSteps,
            cfgScale = localCfgScale,
            seed = localSeed,
            useVulkan = localUseVulkan
        )
    }

    fun providerConfig(
        provider: ImageGenerationProvider = this.provider
    ): ImageProviderConfigDetails {
        return when (provider) {
            ImageGenerationProvider.HTTP -> httpProviderConfig
            ImageGenerationProvider.LOCAL_DREAMLITE,
            ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP -> localProviderConfig(provider)
        }
    }

    companion object {
        const val DEFAULT_REQUEST_TEMPLATE =
            """{"model":"{{model}}","prompt":"{{prompt}}","size":"1024x1024"}"""
        const val DEFAULT_RESPONSE_FIELD_PATH = "data.0.url"
    }
}

enum class ImageGenerationProvider {
    HTTP,
    LOCAL_DREAMLITE,
    LOCAL_STABLE_DIFFUSION_CPP;

    val isLocal: Boolean
        get() = this != HTTP
}

sealed interface ImageProviderConfigDetails {
    val provider: ImageGenerationProvider
    val capabilities: ImageGenerationCapabilities
}

data class HttpImageProviderConfig(
    val endpoint: String = "",
    val apiKey: String = "",
    val model: String = "",
    val requestTemplate: String = ImageGenerationConfig.DEFAULT_REQUEST_TEMPLATE,
    val responseImageFieldPath: String = ImageGenerationConfig.DEFAULT_RESPONSE_FIELD_PATH,
    val timeoutMillis: Int = 60_000
) : ImageProviderConfigDetails {
    override val provider: ImageGenerationProvider = ImageGenerationProvider.HTTP
    override val capabilities: ImageGenerationCapabilities = ImageGenerationCapabilities.HTTP

    val isEndpointConfigured: Boolean
        get() = endpoint.isNotBlank()

    val isApiKeyConfigured: Boolean
        get() = apiKey.isNotBlank()

    val isModelConfigured: Boolean
        get() = model.isNotBlank()
}

data class LocalImageProviderConfig(
    override val provider: ImageGenerationProvider = ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP,
    val modelPath: String = "",
    val width: Int = 512,
    val height: Int = 512,
    val steps: Int = 4,
    val cfgScale: Float = 1.0f,
    val seed: Long? = null,
    val useVulkan: Boolean = true
) : ImageProviderConfigDetails {
    override val capabilities: ImageGenerationCapabilities = ImageGenerationCapabilities.forProvider(provider)
}

data class ImageGenerationCapabilities(
    val supportsTextToImage: Boolean,
    val usesHttpEndpoint: Boolean,
    val usesApiKey: Boolean,
    val usesModelName: Boolean,
    val usesLocalModelPackage: Boolean,
    val usesNegativePrompt: Boolean,
    val usesSeed: Boolean,
    val usesImageSize: Boolean,
    val usesVulkan: Boolean
) {
    companion object {
        val HTTP = ImageGenerationCapabilities(
            supportsTextToImage = true,
            usesHttpEndpoint = true,
            usesApiKey = true,
            usesModelName = true,
            usesLocalModelPackage = false,
            usesNegativePrompt = false,
            usesSeed = false,
            usesImageSize = false,
            usesVulkan = false
        )

        val DREAMLITE = ImageGenerationCapabilities(
            supportsTextToImage = false,
            usesHttpEndpoint = false,
            usesApiKey = false,
            usesModelName = false,
            usesLocalModelPackage = true,
            usesNegativePrompt = false,
            usesSeed = false,
            usesImageSize = false,
            usesVulkan = false
        )

        val STABLE_DIFFUSION_CPP = ImageGenerationCapabilities(
            supportsTextToImage = true,
            usesHttpEndpoint = false,
            usesApiKey = false,
            usesModelName = false,
            usesLocalModelPackage = true,
            usesNegativePrompt = true,
            usesSeed = true,
            usesImageSize = true,
            usesVulkan = true
        )

        fun forProvider(provider: ImageGenerationProvider): ImageGenerationCapabilities {
            return when (provider) {
                ImageGenerationProvider.HTTP -> HTTP
                ImageGenerationProvider.LOCAL_DREAMLITE -> DREAMLITE
                ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP -> STABLE_DIFFUSION_CPP
            }
        }
    }
}

data class ImageGenerationRequest(
    val prompt: String,
    val negativePrompt: String = "",
    val size: String = "1024x1024",
    val seed: Long? = null,
    val steps: Int = 0,
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
