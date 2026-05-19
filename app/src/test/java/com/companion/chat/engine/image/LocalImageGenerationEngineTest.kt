package com.companion.chat.engine.image

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalImageGenerationEngineTest {

    @Test
    fun `stable diffusion request seed and steps override config defaults`() {
        val engine = LocalImageGenerationEngine()
        val runtimeConfig = runtimeConfig(defaultSteps = 8, defaultSeed = 300L)

        val steps = engine.stableDiffusionSteps(
            request = ImageGenerationRequest(prompt = "cat", steps = 12, seed = 100L),
            config = ImageGenerationConfig(localSteps = 4, localSeed = 200L),
            runtimeConfig = runtimeConfig
        )
        val seed = engine.stableDiffusionSeed(
            request = ImageGenerationRequest(prompt = "cat", steps = 12, seed = 100L),
            config = ImageGenerationConfig(localSteps = 4, localSeed = 200L),
            runtimeConfig = runtimeConfig
        )

        assertEquals(12, steps)
        assertEquals(100L, seed)
    }

    @Test
    fun `stable diffusion falls back to config then runtime defaults`() {
        val engine = LocalImageGenerationEngine()
        val runtimeConfig = runtimeConfig(defaultSteps = 8, defaultSeed = 300L)

        assertEquals(
            4,
            engine.stableDiffusionSteps(
                request = ImageGenerationRequest(prompt = "cat", steps = 0),
                config = ImageGenerationConfig(localSteps = 4, localSeed = 200L),
                runtimeConfig = runtimeConfig
            )
        )
        assertEquals(
            200L,
            engine.stableDiffusionSeed(
                request = ImageGenerationRequest(prompt = "cat"),
                config = ImageGenerationConfig(localSeed = 200L),
                runtimeConfig = runtimeConfig
            )
        )
        assertEquals(
            8,
            engine.stableDiffusionSteps(
                request = ImageGenerationRequest(prompt = "cat", steps = 0),
                config = ImageGenerationConfig(localSteps = 0),
                runtimeConfig = runtimeConfig
            )
        )
        assertEquals(
            300L,
            engine.stableDiffusionSeed(
                request = ImageGenerationRequest(prompt = "cat"),
                config = ImageGenerationConfig(localSeed = null),
                runtimeConfig = runtimeConfig
            )
        )
    }

    private fun runtimeConfig(
        defaultSteps: Int,
        defaultSeed: Long?
    ): StableDiffusionRuntimeConfig {
        return StableDiffusionRuntimeConfig(
            modelName = "test",
            modelPath = "/tmp/model.safetensors",
            vaePath = "",
            taesdPath = "",
            loraPaths = emptyList(),
            defaultSteps = defaultSteps,
            defaultWidth = 512,
            defaultHeight = 512,
            defaultCfgScale = 1f,
            defaultSeed = defaultSeed,
            useVulkan = false
        )
    }
}
