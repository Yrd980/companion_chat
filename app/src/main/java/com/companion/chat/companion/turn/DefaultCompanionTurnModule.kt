package com.companion.chat.companion.turn

import com.companion.chat.capability.SkillRepository
import com.companion.chat.companion.CompanionRebuildResult
import com.companion.chat.companion.CompanionRuntime
import com.companion.chat.companion.CompanionTurnEvent as RuntimeTurnEvent
import com.companion.chat.companion.PreferenceLearningAdapter
import com.companion.chat.companion.PreferenceLearningCoordinator
import com.companion.chat.context.ContextConfigRepository
import com.companion.chat.context.ContextManager
import com.companion.chat.context.ContextSettings
import com.companion.chat.context.PromptAssembler
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
import com.companion.chat.engine.EngineConfig
import com.companion.chat.engine.InferenceEngine
import com.companion.chat.engine.InferenceState
import com.companion.chat.engine.VoiceOutputEngine
import com.companion.chat.identity.RoleCardPromptBuilder
import com.companion.chat.identity.RoleCardRepository
import com.companion.chat.memory.MemoryPromptBuilder
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

class DefaultCompanionTurnModule(
    private val scope: CoroutineScope,
    private val contextConfigRepository: ContextConfigRepository,
    private val sessionRepository: ChatSessionRepository,
    private val memoryRepository: MemoryRepository,
    private val preferenceRepository: PreferenceRepository,
    private val roleCardRepository: RoleCardRepository,
    private val skillRepository: SkillRepository,
    private val voiceOutputEngine: VoiceOutputEngine,
    private val contextManager: ContextManager,
    private val promptAssembler: PromptAssembler,
    private val memoryPromptBuilder: MemoryPromptBuilder,
    private val roleCardPromptBuilder: RoleCardPromptBuilder,
    private val preferenceMemoryDeriver: PreferenceMemoryDeriver,
    private val unifiedExtractionPromptBuilder: UnifiedExtractionPromptBuilder,
    private val unifiedExtractionParser: UnifiedExtractionParser,
    private val inferenceEngineProvider: () -> InferenceEngine,
    private val inferenceEngineFactory: () -> InferenceEngine,
    private val currentEngineConfigProvider: () -> EngineConfig?,
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

    private val secondEngineManager = SecondEngineManager(
        primaryEngineStateProvider = { inferenceEngineProvider().state.value },
        engineFactory = inferenceEngineFactory,
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
        engineStateProvider = { inferenceEngineProvider().state.value },
        currentEngineConfigProvider = currentEngineConfigProvider,
        baseSystemPromptProvider = { baseSystemPrompt },
        logger = logger
    )

    private val companionRuntime = CompanionRuntime(
        roleCardRepository = roleCardRepository,
        skillRepository = skillRepository,
        preferenceRepository = preferenceRepository,
        memoryRepository = memoryRepository,
        contextManager = contextManager,
        inferenceEngineProvider = inferenceEngineProvider,
        postTurnLearning = PreferenceLearningAdapter(preferenceLearningCoordinator),
        promptAssembler = promptAssembler,
        memoryPromptBuilder = memoryPromptBuilder,
        roleCardPromptBuilder = roleCardPromptBuilder
    )

    override suspend fun start() {
        loadContextSettings()
        refreshBasePrompt()
        refreshAssistantAvatar()
        loadSessionsFromStorage()
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
                    message = "请输入内容"
                )
            )
            return@flow
        }
        if (state.isGenerating) {
            emit(
                CompanionTurnEvent.Rejected(
                    reason = CompanionTurnRejectReason.AlreadyGenerating,
                    message = "正在生成回复，请稍后再说"
                )
            )
            return@flow
        }

        val engineState = inferenceEngineProvider().state.value
        if (engineState !is InferenceState.Ready) {
            emit(
                CompanionTurnEvent.Rejected(
                    reason = CompanionTurnRejectReason.EngineNotReady,
                    message = "模型未加载，请在设置中配置模型路径。"
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
        saveCurrentSession()
        emit(CompanionTurnEvent.Accepted(voiceFirst = voiceFirst))

        try {
            storeRuleBasedMemoriesForMessage(userMessage, sessionId)
            generateResponse(userInput = text, eventEmitter = { emit(it) })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            updateAssistantMessage("推理出错: ${error.message}")
            emit(CompanionTurnEvent.Failed("推理出错: ${error.message}"))
        } finally {
            finishStreaming(shouldSpeak = voiceFirst)
            emit(CompanionTurnEvent.Completed)
        }
    }

    override suspend fun createSession() {
        val state = snapshot.value
        if (state.currentSessionId.isNotBlank()) {
            triggerConversationBoundary(reason = "新建会话前")
            saveCurrentSession()
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
            saveCurrentSession()
        }
        roleCardRepository.activateRoleCard(roleCardId)
        val roleCard = roleCardRepository.getRoleCard(roleCardId)
        refreshBasePrompt()
        _snapshot.update { it.copy(assistantAvatarImageUri = roleCard?.avatarImageUri.orEmpty()) }

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
        saveCurrentSession()
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
        inferenceEngineProvider().cancel()
        companionRuntime.cancelPostTurnLearning()
        _snapshot.update { it.copy(isGenerating = false) }
    }

    override fun release() {
        companionRuntime.release()
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
        val avatarUri = roleCardRepository.getActiveRoleCard()?.avatarImageUri.orEmpty()
        _snapshot.update { it.copy(assistantAvatarImageUri = avatarUri) }
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
        eventEmitter: suspend (CompanionTurnEvent) -> Unit
    ) {
        val messages = snapshot.value.messages
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
                is RuntimeTurnEvent.ContextRebuildCompleted -> {
                    eventEmitter(
                        CompanionTurnEvent.ContextRebuildCompleted(
                            reason = "发送前上下文检查",
                            result = event.result,
                            stableMessageCount = event.stableMessageCount,
                            compressionThreshold = contextSettings.compressionThreshold
                        )
                    )
                }
                is RuntimeTurnEvent.AssistantToken -> {
                    appendAssistantToken(event.token)
                    eventEmitter(CompanionTurnEvent.AssistantToken(event.token))
                }
                is RuntimeTurnEvent.TurnFailed -> {
                    updateAssistantMessage("推理出错: ${event.message}")
                    eventEmitter(CompanionTurnEvent.Failed("推理出错: ${event.message}"))
                }
            }
        }
    }

    private suspend fun buildMemoryContext(userInput: String): MemoryContext {
        return try {
            val confirmedPreferencePrompt = companionRuntime.buildConfirmedPreferencePrompt()
            val companionMemoryContext = companionRuntime.buildMemoryContext(userInput)
            val persistentPrompt = companionMemoryContext.persistentPrompt
            val memoryPrompt = companionMemoryContext.retrievedPrompt
            if (confirmedPreferencePrompt.isNotBlank()) {
                logger("confirmed 偏好注入: count=${preferenceRepository.getConfirmedPreferences().size}")
            }
            if (persistentPrompt.isNotBlank()) {
                logger("常驻长期记忆注入: count=${companionMemoryContext.persistentMemoryCount}")
            }
            if (memoryPrompt.isNotBlank()) {
                logger(
                    "动态记忆检索成功: count=${companionMemoryContext.retrievedMemoryCount}, " +
                        "query=${userInput.trim()}"
                )
            } else {
                logger("动态记忆检索为空: query=${userInput.trim()}")
            }
            MemoryContext(
                confirmedPreferencePrompt = confirmedPreferencePrompt,
                persistentPrompt = persistentPrompt,
                retrievedPrompt = memoryPrompt
            )
        } catch (error: Exception) {
            logger("发送前记忆检索失败: ${error.message}")
            MemoryContext()
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

    private suspend fun finishStreaming(shouldSpeak: Boolean) {
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
        if (
            shouldSpeak &&
            lastMessage?.role == MessageRole.ASSISTANT &&
            lastMessage.content.isNotBlank()
        ) {
            voiceOutputEngine.speak(lastMessage.content)
        }

        saveCurrentSession()
        companionRuntime.onTurnFinished(
            sessionIdProvider = { snapshot.value.currentSessionId },
            messagesProvider = { snapshot.value.messages }
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
        val engine = inferenceEngineProvider()
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
        logContextRebuildResult(
            reason = reason,
            rebuildResult = rebuildResult,
            stableMessageCount = stableMessages.size,
            compressionThreshold = contextSettings.compressionThreshold
        )
    }

    private suspend fun warmUpConversation(messages: List<ChatMessage>) {
        try {
            val engine = inferenceEngineProvider()
            if (engine.getCurrentConfig() == null) {
                logger("对话预热跳过: 引擎尚未初始化")
                return
            }
            val success = engine.warmUp(messages)
            logger("对话预热结果: success=$success, messageCount=${messages.size}")
        } catch (error: Exception) {
            logger("对话预热失败: ${error.javaClass.simpleName}: ${error.message}")
        } finally {
            _snapshot.update { it.copy(isConversationWarmingUp = false) }
        }
    }

    private fun saveCurrentSession() {
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
                "memoryInjected=${rebuildResult.memoryInjected}"
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

    private data class MemoryContext(
        val confirmedPreferencePrompt: String = "",
        val persistentPrompt: String = "",
        val retrievedPrompt: String = ""
    )

    private companion object {
        const val DEFAULT_SUMMARY_TIMEOUT_MILLIS = 90_000L
    }
}
