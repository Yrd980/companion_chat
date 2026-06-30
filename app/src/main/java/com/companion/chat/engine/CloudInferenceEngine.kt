package com.companion.chat.engine

import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.MessageRole
import com.companion.chat.data.privacy.PrivacyDataType
import com.companion.chat.data.privacy.PrivacyGate
import com.companion.chat.data.privacy.PrivacyGateDefaults
import com.companion.chat.data.privacy.PrivacyGateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class CloudInferenceEngine(
    private val privacyGate: PrivacyGate = PrivacyGateDefaults.denyRemoteByDefault()
) : InferenceEngine {

    private val _state = MutableStateFlow<InferenceState>(InferenceState.Idle)
    override val state: StateFlow<InferenceState> = _state.asStateFlow()

    private var currentConfig: EngineConfig? = null
    @Volatile
    private var currentConnection: HttpURLConnection? = null

    override suspend fun initialize(config: EngineConfig) {
        currentConfig = config
        _state.value = InferenceState.Ready
    }

    override fun getCurrentConfig(): EngineConfig? = currentConfig

    override suspend fun rebuildConversation(systemPrompt: String): Boolean {
        currentConfig = currentConfig?.copy(systemPrompt = systemPrompt)
        return true
    }

    override suspend fun rebuildConversationWithFallbackContext(systemPrompt: String): Boolean {
        return rebuildConversation(systemPrompt)
    }

    override suspend fun replayMessages(messages: List<ChatMessage>): Boolean = true

    override suspend fun warmUp(messages: List<ChatMessage>): Boolean {
        _state.value = InferenceState.Ready
        return true
    }

    override fun sendMessageStream(messages: List<ChatMessage>): Flow<String> = flow {
        val config = currentConfig ?: throw IllegalStateException("Cloud engine not initialized")
        _state.value = InferenceState.Generating("")

        val baseUrl = config.cloudBaseUrl.ifBlank { "https://token-plan-cn.xiaomimimo.com/v1" }
        privacyGate.requireAllowed(
            PrivacyGateRequest(
                dataType = PrivacyDataType.LlmPrompt,
                destination = baseUrl,
                reason = "Cloud LLM inference",
                localAlternative = "local llama.cpp or LiteRT-LM"
            )
        )
        NetworkEndpointPolicy.requireHttpsOrLoopback(baseUrl, "Cloud LLM")

        var reader: BufferedReader? = null
        try {
            val requestBody = buildRequestBody(config, messages)
            val connection = openConnection(config)
            currentConnection = connection

            withContext(Dispatchers.IO) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }
            }

            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                val errorBody = withContext(Dispatchers.IO) {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }
                throw IllegalStateException("Cloud LLM request failed ($statusCode): $errorBody")
            }

            reader = withContext(Dispatchers.IO) {
                connection.inputStream.bufferedReader()
            }

            var accumulated = ""
            for (token in parseSSEStream(reader)) {
                accumulated += token
                _state.value = InferenceState.Generating(accumulated)
                emit(token)
            }
        } catch (e: Exception) {
            _state.value = InferenceState.Error(e.message ?: "Unknown error")
            throw e
        } finally {
            withContext(Dispatchers.IO) {
                reader?.close()
            }
            currentConnection?.disconnect()
            currentConnection = null
            if (_state.value is InferenceState.Generating) {
                _state.value = InferenceState.Ready
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun cancel() {
        currentConnection?.disconnect()
        currentConnection = null
        if (_state.value is InferenceState.Generating) {
            _state.value = InferenceState.Ready
        }
    }

    override fun release() {
        cancel()
        currentConfig = null
        _state.value = InferenceState.Idle
    }

    private fun buildRequestBody(config: EngineConfig, messages: List<ChatMessage>): String {
        val json = JSONObject().apply {
            put("model", config.cloudModelName.ifBlank { "mimo-v2.5-pro" })
            put("stream", true)
            put("max_tokens", config.maxTokens)
            put("temperature", config.temperature.toDouble())
            put("top_p", config.topP.toDouble())

            val messagesArray = JSONArray()
            if (config.systemPrompt.isNotBlank()) {
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", config.systemPrompt)
                })
            }
            for (msg in messages) {
                val role = when (msg.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.SYSTEM -> "system"
                }
                if (msg.images.isEmpty()) {
                    messagesArray.put(JSONObject().apply {
                        put("role", role)
                        put("content", msg.content)
                    })
                } else {
                    val contentArray = JSONArray()
                    contentArray.put(JSONObject().apply {
                        put("type", "text")
                        put("text", msg.content)
                    })
                    for (imageUri in msg.images) {
                        val uriString = imageUri.toString()
                        if (uriString.startsWith("content://")) continue
                        contentArray.put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", uriString)
                            })
                        })
                    }
                    if (contentArray.length() > 1) {
                        messagesArray.put(JSONObject().apply {
                            put("role", role)
                            put("content", contentArray)
                        })
                    } else {
                        messagesArray.put(JSONObject().apply {
                            put("role", role)
                            put("content", msg.content)
                        })
                    }
                }
            }
            put("messages", messagesArray)
        }
        return json.toString()
    }

    private fun openConnection(config: EngineConfig): HttpURLConnection {
        val baseUrl = config.cloudBaseUrl.ifBlank {
            "https://token-plan-cn.xiaomimimo.com/v1"
        }
        val url = URL("$baseUrl/chat/completions")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 120_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val apiKey = config.cloudApiKey
            if (apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
        }
    }

    private fun parseSSEStream(reader: BufferedReader): Sequence<String> = sequence {
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line ?: continue
            if (!l.startsWith("data: ")) continue
            val data = l.removePrefix("data: ").trim()
            if (data == "[DONE]") break

            try {
                val json = JSONObject(data)
                val choices = json.optJSONArray("choices") ?: continue
                if (choices.length() == 0) continue
                val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                val content = delta.optString("content", "")
                if (content.isNotEmpty()) {
                    yield(content)
                }
            } catch (_: Exception) {
                // skip malformed SSE lines
            }
        }
    }
}
