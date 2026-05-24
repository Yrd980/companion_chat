package com.companion.chat.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

    LaunchedEffect(uiState.showVoicePermissionDialog) {
        if (uiState.showVoicePermissionDialog) {
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

    LaunchedEffect(uiState.voiceInputError) {
        if (uiState.voiceInputError.isNotBlank()) {
            snackbarHostState.showSnackbar(uiState.voiceInputError)
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
                            text = "对话",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val engineLabel = when (val s = uiState.engineState) {
                            is InferenceState.Idle -> "未连接"
                            is InferenceState.Initializing -> "模型加载中"
                            is InferenceState.Ready -> "已就绪"
                            is InferenceState.Generating -> "生成中"
                            is InferenceState.Error -> "错误"
                        }
                        Text(
                            text = engineLabel,
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
                isVoiceStarting = uiState.isVoiceStarting,
                isVoiceListening = uiState.isVoiceListening,
                isVoiceAutoSending = uiState.isVoiceAutoSending,
                isGenerating = uiState.isGenerating,
                isImageGenerating = uiState.imageGenerationState is ImageGenerationState.Generating,
                isVoiceSpeaking = uiState.isVoiceSpeaking,
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
