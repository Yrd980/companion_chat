package com.companion.chat.engine.image

import com.companion.chat.engine.NetworkEndpointPolicy

enum class ImageProviderReadinessLevel {
    READY,
    NOT_READY
}

data class ImageProviderReadiness(
    val provider: ImageGenerationProvider,
    val config: ImageProviderConfigDetails,
    val level: ImageProviderReadinessLevel,
    val summary: String,
    val detail: String = ""
) {
    val capabilities: ImageGenerationCapabilities
        get() = config.capabilities

    val isUsable: Boolean
        get() = level == ImageProviderReadinessLevel.READY
}

object ImageProviderReadinessResolver {
    fun resolve(config: ImageGenerationConfig): ImageProviderReadiness {
        return when (config.provider) {
            ImageGenerationProvider.HTTP -> resolveHttp(config.httpProviderConfig)
            ImageGenerationProvider.LOCAL_DREAMLITE -> resolveDreamLite(
                config.localProviderConfig(ImageGenerationProvider.LOCAL_DREAMLITE)
            )
            ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP -> resolveStableDiffusion(
                config.localProviderConfig(ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP)
            )
        }
    }

    private fun resolveHttp(config: HttpImageProviderConfig): ImageProviderReadiness {
        if (!config.isEndpointConfigured) {
            return ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "Image generation Base URL is not configured"
            )
        }
        val endpointError = runCatching {
            NetworkEndpointPolicy.requireHttpsOrLoopback(config.endpoint, "Image generation")
        }.exceptionOrNull()
        return if (endpointError == null) {
            ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.READY,
                summary = "HTTP image generation is configured",
                detail = config.endpoint
            )
        } else {
            ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = endpointError.message ?: "Image generation endpoint is unavailable",
                detail = config.endpoint
            )
        }
    }

    private fun resolveDreamLite(config: LocalImageProviderConfig): ImageProviderReadiness {
        return when (val status = DreamLiteModelPackage.inspect(config.modelPath)) {
            is DreamLiteModelStatus.Ready -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "DreamLite on-device generation is not enabled yet",
                detail = status.config.modelName
            )
            DreamLiteModelStatus.DirectoryNotConfigured -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "DreamLite model directory is not configured",
                detail = config.modelPath
            )
            is DreamLiteModelStatus.InvalidConfig -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "Invalid DreamLite config: ${status.message}",
                detail = config.modelPath
            )
            is DreamLiteModelStatus.MissingFiles -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "Missing DreamLite files: ${status.fileNames.joinToString()}",
                detail = config.modelPath
            )
        }
    }

    private fun resolveStableDiffusion(config: LocalImageProviderConfig): ImageProviderReadiness {
        return when (val status = StableDiffusionModelPackage.inspect(config.modelPath)) {
            is StableDiffusionModelStatus.Ready -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.READY,
                summary = "Local image model is ready",
                detail = status.config.modelName
            )
            StableDiffusionModelStatus.DirectoryNotConfigured -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "Stable Diffusion model directory is not configured",
                detail = config.modelPath
            )
            is StableDiffusionModelStatus.InvalidConfig -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "Invalid Stable Diffusion config: ${status.message}",
                detail = config.modelPath
            )
            is StableDiffusionModelStatus.MissingFiles -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "Missing Stable Diffusion files: ${status.fileNames.joinToString()}",
                detail = config.modelPath
            )
        }
    }
}
