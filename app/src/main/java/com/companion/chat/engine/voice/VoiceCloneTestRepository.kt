package com.companion.chat.engine.voice

import android.content.Context
import com.companion.chat.R

class VoiceCloneTestRepository(context: Context) {
    private val packageName = context.applicationContext.packageName

    fun sweetGirlConfig(): com.companion.chat.engine.VoiceOutputConfig {
        return com.companion.chat.engine.VoiceOutputConfig(
            mode = com.companion.chat.engine.VoiceOutputMode.CLONE,
            referenceAudioUri = "android.resource://$packageName/${R.raw.moss_xiaoyu_sweet_cn}",
            displayName = "Xiaoyu 甜妹声线"
        )
    }
}
