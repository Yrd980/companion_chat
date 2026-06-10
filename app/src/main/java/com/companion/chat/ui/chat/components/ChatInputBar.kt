package com.companion.chat.ui.chat.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.companion.chat.companion.voice.VoiceFirstInteractionState
import com.companion.chat.ui.language.AppLanguage
import com.companion.chat.ui.language.LocalAppLanguage
import com.companion.chat.ui.language.uiText

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelGeneration: () -> Unit,
    onPickImage: () -> Unit,
    onGenerateImage: () -> Unit,
    onVoiceInput: () -> Unit,
    selectedImages: List<Uri>,
    onRemoveImage: (Uri) -> Unit,
    voice: VoiceFirstInteractionState,
    isGenerating: Boolean = false,
    isImageGenerating: Boolean = false,
    canVoiceOutput: Boolean = false,
    onVoiceOutput: () -> Unit = {},
    onStopSpeaking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedImages.isNotEmpty()) {
                    SelectedImagePreviewRow(
                        selectedImages = selectedImages,
                        onRemoveImage = onRemoveImage
                    )
                }
                if (voice.shouldShowInputPreview) {
                    VoiceInputPreview(
                        voice = voice
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    ChatToolIconButton(
                        onClick = onPickImage,
                        icon = Icons.Default.AddPhotoAlternate,
                        contentDescription = uiText("Upload image", "上传图片")
                    )
                    ChatToolIconButton(
                        onClick = onGenerateImage,
                        enabled = !isImageGenerating,
                        icon = Icons.Default.AutoAwesome,
                        contentDescription = if (isImageGenerating) {
                            uiText("Generating image", "图片生成中")
                        } else {
                            uiText("Generate image from input", "根据输入生成图片")
                        },
                        active = isImageGenerating
                    )
                    Spacer(Modifier.width(2.dp))
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp, max = 112.dp)
                            .padding(horizontal = 4.dp, vertical = 11.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 4,
                        decorationBox = { innerTextField ->
                            Box {
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = voiceInputPlaceholder(voice, language),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    ChatToolIconButton(
                        onClick = {
                            if (voice.isSpeaking) {
                                onStopSpeaking()
                            } else {
                                onVoiceOutput()
                            }
                        },
                        enabled = voice.isSpeaking || canVoiceOutput,
                        icon = if (voice.isSpeaking) {
                            Icons.Default.Stop
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = if (voice.isSpeaking) uiText("Stop playback", "停止播放") else uiText("Read latest reply", "朗读最近回复"),
                        active = voice.isSpeaking
                    )
                    Spacer(Modifier.width(4.dp))
                    if (isGenerating) {
                        FilledIconButton(
                            onClick = onCancelGeneration,
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = uiText("Cancel generation", "取消生成")
                            )
                        }
                    } else if (inputText.isNotBlank() || selectedImages.isNotEmpty()) {
                        FilledIconButton(
                            onClick = onSend,
                            modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = uiText("Send", "发送")
                            )
                        }
                    } else {
                        VoicePrimaryButton(
                            voice = voice,
                            isGenerating = isGenerating,
                            language = language,
                            onVoiceInput = onVoiceInput
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceInputPreview(
    voice: VoiceFirstInteractionState
) {
    val language = LocalAppLanguage.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    ) {
        Text(
            text = voice.inputPreview.ifBlank { voiceInputPlaceholder(voice, language) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun SelectedImagePreviewRow(
    selectedImages: List<Uri>,
    onRemoveImage: (Uri) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        selectedImages.forEach { uri ->
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = uiText("Selected image", "选中的图片"),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                FilledIconButton(
                    onClick = { onRemoveImage(uri) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(24.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = uiText("Remove image", "移除图片"),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatToolIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    active: Boolean = false
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (active) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun VoicePrimaryButton(
    voice: VoiceFirstInteractionState,
    isGenerating: Boolean,
    language: AppLanguage,
    onVoiceInput: () -> Unit
) {
    val active = voice.isInputActive
    val description = when {
        active -> uiText(language, "Stop voice input", "停止语音输入")
        voice.isAutoSending -> uiText(language, "Sending voice", "正在发送语音")
        isGenerating -> uiText(language, "Generating reply", "正在生成回复")
        else -> uiText(language, "Start voice input", "开始语音输入")
    }

    FilledIconButton(
        onClick = onVoiceInput,
        modifier = Modifier
            .size(44.dp)
            .semantics { contentDescription = description },
        enabled = !voice.isAutoSending && !isGenerating,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentColor = if (active) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimary
            },
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = if (active) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = null
        )
    }
}

private fun voiceInputPlaceholder(
    voice: VoiceFirstInteractionState,
    language: AppLanguage
): String {
    return when {
        voice.isStarting -> uiText(language, "Starting voice recognition...", "正在启动语音识别...")
        voice.isListening -> uiText(language, "Listening...", "正在听...")
        voice.isAutoSending -> uiText(language, "Sending voice...", "正在发送语音...")
        else -> uiText(language, "Type a message...", "输入消息...")
    }
}
