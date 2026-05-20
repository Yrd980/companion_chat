package com.companion.chat.engine

import java.net.URL

internal object NetworkEndpointPolicy {
    fun requireHttpsOrLoopback(url: String, label: String) {
        val parsedUrl = runCatching { URL(url) }
            .getOrElse { error("$label URL 无效") }
        if (parsedUrl.protocol == "https") return
        if (parsedUrl.protocol == "http" && parsedUrl.host.isLoopbackHost()) return
        error("$label 必须使用 HTTPS；本地调试仅允许 http://127.0.0.1 或 http://localhost")
    }

    private fun String.isLoopbackHost(): Boolean {
        val normalized = trim().lowercase()
        return normalized == "localhost" ||
            normalized == "::1" ||
            normalized == "0:0:0:0:0:0:0:1" ||
            normalized.startsWith("127.")
    }
}
