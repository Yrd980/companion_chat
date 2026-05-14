package com.companion.chat.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.companion.chat.data.engine.VoiceInputEngine
import com.companion.chat.data.engine.VoiceInputEvent
import com.companion.chat.data.voice.CloudAsrConfigRepository
import com.companion.chat.data.voice.LocalSenseVoiceModelStatus
import com.companion.chat.data.voice.VoiceInputBackend
import com.companion.chat.data.voice.VoiceInputConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

class AndroidVoiceInputEngine(
    private val context: Context,
    private val configRepository: VoiceInputConfigRepository = VoiceInputConfigRepository(context),
    private val cloudAsrEngine: CloudHttpAsrEngine = CloudHttpAsrEngine(CloudAsrConfigRepository(context))
) : VoiceInputEngine {

    private val _events = MutableSharedFlow<VoiceInputEvent>(extraBufferCapacity = 16)
    override val events: Flow<VoiceInputEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var recorder: AudioRecord? = null
    @Volatile
    private var isListening = false
    @Volatile
    private var stopRequested = false

    override fun warmUp() {
        when (val status = configRepository.getLocalSenseVoiceModelStatus()) {
            LocalSenseVoiceModelStatus.Ready -> _events.tryEmit(VoiceInputEvent.WarmedUp)
            LocalSenseVoiceModelStatus.DirectoryNotConfigured ->
                _events.tryEmit(VoiceInputEvent.Error("本地 SenseVoice 模型未配置"))
            is LocalSenseVoiceModelStatus.MissingFiles ->
                _events.tryEmit(VoiceInputEvent.Error("本地 SenseVoice 模型文件缺失: ${status.fileNames.joinToString()}"))
        }
    }

    override fun startListening() {
        if (isListening) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _events.tryEmit(VoiceInputEvent.Error("缺少录音权限"))
            return
        }

        val config = configRepository.getConfig()
        if (config.backend == VoiceInputBackend.LOCAL_SENSEVOICE) {
            when (val status = configRepository.getLocalSenseVoiceModelStatus(config)) {
                LocalSenseVoiceModelStatus.Ready -> Unit
                LocalSenseVoiceModelStatus.DirectoryNotConfigured -> {
                    _events.tryEmit(VoiceInputEvent.Error("本地 SenseVoice 模型未配置"))
                    return
                }
                is LocalSenseVoiceModelStatus.MissingFiles -> {
                    _events.tryEmit(VoiceInputEvent.Error("本地 SenseVoice 模型文件缺失: ${status.fileNames.joinToString()}"))
                    return
                }
            }
        }

        isListening = true
        stopRequested = false
        _events.tryEmit(VoiceInputEvent.Listening)
        scope.launch {
            runCatching {
                val audio = recordUntilSilence()
                if (stopRequested) {
                    return@launch
                }
                if (audio.isEmpty) {
                    _events.tryEmit(VoiceInputEvent.Error("未检测到语音"))
                    return@launch
                }
                val text = withContext(Dispatchers.IO) {
                    when (config.backend) {
                        VoiceInputBackend.LOCAL_SENSEVOICE -> {
                            SherpaOnnxSenseVoiceRecognizer(
                                assetManager = null,
                                resolveSenseVoiceModelFiles(config.localSenseVoiceModelDirectory)
                            ).transcribe(audio)
                        }
                        VoiceInputBackend.CLOUD_HTTP_ASR -> cloudAsrEngine.transcribe(audio)
                    }
                }
                if (text.isBlank()) {
                    _events.tryEmit(VoiceInputEvent.Error("未识别到文本"))
                } else {
                    _events.tryEmit(VoiceInputEvent.FinalResult(text))
                }
            }.getOrElse { throwable ->
                Log.e(TAG, "语音输入失败", throwable)
                _events.tryEmit(VoiceInputEvent.Error(throwable.message ?: "语音输入失败"))
            }
            isListening = false
            releaseRecorder()
            _events.tryEmit(VoiceInputEvent.NotListening)
        }
    }

    override fun stopListening() {
        stopRequested = true
        isListening = false
        runCatching {
            recorder?.stop()
        }
        _events.tryEmit(VoiceInputEvent.NotListening)
    }

    override fun release() {
        stopListening()
        scope.cancel()
    }

    private fun recordUntilSilence(): RecordedAudio {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBufferSize, SAMPLE_RATE / 2)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        recorder = audioRecord

        val captured = ByteArrayOutputStream()
        val buffer = ShortArray(FRAME_SAMPLES)
        var voicedFrames = 0
        var silentFramesAfterSpeech = 0
        var totalFrames = 0

        try {
            audioRecord.startRecording()
        } catch (e: IllegalStateException) {
            throw IllegalStateException("启动录音失败: ${e.message}", e)
        }
        while (!stopRequested && isListening && totalFrames < MAX_FRAMES && scope.isActive) {
            val read = audioRecord.read(buffer, 0, buffer.size)
            if (read <= 0) continue

            totalFrames += 1
            val frameEnergy = buffer.averageAbs(read)
            val hasSpeech = frameEnergy >= ENERGY_THRESHOLD
            if (hasSpeech) {
                voicedFrames += 1
                silentFramesAfterSpeech = 0
            } else if (voicedFrames > 0) {
                silentFramesAfterSpeech += 1
            }

            if (voicedFrames > 0) {
                captured.writeShorts(buffer, read)
            }
            if (voicedFrames >= MIN_VOICED_FRAMES && silentFramesAfterSpeech >= SILENCE_END_FRAMES) {
                break
            }
        }

        val bytes = captured.toByteArray()
        val shorts = ShortArray(bytes.size / Short.SIZE_BYTES)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        return RecordedAudio(shorts, SAMPLE_RATE)
    }

    private fun releaseRecorder() {
        val audioRecord = recorder
        recorder = null
        runCatching {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop()
            }
        }
        runCatching {
            audioRecord?.release()
        }
    }

    private fun ShortArray.averageAbs(length: Int): Int {
        var sum = 0L
        for (index in 0 until length) {
            sum += abs(this[index].toInt())
        }
        return (sum / length.coerceAtLeast(1)).toInt()
    }

    private fun ByteArrayOutputStream.writeShorts(values: ShortArray, length: Int) {
        for (index in 0 until length) {
            val value = values[index].toInt()
            write(value and 0xff)
            write((value shr 8) and 0xff)
        }
    }

    private companion object {
        const val TAG = "VoiceInputEngine"
        const val SAMPLE_RATE = 16_000
        const val FRAME_SAMPLES = 512
        const val ENERGY_THRESHOLD = 700
        const val MIN_VOICED_FRAMES = 3
        const val SILENCE_END_FRAMES = 20
        const val MAX_RECORDING_MILLIS = 15_000
        const val MAX_FRAMES = MAX_RECORDING_MILLIS / (FRAME_SAMPLES * 1000 / SAMPLE_RATE)
    }
}
