package com.companion.chat.data.timeline

import com.companion.chat.data.local.dao.TimelineEventDao
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class TimelineEventRepository(
    private val timelineEventDao: TimelineEventDao,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    fun observeRecent(limit: Int = 20): Flow<List<TimelineEvent>> {
        return timelineEventDao.observeRecent(limit)
    }

    suspend fun getRecent(limit: Int = 20): List<TimelineEvent> {
        return timelineEventDao.getRecent(limit)
    }

    suspend fun add(
        type: TimelineEventType,
        title: String,
        detail: String,
        relatedSessionId: String? = null,
        relatedMemoryId: Long? = null,
        mediaUri: String? = null
    ): TimelineEvent {
        val event = TimelineEvent(
            id = idProvider(),
            type = type,
            title = title.trim(),
            detail = detail.trim(),
            relatedSessionId = relatedSessionId?.trim()?.ifBlank { null },
            relatedMemoryId = relatedMemoryId,
            mediaUri = mediaUri?.trim()?.ifBlank { null },
            createdAt = nowProvider()
        )
        timelineEventDao.upsert(event)
        return event
    }

    suspend fun clear() {
        timelineEventDao.deleteAll()
    }
}
