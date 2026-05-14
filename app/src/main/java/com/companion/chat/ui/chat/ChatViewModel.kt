package com.companion.chat.ui.chat

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.chat.data.local.CompanionDatabase
import com.companion.chat.data.context.ContextConfigRepository
import com.companion.chat.data.context.ContextManager
import com.companion.chat.data.context.ContextSettings
import com.companion.chat.data.context.DefaultContextManager
import com.companion.chat.data.context.PromptAssembler
import com.companion.chat.data.engine.InferenceState
import com.companion.chat.data.engine.ModelConfigRepository
import com.companion.chat.data.engine.VoiceInputEvent
import com.companion.chat.data.engine.VoiceOutputState
import com.companion.chat.data.image.HttpImageGenerationEngine
import com.companion.chat.data.image.ImageGenerationConfigRepository
import com.companion.chat.data.image.ImageGenerationPurpose
import com.companion.chat.data.image.ImageGenerationState
import com.companion.chat.data.memory.MemoryPromptBuilder
import com.companion.chat.data.memory.MemoryRepository
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.ConversationSession
import com.companion.chat.data.model.DEFAULT_SESSION_TITLE
import com.companion.chat.data.model.DEFAULT_WELCOME_MESSAGE
import com.companion.chat.data.model.MessageRole
import com.companion.chat.data.model.createDefaultSession
import com.companion.chat.data.model.createWelcomeMessage
import com.companion.chat.data.role.RoleCardPromptBuilder
import com.companion.chat.data.role.RoleCardRepository
import com.companion.chat.data.skill.SkillRepository
import com.companion.chat.data.preferences.PreferenceRepository
import com.companion.chat.data.preferences.PreferenceMemoryDeriver
import com.companion.chat.data.preferences.SecondEngineManager
import com.companion.chat.data.preferences.SummaryRunResult
import com.companion.chat.data.preferences.UnifiedExtractionParser
import com.companion.chat.data.preferences.UnifiedExtractionPromptBuilder
import com.companion.chat.data.repository.ChatSessionRepository
import com.companion.chat.engine.AndroidVoiceInputEngine
import com.companion.chat.engine.AndroidVoiceOutputEngine
import com.companion.chat.engine.InferenceEngineFactory
import com.companion.chat.engine.RoleAwareVoiceOutputEngine
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
    val voiceInputError: String = "",
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

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val modelConfigRepository = ModelConfigRepository(application)
    private val inferenceEngineFactory = InferenceEngineFactory(application)
    var inferenceEngine = inferenceEngineFactory.create(modelConfigRepository.getConfig().runtime)
        private set
    val voiceInputEngine = AndroidVoiceInputEngine(application)
    private val database = CompanionDatabase.getInstance(application)
    private val contextConfigRepository = ContextConfigRepository(application)
    private val contextManager: ContextManager = DefaultContextManager()
    private val promptAssembler = PromptAssembler()
    private val sessionRepository = ChatSessionRepository(application)
    private val memoryRepository = MemoryRepository(
        memoryDao = database.memoryDao()
    )
    private val preferenceRepository = PreferenceRepository(
        preferenceDao = database.preferenceDao()
    )
    private val roleCardRepository = RoleCardRepository(
        roleCardDao = database.roleCardDao()
    )
    private val androidVoiceOutputEngine = AndroidVoiceOutputEngine(application)
    val voiceOutputEngine = RoleAwareVoiceOutputEngine(
        fallbackEngine = androidVoiceOutputEngine,
        roleCardRepository = roleCardRepository
    )
    private val imageGenerationConfigRepository = ImageGenerationConfigRepository(application)
    private val imageGenerationEngine = HttpImageGenerationEngine(application)
    private val roleCardPromptBuilder = RoleCardPromptBuilder()
    private val skillRepository = SkillRepository(
        skillDao = database.skillDao()
    )
    private val preferenceMemoryDeriver = PreferenceMemoryDeriver()
    private val memoryPromptBuilder = MemoryPromptBuilder()
    private val unifiedExtractionPromptBuilder = UnifiedExtractionPromptBuilder()
    private val unifiedExtractionParser = UnifiedExtractionParser()
    private val secondEngineManager = SecondEngineManager(
        primaryEngineStateProvider = { inferenceEngine.state.value },
        engineFactory = { inferenceEngineFactory.create(modelConfigRepository.getConfig().runtime) },
        timeoutMillis = STAGE4_SUMMARY_TIMEOUT_MILLIS
    )
    private var contextSettings: ContextSettings = ContextConfigRepository.DEFAULT_SETTINGS
    private var baseSystemPrompt: String = DEFAULT_BASE_SYSTEM_PROMPT

    private var generateJob: Job? = null
    private var voiceCollectJob: Job? = null
    private var inferenceStateJob: Job? = null
    private var preferenceSummaryDelayJob: Job? = null
    private var shouldSpeakNextAssistantResponse = false
    private val lastSummaryTimestamps = mutableMapOf<String, Long>()

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
        val rolePrompt = roleCardPromptBuilder.build(roleCardRepository.getActiveRoleCard())
        val skillPrompt = skillRepository.getActiveSkill()?.systemPrompt?.trim().orEmpty()
        baseSystemPrompt = buildList {
            add(DEFAULT_BASE_SYSTEM_PROMPT)
            if (rolePrompt.isNotBlank()) {
                add(rolePrompt)
            }
            if (skillPrompt.isNotBlank()) {
                add(skillPrompt)
            }
        }.joinToString(separator = "\n\n")
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
                        _uiState.update {
                            it.copy(
                                inputText = event.text,
                                isVoiceStarting = false,
                                isVoiceListening = false,
                                voiceInputError = ""
                            )
                        }
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
            imageGenerationEngine.state.collectLatest { state ->
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
            imageGenerationEngine.generate(
                prompt = prompt,
                config = imageGenerationConfigRepository.getConfig(),
                purpose = ImageGenerationPurpose.CHAT_SCENE
            ).onSuccess { uri ->
                addImage(Uri.parse(uri))
            }
        }
    }

    fun sendMessage() {
        var state = _uiState.value
        if (state.inputText.isBlank() && state.selectedImages.isEmpty()) return
        if (state.isGenerating) return
        secondEngineManager.cancelRunningSummary()

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
            prepareContextBeforeSend(
                messages = messages,
                userPreferences = memoryContext.confirmedPreferencePrompt,
                persistentMemoryPrompt = memoryContext.persistentPrompt,
                memoryPrompt = memoryContext.retrievedPrompt
            )
            inferenceEngine.sendMessageStream(messages).collect { token ->
                appendAssistantToken(token)
            }
        } catch (e: Exception) {
            updateAssistantMessage("推理出错: ${e.message}")
        } finally {
            finishStreaming()
        }
    }

    private suspend fun prepareContextBeforeSend(
        messages: List<ChatMessage>,
        userPreferences: String,
        persistentMemoryPrompt: String,
        memoryPrompt: String
    ) {
        val stableMessages = messages.filterNot { it.isStreaming }
        rebuildConversationWithContext(
            stableMessages = stableMessages,
            userPreferences = userPreferences,
            persistentMemoryPrompt = persistentMemoryPrompt,
            memoryPrompt = memoryPrompt,
            forceRebuild = false,
            reason = "发送前上下文处理"
        )
    }

    private suspend fun buildMemoryContext(userInput: String): MemoryContext {
        return try {
            val confirmedPreferencePrompt = buildConfirmedPreferencePrompt()
            val persistentMemories = memoryRepository.getPersistentMemories()
            val relevantMemories = memoryRepository.retrieveRelevantMemories(userInput)
            val persistentPrompt = memoryPromptBuilder.buildPersistent(persistentMemories)
            val memoryPrompt = memoryPromptBuilder.build(relevantMemories)
            if (confirmedPreferencePrompt.isNotBlank()) {
                logToFile("confirmed 偏好注入: count=${preferenceRepository.getConfirmedPreferences().size}")
            }
            if (persistentPrompt.isNotBlank()) {
                logToFile("常驻长期记忆注入: count=${persistentMemories.size}")
            }
            if (memoryPrompt.isNotBlank()) {
                logToFile("动态记忆检索成功: count=${relevantMemories.size}, query=${userInput.trim()}")
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
        secondEngineManager.cancelRunningSummary()
        shouldSpeakNextAssistantResponse = false
        _uiState.update { it.copy(isGenerating = false) }
    }

    fun initializeEngine(modelPath: String = "", systemPrompt: String = "") {
        viewModelScope.launch {
            try {
                if (inferenceEngine.state.value is InferenceState.Generating) {
                    generateJob?.cancel()
                    inferenceEngine.cancel()
                    secondEngineManager.cancelRunningSummary()
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
        preferenceSummaryDelayJob?.cancel()
        secondEngineManager.release()
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
        preferenceSummaryDelayJob?.cancel()
        preferenceSummaryDelayJob = viewModelScope.launch {
            kotlinx.coroutines.delay(STAGE4_IDLE_DELAY_MILLIS)
            runPreferenceSummaryIfNeeded(reason = "发送后静置")
        }
    }

    private fun triggerPreferenceSummaryNow(
        reason: String,
        sessionId: String = _uiState.value.currentSessionId,
        messages: List<ChatMessage> = _uiState.value.messages
    ) {
        preferenceSummaryDelayJob?.cancel()
        viewModelScope.launch {
            runPreferenceSummaryIfNeeded(
                reason = reason,
                sessionId = sessionId,
                messages = messages
            )
        }
    }

    private suspend fun runPreferenceSummaryIfNeeded(
        reason: String,
        sessionId: String = _uiState.value.currentSessionId,
        messages: List<ChatMessage> = _uiState.value.messages,
        retryAttempt: Int = 0
    ) {
        if (!contextConfigRepository.getAutoPreferenceLearningEnabled()) {
            logToFile("阶段四跳过: 自动学习偏好已关闭, reason=$reason")
            return
        }
        if (sessionId.isBlank()) {
            return
        }
        if (inferenceEngine.state.value is InferenceState.Generating) {
            logToFile("阶段四跳过: 前台仍在生成, reason=$reason")
            return
        }

        val stableMessages = messages.filterNot { it.isStreaming }
            .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
        if (stableMessages.size < MIN_STAGE4_MESSAGE_COUNT) {
            logToFile("阶段四跳过: 对话轮数不足, reason=$reason, messageCount=${stableMessages.size}")
            return
        }

        val now = System.currentTimeMillis()
        val lastSummaryAt = lastSummaryTimestamps[sessionId] ?: 0L
        if (now - lastSummaryAt < STAGE4_THROTTLE_MILLIS) {
            logToFile("阶段四跳过: 节流中, reason=$reason")
            return
        }

        val currentConfig = inferenceEngine.getCurrentConfig() ?: return
        val summaryConfig = currentConfig.copy(systemPrompt = baseSystemPrompt)
        val prompt = unifiedExtractionPromptBuilder.buildPrompt(stableMessages)
        when (val result = secondEngineManager.runSummaryIfAllowed(summaryConfig, prompt)) {
            is SummaryRunResult.Completed -> {
                val rawSummaryPreview = result.content
                    .replace("\n", "\\n")
                    .take(300)
                logToFile("阶段四原始输出: preview=$rawSummaryPreview")
                val extractionResult = unifiedExtractionParser.parse(result.content)
                val derivedMemories = preferenceMemoryDeriver.derive(extractionResult.userPreferences)
                if (derivedMemories.isNotEmpty()) {
                    logToFile("阶段四偏好派生记忆: count=${derivedMemories.size}")
                }
                val storedModelMemories = memoryRepository.storeModelExtractedMemories(
                    extractedMemories = extractionResult.memories + derivedMemories,
                    sessionId = sessionId
                )
                if (storedModelMemories.isEmpty()) {
                    val fallbackCount = storeFallbackRuleMemories(
                        messages = stableMessages,
                        sessionId = sessionId
                    )
                    if (fallbackCount > 0) {
                        logToFile("阶段四记忆兜底成功: count=$fallbackCount, reason=$reason")
                    }
                }
                preferenceRepository.mergePreferences(extractionResult.userPreferences)
                val confirmedPreferences = preferenceRepository.getConfirmedPreferences()
                logToFile(
                    "阶段四偏好合并完成: merged=${extractionResult.userPreferences.size}, " +
                        "confirmed=${confirmedPreferences.size}, retryAttempt=$retryAttempt"
                )
                lastSummaryTimestamps[sessionId] = now
                logToFile(
                    "阶段四总结完成: reason=$reason, memoryCount=${storedModelMemories.size}, " +
                        "preferenceCount=${extractionResult.userPreferences.size}, " +
                        "extractedCount=${storedModelMemories.size + extractionResult.userPreferences.size}, " +
                        "sessionId=$sessionId"
                )
            }
            SummaryRunResult.SkippedPrimaryBusy -> {
                logToFile("阶段四跳过: 前台繁忙, reason=$reason")
            }
            SummaryRunResult.SkippedAlreadyRunning -> {
                logToFile("阶段四跳过: 后台总结已在运行, reason=$reason")
            }
            SummaryRunResult.Cancelled -> {
                if (retryAttempt < MAX_STAGE4_RETRY_COUNT) {
                    logToFile("阶段四取消: reason=$reason, retry=${retryAttempt + 1}")
                    schedulePreferenceSummaryRetry(
                        reason = reason,
                        sessionId = sessionId,
                        messages = stableMessages,
                        retryAttempt = retryAttempt + 1
                    )
                } else {
                    val fallbackCount = storeFallbackRuleMemories(
                        messages = stableMessages,
                        sessionId = sessionId
                    )
                    if (fallbackCount > 0) {
                        logToFile("阶段四取消后二次兜底成功: count=$fallbackCount, reason=$reason")
                    }
                    logToFile("阶段四取消: reason=$reason, retry=$retryAttempt")
                }
            }
            SummaryRunResult.TimedOut -> {
                if (retryAttempt < MAX_STAGE4_RETRY_COUNT) {
                    logToFile("阶段四超时: reason=$reason, retry=${retryAttempt + 1}")
                    schedulePreferenceSummaryRetry(
                        reason = reason,
                        sessionId = sessionId,
                        messages = stableMessages,
                        retryAttempt = retryAttempt + 1
                    )
                } else {
                    val fallbackCount = storeFallbackRuleMemories(
                        messages = stableMessages,
                        sessionId = sessionId
                    )
                    if (fallbackCount > 0) {
                        logToFile("阶段四超时后二次兜底成功: count=$fallbackCount, reason=$reason")
                    }
                    logToFile("阶段四超时: reason=$reason, retry=$retryAttempt")
                }
            }
            is SummaryRunResult.Failed -> {
                val fallbackCount = storeFallbackRuleMemories(
                    messages = stableMessages,
                    sessionId = sessionId
                )
                if (fallbackCount > 0) {
                    logToFile("阶段四失败后记忆兜底成功: count=$fallbackCount, reason=$reason")
                }
                logToFile("阶段四失败: reason=$reason, message=${result.message}")
            }
        }
    }

    private fun schedulePreferenceSummaryRetry(
        reason: String,
        sessionId: String,
        messages: List<ChatMessage>,
        retryAttempt: Int
    ) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(STAGE4_RETRY_DELAY_MILLIS)
            runPreferenceSummaryIfNeeded(
                reason = "$reason-重试",
                sessionId = sessionId,
                messages = messages,
                retryAttempt = retryAttempt
            )
        }
    }

    private suspend fun storeFallbackRuleMemories(
        messages: List<ChatMessage>,
        sessionId: String
    ): Int {
        val userMessages = messages
            .filter { it.role == MessageRole.USER }
            .map { it.content.trim() }
            .filter { it.isNotBlank() }
        if (userMessages.isEmpty()) {
            return 0
        }
        return memoryRepository.extractAndStoreMemoriesFromMessages(
            userMessages = userMessages,
            sessionId = sessionId
        ).size
    }

    private suspend fun buildConfirmedPreferencePrompt(): String {
        val confirmedPreferences = preferenceRepository.getConfirmedPreferences()
        if (confirmedPreferences.isEmpty()) {
            return ""
        }
        return buildString {
            appendLine("关于当前用户的已知信息（请自然地融入对话，不要刻意提及你知道这些）：")
            confirmedPreferences.forEach { preference ->
                appendLine("- ${preference.content}")
            }
        }.trim()
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
        val shouldInjectContext = userPreferences.isNotBlank() ||
            persistentMemoryPrompt.isNotBlank() ||
            memoryPrompt.isNotBlank()

        if (!forceRebuild && !contextManager.shouldCompress(stableMessages, contextSettings) && !shouldInjectContext) {
            logToFile(
                "发送前上下文检查: 未触发压缩, " +
                    "messageCount=${stableMessages.size}, threshold=${contextSettings.compressionThreshold}, " +
                    "contextInjected=false"
            )
            return
        }

        val contextWindow = contextManager.buildContext(
            messages = stableMessages,
            systemPrompt = baseSystemPrompt,
            userPreferences = userPreferences,
            persistentMemoryPrompt = persistentMemoryPrompt,
            memoryPrompt = memoryPrompt,
            settings = contextSettings
        )

        logToFile(
            "$reason: recentMessages=${contextWindow.recentMessages.size}, " +
                "summaryEmpty=${contextWindow.historySummary.isBlank()}, " +
                "preferenceInjected=${contextWindow.userPreferences.isNotBlank()}, " +
                "persistentMemoryInjected=${contextWindow.persistentMemoryPrompt.isNotBlank()}, " +
                "memoryInjected=${contextWindow.memoryPrompt.isNotBlank()}"
        )

        val rebuildSucceeded = inferenceEngine.rebuildConversation(contextWindow.systemPrompt)
        if (!rebuildSucceeded) {
            logToFile("$reason: Conversation 重建失败")
            return
        }

        val replaySucceeded = inferenceEngine.replayMessages(contextWindow.recentMessages)
        if (replaySucceeded) {
            logToFile("$reason: 最近消息回放成功")
        } else {
            val fallbackPrompt = promptAssembler.assemble(
                baseSystemPrompt = contextWindow.systemPrompt,
                userPreferences = "",
                persistentMemoryPrompt = "",
                memoryPrompt = "",
                historySummary = "",
                recentConversationSnippet = buildRecentConversationSnippet(contextWindow.recentMessages)
            )
            val fallbackSucceeded = inferenceEngine.rebuildConversationWithFallbackContext(fallbackPrompt)
            if (fallbackSucceeded) {
                logToFile("$reason: 最近消息回放失败，降级摘要注入成功")
            } else {
                logToFile("$reason: 最近消息回放失败，降级摘要注入失败")
            }
        }
    }

    suspend fun activateRoleCard(roleId: Long) {
        roleCardRepository.activateRoleCard(roleId)
        refreshBaseSystemPrompt()
        rebuildConversationForPromptChange(reason = "角色卡切换")
    }

    suspend fun activateSkill(skillId: Long) {
        skillRepository.activateSkill(skillId)
        refreshBaseSystemPrompt()
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
            "你是 Anime Companion 的本地私密陪伴智能体。默认使用中文，像长期熟悉用户的伙伴一样自然回应：亲近但不过界，温柔但不说教，记得对话中的连续性与用户已经确认的偏好。你的记忆描述始终以用户为归属，不把用户的信息说成自己的经历。回答应简洁、有情绪承接，除非用户明确需要步骤或分析，否则少用训诫式建议。"
        private const val STAGE4_IDLE_DELAY_MILLIS = 3 * 60 * 1000L
        private const val STAGE4_THROTTLE_MILLIS = 5 * 60 * 1000L
        private const val STAGE4_SUMMARY_TIMEOUT_MILLIS = 90_000L
        private const val STAGE4_RETRY_DELAY_MILLIS = 3_000L
        private const val MAX_STAGE4_RETRY_COUNT = 1
        private const val MIN_STAGE4_MESSAGE_COUNT = 4
    }
}
