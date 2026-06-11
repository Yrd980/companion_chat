package com.companion.chat

import android.app.Application
import com.companion.chat.companion.readiness.CompanionReadinessRepository
import com.companion.chat.companion.turn.CompanionTurnModule
import com.companion.chat.companion.turn.DefaultCompanionTurnModule
import com.companion.chat.context.ContextConfigRepository
import com.companion.chat.context.DefaultContextManager
import com.companion.chat.context.PromptAssembler
import com.companion.chat.data.dashboard.HomeDashboardRepository
import com.companion.chat.data.discover.DiscoverRoleRepository
import com.companion.chat.data.export.DataExportRepository
import com.companion.chat.engine.ModelConfigRepository
import com.companion.chat.engine.image.HttpImageGenerationEngine
import com.companion.chat.engine.image.ImageGenerationConfigRepository
import com.companion.chat.engine.image.ImageGenerationEngineSelector
import com.companion.chat.engine.image.LocalImageGenerationEngine
import com.companion.chat.data.local.CompanionDatabase
import com.companion.chat.data.memory.DurableMemoryModule
import com.companion.chat.data.memory.MemoryRepository
import com.companion.chat.data.plan.PlanRepository
import com.companion.chat.data.privacy.PrivacyGate
import com.companion.chat.data.privacy.PrivacySettingsRepository
import com.companion.chat.data.profile.UserProfileRepository
import com.companion.chat.preference.PreferenceMemoryDeriver
import com.companion.chat.data.preferences.PreferenceRepository
import com.companion.chat.preference.UnifiedExtractionParser
import com.companion.chat.preference.UnifiedExtractionPromptBuilder
import com.companion.chat.data.repository.ChatSessionRepository
import com.companion.chat.data.setup.SetupRepository
import com.companion.chat.data.timeline.TimelineEventRepository
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
import com.companion.chat.engine.voice.VoiceCloneTestRepository
import com.companion.chat.engine.voice.role.RoleVoiceCloneRouter
import com.companion.chat.engine.voice.role.RoleVoiceProfileResolver
import com.companion.chat.ui.language.AppLanguageRepository
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
    val companionReadinessRepository: CompanionReadinessRepository by lazy {
        CompanionReadinessRepository(
            modelConfigRepository = modelConfigRepository,
            voiceInputConfigRepository = voiceInputConfigRepository,
            cloudAsrConfigRepository = cloudAsrConfigRepository,
            voiceCloneConfigRepository = voiceCloneConfigRepository,
            imageGenerationConfigRepository = imageGenerationConfigRepository
        )
    }
    val appLanguageRepository: AppLanguageRepository by lazy { AppLanguageRepository(application) }

    val chatSessionRepository: ChatSessionRepository by lazy { ChatSessionRepository(application, database) }
    val memoryRepository: MemoryRepository by lazy { MemoryRepository(database.memoryDao()) }
    val preferenceRepository: PreferenceRepository by lazy { PreferenceRepository(database.preferenceDao()) }
    val roleCardRepository: RoleCardRepository by lazy { RoleCardRepository(database.roleCardDao()) }
    val skillRepository: SkillRepository by lazy { SkillRepository(database.skillDao()) }
    val timelineEventRepository: TimelineEventRepository by lazy {
        TimelineEventRepository(database.timelineEventDao())
    }
    val userProfileRepository: UserProfileRepository by lazy { UserProfileRepository(application) }
    val privacySettingsRepository: PrivacySettingsRepository by lazy {
        PrivacySettingsRepository(application)
    }
    val privacyGate: PrivacyGate by lazy {
        PrivacyGate(settingsProvider = privacySettingsRepository::getSettings)
    }
    val planRepository: PlanRepository by lazy { PlanRepository() }
    val dataExportRepository: DataExportRepository by lazy {
        DataExportRepository(
            context = application,
            database = database
        )
    }
    val setupRepository: SetupRepository by lazy {
        SetupRepository(
            context = application,
            userProfileRepository = userProfileRepository,
            readinessRepository = companionReadinessRepository
        )
    }
    val homeDashboardRepository: HomeDashboardRepository by lazy {
        HomeDashboardRepository(
            roleCardRepository = roleCardRepository,
            durableMemoryModule = durableMemoryModule,
            readinessRepository = companionReadinessRepository,
            timelineEventRepository = timelineEventRepository
        )
    }
    val discoverRoleRepository: DiscoverRoleRepository by lazy {
        DiscoverRoleRepository(
            context = application,
            roleCardRepository = roleCardRepository
        )
    }

    val inferenceEngineFactory: InferenceEngineFactory by lazy { InferenceEngineFactory(application) }
    val voiceInputEngine: AndroidVoiceInputEngine by lazy {
        AndroidVoiceInputEngine(
            context = application,
            privacyGate = privacyGate
        )
    }
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
            configProvider = { voiceCloneConfigRepository.getConfig() },
            privacyGate = privacyGate
        )
    }
    val roleVoiceCloneRouter: RoleVoiceCloneRouter by lazy {
        RoleVoiceCloneRouter(
            voiceCloneConfigRepository = voiceCloneConfigRepository,
            httpCloneEngine = httpVoiceCloneEngine,
            mossTtsNanoEngine = mossTtsNanoVoiceCloneEngine
        )
    }
    val roleVoiceProfileResolver: RoleVoiceProfileResolver by lazy {
        RoleVoiceProfileResolver(roleCardRepository = roleCardRepository)
    }
    val voiceOutputEngine: RoleAwareVoiceOutputEngine by lazy {
        RoleAwareVoiceOutputEngine(
            fallbackEngine = androidVoiceOutputEngine,
            profileResolver = roleVoiceProfileResolver,
            cloneRouter = roleVoiceCloneRouter,
            localAudioPlaybackEngine = localAudioPlaybackEngine
        )
    }

    val imageGenerationEngine: HttpImageGenerationEngine by lazy {
        HttpImageGenerationEngine(
            context = application,
            privacyGate = privacyGate
        )
    }
    val imageGenerationEngineSelector: ImageGenerationEngineSelector by lazy {
        ImageGenerationEngineSelector(
            httpEngine = imageGenerationEngine,
            localEngine = LocalImageGenerationEngine(application)
        )
    }

    val contextManager: DefaultContextManager by lazy { DefaultContextManager() }
    val promptAssembler: PromptAssembler by lazy { PromptAssembler() }
    val roleCardPromptBuilder: RoleCardPromptBuilder by lazy { RoleCardPromptBuilder() }
    val durableMemoryModule: DurableMemoryModule by lazy {
        DurableMemoryModule(memoryRepository)
    }
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
            contextManager = contextManager,
            promptAssembler = promptAssembler,
            durableMemoryModule = durableMemoryModule,
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
