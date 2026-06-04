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
                summary = "图片生成 Base URL 未配置"
            )
        }
        val endpointError = runCatching {
            NetworkEndpointPolicy.requireHttpsOrLoopback(config.endpoint, "图片生成")
        }.exceptionOrNull()
        return if (endpointError == null) {
            ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.READY,
                summary = "HTTP 图片生成已配置",
                detail = config.endpoint
            )
        } else {
            ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = endpointError.message ?: "图片生成 endpoint 不可用",
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
                summary = "DreamLite 端侧真实出图尚未启用",
                detail = status.config.modelName
            )
            DreamLiteModelStatus.DirectoryNotConfigured -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "DreamLite 模型目录未配置",
                detail = config.modelPath
            )
            is DreamLiteModelStatus.InvalidConfig -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "DreamLite 配置无效：${status.message}",
                detail = config.modelPath
            )
            is DreamLiteModelStatus.MissingFiles -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "DreamLite 文件缺失：${status.fileNames.joinToString()}",
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
                summary = "本地图片模型已就绪",
                detail = status.config.modelName
            )
            StableDiffusionModelStatus.DirectoryNotConfigured -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "Stable Diffusion 模型目录未配置",
                detail = config.modelPath
            )
            is StableDiffusionModelStatus.InvalidConfig -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "Stable Diffusion 配置无效：${status.message}",
                detail = config.modelPath
            )
            is StableDiffusionModelStatus.MissingFiles -> ImageProviderReadiness(
                provider = config.provider,
                config = config,
                level = ImageProviderReadinessLevel.NOT_READY,
                summary = "Stable Diffusion 文件缺失：${status.fileNames.joinToString()}",
                detail = config.modelPath
            )
        }
    }
}
