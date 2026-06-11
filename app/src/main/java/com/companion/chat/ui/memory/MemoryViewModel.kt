package com.companion.chat.ui.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.companion.chat.appContainer
import com.companion.chat.data.local.CompanionDatabase
import com.companion.chat.data.local.entity.Memory
import com.companion.chat.data.memory.DurableMemoryModule
import com.companion.chat.data.memory.DurableMemoryReviewProjection
import com.companion.chat.data.memory.MemoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemoryViewModel(
    application: Application,
    private val durableMemoryModule: DurableMemoryModule = DurableMemoryModule(
        repository = MemoryRepository(
            memoryDao = CompanionDatabase.getInstance(application).memoryDao()
        )
    ),
    private val workerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        durableMemoryModule = defaultDurableMemoryModule(application),
        workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    private var memoryProjection = DurableMemoryReviewProjection(
        confirmedMemories = emptyList(),
        candidateMemories = emptyList(),
        pinnedMemories = emptyList(),
        healthMetrics = com.companion.chat.data.memory.MemoryHealthMetrics(
            total = 0,
            pinned = 0,
            candidates = 0,
            longTerm = 0,
            shortTerm = 0
        )
    )

    init {
        observeMemories()
    }

    fun loadMemories() {
        workerScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            refreshMemories()
        }
    }

    fun setFilter(filter: MemoryFilter) {
        _uiState.update { it.copy(filter = filter) }
        publishMemories()
    }

    fun addMemory(content: String, category: String) {
        if (content.isBlank()) {
            return
        }
        workerScope.launch {
            durableMemoryModule.addManualMemory(content, category)
            refreshMemories()
        }
    }

    fun updateMemory(memoryId: Long, content: String, category: String) {
        val existing = memoryProjection.confirmedMemories.firstOrNull { it.id == memoryId } ?: return
        workerScope.launch {
            durableMemoryModule.updateMemory(
                existing.copy(
                    content = content,
                    category = category
                )
            )
            refreshMemories()
        }
    }

    fun deleteMemory(memory: Memory) {
        workerScope.launch {
            durableMemoryModule.deleteMemory(memory)
            refreshMemories()
        }
    }

    fun keepCandidate(memoryId: Long) {
        workerScope.launch {
            durableMemoryModule.keepCandidate(memoryId)
            refreshMemories(message = "Memory kept")
        }
    }

    fun deleteCandidate(memory: Memory) {
        workerScope.launch {
            durableMemoryModule.deleteCandidate(memory)
            refreshMemories(message = "Candidate deleted")
        }
    }

    fun pinMemory(memoryId: Long) {
        workerScope.launch {
            durableMemoryModule.pinMemory(memoryId)
            refreshMemories(message = "Memory pinned")
        }
    }

    fun unpinMemory(memoryId: Long) {
        workerScope.launch {
            durableMemoryModule.unpinMemory(memoryId)
            refreshMemories(message = "Memory unpinned")
        }
    }

    fun useNextTurn(memoryId: Long) {
        _uiState.update {
            it.copy(
                selectedUseNextTurnMemoryId = memoryId,
                message = "Memory selected for next turn"
            )
        }
    }

    fun clearUseNextTurn() {
        _uiState.update {
            it.copy(
                selectedUseNextTurnMemoryId = null,
                message = ""
            )
        }
    }

    fun promoteMemory(memoryId: Long) {
        workerScope.launch {
            durableMemoryModule.promoteMemory(memoryId)
            refreshMemories()
        }
    }

    private fun observeMemories() {
        workerScope.launch {
            durableMemoryModule.observeReviewProjection().collectLatest { projection ->
                memoryProjection = projection
                publishMemories(isLoading = false)
            }
        }
    }

    private suspend fun refreshMemories(message: String = _uiState.value.message) {
        memoryProjection = durableMemoryModule.getReviewProjection()
        publishMemories(isLoading = false, message = message)
    }

    private fun publishMemories(
        isLoading: Boolean = _uiState.value.isLoading,
        message: String = _uiState.value.message
    ) {
        val filter = _uiState.value.filter
        _uiState.update {
            it.copy(
                memories = memoryProjection.confirmedForCategory(filter.category),
                candidateMemories = memoryProjection.candidateMemories,
                pinnedMemories = memoryProjection.pinnedMemories,
                healthMetrics = memoryProjection.healthMetrics,
                message = message,
                isLoading = isLoading
            )
        }
    }
}

private fun defaultDurableMemoryModule(application: Application): DurableMemoryModule {
    return runCatching { application.appContainer.durableMemoryModule }.getOrElse {
        DurableMemoryModule(
            repository = MemoryRepository(
                memoryDao = CompanionDatabase.getInstance(application).memoryDao()
            )
        )
    }
}
