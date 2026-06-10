package com.companion.chat.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.chat.companion.readiness.CapabilityReadiness
import com.companion.chat.companion.readiness.CompanionCapability
import com.companion.chat.companion.readiness.CompanionReadinessLevel
import com.companion.chat.engine.BackendType
import com.companion.chat.engine.LocalLmFileStatus
import com.companion.chat.engine.ModelRuntime
import com.companion.chat.engine.image.ImageGenerationConfig
import com.companion.chat.engine.image.ImageGenerationCapabilities
import com.companion.chat.engine.image.ImageGenerationProvider
import com.companion.chat.engine.image.DreamLiteModelStatus
import com.companion.chat.engine.image.StableDiffusionModelStatus
import com.companion.chat.ui.language.AppLanguage
import com.companion.chat.ui.language.LocalAppLanguage
import com.companion.chat.ui.language.uiLabel
import com.companion.chat.ui.language.localizedRuntimeText
import com.companion.chat.ui.language.uiProvider
import com.companion.chat.ui.language.uiSummary
import com.companion.chat.ui.language.uiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onModelConfigChanged: () -> Unit = {},
    viewModel: ModelConfigViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val retainedRounds = uiState.retainedRounds
    val modelConfig = uiState.modelConfig
    val imageConfig = uiState.imageConfig
    val dreamLiteModelStatus = uiState.dreamLiteModelStatus
    val stableDiffusionModelStatus = uiState.stableDiffusionModelStatus
    val options = listOf(3, 5, 10, 15, 20)
    val localLmPackageStatus = uiState.localLmPackageStatus
    val imageProviderReadiness = uiState.imageProviderReadiness
    val imageCapabilities = imageProviderReadiness.capabilities
    val readinessSnapshot = uiState.readinessSnapshot
    val language = LocalAppLanguage.current

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = uiText("Model Configuration", "模型配置"),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = uiText("Back", "返回")
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = uiText("Context Window Size", "上下文窗口大小"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiText(
                        "Currently keeps the last $retainedRounds complete conversation rounds.\nChanges apply after the next message is sent.",
                        "当前保留最近 $retainedRounds 轮完整对话。\n修改后会在下一次发送消息时生效。"
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = uiText("Global Runtime Status", "全域运行状态"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                readinessSnapshot.capabilities.forEach { readiness ->
                    ReadinessInfoRow(readiness, language)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = uiText("Inference Backend", "推理后端"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                ModelRuntimeOptionItem(
                    title = "llama.cpp GGUF",
                    description = uiText(
                        "Default text backend. Reads an external GGUF uncensor model.",
                        "默认文本后端，读取外部 GGUF uncensor 模型。"
                    ),
                    selected = modelConfig.runtime == ModelRuntime.LLAMA_CPP_GGUF,
                    onClick = {
                        viewModel.setRuntime(ModelRuntime.LLAMA_CPP_GGUF)
                        onModelConfigChanged()
                    }
                )
                ModelRuntimeOptionItem(
                    title = "LiteRT-LM",
                    description = uiText(
                        "Optional multimodal backend that keeps the image-input path available.",
                        "可选多模态后端，继续支持图片输入链路。"
                    ),
                    selected = modelConfig.runtime == ModelRuntime.LITERT_LM,
                    onClick = {
                        viewModel.setRuntime(ModelRuntime.LITERT_LM)
                        onModelConfigChanged()
                    }
                )
                if (modelConfig.runtime == ModelRuntime.LITERT_LM) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiText("LiteRT-LM Acceleration Backend", "LiteRT-LM 加速后端"),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    BackendOptionItem(
                        title = "CPU",
                        description = uiText(
                            "Currently verified. Runs through XNNPack.",
                            "当前已验证可用，使用 XNNPack 执行。"
                        ),
                        selected = modelConfig.backend == BackendType.CPU,
                        onClick = {
                            viewModel.setBackend(BackendType.CPU)
                            onModelConfigChanged()
                        }
                    )
                    BackendOptionItem(
                        title = "GPU",
                        description = uiText(
                            "Attempts Mali/OpenCL/OpenGL acceleration and falls back to CPU on failure.",
                            "尝试 Mali/OpenCL/OpenGL 加速；失败会自动回退 CPU。"
                        ),
                        selected = modelConfig.backend == BackendType.GPU,
                        onClick = {
                            viewModel.setBackend(BackendType.GPU)
                            onModelConfigChanged()
                        }
                    )
                    BackendOptionItem(
                        title = "NPU",
                        description = uiText(
                            "Attempts the vendor NPU runtime and falls back to GPU when the model is incompatible.",
                            "尝试厂商 NPU runtime；模型不兼容时会先回退 GPU。"
                        ),
                        selected = modelConfig.backend == BackendType.NPU,
                        onClick = {
                            viewModel.setBackend(BackendType.NPU)
                            onModelConfigChanged()
                        }
                    )
                }
                ModelConfigField(uiText("Model Path", "模型路径"), modelConfig.modelPath) {
                    viewModel.updateModelPath(it)
                }
                Text(
                    text = uiText(
                        "Leave blank to use the default path: ${localLmPackageStatus.modelPath}",
                        "留空使用默认路径：${localLmPackageStatus.modelPath}"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = uiText("Text model: ", "文本模型：") +
                        "${localLmPackageStatus.modelFileStatus.displayName(language)}\n" +
                        localLmPackageStatus.modelPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (localLmPackageStatus.isModelReady) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (localLmPackageStatus.isMmprojRelevant) {
                    Text(
                        text = uiText("Image projector: ", "图片 projector：") +
                            localLmPackageStatus.mmprojFileStatus.displayName(language) +
                            uiText(" (only required for image input)\n", "（仅图片输入需要）\n") +
                            localLmPackageStatus.mmprojPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (localLmPackageStatus.mmprojFileStatus is LocalLmFileStatus.Ready) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                ModelConfigField("Context Size", modelConfig.contextSize.toString()) {
                    viewModel.updateContextSize(it)
                }
                ModelConfigField("Max Tokens", modelConfig.maxTokens.toString()) {
                    viewModel.updateMaxTokens(it)
                }
                ModelConfigField("Temperature", modelConfig.temperature.toString()) {
                    viewModel.updateTemperature(it)
                }
                ModelConfigField("Top K", modelConfig.topK.toString()) {
                    viewModel.updateTopK(it)
                }
                ModelConfigField("Top P", modelConfig.topP.toString()) {
                    viewModel.updateTopP(it)
                }
                androidx.compose.material3.Button(
                    onClick = onModelConfigChanged,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(uiText("Apply Model Configuration", "应用模型配置"))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            options.forEach { option ->
                ContextWindowOptionItem(
                    rounds = option,
                    selected = retainedRounds == option,
                    onClick = {
                        viewModel.updateRetainedRounds(option)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            Text(
                text = uiText(
                    "Recommended range: 3-20. Higher values keep more raw context; lower values trigger compression earlier.",
                    "建议范围 3~20。轮数越大，保留原始上下文越多；轮数越小，越早触发压缩。"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = uiText("Image Generation Provider Configuration", "图片生成 Provider 配置"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                ImageProviderOptionItem(
                    title = uiText("HTTP Online Generation", "HTTP 联网生成"),
                    description = uiText(
                        "Uses a generic HTTP image API. Can generate real images when configured.",
                        "使用通用 HTTP 图片接口，配置可真实生成图片"
                    ),
                    selected = imageConfig.provider == ImageGenerationProvider.HTTP,
                    onClick = {
                        viewModel.setImageProvider(ImageGenerationProvider.HTTP)
                    }
                )
                ImageProviderOptionItem(
                    title = uiText("Local SD1.5 Hyper-SD", "本地 SD1.5 Hyper-SD"),
                    description = uiText(
                        "stable-diffusion.cpp + Vulkan for private local image generation.",
                        "stable-diffusion.cpp + Vulkan，本地私有出图"
                    ),
                    selected = imageConfig.provider == ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP,
                    onClick = {
                        viewModel.setImageProvider(ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP)
                    }
                )
                ImageProviderOptionItem(
                    title = uiText("Local DreamLite", "本地 DreamLite"),
                    description = uiText(
                        "Model package can be checked; Android inference runtime is not wired yet.",
                        "模型包可检查，Android 端推理运行时待接入"
                    ),
                    selected = imageConfig.provider == ImageGenerationProvider.LOCAL_DREAMLITE,
                    onClick = {
                        viewModel.setImageProvider(ImageGenerationProvider.LOCAL_DREAMLITE)
                    }
                )
                Text(
                    text = uiText("Current status: ", "当前状态：") + localizedRuntimeText(language, imageProviderReadiness.summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (imageProviderReadiness.isUsable) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = imageCapabilities.displayName(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (imageCapabilities.usesLocalModelPackage) {
                    ImageConfigField(uiText("Local Model Path", "本地模型路径"), imageConfig.localModelPath) {
                        viewModel.updateLocalModelPath(it)
                    }
                    Text(
                        text = when (imageConfig.provider) {
                            ImageGenerationProvider.LOCAL_STABLE_DIFFUSION_CPP ->
                                uiText("Stable Diffusion status: ", "Stable Diffusion 状态：") +
                                    stableDiffusionModelStatus.displayName(language)
                            else -> uiText("DreamLite status: ", "DreamLite 状态：") +
                                dreamLiteModelStatus.displayName(language)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (imageProviderReadiness.isUsable) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (imageCapabilities.usesImageSize) {
                    ImageConfigField(uiText("Local Width", "本地宽度"), imageConfig.localWidth.toString()) {
                        viewModel.updateLocalWidth(it)
                    }
                    ImageConfigField(uiText("Local Height", "本地高度"), imageConfig.localHeight.toString()) {
                        viewModel.updateLocalHeight(it)
                    }
                    ImageConfigField(uiText("Local Steps", "本地 Steps"), imageConfig.localSteps.toString()) {
                        viewModel.updateLocalSteps(it)
                    }
                    ImageConfigField(uiText("Local CFG Scale", "本地 CFG Scale"), imageConfig.localCfgScale.toString()) {
                        viewModel.updateLocalCfgScale(it)
                    }
                }
                if (imageCapabilities.usesSeed) {
                    ImageConfigField(uiText("Local Seed (blank for random)", "本地 Seed（留空随机）"), imageConfig.localSeed?.toString().orEmpty()) {
                        viewModel.updateLocalSeed(it)
                    }
                }
                if (imageCapabilities.usesVulkan) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = imageConfig.localUseVulkan,
                            onCheckedChange = { viewModel.setLocalUseVulkan(it) }
                        )
                        Text(
                            text = uiText("Enable Vulkan", "启用 Vulkan"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (imageCapabilities.usesHttpEndpoint) {
                    ImageConfigField("Base URL", imageConfig.baseUrl) {
                        viewModel.updateImageBaseUrl(it)
                    }
                }
                if (imageCapabilities.usesApiKey) {
                    ImageConfigField("API Key", imageConfig.apiKey) {
                        viewModel.updateImageApiKey(it)
                    }
                }
                if (imageCapabilities.usesModelName) {
                    ImageConfigField("Model", imageConfig.model) {
                        viewModel.updateImageModel(it)
                    }
                }
                if (imageCapabilities.usesHttpEndpoint) {
                    ImageConfigField("Request Template", imageConfig.requestTemplate, minLines = 3) {
                        viewModel.updateRequestTemplate(it)
                    }
                    ImageConfigField("Response Image Field Path", imageConfig.responseImageFieldPath) {
                        viewModel.updateResponseImageFieldPath(it)
                    }
                    ImageConfigField("Timeout Millis", imageConfig.timeoutMillis.toString()) {
                        viewModel.updateTimeoutMillis(it)
                    }
                    Text(
                        text = uiText(
                            "Templates support {{model}} and {{prompt}}. Response field examples: data.0.url or data.0.b64_json.",
                            "模板支持 {{model}} 与 {{prompt}}。响应字段示例：data.0.url 或 data.0.b64_json。"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadinessInfoRow(readiness: CapabilityReadiness, language: AppLanguage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = readiness.capability.uiLabel(language),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.8f)
        )
        Text(
            text = "${readiness.uiProvider(language)}: ${readiness.uiSummary(language)}",
            style = MaterialTheme.typography.bodySmall,
            color = when (readiness.level) {
                CompanionReadinessLevel.READY -> MaterialTheme.colorScheme.onSurfaceVariant
                CompanionReadinessLevel.DEGRADED -> MaterialTheme.colorScheme.primary
                CompanionReadinessLevel.NOT_READY -> MaterialTheme.colorScheme.error
            },
            modifier = Modifier.weight(2.2f)
        )
    }
}

private fun DreamLiteModelStatus.displayName(language: AppLanguage): String {
    return when (this) {
        is DreamLiteModelStatus.Ready -> uiText(
            language,
            "Model package is ready: ${config.modelName}. Android inference runtime is not wired yet.",
            "模型包已就绪：${config.modelName}，Android 端推理运行时待接入"
        )
        DreamLiteModelStatus.DirectoryNotConfigured -> uiText(language, "Model directory is not configured", "模型目录未配置")
        is DreamLiteModelStatus.InvalidConfig -> uiText(language, "Invalid config: $message", "配置无效：$message")
        is DreamLiteModelStatus.MissingFiles -> uiText(language, "Missing files: ${fileNames.joinToString()}", "文件缺失：${fileNames.joinToString()}")
    }
}

private fun StableDiffusionModelStatus.displayName(language: AppLanguage): String {
    return when (this) {
        is StableDiffusionModelStatus.Ready -> uiText(language, "Model package is ready: ${config.modelName}", "模型包已就绪：${config.modelName}")
        StableDiffusionModelStatus.DirectoryNotConfigured -> uiText(language, "Model directory is not configured", "模型目录未配置")
        is StableDiffusionModelStatus.InvalidConfig -> uiText(language, "Invalid config: $message", "配置无效：$message")
        is StableDiffusionModelStatus.MissingFiles -> uiText(language, "Missing files: ${fileNames.joinToString()}", "文件缺失：${fileNames.joinToString()}")
    }
}

private fun ImageGenerationCapabilities.displayName(language: AppLanguage): String {
    val values = listOfNotNull(
        uiText(language, "Text to image", "文生图").takeIf { supportsTextToImage },
        "HTTP endpoint".takeIf { usesHttpEndpoint },
        "API key".takeIf { usesApiKey },
        uiText(language, "Model name", "模型名").takeIf { usesModelName },
        uiText(language, "Local model package", "本地模型包").takeIf { usesLocalModelPackage },
        uiText(language, "Negative prompt", "负向提示词").takeIf { usesNegativePrompt },
        "Seed".takeIf { usesSeed },
        uiText(language, "Size parameters", "尺寸参数").takeIf { usesImageSize },
        "Vulkan".takeIf { usesVulkan }
    )
    return uiText(language, "Capabilities: ", "能力：") +
        values.ifEmpty { listOf(uiText(language, "Real image generation is not enabled yet", "暂未启用真实出图")) }.joinToString(" / ")
}

private fun LocalLmFileStatus.displayName(language: AppLanguage): String {
    return when (this) {
        is LocalLmFileStatus.Ready -> uiText(language, "Ready (${byteCount} bytes)", "已就绪 (${byteCount} bytes)")
        LocalLmFileStatus.Missing -> uiText(language, "Missing", "缺失")
        LocalLmFileStatus.Unreadable -> uiText(language, "Unreadable", "不可读取")
        LocalLmFileStatus.Empty -> uiText(language, "File is empty", "文件为空")
        LocalLmFileStatus.NotRequired -> uiText(language, "Not required", "不需要")
    }
}

@Composable
private fun ImageProviderOptionItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImageConfigField(
    label: String,
    value: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = minLines,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}

@Composable
private fun ModelConfigField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}

@Composable
private fun ModelRuntimeOptionItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BackendOptionItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContextWindowOptionItem(
    rounds: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.height(0.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = uiText("Keep the last $rounds rounds", "保留最近 $rounds 轮"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = uiText(
                    "Compression threshold is about ${rounds * 2 + 10} messages",
                    "压缩阈值约为 ${rounds * 2 + 10} 条消息"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
