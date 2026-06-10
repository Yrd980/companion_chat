package com.companion.chat.ui.memory

import com.companion.chat.data.local.entity.Memory
import com.companion.chat.data.memory.MemoryHealthMetrics

enum class MemoryFilter(val category: String?) {
    ALL(null),
    FACT("fact"),
    PREFERENCE("preference"),
    EVENT("event"),
    RELATION("relation"),
    TIME("time"),
    OTHER("other")
}

data class MemoryUiState(
    val memories: List<Memory> = emptyList(),
    val candidateMemories: List<Memory> = emptyList(),
    val pinnedMemories: List<Memory> = emptyList(),
    val healthMetrics: MemoryHealthMetrics = MemoryHealthMetrics(
        total = 0,
        pinned = 0,
        candidates = 0,
        longTerm = 0,
        shortTerm = 0
    ),
    val selectedUseNextTurnMemoryId: Long? = null,
    val message: String = "",
    val filter: MemoryFilter = MemoryFilter.ALL,
    val isLoading: Boolean = true
)
