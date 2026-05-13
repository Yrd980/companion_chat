package com.companion.chat.ui.memory

import com.companion.chat.data.local.entity.Memory

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
    val filter: MemoryFilter = MemoryFilter.ALL,
    val isLoading: Boolean = true
)
