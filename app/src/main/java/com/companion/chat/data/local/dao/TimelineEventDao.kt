package com.companion.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.companion.chat.data.timeline.TimelineEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: TimelineEvent)

    @Query("SELECT * FROM timeline_events ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TimelineEvent>>

    @Query("SELECT * FROM timeline_events ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TimelineEvent>

    @Query("DELETE FROM timeline_events")
    suspend fun deleteAll()
}
