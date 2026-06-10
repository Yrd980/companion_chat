package com.companion.chat.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.chat.companion.readiness.CompanionReadinessLevel
import com.companion.chat.ui.components.ProductCard
import com.companion.chat.ui.components.ProductInnerShape
import com.companion.chat.ui.components.SectionTitle
import com.companion.chat.ui.components.StatusChip
import com.companion.chat.ui.language.uiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(),
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenModelSettings: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onOpenLanguage: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = uiText("Back", "返回"))
                    }
                },
                title = {
                    Text(
                        uiText("Setup Check", "设置检查"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ProductCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = ProductInnerShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                Icons.Default.HeadsetMic,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                uiText("Local Companion Setup", "本地陪伴设置"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                uiText(
                                    "Helmet pairing can be completed later. This check prepares profile, model, voice, image, and privacy.",
                                    "头盔配对可稍后完成。此检查会准备资料、模型、语音、图片和隐私。"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusChip(
                            text = if (uiState.isComplete) uiText("Ready", "就绪") else uiText("Review", "检查"),
                            level = if (uiState.isComplete) CompanionReadinessLevel.READY else CompanionReadinessLevel.DEGRADED
                        )
                    }
                }
            }
            item {
                ProductCard {
                    SectionTitle(uiText("Setup Steps", "设置步骤"))
                    uiState.steps.forEachIndexed { index, step ->
                        SetupStepRow(
                            step = step,
                            onClick = {
                                when (step.routeHint) {
                                    OnboardingViewModel.ROUTE_MODEL -> onOpenModelSettings()
                                    OnboardingViewModel.ROUTE_VOICE -> onOpenVoiceSettings()
                                    OnboardingViewModel.ROUTE_PRIVACY -> {
                                        viewModel.markPrivacyReviewed()
                                        onOpenProfile()
                                    }
                                    OnboardingViewModel.ROUTE_PROFILE -> onOpenProfile()
                                }
                            },
                            onInlineAction = {
                                when (step.id) {
                                    OnboardingViewModel.STEP_MICROPHONE -> viewModel.toggleMicrophoneReviewed()
                                    OnboardingViewModel.STEP_PRIVACY -> viewModel.markPrivacyReviewed()
                                    else -> Unit
                                }
                            }
                        )
                        if (index != uiState.steps.lastIndex) {
                            androidx.compose.material3.HorizontalDivider()
                        }
                    }
                }
            }
            item {
                ProductCard {
                    SectionTitle(uiText("Quick Repair", "快速修复"))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SetupActionChip(uiText("Model", "模型"), Icons.Default.Psychology, onOpenModelSettings)
                        SetupActionChip(uiText("Voice", "语音"), Icons.Default.RecordVoiceOver, onOpenVoiceSettings)
                        SetupActionChip(uiText("Privacy", "隐私"), Icons.Default.Security) {
                            viewModel.markPrivacyReviewed()
                            onOpenProfile()
                        }
                        SetupActionChip(uiText("Language", "语言"), Icons.Default.Language, onOpenLanguage)
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepRow(
    step: SetupStepUiState,
    onClick: () -> Unit,
    onInlineAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = ProductInnerShape,
            color = statusLevel(step.status).containerColor()
        ) {
            Icon(
                imageVector = step.icon(),
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
                tint = statusLevel(step.status).contentColor()
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = localizedStepTitle(step),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(localizedStatus(step.status), statusLevel(step.status))
            }
            Text(
                text = localizedStepDetail(step),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (step.id == OnboardingViewModel.STEP_MICROPHONE ||
                step.id == OnboardingViewModel.STEP_PRIVACY
            ) {
                Text(
                    text = localizedActionLabel(step),
                    modifier = Modifier.clickable(onClick = onInlineAction),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun SetupActionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun SetupStepUiState.icon(): ImageVector {
    return when (id) {
        OnboardingViewModel.STEP_PROFILE -> Icons.Default.Person
        OnboardingViewModel.STEP_MICROPHONE -> Icons.Default.Mic
        OnboardingViewModel.STEP_TEXT_MODEL -> Icons.Default.Psychology
        OnboardingViewModel.STEP_VOICE_INPUT -> Icons.Default.RecordVoiceOver
        OnboardingViewModel.STEP_VOICE_OUTPUT -> Icons.Default.HeadsetMic
        OnboardingViewModel.STEP_IMAGE -> Icons.Default.Image
        OnboardingViewModel.STEP_PRIVACY -> Icons.Default.PrivacyTip
        else -> Icons.Default.Security
    }
}

@Composable
private fun localizedStepTitle(step: SetupStepUiState): String {
    return when (step.id) {
        OnboardingViewModel.STEP_PROFILE -> uiText("Local Profile", "本地资料")
        OnboardingViewModel.STEP_MICROPHONE -> uiText("Microphone Permission", "麦克风权限")
        OnboardingViewModel.STEP_TEXT_MODEL -> uiText("Text Model", "文本模型")
        OnboardingViewModel.STEP_VOICE_INPUT -> uiText("Voice Input", "语音输入")
        OnboardingViewModel.STEP_VOICE_OUTPUT -> uiText("Voice Output", "语音输出")
        OnboardingViewModel.STEP_IMAGE -> uiText("Image Generation", "图片生成")
        OnboardingViewModel.STEP_PRIVACY -> uiText("Privacy Review", "隐私检查")
        else -> step.title
    }
}

@Composable
private fun localizedStepDetail(step: SetupStepUiState): String {
    return when (step.id to step.status) {
        OnboardingViewModel.STEP_PROFILE to SetupStatus.READY -> uiText("Profile is ready for local companion use.", "资料已可用于本地陪伴。")
        OnboardingViewModel.STEP_PROFILE to SetupStatus.REQUIRED -> uiText("Add a display name before daily use.", "日常使用前请添加显示名称。")
        OnboardingViewModel.STEP_MICROPHONE to SetupStatus.READY -> uiText("Voice capture has been reviewed on this device.", "已在本设备检查语音采集。")
        OnboardingViewModel.STEP_MICROPHONE to SetupStatus.OPTIONAL -> uiText("Text chat works now. Review microphone access before voice capture.", "文字聊天现在可用。语音采集前请检查麦克风权限。")
        OnboardingViewModel.STEP_TEXT_MODEL to SetupStatus.READY -> uiText("Local text model package is ready.", "本地文本模型包已就绪。")
        OnboardingViewModel.STEP_TEXT_MODEL to SetupStatus.NEEDS_ATTENTION -> uiText("Choose a local model path or continue with degraded text capability.", "选择本地模型路径，或以降级文本能力继续。")
        OnboardingViewModel.STEP_VOICE_INPUT to SetupStatus.READY -> uiText("Speech recognition and voice output are usable.", "语音识别和语音输出可用。")
        OnboardingViewModel.STEP_VOICE_INPUT to SetupStatus.NEEDS_ATTENTION -> uiText("Configure local voice input or keep text as the reliable fallback.", "配置本地语音输入，或保持文字作为可靠回退。")
        OnboardingViewModel.STEP_VOICE_OUTPUT to SetupStatus.READY -> uiText("Voice output can respond during companion turns.", "陪伴对话中可以语音回复。")
        OnboardingViewModel.STEP_VOICE_OUTPUT to SetupStatus.OPTIONAL -> uiText("System TTS can remain as fallback while voice clone models are missing.", "语音克隆模型缺失时可保留系统 TTS 回退。")
        OnboardingViewModel.STEP_IMAGE to SetupStatus.READY -> uiText("Local or configured image generation is ready.", "本地或已配置图片生成已就绪。")
        OnboardingViewModel.STEP_IMAGE to SetupStatus.OPTIONAL -> uiText("Image generation is optional and can be configured later.", "图片生成是可选项，可稍后配置。")
        OnboardingViewModel.STEP_PRIVACY to SetupStatus.READY -> uiText("Local-only defaults and opt-in cloud controls have been reviewed.", "已检查仅本地默认值和云端显式选择开关。")
        OnboardingViewModel.STEP_PRIVACY to SetupStatus.REQUIRED -> uiText("Review local-only mode before capture or generation.", "采集或生成前请检查仅本地模式。")
        else -> step.detail
    }
}

@Composable
private fun localizedActionLabel(step: SetupStepUiState): String {
    return when (step.id to step.status) {
        OnboardingViewModel.STEP_MICROPHONE to SetupStatus.READY -> uiText("Mark pending", "标记待检查")
        OnboardingViewModel.STEP_MICROPHONE to SetupStatus.OPTIONAL -> uiText("Mark reviewed", "标记已检查")
        OnboardingViewModel.STEP_PRIVACY to SetupStatus.READY -> uiText("Review again", "再次检查")
        OnboardingViewModel.STEP_PRIVACY to SetupStatus.REQUIRED -> uiText("Mark reviewed", "标记已检查")
        else -> step.actionLabel
    }
}

@Composable
private fun localizedStatus(status: SetupStatus): String {
    return when (status) {
        SetupStatus.READY -> uiText("Ready", "就绪")
        SetupStatus.REQUIRED -> uiText("Required", "需要配置")
        SetupStatus.OPTIONAL -> uiText("Optional", "可选")
        SetupStatus.SKIPPED -> uiText("Skipped", "已跳过")
        SetupStatus.NEEDS_ATTENTION -> uiText("Needs setup", "需要设置")
    }
}

private fun statusLevel(status: SetupStatus): CompanionReadinessLevel {
    return when (status) {
        SetupStatus.READY -> CompanionReadinessLevel.READY
        SetupStatus.OPTIONAL,
        SetupStatus.SKIPPED -> CompanionReadinessLevel.DEGRADED
        SetupStatus.REQUIRED,
        SetupStatus.NEEDS_ATTENTION -> CompanionReadinessLevel.NOT_READY
    }
}

@Composable
private fun CompanionReadinessLevel.containerColor() = when (this) {
    CompanionReadinessLevel.READY -> MaterialTheme.colorScheme.primaryContainer
    CompanionReadinessLevel.DEGRADED -> MaterialTheme.colorScheme.tertiaryContainer
    CompanionReadinessLevel.NOT_READY -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun CompanionReadinessLevel.contentColor() = when (this) {
    CompanionReadinessLevel.READY -> MaterialTheme.colorScheme.primary
    CompanionReadinessLevel.DEGRADED -> MaterialTheme.colorScheme.onTertiaryContainer
    CompanionReadinessLevel.NOT_READY -> MaterialTheme.colorScheme.error
}
