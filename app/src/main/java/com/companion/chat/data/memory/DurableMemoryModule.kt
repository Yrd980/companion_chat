package com.companion.chat.data.memory

import com.companion.chat.data.local.entity.Memory
import com.companion.chat.memory.MemoryPromptBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DurableMemoryModule(
    private val repository: MemoryRepository,
    private val memoryPromptBuilder: MemoryPromptBuilder = MemoryPromptBuilder()
) {

    fun observeReviewProjection(): Flow<DurableMemoryReviewProjection> {
        return repository.observeAllMemories().map { memories ->
            buildReviewProjection(memories)
        }
    }

    suspend fun getReviewProjection(): DurableMemoryReviewProjection {
        return buildReviewProjection(repository.getAllMemories())
    }

    suspend fun prepareInjection(
        query: String,
        oneTurnMemoryIds: List<Long> = emptyList()
    ): DurableMemoryInjection {
        val persistentMemories = repository.getPersistentMemories()
        val retrievedMemories = repository.retrieveRelevantMemories(query)
        val oneTurnMemories = resolveOneTurnMemories(oneTurnMemoryIds)
        return DurableMemoryInjection(
            query = query.trim(),
            persistentMemories = persistentMemories,
            retrievedMemories = retrievedMemories,
            oneTurnMemories = oneTurnMemories,
            persistentMemoryPrompt = memoryPromptBuilder.buildPersistent(persistentMemories),
            retrievedMemoryPrompt = memoryPromptBuilder.build(retrievedMemories),
            oneTurnMemoryPrompt = memoryPromptBuilder.buildOneTurn(oneTurnMemories)
        )
    }

    suspend fun getPinnedProjection(): List<Memory> {
        return repository.getPinnedMemories()
    }

    suspend fun findConfirmedMemory(memoryId: Long): Memory? {
        return repository.getConfirmedMemoriesByIds(listOf(memoryId)).firstOrNull()
    }

    suspend fun keepCandidate(memoryId: Long): Boolean {
        return repository.confirmCandidate(memoryId)
    }

    suspend fun deleteCandidate(memory: Memory) {
        repository.deleteCandidate(memory)
    }

    suspend fun pinMemory(memoryId: Long): Boolean {
        return repository.pinMemory(memoryId)
    }

    suspend fun unpinMemory(memoryId: Long): Boolean {
        return repository.unpinMemory(memoryId)
    }

    suspend fun promoteMemory(memoryId: Long): Boolean {
        return repository.promoteMemory(memoryId)
    }

    suspend fun addManualMemory(content: String, category: String): Memory {
        return repository.addManualMemory(content, category)
    }

    suspend fun updateMemory(memory: Memory) {
        repository.updateMemory(memory)
    }

    suspend fun deleteMemory(memory: Memory) {
        repository.deleteMemory(memory)
    }

    private suspend fun resolveOneTurnMemories(memoryIds: List<Long>): List<Memory> {
        val memories = repository.getConfirmedMemoriesByIds(memoryIds)
        memories.forEach { repository.markMemoryUsed(it.id) }
        return memories
    }

    private fun buildReviewProjection(memories: List<Memory>): DurableMemoryReviewProjection {
        val candidateMemories = memories
            .filter { it.reviewState == MemoryRepository.REVIEW_STATE_CANDIDATE }
            .sortedByDescending { it.createdAt }
        val confirmedMemories = memories
            .filter { it.reviewState != MemoryRepository.REVIEW_STATE_CANDIDATE }
        val pinnedMemories = memories
            .filter { it.isPinned }
            .sortedByDescending { it.updatedAt }
        return DurableMemoryReviewProjection(
            confirmedMemories = confirmedMemories,
            candidateMemories = candidateMemories,
            pinnedMemories = pinnedMemories,
            healthMetrics = DurableMemoryHealth.from(memories, pinnedMemories, candidateMemories)
        )
    }
}

data class DurableMemoryReviewProjection(
    val confirmedMemories: List<Memory>,
    val candidateMemories: List<Memory>,
    val pinnedMemories: List<Memory>,
    val healthMetrics: MemoryHealthMetrics
) {
    fun confirmedForCategory(category: String?): List<Memory> {
        return when (category) {
            null -> confirmedMemories
            "relation" -> confirmedMemories.filter {
                it.category == "relation" || it.category == "relationship"
            }
            else -> confirmedMemories.filter { it.category == category }
        }
    }
}

data class DurableMemoryInjection(
    val query: String,
    val persistentMemories: List<Memory>,
    val retrievedMemories: List<Memory>,
    val oneTurnMemories: List<Memory>,
    val persistentMemoryPrompt: String,
    val retrievedMemoryPrompt: String,
    val oneTurnMemoryPrompt: String
) {
    val persistentMemoryCount: Int
        get() = persistentMemories.size

    val retrievedMemoryCount: Int
        get() = retrievedMemories.size

    val oneTurnMemoryCount: Int
        get() = oneTurnMemories.size
}

private object DurableMemoryHealth {
    fun from(
        allMemories: List<Memory>,
        pinnedMemories: List<Memory>,
        candidateMemories: List<Memory>
    ): MemoryHealthMetrics {
        return MemoryHealthMetrics(
            total = allMemories.size,
            pinned = pinnedMemories.size,
            candidates = candidateMemories.size,
            longTerm = allMemories.count { it.layer == LONG_TERM_LAYER },
            shortTerm = allMemories.count { it.layer == SHORT_TERM_LAYER }
        )
    }

    private const val LONG_TERM_LAYER = "long_term"
    private const val SHORT_TERM_LAYER = "short_term"
}
