package com.companion.chat.engine.voice

import org.json.JSONObject
import java.io.File

object MossTtsNanoModelPackage {
    const val DEFAULT_MODEL_RELATIVE_DIRECTORY = "models/tts/moss-tts-nano"
    const val TTS_META_FILE_NAME = "tts/tts_browser_onnx_meta.json"
    const val CODEC_META_FILE_NAME = "audio_tokenizer/codec_browser_onnx_meta.json"

    val REQUIRED_MODEL_FILES = listOf(
        TTS_META_FILE_NAME,
        CODEC_META_FILE_NAME,
        "tts/browser_poc_manifest.json",
        "tts/tokenizer.model",
        "tts/moss_tts_prefill.onnx",
        "tts/moss_tts_decode_step.onnx",
        "tts/moss_tts_local_decoder.onnx",
        "tts/moss_tts_local_cached_step.onnx",
        "tts/moss_tts_local_fixed_sampled_frame.onnx",
        "tts/moss_tts_global_shared.data",
        "tts/moss_tts_local_shared.data",
        "audio_tokenizer/moss_audio_tokenizer_encode.onnx",
        "audio_tokenizer/moss_audio_tokenizer_encode.data",
        "audio_tokenizer/moss_audio_tokenizer_decode_full.onnx",
        "audio_tokenizer/moss_audio_tokenizer_decode_step.onnx",
        "audio_tokenizer/moss_audio_tokenizer_decode_shared.data"
    )

    fun inspect(modelDirectory: String): MossTtsNanoModelStatus {
        val directoryPath = modelDirectory.trim()
        if (directoryPath.isBlank()) return MossTtsNanoModelStatus.DirectoryNotConfigured

        val directory = File(directoryPath)
        if (!directory.isDirectory) {
            return MossTtsNanoModelStatus.MissingFiles(REQUIRED_MODEL_FILES)
        }

        val missingFiles = REQUIRED_MODEL_FILES.filterNot { File(directory, it).isFile }
        if (missingFiles.isNotEmpty()) {
            return MossTtsNanoModelStatus.MissingFiles(missingFiles)
        }

        return runCatching {
            MossTtsNanoConfig.fromDirectory(directory)
        }.fold(
            onSuccess = { MossTtsNanoModelStatus.Ready },
            onFailure = { MossTtsNanoModelStatus.InvalidConfig(it.message ?: "配置 JSON 解析失败") }
        )
    }
}

data class MossTtsNanoConfig(
    val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    val channels: Int = 2,
    val ttsPrefillModelPath: String = "tts/moss_tts_prefill.onnx",
    val ttsDecodeStepModelPath: String = "tts/moss_tts_decode_step.onnx",
    val ttsLocalDecoderModelPath: String = "tts/moss_tts_local_decoder.onnx",
    val ttsLocalCachedStepModelPath: String = "tts/moss_tts_local_cached_step.onnx",
    val ttsLocalFixedSampledFrameModelPath: String = "tts/moss_tts_local_fixed_sampled_frame.onnx",
    val audioTokenizerEncodeModelPath: String = "audio_tokenizer/moss_audio_tokenizer_encode.onnx",
    val audioTokenizerDecodeFullModelPath: String = "audio_tokenizer/moss_audio_tokenizer_decode_full.onnx",
    val audioTokenizerDecodeStepModelPath: String = "audio_tokenizer/moss_audio_tokenizer_decode_step.onnx",
    val tokenizerModelPath: String = "tts/tokenizer.model"
) {
    companion object {
        const val DEFAULT_SAMPLE_RATE = 48_000

        fun fromDirectory(directory: File): MossTtsNanoConfig {
            val ttsMeta = JSONObject(File(directory, MossTtsNanoModelPackage.TTS_META_FILE_NAME).readText())
            val codecMeta = JSONObject(File(directory, MossTtsNanoModelPackage.CODEC_META_FILE_NAME).readText())
            val codecConfig = codecMeta.getJSONObject("codec_config")
            val sampleRate = codecConfig.optInt("sample_rate", DEFAULT_SAMPLE_RATE)
            val channels = codecConfig.optInt("channels", 2)
            require(sampleRate in 8_000..96_000) { "sample_rate 必须在 8000 到 96000 之间" }
            require(channels in 1..2) { "channels 只支持 1 或 2" }
            val ttsFiles = ttsMeta.getJSONObject("files")
            val codecFiles = codecMeta.getJSONObject("files")
            return MossTtsNanoConfig(
                sampleRate = sampleRate,
                channels = channels,
                ttsPrefillModelPath = "tts/${ttsFiles.getString("prefill")}",
                ttsDecodeStepModelPath = "tts/${ttsFiles.getString("decode_step")}",
                ttsLocalDecoderModelPath = "tts/${ttsFiles.getString("local_decoder")}",
                ttsLocalCachedStepModelPath = "tts/${ttsFiles.getString("local_cached_step")}",
                ttsLocalFixedSampledFrameModelPath = "tts/${ttsFiles.getString("local_fixed_sampled_frame")}",
                audioTokenizerEncodeModelPath = "audio_tokenizer/${codecFiles.getString("encode")}",
                audioTokenizerDecodeFullModelPath = "audio_tokenizer/${codecFiles.getString("decode_full")}",
                audioTokenizerDecodeStepModelPath = "audio_tokenizer/${codecFiles.getString("decode_step")}"
            )
        }
    }
}

sealed class MossTtsNanoModelStatus {
    data object Ready : MossTtsNanoModelStatus()
    data object DirectoryNotConfigured : MossTtsNanoModelStatus()
    data class MissingFiles(val fileNames: List<String>) : MossTtsNanoModelStatus()
    data class InvalidConfig(val message: String) : MossTtsNanoModelStatus()
}
