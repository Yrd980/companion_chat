package com.companion.chat.engine

import android.content.Context
import com.companion.chat.engine.InferenceEngine
import com.companion.chat.engine.ModelRuntime

class InferenceEngineFactory(
    private val context: Context
) {
    fun create(runtime: ModelRuntime): InferenceEngine {
        return when (runtime) {
            ModelRuntime.LLAMA_CPP_GGUF -> LlamaCppInferenceEngine(context)
            ModelRuntime.LITERT_LM -> LiteRTLMInferenceEngine(context)
        }
    }
}
