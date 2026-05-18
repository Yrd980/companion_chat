package com.companion.chat.ui.chat

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.chat.AppContainer
import com.companion.chat.appContainer
import com.companion.chat.companion.CompanionRuntime
import com.companion.chat.companion.CompanionTurnEvent
import com.companion.chat.companion.PreferenceLearningCoordinator
import com.companion.chat.companion.PreferenceLearningAdapter
import com.companion.chat.data.context.ContextConfigRepository
import com.companion.chat.data.context.ContextManager
import com.companion.chat.data.context.ContextSettings
import com.companion.chat.data.engine.InferenceState
import com.companion.chat.data.engine.VoiceInputEvent
import com.companion.chat.data.engine.VoiceOutputState
import com.companion.chat.data.image.ImageGenerationPurpose
import com.companion.chat.data.image.ImageGenerationRequest
import com.companion.chat.data.image.ImageGenerationState
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.ConversationSession
import com.companion.chat.data.model.DEFAULT_SESSION_TITLE
import com.companion.chat.data.model.DEFAULT_WELCOME_MESSAGE
import com.companion.chat.data.model.MessageRole
import com.companion.chat.data.model.createDefaultSession
import com.companion.chat.data.preferences.SecondEngineManager
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
    val isVoiceStarting: Boolean = false,
    val isVoiceListening: Boolean = false,
    val isVoiceWarmedUp: Boolean = false,
    val isVoiceSpeaking: Boolean = false,
    val isVoiceAutoSending: Boolean = false,
    val voiceInputError: String = "",
    val lastVoiceTranscript: String = "",
    val imageGenerationState: ImageGenerationState = ImageGenerationState.Idle,
    val imageGenerationError: String = "",
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
) {
    val hasSpeakableAssistantMessage: Boolean
        get() = messages.any { message ->
            message.role == MessageRole.ASSISTANT &&
                !message.isStreaming &&
                message.content.isNotBlank()
        }
}

class ChatViewModel(
    application: Application,
    private val container: AppContainer = application.appContainer
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val modelConfigRepository = container.modelConfigRepository
    private val inferenceEngineFactory = container.inferenceEngineFactory
    var inferenceEngine = inferenceEngineFactory.create(modelConfigRepository.getConfig().runtime)
        private set
    val voiceInputEngine = container.voiceInputEngine
    private val contextConfigRepository = container.contextConfigRepository
    private val contextManager: ContextManager = container.contextManager
    private val promptAssembler = container.promptAssembler
    private val sessionRepository = container.chatSessionRepository
    private val memoryRepository = container.memoryRepository
    private val preferenceRepository = container.preferenceRepository
    private val roleCardRepository = container.roleCardRepository
    val voiceOutputEngine = container.voiceOutputEngine
    private val imageGenerationConfigRepository = container.imageGenerationConfigRepository
    private val imageGenerationEngine = container.imageGenerationEngine
    private val imageGenerationEngineSelector = container.imageGenerationEngineSelector
    private val preferenceMemoryDeriver = container.preferenceMemoryDeriver
    private val unifiedExtractionPromptBuilder = container.unifiedExtractionPromptBuilder
    private val unifiedExtractionParser = container.unifiedExtractionParser
    private val secondEngineManager = SecondEngineManager(
        primaryEngineStateProvider = { inferenceEngine.state.value },
        engineFactory = { inferenceEngineFactory.create(modelConfigRepository.getConfig().runtime) },
        timeoutMillis = STAGE4_SUMMARY_TIMEOUT_MILLIS
    )
    private val preferenceLearningCoordinator = PreferenceLearningCoordinator(
        scope = viewModelScope,
        contextConfigRepository = contextConfigRepository,
        memoryRepository = memoryRepository,
        preferenceRepository = preferenceRepository,
        preferenceMemoryDeriver = preferenceMemoryDeriver,
        unifiedExtractionPromptBuilder = unifiedExtractionPromptBuilder,
        unifiedExtractionParser = unifiedExtractionParser,
        secondEngineManager = secondEngineManager,
        engineStateProvider = { inferenceEngine.state.value },
        currentEngineConfigProvider = { inferenceEngine.getCurrentConfig() },
        baseSystemPromptProvider = { baseSystemPrompt },
        logger = ::logToFile
    )
    private val companionRuntime = CompanionRuntime(
        roleCardRepository = roleCardRepository,
        skillRepository = container.skillRepository,
        preferenceRepository = preferenceRepository,
        memoryRepository = memoryRepository,
        contextManager = contextManager,
        inferenceEngineProvider = { inferenceEngine },
        postTurnLearning = PreferenceLearningAdapter(preferenceLearningCoordinator),
        promptAssembler = promptAssembler,
        memoryPromptBuilder = container.memoryPromptBuilder,
        roleCardPromptBuilder = container.roleCardPromptBuilder
    )
    private var contextSettings: ContextSettings = ContextConfigRepository.DEFAULT_SETTINGS
    private var baseSystemPrompt: String = DEFAULT_BASE_SYSTEM_PROMPT

    private var generateJob: Job? = null
    private var voiceCollectJob: Job? = null
    private var inferenceStateJob: Job? = null
    private var shouldSpeakNextAssistantResponse = false

    init {
        logToFile("=== ChatViewModel 创建 ===")
        collectInferenceState()
        collectVoiceEvents()
        collectVoiceOutputState()
        collectImageGenerationState()
        loadContextSettings()
        loadSessionsFromStorage()
        voiceInputEngine.warmUp()

        viewModelScope.launch {
            refreshBaseSystemPrompt()
            logToFile("ChatViewModel 初始化完成，开始自动初始化引擎")
            initializeEngine(systemPrompt = baseSystemPrompt)
        }
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

    private suspend fun refreshBaseSystemPrompt() {
        baseSystemPrompt = companionRuntime.refreshBasePrompt()
    }

    internal fun debugBaseSystemPrompt(): String = baseSystemPrompt

    private fun collectInferenceState() {
        inferenceStateJob?.cancel()
        inferenceStateJob = viewModelScope.launch {
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
                    is VoiceInputEvent.WarmedUp -> {
                        _uiState.update { it.copy(isVoiceWarmedUp = true) }
                    }
                    is VoiceInputEvent.PartialResult -> {
                        _uiState.update { it.copy(inputText = event.text, voiceInputError = "") }
                    }
                    is VoiceInputEvent.FinalResult -> {
                        val transcript = event.text.trim()
                        _uiState.update {
                            it.copy(
                                inputText = transcript,
                                isVoiceStarting = false,
                                isVoiceListening = false,
                                voiceInputError = "",
                                lastVoiceTranscript = transcript
                            )
                        }
                        handleVoiceTranscript(transcript)
                    }
                    is VoiceInputEvent.Listening -> {
                        _uiState.update {
                            it.copy(
                                isVoiceStarting = false,
                                isVoiceListening = true,
                                voiceInputError = ""
                            )
                        }
                    }
                    is VoiceInputEvent.NotListening -> {
                        _uiState.update { it.copy(isVoiceStarting = false, isVoiceListening = false) }
                    }
                    is VoiceInputEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                isVoiceStarting = false,
                                isVoiceListening = false,
                                voiceInputError = event.message
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

    private fun collectImageGenerationState() {
        viewModelScope.launch {
            imageGenerationEngineSelector.state.collectLatest { state ->
                _uiState.update {
                    it.copy(
                        imageGenerationState = state,
                        imageGenerationError = (state as? ImageGenerationState.Error)?.message.orEmpty()
                    )
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

    fun generateChatSceneImage(prompt: String) {
        viewModelScope.launch {
            val resolvedPrompt = prompt.ifBlank { buildImagePromptFromConversation() }
            logToFile("图片生成请求: promptLength=${resolvedPrompt.length}, provider=${imageGenerationConfigRepository.getConfig().provider}")
            imageGenerationEngineSelector.generate(
                request = ImageGenerationRequest(
                    prompt = resolvedPrompt,
                    purpose = ImageGenerationPurpose.CHAT_SCENE
                ),
                config = imageGenerationConfigRepository.getConfig()
            ).onSuccess { uri ->
                logToFile("图片生成成功: $uri")
                addImage(Uri.parse(uri))
            }.onFailure { error ->
                logToFile("图片生成失败: ${error.message}")
                _uiState.update {
                    it.copy(
                        imageGenerationState = ImageGenerationState.Error(error.message ?: "图片生成失败"),
                        imageGenerationError = error.message ?: "图片生成失败"
                    )
                }
            }
        }
    }

    private fun buildImagePromptFromConversation(): String {
        return _uiState.value.messages
            .asReversed()
            .firstOrNull { it.content.isNotBlank() }
            ?.content
            ?.take(180)
            ?: "warm anime companion chat scene"
    }

    fun sendMessage() {
        submitCurrentMessage(autoSpeakResponse = false)
    }

    private fun handleVoiceTranscript(transcript: String) {
        when (
            val decision = VoiceDrivenChatPolicy.evaluateTranscript(
                transcript = transcript,
                isGenerating = _uiState.value.isGenerating,
                isEngineReady = inferenceEngine.state.value is InferenceState.Ready
            )
        ) {
            VoiceTranscriptDecision.AutoSend -> {
                submitCurrentMessage(autoSpeakResponse = true)
            }
            is VoiceTranscriptDecision.HoldForUser -> {
                _uiState.update {
                    it.copy(
                        voiceInputError = decision.message,
                        isVoiceAutoSending = false
                    )
                }
            }
        }
    }

    private fun submitCurrentMessage(autoSpeakResponse: Boolean) {
        if (!autoSpeakResponse) {
            shouldSpeakNextAssistantResponse = false
        }
        var state = _uiState.value
        if (state.inputText.isBlank() && state.selectedImages.isEmpty()) {
            _uiState.update { it.copy(isVoiceAutoSending = false) }
            return
        }
        if (state.isGenerating) {
            _uiState.update { it.copy(isVoiceAutoSending = false) }
            return
        }
        companionRuntime.cancelPostTurnLearning()

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
                isGenerating = true,
                isVoiceAutoSending = autoSpeakResponse
            )
        }
        saveCurrentSession()

        generateJob?.cancel()
        shouldSpeakNextAssistantResponse = autoSpeakResponse
        generateJob = viewModelScope.launch {
            if (!contextConfigRepository.getAutoPreferenceLearningEnabled()) {
                storeRuleBasedMemoriesForMessage(userMessage)
            }
            generateResponse(userMessage.content.trim())
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
            val memoryContext = buildMemoryContext(userInput)
            contextSettings = contextConfigRepository.getSettings()
            companionRuntime.runTurn(
                messages = messages,
                baseSystemPrompt = baseSystemPrompt,
                settings = contextSettings,
                userPreferences = memoryContext.confirmedPreferencePrompt,
                persistentMemoryPrompt = memoryContext.persistentPrompt,
                memoryPrompt = memoryContext.retrievedPrompt
            ).collect { event ->
                when (event) {
                    is CompanionTurnEvent.AssistantToken -> appendAssistantToken(event.token)
                }
            }
        } catch (e: Exception) {
            updateAssistantMessage("推理出错: ${e.message}")
        } finally {
            finishStreaming()
        }
    }

    private suspend fun buildMemoryContext(userInput: String): MemoryContext {
        return try {
            val confirmedPreferencePrompt = buildConfirmedPreferencePrompt()
            val companionMemoryContext = companionRuntime.buildMemoryContext(userInput)
            val persistentPrompt = companionMemoryContext.persistentPrompt
            val memoryPrompt = companionMemoryContext.retrievedPrompt
            if (confirmedPreferencePrompt.isNotBlank()) {
                logToFile("confirmed 偏好注入: count=${preferenceRepository.getConfirmedPreferences().size}")
            }
            if (persistentPrompt.isNotBlank()) {
                logToFile("常驻长期记忆注入: count=${companionMemoryContext.persistentMemoryCount}")
            }
            if (memoryPrompt.isNotBlank()) {
                logToFile(
                    "动态记忆检索成功: count=${companionMemoryContext.retrievedMemoryCount}, " +
                        "query=${userInput.trim()}"
                )
            } else {
                logToFile("动态记忆检索为空: query=${userInput.trim()}")
            }
            MemoryContext(
                confirmedPreferencePrompt = confirmedPreferencePrompt,
                persistentPrompt = persistentPrompt,
                retrievedPrompt = memoryPrompt
            )
        } catch (e: Exception) {
            logToFile("发送前记忆检索失败: ${e.message}")
            MemoryContext()
        }
    }

    private data class MemoryContext(
        val confirmedPreferencePrompt: String = "",
        val persistentPrompt: String = "",
        val retrievedPrompt: String = ""
    )

    private suspend fun storeRuleBasedMemoriesForMessage(userMessage: ChatMessage) {
        try {
            if (userMessage.content.isBlank()) {
                return
            }
            val sessionId = _uiState.value.currentSessionId.ifBlank { return }
            val insertedMemories = memoryRepository.extractAndStoreMemories(
                userMessage = userMessage.content,
                sessionId = sessionId
            )
            if (insertedMemories.isNotEmpty()) {
                logToFile("规则兜底记忆写入成功: count=${insertedMemories.size}")
            }
        } catch (e: Exception) {
            logToFile("规则兜底记忆写入失败: ${e.message}")
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
            state.copy(messages = updatedMessages, isGenerating = false, isVoiceAutoSending = false)
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
            state.copy(messages = updatedMessages, isGenerating = false, isVoiceAutoSending = false)
        }

        val lastMessage = _uiState.value.messages.lastOrNull()
        if (
            shouldSpeakNextAssistantResponse &&
            lastMessage?.role == MessageRole.ASSISTANT &&
            lastMessage.content.isNotBlank()
        ) {
            speakMessage(lastMessage.content)
        }
        shouldSpeakNextAssistantResponse = false

        saveCurrentSession()
        schedulePreferenceSummaryAfterDelay()
    }

    fun toggleVoiceListening() {
        if (_uiState.value.isVoiceListening || _uiState.value.isVoiceStarting) {
            voiceInputEngine.stopListening()
            _uiState.update {
                it.copy(
                    isVoiceStarting = false,
                    isVoiceListening = false,
                    showVoicePermissionDialog = false
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isVoiceStarting = true,
                    voiceInputError = "",
                    showVoicePermissionDialog = true
                )
            }
        }
    }

    fun onVoicePermissionGranted() {
        _uiState.update { it.copy(showVoicePermissionDialog = false) }
        voiceInputEngine.startListening()
    }

    fun onVoicePermissionDenied() {
        _uiState.update {
            it.copy(
                isVoiceStarting = false,
                showVoicePermissionDialog = false,
                voiceInputError = "缺少录音权限，无法使用语音输入"
            )
        }
    }

    fun clearVoiceInputError() {
        _uiState.update { it.copy(voiceInputError = "") }
    }

    fun speakMessage(text: String) {
        viewModelScope.launch {
            voiceOutputEngine.speak(text)
        }
    }

    fun speakLatestAssistantMessage() {
        val latestAssistantMessage = _uiState.value.messages.lastOrNull { message ->
            message.role == MessageRole.ASSISTANT &&
                !message.isStreaming &&
                message.content.isNotBlank()
        } ?: return

        speakMessage(latestAssistantMessage.content)
    }

    fun stopSpeaking() {
        voiceOutputEngine.stop()
    }

    fun cancelGeneration() {
        generateJob?.cancel()
        inferenceEngine.cancel()
        companionRuntime.cancelPostTurnLearning()
        shouldSpeakNextAssistantResponse = false
        _uiState.update { it.copy(isGenerating = false, isVoiceAutoSending = false) }
    }

    fun initializeEngine(modelPath: String = "", systemPrompt: String = "") {
        viewModelScope.launch {
            try {
                if (inferenceEngine.state.value is InferenceState.Generating) {
                    generateJob?.cancel()
                    inferenceEngine.cancel()
                    companionRuntime.cancelPostTurnLearning()
                    _uiState.update { it.copy(isGenerating = false) }
                    logToFile("模型配置变更: 已取消当前生成并准备重建引擎")
                }

                val app = getApplication<Application>()
                val modelConfig = modelConfigRepository.getConfig()
                val actualPath = modelPath.ifBlank { modelConfigRepository.resolveModelPath(modelConfig) }
                val file = java.io.File(actualPath)

                logToFile("getExternalFilesDir('models') = ${app.getExternalFilesDir("models")?.absolutePath}")
                logToFile("filesDir = ${app.filesDir.absolutePath}")
                logToFile("模型运行时 = ${modelConfig.runtime}")
                logToFile("实际模型路径 = $actualPath")
                logToFile("文件存在 = ${file.exists()}")
                logToFile("文件大小 = ${file.length()} bytes")

                app.getExternalFilesDir("models")?.listFiles()?.forEach { f ->
                    logToFile("models目录: ${f.name} (${f.length()} bytes)")
                }

                val resolvedSystemPrompt = systemPrompt.ifBlank {
                    baseSystemPrompt.ifBlank { DEFAULT_BASE_SYSTEM_PROMPT }
                }
                baseSystemPrompt = resolvedSystemPrompt
                val config = modelConfigRepository.toEngineConfig(
                    systemPrompt = resolvedSystemPrompt
                ).copy(modelPath = actualPath)
                if (config.runtime != inferenceEngine.getCurrentConfig()?.runtime) {
                    logToFile("切换模型运行时: ${inferenceEngine.getCurrentConfig()?.runtime} -> ${config.runtime}")
                    inferenceEngine.release()
                    inferenceEngine = inferenceEngineFactory.create(config.runtime)
                    collectInferenceState()
                }
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
        inferenceStateJob?.cancel()
        companionRuntime.release()
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
            triggerPreferenceSummaryNow(reason = "新建会话前")
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

    suspend fun startRoleConversation(roleId: Long) {
        if (_uiState.value.currentSessionId.isNotBlank()) {
            triggerPreferenceSummaryNow(reason = "角色对话前")
            saveCurrentSession()
        }
        roleCardRepository.activateRoleCard(roleId)
        val roleCard = roleCardRepository.getRoleCard(roleId)
        refreshBaseSystemPrompt()

        val openingMessage = roleCard?.openingMessage
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_WELCOME_MESSAGE
        val now = System.currentTimeMillis()
        val newSession = ConversationSession(
            title = roleCard?.name?.takeIf { it.isNotBlank() } ?: DEFAULT_SESSION_TITLE,
            messages = listOf(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = openingMessage,
                    timestamp = now
                )
            ),
            createdAt = now,
            updatedAt = now
        )
        _uiState.update {
            it.copy(
                sessions = listOf(newSession) + it.sessions,
                currentSessionId = newSession.id,
                messages = newSession.messages,
                showSessionDrawer = false,
                sessionSearchQuery = "",
                inputText = "",
                selectedImages = emptyList()
            )
        }
        persistSession(newSession)
        rebuildConversationForPromptChange(reason = "角色对话开始")
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
        triggerPreferenceSummaryNow(
            reason = "切换会话",
            sessionId = state.currentSessionId,
            messages = state.messages
        )
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

    fun onAppBackgrounded() {
        triggerPreferenceSummaryNow(reason = "应用进入后台")
    }

    private fun schedulePreferenceSummaryAfterDelay() {
        companionRuntime.onTurnFinished(
            sessionIdProvider = { _uiState.value.currentSessionId },
            messagesProvider = { _uiState.value.messages }
        )
    }

    private fun triggerPreferenceSummaryNow(
        reason: String,
        sessionId: String = _uiState.value.currentSessionId,
        messages: List<ChatMessage> = _uiState.value.messages
    ) {
        companionRuntime.onConversationBoundary(
            reason = reason,
            sessionId = sessionId,
            messages = messages
        )
    }

    private suspend fun buildConfirmedPreferencePrompt(): String {
        return companionRuntime.buildConfirmedPreferencePrompt()
    }

    private suspend fun rebuildConversationWithContext(
        stableMessages: List<ChatMessage>,
        userPreferences: String,
        persistentMemoryPrompt: String,
        memoryPrompt: String,
        forceRebuild: Boolean,
        reason: String
    ) {
        contextSettings = contextConfigRepository.getSettings()
        val rebuildResult = companionRuntime.rebuildConversationWithContext(
            stableMessages = stableMessages,
            baseSystemPrompt = baseSystemPrompt,
            settings = contextSettings,
            userPreferences = userPreferences,
            persistentMemoryPrompt = persistentMemoryPrompt,
            memoryPrompt = memoryPrompt,
            forceRebuild = forceRebuild
        )
        if (!rebuildResult.rebuildAttempted) {
            logToFile(
                "发送前上下文检查: 未触发压缩, " +
                    "messageCount=${stableMessages.size}, threshold=${contextSettings.compressionThreshold}, " +
                    "contextInjected=false"
            )
            return
        }

        logToFile(
            "$reason: recentMessages=${rebuildResult.recentMessageCount}, " +
                "summaryEmpty=${rebuildResult.historySummaryEmpty}, " +
                "preferenceInjected=${rebuildResult.preferenceInjected}, " +
                "persistentMemoryInjected=${rebuildResult.persistentMemoryInjected}, " +
                "memoryInjected=${rebuildResult.memoryInjected}"
        )

        if (rebuildResult.rebuildSucceeded == false) {
            logToFile("$reason: Conversation 重建失败")
            return
        }

        if (rebuildResult.replaySucceeded == true) {
            logToFile("$reason: 最近消息回放成功")
        } else if (rebuildResult.replaySucceeded == false && rebuildResult.fallbackSucceeded == true) {
            logToFile("$reason: 最近消息回放失败，降级摘要注入成功")
        } else if (rebuildResult.replaySucceeded == false) {
            logToFile("$reason: 最近消息回放失败，降级摘要注入失败")
        }
    }

    suspend fun activateRoleCard(roleId: Long) {
        baseSystemPrompt = companionRuntime.activateRoleCardAndRefreshPrompt(roleId)
        rebuildConversationForPromptChange(reason = "角色卡切换")
    }

    suspend fun activateSkill(skillId: Long) {
        baseSystemPrompt = companionRuntime.activateSkillAndRefreshPrompt(skillId)
        rebuildConversationForPromptChange(reason = "Skill 切换")
    }

    private suspend fun rebuildConversationForPromptChange(reason: String) {
        if (inferenceEngine.state.value is InferenceState.Generating) {
            logToFile("$reason: 当前正在生成，暂不重建 Conversation")
            return
        }
        if (inferenceEngine.getCurrentConfig() == null) {
            logToFile("$reason: 引擎尚未初始化，已仅更新基础 prompt")
            return
        }

        val stableMessages = _uiState.value.messages
            .filterNot { it.isStreaming }
            .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
        val latestUserInput = stableMessages.lastOrNull { it.role == MessageRole.USER }?.content.orEmpty()
        if (latestUserInput.isBlank()) {
            val rebuildSucceeded = inferenceEngine.rebuildConversation(baseSystemPrompt)
            if (rebuildSucceeded) {
                logToFile("$reason: 无用户消息，已仅使用基础 prompt 重建 Conversation")
            } else {
                logToFile("$reason: 无用户消息，基础 prompt 重建 Conversation 失败")
            }
            return
        }
        val memoryContext = buildMemoryContext(latestUserInput)

        rebuildConversationWithContext(
            stableMessages = stableMessages,
            userPreferences = memoryContext.confirmedPreferencePrompt,
            persistentMemoryPrompt = memoryContext.persistentPrompt,
            memoryPrompt = memoryContext.retrievedPrompt,
            forceRebuild = true,
            reason = reason
        )
    }

    companion object {
        private const val DEFAULT_BASE_SYSTEM_PROMPT =
            CompanionRuntime.DEFAULT_BASE_PROMPT
        private const val STAGE4_SUMMARY_TIMEOUT_MILLIS = 90_000L
    }
}
