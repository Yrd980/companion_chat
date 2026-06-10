package com.companion.chat.ui.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.chat.companion.readiness.CompanionReadinessLevel
import com.companion.chat.data.local.entity.Memory
import com.companion.chat.ui.components.CompanionAvatar
import com.companion.chat.ui.components.MetricTile
import com.companion.chat.ui.components.ProductCard
import com.companion.chat.ui.components.ProductInnerShape
import com.companion.chat.ui.components.ProductProgress
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
fun MemoryScreen(
    modifier: Modifier = Modifier,
    memoryViewModel: MemoryViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by memoryViewModel.uiState.collectAsStateWithLifecycle()
    var editingMemory by remember { mutableStateOf<Memory?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var draftContent by remember { mutableStateOf("") }
    var draftCategory by remember { mutableStateOf("fact") }
    var deletingMemory by remember { mutableStateOf<Memory?>(null) }

    DisposableEffect(lifecycleOwner, memoryViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                memoryViewModel.loadMemories()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openEditor(memory: Memory?) {
        editingMemory = memory
        draftContent = memory?.content.orEmpty()
        draftCategory = memory?.category ?: "fact"
        showEditor = true
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = uiText("Back", "返回"))
                    }
                },
                title = {
                    Text(
                        uiText("Memory", "记忆"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { openEditor(null) }) {
                        Icon(Icons.Default.Add, contentDescription = uiText("Add memory", "新增记忆"))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.FilterList, contentDescription = uiText("Filters", "筛选"))
                    }
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
            item { RelationshipHero(uiState.memories) }
            item { ReviewQueueCard() }
            item { PinnedMemoriesCard(uiState.memories, onEdit = { openEditor(it) }) }
            item { RelationshipTimelineCard(uiState.memories) }
            item { LearnedPreferencesCard(uiState.memories) }
            item { LocalOnlyStorageCard() }
            item {
                MemoryFilterRow(selected = uiState.filter, onSelected = memoryViewModel::setFilter)
            }
            when {
                uiState.isLoading -> item {
                    EmptyState(title = uiText("Loading memories", "正在加载记忆"), message = uiText("Please wait...", "请稍候..."))
                }
                uiState.memories.isEmpty() -> item {
                    EmptyState(
                        title = uiText("No memories yet", "还没有记忆"),
                        message = uiText(
                            "Say \"remember...\" in chat, or add one from the top-right button.",
                            "在对话里说“记住...”，或点右上角新增一条。"
                        )
                    )
                }
                else -> items(items = uiState.memories, key = { it.id }) { memory ->
                    MemoryCard(
                        memory = memory,
                        onEdit = { openEditor(memory) },
                        onDelete = { deletingMemory = memory },
                        onPromote = { memoryViewModel.promoteMemory(memory.id) }
                    )
                }
            }
        }
    }

    if (showEditor) {
        MemoryEditorDialog(
            title = if (editingMemory == null) uiText("Add Memory", "新增记忆") else uiText("Edit Memory", "编辑记忆"),
            content = draftContent,
            category = draftCategory,
            onContentChange = { draftContent = it },
            onCategoryChange = { draftCategory = it },
            onDismiss = { showEditor = false },
            onConfirm = {
                if (editingMemory == null) {
                    memoryViewModel.addMemory(draftContent, draftCategory)
                } else {
                    memoryViewModel.updateMemory(
                        memoryId = editingMemory!!.id,
                        content = draftContent,
                        category = draftCategory
                    )
                }
                showEditor = false
            }
        )
    }

    deletingMemory?.let { memory ->
        AlertDialog(
            onDismissRequest = { deletingMemory = null },
            title = { Text(uiText("Delete Memory", "删除记忆")) },
            text = { Text(uiText("Delete this memory?", "确认删除这条记忆吗？")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        memoryViewModel.deleteMemory(memory)
                        deletingMemory = null
                    }
                ) {
                    Text(uiText("Delete", "删除"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMemory = null }) { Text(uiText("Cancel", "取消")) }
            }
        )
    }
}

@Composable
private fun RelationshipHero(memories: List<Memory>) {
    val pinnedCount = memories.count { it.referenceCount > 0 }
    val preferenceCount = memories.count { it.category == "preference" }
    val latest = memories.maxByOrNull { it.updatedAt }
    val progress = (memories.size.coerceAtMost(1248) / 1248f).coerceAtLeast(0.12f)

    ProductCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompanionAvatar(null, size = 96.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Aiko Hoshizora",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusChip(uiText("Active", "使用中"), CompanionReadinessLevel.READY)
                Text(uiText("Relationship Level", "关系等级"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        uiText("Lv. ${23 + memories.size.coerceAtMost(7)}", "Lv. ${23 + memories.size.coerceAtMost(7)}"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    uiText("Close Companion", "亲密伙伴"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ProductProgress(progress = progress)
            }
        }
        MemoryMetricPanel(
            memories = memories.size.toString(),
            pinned = pinnedCount.toString(),
            accuracy = if (memories.isEmpty()) "N/A" else "92%",
            capacity = "${(progress * 100).toInt()}%"
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(7.dp))
            Text(
                latest?.let { uiText("Last memory update  ${formatTime(it.updatedAt)}", "最近记忆更新  ${formatTime(it.updatedAt)}") }
                    ?: uiText("No confirmed memory yet", "还没有已确认记忆"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricText(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MemoryMetricPanel(
    memories: String,
    pinned: String,
    accuracy: String,
    capacity: String
) {
    Surface(
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                MetricText(uiText("Memories", "记忆"), memories, Modifier.weight(1f))
                MetricText(uiText("Pinned", "置顶"), pinned, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricText(uiText("Accuracy", "准确率"), accuracy, Modifier.weight(1f))
                MetricText(uiText("Capacity", "容量"), capacity, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReviewQueueCard() {
    val rows = listOf(
        Triple(Icons.Default.Mic, uiText("Voice Session · Today, 9:58 AM", "语音会话 · 今天 9:58"), uiText("You mentioned wanting to try a sunset ride route.", "你提到想尝试一条落日骑行路线。")),
        Triple(Icons.Default.Memory, uiText("Chat · Today, 9:31 AM", "聊天 · 今天 9:31"), uiText("You said you prefer lo-fi music during rides.", "你说骑行时更喜欢 lo-fi 音乐。")),
        Triple(Icons.Default.Mic, uiText("Voice Session · Today, 9:12 AM", "语音会话 · 今天 9:12"), uiText("You mentioned your cat's name is Mochi.", "你提到你的猫叫 Mochi。"))
    )
    ProductCard {
        SectionTitle(uiText("Review Queue", "待审核队列"), action = uiText("Review all", "全部审核"))
        rows.forEachIndexed { index, item ->
            val (icon, meta, body) = item
            ReviewRow(icon = icon, meta = meta, body = body, confidence = listOf("92%", "88%", "95%")[index])
        }
    }
}

@Composable
private fun ReviewRow(icon: ImageVector, meta: String, body: String, confidence: String) {
    Surface(
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(body, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    StatusChip(uiText("High", "高"), CompanionReadinessLevel.READY)
                    Text(confidence, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(uiText("Keep", "保留"), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Text(uiText("Edit", "编辑"), style = MaterialTheme.typography.labelMedium)
                Text(uiText("Delete", "删除"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                Text(uiText("Pin", "置顶"), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PinnedMemoriesCard(memories: List<Memory>, onEdit: (Memory) -> Unit) {
    val pinned = memories.filter { it.referenceCount > 0 }.ifEmpty { memories.take(3) }
    ProductCard {
        SectionTitle(uiText("Pinned Memories", "置顶记忆"), action = uiText("View all (${pinned.size})", "查看全部 (${pinned.size})"))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (pinned.isEmpty()) {
                item {
                    EmptyMiniCard(uiText("No pinned memories yet", "还没有置顶记忆"), uiText("Pin memories from conversations before reusing them.", "从对话中置顶记忆后可复用。"))
                }
            } else {
                items(pinned, key = { it.id }) { memory ->
                    Surface(
                        modifier = Modifier
                            .width(270.dp)
                            .clickable { onEdit(memory) },
                        shape = ProductInnerShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row {
                                MemoryIllustration(Icons.Default.PushPin)
                                Column(modifier = Modifier.padding(start = 10.dp)) {
                                    Text(memory.content, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                    Text(uiText("Pinned · ${shortDate(memory.updatedAt)}", "置顶 · ${shortDate(memory.updatedAt)}"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MiniButton(Icons.Default.PlayArrow, uiText("Play", "播放"))
                                MiniButton(Icons.Default.KeyboardArrowUp, uiText("Use Next Turn", "下轮使用"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipTimelineCard(memories: List<Memory>) {
    ProductCard {
        SectionTitle(uiText("Relationship Timeline", "关系时间线"), action = uiText("View full timeline", "查看完整时间线"))
        val rows = memories.take(4).ifEmpty {
            listOf(
                Memory(content = uiText("You talked about planning a mountain ride this weekend.", "你聊到这个周末计划山路骑行。"), category = "event", layer = "short_term", source = "voice", createdAt = 0L, updatedAt = System.currentTimeMillis()),
                Memory(content = uiText("Scenic coastal road at golden hour.", "黄金时刻的海岸风景路。"), category = "event", layer = "short_term", source = "camera", createdAt = 0L, updatedAt = System.currentTimeMillis() - 86_400_000L),
                Memory(content = uiText("You set Aiko's role to Ride Buddy.", "你把 Aiko 的角色设为骑行伙伴。"), category = "relation", layer = "long_term", source = "manual", createdAt = 0L, updatedAt = System.currentTimeMillis() - 92_000_000L)
            )
        }
        rows.forEachIndexed { index, memory ->
            TimelineRow(
                icon = when {
                    memory.source.contains("voice", true) -> Icons.Default.Mic
                    memory.category == "relation" -> Icons.Default.Person
                    else -> Icons.Default.Memory
                },
                time = if (index == 0) "10:21 AM" else shortDate(memory.updatedAt),
                title = when (memory.category) {
                    "relation" -> uiText("Role Change", "角色变更")
                    "preference" -> uiText("Preference Learned", "已学习偏好")
                    "event" -> uiText("Saved Ride Moment", "已保存骑行瞬间")
                    else -> uiText("Conversation Marker", "对话标记")
                },
                body = memory.content
            )
        }
    }
}

@Composable
private fun TimelineRow(icon: ImageVector, time: String, title: String, body: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(time, modifier = Modifier.width(82.dp).padding(start = 12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, modifier = Modifier.width(140.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(body, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LearnedPreferencesCard(memories: List<Memory>) {
    val preferences = memories.filter { it.category == "preference" }.take(4)
    val language = LocalAppLanguage.current
    ProductCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(uiText("Learned Preferences", "已学习偏好"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(uiText("All ${preferences.size}", "全部 ${preferences.size}"), CompanionReadinessLevel.READY)
                StatusChip(uiText("Confirmed ${preferences.size}", "已确认 ${preferences.size}"), CompanionReadinessLevel.READY)
                StatusChip(uiText("Unconfirmed 3", "未确认 3"), CompanionReadinessLevel.DEGRADED)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val shown = preferences.ifEmpty {
                listOf(
                    Memory(content = uiText(language, "Prefers lo-fi music on most rides", "多数骑行时偏好 lo-fi 音乐"), category = "preference", layer = "long_term", source = "model", createdAt = 0L, updatedAt = System.currentTimeMillis()),
                    Memory(content = uiText(language, "Enjoys sunset and night rides", "喜欢落日和夜间骑行"), category = "preference", layer = "long_term", source = "model", createdAt = 0L, updatedAt = System.currentTimeMillis()),
                    Memory(content = uiText(language, "Interested in astrophotography", "对天文摄影感兴趣"), category = "preference", layer = "short_term", source = "model", createdAt = 0L, updatedAt = System.currentTimeMillis())
                )
            }
            items(shown, key = { it.id.takeIf { id -> id != 0L } ?: it.content.hashCode().toLong() }) { memory ->
                Surface(
                    modifier = Modifier.width(230.dp),
                    shape = ProductInnerShape,
                    color = if (memory.layer == "long_term") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(memory.content, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text(
                                "${if (memory.layer == "long_term") uiText("Confirmed", "已确认") else uiText("Unconfirmed", "未确认")} · ${shortDate(memory.updatedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            MiniTextAction(uiText("Approve", "批准"))
            MiniTextAction(uiText("Edit", "编辑"))
            MiniTextAction(uiText("Disable", "禁用"))
        }
    }
}

@Composable
private fun LocalOnlyStorageCard() {
    ProductCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(58.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.padding(14.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(uiText("Local-Only Storage", "仅本地存储"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(uiText("All memories are stored securely on your device. No data leaves your helmet.", "所有记忆都安全存储在本设备上，数据不会离开你的头盔。"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(uiText("Cloud Sync  Disabled", "云同步  已关闭"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                StatusChip(uiText("100% Local", "100% 本地"), CompanionReadinessLevel.READY)
            }
        }
    }
}

@Composable
private fun MemoryFilterRow(
    selected: MemoryFilter,
    onSelected: (MemoryFilter) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(MemoryFilter.entries) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(filterLabel(filter)) },
                leadingIcon = if (filter == selected) {
                    {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun MemoryCard(
    memory: Memory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPromote: () -> Unit
) {
    ProductCard {
        Text(memory.content, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MemoryTag(categoryLabel(memory.category), emphatic = true)
            MemoryTag(layerLabel(memory.layer), emphatic = memory.layer == "long_term")
            MemoryTag(sourceLabel(memory.source), emphatic = false)
            if (memory.referenceCount > 0) MemoryTag(uiText("Pinned ${memory.referenceCount}", "置顶 ${memory.referenceCount}"), emphatic = true)
        }
        Text(uiText("Updated: ${formatTime(memory.updatedAt)}", "更新时间：${formatTime(memory.updatedAt)}"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FilledTonalIconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = uiText("Edit memory", "编辑记忆")) }
            Spacer(modifier = Modifier.size(8.dp))
            FilledTonalIconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = uiText("Delete memory", "删除记忆")) }
            if (memory.layer == "short_term") {
                Spacer(modifier = Modifier.size(8.dp))
                FilledTonalIconButton(onClick = onPromote) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = uiText("Promote to long-term memory", "提升为长期记忆")) }
            }
        }
    }
}

@Composable
private fun MemoryTag(text: String, emphatic: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (emphatic) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (emphatic) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MemoryEditorDialog(
    title: String,
    content: String,
    category: String,
    onContentChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = content, onValueChange = onContentChange, label = { Text(uiText("Memory content", "记忆内容")) }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("fact", "preference", "event", "relation", "time", "other").forEach { item ->
                        AssistChip(
                            onClick = { onCategoryChange(item) },
                            label = { Text(categoryLabel(item)) },
                            leadingIcon = if (item == category) {
                                { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = content.isNotBlank()) { Text(uiText("Save", "保存")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(uiText("Cancel", "取消")) }
        }
    )
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EmptyMiniCard(title: String, body: String) {
    Surface(
        modifier = Modifier.width(270.dp),
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MemoryIllustration(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(74.dp)
            .clip(ProductInnerShape)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant))),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun MiniButton(icon: ImageVector, label: String) {
    Surface(
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MiniTextAction(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun filterLabel(filter: MemoryFilter): String {
    return when (filter) {
        MemoryFilter.ALL -> uiText("All", "全部")
        MemoryFilter.FACT -> uiText("Facts", "事实")
        MemoryFilter.PREFERENCE -> uiText("Preferences", "偏好")
        MemoryFilter.EVENT -> uiText("Events", "事件")
        MemoryFilter.RELATION -> uiText("Relationships", "关系")
        MemoryFilter.TIME -> uiText("Time", "时间")
        MemoryFilter.OTHER -> uiText("Other", "其他")
    }
}

@Composable
private fun categoryLabel(category: String): String {
    return when (category) {
        "fact" -> uiText("Fact", "事实")
        "preference" -> uiText("Preference", "偏好")
        "event" -> uiText("Event", "事件")
        "relation", "relationship" -> uiText("Relationship", "关系")
        "time" -> uiText("Time", "时间")
        "other" -> uiText("Other", "其他")
        else -> category
    }
}

@Composable
private fun layerLabel(layer: String): String {
    return when (layer) {
        "short_term" -> uiText("Short-term", "短期")
        "long_term" -> uiText("Long-term", "长期")
        else -> layer
    }
}

@Composable
private fun sourceLabel(source: String): String {
    return when (source) {
        "manual" -> uiText("Manual", "手动")
        "rule" -> uiText("Rule", "规则")
        "model" -> uiText("Model extracted", "模型提取")
        else -> source.ifBlank { uiText("Unknown source", "未知来源") }
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun shortDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
}
