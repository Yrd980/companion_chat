package com.companion.chat.data.dashboard

import com.companion.chat.companion.readiness.CompanionReadinessLevel
import com.companion.chat.companion.readiness.CompanionReadinessRepository
import com.companion.chat.data.local.entity.Memory
import com.companion.chat.data.memory.MemoryRepository
import com.companion.chat.data.timeline.TimelineEvent
import com.companion.chat.data.timeline.TimelineEventRepository
import com.companion.chat.identity.RoleCardRepository
import java.util.concurrent.TimeUnit

class HomeDashboardRepository(
    private val roleCardRepository: RoleCardRepository,
    private val memoryRepository: MemoryRepository,
    private val readinessRepository: CompanionReadinessRepository,
    private val timelineEventRepository: TimelineEventRepository,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun getDashboardState(): HomeDashboardUiState {
        val roleCard = roleCardRepository.getActiveRoleCard()
        val memories = memoryRepository.getAllMemories()
        val readiness = readinessRepository.getSnapshot()
        val events = timelineEventRepository.getRecent()
        val memoryCount = memories.size
        val level = (memoryCount / MEMORIES_PER_LEVEL) + 1
        val xp = (memoryCount % MEMORIES_PER_LEVEL) * XP_PER_MEMORY

        return HomeDashboardUiState(
            relationship = RelationshipSummary(
                companionName = roleCard?.name?.takeIf { it.isNotBlank() } ?: "Aiko Hoshizora",
                companionMood = if (readiness.isReadyForVoiceFirstTurn) "Connected" else "Settling in",
                level = level,
                xp = xp,
                nextLevelXp = MEMORIES_PER_LEVEL * XP_PER_MEMORY,
                closenessLabel = closenessLabel(memoryCount)
            ),
            localDevice = LocalDeviceSummary(
                modelReady = readiness.llm.level == CompanionReadinessLevel.READY,
                voiceReady = readiness.asr.isUsable && readiness.tts.isUsable,
                imageReady = readiness.image.level == CompanionReadinessLevel.READY,
                noHelmetMode = true,
                statusLabel = "Local companion mode"
            ),
            quickActions = buildQuickActions(readiness.llm.isUsable, readiness.asr.isUsable, readiness.image.isUsable),
            recentMemories = memories.take(RECENT_MEMORY_LIMIT).map { it.toHomeMemorySummary() },
            recentActivity = events.map { it.toHomeActivitySummary(nowProvider()) },
            suggestions = buildSuggestions(
                modelReady = readiness.llm.level == CompanionReadinessLevel.READY,
                voiceReady = readiness.asr.isUsable && readiness.tts.isUsable,
                memoryCount = memoryCount
            ),
            isLoading = false
        )
    }

    private fun buildQuickActions(
        modelReady: Boolean,
        voiceReady: Boolean,
        imageReady: Boolean
    ): List<HomeQuickAction> {
        return listOf(
            HomeQuickAction(
                id = "chat",
                title = "Start chat",
                subtitle = if (modelReady) "Continue locally" else "Set up the text model first",
                enabled = modelReady,
                disabledReason = if (modelReady) "" else "Text model package is not ready"
            ),
            HomeQuickAction(
                id = "voice",
                title = "Voice check",
                subtitle = if (voiceReady) "Voice input and output are usable" else "Review voice setup",
                enabled = voiceReady,
                disabledReason = if (voiceReady) "" else "Voice readiness needs attention"
            ),
            HomeQuickAction(
                id = "memory",
                title = "Review memories",
                subtitle = "Inspect local companion memory"
            ),
            HomeQuickAction(
                id = "image",
                title = "Generate image",
                subtitle = if (imageReady) "Image generation is ready" else "Review image model setup",
                enabled = imageReady,
                disabledReason = if (imageReady) "" else "Image generation is not ready"
            )
        )
    }

    private fun buildSuggestions(
        modelReady: Boolean,
        voiceReady: Boolean,
        memoryCount: Int
    ): List<HomeSuggestion> {
        return buildList {
            if (!modelReady) {
                add(HomeSuggestion("model_setup", "Finish local text model setup.", "settings/model"))
            }
            if (!voiceReady) {
                add(HomeSuggestion("voice_setup", "Check local voice input and output readiness.", "settings/voice"))
            }
            if (memoryCount == 0) {
                add(HomeSuggestion("first_memory", "Add a memory so your companion can keep continuity.", "memory"))
            }
            add(HomeSuggestion("privacy", "Review local-only privacy controls.", "profile"))
        }
    }

    private fun Memory.toHomeMemorySummary(): HomeMemorySummary {
        return HomeMemorySummary(
            id = id,
            title = category.ifBlank { "Memory" },
            detail = content,
            category = category
        )
    }

    private fun TimelineEvent.toHomeActivitySummary(now: Long): HomeActivitySummary {
        return HomeActivitySummary(
            id = id,
            title = title,
            detail = detail,
            timestampLabel = createdAt.toRelativeLabel(now)
        )
    }

    private fun Long.toRelativeLabel(now: Long): String {
        val elapsed = (now - this).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
        val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
        val days = TimeUnit.MILLISECONDS.toDays(elapsed)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "Earlier"
        }
    }

    private fun closenessLabel(memoryCount: Int): String {
        return when {
            memoryCount >= 30 -> "Deepening bond"
            memoryCount >= 10 -> "Growing familiar"
            memoryCount > 0 -> "Getting to know you"
            else -> "New companion"
        }
    }

    private companion object {
        const val RECENT_MEMORY_LIMIT = 3
        const val MEMORIES_PER_LEVEL = 5
        const val XP_PER_MEMORY = 20
    }
}
