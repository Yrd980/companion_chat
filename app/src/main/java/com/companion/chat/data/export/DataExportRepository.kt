package com.companion.chat.data.export

import android.content.Context
import androidx.room.withTransaction
import com.companion.chat.data.local.CompanionDatabase
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

enum class LocalDataDeleteScope {
    MEMORIES,
    CONVERSATIONS,
    ROLE_CARDS,
    ALL_LOCAL_USER_DATA
}

class DataExportRepository(
    private val context: Context,
    private val database: CompanionDatabase
) {

    suspend fun exportAll(): String {
        val exportDirectory = File(context.filesDir, "exports").apply { mkdirs() }
        val exportFile = File(exportDirectory, "companion-export-${System.currentTimeMillis()}.json")
        val payload = JSONObject()
            .put("createdAt", System.currentTimeMillis())
            .put("conversations", conversationsJson())
            .put("memories", memoriesJson())
            .put("roleCards", roleCardsJson())
            .put("preferences", preferencesJson())
            .put("timelineEvents", timelineEventsJson())

        exportFile.writeText(payload.toString(2))
        return exportFile.absolutePath
    }

    suspend fun deleteLocalData(scope: LocalDataDeleteScope): Int {
        return database.withTransaction {
            when (scope) {
                LocalDataDeleteScope.MEMORIES -> {
                    database.preferenceDao().deleteAll()
                    database.memoryDao().deleteAll()
                }
                LocalDataDeleteScope.CONVERSATIONS -> {
                    database.messageDao().deleteAllMessages()
                    database.conversationDao().deleteAllConversations()
                }
                LocalDataDeleteScope.ROLE_CARDS -> {
                    database.roleCardDao().deleteUserRoleCards()
                }
                LocalDataDeleteScope.ALL_LOCAL_USER_DATA -> {
                    val preferences = database.preferenceDao().deleteAll()
                    val memories = database.memoryDao().deleteAll()
                    val messages = database.messageDao().deleteAllMessages()
                    val conversations = database.conversationDao().deleteAllConversations()
                    val roleCards = database.roleCardDao().deleteUserRoleCards()
                    database.timelineEventDao().deleteAll()
                    preferences + memories + messages + conversations + roleCards
                }
            }
        }
    }

    private suspend fun conversationsJson(): JSONArray {
        val conversations = database.conversationDao().getAllConversationsWithMessages()
        return JSONArray().apply {
            conversations.forEach { item ->
                put(
                    JSONObject()
                        .put("id", item.conversation.id)
                        .put("title", item.conversation.title)
                        .put("createdAt", item.conversation.createdAt)
                        .put("updatedAt", item.conversation.updatedAt)
                        .put(
                            "messages",
                            JSONArray().apply {
                                item.messages.sortedBy { it.position }.forEach { message ->
                                    put(
                                        JSONObject()
                                            .put("id", message.id)
                                            .put("role", message.role.name)
                                            .put("content", message.content)
                                            .put("images", message.imageUris.toJsonArray())
                                            .put("timestamp", message.timestamp)
                                            .put("position", message.position)
                                    )
                                }
                            }
                        )
                )
            }
        }
    }

    private suspend fun memoriesJson(): JSONArray {
        return JSONArray().apply {
            database.memoryDao().getAll().forEach { memory ->
                put(
                    JSONObject()
                        .put("id", memory.id)
                        .put("content", memory.content)
                        .put("category", memory.category)
                        .put("layer", memory.layer)
                        .put("source", memory.source)
                        .put("referenceCount", memory.referenceCount)
                        .put("sessionId", memory.sessionId)
                        .put("createdAt", memory.createdAt)
                        .put("updatedAt", memory.updatedAt)
                        .put("expiresAt", memory.expiresAt)
                        .put("isPinned", memory.isPinned)
                        .put("reviewState", memory.reviewState)
                        .put("lastUsedAt", memory.lastUsedAt)
                )
            }
        }
    }

    private suspend fun roleCardsJson(): JSONArray {
        return JSONArray().apply {
            database.roleCardDao().getAll().forEach { role ->
                put(
                    JSONObject()
                        .put("id", role.id)
                        .put("name", role.name)
                        .put("description", role.description)
                        .put("avatar", role.avatar)
                        .put("persona", role.persona)
                        .put("speakingStyle", role.speakingStyle)
                        .put("background", role.background)
                        .put("rules", role.rules)
                        .put("taboos", role.taboos)
                        .put("openingMessage", role.openingMessage)
                        .put("exampleDialogue", role.exampleDialogue)
                        .put("avatarImageUri", role.avatarImageUri)
                        .put("galleryImageUris", role.galleryImageUris.toJsonArray())
                        .put("imageStylePrompt", role.imageStylePrompt)
                        .put("voiceProfileUri", role.voiceProfileUri)
                        .put("voiceMode", role.voiceMode)
                        .put("voiceDisplayName", role.voiceDisplayName)
                        .put("isBuiltIn", role.isBuiltIn)
                        .put("isActive", role.isActive)
                        .put("createdAt", role.createdAt)
                        .put("updatedAt", role.updatedAt)
                )
            }
        }
    }

    private suspend fun preferencesJson(): JSONArray {
        return JSONArray().apply {
            database.preferenceDao().getAll().forEach { preference ->
                put(
                    JSONObject()
                        .put("id", preference.id)
                        .put("category", preference.category)
                        .put("content", preference.content)
                        .put("confidence", preference.confidence)
                        .put("createdAt", preference.createdAt)
                        .put("updatedAt", preference.updatedAt)
                )
            }
        }
    }

    private suspend fun timelineEventsJson(): JSONArray {
        return JSONArray().apply {
            database.timelineEventDao().getRecent(limit = Int.MAX_VALUE).forEach { event ->
                put(
                    JSONObject()
                        .put("id", event.id)
                        .put("type", event.type.name)
                        .put("title", event.title)
                        .put("detail", event.detail)
                        .put("relatedSessionId", event.relatedSessionId)
                        .put("relatedMemoryId", event.relatedMemoryId)
                        .put("mediaUri", event.mediaUri)
                        .put("createdAt", event.createdAt)
                )
            }
        }
    }
}

private fun List<String>.toJsonArray(): JSONArray {
    return JSONArray().also { array ->
        forEach { array.put(it) }
    }
}
