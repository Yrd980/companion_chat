package com.companion.chat.data.image

import android.content.Context
import android.content.SharedPreferences

class ImageGenerationConfigRepository(
    private val sharedPreferences: SharedPreferences,
    private val defaultDreamLiteModelDirectoryProvider: () -> String = { "" }
) {
    constructor(context: Context) : this(
        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        defaultDreamLiteModelDirectoryProvider = {
            context.applicationContext.getExternalFilesDir(DreamLiteModelPackage.DEFAULT_MODEL_RELATIVE_DIRECTORY)
                ?.absolutePath
                .orEmpty()
        }
    )

    fun getConfig(): ImageGenerationConfig {
        return ImageGenerationConfig(
            baseUrl = sharedPreferences.getString(KEY_BASE_URL, "").orEmpty(),
            apiKey = sharedPreferences.getString(KEY_API_KEY, "").orEmpty(),
            model = sharedPreferences.getString(KEY_MODEL, "").orEmpty(),
            provider = runCatching {
                ImageGenerationProvider.valueOf(
                    sharedPreferences.getString(KEY_PROVIDER, ImageGenerationProvider.HTTP.name).orEmpty()
                )
            }.getOrDefault(ImageGenerationProvider.HTTP),
            localModelPath = sharedPreferences.getString(KEY_LOCAL_MODEL_PATH, null)
                ?.trim()
                .orEmpty()
                .ifBlank { defaultDreamLiteModelDirectoryProvider().trim() },
            requestTemplate = sharedPreferences.getString(
                KEY_REQUEST_TEMPLATE,
                ImageGenerationConfig.DEFAULT_REQUEST_TEMPLATE
            ).orEmpty().ifBlank { ImageGenerationConfig.DEFAULT_REQUEST_TEMPLATE },
            responseImageFieldPath = sharedPreferences.getString(
                KEY_RESPONSE_FIELD_PATH,
                ImageGenerationConfig.DEFAULT_RESPONSE_FIELD_PATH
            ).orEmpty().ifBlank { ImageGenerationConfig.DEFAULT_RESPONSE_FIELD_PATH },
            timeoutMillis = sharedPreferences.getInt(KEY_TIMEOUT_MILLIS, 60_000)
                .coerceIn(5_000, 180_000)
        )
    }

    fun updateConfig(config: ImageGenerationConfig) {
        sharedPreferences.edit()
            .putString(KEY_BASE_URL, config.baseUrl.trim())
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_MODEL, config.model.trim())
            .putString(KEY_PROVIDER, config.provider.name)
            .putString(KEY_LOCAL_MODEL_PATH, config.localModelPath.trim())
            .putString(KEY_REQUEST_TEMPLATE, config.requestTemplate.trim())
            .putString(KEY_RESPONSE_FIELD_PATH, config.responseImageFieldPath.trim())
            .putInt(KEY_TIMEOUT_MILLIS, config.timeoutMillis.coerceIn(5_000, 180_000))
            .apply()
    }

    fun getDreamLiteModelStatus(config: ImageGenerationConfig = getConfig()): DreamLiteModelStatus {
        return DreamLiteModelPackage.inspect(config.localModelPath)
    }

    private companion object {
        const val PREFS_NAME = "image_generation_config"
        const val KEY_BASE_URL = "base_url"
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
        const val KEY_PROVIDER = "provider"
        const val KEY_LOCAL_MODEL_PATH = "local_model_path"
        const val KEY_REQUEST_TEMPLATE = "request_template"
        const val KEY_RESPONSE_FIELD_PATH = "response_image_field_path"
        const val KEY_TIMEOUT_MILLIS = "timeout_millis"
    }
}
