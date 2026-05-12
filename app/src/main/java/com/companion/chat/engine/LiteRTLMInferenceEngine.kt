package com.companion.chat.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.companion.chat.data.engine.BackendType
import com.companion.chat.data.engine.EngineConfig
import com.companion.chat.data.engine.InferenceEngine
import com.companion.chat.data.engine.InferenceState
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.MessageRole
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.EngineConfig as LiteRTConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LiteRTLMInferenceEngine(private val context: Context) : InferenceEngine {

    companion object {
        private const val TAG = "LiteRTLMEngine"
        private const val DEFAULT_MODEL_FILE = "gemma-4-E2B-it.litertlm"
    }

    private val _state = MutableStateFlow<InferenceState>(InferenceState.Idle)
    override val state: StateFlow<InferenceState> = _state.asStateFlow()

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentConfig: EngineConfig? = null

    private fun logToFile(msg: String) {
        try {
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val line = "[$time] $msg\n"
            context.openFileOutput("engine_log.txt", android.content.Context.MODE_APPEND).use { fos ->
                fos.write(line.toByteArray())
            }
            Log.i(TAG, msg)
        } catch (e: Exception) {
            Log.e(TAG, "写日志失败: ${e.message}")
        }
    }

    private fun getDefaultModelPath(): String {
        val internalDir = File(context.filesDir, "models")
        return "${internalDir.absolutePath}/$DEFAULT_MODEL_FILE"
    }

    private fun getExternalModelPath(): String {
        val externalDir = context.getExternalFilesDir("models")
        return if (externalDir != null) {
            "${externalDir.absolutePath}/$DEFAULT_MODEL_FILE"
        } else ""
    }

    private suspend fun ensureModelInInternalStorage(): String = withContext(Dispatchers.IO) {
        val internalPath = getDefaultModelPath()
        val internalFile = File(internalPath)

        if (internalFile.exists() && internalFile.length() > 0) {
            logToFile("模型已在内部存储: $internalPath (${internalFile.length()} bytes)")
            return@withContext internalPath
        }

        val externalPath = getExternalModelPath()
        val externalFile = File(externalPath)

        if (!externalFile.exists()) {
            logToFile("外部存储也无模型文件: $externalPath")
            return@withContext internalPath
        }

        logToFile("模型在外部存储，开始复制到内部存储...")
        logToFile("源: $externalPath (${externalFile.length()} bytes)")
        logToFile("目标: $internalPath")

        internalFile.parentFile?.mkdirs()

        try {
            externalFile.inputStream().use { input ->
                internalFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024 * 1024)
                    var copied = 0L
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        copied += bytesRead
                        if (copied % (100 * 1024 * 1024) < buffer.size) {
                            logToFile("复制进度: ${copied / 1024 / 1024}MB / ${externalFile.length() / 1024 / 1024}MB")
                        }
                    }
                    output.flush()
                }
            }
            logToFile("模型复制完成: ${internalFile.length()} bytes")
        } catch (e: Exception) {
            logToFile("模型复制失败: ${e.message}")
            if (internalFile.exists()) internalFile.delete()
        }

        internalPath
    }

    private fun uriToImageBytes(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
                val maxSize = 1024
                val scaled = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                    val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).toInt(),
                        (bitmap.height * ratio).toInt(),
                        true
                    )
                } else {
                    bitmap
                }
                val output = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.PNG, 100, output)
                if (scaled !== bitmap) scaled.recycle()
                bitmap.recycle()
                output.toByteArray()
            }
        } catch (e: Exception) {
            logToFile("图片转换失败: ${uri} - ${e.message}")
            null
        }
    }

    override suspend fun initialize(config: EngineConfig) = withContext(Dispatchers.IO) {
        if (_state.value is InferenceState.Ready && currentConfig == config) {
            return@withContext
        }

        _state.value = InferenceState.Initializing
        release()

        try {
            val modelPath = config.modelPath.ifBlank {
                ensureModelInInternalStorage()
            }
            val modelFile = File(modelPath)
            logToFile("=== 开始初始化引擎 ===")
            logToFile("模型路径: $modelPath")
            logToFile("模型文件存在: ${modelFile.exists()}")
            logToFile("模型文件大小: ${modelFile.length()} bytes")

            if (!modelFile.exists() || modelFile.length() == 0L) {
                _state.value = InferenceState.Error("模型文件不存在或为空: $modelPath")
                logToFile("模型文件不存在或为空，初始化终止")
                return@withContext
            }

            val backend = when (config.backend) {
                BackendType.GPU -> {
                    logToFile("使用 GPU 后端")
                    Backend.GPU()
                }
                else -> {
                    logToFile("使用 CPU 后端")
                    Backend.CPU()
                }
            }

            logToFile("创建 EngineConfig...")
            val litertConfig = LiteRTConfig(
                modelPath = modelPath,
                backend = backend,
                visionBackend = Backend.CPU(),
                maxNumImages = 4,
                cacheDir = context.cacheDir.absolutePath
            )
            logToFile("EngineConfig 创建成功 (含 visionBackend=CPU, maxNumImages=4)")

            logToFile("创建 Engine...")
            val eng = Engine(litertConfig)
            logToFile("Engine 创建成功")

            logToFile("Engine.initialize() 开始...")
            eng.initialize()
            logToFile("Engine.initialize() 完成")

            val systemPrompt = config.systemPrompt.ifBlank {
                "你是一个友善的AI助手，请用中文回答用户的问题。"
            }
            logToFile("系统提示词: ${systemPrompt.take(50)}...")

            val convConfig = ConversationConfig(
                systemInstruction = Contents.of(systemPrompt),
                samplerConfig = SamplerConfig(
                    topK = 40,
                    topP = 0.95,
                    temperature = 0.7
                )
            )
            logToFile("ConversationConfig 创建成功")

            logToFile("创建 Conversation...")
            val conv = eng.createConversation(convConfig)
            logToFile("Conversation 创建成功")

            engine = eng
            conversation = conv
            currentConfig = config
            _state.value = InferenceState.Ready

            logToFile("=== 引擎初始化完成，状态: Ready ===")
        } catch (e: Exception) {
            logToFile("!!! 引擎初始化失败 !!!")
            logToFile("异常类型: ${e.javaClass.simpleName}")
            logToFile("异常信息: ${e.message}")
            logToFile("堆栈: ${e.stackTraceToString().take(500)}")
            _state.value = InferenceState.Error("模型初始化失败: ${e.message}")
        }
    }

    override fun sendMessageStream(messages: List<ChatMessage>): Flow<String> = callbackFlow {
        val conv = conversation
        if (conv == null) {
            logToFile("sendMessageStream: 引擎未初始化")
            close(IllegalStateException("引擎未初始化"))
            return@callbackFlow
        }

        _state.value = InferenceState.Generating()

        val lastUserMessage = messages.lastOrNull { it.role == MessageRole.USER }
        if (lastUserMessage == null) {
            logToFile("sendMessageStream: 没有用户消息")
            close(IllegalStateException("没有用户消息"))
            return@callbackFlow
        }

        logToFile("开始推理: ${lastUserMessage.content.take(50)}...")

        val imageBytesList = lastUserMessage.images.mapNotNull { uri ->
            uriToImageBytes(uri)
        }
        if (imageBytesList.isNotEmpty()) {
            logToFile("检测到 ${imageBytesList.size} 张图片，构建多模态消息")
        }

        try {
            val flow = if (imageBytesList.isNotEmpty()) {
                val contentList = mutableListOf<Content>()
                imageBytesList.forEach { bytes ->
                    contentList.add(Content.ImageBytes(bytes))
                }
                if (lastUserMessage.content.isNotBlank()) {
                    contentList.add(Content.Text(lastUserMessage.content))
                }
                val contents = Contents.of(contentList)
                logToFile("发送多模态消息 (${contentList.size} 个内容块)")
                conv.sendMessageAsync(contents)
            } else {
                logToFile("发送纯文本消息")
                conv.sendMessageAsync(lastUserMessage.content)
            }

            flow.collect { message ->
                val text = message.toString()
                if (text.isNotEmpty()) {
                    trySend(text)
                }
            }
            logToFile("推理完成")
        } catch (e: CancellationException) {
            logToFile("推理被取消")
            throw e
        } catch (e: Exception) {
            logToFile("推理出错: ${e.javaClass.simpleName}: ${e.message}")
            trySend("[推理出错: ${e.message}]")
        } finally {
            _state.value = InferenceState.Ready
            close()
        }

        awaitClose {
            cancel()
        }
    }

    override fun cancel() {
        try {
            conversation?.cancelProcess()
        } catch (e: Exception) {
            logToFile("取消推理出错: ${e.message}")
        }
    }

    override fun release() {
        try {
            conversation?.close()
            engine?.close()
        } catch (e: Exception) {
            logToFile("释放引擎出错: ${e.message}")
        }
        conversation = null
        engine = null
        currentConfig = null
        _state.value = InferenceState.Idle
    }
}
