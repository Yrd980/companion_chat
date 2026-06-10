package com.companion.chat.ui.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.companion.chat.appContainer
import com.companion.chat.data.local.CompanionDatabase
import com.companion.chat.data.local.entity.Memory
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
    private val memoryRepository: MemoryRepository = MemoryRepository(
        memoryDao = CompanionDatabase.getInstance(application).memoryDao()
    ),
    private val workerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        memoryRepository = defaultMemoryRepository(application),
        workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    private var allMemories: List<Memory> = emptyList()
    private var candidateMemories: List<Memory> = emptyList()
    private var pinnedMemories: List<Memory> = emptyList()

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
            memoryRepository.addManualMemory(content, category)
            refreshMemories()
        }
    }

    fun updateMemory(memoryId: Long, content: String, category: String) {
        val existing = allMemories.firstOrNull { it.id == memoryId } ?: return
        workerScope.launch {
            memoryRepository.updateMemory(
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
            memoryRepository.deleteMemory(memory)
            refreshMemories()
        }
    }

    fun keepCandidate(memoryId: Long) {
        workerScope.launch {
            memoryRepository.confirmCandidate(memoryId)
            refreshMemories(message = "Memory kept")
        }
    }

    fun deleteCandidate(memory: Memory) {
        workerScope.launch {
            memoryRepository.deleteCandidate(memory)
            refreshMemories(message = "Candidate deleted")
        }
    }

    fun pinMemory(memoryId: Long) {
        workerScope.launch {
            memoryRepository.pinMemory(memoryId)
            refreshMemories(message = "Memory pinned")
        }
    }

    fun unpinMemory(memoryId: Long) {
        workerScope.launch {
            memoryRepository.unpinMemory(memoryId)
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
            memoryRepository.promoteMemory(memoryId)
            refreshMemories()
        }
    }

    private fun observeMemories() {
        workerScope.launch {
            memoryRepository.observeAllMemories().collectLatest { memories ->
                allMemories = memories
                candidateMemories = memoryRepository.getCandidateMemories()
                pinnedMemories = memoryRepository.getPinnedMemories()
                publishMemories(isLoading = false)
            }
        }
    }

    private suspend fun refreshMemories(message: String = _uiState.value.message) {
        allMemories = memoryRepository.getAllMemories()
        candidateMemories = memoryRepository.getCandidateMemories()
        pinnedMemories = memoryRepository.getPinnedMemories()
        publishMemories(isLoading = false, message = message)
    }

    private fun publishMemories(
        isLoading: Boolean = _uiState.value.isLoading,
        message: String = _uiState.value.message
    ) {
        val filter = _uiState.value.filter
        val confirmedMemories = allMemories.filter { it.reviewState != MemoryRepository.REVIEW_STATE_CANDIDATE }
        val visibleMemories = when (filter) {
            MemoryFilter.ALL -> confirmedMemories
            MemoryFilter.RELATION -> confirmedMemories.filter {
                it.category == "relation" || it.category == "relationship"
            }
            else -> confirmedMemories.filter { it.category == filter.category }
        }
        _uiState.update {
            it.copy(
                memories = visibleMemories,
                candidateMemories = candidateMemories,
                pinnedMemories = pinnedMemories,
                healthMetrics = memoryRepositoryHealthSnapshot(),
                message = message,
                isLoading = isLoading
            )
        }
    }

    private fun memoryRepositoryHealthSnapshot(): com.companion.chat.data.memory.MemoryHealthMetrics {
        return com.companion.chat.data.memory.MemoryHealthMetrics(
            total = allMemories.size,
            pinned = pinnedMemories.size,
            candidates = candidateMemories.size,
            longTerm = allMemories.count { it.layer == "long_term" },
            shortTerm = allMemories.count { it.layer == "short_term" }
        )
    }
}

private fun defaultMemoryRepository(application: Application): MemoryRepository {
    return runCatching { application.appContainer.memoryRepository }.getOrElse {
        MemoryRepository(
            memoryDao = CompanionDatabase.getInstance(application).memoryDao()
        )
    }
}
