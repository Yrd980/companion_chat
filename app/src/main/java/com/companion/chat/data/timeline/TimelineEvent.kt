package com.companion.chat.data.timeline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timeline_events")
data class TimelineEvent(
    @PrimaryKey val id: String,
    val type: TimelineEventType,
    val title: String,
    val detail: String,
    val relatedSessionId: String? = null,
    val relatedMemoryId: Long? = null,
    val mediaUri: String? = null,
    val createdAt: Long
)

enum class TimelineEventType {
    CHAT,
    MEMORY_CREATED,
    MEMORY_PINNED,
    VOICE_NOTE,
    IMAGE_GENERATED,
    PRIVACY_CHANGED,
    SETUP_CHANGED,
    DATA_EXPORTED,
    LOCAL_DATA_DELETED
}
