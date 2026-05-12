package com.companion.chat.engine

import android.content.Context
import android.util.Log
import com.companion.chat.data.engine.DefaultModelConfig
import com.companion.chat.data.engine.EngineConfig
import com.companion.chat.data.engine.InferenceEngine
import com.companion.chat.data.engine.InferenceState
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.MessageRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class LlamaCppInferenceEngine(private val context: Context) : InferenceEngine {
    companion object {
        private const val TAG = "LlamaCppEngine"
        private const val UnsupportedImageMessage = "当前 GGUF 文本模型不支持图片输入。"
    }

    private val runtimeDispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "llama-cpp-runtime").apply { isDaemon = true }
        }.asCoroutineDispatcher()

    private val _state = MutableStateFlow<InferenceState>(InferenceState.Idle)
    override val state: StateFlow<InferenceState> = _state.asStateFlow()

    private var handle: Long = 0L
    private var currentConfig: EngineConfig? = null

    private fun logToFile(msg: String) {
        try {
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val line = "[$time] $msg\n"
            context.openFileOutput("llama_engine_log.txt", Context.MODE_APPEND).use { fos ->
                fos.write(line.toByteArray())
            }
            Log.i(TAG, msg)
        } catch (e: Exception) {
            Log.e(TAG, "写日志失败: ${e.message}")
        }
    }

    private fun defaultModelPath(): String {
        val externalDir = context.getExternalFilesDir(DefaultModelConfig.ExternalModelsDir)
        return if (externalDir != null) {
            File(externalDir, DefaultModelConfig.ModelFileName).absolutePath
        } else {
            File(File(context.filesDir, DefaultModelConfig.ExternalModelsDir), DefaultModelConfig.ModelFileName).absolutePath
        }
    }

    override suspend fun initialize(config: EngineConfig) = withContext(runtimeDispatcher) {
        val resolvedConfig = config.copy(
            modelPath = config.modelPath.ifBlank { defaultModelPath() },
            systemPrompt = config.systemPrompt.ifBlank { DefaultModelConfig.DefaultSystemPrompt }
        )
        if (_state.value is InferenceState.Ready && currentConfig == resolvedConfig) {
            return@withContext
        }

        _state.value = InferenceState.Initializing
        releaseLoadedModel()

        val modelFile = File(resolvedConfig.modelPath)
        logToFile("=== 开始初始化 llama.cpp 引擎 ===")
        logToFile("llama.cpp systemInfo: ${LlamaCppNative.systemInfo()}")
        logToFile("模型路径: ${modelFile.absolutePath}")
        logToFile("模型文件存在: ${modelFile.exists()}")
        logToFile("模型可读: ${modelFile.canRead()}")
        logToFile("模型文件大小: ${modelFile.length()} bytes")

        when {
            !modelFile.exists() -> {
                val message = "GGUF 模型文件不存在: ${modelFile.absolutePath}"
                logToFile(message)
                _state.value = InferenceState.Error(message)
                return@withContext
            }
            !modelFile.canRead() -> {
                val message = "GGUF 模型文件不可读: ${modelFile.absolutePath}"
                logToFile(message)
                _state.value = InferenceState.Error(message)
                return@withContext
            }
            modelFile.length() <= 0L -> {
                val message = "GGUF 模型文件为空: ${modelFile.absolutePath}"
                logToFile(message)
                _state.value = InferenceState.Error(message)
                return@withContext
            }
        }

        try {
            handle = LlamaCppNative.loadModel(
                resolvedConfig.modelPath,
                resolvedConfig.contextSize,
                resolvedConfig.systemPrompt
            )
            currentConfig = resolvedConfig
            _state.value = InferenceState.Ready
            logToFile("=== llama.cpp 引擎初始化完成，状态: Ready ===")
        } catch (e: Exception) {
            releaseLoadedModel()
            val message = "GGUF 模型初始化失败: ${e.message}"
            logToFile("!!! $message")
            logToFile("异常类型: ${e.javaClass.simpleName}")
            logToFile("堆栈: ${e.stackTraceToString().take(1000)}")
            _state.value = InferenceState.Error(message)
        }
    }

    override fun sendMessageStream(messages: List<ChatMessage>): Flow<String> = callbackFlow {
        val activeHandle = handle
        if (activeHandle == 0L) {
            close(IllegalStateException("llama.cpp 引擎未初始化"))
            return@callbackFlow
        }

        val userMessagesWithImages = messages.filter { it.role == MessageRole.USER && it.images.isNotEmpty() }
        if (userMessagesWithImages.isNotEmpty()) {
            logToFile("检测到图片输入，当前 GGUF 文本模型不支持")
            trySend(UnsupportedImageMessage)
            close()
            return@callbackFlow
        }

        val promptMessages = messages.toPromptMessages()
        if (promptMessages.none { it.role == MessageRole.USER }) {
            close(IllegalStateException("没有可发送的用户文本消息"))
            return@callbackFlow
        }

        val roles = promptMessages.map { it.role.toNativeRole() }.toTypedArray()
        val contents = promptMessages.map { it.content }.toTypedArray()
        val config = currentConfig ?: EngineConfig(modelPath = "")
        logToFile("发送推理请求: promptMessages=${promptMessages.size}, maxTokens=${config.maxTokens}, contextSize=${config.contextSize}")

        _state.value = InferenceState.Generating()
        val job = launch(runtimeDispatcher) {
            try {
                LlamaCppNative.generate(
                    activeHandle,
                    roles,
                    contents,
                    config.maxTokens,
                    config.temperature,
                    config.topK,
                    config.topP,
                    object : LlamaCppNative.TokenCallback {
                        override fun onTokenBytes(bytes: ByteArray) {
                            trySend(bytes.toString(Charsets.UTF_8))
                        }

                        override fun onPerformanceLog(message: String) {
                            logToFile(message)
                        }
                    }
                )
                logToFile("推理完成")
            } catch (e: CancellationException) {
                LlamaCppNative.cancel(activeHandle)
                logToFile("推理被取消")
                throw e
            } catch (e: Exception) {
                val message = "推理出错: ${e.message}"
                logToFile("$message (${e.javaClass.simpleName})")
                trySend("[$message]")
            } finally {
                _state.value = InferenceState.Ready
                close()
            }
        }

        awaitClose {
            job.cancel()
            cancel()
        }
    }

    private fun List<ChatMessage>.toPromptMessages(): List<ChatMessage> {
        val nonStreamingText = filter { !it.isStreaming && it.content.isNotBlank() }
        val firstUserIndex = nonStreamingText.indexOfFirst { it.role == MessageRole.USER }
        val conversationMessages = if (firstUserIndex >= 0) {
            nonStreamingText.drop(firstUserIndex)
        } else {
            nonStreamingText
        }
        val recentMessages = conversationMessages.takeLast(DefaultModelConfig.MaxPromptMessages)
        val firstRecentUserIndex = recentMessages.indexOfFirst { it.role == MessageRole.USER }
        return if (firstRecentUserIndex >= 0) {
            recentMessages.drop(firstRecentUserIndex)
        } else {
            recentMessages
        }
    }

    private fun MessageRole.toNativeRole(): String = when (this) {
        MessageRole.USER -> "user"
        MessageRole.ASSISTANT -> "assistant"
        MessageRole.SYSTEM -> "system"
    }

    override fun cancel() {
        val activeHandle = handle
        if (activeHandle != 0L) {
            LlamaCppNative.cancel(activeHandle)
        }
    }

    override fun release() {
        releaseLoadedModel()
        _state.value = InferenceState.Idle
    }

    private fun releaseLoadedModel() {
        val activeHandle = handle
        if (activeHandle != 0L) {
            try {
                LlamaCppNative.releaseModel(activeHandle)
            } catch (e: Exception) {
                logToFile("释放 llama.cpp 引擎出错: ${e.message}")
            }
        }
        handle = 0L
        currentConfig = null
    }
}
