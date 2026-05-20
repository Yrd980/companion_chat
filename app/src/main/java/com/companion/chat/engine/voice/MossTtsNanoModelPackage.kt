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
            MossTtsNanoRunnerPlan.fromDirectory(directory)
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

data class MossTtsNanoRunnerPlan(
    val sessionOrder: List<MossTtsNanoSessionSpec>,
    val ttsDecodeInputNames: List<String>,
    val ttsDecodeOutputNames: List<String>,
    val localCachedInputNames: List<String>,
    val localCachedOutputNames: List<String>,
    val sampleMode: String,
    val frameChannels: Int,
    val audioPadTokenId: Int,
    val audioStartTokenId: Int,
    val audioEndTokenId: Int,
    val audioUserSlotTokenId: Int,
    val audioAssistantSlotTokenId: Int,
    val userPromptPrefixTokenIds: List<Int>,
    val userPromptAfterReferenceTokenIds: List<Int>,
    val assistantPromptPrefixTokenIds: List<Int>,
    val builtinVoices: List<MossTtsNanoBuiltinVoice>,
    val textSamples: List<MossTtsNanoTextSample>,
    val codecSampleRate: Int,
    val codecChannels: Int,
    val codecDownsampleRate: Int
) {
    companion object {
        fun fromDirectory(directory: File): MossTtsNanoRunnerPlan {
            val manifest = JSONObject(File(directory, "tts/browser_poc_manifest.json").readText())
            val ttsMeta = JSONObject(File(directory, MossTtsNanoModelPackage.TTS_META_FILE_NAME).readText())
            val codecMeta = JSONObject(File(directory, MossTtsNanoModelPackage.CODEC_META_FILE_NAME).readText())
            val ttsFiles = ttsMeta.getJSONObject("files")
            val codecFiles = codecMeta.getJSONObject("files")
            val onnx = ttsMeta.optJSONObject("onnx") ?: JSONObject()
            val ttsConfig = manifest.optJSONObject("tts_config") ?: JSONObject()
            val generationDefaults = manifest.optJSONObject("generation_defaults") ?: JSONObject()
            val promptTemplates = manifest.optJSONObject("prompt_templates") ?: JSONObject()
            val codecConfig = codecMeta.getJSONObject("codec_config")
            val sessionOrder = buildList {
                add(MossTtsNanoSessionSpec("prefill", "tts/${ttsFiles.getString("prefill")}"))
                add(MossTtsNanoSessionSpec("decode", "tts/${ttsFiles.getString("decode_step")}"))
                add(MossTtsNanoSessionSpec("codecDecode", "audio_tokenizer/${codecFiles.getString("decode_full")}"))
                add(MossTtsNanoSessionSpec("codecEncode", "audio_tokenizer/${codecFiles.getString("encode")}"))
                ttsFiles.optString("local_greedy_frame").takeIf { it.isNotBlank() }
                    ?.let { add(MossTtsNanoSessionSpec("localGreedyFrame", "tts/$it")) }
                ttsFiles.optString("local_fixed_sampled_frame").takeIf { it.isNotBlank() }
                    ?.let { add(MossTtsNanoSessionSpec("localFixedSampledFrame", "tts/$it")) }
                ttsFiles.optString("local_cached_step").takeIf { it.isNotBlank() }
                    ?.let { add(MossTtsNanoSessionSpec("localCachedStep", "tts/$it")) }
                    ?: add(MossTtsNanoSessionSpec("localDecoder", "tts/${ttsFiles.getString("local_decoder")}"))
                codecFiles.optString("decode_step").takeIf { it.isNotBlank() }
                    ?.let { add(MossTtsNanoSessionSpec("codecDecodeStep", "audio_tokenizer/$it")) }
            }
            val frameChannels = ttsConfig.optInt("n_vq", 0)
            require(frameChannels > 0) { "browser_poc_manifest.json 缺少有效 tts_config.n_vq" }
            val sampleRate = codecConfig.optInt("sample_rate", MossTtsNanoConfig.DEFAULT_SAMPLE_RATE)
            val channels = codecConfig.optInt("channels", 2)
            return MossTtsNanoRunnerPlan(
                sessionOrder = sessionOrder,
                ttsDecodeInputNames = onnx.optStringArray("decode_input_names"),
                ttsDecodeOutputNames = onnx.optStringArray("decode_output_names"),
                localCachedInputNames = onnx.optStringArray("local_cached_input_names"),
                localCachedOutputNames = onnx.optStringArray("local_cached_output_names"),
                sampleMode = generationDefaults.optString("sample_mode", "greedy").ifBlank { "greedy" },
                frameChannels = frameChannels,
                audioPadTokenId = ttsConfig.optInt("audio_pad_token_id", 1024),
                audioStartTokenId = ttsConfig.optInt("audio_start_token_id", 6),
                audioEndTokenId = ttsConfig.optInt("audio_end_token_id", 7),
                audioUserSlotTokenId = ttsConfig.optInt("audio_user_slot_token_id", 8),
                audioAssistantSlotTokenId = ttsConfig.optInt("audio_assistant_slot_token_id", 9),
                userPromptPrefixTokenIds = promptTemplates.optIntArray("user_prompt_prefix_token_ids"),
                userPromptAfterReferenceTokenIds = promptTemplates.optIntArray("user_prompt_after_reference_token_ids"),
                assistantPromptPrefixTokenIds = promptTemplates.optIntArray("assistant_prompt_prefix_token_ids"),
                builtinVoices = manifest.optBuiltinVoices(),
                textSamples = manifest.optTextSamples(),
                codecSampleRate = sampleRate,
                codecChannels = channels,
                codecDownsampleRate = codecConfig.optInt("downsample_rate", 0)
            )
        }

        private fun JSONObject.optStringArray(name: String): List<String> {
            val array = optJSONArray(name) ?: return emptyList()
            return List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
        }

        private fun JSONObject.optIntArray(name: String): List<Int> {
            val array = optJSONArray(name) ?: return emptyList()
            return List(array.length()) { index -> array.optInt(index) }
        }

        private fun JSONObject.optBuiltinVoices(): List<MossTtsNanoBuiltinVoice> {
            val array = optJSONArray("builtin_voices") ?: return emptyList()
            return List(array.length()) { index ->
                val voice = array.getJSONObject(index)
                MossTtsNanoBuiltinVoice(
                    voice = voice.optString("voice"),
                    displayName = voice.optString("display_name"),
                    promptAudioCodes = voice.optIntRows("prompt_audio_codes")
                )
            }.filter { it.promptAudioCodes.isNotEmpty() }
        }

        private fun JSONObject.optTextSamples(): List<MossTtsNanoTextSample> {
            val array = optJSONArray("text_samples") ?: return emptyList()
            return List(array.length()) { index ->
                val sample = array.getJSONObject(index)
                MossTtsNanoTextSample(
                    text = sample.optString("text"),
                    textTokenIds = sample.optIntArray("text_token_ids")
                )
            }.filter { it.text.isNotBlank() && it.textTokenIds.isNotEmpty() }
        }

        private fun JSONObject.optIntRows(name: String): List<IntArray> {
            val rows = optJSONArray(name) ?: return emptyList()
            return List(rows.length()) { rowIndex ->
                val row = rows.getJSONArray(rowIndex)
                IntArray(row.length()) { columnIndex -> row.optInt(columnIndex) }
            }
        }
    }
}

data class MossTtsNanoSessionSpec(
    val name: String,
    val relativePath: String
)

data class MossTtsNanoBuiltinVoice(
    val voice: String,
    val displayName: String,
    val promptAudioCodes: List<IntArray>
)

data class MossTtsNanoTextSample(
    val text: String,
    val textTokenIds: List<Int>
)

sealed class MossTtsNanoModelStatus {
    data object Ready : MossTtsNanoModelStatus()
    data object DirectoryNotConfigured : MossTtsNanoModelStatus()
    data class MissingFiles(val fileNames: List<String>) : MossTtsNanoModelStatus()
    data class InvalidConfig(val message: String) : MossTtsNanoModelStatus()
}
