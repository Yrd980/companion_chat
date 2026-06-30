package com.companion.chat.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.companion.chat.companion.readiness.CompanionReadinessLevel
import com.companion.chat.data.local.entity.Memory
import com.companion.chat.data.model.ChatMessage
import com.companion.chat.data.model.MessageRole
import com.companion.chat.data.timeline.TimelineEvent
import com.companion.chat.engine.InferenceState
import com.companion.chat.engine.image.ImageGenerationState
import com.companion.chat.ui.chat.components.ChatInputBar
import com.companion.chat.ui.chat.components.ConversationDrawerSheet
import com.companion.chat.ui.chat.components.MessageBubble
import com.companion.chat.ui.components.CompanionAvatar
import com.companion.chat.ui.components.ProductCard
import com.companion.chat.ui.components.ProductInnerShape
import com.companion.chat.ui.components.SectionTitle
import com.companion.chat.ui.components.StatusChip
import com.companion.chat.ui.language.AppLanguage
import com.companion.chat.ui.language.LocalAppLanguage
import com.companion.chat.ui.language.uiText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val language = LocalAppLanguage.current

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
            snackbarHostState.showSnackbar(userFacingVoiceInputError(uiState.voice.inputError, language))
            viewModel.clearVoiceInputError()
        }
    }

    LaunchedEffect(uiState.imageGenerationError) {
        if (uiState.imageGenerationError.isNotBlank()) {
            snackbarHostState.showSnackbar(uiState.imageGenerationError)
        }
    }

    ScrollToLatestMessageEffect(listState = listState, messages = uiState.messages)

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.toggleSessionDrawer() }) {
                        Icon(Icons.Default.Menu, contentDescription = uiText("Conversation list", "对话列表"))
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompanionAvatar(uiState.assistantAvatarImageUri, size = 42.dp)
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = uiState.assistantName.ifBlank { "Anime Companion" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HeadsetMic, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (uiState.engineState is InferenceState.Ready) {
                                    uiText("Text anytime", "随时可以聊")
                                } else {
                                    engineStatusLabel(uiState.engineState, uiState.isConversationWarmingUp, language)
                                },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier.padding(end = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Radio, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(7.dp))
                            Text(uiText("Local", "本地"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
        bottomBar = {
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = listState,
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ChatContextStrip(
                    uiState = uiState,
                    onUseNextTurn = viewModel::useMemoryNextTurn,
                    onClearUseNextTurn = viewModel::clearUseNextTurnMemory
                )
            }
            if (uiState.messages.isEmpty()) {
                item {
                    WelcomeCard(uiState)
                }
            } else {
                items(items = uiState.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        assistantAvatarImageUri = uiState.assistantAvatarImageUri,
                        privacyLabel = localizedPrivacyLabel(uiState)
                    )
                }
            }
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
private fun PinnedMemoriesDeck(
    memories: List<Memory>,
    selectedMemory: Memory?,
    onUseNextTurn: (Long) -> Unit,
    onClearUseNextTurn: () -> Unit
) {
    ProductCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(10.dp))
            SectionTitle(uiText("Pinned Memories", "置顶记忆"), action = uiText("Manage", "管理"), modifier = Modifier.weight(1f))
        }
        if (selectedMemory != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClearUseNextTurn),
                shape = ProductInnerShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(uiText("Memory selected for this turn", "本轮已选择记忆"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(selectedMemory.content, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text(uiText("Clear", "清除"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (memories.isEmpty()) {
            Text(
                uiText(
                    "Pin memories in Memory & Relationship to reuse them during chat.",
                    "在记忆与关系中置顶记忆，聊天时即可复用。"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(memories, key = { it.id }) { memory ->
                    val selected = selectedMemory?.id == memory.id
                    Surface(
                        modifier = Modifier
                            .width(252.dp)
                            .clickable { onUseNextTurn(memory.id) },
                        shape = ProductInnerShape,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Row(modifier = Modifier.padding(10.dp)) {
                            MemoryThumb(Icons.Default.Memory)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(memory.category.ifBlank { uiText("Memory", "记忆") }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(memory.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                StatusChip(
                                    if (selected) uiText("Using next turn", "下轮使用") else uiText("Use next turn", "下轮使用"),
                                    CompanionReadinessLevel.READY
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryThumb(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(ProductInnerShape)
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceContainerHigh)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ChatContextStrip(
    uiState: ChatUiState,
    onUseNextTurn: (Long) -> Unit,
    onClearUseNextTurn: () -> Unit
) {
    ProductCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ContextAction(
                icon = Icons.Default.PushPin,
                title = uiText("Pins", "记忆"),
                value = uiState.useNextTurnMemory?.category?.ifBlank { uiText("Ready", "已选") }
                    ?: uiText("Ready", "可用"),
                modifier = Modifier.weight(1f)
            )
            ContextAction(
                icon = Icons.Default.Share,
                title = uiText("Notes", "记录"),
                value = if (uiState.timelineEvents.isEmpty()) uiText("Calm", "安静") else uiText("Updated", "已更新"),
                modifier = Modifier.weight(1f)
            )
            ContextAction(
                icon = Icons.Default.Person,
                title = uiText("Tone", "语气"),
                value = uiText("Warm", "温暖"),
                modifier = Modifier.weight(1f)
            )
        }
        if (uiState.useNextTurnMemory != null) {
            Text(
                uiState.useNextTurnMemory.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onClearUseNextTurn)
            )
        } else if (uiState.pinnedMemories.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.pinnedMemories, key = { it.id }) { memory ->
                    StatusChip(
                        memory.category.ifBlank { uiText("Use next turn", "下轮使用") },
                        CompanionReadinessLevel.READY,
                        modifier = Modifier.clickable { onUseNextTurn(memory.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextAction(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ConversationTimelineCard(uiState: ChatUiState) {
    ProductCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            SectionTitle(uiText("Conversation Timeline", "对话时间线"), action = uiText("Open Timeline", "打开时间线"), modifier = Modifier.weight(1f))
        }
        if (uiState.timelineEvents.isEmpty()) {
            Text(
                uiText(
                    "Timeline events appear after messages, voice transcripts, images, and memory selections.",
                    "发送消息、语音转写、图片和选择记忆后会显示时间线。"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.timelineEvents, key = { it.id }) { event ->
                    TimelineNode(event)
                }
            }
        }
    }
}

@Composable
private fun TimelineNode(event: TimelineEvent) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = event.type.name.take(1),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Text(localizedTimelineTitle(event), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(formatTimelineTime(event.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WelcomeCard(uiState: ChatUiState) {
    ProductCard {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompanionAvatar(uiState.assistantAvatarImageUri, size = 72.dp)
            Text(
                uiState.assistantName.ifBlank { "Companion" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                uiText(
                    "Start a conversation by typing or using voice input.",
                    "开始对话吧，可以输入文字或使用语音。"
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VoiceDock(
    uiState: ChatUiState,
    onVoiceInput: () -> Unit
) {
    ProductCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clickable(onClick = onVoiceInput),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 3.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = uiText("Push to Talk", "按住说话"),
                    modifier = Modifier.padding(15.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    if (uiState.voice.isInputActive) uiText("Listening", "正在听") else uiText("Tap to talk", "点按说话"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    uiText("Hold the mic when you want to speak.", "想说话时按住麦克风。"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusChip(localizedPrivacyLabel(uiState), CompanionReadinessLevel.READY)
        }
    }
}

@Composable
private fun localizedPrivacyLabel(uiState: ChatUiState): String {
    return if (uiState.localOnlyMode) {
        uiText("Local Only", "仅本地")
    } else {
        uiText("Cloud Optional", "云端可选")
    }
}

@Composable
private fun localizedTimelineTitle(event: TimelineEvent): String {
    return when (event.type) {
        com.companion.chat.data.timeline.TimelineEventType.CHAT -> uiText("Chat", "对话")
        com.companion.chat.data.timeline.TimelineEventType.VOICE_NOTE -> uiText("Voice", "语音")
        com.companion.chat.data.timeline.TimelineEventType.IMAGE_GENERATED -> uiText("Image", "图片")
        com.companion.chat.data.timeline.TimelineEventType.MEMORY_PINNED -> uiText("Memory", "记忆")
        com.companion.chat.data.timeline.TimelineEventType.MEMORY_CREATED -> uiText("Memory", "记忆")
        com.companion.chat.data.timeline.TimelineEventType.PRIVACY_CHANGED -> uiText("Privacy", "隐私")
        com.companion.chat.data.timeline.TimelineEventType.SETUP_CHANGED -> uiText("Setup", "设置")
        com.companion.chat.data.timeline.TimelineEventType.DATA_EXPORTED -> uiText("Export", "导出")
        com.companion.chat.data.timeline.TimelineEventType.LOCAL_DATA_DELETED -> uiText("Delete", "删除")
    }
}

private fun formatTimelineTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@Composable
private fun VoicePersonalityCard() {
    ProductCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            SectionTitle(uiText("Voice Personality", "语音人格"), action = uiText("Edit", "编辑"), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VoiceOption(uiText("Tone", "语气"), uiText("Warm & Friendly", "温暖友好"), Modifier.weight(1f))
            VoiceOption(uiText("Language", "语言"), uiText("English", "中文"), Modifier.weight(1f))
            VoiceOption(uiText("Verbosity", "详略"), uiText("Moderate", "适中"), Modifier.weight(1f))
        }
    }
}

@Composable
private fun VoiceOption(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun engineStatusLabel(
    engineState: InferenceState,
    isConversationWarmingUp: Boolean,
    language: AppLanguage
): String {
    return when (engineState) {
        is InferenceState.Idle -> uiText(language, "Model disconnected", "模型未连接")
        is InferenceState.Initializing -> uiText(language, "Loading model", "模型加载中")
        is InferenceState.Ready -> if (isConversationWarmingUp) {
            uiText(language, "Warming up conversation", "对话预热中")
        } else {
            uiText(language, "Local model ready", "本地模型就绪")
        }
        is InferenceState.Generating -> uiText(language, "Replying", "正在回应")
        is InferenceState.Error -> uiText(language, "Chat setup needed", "聊天还没准备好")
    }
}

private fun userFacingVoiceInputError(error: String, language: AppLanguage): String {
    return when {
        error.contains("SenseVoice", ignoreCase = true) ||
            error.contains("model", ignoreCase = true) ||
            error.contains(".onnx", ignoreCase = true) -> uiText(
            language,
            "Voice input is not ready yet. You can still type.",
            "语音输入还没准备好，可以先打字聊天。"
        )
        else -> error
    }
}

@Composable
private fun ScrollToLatestMessageEffect(
    listState: LazyListState,
    messages: List<ChatMessage>
) {
    val latestMessage = messages.lastOrNull()
    LaunchedEffect(messages.size, latestMessage?.id, latestMessage?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex + 2)
        }
    }
}
