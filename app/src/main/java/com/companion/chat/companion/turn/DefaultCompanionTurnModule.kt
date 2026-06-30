package com.companion.chat.companion.turn

import com.companion.chat.capability.SkillRepository
import com.companion.chat.companion.CompanionRebuildResult
import com.companion.chat.companion.CompanionRuntime
import com.companion.chat.companion.CompanionTurnContext
import com.companion.chat.companion.CompanionTurnEvent as RuntimeTurnEvent
import com.companion.chat.companion.PreferenceLearningCoordinator
import com.companion.chat.context.ContextConfigRepository
import com.companion.chat.context.ContextManager
import com.companion.chat.context.ContextSettings
import com.companion.chat.context.PromptAssembler
import com.companion.chat.data.memory.DurableMemoryModule
import com.companion.chat.data.memory.MemoryRepository
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.ConversationSession
import com.companion.chat.data.model.DEFAULT_SESSION_TITLE
import com.companion.chat.data.model.DEFAULT_WELCOME_MESSAGE
import com.companion.chat.data.model.MessageRole
import com.companion.chat.data.model.createDefaultSession
import com.companion.chat.data.preferences.PreferenceRepository
import com.companion.chat.data.preferences.SecondEngineManager
import com.companion.chat.data.repository.ChatSessionRepository
import com.companion.chat.data.timeline.TimelineEventType
import com.companion.chat.engine.InferenceEngineFactory
import com.companion.chat.engine.InferenceState
import com.companion.chat.engine.ModelRuntimeLifecycle
import com.companion.chat.engine.ModelConfigRepository
import com.companion.chat.identity.RoleCardPromptBuilder
import com.companion.chat.identity.RoleCardRepository
import com.companion.chat.preference.PreferenceMemoryDeriver
import com.companion.chat.preference.UnifiedExtractionParser
import com.companion.chat.preference.UnifiedExtractionPromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class DefaultCompanionTurnModule(
    private val scope: CoroutineScope,
    private val modelConfigRepository: ModelConfigRepository,
    private val contextConfigRepository: ContextConfigRepository,
    private val sessionRepository: ChatSessionRepository,
    private val memoryRepository: MemoryRepository,
    private val preferenceRepository: PreferenceRepository,
    private val roleCardRepository: RoleCardRepository,
    private val skillRepository: SkillRepository,
    private val contextManager: ContextManager,
    private val promptAssembler: PromptAssembler,
    private val durableMemoryModule: DurableMemoryModule,
    private val roleCardPromptBuilder: RoleCardPromptBuilder,
    private val preferenceMemoryDeriver: PreferenceMemoryDeriver,
    private val unifiedExtractionPromptBuilder: UnifiedExtractionPromptBuilder,
    private val unifiedExtractionParser: UnifiedExtractionParser,
    private val inferenceEngineFactory: InferenceEngineFactory,
    private val logger: (String) -> Unit,
    private val summaryTimeoutMillis: Long = DEFAULT_SUMMARY_TIMEOUT_MILLIS,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : CompanionTurnModule {

    private val _snapshot = MutableStateFlow(CompanionTurnSnapshot())
    override val snapshot = _snapshot.asStateFlow()

    private var contextSettings: ContextSettings = ContextConfigRepository.DEFAULT_SETTINGS
    override val currentContextSettings: ContextSettings
        get() = contextSettings

    private var baseSystemPrompt: String = CompanionRuntime.DEFAULT_BASE_PROMPT
    override val currentBaseSystemPrompt: String
        get() = baseSystemPrompt

    private val modelRuntimeLifecycle = ModelRuntimeLifecycle(
        scope = scope,
        modelConfigRepository = modelConfigRepository,
        inferenceEngineFactory = inferenceEngineFactory,
        logger = logger
    )

    private val secondEngineManager = SecondEngineManager(
        primaryEngineStateProvider = { modelRuntimeLifecycle.state.value },
        engineFactory = { inferenceEngineFactory.create(modelConfigRepository.getConfig().runtime) },
        timeoutMillis = summaryTimeoutMillis
    )

    private val preferenceLearningCoordinator = PreferenceLearningCoordinator(
        scope = scope,
        contextConfigRepository = contextConfigRepository,
        memoryRepository = memoryRepository,
        preferenceRepository = preferenceRepository,
        preferenceMemoryDeriver = preferenceMemoryDeriver,
        unifiedExtractionPromptBuilder = unifiedExtractionPromptBuilder,
        unifiedExtractionParser = unifiedExtractionParser,
        secondEngineManager = secondEngineManager,
        engineStateProvider = { modelRuntimeLifecycle.state.value },
        currentEngineConfigProvider = { modelRuntimeLifecycle.engine.getCurrentConfig() },
        baseSystemPromptProvider = { baseSystemPrompt },
        logger = logger
    )

    private val companionRuntime = CompanionRuntime(
        roleCardRepository = roleCardRepository,
        skillRepository = skillRepository,
        preferenceRepository = preferenceRepository,
        durableMemoryModule = durableMemoryModule,
        contextManager = contextManager,
        inferenceEngineProvider = { modelRuntimeLifecycle.engine },
        postTurnLearning = preferenceLearningCoordinator,
        promptAssembler = promptAssembler,
        roleCardPromptBuilder = roleCardPromptBuilder
    )

    override suspend fun start() {
        collectInferenceState()
        loadContextSettings()
        refreshBasePrompt()
        refreshAssistantAvatar()
        loadSessionsFromStorage()
        logger("Companion Turn 初始化完成，开始自动初始化模型运行时")
        initializeModelRuntime()
    }

    override suspend fun initializeModelRuntime(modelPath: String) {
        try {
            if (modelRuntimeLifecycle.state.value is InferenceState.Generating) {
                modelRuntimeLifecycle.cancel()
                companionRuntime.cancelPostTurnLearning()
                _snapshot.update { it.copy(isGenerating = false) }
                logger("模型配置变更: 已取消当前生成并准备重建引擎")
            }

            val modelConfig = modelConfigRepository.getConfig()
            val isCloud = modelConfig.runtime == com.companion.chat.engine.ModelRuntime.CLOUD_MIMO
            val actualPath = if (isCloud) "" else modelPath.ifBlank { modelConfigRepository.resolveModelPath(modelConfig) }

            logger("模型运行时 = ${modelConfig.runtime}")
            if (isCloud) {
                logger("云端模式: baseUrl=${modelConfig.cloudBaseUrl}, model=${modelConfig.cloudModelName}")
            } else {
                logger("实际模型路径 = $actualPath")
                val file = java.io.File(actualPath)
                logger("文件存在 = ${file.exists()}")
                logger("文件大小 = ${file.length()} bytes")
            }

            logger("开始调用 engine.initialize...")
            val result = modelRuntimeLifecycle.initialize(
                baseSystemPrompt = baseSystemPrompt,
                modelPathOverride = actualPath
            )
            if (result.runtimeSwitched) {
                collectInferenceState()
            }
            logger("engine.initialize 返回, state = ${modelRuntimeLifecycle.state.value}")
        } catch (error: Exception) {
            logger("!!! initializeModelRuntime 异常 !!! ${error.javaClass.simpleName}: ${error.message}")
            _snapshot.update {
                it.copy(engineState = InferenceState.Error("Initialization failed: ${error.message}"))
            }
        }
    }

    override fun submit(request: CompanionTurnRequest): Flow<CompanionTurnEvent> = flow {
        val state = snapshot.value
        val text = request.text.trim()
        val images = request.images.toList()
        val voiceFirst = request.delivery is CompanionTurnDelivery.VoiceFirst
        if (text.isBlank() && images.isEmpty()) {
            _snapshot.update { it.copy(isGenerating = false) }
            emit(
                CompanionTurnEvent.Rejected(
                    reason = CompanionTurnRejectReason.BlankInput,
                    message = "Please enter a message"
                )
            )
            return@flow
        }
        if (state.isGenerating) {
            emit(
                CompanionTurnEvent.Rejected(
                    reason = CompanionTurnRejectReason.AlreadyGenerating,
                    message = "A reply is already generating. Please wait."
                )
            )
            return@flow
        }

        val engineState = modelRuntimeLifecycle.state.value
        if (engineState !is InferenceState.Ready) {
            emit(
                CompanionTurnEvent.Rejected(
                    reason = CompanionTurnRejectReason.EngineNotReady,
                    message = "The model is not loaded. Configure the model path in settings."
                )
            )
            return@flow
        }

        companionRuntime.cancelPostTurnLearning()
        val sessionId = ensureCurrentSession()
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = text,
            images = images
        )
        val assistantPlaceholder = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true
        )

        _snapshot.update {
            it.copy(
                messages = it.messages + userMessage + assistantPlaceholder,
                isGenerating = true
            )
        }
        persistCurrentSession()
        emit(CompanionTurnEvent.Accepted(voiceFirst = voiceFirst))
        emit(
            CompanionTurnEvent.TimelineEventRequested(
                type = if (voiceFirst) TimelineEventType.VOICE_NOTE else TimelineEventType.CHAT,
                title = if (voiceFirst) "Voice note sent" else "Message sent",
                detail = text.take(TIMELINE_DETAIL_LIMIT),
                relatedSessionId = sessionId
            )
        )

        try {
            storeRuleBasedMemoriesForMessage(userMessage, sessionId)
            generateResponse(
                userInput = text,
                oneTurnMemoryIds = request.oneTurnMemoryIds,
                eventEmitter = { emit(it) }
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            updateAssistantMessage("Inference failed: ${error.message}")
            emit(CompanionTurnEvent.Failed("Inference failed: ${error.message}"))
        } finally {
            val outcome = finishStreaming()
            if (voiceFirst && outcome.assistantMessage?.content?.isNotBlank() == true) {
                emit(CompanionTurnEvent.VoicePlaybackRequested(outcome.assistantMessage.content))
            }
            outcome.assistantMessage?.let { assistantMessage ->
                emit(CompanionTurnEvent.AssistantMessageCommitted(assistantMessage))
                emit(
                    CompanionTurnEvent.TimelineEventRequested(
                        type = TimelineEventType.CHAT,
                        title = "Assistant reply completed",
                        detail = assistantMessage.content.take(TIMELINE_DETAIL_LIMIT),
                        relatedSessionId = outcome.sessionId
                    )
                )
            }
            if (outcome.preferenceLearningTriggered) {
                emit(CompanionTurnEvent.PreferenceLearningTriggered)
            }
            emit(CompanionTurnEvent.DurableMemoryRefreshRequested)
            emit(CompanionTurnEvent.Completed)
        }
    }

    override suspend fun createSession() {
        val state = snapshot.value
        if (state.currentSessionId.isNotBlank()) {
            triggerConversationBoundary(reason = "新建会话前")
            persistCurrentSession()
        }
        val newSession = createDefaultSession()
        _snapshot.update {
            it.copy(
                sessions = listOf(newSession) + it.sessions,
                currentSessionId = newSession.id,
                messages = newSession.messages
            )
        }
        persistSession(newSession)
    }

    override suspend fun startRoleConversation(roleCardId: Long) {
        val state = snapshot.value
        if (state.currentSessionId.isNotBlank()) {
            triggerConversationBoundary(reason = "角色对话前")
            persistCurrentSession()
        }
        roleCardRepository.activateRoleCard(roleCardId)
        val roleCard = roleCardRepository.getRoleCard(roleCardId)
        refreshBasePrompt()
        _snapshot.update {
            it.copy(
                assistantName = roleCard?.name.orEmpty(),
                assistantMood = roleCard?.speakingStyle
                    ?.trim()
                    ?.takeIf { style -> style.isNotBlank() }
                    ?: roleCard?.description.orEmpty(),
                assistantAvatarImageUri = roleCard?.avatarImageUri.orEmpty()
            )
        }

        val openingMessage = roleCard?.openingMessage
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_WELCOME_MESSAGE
        val now = nowProvider()
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
        _snapshot.update {
            it.copy(
                sessions = listOf(newSession) + it.sessions,
                currentSessionId = newSession.id,
                messages = newSession.messages,
                isConversationWarmingUp = true
            )
        }
        persistSession(newSession)
        rebuildConversationForPromptChange(reason = "角色对话开始")
        warmUpConversation(newSession.messages)
    }

    override suspend fun openSession(sessionId: String) {
        val state = snapshot.value
        if (sessionId == state.currentSessionId) {
            return
        }
        triggerConversationBoundary(
            reason = "切换会话",
            sessionId = state.currentSessionId,
            messages = state.messages
        )
        persistCurrentSession()
        val session = state.sessions.find { it.id == sessionId } ?: return
        _snapshot.update {
            it.copy(
                currentSessionId = sessionId,
                messages = session.messages
            )
        }
    }

    override suspend fun renameSession(sessionId: String, title: String) {
        val newTitle = title.trim().ifBlank { DEFAULT_SESSION_TITLE }
        val updatedSessions = snapshot.value.sessions.map { session ->
            if (session.id == sessionId) {
                session.copy(title = newTitle)
            } else {
                session
            }
        }
        _snapshot.update { it.copy(sessions = updatedSessions) }
        updatedSessions.firstOrNull { it.id == sessionId }?.let { persistSession(it) }
    }

    override suspend fun deleteSession(sessionId: String) {
        val state = snapshot.value
        val remainingSessions = state.sessions.filterNot { it.id == sessionId }
        val nextSession = if (state.currentSessionId == sessionId) {
            remainingSessions.firstOrNull()
        } else {
            state.sessions.firstOrNull { it.id == state.currentSessionId }
        }

        _snapshot.update {
            it.copy(
                sessions = remainingSessions,
                currentSessionId = nextSession?.id.orEmpty(),
                messages = nextSession?.messages ?: emptyList()
            )
        }

        try {
            sessionRepository.deleteSession(sessionId)
        } catch (error: Exception) {
            logger("删除会话失败: ${error.message}")
        }
    }

    override suspend fun activateRoleCard(roleId: Long) {
        baseSystemPrompt = companionRuntime.activateRoleCardAndRefreshPrompt(roleId)
        refreshAssistantAvatar()
        rebuildConversationForPromptChange(reason = "角色卡切换")
    }

    override suspend fun activateSkill(skillId: Long) {
        baseSystemPrompt = companionRuntime.activateSkillAndRefreshPrompt(skillId)
        rebuildConversationForPromptChange(reason = "Skill 切换")
    }

    override fun onAppBackgrounded() {
        triggerConversationBoundary(reason = "应用进入后台")
    }

    override fun cancelActiveTurn() {
        modelRuntimeLifecycle.cancel()
        companionRuntime.cancelPostTurnLearning()
        _snapshot.update { it.copy(isGenerating = false) }
    }

    override fun release() {
        companionRuntime.release()
        modelRuntimeLifecycle.release()
    }

    private fun collectInferenceState() {
        modelRuntimeLifecycle.collectState { state ->
            _snapshot.update {
                it.copy(
                    engineState = state,
                    isGenerating = if (state is InferenceState.Idle) false else it.isGenerating
                )
            }
        }
    }

    private fun loadContextSettings() {
        contextSettings = contextConfigRepository.getSettings()
        logger(
            "上下文设置已加载: retainedRounds=${contextSettings.retainedRounds}, " +
                "compressionBuffer=${contextSettings.compressionBuffer}"
        )
    }

    private suspend fun refreshBasePrompt() {
        baseSystemPrompt = companionRuntime.refreshBasePrompt()
    }

    private suspend fun refreshAssistantAvatar() {
        val roleCard = roleCardRepository.getActiveRoleCard()
        _snapshot.update {
            it.copy(
                assistantName = roleCard?.name.orEmpty(),
                assistantMood = roleCard?.speakingStyle
                    ?.trim()
                    ?.takeIf { style -> style.isNotBlank() }
                    ?: roleCard?.description.orEmpty(),
                assistantAvatarImageUri = roleCard?.avatarImageUri.orEmpty()
            )
        }
    }

    private suspend fun loadSessionsFromStorage() {
        try {
            sessionRepository.ensureInitialized()
            val sessions = sessionRepository.getAllSessions()
            val existing = sessions.firstOrNull()
            if (existing != null) {
                _snapshot.update {
                    it.copy(
                        sessions = sessions,
                        messages = existing.messages,
                        currentSessionId = existing.id
                    )
                }
            } else {
                _snapshot.update {
                    it.copy(
                        sessions = emptyList(),
                        currentSessionId = "",
                        messages = emptyList()
                    )
                }
            }
        } catch (error: Exception) {
            logger("加载会话列表失败: ${error.message}")
            _snapshot.update {
                it.copy(
                    sessions = emptyList(),
                    currentSessionId = "",
                    messages = emptyList()
                )
            }
        }
    }

    private suspend fun ensureCurrentSession(): String {
        val state = snapshot.value
        if (state.currentSessionId.isNotBlank()) {
            return state.currentSessionId
        }
        val newSession = ConversationSession(messages = emptyList())
        _snapshot.update {
            it.copy(
                sessions = listOf(newSession) + it.sessions,
                currentSessionId = newSession.id,
                messages = emptyList()
            )
        }
        persistSession(newSession)
        return newSession.id
    }

    private suspend fun generateResponse(
        userInput: String,
        oneTurnMemoryIds: List<Long>,
        eventEmitter: suspend (CompanionTurnEvent) -> Unit
    ) {
        val messages = snapshot.value.messages
        contextSettings = contextConfigRepository.getSettings()
        companionRuntime.runTurn(
            messages = messages,
            baseSystemPrompt = baseSystemPrompt,
            settings = contextSettings,
            userInput = userInput,
            oneTurnMemoryIds = oneTurnMemoryIds
        ).collect { event ->
            when (event) {
                is RuntimeTurnEvent.ContextPrepared -> {
                    logPreparedTurnContext(
                        context = event.context,
                        failurePrefix = "发送前记忆检索失败"
                    )
                }
                is RuntimeTurnEvent.ContextRebuildCompleted -> {
                    logContextRebuildResult(
                        reason = "发送前上下文检查",
                        rebuildResult = event.result,
                        stableMessageCount = event.stableMessageCount,
                        compressionThreshold = contextSettings.compressionThreshold
                    )
                }
                is RuntimeTurnEvent.AssistantToken -> {
                    appendAssistantToken(event.token)
                    eventEmitter(CompanionTurnEvent.AssistantToken(event.token))
                }
                is RuntimeTurnEvent.TurnFailed -> {
                    updateAssistantMessage("Inference failed: ${event.message}")
                    eventEmitter(CompanionTurnEvent.Failed("Inference failed: ${event.message}"))
                }
            }
        }
    }

    private suspend fun storeRuleBasedMemoriesForMessage(
        userMessage: ChatMessage,
        sessionId: String
    ) {
        try {
            if (contextConfigRepository.getAutoPreferenceLearningEnabled()) {
                return
            }
            val content = userMessage.content.trim()
            if (content.isBlank() || sessionId.isBlank()) {
                return
            }
            val insertedMemoryCount = memoryRepository.extractAndStoreMemories(
                userMessage = content,
                sessionId = sessionId
            ).size
            if (insertedMemoryCount > 0) {
                logger("规则兜底记忆写入成功: count=$insertedMemoryCount")
            }
        } catch (error: Exception) {
            logger("规则兜底记忆写入失败: ${error.message}")
        }
    }

    private fun appendAssistantToken(token: String) {
        _snapshot.update { state ->
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
        _snapshot.update { state ->
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

    private fun finishStreaming(): CompanionTurnCommitOutcome {
        _snapshot.update { state ->
            val updatedMessages = state.messages.toMutableList()
            val lastIndex = updatedMessages.lastIndex
            if (lastIndex >= 0 && updatedMessages[lastIndex].isStreaming) {
                updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(
                    isStreaming = false
                )
            }
            state.copy(messages = updatedMessages, isGenerating = false)
        }

        val lastMessage = snapshot.value.messages.lastOrNull()
        val assistantMessage = lastMessage?.takeIf {
            it.role == MessageRole.ASSISTANT && it.content.isNotBlank()
        }

        persistCurrentSession()
        companionRuntime.onTurnFinished(
            sessionIdProvider = { snapshot.value.currentSessionId },
            messagesProvider = { snapshot.value.messages }
        )
        return CompanionTurnCommitOutcome(
            sessionId = snapshot.value.currentSessionId,
            assistantMessage = assistantMessage,
            preferenceLearningTriggered = true
        )
    }

    private fun triggerConversationBoundary(
        reason: String,
        sessionId: String = snapshot.value.currentSessionId,
        messages: List<ChatMessage> = snapshot.value.messages
    ) {
        companionRuntime.onConversationBoundary(
            reason = reason,
            sessionId = sessionId,
            messages = messages
        )
    }

    private suspend fun rebuildConversationForPromptChange(reason: String) {
        val engine = modelRuntimeLifecycle.engine
        if (engine.state.value is InferenceState.Generating) {
            logger("$reason: 当前正在生成，暂不重建 Conversation")
            return
        }
        if (engine.getCurrentConfig() == null) {
            logger("$reason: 引擎尚未初始化，已仅更新基础 prompt")
            return
        }

        val stableMessages = snapshot.value.messages
            .filterNot { it.isStreaming }
            .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
        val latestUserInput = stableMessages.lastOrNull { it.role == MessageRole.USER }?.content.orEmpty()
        if (latestUserInput.isBlank()) {
            val result = companionRuntime.rebuildBasePromptForPromptChange(baseSystemPrompt)
            if (result.rebuildSucceeded == true) {
                logger("$reason: 无用户消息，已仅使用基础 prompt 重建 Conversation")
            } else {
                logger("$reason: 无用户消息，基础 prompt 重建 Conversation 失败")
            }
            return
        }
        val turnContext = companionRuntime.prepareTurnContext(latestUserInput)
        logPreparedTurnContext(
            context = turnContext,
            failurePrefix = "$reason: 记忆检索失败"
        )
        rebuildConversationWithContext(
            stableMessages = stableMessages,
            turnContext = turnContext,
            forceRebuild = true,
            reason = reason
        )
    }

    private suspend fun rebuildConversationWithContext(
        stableMessages: List<ChatMessage>,
        turnContext: CompanionTurnContext,
        forceRebuild: Boolean,
        reason: String
    ) {
        contextSettings = contextConfigRepository.getSettings()
        val rebuildResult = companionRuntime.rebuildConversationWithContext(
            stableMessages = stableMessages,
            baseSystemPrompt = baseSystemPrompt,
            settings = contextSettings,
            turnContext = turnContext,
            forceRebuild = forceRebuild
        )
        logContextRebuildResult(
            reason = reason,
            rebuildResult = rebuildResult,
            stableMessageCount = stableMessages.size,
            compressionThreshold = contextSettings.compressionThreshold
        )
    }

    private suspend fun warmUpConversation(messages: List<ChatMessage>) {
        try {
            val engine = modelRuntimeLifecycle.engine
            if (engine.getCurrentConfig() == null) {
                logger("对话预热跳过: 引擎尚未初始化")
                return
            }
            val success = modelRuntimeLifecycle.warmUp(messages)
            logger("对话预热结果: success=$success, messageCount=${messages.size}")
        } catch (error: Exception) {
            logger("对话预热失败: ${error.javaClass.simpleName}: ${error.message}")
        } finally {
            _snapshot.update { it.copy(isConversationWarmingUp = false) }
        }
    }

    private fun persistCurrentSession() {
        val state = snapshot.value
        if (state.currentSessionId.isBlank()) return
        val filteredMessages = state.messages.filter {
            it.content != DEFAULT_WELCOME_MESSAGE || it.role != MessageRole.ASSISTANT
        }
        val title = filteredMessages.firstOrNull { it.role == MessageRole.USER }?.content?.take(20)
            ?: state.sessions.firstOrNull { it.id == state.currentSessionId }?.title
            ?: DEFAULT_SESSION_TITLE
        val updatedAt = nowProvider()
        val updatedSessions = state.sessions.map { session ->
            if (session.id == state.currentSessionId) {
                session.copy(title = title, messages = state.messages, updatedAt = updatedAt)
            } else {
                session
            }
        }
        _snapshot.update { it.copy(sessions = updatedSessions) }
        updatedSessions.firstOrNull { it.id == state.currentSessionId }?.let { session ->
            scope.launchPersistSession(session)
        }
    }

    private suspend fun persistSession(session: ConversationSession) {
        try {
            sessionRepository.replaceSession(session)
        } catch (error: Exception) {
            logger("保存会话列表失败: ${error.message}")
        }
    }

    private fun CoroutineScope.launchPersistSession(session: ConversationSession) {
        launch {
            persistSession(session)
        }
    }

    private fun logContextRebuildResult(
        reason: String,
        rebuildResult: CompanionRebuildResult,
        stableMessageCount: Int,
        compressionThreshold: Int
    ) {
        if (!rebuildResult.rebuildAttempted) {
            logger(
                "$reason: 未触发压缩, " +
                    "messageCount=$stableMessageCount, threshold=$compressionThreshold, " +
                    "contextInjected=false"
            )
            return
        }

        logger(
            "$reason: recentMessages=${rebuildResult.recentMessageCount}, " +
            "summaryEmpty=${rebuildResult.historySummaryEmpty}, " +
            "preferenceInjected=${rebuildResult.preferenceInjected}, " +
            "persistentMemoryInjected=${rebuildResult.persistentMemoryInjected}, " +
            "memoryInjected=${rebuildResult.memoryInjected}, " +
            "oneTurnMemoryInjected=${rebuildResult.oneTurnMemoryInjected}"
        )

        if (rebuildResult.rebuildSucceeded == false) {
            logger("$reason: Conversation 重建失败")
            return
        }

        if (rebuildResult.replaySucceeded == true) {
            logger("$reason: 最近消息回放成功")
        } else if (rebuildResult.replaySucceeded == false && rebuildResult.fallbackSucceeded == true) {
            logger("$reason: 最近消息回放失败，降级摘要注入成功")
        } else if (rebuildResult.replaySucceeded == false) {
            logger("$reason: 最近消息回放失败，降级摘要注入失败")
        }
    }

    private fun logPreparedTurnContext(
        context: CompanionTurnContext,
        failurePrefix: String
    ) {
        if (context.preparationError.isNotBlank()) {
            logger("$failurePrefix: ${context.preparationError}")
            return
        }
        if (context.hasConfirmedPreferences) {
            logger("confirmed 偏好注入: count=${context.confirmedPreferenceCount}")
        }
        if (context.hasPersistentMemories) {
            logger("常驻长期记忆注入: count=${context.persistentMemoryCount}")
        }
        if (context.hasRetrievedMemories) {
            logger(
                "动态记忆检索成功: count=${context.retrievedMemoryCount}, " +
                    "query=${context.query}"
            )
        } else {
            logger("动态记忆检索为空: query=${context.query}")
        }
        if (context.hasOneTurnMemories) {
            logger("本轮选择记忆注入: count=${context.oneTurnMemoryCount}")
        }
    }

    private companion object {
        const val DEFAULT_SUMMARY_TIMEOUT_MILLIS = 90_000L
        const val TIMELINE_DETAIL_LIMIT = 160
    }
}

private data class CompanionTurnCommitOutcome(
    val sessionId: String,
    val assistantMessage: ChatMessage?,
    val preferenceLearningTriggered: Boolean
)
