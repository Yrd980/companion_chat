package com.companion.chat.engine

import com.companion.chat.data.privacy.PrivacyDataType
import com.companion.chat.data.privacy.PrivacyGate
import com.companion.chat.data.privacy.PrivacyGateDefaults
import com.companion.chat.data.privacy.PrivacyGateRequest
import com.companion.chat.engine.voice.CloudAsrConfigRepository
import com.companion.chat.engine.voice.CloudAsrResponseParser
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class CloudHttpAsrEngine private constructor(
    private val configProvider: () -> com.companion.chat.engine.voice.CloudAsrConfig,
    private val privacyGate: PrivacyGate,
    private val responseParser: CloudAsrResponseParser = CloudAsrResponseParser(),
    private val httpClient: CloudAsrHttpClient = UrlConnectionCloudAsrHttpClient()
) {
    constructor(
        configRepository: CloudAsrConfigRepository,
        privacyGate: PrivacyGate = PrivacyGateDefaults.denyRemoteByDefault(),
        responseParser: CloudAsrResponseParser = CloudAsrResponseParser(),
        httpClient: CloudAsrHttpClient = UrlConnectionCloudAsrHttpClient()
    ) : this(
        configProvider = configRepository::getConfig,
        privacyGate = privacyGate,
        responseParser = responseParser,
        httpClient = httpClient
    )

    internal constructor(
        config: com.companion.chat.engine.voice.CloudAsrConfig,
        privacyGate: PrivacyGate = PrivacyGateDefaults.denyRemoteByDefault(),
        responseParser: CloudAsrResponseParser = CloudAsrResponseParser(),
        httpClient: CloudAsrHttpClient = UrlConnectionCloudAsrHttpClient()
    ) : this(
        configProvider = { config },
        privacyGate = privacyGate,
        responseParser = responseParser,
        httpClient = httpClient
    )

    fun transcribe(audio: RecordedAudio): String {
        val config = configProvider()
        if (!config.isConfigured) {
            throw IllegalStateException("云 ASR 未配置")
        }
        privacyGate.requireAllowed(
            PrivacyGateRequest(
                dataType = PrivacyDataType.Audio,
                destination = config.baseUrl,
                reason = "Cloud ASR",
                localAlternative = "local SenseVoice recognition"
            )
        )
        NetworkEndpointPolicy.requireHttpsOrLoopback(config.baseUrl, "云 ASR")

        val boundary = "CompanionChatAsr${System.currentTimeMillis()}"
        val wavBytes = WavEncoder.encodePcm16Mono(audio)
        val requestBody = buildByteArray { output ->
            output.writeMultipartFile(boundary, config.requestFieldName, "speech.wav", "audio/wav", wavBytes)
            output.writeAscii("--$boundary--\r\n")
        }
        val response = httpClient.postMultipart(
            url = config.baseUrl,
            apiKey = config.apiKey,
            timeoutMillis = config.timeoutMillis,
            boundary = boundary,
            body = requestBody
        )

        if (response.statusCode !in 200..299) {
            throw IllegalStateException("云 ASR 请求失败 (${response.statusCode}): ${response.body}")
        }

        return responseParser.extractText(response.body, config.responseTextFieldPath)
    }

    private inline fun buildByteArray(writeBody: (OutputStream) -> Unit): ByteArray {
        return java.io.ByteArrayOutputStream().use { output ->
            writeBody(output)
            output.toByteArray()
        }
    }

    internal fun OutputStream.writeMultipartFile(
        boundary: String,
        fieldName: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray
    ) {
        writeAscii("--$boundary\r\n")
        writeAscii("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"\r\n")
        writeAscii("Content-Type: $contentType\r\n\r\n")
        write(bytes)
        writeAscii("\r\n")
    }

    internal fun OutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }
}

data class CloudAsrHttpResponse(
    val statusCode: Int,
    val body: String
)

interface CloudAsrHttpClient {
    fun postMultipart(
        url: String,
        apiKey: String,
        timeoutMillis: Int,
        boundary: String,
        body: ByteArray
    ): CloudAsrHttpResponse
}

private class UrlConnectionCloudAsrHttpClient : CloudAsrHttpClient {
    override fun postMultipart(
        url: String,
        apiKey: String,
        timeoutMillis: Int,
        boundary: String,
        body: ByteArray
    ): CloudAsrHttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            if (apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
        }

        try {
            connection.outputStream.use { it.write(body) }
            val statusCode = connection.responseCode
            val responseBody = if (statusCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            return CloudAsrHttpResponse(statusCode, responseBody)
        } finally {
            connection.disconnect()
        }
    }
}
