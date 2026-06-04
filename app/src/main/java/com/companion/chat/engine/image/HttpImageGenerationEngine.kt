package com.companion.chat.engine.image

import android.content.Context
import com.companion.chat.engine.NetworkEndpointPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

class HttpImageGenerationEngine : ImageGenerationEngine {

    constructor(context: Context) : this(
        httpClient = UrlConnectionImageHttpClient(),
        imageStore = GeneratedImageStore { bytes, purpose ->
            ImageFileStore(context).saveBytes(bytes, purpose)
        }
    )

    internal constructor(
        httpClient: ImageGenerationHttpClient,
        imageStore: GeneratedImageStore
    ) {
        this.httpClient = httpClient
        this.imageStore = imageStore
    }

    private val httpClient: ImageGenerationHttpClient
    private val imageStore: GeneratedImageStore
    private val _state = MutableStateFlow<ImageGenerationState>(ImageGenerationState.Idle)
    override val state: StateFlow<ImageGenerationState> = _state.asStateFlow()

    override suspend fun generate(
        prompt: String,
        config: ImageGenerationConfig,
        purpose: ImageGenerationPurpose
    ): Result<String> = withContext(Dispatchers.IO) {
        val providerConfig = config.httpProviderConfig
        if (!providerConfig.isEndpointConfigured) {
            val error = "图片生成 Base URL 未配置"
            _state.value = ImageGenerationState.Error(error)
            return@withContext Result.failure(IllegalStateException(error))
        }
        if (prompt.isBlank()) {
            val error = "图片生成提示词不能为空"
            _state.value = ImageGenerationState.Error(error)
            return@withContext Result.failure(IllegalArgumentException(error))
        }

        _state.value = ImageGenerationState.Generating
        runCatching {
            NetworkEndpointPolicy.requireHttpsOrLoopback(providerConfig.endpoint, "图片生成")
            val response = httpClient.postJson(
                url = providerConfig.endpoint,
                apiKey = providerConfig.apiKey,
                body = renderTemplate(providerConfig.requestTemplate, providerConfig.model, prompt),
                timeoutMillis = providerConfig.timeoutMillis
            )
            val imageValue = readFieldPath(JSONObject(response), providerConfig.responseImageFieldPath)
                ?: error("响应中未找到图片字段: ${providerConfig.responseImageFieldPath}")
            val uri = when {
                imageValue.startsWith("http://") || imageValue.startsWith("https://") ->
                    saveBytes(httpClient.getBytes(imageValue, providerConfig.timeoutMillis), purpose)
                imageValue.startsWith("data:image") ->
                    saveBase64Image(imageValue.substringAfter(","), purpose)
                else -> saveBase64Image(imageValue, purpose)
            }
            _state.value = ImageGenerationState.Success(uri)
            uri
        }.onFailure { error ->
            _state.value = ImageGenerationState.Error(error.message ?: "图片生成失败")
        }
    }

    private fun renderTemplate(template: String, model: String, prompt: String): String {
        return template
            .replace("{{model}}", escapeJson(model))
            .replace("{{prompt}}", escapeJson(prompt))
    }

    private fun escapeJson(value: String): String =
        JSONObject.quote(value).removePrefix("\"").removeSuffix("\"")

    private fun readFieldPath(root: JSONObject, path: String): String? {
        var current: Any = root
        path.split(".").filter { it.isNotBlank() }.forEach { part ->
            current = when (current) {
                is JSONObject -> current.opt(part) ?: return null
                is JSONArray -> current.opt(part.toIntOrNull() ?: return null)
                    ?: return null
                else -> return null
            }
        }
        return current.toString().takeIf { it.isNotBlank() && it != "null" }
    }

    private fun saveBase64Image(base64: String, purpose: ImageGenerationPurpose): String =
        saveBytes(Base64.getMimeDecoder().decode(base64), purpose)

    private fun saveBytes(bytes: ByteArray, purpose: ImageGenerationPurpose): String =
        imageStore.saveBytes(bytes, purpose)
}

internal fun interface GeneratedImageStore {
    fun saveBytes(bytes: ByteArray, purpose: ImageGenerationPurpose): String
}

internal interface ImageGenerationHttpClient {
    fun postJson(
        url: String,
        apiKey: String,
        body: String,
        timeoutMillis: Int
    ): String

    fun getBytes(url: String, timeoutMillis: Int): ByteArray
}

private class UrlConnectionImageHttpClient : ImageGenerationHttpClient {
    override fun postJson(
        url: String,
        apiKey: String,
        body: String,
        timeoutMillis: Int
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            val response = stream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) {
                error("图片生成 HTTP ${connection.responseCode}: ${response.take(160)}")
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    override fun getBytes(url: String, timeoutMillis: Int): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("图片下载 HTTP ${connection.responseCode}")
            }
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}
