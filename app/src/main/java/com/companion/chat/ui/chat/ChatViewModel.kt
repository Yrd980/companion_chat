package com.companion.chat.ui.chat

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.chat.AppContainer
import com.companion.chat.appContainer
import com.companion.chat.companion.turn.CompanionTurnDelivery
import com.companion.chat.companion.turn.CompanionTurnEvent
import com.companion.chat.companion.turn.CompanionTurnModule
import com.companion.chat.companion.turn.CompanionTurnRejectReason
import com.companion.chat.companion.turn.CompanionTurnRequest
import com.companion.chat.companion.turn.DefaultCompanionTurnModule
import com.companion.chat.engine.BackendType
import com.companion.chat.engine.InferenceState
import com.companion.chat.engine.VoiceInputEvent
import com.companion.chat.engine.VoiceOutputState
import com.companion.chat.engine.image.ImageGenerationPurpose
import com.companion.chat.engine.image.ImageGenerationRequest
import com.companion.chat.engine.image.ImageGenerationState
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.ConversationSession
import com.companion.chat.data.model.DEFAULT_SESSION_TITLE
import com.companion.chat.data.model.MessageRole
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
    val voiceInputPreview: String = "",
    val isConversationWarmingUp: Boolean = false,
    val imageGenerationState: ImageGenerationState = ImageGenerationState.Idle,
    val imageGenerationError: String = "",
    val assistantAvatarImageUri: String = "",
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
    val voiceOutputEngine = container.voiceOutputEngine
    private val imageGenerationConfigRepository = container.imageGenerationConfigRepository
    private val imageGenerationEngineSelector = container.imageGenerationEngineSelector
    private val companionTurnModule: CompanionTurnModule = DefaultCompanionTurnModule(
        scope = viewModelScope,
        contextConfigRepository = container.contextConfigRepository,
        sessionRepository = container.chatSessionRepository,
        memoryRepository = container.memoryRepository,
        preferenceRepository = container.preferenceRepository,
        roleCardRepository = container.roleCardRepository,
        skillRepository = container.skillRepository,
        voiceOutputEngine = voiceOutputEngine,
        contextManager = container.contextManager,
        promptAssembler = container.promptAssembler,
        memoryPromptBuilder = container.memoryPromptBuilder,
        roleCardPromptBuilder = container.roleCardPromptBuilder,
        preferenceMemoryDeriver = container.preferenceMemoryDeriver,
        unifiedExtractionPromptBuilder = container.unifiedExtractionPromptBuilder,
        unifiedExtractionParser = container.unifiedExtractionParser,
        inferenceEngineProvider = { inferenceEngine },
        inferenceEngineFactory = { inferenceEngineFactory.create(modelConfigRepository.getConfig().runtime) },
        currentEngineConfigProvider = { inferenceEngine.getCurrentConfig() },
        logger = ::logToFile
    )

    private var generateJob: Job? = null
    private var voiceCollectJob: Job? = null
    private var inferenceStateJob: Job? = null

    init {
        logToFile("=== ChatViewModel 创建 ===")
        collectInferenceState()
        collectVoiceEvents()
        collectVoiceOutputState()
        collectImageGenerationState()
        collectCompanionTurnSnapshot()
        voiceInputEngine.warmUp()

        viewModelScope.launch {
            companionTurnModule.start()
            logToFile("ChatViewModel 初始化完成，开始自动初始化引擎")
            initializeEngine(systemPrompt = companionTurnModule.currentBaseSystemPrompt)
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

    internal fun debugBaseSystemPrompt(): String = companionTurnModule.currentBaseSystemPrompt

    private fun collectCompanionTurnSnapshot() {
        viewModelScope.launch {
            companionTurnModule.snapshot.collectLatest { snapshot ->
                _uiState.update {
                    it.copy(
                        sessions = snapshot.sessions,
                        currentSessionId = snapshot.currentSessionId,
                        messages = snapshot.messages,
                        assistantAvatarImageUri = snapshot.assistantAvatarImageUri,
                        isGenerating = snapshot.isGenerating,
                        isConversationWarmingUp = snapshot.isConversationWarmingUp
                    )
                }
            }
        }
    }

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
                logToFile("语音输入事件: ${voiceEventLabel(event)}")
                when (event) {
                    is VoiceInputEvent.WarmedUp -> {
                        _uiState.update { it.copy(isVoiceWarmedUp = true) }
                    }
                    is VoiceInputEvent.PartialResult -> {
                        _uiState.update {
                            it.copy(
                                inputText = event.text,
                                voiceInputPreview = event.text,
                                voiceInputError = ""
                            )
                        }
                    }
                    is VoiceInputEvent.FinalResult -> {
                        val transcript = event.text.trim()
                        _uiState.update {
                            it.copy(
                                inputText = transcript,
                                isVoiceStarting = false,
                                isVoiceListening = false,
                                voiceInputError = "",
                                lastVoiceTranscript = transcript,
                                voiceInputPreview = transcript
                            )
                        }
                        handleVoiceTranscript(transcript)
                    }
                    is VoiceInputEvent.Listening -> {
                        _uiState.update {
                            it.copy(
                                isVoiceStarting = false,
                                isVoiceListening = true,
                                voiceInputError = "",
                                voiceInputPreview = "正在听..."
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
                                voiceInputError = event.message,
                                voiceInputPreview = ""
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
        val state = _uiState.value
        if (state.inputText.isBlank() && state.selectedImages.isEmpty()) {
            _uiState.update { it.copy(isVoiceAutoSending = false) }
            return
        }
        if (state.isGenerating) {
            _uiState.update { it.copy(isVoiceAutoSending = false) }
            return
        }

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            companionTurnModule.submit(
                CompanionTurnRequest(
                    text = state.inputText,
                    images = state.selectedImages.toList(),
                    delivery = if (autoSpeakResponse) {
                        CompanionTurnDelivery.VoiceFirst
                    } else {
                        CompanionTurnDelivery.TextOnly
                    }
                )
            ).collect { event ->
                when (event) {
                    is CompanionTurnEvent.Accepted -> {
                        _uiState.update {
                            it.copy(
                                inputText = "",
                                selectedImages = emptyList(),
                                isVoiceAutoSending = event.voiceFirst
                            )
                        }
                    }
                    is CompanionTurnEvent.ContextRebuildCompleted -> {
                        logContextRebuildResult(event)
                    }
                    is CompanionTurnEvent.Rejected -> {
                        handleRejectedTurn(event)
                    }
                    is CompanionTurnEvent.Failed -> {
                        logToFile(event.message)
                    }
                    CompanionTurnEvent.Completed -> {
                        _uiState.update { it.copy(isVoiceAutoSending = false) }
                    }
                    is CompanionTurnEvent.AssistantToken -> Unit
                }
            }
        }
    }

    private fun handleRejectedTurn(event: CompanionTurnEvent.Rejected) {
        when (event.reason) {
            CompanionTurnRejectReason.BlankInput,
            CompanionTurnRejectReason.AlreadyGenerating -> {
                _uiState.update { it.copy(isVoiceAutoSending = false) }
            }
            CompanionTurnRejectReason.EngineNotReady -> {
                _uiState.update {
                    it.copy(
                        isVoiceAutoSending = false,
                        voiceInputError = if (it.lastVoiceTranscript.isNotBlank()) {
                            event.message
                        } else {
                            it.voiceInputError
                        }
                    )
                }
            }
        }
        logToFile(event.message)
    }

    fun toggleVoiceListening() {
        logToFile(
            "语音输入按钮点击: isVoiceStarting=${_uiState.value.isVoiceStarting}, " +
                "isVoiceListening=${_uiState.value.isVoiceListening}, " +
                "showPermission=${_uiState.value.showVoicePermissionDialog}"
        )
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
                    voiceInputPreview = "正在启动语音识别...",
                    showVoicePermissionDialog = true
                )
            }
        }
    }

    fun onVoicePermissionGranted() {
        logToFile("语音权限已授予，开始启动语音输入")
        _uiState.update { it.copy(showVoicePermissionDialog = false) }
        voiceInputEngine.startListening()
    }

    fun onVoicePermissionDenied() {
        logToFile("语音权限被拒绝")
        _uiState.update {
            it.copy(
                isVoiceStarting = false,
                showVoicePermissionDialog = false,
                voiceInputError = "缺少录音权限，无法使用语音输入",
                voiceInputPreview = ""
            )
        }
    }

    fun clearVoiceInputError() {
        _uiState.update { it.copy(voiceInputError = "") }
    }

    private fun voiceEventLabel(event: VoiceInputEvent): String {
        return when (event) {
            is VoiceInputEvent.WarmedUp -> "WarmedUp"
            is VoiceInputEvent.PartialResult -> "PartialResult(length=${event.text.length})"
            is VoiceInputEvent.FinalResult -> "FinalResult(length=${event.text.length})"
            is VoiceInputEvent.Listening -> "Listening"
            is VoiceInputEvent.NotListening -> "NotListening"
            is VoiceInputEvent.Error -> "Error(${event.message})"
        }
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
        companionTurnModule.cancelActiveTurn()
        _uiState.update { it.copy(isGenerating = false, isVoiceAutoSending = false) }
    }

    fun initializeEngine(modelPath: String = "", systemPrompt: String = "") {
        viewModelScope.launch {
            try {
                if (inferenceEngine.state.value is InferenceState.Generating) {
                    generateJob?.cancel()
                    companionTurnModule.cancelActiveTurn()
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
                    companionTurnModule.currentBaseSystemPrompt
                }
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
                persistActualBackendIfNeeded(modelConfig.backend)
                logToFile("engine.initialize 返回, state = ${inferenceEngine.state.value}")
            } catch (e: Exception) {
                logToFile("!!! initializeEngine 异常 !!! ${e.javaClass.simpleName}: ${e.message}")
                _uiState.update {
                    it.copy(engineState = InferenceState.Error("初始化异常: ${e.message}"))
                }
            }
        }
    }

    private fun persistActualBackendIfNeeded(requestedBackend: BackendType) {
        val actualBackend = inferenceEngine.getCurrentConfig()?.backend ?: return
        if (actualBackend == requestedBackend) return
        if (requestedBackend == BackendType.CPU) return

        val latestConfig = modelConfigRepository.getConfig()
        modelConfigRepository.updateConfig(latestConfig.copy(backend = actualBackend))
        logToFile("模型后端已同步为实际可用后端: $requestedBackend -> $actualBackend")
    }

    override fun onCleared() {
        super.onCleared()
        generateJob?.cancel()
        voiceCollectJob?.cancel()
        inferenceStateJob?.cancel()
        companionTurnModule.release()
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
        viewModelScope.launch {
            companionTurnModule.createSession()
            _uiState.update { it.copy(showSessionDrawer = false, sessionSearchQuery = "") }
        }
    }

    suspend fun startRoleConversation(roleId: Long) {
        companionTurnModule.startRoleConversation(roleId)
        _uiState.update {
            it.copy(
                showSessionDrawer = false,
                sessionSearchQuery = "",
                inputText = "",
                selectedImages = emptyList()
            )
        }
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
        _uiState.update {
            it.copy(
                editingSessionId = "",
                editingTitle = ""
            )
        }
        viewModelScope.launch {
            companionTurnModule.renameSession(state.editingSessionId, newTitle)
        }
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
        viewModelScope.launch {
            companionTurnModule.openSession(sessionId)
            _uiState.update {
                it.copy(
                    showSessionDrawer = false,
                    sessionSearchQuery = "",
                    inputText = "",
                    selectedImages = emptyList()
                )
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            companionTurnModule.deleteSession(sessionId)
            _uiState.update {
                it.copy(
                    showSessionDrawer = false,
                    sessionSearchQuery = "",
                    editingSessionId = if (it.editingSessionId == sessionId) "" else it.editingSessionId,
                    editingTitle = if (it.editingSessionId == sessionId) "" else it.editingTitle
                )
            }
        }
    }

    fun onAppBackgrounded() {
        companionTurnModule.onAppBackgrounded()
    }

    private fun logContextRebuildResult(event: CompanionTurnEvent.ContextRebuildCompleted) {
        val rebuildResult = event.result
        if (!rebuildResult.rebuildAttempted) {
            logToFile(
                "${event.reason}: 未触发压缩, " +
                    "messageCount=${event.stableMessageCount}, threshold=${event.compressionThreshold}, " +
                    "contextInjected=false"
            )
            return
        }

        logToFile(
            "${event.reason}: recentMessages=${rebuildResult.recentMessageCount}, " +
                "summaryEmpty=${rebuildResult.historySummaryEmpty}, " +
                "preferenceInjected=${rebuildResult.preferenceInjected}, " +
                "persistentMemoryInjected=${rebuildResult.persistentMemoryInjected}, " +
                "memoryInjected=${rebuildResult.memoryInjected}"
        )

        if (rebuildResult.rebuildSucceeded == false) {
            logToFile("${event.reason}: Conversation 重建失败")
            return
        }

        if (rebuildResult.replaySucceeded == true) {
            logToFile("${event.reason}: 最近消息回放成功")
        } else if (rebuildResult.replaySucceeded == false && rebuildResult.fallbackSucceeded == true) {
            logToFile("${event.reason}: 最近消息回放失败，降级摘要注入成功")
        } else if (rebuildResult.replaySucceeded == false) {
            logToFile("${event.reason}: 最近消息回放失败，降级摘要注入失败")
        }
    }

    suspend fun activateRoleCard(roleId: Long) {
        companionTurnModule.activateRoleCard(roleId)
    }

    suspend fun activateSkill(skillId: Long) {
        companionTurnModule.activateSkill(skillId)
    }
}
