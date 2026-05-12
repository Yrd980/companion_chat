package com.companion.chat.ui.chat

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.chat.data.engine.DefaultModelConfig
import com.companion.chat.data.engine.EngineConfig
import com.companion.chat.data.engine.InferenceState
import com.companion.chat.data.engine.VoiceInputEvent
import com.companion.chat.data.engine.VoiceOutputState
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.MessageRole
import com.companion.chat.engine.AndroidVoiceInputEngine
import com.companion.chat.engine.AndroidVoiceOutputEngine
import com.companion.chat.engine.LlamaCppInferenceEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val selectedImages: List<Uri> = emptyList(),
    val isGenerating: Boolean = false,
    val isVoiceListening: Boolean = false,
    val isVoiceSpeaking: Boolean = false,
    val engineState: InferenceState = InferenceState.Idle,
    val showVoicePermissionDialog: Boolean = false,
    val diagnosticLog: String = ""
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val ChatPrefsName = "chat_history"
        private const val MessagesKey = "messages"
        private val StopMarkers = listOf(
            "<end_of_turn>",
            "<|im_end|>",
            "<end_of_",
            "</s>",
            "<eos>"
        )
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val inferenceEngine = LlamaCppInferenceEngine(application)
    val voiceInputEngine = AndroidVoiceInputEngine(application)
    val voiceOutputEngine = AndroidVoiceOutputEngine(application)

    private var generateJob: Job? = null
    private var voiceCollectJob: Job? = null
    private var stopSequenceReached = false

    init {
        logToFile("=== ChatViewModel 创建 ===")
        collectInferenceState()
        collectVoiceEvents()
        collectVoiceOutputState()

        _uiState.update {
            it.copy(messages = loadMessages())
        }

        logToFile("ChatViewModel 初始化完成，开始自动初始化引擎")
        initializeEngine()
    }

    private fun logToFile(msg: String) {
        try {
            val app = getApplication<Application>()
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val line = "[$time] $msg"
            app.openFileOutput("viewmodel_log.txt", Context.MODE_APPEND).use { fos ->
                fos.write("$line\n".toByteArray())
            }
            _uiState.update { it.copy(diagnosticLog = it.diagnosticLog + line + "\n") }
        } catch (e: Exception) {
            try {
                val app = getApplication<Application>()
                app.openFileOutput("viewmodel_log.txt", Context.MODE_PRIVATE).use { fos ->
                    fos.write("LOG_INIT_ERROR: ${e.message}\n".toByteArray())
                }
                _uiState.update { it.copy(diagnosticLog = it.diagnosticLog + "LOG_INIT_ERROR: ${e.message}\n") }
            } catch (_: Exception) {}
        }
    }

    private fun collectInferenceState() {
        viewModelScope.launch {
            inferenceEngine.state.collectLatest { state ->
                _uiState.update { it.copy(engineState = state) }
                if (state is InferenceState.Idle) {
                    _uiState.update { it.copy(isGenerating = false) }
                }
            }
        }
    }

    private fun collectVoiceEvents() {
        voiceCollectJob = viewModelScope.launch {
            voiceInputEngine.events.collectLatest { event ->
                when (event) {
                    is VoiceInputEvent.PartialResult -> {
                        _uiState.update { it.copy(inputText = event.text) }
                    }
                    is VoiceInputEvent.FinalResult -> {
                        _uiState.update {
                            it.copy(
                                inputText = event.text,
                                isVoiceListening = false
                            )
                        }
                        sendMessage()
                    }
                    is VoiceInputEvent.Listening -> {
                        _uiState.update { it.copy(isVoiceListening = true) }
                    }
                    is VoiceInputEvent.NotListening -> {
                        _uiState.update { it.copy(isVoiceListening = false) }
                    }
                    is VoiceInputEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                isVoiceListening = false,
                                inputText = ""
                            )
                        }
                    }
                }
            }
        }
    }

    private fun collectVoiceOutputState() {
        viewModelScope.launch {
            voiceOutputEngine.state.collectLatest { state ->
                _uiState.update {
                    it.copy(isVoiceSpeaking = state is VoiceOutputState.Speaking)
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun addImage(uri: Uri) {
        _uiState.update { it.copy(selectedImages = it.selectedImages + uri) }
    }

    fun removeImage(uri: Uri) {
        _uiState.update { it.copy(selectedImages = it.selectedImages - uri) }
    }

    fun sendMessage() {
        val state = _uiState.value
        if (state.inputText.isBlank() && state.selectedImages.isEmpty()) return
        if (state.isGenerating) return
        stopSequenceReached = false

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = state.inputText.trim(),
            images = state.selectedImages.toList()
        )

        val assistantPlaceholder = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + assistantPlaceholder,
                inputText = "",
                selectedImages = emptyList(),
                isGenerating = true
            )
        }
        persistMessages(_uiState.value.messages)

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            generateResponse(state.inputText.trim())
        }
    }

    private suspend fun generateResponse(userInput: String) {
        val engineState = inferenceEngine.state.value
        if (engineState !is InferenceState.Ready) {
            updateAssistantMessage("模型未加载，请在设置中配置模型路径。")
            return
        }

        try {
            val messages = _uiState.value.messages
            inferenceEngine.sendMessageStream(messages).collect { token ->
                if (appendAssistantToken(token)) {
                    stopSequenceReached = true
                    inferenceEngine.cancel()
                }
            }
        } catch (e: Exception) {
            if (!stopSequenceReached) {
                updateAssistantMessage("推理出错: ${e.message}")
            }
        } finally {
            finishStreaming()
        }
    }

    private fun appendAssistantToken(token: String): Boolean {
        var shouldStop = false
        _uiState.update { state ->
            val updatedMessages = state.messages.toMutableList()
            val lastIndex = updatedMessages.lastIndex
            if (lastIndex >= 0 && updatedMessages[lastIndex].isStreaming) {
                val rawContent = updatedMessages[lastIndex].content + token
                val sanitizedContent = trimStopMarkers(rawContent)
                shouldStop = sanitizedContent.length != rawContent.length
                updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(
                    content = sanitizedContent
                )
            }
            state.copy(messages = updatedMessages)
        }
        return shouldStop
    }

    private fun updateAssistantMessage(content: String) {
        _uiState.update { state ->
            val updatedMessages = state.messages.toMutableList()
            val lastIndex = updatedMessages.lastIndex
            if (lastIndex >= 0 && updatedMessages[lastIndex].isStreaming) {
                updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(
                    content = trimStopMarkers(content),
                    isStreaming = false
                )
            }
            state.copy(messages = updatedMessages, isGenerating = false)
        }
        persistMessages(_uiState.value.messages)
    }

    private fun finishStreaming() {
        _uiState.update { state ->
            val updatedMessages = state.messages.toMutableList()
            val lastIndex = updatedMessages.lastIndex
            if (lastIndex >= 0 && updatedMessages[lastIndex].isStreaming) {
                val finishedContent = trimStopMarkers(updatedMessages[lastIndex].content)
                updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(
                    content = finishedContent,
                    isStreaming = false
                )
            }
            state.copy(messages = updatedMessages, isGenerating = false)
        }
        persistMessages(_uiState.value.messages)

        val lastMessage = _uiState.value.messages.lastOrNull()
        if (lastMessage?.role == MessageRole.ASSISTANT && lastMessage.content.isNotBlank()) {
            speakMessage(lastMessage.content)
        }
    }

    fun toggleVoiceListening() {
        if (_uiState.value.isVoiceListening) {
            voiceInputEngine.stopListening()
        } else {
            _uiState.update { it.copy(showVoicePermissionDialog = true) }
        }
    }

    fun onVoicePermissionGranted() {
        _uiState.update { it.copy(showVoicePermissionDialog = false) }
        voiceInputEngine.startListening()
    }

    fun onVoicePermissionDenied() {
        _uiState.update { it.copy(showVoicePermissionDialog = false) }
    }

    fun speakMessage(text: String) {
        viewModelScope.launch {
            voiceOutputEngine.speak(text)
        }
    }

    fun stopSpeaking() {
        voiceOutputEngine.stop()
    }

    fun cancelGeneration() {
        generateJob?.cancel()
        inferenceEngine.cancel()
        finishStreaming()
    }

    private fun defaultWelcomeMessage(): ChatMessage {
        return ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "你好！我是你的 AI 伙伴。点击下方麦克风按钮开始语音对话，或直接输入文字。"
        )
    }

    private fun loadMessages(): List<ChatMessage> {
        val app = getApplication<Application>()
        val rawMessages = app.getSharedPreferences(ChatPrefsName, Context.MODE_PRIVATE)
            .getString(MessagesKey, null)
            ?: return listOf(defaultWelcomeMessage())

        return try {
            val array = JSONArray(rawMessages)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val role = MessageRole.valueOf(item.getString("role"))
                    add(
                        ChatMessage(
                            id = item.getString("id"),
                            role = role,
                            content = trimStopMarkers(item.getString("content")),
                            timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                            isStreaming = false
                        )
                    )
                }
            }.ifEmpty { listOf(defaultWelcomeMessage()) }
        } catch (e: Exception) {
            logToFile("读取对话历史失败: ${e.message}")
            listOf(defaultWelcomeMessage())
        }
    }

    private fun persistMessages(messages: List<ChatMessage>) {
        try {
            val array = JSONArray()
            messages
                .filter { !it.isStreaming && it.content.isNotBlank() }
                .forEach { message ->
                    array.put(
                        JSONObject()
                            .put("id", message.id)
                            .put("role", message.role.name)
                            .put("content", trimStopMarkers(message.content))
                            .put("timestamp", message.timestamp)
                    )
                }

            getApplication<Application>()
                .getSharedPreferences(ChatPrefsName, Context.MODE_PRIVATE)
                .edit()
                .putString(MessagesKey, array.toString())
                .apply()
        } catch (e: Exception) {
            logToFile("保存对话历史失败: ${e.message}")
        }
    }

    private fun trimStopMarkers(text: String): String {
        val firstMarkerIndex = StopMarkers
            .mapNotNull { marker ->
                text.indexOf(marker).takeIf { it >= 0 }
            }
            .minOrNull()

        return if (firstMarkerIndex == null) {
            text
        } else {
            text.substring(0, firstMarkerIndex).trimEnd()
        }
    }

    fun initializeEngine(modelPath: String = "", systemPrompt: String = "") {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val modelsDir = app.getExternalFilesDir(DefaultModelConfig.ExternalModelsDir)
                val defaultPath = if (modelsDir != null) {
                    "${modelsDir.absolutePath}/${DefaultModelConfig.ModelFileName}"
                } else {
                    "${app.filesDir.absolutePath}/${DefaultModelConfig.ExternalModelsDir}/${DefaultModelConfig.ModelFileName}"
                }
                val actualPath = modelPath.ifBlank { defaultPath }
                val file = java.io.File(actualPath)

                logToFile("getExternalFilesDir('models') = ${modelsDir?.absolutePath}")
                logToFile("filesDir = ${app.filesDir.absolutePath}")
                logToFile("实际模型路径 = $actualPath")
                logToFile("文件存在 = ${file.exists()}")
                logToFile("文件大小 = ${file.length()} bytes")

                // 列出 models 目录下的文件
                modelsDir?.listFiles()?.forEach { f ->
                    logToFile("models目录: ${f.name} (${f.length()} bytes)")
                }

                val config = EngineConfig(
                    modelPath = actualPath,
                    systemPrompt = systemPrompt.ifBlank { DefaultModelConfig.DefaultSystemPrompt },
                    contextSize = DefaultModelConfig.ContextSize,
                    maxTokens = DefaultModelConfig.MaxTokens,
                    temperature = DefaultModelConfig.Temperature,
                    topK = DefaultModelConfig.TopK,
                    topP = DefaultModelConfig.TopP
                )
                logToFile("开始调用 engine.initialize...")
                inferenceEngine.initialize(config)
                logToFile("engine.initialize 返回, state = ${inferenceEngine.state.value}")
            } catch (e: Exception) {
                logToFile("!!! initializeEngine 异常 !!! ${e.javaClass.simpleName}: ${e.message}")
                _uiState.update {
                    it.copy(engineState = InferenceState.Error("初始化异常: ${e.message}"))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        generateJob?.cancel()
        voiceCollectJob?.cancel()
        inferenceEngine.release()
        voiceInputEngine.release()
        voiceOutputEngine.release()
    }
}
