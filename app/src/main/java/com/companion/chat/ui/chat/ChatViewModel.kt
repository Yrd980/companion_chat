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
import com.companion.chat.companion.voice.VoiceFirstInteractionState
import com.companion.chat.companion.voice.VoiceFirstTurnPolicy
import com.companion.chat.companion.voice.VoiceTranscriptDecision
import com.companion.chat.engine.InferenceState
import com.companion.chat.engine.VoiceInputEvent
import com.companion.chat.engine.VoiceOutputState
import com.companion.chat.engine.image.ImageGenerationPurpose
import com.companion.chat.engine.image.ImageGenerationRequest
import com.companion.chat.engine.image.ImageGenerationState
import com.companion.chat.data.local.entity.Memory
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.ConversationSession
import com.companion.chat.data.model.DEFAULT_SESSION_TITLE
import com.companion.chat.data.model.MessageRole
import com.companion.chat.data.timeline.TimelineEvent
import com.companion.chat.data.timeline.TimelineEventType
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
    val voice: VoiceFirstInteractionState = VoiceFirstInteractionState(),
    val isConversationWarmingUp: Boolean = false,
    val imageGenerationState: ImageGenerationState = ImageGenerationState.Idle,
    val imageGenerationError: String = "",
    val assistantName: String = "",
    val assistantMood: String = "",
    val assistantAvatarImageUri: String = "",
    val engineState: InferenceState = InferenceState.Idle,
    val diagnosticLog: String = "",
    val sessions: List<ConversationSession> = emptyList(),
    val currentSessionId: String = "",
    val showSessionDrawer: Boolean = false,
    val sessionSearchQuery: String = "",
    val dateFilter: DateFilter = DateFilter.ALL,
    val editingSessionId: String = "",
    val editingTitle: String = "",
    val privacyModeLabel: String = "Local Only",
    val localOnlyMode: Boolean = true,
    val pinnedMemories: List<Memory> = emptyList(),
    val useNextTurnMemory: Memory? = null,
    val timelineEvents: List<TimelineEvent> = emptyList(),
    val voiceNoteDurationLabel: String = ""
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

    val voiceInputEngine = container.voiceInputEngine
    val voiceOutputEngine = container.voiceOutputEngine
    private val readinessRepository = container.companionReadinessRepository
    private val imageGenerationConfigRepository = container.imageGenerationConfigRepository
    private val imageGenerationEngineSelector = container.imageGenerationEngineSelector
    private val memoryRepository = container.memoryRepository
    private val privacySettingsRepository = container.privacySettingsRepository
    private val timelineEventRepository = container.timelineEventRepository
    private val companionTurnModule: CompanionTurnModule = container.createCompanionTurnModule(
        scope = viewModelScope,
        logger = ::logToFile
    )

    private var generateJob: Job? = null
    private var voiceCollectJob: Job? = null

    init {
        logToFile("=== ChatViewModel 创建 ===")
        collectVoiceEvents()
        collectVoiceOutputState()
        collectImageGenerationState()
        collectCompanionTurnSnapshot()
        collectTimelineEvents()
        refreshPrivacyAndMemories()
        voiceInputEngine.warmUp()

        viewModelScope.launch {
            companionTurnModule.start()
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
                        assistantName = snapshot.assistantName,
                        assistantMood = snapshot.assistantMood,
                        assistantAvatarImageUri = snapshot.assistantAvatarImageUri,
                        engineState = snapshot.engineState,
                        isGenerating = snapshot.isGenerating,
                        isConversationWarmingUp = snapshot.isConversationWarmingUp
                    )
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
                        _uiState.update { it.copy(voice = it.voice.copy(isWarmedUp = true)) }
                    }
                    is VoiceInputEvent.PartialResult -> {
                        _uiState.update {
                            it.copy(
                                inputText = event.text,
                                voice = it.voice.copy(
                                    inputPreview = event.text,
                                    inputError = ""
                                )
                            )
                        }
                    }
                    is VoiceInputEvent.FinalResult -> {
                        val transcript = event.text.trim()
                        _uiState.update {
                            it.copy(
                                inputText = transcript,
                                voice = it.voice.copy(
                                    isStarting = false,
                                    isListening = false,
                                    inputError = "",
                                    lastTranscript = transcript,
                                    inputPreview = transcript
                                )
                            )
                        }
                        if (transcript.isNotBlank()) {
                            recordTimeline(
                                type = TimelineEventType.VOICE_NOTE,
                                title = "Voice transcript ready",
                                detail = transcript.take(160),
                                relatedSessionId = _uiState.value.currentSessionId
                            )
                        }
                        handleVoiceTranscript(transcript)
                    }
                    is VoiceInputEvent.Listening -> {
                        _uiState.update {
                            it.copy(
                                voice = it.voice.copy(
                                    isStarting = false,
                                    isListening = true,
                                    inputError = "",
                                    inputPreview = "Listening..."
                                )
                            )
                        }
                    }
                    is VoiceInputEvent.NotListening -> {
                        _uiState.update {
                            it.copy(
                                voice = it.voice.copy(
                                    isStarting = false,
                                    isListening = false
                                )
                            )
                        }
                    }
                    is VoiceInputEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                voice = it.voice.copy(
                                    isStarting = false,
                                    isListening = false,
                                    inputError = event.message,
                                    inputPreview = ""
                                )
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
                    it.copy(
                        voice = it.voice.copy(isSpeaking = state is VoiceOutputState.Speaking)
                    )
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

    private fun collectTimelineEvents() {
        viewModelScope.launch {
            timelineEventRepository.observeRecent(limit = 8).collectLatest { events ->
                _uiState.update { it.copy(timelineEvents = events) }
            }
        }
    }

    private fun refreshPrivacyAndMemories() {
        viewModelScope.launch {
            val privacySettings = privacySettingsRepository.getSettings()
            _uiState.update {
                it.copy(
                    privacyModeLabel = if (privacySettings.localOnlyMode) {
                        "Local Only"
                    } else {
                        "Cloud Optional"
                    },
                    localOnlyMode = privacySettings.localOnlyMode,
                    pinnedMemories = memoryRepository.getPinnedMemories()
                )
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
                recordTimeline(
                    type = TimelineEventType.IMAGE_GENERATED,
                    title = "Image generated",
                    detail = resolvedPrompt.take(120),
                    mediaUri = uri
                )
            }.onFailure { error ->
                logToFile("图片生成失败: ${error.message}")
                _uiState.update {
                    it.copy(
                        imageGenerationState = ImageGenerationState.Error(error.message ?: "Image generation failed"),
                        imageGenerationError = error.message ?: "Image generation failed"
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
            val decision = VoiceFirstTurnPolicy.evaluateTranscript(
                transcript = transcript,
                isGenerating = _uiState.value.isGenerating,
                isEngineReady = _uiState.value.engineState is InferenceState.Ready,
                isVoiceFirstReady = readinessRepository.getSnapshot().isReadyForVoiceFirstTurn
            )
        ) {
            VoiceTranscriptDecision.AutoSend -> {
                submitCurrentMessage(autoSpeakResponse = true)
            }
            is VoiceTranscriptDecision.HoldForUser -> {
                _uiState.update {
                    it.copy(
                        voice = it.voice.copy(
                            inputError = decision.message,
                            isAutoSending = false
                        )
                    )
                }
            }
        }
    }

    private fun submitCurrentMessage(autoSpeakResponse: Boolean) {
        val state = _uiState.value
        if (state.inputText.isBlank() && state.selectedImages.isEmpty()) {
            _uiState.update { it.copy(voice = it.voice.copy(isAutoSending = false)) }
            return
        }
        if (state.isGenerating) {
            _uiState.update { it.copy(voice = it.voice.copy(isAutoSending = false)) }
            return
        }

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            val oneTurnMemoryIds = state.useNextTurnMemory?.let { listOf(it.id) }.orEmpty()
            companionTurnModule.submit(
                CompanionTurnRequest(
                    text = state.inputText,
                    images = state.selectedImages.toList(),
                    delivery = if (autoSpeakResponse) {
                        CompanionTurnDelivery.VoiceFirst
                    } else {
                        CompanionTurnDelivery.TextOnly
                    },
                    oneTurnMemoryIds = oneTurnMemoryIds
                )
            ).collect { event ->
                when (event) {
                    is CompanionTurnEvent.Accepted -> {
                        recordTimeline(
                            type = if (event.voiceFirst) TimelineEventType.VOICE_NOTE else TimelineEventType.CHAT,
                            title = if (event.voiceFirst) "Voice note sent" else "Message sent",
                            detail = state.inputText.take(160),
                            relatedSessionId = state.currentSessionId
                        )
                        _uiState.update {
                            it.copy(
                                inputText = "",
                                selectedImages = emptyList(),
                                useNextTurnMemory = null,
                                voice = it.voice.copy(isAutoSending = event.voiceFirst)
                            )
                        }
                    }
                    is CompanionTurnEvent.Rejected -> {
                        handleRejectedTurn(event)
                    }
                    is CompanionTurnEvent.Failed -> {
                        logToFile(event.message)
                    }
                    CompanionTurnEvent.Completed -> {
                        _uiState.update { it.copy(voice = it.voice.copy(isAutoSending = false)) }
                        recordTimeline(
                            type = TimelineEventType.CHAT,
                            title = "Assistant reply completed",
                            detail = _uiState.value.messages.lastOrNull { message ->
                                message.role == MessageRole.ASSISTANT && message.content.isNotBlank()
                            }?.content.orEmpty().take(160),
                            relatedSessionId = _uiState.value.currentSessionId
                        )
                        refreshPrivacyAndMemories()
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
                _uiState.update { it.copy(voice = it.voice.copy(isAutoSending = false)) }
            }
            CompanionTurnRejectReason.EngineNotReady -> {
                _uiState.update {
                    it.copy(
                        voice = it.voice.copy(
                            isAutoSending = false,
                            inputError = if (it.voice.lastTranscript.isNotBlank()) {
                                event.message
                            } else {
                                it.voice.inputError
                            }
                        )
                    )
                }
            }
        }
        logToFile(event.message)
    }

    fun toggleVoiceListening() {
        logToFile(
            "语音输入按钮点击: isVoiceStarting=${_uiState.value.voice.isStarting}, " +
                "isVoiceListening=${_uiState.value.voice.isListening}, " +
                "showPermission=${_uiState.value.voice.showPermissionDialog}"
        )
        if (_uiState.value.voice.isInputActive) {
            voiceInputEngine.stopListening()
            _uiState.update {
                it.copy(
                    voice = it.voice.copy(
                        isStarting = false,
                        isListening = false,
                        showPermissionDialog = false
                    )
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    voice = it.voice.copy(
                        isStarting = true,
                        inputError = "",
                        inputPreview = "Starting voice recognition...",
                        showPermissionDialog = true
                    )
                )
            }
        }
    }

    fun onVoicePermissionGranted() {
        logToFile("语音权限已授予，开始启动语音输入")
        _uiState.update { it.copy(voice = it.voice.copy(showPermissionDialog = false)) }
        voiceInputEngine.startListening()
    }

    fun onVoicePermissionDenied() {
        logToFile("语音权限被拒绝")
        _uiState.update {
            it.copy(
                voice = it.voice.copy(
                    isStarting = false,
                    showPermissionDialog = false,
                    inputError = "Microphone permission is required for voice input",
                    inputPreview = ""
                )
            )
        }
    }

    fun clearVoiceInputError() {
        _uiState.update { it.copy(voice = it.voice.copy(inputError = "")) }
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

    fun useMemoryNextTurn(memoryId: Long) {
        viewModelScope.launch {
            val memory = _uiState.value.pinnedMemories.firstOrNull { it.id == memoryId }
                ?: memoryRepository.getConfirmedMemoriesByIds(listOf(memoryId)).firstOrNull()
                ?: return@launch
            _uiState.update { it.copy(useNextTurnMemory = memory) }
            recordTimeline(
                type = TimelineEventType.MEMORY_PINNED,
                title = "Memory selected for next turn",
                detail = memory.content.take(160),
                relatedMemoryId = memory.id
            )
        }
    }

    fun clearUseNextTurnMemory() {
        _uiState.update { it.copy(useNextTurnMemory = null) }
    }

    fun cancelGeneration() {
        generateJob?.cancel()
        companionTurnModule.cancelActiveTurn()
        _uiState.update {
            it.copy(
                isGenerating = false,
                voice = it.voice.copy(isAutoSending = false)
            )
        }
    }

    fun initializeEngine(modelPath: String = "") {
        viewModelScope.launch {
            companionTurnModule.initializeModelRuntime(modelPath)
        }
    }

    override fun onCleared() {
        super.onCleared()
        generateJob?.cancel()
        voiceCollectJob?.cancel()
        companionTurnModule.release()
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

    suspend fun activateRoleCard(roleId: Long) {
        companionTurnModule.activateRoleCard(roleId)
    }

    suspend fun activateSkill(skillId: Long) {
        companionTurnModule.activateSkill(skillId)
    }

    private fun recordTimeline(
        type: TimelineEventType,
        title: String,
        detail: String,
        relatedSessionId: String? = null,
        relatedMemoryId: Long? = null,
        mediaUri: String? = null
    ) {
        viewModelScope.launch {
            timelineEventRepository.add(
                type = type,
                title = title,
                detail = detail,
                relatedSessionId = relatedSessionId,
                relatedMemoryId = relatedMemoryId,
                mediaUri = mediaUri
            )
        }
    }
}
