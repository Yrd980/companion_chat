package com.companion.chat

import android.app.Application
import com.companion.chat.companion.turn.CompanionTurnModule
import com.companion.chat.companion.turn.DefaultCompanionTurnModule
import com.companion.chat.context.ContextConfigRepository
import com.companion.chat.context.DefaultContextManager
import com.companion.chat.context.PromptAssembler
import com.companion.chat.data.discover.DiscoverRoleRepository
import com.companion.chat.engine.ModelConfigRepository
import com.companion.chat.engine.image.HttpImageGenerationEngine
import com.companion.chat.engine.image.ImageGenerationConfigRepository
import com.companion.chat.engine.image.ImageGenerationEngineSelector
import com.companion.chat.engine.image.LocalImageGenerationEngine
import com.companion.chat.data.local.CompanionDatabase
import com.companion.chat.memory.MemoryPromptBuilder
import com.companion.chat.data.memory.MemoryRepository
import com.companion.chat.preference.PreferenceMemoryDeriver
import com.companion.chat.data.preferences.PreferenceRepository
import com.companion.chat.preference.UnifiedExtractionParser
import com.companion.chat.preference.UnifiedExtractionPromptBuilder
import com.companion.chat.data.repository.ChatSessionRepository
import com.companion.chat.identity.RoleCardPromptBuilder
import com.companion.chat.identity.RoleCardRepository
import com.companion.chat.capability.SkillRepository
import com.companion.chat.engine.voice.CloudAsrConfigRepository
import com.companion.chat.engine.voice.VoiceCloneConfigRepository
import com.companion.chat.engine.voice.VoiceInputConfigRepository
import com.companion.chat.engine.AndroidVoiceInputEngine
import com.companion.chat.engine.AndroidVoiceOutputEngine
import com.companion.chat.engine.HttpVoiceCloneEngine
import com.companion.chat.engine.InferenceEngineFactory
import com.companion.chat.engine.LocalAudioPlaybackEngine
import com.companion.chat.engine.AndroidMossTtsNanoRunner
import com.companion.chat.engine.MossTtsNanoVoiceCloneEngine
import com.companion.chat.engine.RoleAwareVoiceOutputEngine
import com.companion.chat.engine.voice.VoiceCloneEngine
import com.companion.chat.engine.voice.VoiceCloneRequest
import com.companion.chat.engine.voice.VoiceCloneResult
import com.companion.chat.engine.voice.VoiceCloneTestRepository
import kotlinx.coroutines.CoroutineScope

class AppContainer(
    private val application: Application
) {
    val database: CompanionDatabase by lazy { CompanionDatabase.getInstance(application) }

    val modelConfigRepository: ModelConfigRepository by lazy { ModelConfigRepository(application) }
    val contextConfigRepository: ContextConfigRepository by lazy { ContextConfigRepository(application) }
    val imageGenerationConfigRepository: ImageGenerationConfigRepository by lazy {
        ImageGenerationConfigRepository(application)
    }
    val voiceInputConfigRepository: VoiceInputConfigRepository by lazy { VoiceInputConfigRepository(application) }
    val cloudAsrConfigRepository: CloudAsrConfigRepository by lazy { CloudAsrConfigRepository(application) }
    val voiceCloneConfigRepository: VoiceCloneConfigRepository by lazy { VoiceCloneConfigRepository(application) }
    val voiceCloneTestRepository: VoiceCloneTestRepository by lazy { VoiceCloneTestRepository(application) }

    val chatSessionRepository: ChatSessionRepository by lazy { ChatSessionRepository(application, database) }
    val memoryRepository: MemoryRepository by lazy { MemoryRepository(database.memoryDao()) }
    val preferenceRepository: PreferenceRepository by lazy { PreferenceRepository(database.preferenceDao()) }
    val roleCardRepository: RoleCardRepository by lazy { RoleCardRepository(database.roleCardDao()) }
    val skillRepository: SkillRepository by lazy { SkillRepository(database.skillDao()) }
    val discoverRoleRepository: DiscoverRoleRepository by lazy {
        DiscoverRoleRepository(
            context = application,
            roleCardRepository = roleCardRepository
        )
    }

    val inferenceEngineFactory: InferenceEngineFactory by lazy { InferenceEngineFactory(application) }
    val voiceInputEngine: AndroidVoiceInputEngine by lazy { AndroidVoiceInputEngine(application) }
    val androidVoiceOutputEngine: AndroidVoiceOutputEngine by lazy { AndroidVoiceOutputEngine(application) }
    val localAudioPlaybackEngine: LocalAudioPlaybackEngine by lazy { LocalAudioPlaybackEngine(application) }
    val mossTtsNanoVoiceCloneEngine: MossTtsNanoVoiceCloneEngine by lazy {
        MossTtsNanoVoiceCloneEngine(
            context = application,
            modelDirectoryProvider = { voiceCloneConfigRepository.getConfig().mossModelDirectory },
            runner = AndroidMossTtsNanoRunner()
        )
    }
    val httpVoiceCloneEngine: HttpVoiceCloneEngine by lazy {
        HttpVoiceCloneEngine(
            context = application,
            configProvider = { voiceCloneConfigRepository.getConfig() }
        )
    }
    val voiceCloneEngine: VoiceCloneEngine by lazy {
        object : VoiceCloneEngine {
            override suspend fun synthesize(request: VoiceCloneRequest): Result<VoiceCloneResult> {
                val config = voiceCloneConfigRepository.getConfig()
                return if (config.isHttpCloneConfigured) {
                    httpVoiceCloneEngine.synthesize(request)
                } else {
                    mossTtsNanoVoiceCloneEngine.synthesize(request)
                }
            }
        }
    }
    val voiceOutputEngine: RoleAwareVoiceOutputEngine by lazy {
        RoleAwareVoiceOutputEngine(
            fallbackEngine = androidVoiceOutputEngine,
            roleCardRepository = roleCardRepository,
            cloneEngine = voiceCloneEngine,
            localAudioPlaybackEngine = localAudioPlaybackEngine
        )
    }

    val imageGenerationEngine: HttpImageGenerationEngine by lazy { HttpImageGenerationEngine(application) }
    val imageGenerationEngineSelector: ImageGenerationEngineSelector by lazy {
        ImageGenerationEngineSelector(
            httpEngine = imageGenerationEngine,
            localEngine = LocalImageGenerationEngine(application)
        )
    }

    val contextManager: DefaultContextManager by lazy { DefaultContextManager() }
    val promptAssembler: PromptAssembler by lazy { PromptAssembler() }
    val roleCardPromptBuilder: RoleCardPromptBuilder by lazy { RoleCardPromptBuilder() }
    val memoryPromptBuilder: MemoryPromptBuilder by lazy { MemoryPromptBuilder() }
    val unifiedExtractionPromptBuilder: UnifiedExtractionPromptBuilder by lazy {
        UnifiedExtractionPromptBuilder()
    }
    val unifiedExtractionParser: UnifiedExtractionParser by lazy { UnifiedExtractionParser() }
    val preferenceMemoryDeriver: PreferenceMemoryDeriver by lazy { PreferenceMemoryDeriver() }

    fun createCompanionTurnModule(
        scope: CoroutineScope,
        logger: (String) -> Unit
    ): CompanionTurnModule {
        return DefaultCompanionTurnModule(
            scope = scope,
            modelConfigRepository = modelConfigRepository,
            contextConfigRepository = contextConfigRepository,
            sessionRepository = chatSessionRepository,
            memoryRepository = memoryRepository,
            preferenceRepository = preferenceRepository,
            roleCardRepository = roleCardRepository,
            skillRepository = skillRepository,
            voiceOutputEngine = voiceOutputEngine,
            contextManager = contextManager,
            promptAssembler = promptAssembler,
            memoryPromptBuilder = memoryPromptBuilder,
            roleCardPromptBuilder = roleCardPromptBuilder,
            preferenceMemoryDeriver = preferenceMemoryDeriver,
            unifiedExtractionPromptBuilder = unifiedExtractionPromptBuilder,
            unifiedExtractionParser = unifiedExtractionParser,
            inferenceEngineFactory = inferenceEngineFactory,
            logger = logger
        )
    }
}

val Application.appContainer: AppContainer
    get() = (this as CompanionChatApplication).appContainer
