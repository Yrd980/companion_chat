package com.companion.chat.engine

import android.content.Context
import com.companion.chat.data.privacy.PrivacyGate
import com.companion.chat.data.privacy.PrivacyGateDefaults

class InferenceEngineFactory(
    private val context: Context,
    private val privacyGate: PrivacyGate = PrivacyGateDefaults.denyRemoteByDefault()
) {
    fun create(runtime: ModelRuntime): InferenceEngine {
        return when (runtime) {
            ModelRuntime.LLAMA_CPP_GGUF -> LlamaCppInferenceEngine(context)
            ModelRuntime.LITERT_LM -> LiteRTLMInferenceEngine(context)
            ModelRuntime.CLOUD_MIMO -> CloudInferenceEngine(privacyGate)
        }
    }
}
