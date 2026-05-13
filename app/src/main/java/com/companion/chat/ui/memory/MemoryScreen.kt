package com.companion.chat.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.chat.data.local.entity.Memory
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                title = {
                    Text(
                        text = "记忆管理",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { openEditor(null) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新增记忆"
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            MemoryFilterRow(
                selected = uiState.filter,
                onSelected = memoryViewModel::setFilter
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    EmptyState(
                        title = "正在加载记忆",
                        message = "请稍候..."
                    )
                }

                uiState.memories.isEmpty() -> {
                    EmptyState(
                        title = "还没有记忆",
                        message = "还没有记忆，对话中说“记住...”会自动保存"
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.memories,
                            key = { it.id }
                        ) { memory ->
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
        }
    }

    if (showEditor) {
        MemoryEditorDialog(
            title = if (editingMemory == null) "新增记忆" else "编辑记忆",
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
            title = { Text("删除记忆") },
            text = { Text("确认删除这条记忆吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        memoryViewModel.deleteMemory(memory)
                        deletingMemory = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMemory = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MemoryFilterRow(
    selected: MemoryFilter,
    onSelected: (MemoryFilter) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MemoryFilter.entries.forEach { filter ->
            AssistChip(
                onClick = { onSelected(filter) },
                label = { Text(filterLabel(filter)) },
                leadingIcon = if (filter == selected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = memory.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MemoryTag(
                    text = categoryLabel(memory.category),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
                MemoryTag(
                    text = layerLabel(memory.layer),
                    containerColor = if (memory.layer == "long_term") {
                        Color(0xFFD8F3DC)
                    } else {
                        Color(0xFFFFE8CC)
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "更新时间：${formatTime(memory.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("编辑")
                }
                FilledTonalButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("删除")
                }
                if (memory.layer == "short_term") {
                    FilledTonalButton(onClick = onPromote) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("提升")
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryTag(
    text: String,
    containerColor: Color
) {
    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
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
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text("记忆内容") },
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("fact", "preference", "event", "relation", "time", "other").forEach { item ->
                        AssistChip(
                            onClick = { onCategoryChange(item) },
                            label = { Text(categoryLabel(item)) },
                            leadingIcon = if (item == category) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = content.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EmptyState(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun filterLabel(filter: MemoryFilter): String {
    return when (filter) {
        MemoryFilter.ALL -> "全部"
        MemoryFilter.FACT -> "事实"
        MemoryFilter.PREFERENCE -> "偏好"
        MemoryFilter.EVENT -> "事件"
        MemoryFilter.RELATION -> "关系"
        MemoryFilter.TIME -> "时间"
        MemoryFilter.OTHER -> "其他"
    }
}

private fun categoryLabel(category: String): String {
    return when (category) {
        "fact" -> "事实"
        "preference" -> "偏好"
        "event" -> "事件"
        "relation", "relationship" -> "关系"
        "time" -> "时间"
        "other" -> "其他"
        else -> category
    }
}

private fun layerLabel(layer: String): String {
    return when (layer) {
        "short_term" -> "短期"
        "long_term" -> "长期"
        else -> layer
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
