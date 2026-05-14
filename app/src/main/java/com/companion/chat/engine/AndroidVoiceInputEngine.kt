package com.companion.chat.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.companion.chat.data.engine.VoiceInputEngine
import com.companion.chat.data.engine.VoiceInputEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale

class AndroidVoiceInputEngine(private val context: Context) : VoiceInputEngine {

    companion object {
        private const val TAG = "VoiceInputEngine"
    }

    private val _events = MutableSharedFlow<VoiceInputEvent>(extraBufferCapacity = 16)
    override val events: Flow<VoiceInputEvent> = _events.asSharedFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun warmUp() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _events.tryEmit(VoiceInputEvent.Error("设备不支持语音识别"))
            return
        }
        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            }
            _events.tryEmit(VoiceInputEvent.WarmedUp)
        } catch (e: Exception) {
            Log.e(TAG, "预热语音识别失败", e)
            _events.tryEmit(VoiceInputEvent.Error("预热语音识别失败: ${e.message}"))
        }
    }

    override fun startListening() {
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _events.tryEmit(VoiceInputEvent.Error("设备不支持语音识别"))
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            _events.tryEmit(VoiceInputEvent.Listening)
        } catch (e: Exception) {
            Log.e(TAG, "启动语音识别失败", e)
            _events.tryEmit(VoiceInputEvent.Error("启动语音识别失败: ${e.message}"))
        }
    }

    override fun stopListening() {
        if (!isListening) return
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "停止语音识别出错", e)
        }
        isListening = false
        _events.tryEmit(VoiceInputEvent.NotListening)
    }

    override fun release() {
        stopListening()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "释放语音识别出错", e)
        }
        speechRecognizer = null
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "准备就绪，请说话")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "检测到语音")
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "语音结束")
            isListening = false
            _events.tryEmit(VoiceInputEvent.NotListening)
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少录音权限"
                SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别引擎忙碌"
                SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时，请重试"
                else -> "未知错误 ($error)"
            }
            Log.w(TAG, "语音识别错误: $message")
            isListening = false
            _events.tryEmit(VoiceInputEvent.Error(message))
            _events.tryEmit(VoiceInputEvent.NotListening)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            Log.d(TAG, "识别结果: $text")
            if (text.isNotBlank()) {
                _events.tryEmit(VoiceInputEvent.FinalResult(text))
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                _events.tryEmit(VoiceInputEvent.PartialResult(text))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
