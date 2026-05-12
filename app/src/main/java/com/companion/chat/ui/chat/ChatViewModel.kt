package com.companion.chat.ui.chat

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.chat.data.engine.BackendType
import com.companion.chat.data.engine.EngineConfig
import com.companion.chat.data.engine.InferenceState
import com.companion.chat.data.engine.VoiceInputEvent
import com.companion.chat.data.engine.VoiceOutputState
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.ConversationSession
import com.companion.chat.data.model.MessageRole
import com.companion.chat.engine.AndroidVoiceInputEngine
import com.companion.chat.engine.AndroidVoiceOutputEngine
import com.companion.chat.engine.LiteRTLMInferenceEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DateFilter { ALL, TODAY, YESTERDAY, WEEK, MONTH }

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val selectedImages: List<Uri> = emptyList(),
    val isGenerating: Boolean = false,
    val isVoiceListening: Boolean = false,
    val isVoiceSpeaking: Boolean = false,
    val engineState: InferenceState = InferenceState.Idle,
    val showVoicePermissionDialog: Boolean = false,
    val diagnosticLog: String = "",
    val sessions: List<ConversationSession> = emptyList(),
    val currentSessionId: String = "",
    val showSessionDrawer: Boolean = false,
    val sessionSearchQuery: String = "",
    val dateFilter: DateFilter = DateFilter.ALL,
    val editingSessionId: String = "",
    val editingTitle: String = ""
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val inferenceEngine = LiteRTLMInferenceEngine(application)
    val voiceInputEngine = AndroidVoiceInputEngine(application)
    val voiceOutputEngine = AndroidVoiceOutputEngine(application)

    private var generateJob: Job? = null
    private var voiceCollectJob: Job? = null
    private val sessionsFile: File
        get() = File(getApplication<Application>().filesDir, "conversations.json")

    init {
        logToFile("=== ChatViewModel 创建 ===")
        collectInferenceState()
        collectVoiceEvents()
        collectVoiceOutputState()

        loadSessions()
        val existing = _uiState.value.sessions.firstOrNull()
        if (existing != null) {
            _uiState.update {
                it.copy(
                    messages = existing.messages.ifEmpty {
                        listOf(welcomeMessage())
                    },
                    currentSessionId = existing.id
                )
            }
        } else {
            val defaultSession = ConversationSession(
                title = "新对话",
                messages = listOf(welcomeMessage())
            )
            _uiState.update {
                it.copy(
                    sessions = listOf(defaultSession),
                    currentSessionId = defaultSession.id,
                    messages = defaultSession.messages
                )
            }
            saveSessions()
        }

        logToFile("ChatViewModel 初始化完成，开始自动初始化引擎")
        initializeEngine()
    }

    private fun welcomeMessage() = ChatMessage(
        role = MessageRole.ASSISTANT,
        content = "你好！我是你的 AI 伙伴。点击下方麦克风按钮开始语音对话，或直接输入文字。"
    )

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
                appendAssistantToken(token)
            }
        } catch (e: Exception) {
            updateAssistantMessage("推理出错: ${e.message}")
        } finally {
            finishStreaming()
        }
    }

    private fun appendAssistantToken(token: String) {
        _uiState.update { state ->
            val updatedMessages = state.messages.toMutableList()
            val lastIndex = updatedMessages.lastIndex
            if (lastIndex >= 0 && updatedMessages[lastIndex].isStreaming) {
                updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(
                    content = updatedMessages[lastIndex].content + token
                )
            }
            state.copy(messages = updatedMessages)
        }
    }

    private fun updateAssistantMessage(content: String) {
        _uiState.update { state ->
            val updatedMessages = state.messages.toMutableList()
            val lastIndex = updatedMessages.lastIndex
            if (lastIndex >= 0 && updatedMessages[lastIndex].isStreaming) {
                updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(
                    content = content,
                    isStreaming = false
                )
            }
            state.copy(messages = updatedMessages, isGenerating = false)
        }
    }

    private fun finishStreaming() {
        _uiState.update { state ->
            val updatedMessages = state.messages.toMutableList()
            val lastIndex = updatedMessages.lastIndex
            if (lastIndex >= 0 && updatedMessages[lastIndex].isStreaming) {
                updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(
                    isStreaming = false
                )
            }
            state.copy(messages = updatedMessages, isGenerating = false)
        }

        val lastMessage = _uiState.value.messages.lastOrNull()
        if (lastMessage?.role == MessageRole.ASSISTANT && lastMessage.content.isNotBlank()) {
            speakMessage(lastMessage.content)
        }

        saveCurrentSession()
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
        _uiState.update { it.copy(isGenerating = false) }
    }

    fun initializeEngine(modelPath: String = "", systemPrompt: String = "") {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val modelsDir = app.getExternalFilesDir("models")
                val defaultPath = if (modelsDir != null) {
                    "${modelsDir.absolutePath}/gemma-4-E2B-it.litertlm"
                } else {
                    "${app.filesDir.absolutePath}/models/gemma-4-E2B-it.litertlm"
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
                    systemPrompt = systemPrompt
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

    fun toggleSessionDrawer() {
        _uiState.update { it.copy(showSessionDrawer = !it.showSessionDrawer) }
    }

    fun closeSessionDrawer() {
        _uiState.update { it.copy(showSessionDrawer = false, sessionSearchQuery = "") }
    }

    fun updateSessionSearchQuery(query: String) {
        _uiState.update { it.copy(sessionSearchQuery = query) }
    }

    fun createNewSession() {
        saveCurrentSession()
        val newSession = ConversationSession(
            title = "新对话",
            messages = listOf(welcomeMessage())
        )
        _uiState.update {
            it.copy(
                sessions = listOf(newSession) + it.sessions,
                currentSessionId = newSession.id,
                messages = newSession.messages,
                showSessionDrawer = false,
                sessionSearchQuery = ""
            )
        }
        saveSessions()
    }

    fun setDateFilter(filter: DateFilter) {
        _uiState.update { it.copy(dateFilter = filter) }
    }

    fun startEditingTitle(sessionId: String) {
        val session = _uiState.value.sessions.find { it.id == sessionId } ?: return
        _uiState.update { it.copy(editingSessionId = sessionId, editingTitle = session.title) }
    }

    fun updateEditingTitle(title: String) {
        _uiState.update { it.copy(editingTitle = title) }
    }

    fun confirmEditingTitle() {
        val state = _uiState.value
        if (state.editingSessionId.isBlank()) return
        val newTitle = state.editingTitle.trim().ifBlank { "新对话" }
        val updatedSessions = state.sessions.map { session ->
            if (session.id == state.editingSessionId) {
                session.copy(title = newTitle)
            } else {
                session
            }
        }
        _uiState.update {
            it.copy(
                sessions = updatedSessions,
                editingSessionId = "",
                editingTitle = ""
            )
        }
        saveSessions()
    }

    fun cancelEditingTitle() {
        _uiState.update { it.copy(editingSessionId = "", editingTitle = "") }
    }

    fun switchToSession(sessionId: String) {
        val state = _uiState.value
        if (sessionId == state.currentSessionId) {
            _uiState.update { it.copy(showSessionDrawer = false, sessionSearchQuery = "") }
            return
        }
        saveCurrentSession()
        val session = state.sessions.find { it.id == sessionId } ?: return
        _uiState.update {
            it.copy(
                currentSessionId = sessionId,
                messages = session.messages.ifEmpty { listOf(welcomeMessage()) },
                showSessionDrawer = false,
                sessionSearchQuery = "",
                inputText = "",
                selectedImages = emptyList()
            )
        }
    }

    private fun saveCurrentSession() {
        val state = _uiState.value
        if (state.currentSessionId.isBlank()) return
        val filteredMessages = state.messages.filter { it.content != "你好！我是你的 AI 伙伴。点击下方麦克风按钮开始语音对话，或直接输入文字。" || it.role != MessageRole.ASSISTANT }
        val title = filteredMessages.firstOrNull { it.role == MessageRole.USER }?.content?.take(20) ?: "新对话"
        val updatedSessions = state.sessions.map { session ->
            if (session.id == state.currentSessionId) {
                session.copy(title = title, messages = state.messages)
            } else {
                session
            }
        }
        _uiState.update { it.copy(sessions = updatedSessions) }
        saveSessions()
    }

    private fun loadSessions() {
        try {
            if (!sessionsFile.exists()) return
            val json = sessionsFile.readText()
            val arr = JSONArray(json)
            val sessions = mutableListOf<ConversationSession>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val msgsArr = obj.getJSONArray("messages")
                val messages = mutableListOf<ChatMessage>()
                for (j in 0 until msgsArr.length()) {
                    val msgObj = msgsArr.getJSONObject(j)
                    messages.add(
                        ChatMessage(
                            id = msgObj.getString("id"),
                            role = MessageRole.valueOf(msgObj.getString("role")),
                            content = msgObj.getString("content"),
                            timestamp = msgObj.getLong("timestamp")
                        )
                    )
                }
                sessions.add(
                    ConversationSession(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        messages = messages,
                        createdAt = obj.getLong("createdAt")
                    )
                )
            }
            val sorted = sessions.sortedByDescending { it.createdAt }
            _uiState.update { it.copy(sessions = sorted) }
        } catch (e: Exception) {
            logToFile("加载会话列表失败: ${e.message}")
        }
    }

    private fun saveSessions() {
        try {
            val sessions = _uiState.value.sessions
            val arr = JSONArray()
            for (session in sessions) {
                val obj = JSONObject()
                obj.put("id", session.id)
                obj.put("title", session.title)
                obj.put("createdAt", session.createdAt)
                val msgsArr = JSONArray()
                for (msg in session.messages) {
                    val msgObj = JSONObject()
                    msgObj.put("id", msg.id)
                    msgObj.put("role", msg.role.name)
                    msgObj.put("content", msg.content)
                    msgObj.put("timestamp", msg.timestamp)
                    msgsArr.put(msgObj)
                }
                obj.put("messages", msgsArr)
                arr.put(obj)
            }
            sessionsFile.writeText(arr.toString())
        } catch (e: Exception) {
            logToFile("保存会话列表失败: ${e.message}")
        }
    }
}
