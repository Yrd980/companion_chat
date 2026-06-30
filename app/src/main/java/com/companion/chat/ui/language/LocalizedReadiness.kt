package com.companion.chat.ui.language

import com.companion.chat.companion.readiness.CapabilityReadiness
import com.companion.chat.companion.readiness.CompanionCapability
import com.companion.chat.companion.readiness.CompanionReadinessLevel

fun CompanionCapability.uiLabel(language: AppLanguage): String {
    return when (this) {
        CompanionCapability.LLM -> uiText(language, "LLM", "大模型")
        CompanionCapability.ASR -> uiText(language, "Speech Recognition", "语音识别")
        CompanionCapability.TTS -> uiText(language, "Voice Output", "语音输出")
        CompanionCapability.IMAGE -> uiText(language, "Image Generation", "图片生成")
    }
}

fun CompanionReadinessLevel.uiLabel(language: AppLanguage): String {
    return when (this) {
        CompanionReadinessLevel.READY -> uiText(language, "Ready", "就绪")
        CompanionReadinessLevel.DEGRADED -> uiText(language, "Degraded", "降级可用")
        CompanionReadinessLevel.NOT_READY -> uiText(language, "Needs setup", "需要设置")
    }
}

fun CapabilityReadiness.uiProvider(language: AppLanguage): String {
    if (language == AppLanguage.ENGLISH) return provider
    return when (provider) {
        "Local SenseVoice" -> "本地 SenseVoice"
        "Cloud HTTP ASR" -> "云端 HTTP ASR"
        "HTTP clone + system TTS" -> "HTTP 克隆 + 系统 TTS"
        "Local MOSS clone + system TTS" -> "本地 MOSS 克隆 + 系统 TTS"
        "System TTS" -> "系统 TTS"
        "HTTP image generation" -> "HTTP 图片生成"
        "Local DreamLite" -> "本地 DreamLite"
        "Local Stable Diffusion" -> "本地 Stable Diffusion"
        "Xiaomi MiMo Cloud" -> "小米 MiMo 云端"
        else -> provider
    }
}

fun CapabilityReadiness.uiSummary(language: AppLanguage): String {
    return localizedRuntimeText(language, summary)
}

fun localizedRuntimeText(language: AppLanguage, text: String): String {
    if (language == AppLanguage.ENGLISH) return text.toEnglishRuntimeText()
    return text.toChineseRuntimeText()
}

private fun String.toEnglishRuntimeText(): String {
    return when {
        this == "图片生成 Base URL 未配置" -> "Image generation Base URL is not configured"
        this == "HTTP 图片生成已配置" -> "HTTP image generation is configured"
        this == "图片生成 endpoint 不可用" -> "Image generation endpoint is unavailable"
        this == "DreamLite 端侧真实出图尚未启用" -> "DreamLite on-device generation is not enabled yet"
        this == "DreamLite 模型目录未配置" -> "DreamLite model directory is not configured"
        startsWith("DreamLite 配置无效：") -> "Invalid DreamLite config: ${removePrefix("DreamLite 配置无效：")}"
        startsWith("DreamLite 文件缺失：") -> "Missing DreamLite files: ${removePrefix("DreamLite 文件缺失：")}"
        this == "本地图片模型已就绪" -> "Local image model is ready"
        this == "Stable Diffusion 模型目录未配置" -> "Stable Diffusion model directory is not configured"
        startsWith("Stable Diffusion 配置无效：") -> "Invalid Stable Diffusion config: ${removePrefix("Stable Diffusion 配置无效：")}"
        startsWith("Stable Diffusion 文件缺失：") -> "Missing Stable Diffusion files: ${removePrefix("Stable Diffusion 文件缺失：")}"
        this == "正在听..." -> "Listening..."
        this == "正在启动语音识别..." -> "Starting voice recognition..."
        this == "缺少录音权限，无法使用语音输入" -> "Microphone permission is required for voice input"
        this == "图片生成失败" -> "Image generation failed"
        startsWith("推理出错: ") -> "Inference failed: ${removePrefix("推理出错: ")}"
        this == "云端大模型已配置" -> "Cloud LLM is configured"
        this == "云端 API Key 未配置" -> "Cloud API key is not configured"
        else -> this
    }
}

private fun String.toChineseRuntimeText(): String {
    return when {
        this == "Text model package is ready" -> "文本模型包已就绪"
        this == "Text model is ready" -> "文本模型已就绪"
        this == "Text model is missing" -> "文本模型缺失"
        this == "Text model is unreadable" -> "文本模型不可读取"
        this == "Text model is empty" -> "文本模型为空"
        this == "Text model is not required" -> "不需要文本模型"
        this == "Speech recognition model is ready" -> "语音识别模型已就绪"
        this == "Local SenseVoice model is not configured" -> "本地 SenseVoice 模型未配置"
        startsWith("Speech recognition files are missing: ") -> "语音识别文件缺失：${removePrefix("Speech recognition files are missing: ")}"
        this == "Cloud ASR base URL is not configured" -> "云 ASR Base URL 未配置"
        this == "Cloud speech recognition is configured" -> "云语音识别已配置"
        this == "Cloud ASR endpoint is unavailable" -> "云 ASR endpoint 不可用"
        this == "HTTP voice clone is configured" -> "HTTP 语音克隆已配置"
        this == "MOSS voice clone model is ready" -> "MOSS 语音克隆模型已就绪"
        this == "HTTP clone endpoint is unavailable. System TTS can fall back." -> "HTTP 克隆 endpoint 不可用，可回退系统 TTS。"
        this == "Voice clone is not ready. System TTS can fall back." -> "语音克隆未就绪，可回退系统 TTS。"
        this == "moss-tts-nano model is not configured" -> "moss-tts-nano 模型未配置"
        startsWith("MOSS config is invalid: ") -> "MOSS 配置无效：${removePrefix("MOSS config is invalid: ")}"
        startsWith("MOSS files are missing: ") -> "MOSS 文件缺失：${removePrefix("MOSS files are missing: ")}"
        this == "Image generation Base URL is not configured" -> "图片生成 Base URL 未配置"
        this == "HTTP image generation is configured" -> "HTTP 图片生成已配置"
        this == "Image generation endpoint is unavailable" -> "图片生成 endpoint 不可用"
        this == "DreamLite on-device generation is not enabled yet" -> "DreamLite 端侧真实出图尚未启用"
        this == "DreamLite model directory is not configured" -> "DreamLite 模型目录未配置"
        startsWith("Invalid DreamLite config: ") -> "DreamLite 配置无效：${removePrefix("Invalid DreamLite config: ")}"
        startsWith("Missing DreamLite files: ") -> "DreamLite 文件缺失：${removePrefix("Missing DreamLite files: ")}"
        this == "Local image model is ready" -> "本地图片模型已就绪"
        this == "Stable Diffusion model directory is not configured" -> "Stable Diffusion 模型目录未配置"
        startsWith("Invalid Stable Diffusion config: ") -> "Stable Diffusion 配置无效：${removePrefix("Invalid Stable Diffusion config: ")}"
        startsWith("Missing Stable Diffusion files: ") -> "Stable Diffusion 文件缺失：${removePrefix("Missing Stable Diffusion files: ")}"
        this == "Listening..." -> "正在听..."
        this == "Starting voice recognition..." -> "正在启动语音识别..."
        this == "Microphone permission is required for voice input" -> "缺少录音权限，无法使用语音输入"
        this == "Image generation failed" -> "图片生成失败"
        startsWith("Inference failed: ") -> "推理出错: ${removePrefix("Inference failed: ")}"
        this == "Please enter a message" -> "请输入内容"
        this == "A reply is already generating. Please wait." -> "正在生成回复，请稍后再说"
        this == "The model is not loaded. Configure the model path in settings." -> "模型未加载，请在设置中配置模型路径。"
        this == "Cloud LLM is configured" -> "云端大模型已配置"
        this == "Cloud API key is not configured" -> "云端 API Key 未配置"
        else -> this
    }
}
