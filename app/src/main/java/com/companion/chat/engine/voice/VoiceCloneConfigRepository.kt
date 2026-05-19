package com.companion.chat.engine.voice

import android.content.Context
import android.content.SharedPreferences

class VoiceCloneConfigRepository(
    private val sharedPreferences: SharedPreferences,
    private val defaultMossModelDirectoryProvider: () -> String = { "" }
) {
    constructor(context: Context) : this(
        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        defaultMossModelDirectoryProvider = {
            context.applicationContext.getExternalFilesDir(MossTtsNanoModelPackage.DEFAULT_MODEL_RELATIVE_DIRECTORY)
                ?.absolutePath
                .orEmpty()
        }
    )

    fun getConfig(): VoiceCloneConfig {
        return VoiceCloneConfig(
            mossModelDirectory = sharedPreferences.getString(KEY_MOSS_MODEL_DIRECTORY, null)
                ?.trim()
                .orEmpty()
                .ifBlank { defaultMossModelDirectoryProvider().trim() }
        )
    }

    fun updateConfig(config: VoiceCloneConfig) {
        sharedPreferences.edit()
            .putString(KEY_MOSS_MODEL_DIRECTORY, config.mossModelDirectory.trim())
            .apply()
    }

    fun getMossModelStatus(config: VoiceCloneConfig = getConfig()): MossTtsNanoModelStatus {
        return MossTtsNanoModelPackage.inspect(config.mossModelDirectory)
    }

    companion object {
        const val PREFS_NAME = "voice_clone_config"
        private const val KEY_MOSS_MODEL_DIRECTORY = "moss_model_directory"
    }
}

data class VoiceCloneConfig(
    val mossModelDirectory: String = ""
)
