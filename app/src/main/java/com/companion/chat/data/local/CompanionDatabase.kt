package com.companion.chat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.companion.chat.data.local.dao.ConversationDao
import com.companion.chat.data.local.dao.MemoryDao
import com.companion.chat.data.local.dao.MessageDao
import com.companion.chat.data.local.dao.PreferenceDao
import com.companion.chat.data.local.dao.SkillDao
import com.companion.chat.data.local.entity.ConversationEntity
import com.companion.chat.data.local.entity.Memory
import com.companion.chat.data.local.entity.MessageEntity
import com.companion.chat.data.local.entity.Skill
import com.companion.chat.data.local.entity.UserPreference
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        Memory::class,
        UserPreference::class,
        Skill::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CompanionDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun skillDao(): SkillDao

    companion object {
        private const val DATABASE_NAME = "companion_chat.db"

        @Volatile
        private var instance: CompanionDatabase? = null

        fun getInstance(context: Context): CompanionDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CompanionDatabase::class.java,
                    DATABASE_NAME
                ).addCallback(DatabaseInitializationCallback()).build().also { instance = it }
            }
        }

        private class DatabaseInitializationCallback : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                createMemoryFtsTables(db)
                seedBuiltInSkills(db)
            }

            private fun createMemoryFtsTables(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS memories_fts USING fts4(
                        content,
                        category
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS memories_ai AFTER INSERT ON memories BEGIN
                        INSERT INTO memories_fts(docid, content, category)
                        VALUES (new.id, new.content, new.category);
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS memories_ad AFTER DELETE ON memories BEGIN
                        DELETE FROM memories_fts WHERE docid = old.id;
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS memories_au AFTER UPDATE ON memories BEGIN
                        DELETE FROM memories_fts WHERE docid = old.id;
                        INSERT INTO memories_fts(docid, content, category)
                        VALUES (new.id, new.content, new.category);
                    END
                    """.trimIndent()
                )
            }

            private fun seedBuiltInSkills(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                builtInSkills.forEachIndexed { index, skill ->
                    db.execSQL(
                        """
                        INSERT INTO skills(
                            id, name, description, systemPrompt, icon,
                            isBuiltIn, isActive, usageCount, createdAt, updatedAt
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf<Any>(
                            index + 1L,
                            skill.name,
                            skill.description,
                            skill.systemPrompt,
                            skill.icon,
                            1,
                            if (index == 0) 1 else 0,
                            0,
                            now,
                            now
                        )
                    )
                }
            }
        }

        private data class BuiltInSkillSeed(
            val name: String,
            val description: String,
            val systemPrompt: String,
            val icon: String
        )

        private val builtInSkills = listOf(
            BuiltInSkillSeed(
                name = "通用助手",
                description = "你的默认 AI 伙伴",
                systemPrompt = "你是一个友善的 AI 助手，请用中文回答用户的问题。",
                icon = "assistant"
            ),
            BuiltInSkillSeed(
                name = "翻译助手",
                description = "准确翻译并保持原意",
                systemPrompt = "你是一个专业的翻译助手。用户会给你需要翻译的内容，请准确翻译并保持原意。",
                icon = "translate"
            ),
            BuiltInSkillSeed(
                name = "代码助手",
                description = "擅长代码审查和问题排查",
                systemPrompt = "你是一个编程助手，擅长代码审查和问题排查。回答时给出清晰的代码示例。",
                icon = "code"
            ),
            BuiltInSkillSeed(
                name = "写作助手",
                description = "帮助润色文字和生成创意内容",
                systemPrompt = "你是一个写作助手，帮助用户润色文字、生成创意内容。",
                icon = "writing"
            )
        )
    }
}
