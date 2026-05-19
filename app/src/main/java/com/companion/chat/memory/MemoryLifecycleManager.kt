package com.companion.chat.memory

import com.companion.chat.data.memory.MemoryRepository

class MemoryLifecycleManager(
    private val memoryRepository: MemoryRepository,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun runStartupMaintenance() {
        val now = nowProvider()
        memoryRepository.cleanupExpiredShortTerm(now)
        memoryRepository.promoteEligibleShortTerm(now)
    }
}
