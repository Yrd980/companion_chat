package com.companion.chat.engine

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.companion.chat.engine.VoiceOutputEngine
import com.companion.chat.engine.VoiceOutputConfig
import com.companion.chat.engine.VoiceOutputState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class AndroidVoiceOutputEngine(private val context: Context) : VoiceOutputEngine,
    TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceOutputEngine"
    }

    private val _state = MutableStateFlow<VoiceOutputState>(VoiceOutputState.Idle)
    override val state: StateFlow<VoiceOutputState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingInitCallbacks: MutableList<(Boolean) -> Unit> = mutableListOf()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "中文 TTS 不可用，尝试英文")
                tts?.setLanguage(Locale.ENGLISH)
            }
            configureSweetFemaleVoice()
            isInitialized = true
            Log.i(TAG, "TTS 初始化成功")
        } else {
            Log.e(TAG, "TTS 初始化失败: $status")
            _state.value = VoiceOutputState.Error("TTS 初始化失败")
        }
        pendingInitCallbacks.forEach { it(isInitialized) }
        pendingInitCallbacks.clear()
    }

    override suspend fun speak(text: String, config: VoiceOutputConfig) {
        if (!isInitialized) {
            val ready = suspendCancellableCoroutine { cont ->
                pendingInitCallbacks.add { success -> cont.resume(success) }
            }
            if (!ready) {
                _state.value = VoiceOutputState.Error("TTS 未就绪")
                return
            }
        }

        if (text.isBlank()) return

        _state.value = VoiceOutputState.Speaking

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = VoiceOutputState.Error("语音播放出错")
            }

            override fun onDone(utteranceId: String?) {
                _state.value = VoiceOutputState.Idle
            }
        })

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utterance_${System.currentTimeMillis()}")
    }

    private fun configureSweetFemaleVoice() {
        val engine = tts ?: return
        val sweetVoice = engine.voices
            ?.filter { voice ->
                val locale = voice.locale
                locale.language == Locale.CHINESE.language ||
                    locale.language.equals("zh", ignoreCase = true)
            }
            ?.filterNot { it.isNetworkConnectionRequired }
            ?.sortedWith(
                compareBy(
                    { voice -> if (voice.name.contains("female", ignoreCase = true)) 0 else 1 },
                    { voice -> if (voice.name.contains("girl", ignoreCase = true)) 0 else 1 },
                    { voice -> if (voice.name.contains("zh", ignoreCase = true)) 0 else 1 }
                )
            )
            ?.firstOrNull()
        if (sweetVoice != null) {
            runCatching { engine.voice = sweetVoice }
                .onSuccess { Log.i(TAG, "已选择系统 TTS 甜美女声候选: ${sweetVoice.name}") }
                .onFailure { Log.w(TAG, "设置系统 TTS 声线失败: ${it.message}") }
        }
        engine.setSpeechRate(1.06f)
        engine.setPitch(1.18f)
    }

    override fun stop() {
        tts?.stop()
        _state.value = VoiceOutputState.Idle
    }

    override fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "释放 TTS 出错", e)
        }
        tts = null
        isInitialized = false
        _state.value = VoiceOutputState.Idle
    }
}
