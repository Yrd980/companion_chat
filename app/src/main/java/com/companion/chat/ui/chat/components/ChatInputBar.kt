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
                        contentDescription = "上传图片"
                    )
                    ChatToolIconButton(
                        onClick = onGenerateImage,
                        enabled = !isImageGenerating,
                        icon = Icons.Default.AutoAwesome,
                        contentDescription = if (isImageGenerating) "图片生成中" else "根据输入生成图片",
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
                                        text = voice.inputPlaceholder,
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
                        contentDescription = if (voice.isSpeaking) "停止播放" else "朗读最近回复",
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
                                contentDescription = "取消生成"
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
                                contentDescription = "发送"
                            )
                        }
                    } else {
                        VoicePrimaryButton(
                            voice = voice,
                            isGenerating = isGenerating,
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
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    ) {
        Text(
            text = voice.inputPreview.ifBlank { voice.inputPlaceholder },
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
                    contentDescription = "选中的图片",
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
                        contentDescription = "移除图片",
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
    onVoiceInput: () -> Unit
) {
    val active = voice.isInputActive
    val description = when {
        active -> "停止语音输入"
        voice.isAutoSending -> "正在发送语音"
        isGenerating -> "正在生成回复"
        else -> "开始语音输入"
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
