package com.companion.chat.engine.image

internal object StableDiffusionNative {
    init {
        System.loadLibrary("companion_sd")
    }

    external fun generateTxt2ImgPng(
        modelPath: String,
        vaePath: String,
        taesdPath: String,
        loraPaths: Array<String>,
        prompt: String,
        negativePrompt: String,
        width: Int,
        height: Int,
        steps: Int,
        cfgScale: Float,
        seed: Long,
        useVulkan: Boolean
    ): ByteArray

    external fun systemInfo(): String
}
