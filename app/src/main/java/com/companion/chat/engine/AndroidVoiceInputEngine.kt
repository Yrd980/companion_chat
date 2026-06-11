package com.companion.chat.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.companion.chat.data.privacy.PrivacyGate
import com.companion.chat.data.privacy.PrivacyGateDefaults
import com.companion.chat.engine.VoiceInputEngine
import com.companion.chat.engine.VoiceInputEvent
import com.companion.chat.engine.voice.CloudAsrConfigRepository
import com.companion.chat.engine.voice.LocalSenseVoiceModelStatus
import com.companion.chat.engine.voice.VoiceInputBackend
import com.companion.chat.engine.voice.VoiceInputConfig
import com.companion.chat.engine.voice.VoiceInputConfigRepository
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
import kotlin.math.max

class AndroidVoiceInputEngine(
    private val context: Context,
    private val configRepository: VoiceInputConfigRepository = VoiceInputConfigRepository(context),
    privacyGate: PrivacyGate = PrivacyGateDefaults.denyRemoteByDefault(),
    private val cloudAsrEngine: CloudHttpAsrEngine = CloudHttpAsrEngine(
        configRepository = CloudAsrConfigRepository(context),
        privacyGate = privacyGate
    )
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
        val config = configRepository.getConfig()
        Log.i(TAG, "warmUp backend=${config.backend}")
        if (config.backend == VoiceInputBackend.CLOUD_HTTP_ASR) {
            _events.tryEmit(VoiceInputEvent.WarmedUp)
            return
        }

        emitLocalModelStatus(config)
    }

    private fun emitLocalModelStatus(config: VoiceInputConfig): Boolean {
        return when (val status = configRepository.getLocalSenseVoiceModelStatus(config)) {
            LocalSenseVoiceModelStatus.Ready -> {
                Log.i(TAG, "local SenseVoice model ready")
                _events.tryEmit(VoiceInputEvent.WarmedUp)
                true
            }
            LocalSenseVoiceModelStatus.DirectoryNotConfigured -> {
                Log.w(TAG, "local SenseVoice model directory not configured")
                _events.tryEmit(VoiceInputEvent.Error("本地 SenseVoice 模型未配置"))
                false
            }
            is LocalSenseVoiceModelStatus.MissingFiles -> {
                Log.w(TAG, "local SenseVoice model missing files: ${status.fileNames.joinToString()}")
                _events.tryEmit(VoiceInputEvent.Error("本地 SenseVoice 模型文件缺失: ${status.fileNames.joinToString()}"))
                false
            }
        }
    }

    override fun startListening() {
        Log.i(TAG, "startListening requested isListening=$isListening")
        if (isListening) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "startListening denied: RECORD_AUDIO missing")
            _events.tryEmit(VoiceInputEvent.Error("缺少录音权限"))
            return
        }

        val config = configRepository.getConfig()
        Log.i(TAG, "startListening backend=${config.backend}")
        if (config.backend == VoiceInputBackend.LOCAL_SENSEVOICE && !emitLocalModelStatus(config)) {
            return
        }

        isListening = true
        stopRequested = false
        _events.tryEmit(VoiceInputEvent.Listening)
        Log.i(TAG, "listening event emitted")
        scope.launch {
            runCatching {
                val audio = recordUntilSilence(config)
                if (stopRequested) {
                    return@launch
                }
                if (audio.isEmpty) {
                    Log.w(TAG, "recorded audio is empty")
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
                    Log.w(TAG, "ASR returned blank text")
                    _events.tryEmit(VoiceInputEvent.Error("未识别到文本"))
                } else {
                    Log.i(TAG, "ASR final text length=${text.length}")
                    _events.tryEmit(VoiceInputEvent.FinalResult(text))
                }
            }.getOrElse { throwable ->
                if (stopRequested) {
                    return@launch
                }
                Log.e(TAG, "语音输入失败", throwable)
                _events.tryEmit(VoiceInputEvent.Error(throwable.message ?: "语音输入失败"))
            }
            isListening = false
            releaseRecorder()
            _events.tryEmit(VoiceInputEvent.NotListening)
            Log.i(TAG, "not listening event emitted")
        }
    }

    override fun stopListening() {
        Log.i(TAG, "stopListening requested")
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

    private fun recordUntilSilence(config: VoiceInputConfig): RecordedAudio {
        if (config.backend == VoiceInputBackend.CLOUD_HTTP_ASR) {
            return recordFixedWindow()
        }

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

        val buffer = ShortArray(FRAME_SAMPLES)
        var totalFrames = 0
        val vad = SherpaOnnxSileroVad(
            assetManager = null,
            configValues = SileroVadConfigValues(
                model = resolveSenseVoiceModelFiles(config.localSenseVoiceModelDirectory).vad
            )
        )

        try {
            audioRecord.startRecording()
            while (!stopRequested && isListening && totalFrames < MAX_FRAMES && scope.isActive) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read <= 0) continue

                totalFrames += 1
                vad.acceptWaveform(AudioPcmConverter.pcm16ToFloatArray(buffer, read))
                val segment = vad.drainSegments().firstOrNull { audio -> !audio.isEmpty }
                if (segment != null) {
                    return segment
                }
            }
            vad.flush()
            return vad.drainSegments().firstOrNull { audio -> !audio.isEmpty }
                ?: RecordedAudio(ShortArray(0), SAMPLE_RATE)
        } catch (e: IllegalStateException) {
            if (stopRequested) {
                return RecordedAudio(ShortArray(0), SAMPLE_RATE)
            }
            throw e
        } catch (e: Exception) {
            if (stopRequested) {
                return RecordedAudio(ShortArray(0), SAMPLE_RATE)
            }
            throw IllegalStateException(e.message ?: "语音录制失败", e)
        } finally {
            vad.release()
        }
    }

    private fun recordFixedWindow(): RecordedAudio {
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

        val buffer = ShortArray(FRAME_SAMPLES)
        val samples = mutableListOf<Short>()
        var totalFrames = 0

        try {
            audioRecord.startRecording()
            while (!stopRequested && isListening && totalFrames < CLOUD_MAX_FRAMES && scope.isActive) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read <= 0) continue

                totalFrames += 1
                repeat(read) { index ->
                    samples += buffer[index]
                }
            }
            return RecordedAudio(samples.toShortArray(), SAMPLE_RATE)
        } catch (e: IllegalStateException) {
            if (stopRequested) {
                return RecordedAudio(ShortArray(0), SAMPLE_RATE)
            }
            throw e
        } catch (e: Exception) {
            if (stopRequested) {
                return RecordedAudio(ShortArray(0), SAMPLE_RATE)
            }
            throw IllegalStateException(e.message ?: "语音录制失败", e)
        }
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

    private companion object {
        const val TAG = "VoiceInputEngine"
        const val SAMPLE_RATE = 16_000
        const val FRAME_SAMPLES = 512
        const val MAX_RECORDING_MILLIS = 15_000
        const val MAX_FRAMES = MAX_RECORDING_MILLIS / (FRAME_SAMPLES * 1000 / SAMPLE_RATE)
        const val CLOUD_RECORDING_MILLIS = 5_000
        const val CLOUD_MAX_FRAMES = CLOUD_RECORDING_MILLIS / (FRAME_SAMPLES * 1000 / SAMPLE_RATE)
    }
}
