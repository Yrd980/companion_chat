package com.companion.chat.data.memory

data class MemoryHealthMetrics(
    val total: Int,
    val pinned: Int,
    val candidates: Int,
    val longTerm: Int,
    val shortTerm: Int
)
