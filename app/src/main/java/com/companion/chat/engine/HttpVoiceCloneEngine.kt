package com.companion.chat.engine

import android.content.Context
import com.companion.chat.engine.voice.VoiceCloneConfig
import com.companion.chat.engine.voice.VoiceCloneConfigRepository
import com.companion.chat.engine.voice.VoiceCloneEngine
import com.companion.chat.engine.voice.VoiceCloneProvider
import com.companion.chat.engine.voice.VoiceCloneRequest
import com.companion.chat.engine.voice.VoiceCloneResult
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class HttpVoiceCloneEngine private constructor(
    private val configProvider: () -> VoiceCloneConfig,
    private val httpClient: VoiceCloneHttpClient = UrlConnectionVoiceCloneHttpClient(),
    private val audioStore: VoiceCloneAudioStore
) : VoiceCloneEngine {

    constructor(
        context: Context,
        configProvider: () -> VoiceCloneConfig = VoiceCloneConfigRepository(context)::getConfig,
        httpClient: VoiceCloneHttpClient = UrlConnectionVoiceCloneHttpClient()
    ) : this(
        configProvider = configProvider,
        httpClient = httpClient,
        audioStore = VoiceCloneAudioStore { bytes ->
            val outputDirectory = File(context.filesDir, "generated_audio/http_clone").apply { mkdirs() }
            val outputFile = File(outputDirectory, "clone_${System.currentTimeMillis()}.wav")
            outputFile.writeBytes(bytes)
            outputFile
        }
    )

    internal constructor(
        config: VoiceCloneConfig,
        httpClient: VoiceCloneHttpClient,
        audioStore: VoiceCloneAudioStore
    ) : this(
        configProvider = { config },
        httpClient = httpClient,
        audioStore = audioStore
    )

    override suspend fun synthesize(request: VoiceCloneRequest): Result<VoiceCloneResult> {
        return runCatching {
            require(request.text.isNotBlank()) { "朗读文本为空" }
            val config = configProvider()
            require(config.isHttpCloneConfigured) { "HTTP 语音克隆后端未配置" }
            NetworkEndpointPolicy.requireHttpsOrLoopback(config.httpCloneBaseUrl, "HTTP 语音克隆")
            val response = httpClient.postJson(
                url = config.httpCloneBaseUrl,
                apiKey = config.httpCloneApiKey,
                timeoutMillis = config.httpCloneTimeoutMillis,
                body = buildRequestBody(config, request)
            )
            if (response.statusCode !in 200..299) {
                error("HTTP 语音克隆请求失败 (${response.statusCode}): ${response.body.take(160)}")
            }
            val audioFile = writeAudioResponse(response)
            VoiceCloneResult(
                provider = VoiceCloneProvider.HTTP_CLONE,
                audioUri = audioFile.toURI().toString(),
                fallbackToSystemTts = false,
                message = "HTTP 语音克隆合成完成"
            )
        }.recoverCatching { error ->
            VoiceCloneResult(
                provider = VoiceCloneProvider.HTTP_CLONE,
                fallbackToSystemTts = true,
                message = error.message ?: "HTTP 语音克隆失败"
            )
        }
    }

    private fun buildRequestBody(config: VoiceCloneConfig, request: VoiceCloneRequest): String {
        return JSONObject()
            .put("text", request.text)
            .put("voice", request.displayName.ifBlank { config.httpCloneVoice })
            .put("mode", "voice_clone")
            .put("prompt_audio_path", request.referenceAudioUri)
            .toString()
    }

    private fun writeAudioResponse(response: VoiceCloneHttpResponse): File {
        val bytes = response.audioBytes
            ?: response.body.toByteArray(Charsets.ISO_8859_1)
        require(bytes.isNotEmpty()) { "HTTP 语音克隆返回空音频" }
        return audioStore.save(bytes)
    }
}

internal fun interface VoiceCloneAudioStore {
    fun save(bytes: ByteArray): File
}

data class VoiceCloneHttpResponse(
    val statusCode: Int,
    val body: String = "",
    val audioBytes: ByteArray? = null
)

interface VoiceCloneHttpClient {
    fun postJson(
        url: String,
        apiKey: String,
        timeoutMillis: Int,
        body: String
    ): VoiceCloneHttpResponse
}

private class UrlConnectionVoiceCloneHttpClient : VoiceCloneHttpClient {
    override fun postJson(
        url: String,
        apiKey: String,
        timeoutMillis: Int,
        body: String
    ): VoiceCloneHttpResponse {
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
            val statusCode = connection.responseCode
            val isSuccess = statusCode in 200..299
            val stream = if (isSuccess) connection.inputStream else connection.errorStream ?: connection.inputStream
            val bytes = stream.use { it.readBytes() }
            return if (isSuccess) {
                VoiceCloneHttpResponse(statusCode = statusCode, audioBytes = bytes)
            } else {
                VoiceCloneHttpResponse(
                    statusCode = statusCode,
                    body = bytes.toString(Charsets.UTF_8)
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}
