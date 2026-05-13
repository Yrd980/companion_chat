package com.companion.chat.ui.chat

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.chat.data.context.ContextConfigRepository
import com.companion.chat.data.context.ContextManager
import com.companion.chat.data.context.ContextSettings
import com.companion.chat.data.context.DefaultContextManager
import com.companion.chat.data.context.PromptAssembler
import com.companion.chat.data.engine.BackendType
import com.companion.chat.data.engine.EngineConfig
import com.companion.chat.data.engine.InferenceState
import com.companion.chat.data.engine.VoiceInputEvent
import com.companion.chat.data.engine.VoiceOutputState
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.ConversationSession
import com.companion.chat.data.model.DEFAULT_SESSION_TITLE
import com.companion.chat.data.model.DEFAULT_WELCOME_MESSAGE
import com.companion.chat.data.model.MessageRole
import com.companion.chat.data.model.createDefaultSession
import com.companion.chat.data.model.createWelcomeMessage
import com.companion.chat.data.repository.ChatSessionRepository
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
    private val contextConfigRepository = ContextConfigRepository(application)
    private val contextManager: ContextManager = DefaultContextManager()
    private val promptAssembler = PromptAssembler()
    private val sessionRepository = ChatSessionRepository(application)
    private var contextSettings: ContextSettings = ContextConfigRepository.DEFAULT_SETTINGS

    private var generateJob: Job? = null
    private var voiceCollectJob: Job? = null

    init {
        logToFile("=== ChatViewModel 创建 ===")
        collectInferenceState()
        collectVoiceEvents()
        collectVoiceOutputState()
        loadContextSettings()
        loadSessionsFromStorage()

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

    private fun loadContextSettings() {
        contextSettings = contextConfigRepository.getSettings()
        logToFile(
            "上下文设置已加载: retainedRounds=${contextSettings.retainedRounds}, " +
                "compressionBuffer=${contextSettings.compressionBuffer}"
        )
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
        var state = _uiState.value
        if (state.inputText.isBlank() && state.selectedImages.isEmpty()) return
        if (state.isGenerating) return

        if (state.currentSessionId.isBlank()) {
            val newSession = ConversationSession(messages = emptyList())
            _uiState.update {
                it.copy(
                    sessions = listOf(newSession) + it.sessions,
                    currentSessionId = newSession.id,
                    messages = emptyList()
                )
            }
            persistSession(newSession)
            state = _uiState.value
        }

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
        saveCurrentSession()

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
            prepareContextBeforeSend(messages)
            inferenceEngine.sendMessageStream(messages).collect { token ->
                appendAssistantToken(token)
            }
        } catch (e: Exception) {
            updateAssistantMessage("推理出错: ${e.message}")
        } finally {
            finishStreaming()
        }
    }

    private suspend fun prepareContextBeforeSend(messages: List<ChatMessage>) {
        val stableMessages = messages.filterNot { it.isStreaming }
        contextSettings = contextConfigRepository.getSettings()

        if (!contextManager.shouldCompress(stableMessages, contextSettings)) {
            logToFile(
                "发送前上下文检查: 未触发压缩, " +
                    "messageCount=${stableMessages.size}, threshold=${contextSettings.compressionThreshold}"
            )
            return
        }

        val currentConfig = inferenceEngine.getCurrentConfig()
        val baseSystemPrompt = currentConfig?.systemPrompt?.ifBlank {
            "你是一个友善的AI助手，请用中文回答用户的问题。"
        } ?: "你是一个友善的AI助手，请用中文回答用户的问题。"
        val contextWindow = contextManager.buildContext(
            messages = stableMessages,
            systemPrompt = baseSystemPrompt,
            userPreferences = "",
            settings = contextSettings
        )

        logToFile(
            "发送前上下文压缩: recentMessages=${contextWindow.recentMessages.size}, " +
                "summaryEmpty=${contextWindow.historySummary.isBlank()}"
        )

        val rebuildSucceeded = inferenceEngine.rebuildConversation(contextWindow.systemPrompt)
        if (!rebuildSucceeded) {
            logToFile("发送前上下文重建失败，本轮继续沿用当前 Conversation")
            return
        }

        val replaySucceeded = inferenceEngine.replayMessages(contextWindow.recentMessages)
        if (replaySucceeded) {
            logToFile("发送前上下文处理: 最近消息回放成功")
        } else {
            val fallbackPrompt = promptAssembler.assemble(
                baseSystemPrompt = contextWindow.systemPrompt,
                userPreferences = "",
                historySummary = "",
                recentConversationSnippet = buildRecentConversationSnippet(contextWindow.recentMessages)
            )
            val fallbackSucceeded = inferenceEngine.rebuildConversationWithFallbackContext(fallbackPrompt)
            if (fallbackSucceeded) {
                logToFile("发送前上下文处理: 最近消息回放失败，降级摘要注入成功")
            } else {
                logToFile("发送前上下文处理: 最近消息回放失败，降级摘要注入失败")
            }
        }
    }

    private fun buildRecentConversationSnippet(messages: List<ChatMessage>): String {
        return messages.mapNotNull { message ->
            val content = message.content.trim()
            if (content.isBlank()) {
                return@mapNotNull null
            }

            val roleLabel = when (message.role) {
                MessageRole.USER -> "用户"
                MessageRole.ASSISTANT -> "助手"
                MessageRole.SYSTEM -> "系统"
            }
            "$roleLabel：${content.take(80)}"
        }.joinToString(separator = "\n")
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
        if (_uiState.value.currentSessionId.isNotBlank()) {
            saveCurrentSession()
        }
        val newSession = createDefaultSession()
        _uiState.update {
            it.copy(
                sessions = listOf(newSession) + it.sessions,
                currentSessionId = newSession.id,
                messages = newSession.messages,
                showSessionDrawer = false,
                sessionSearchQuery = ""
            )
        }
        persistSession(newSession)
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
        val newTitle = state.editingTitle.trim().ifBlank { DEFAULT_SESSION_TITLE }
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
        updatedSessions.firstOrNull { it.id == state.editingSessionId }?.let(::persistSession)
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
                messages = session.messages,
                showSessionDrawer = false,
                sessionSearchQuery = "",
                inputText = "",
                selectedImages = emptyList()
            )
        }
    }

    fun deleteSession(sessionId: String) {
        val state = _uiState.value
        val remainingSessions = state.sessions.filterNot { it.id == sessionId }
        val nextSession = if (state.currentSessionId == sessionId) {
            remainingSessions.firstOrNull()
        } else {
            state.sessions.firstOrNull { it.id == state.currentSessionId }
        }

        _uiState.update {
            it.copy(
                sessions = remainingSessions,
                currentSessionId = nextSession?.id.orEmpty(),
                messages = nextSession?.messages ?: emptyList(),
                showSessionDrawer = false,
                sessionSearchQuery = "",
                editingSessionId = if (it.editingSessionId == sessionId) "" else it.editingSessionId,
                editingTitle = if (it.editingSessionId == sessionId) "" else it.editingTitle
            )
        }

        viewModelScope.launch {
            try {
                sessionRepository.deleteSession(sessionId)
            } catch (e: Exception) {
                logToFile("删除会话失败: ${e.message}")
            }
        }
    }

    private fun saveCurrentSession() {
        val state = _uiState.value
        if (state.currentSessionId.isBlank()) return
        val filteredMessages = state.messages.filter {
            it.content != DEFAULT_WELCOME_MESSAGE || it.role != MessageRole.ASSISTANT
        }
        val title = filteredMessages.firstOrNull { it.role == MessageRole.USER }?.content?.take(20)
            ?: state.sessions.firstOrNull { it.id == state.currentSessionId }?.title
            ?: DEFAULT_SESSION_TITLE
        val updatedAt = System.currentTimeMillis()
        val updatedSessions = state.sessions.map { session ->
            if (session.id == state.currentSessionId) {
                session.copy(title = title, messages = state.messages, updatedAt = updatedAt)
            } else {
                session
            }
        }
        _uiState.update { it.copy(sessions = updatedSessions) }
        updatedSessions.firstOrNull { it.id == state.currentSessionId }?.let(::persistSession)
    }

    private fun loadSessionsFromStorage() {
        viewModelScope.launch {
            try {
                sessionRepository.ensureInitialized()
                val sessions = sessionRepository.getAllSessions()
                val existing = sessions.firstOrNull()
                if (existing != null) {
                    _uiState.update {
                        it.copy(
                            sessions = sessions,
                            messages = existing.messages,
                            currentSessionId = existing.id
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            sessions = emptyList(),
                            currentSessionId = "",
                            messages = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                logToFile("加载会话列表失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        sessions = emptyList(),
                        currentSessionId = "",
                        messages = emptyList()
                    )
                }
            }
        }
    }

    private fun persistSession(session: ConversationSession) {
        viewModelScope.launch {
            try {
                sessionRepository.replaceSession(session)
            } catch (e: Exception) {
                logToFile("保存会话列表失败: ${e.message}")
            }
        }
    }
}
