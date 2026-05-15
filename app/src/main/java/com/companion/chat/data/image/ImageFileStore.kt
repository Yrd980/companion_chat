package com.companion.chat.data.image

import android.content.Context
import java.io.File

class ImageFileStore(
    private val context: Context
) {
    fun saveBytes(bytes: ByteArray, purpose: ImageGenerationPurpose): String {
        val dir = File(context.filesDir, "generated_images/${purpose.name.lowercase()}").apply {
            mkdirs()
        }
        val file = File(dir, "image_${System.currentTimeMillis()}.png")
        file.writeBytes(bytes)
        return file.toURI().toString()
    }
}
