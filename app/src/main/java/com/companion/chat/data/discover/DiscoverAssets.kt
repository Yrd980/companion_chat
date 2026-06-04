package com.companion.chat.data.discover

import com.companion.chat.R

object DiscoverAssets {
    val XiaUrbanCover = drawableUri(R.drawable.cover_xia_urban)
    val ChenNocturneCover = drawableUri(R.drawable.cover_chen_nocturne)
    val MiraAdventureCover = drawableUri(R.drawable.cover_mira_adventure)
    val RinMatureCover = drawableUri(R.drawable.cover_rin_mature)
    val NikoAnimeCover = drawableUri(R.drawable.cover_niko_anime)

    val SweetGirlReferenceAudio = rawUri(R.raw.moss_xiaoyu_sweet_cn)
    const val SweetGirlVoiceName = "Xiaoyu"
    const val SweetGirlVoiceDisplayName = "Xiaoyu 甜妹声线"

    private fun drawableUri(resourceId: Int): String = "android.resource://com.companion.chat/$resourceId"

    private fun rawUri(resourceId: Int): String = "android.resource://com.companion.chat/$resourceId"
}
