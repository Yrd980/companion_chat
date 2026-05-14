package com.companion.chat

import android.app.Application
import android.content.Context
import com.companion.chat.data.local.CompanionDatabase
import com.companion.chat.data.memory.MemoryLifecycleManager
import com.companion.chat.data.memory.MemoryRepository
import com.companion.chat.data.repository.ChatSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CompanionChatApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        logToFile("Application.onCreate")

        val sessionRepository = ChatSessionRepository(this)
        val memoryRepository = MemoryRepository(
            memoryDao = CompanionDatabase.getInstance(this).memoryDao()
        )
        val memoryLifecycleManager = MemoryLifecycleManager(memoryRepository)
        applicationScope.launch {
            runCatching {
                logToFile("开始 ensureInitialized")
                sessionRepository.ensureInitialized()
                logToFile("ensureInitialized 完成")
                memoryLifecycleManager.runStartupMaintenance()
                logToFile("记忆生命周期维护完成")
            }.onFailure {
                logToFile("ensureInitialized 失败: ${it.javaClass.simpleName}: ${it.message}")
            }
        }
    }

    private fun logToFile(message: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        openFileOutput("app_init_log.txt", Context.MODE_APPEND).use { output ->
            output.write("[$time] $message\n".toByteArray())
        }
    }
}
