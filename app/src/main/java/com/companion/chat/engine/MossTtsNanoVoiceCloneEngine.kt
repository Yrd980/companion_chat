package com.companion.chat.engine

import android.content.Context
import android.net.Uri
import com.companion.chat.engine.voice.MossTtsNanoConfig
import com.companion.chat.engine.voice.MossTtsNanoModelPackage
import com.companion.chat.engine.voice.MossTtsNanoModelStatus
import com.companion.chat.engine.voice.MossTtsNanoRunnerPlan
import com.companion.chat.engine.voice.VoiceCloneEngine
import com.companion.chat.engine.voice.VoiceCloneProvider
import com.companion.chat.engine.voice.VoiceCloneRequest
import com.companion.chat.engine.voice.VoiceCloneResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class MossTtsNanoVoiceCloneEngine(
    private val context: Context,
    private val modelDirectoryProvider: () -> String,
    private val runner: MossTtsNanoRunner = UnsupportedMossTtsNanoRunner(),
    private val tokenizer: MossTtsNanoTokenizer = JavaScriptSandboxMossTtsNanoTokenizer(context)
) : VoiceCloneEngine {

    override suspend fun synthesize(request: VoiceCloneRequest): Result<VoiceCloneResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(request.text.isNotBlank()) { "朗读文本为空" }
            require(request.referenceAudioUri.isNotBlank()) { "角色参考音频 URI 未配置" }

            val modelDirectory = modelDirectoryProvider().trim()
            when (val status = MossTtsNanoModelPackage.inspect(modelDirectory)) {
                MossTtsNanoModelStatus.Ready -> Unit
                MossTtsNanoModelStatus.DirectoryNotConfigured -> error("moss-tts-nano 模型目录未配置")
                is MossTtsNanoModelStatus.InvalidConfig -> error("moss-tts-nano 配置无效：${status.message}")
                is MossTtsNanoModelStatus.MissingFiles -> error("moss-tts-nano 模型文件缺失：${status.fileNames.joinToString()}")
            }

            val directory = File(modelDirectory)
            val config = MossTtsNanoConfig.fromDirectory(directory)
            val plan = MossTtsNanoRunnerPlan.fromDirectory(directory)
            val referenceAudio = readReferenceAudio(request.referenceAudioUri)
            val textTokenIds = tokenizer.encode(
                modelFile = File(directory, config.tokenizerModelPath),
                text = request.text
            )
            val waveform = runOnnxPipeline(directory, config, plan, request.text, textTokenIds, referenceAudio)
            val outputFile = writeWaveFile(waveform, config.sampleRate)

            VoiceCloneResult(
                provider = VoiceCloneProvider.MOSS_TTS_NANO,
                audioUri = Uri.fromFile(outputFile).toString(),
                fallbackToSystemTts = false,
                message = "moss-tts-nano 本地合成完成"
            )
        }.recoverCatching { error ->
            VoiceCloneResult(
                provider = VoiceCloneProvider.MOSS_TTS_NANO,
                fallbackToSystemTts = true,
                message = error.message ?: "moss-tts-nano 合成失败"
            )
        }
    }

    private fun runOnnxPipeline(
        directory: File,
        config: MossTtsNanoConfig,
        plan: MossTtsNanoRunnerPlan,
        text: String,
        textTokenIds: IntArray,
        referenceAudio: FloatArray
    ): FloatArray {
        require(referenceAudio.isNotEmpty()) { "参考音频为空或格式不支持" }
        require(text.isNotBlank()) { "朗读文本为空" }
        val expectedFiles = listOf(
            config.ttsPrefillModelPath,
            config.ttsDecodeStepModelPath,
            config.ttsLocalDecoderModelPath,
            config.ttsLocalCachedStepModelPath,
            config.ttsLocalFixedSampledFrameModelPath,
            config.audioTokenizerEncodeModelPath,
            config.audioTokenizerDecodeFullModelPath,
            config.audioTokenizerDecodeStepModelPath,
            config.tokenizerModelPath
        )
        val missing = expectedFiles.filterNot { File(directory, it).isFile }
        require(missing.isEmpty()) { "moss-tts-nano 文件缺失：${missing.joinToString()}" }
        return runner.synthesize(
            MossTtsNanoRunnerRequest(
                modelDirectory = directory,
                config = config,
                plan = plan,
                text = text,
                textTokenIds = textTokenIds,
                referenceAudio = referenceAudio
            )
        )
    }

    private fun readReferenceAudio(uriString: String): FloatArray {
        val bytes = context.contentResolver.openInputStream(Uri.parse(uriString))
            ?.use { it.readBytes() }
            ?: error("无法读取参考音频")
        if (bytes.size <= WAV_HEADER_BYTES) return FloatArray(0)

        val dataOffset = findWavDataOffset(bytes).takeIf { it >= 0 } ?: WAV_HEADER_BYTES
        val buffer = ByteBuffer.wrap(bytes, dataOffset, bytes.size - dataOffset).order(ByteOrder.LITTLE_ENDIAN)
        val samples = FloatArray(buffer.remaining() / Short.SIZE_BYTES)
        for (index in samples.indices) {
            samples[index] = buffer.short / Short.MAX_VALUE.toFloat()
        }
        return samples
    }

    private fun findWavDataOffset(bytes: ByteArray): Int {
        val marker = byteArrayOf('d'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte())
        for (index in 12 until bytes.size - 8) {
            if (marker.indices.all { bytes[index + it] == marker[it] }) return index + 8
        }
        return -1
    }

    private fun writeWaveFile(waveform: FloatArray, sampleRate: Int): File {
        require(waveform.isNotEmpty()) { "ONNX 未输出音频波形" }
        val pcm = ShortArray(waveform.size) { index ->
            (waveform[index].coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort()
        }
        val outputDirectory = File(context.filesDir, "generated_audio/moss_tts_nano").apply { mkdirs() }
        val outputFile = File(outputDirectory, "moss_${System.currentTimeMillis()}.wav")
        outputFile.writeBytes(WavEncoder.encodePcm16Mono(RecordedAudio(pcm16 = pcm, sampleRate = sampleRate)))
        return outputFile
    }

    private companion object {
        const val WAV_HEADER_BYTES = 44
    }
}

data class MossTtsNanoRunnerRequest(
    val modelDirectory: File,
    val config: MossTtsNanoConfig,
    val plan: MossTtsNanoRunnerPlan,
    val text: String,
    val textTokenIds: IntArray,
    val referenceAudio: FloatArray
)

interface MossTtsNanoTokenizer {
    suspend fun encode(modelFile: File, text: String): IntArray
}

interface MossTtsNanoRunner {
    fun synthesize(request: MossTtsNanoRunnerRequest): FloatArray
}

class UnsupportedMossTtsNanoRunner : MossTtsNanoRunner {
    override fun synthesize(request: MossTtsNanoRunnerRequest): FloatArray {
        val stages = request.plan.sessionOrder.joinToString(" -> ") { it.name }
        error(
            "MOSS browser ONNX runner 尚未接入 Android 自回归执行器；" +
                "已解析执行计划: $stages"
        )
    }
}
