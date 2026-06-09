package com.companion.chat.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.companion.chat.engine.InferenceState
import com.companion.chat.engine.image.ImageGenerationState
import com.companion.chat.ui.chat.components.ChatInputBar
import com.companion.chat.ui.chat.components.ConversationDrawerSheet
import com.companion.chat.ui.chat.components.MessageBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris: List<Uri> ->
        uris.forEach { viewModel.addImage(it) }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onVoicePermissionGranted()
        } else {
            viewModel.onVoicePermissionDenied()
        }
    }

    LaunchedEffect(uiState.voice.showPermissionDialog) {
        if (uiState.voice.showPermissionDialog) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                viewModel.onVoicePermissionGranted()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(uiState.voice.inputError) {
        if (uiState.voice.inputError.isNotBlank()) {
            snackbarHostState.showSnackbar(uiState.voice.inputError)
            viewModel.clearVoiceInputError()
        }
    }

    LaunchedEffect(uiState.imageGenerationError) {
        if (uiState.imageGenerationError.isNotBlank()) {
            snackbarHostState.showSnackbar(uiState.imageGenerationError)
        }
    }

    ScrollToLatestMessageEffect(
        listState = listState,
        messages = uiState.messages
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.toggleSessionDrawer() }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "对话列表",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "陪伴",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = engineStatusLabel(uiState.engineState, uiState.isConversationWarmingUp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (uiState.engineState) {
                                is InferenceState.Ready -> MaterialTheme.colorScheme.primary
                                is InferenceState.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
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
        ) {
            CompanionHubHeader(
                uiState = uiState,
                onOpenSessions = viewModel::toggleSessionDrawer,
                onVoiceInput = viewModel::toggleVoiceListening,
                onSpeakLatest = viewModel::speakLatestAssistantMessage
            )
            // 消息列表
            if (uiState.currentSessionId.isBlank() && uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "直接输入第一条消息，或从左上角打开会话列表。",
                        modifier = Modifier.padding(horizontal = 32.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            assistantAvatarImageUri = uiState.assistantAvatarImageUri
                        )
                    }

                }
            }

            // 输入栏
            ChatInputBar(
                inputText = uiState.inputText,
                onInputChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                onCancelGeneration = viewModel::cancelGeneration,
                onPickImage = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onGenerateImage = {
                    viewModel.generateChatSceneImage(uiState.inputText.trim())
                },
                onVoiceInput = viewModel::toggleVoiceListening,
                selectedImages = uiState.selectedImages,
                onRemoveImage = viewModel::removeImage,
                voice = uiState.voice,
                isGenerating = uiState.isGenerating,
                isImageGenerating = uiState.imageGenerationState is ImageGenerationState.Generating,
                canVoiceOutput = uiState.hasSpeakableAssistantMessage,
                onVoiceOutput = viewModel::speakLatestAssistantMessage,
                onStopSpeaking = viewModel::stopSpeaking
            )
        }
    }

    if (uiState.showSessionDrawer) {
        ConversationDrawerSheet(
            sessions = uiState.sessions,
            currentSessionId = uiState.currentSessionId,
            searchQuery = uiState.sessionSearchQuery,
            onSearchQueryChange = viewModel::updateSessionSearchQuery,
            onNewConversation = viewModel::createNewSession,
            onSessionClick = { sessionId -> viewModel.switchToSession(sessionId) },
            onDismiss = viewModel::closeSessionDrawer,
            dateFilter = uiState.dateFilter,
            onDateFilterChange = viewModel::setDateFilter,
            editingSessionId = uiState.editingSessionId,
            editingTitle = uiState.editingTitle,
            onStartEditing = viewModel::startEditingTitle,
            onDeleteSession = viewModel::deleteSession,
            onEditingTitleChange = viewModel::updateEditingTitle,
            onConfirmEditing = viewModel::confirmEditingTitle,
            onCancelEditing = viewModel::cancelEditingTitle
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompanionHubHeader(
    uiState: ChatUiState,
    onOpenSessions: () -> Unit,
    onVoiceInput: () -> Unit,
    onSpeakLatest: () -> Unit
) {
    val companionName = uiState.assistantName.ifBlank { "Anime Companion" }
    val mood = uiState.assistantMood.ifBlank { "本地私密陪伴 · 记忆由你掌控" }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompanionAvatar(
                    imageUri = uiState.assistantAvatarImageUri,
                    name = companionName
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = companionName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = mood,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = onVoiceInput) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "语音陪伴"
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompanionStatusChip(
                    label = engineStatusLabel(uiState.engineState, uiState.isConversationWarmingUp),
                    emphatic = uiState.engineState is InferenceState.Ready
                )
                CompanionStatusChip(
                    label = if (uiState.voice.isSpeaking) "正在说话" else "语音待命",
                    emphatic = uiState.voice.isSpeaking || uiState.voice.isInputActive
                )
                CompanionStatusChip(
                    label = "${uiState.messages.count { !it.isStreaming }} 条对话",
                    emphatic = false
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompanionQuickAction(
                    icon = Icons.Default.Memory,
                    title = "会话时间线",
                    subtitle = if (uiState.sessions.isEmpty()) "还没有保存的会话" else "${uiState.sessions.size} 个会话",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSessions
                )
                CompanionQuickAction(
                    icon = Icons.Default.AutoAwesome,
                    title = "最近回复",
                    subtitle = if (uiState.hasSpeakableAssistantMessage) "可再次朗读" else "等待第一条回复",
                    modifier = Modifier.weight(1f),
                    onClick = onSpeakLatest
                )
            }
        }
    }
}

@Composable
private fun CompanionAvatar(
    imageUri: String,
    name: String
) {
    Surface(
        modifier = Modifier.size(58.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (imageUri.isNotBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = "$name 头像",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun CompanionStatusChip(
    label: String,
    emphatic: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (emphatic) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (emphatic) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CompanionQuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun engineStatusLabel(
    engineState: InferenceState,
    isConversationWarmingUp: Boolean
): String {
    return when (engineState) {
        is InferenceState.Idle -> "模型未连接"
        is InferenceState.Initializing -> "模型加载中"
        is InferenceState.Ready -> if (isConversationWarmingUp) "对话预热中" else "本地模型就绪"
        is InferenceState.Generating -> "正在回应"
        is InferenceState.Error -> "需要配置模型"
    }
}

@Composable
private fun ScrollToLatestMessageEffect(
    listState: LazyListState,
    messages: List<com.companion.chat.data.model.ChatMessage>
) {
    val latestMessage = messages.lastOrNull()
    LaunchedEffect(messages.size, latestMessage?.id, latestMessage?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
}
