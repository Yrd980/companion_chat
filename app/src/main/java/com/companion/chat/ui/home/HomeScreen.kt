package com.companion.chat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.companion.chat.data.discover.ContentRating
import com.companion.chat.data.discover.DiscoverRoleCardItem
import com.companion.chat.data.discover.RoleSortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel(),
    onOpenRole: (String) -> Unit = {},
    onCreateRole: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sortMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("发现") },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "排序")
                        }
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false }
                        ) {
                            SortMenuItem("热门", RoleSortMode.HOT, uiState.sortMode) {
                                sortMenuOpen = false
                                viewModel.setSortMode(it)
                            }
                            SortMenuItem("最新", RoleSortMode.NEWEST, uiState.sortMode) {
                                sortMenuOpen = false
                                viewModel.setSortMode(it)
                            }
                            SortMenuItem("名称", RoleSortMode.NAME, uiState.sortMode) {
                                sortMenuOpen = false
                                viewModel.setSortMode(it)
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DiscoverControls(
                query = uiState.query,
                onQueryChange = viewModel::updateQuery,
                tags = uiState.tags,
                selectedTag = uiState.selectedTag,
                onTagSelected = viewModel::selectTag,
                includeMature = uiState.includeMature,
                onIncludeMatureChange = viewModel::setIncludeMature,
                onCreateRole = onCreateRole
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 164.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.items, key = { it.role.id }) { item ->
                    DiscoverRoleCard(
                        item = item,
                        onOpen = { onOpenRole(item.role.id) },
                        onFavorite = { viewModel.toggleFavorite(item.role.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverRoleDetailScreen(
    roleId: String,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel(),
    onBack: () -> Unit = {},
    onStartChat: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val item = uiState.selectedItem

    LaunchedEffect(roleId) {
        viewModel.selectRole(roleId)
    }
    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotBlank()) {
            snackbarHostState.showSnackbar(uiState.message)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(item?.role?.name ?: "角色详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("未找到角色")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RoleHero(item)
                RoleDetailActions(
                    item = item,
                    isGeneratingImage = uiState.isGeneratingImage,
                    onFavorite = { viewModel.toggleFavorite(item.role.id) },
                    onUnlock = { viewModel.unlock(item.role.id) },
                    onGenerateImage = { viewModel.generateRoleImage(item.role.id) },
                    onStartChat = {
                        viewModel.copyAndActivate(item.role.id, onReady = onStartChat)
                    }
                )
                HorizontalDivider()
                Text(
                    text = item.role.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                DetailSection("人设摘要", item.role.persona)
                DetailSection("语音", item.role.voiceSummary)
                DetailSection("图片风格", item.role.imageStyle.ifBlank { "未配置" })
            }
        }
    }
}

@Composable
private fun DiscoverControls(
    query: String,
    onQueryChange: (String) -> Unit,
    tags: List<String>,
    selectedTag: String?,
    onTagSelected: (String) -> Unit,
    includeMature: Boolean,
    onIncludeMatureChange: (Boolean) -> Unit,
    onCreateRole: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            placeholder = { Text("搜索角色、作者、标签") },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("创建你的角色", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "人设、头像、语音会保存到角色卡",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onCreateRole) {
                    Text("创建")
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "显示私密",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = includeMature, onCheckedChange = onIncludeMatureChange)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags.size) { index ->
                val tag = tags[index]
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { onTagSelected(tag) },
                    label = { Text(tag) }
                )
            }
        }
    }
}

@Composable
private fun DiscoverRoleCard(
    item: DiscoverRoleCardItem,
    onOpen: () -> Unit,
    onFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            CoverBlock(
                name = item.role.name,
                coverImageUri = item.role.coverImageUri,
                contentRating = item.role.contentRating,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.86f)
            )
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.role.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (item.collection.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (item.collection.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "by ${item.role.author}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.role.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.role.tags.take(3).forEach { tag ->
                        AssistChip(
                            onClick = onOpen,
                            label = { Text(tag) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleHero(item: DiscoverRoleCardItem) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CoverBlock(
            name = item.role.name,
            coverImageUri = item.role.coverImageUri,
            contentRating = item.role.contentRating,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.role.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "by ${item.role.author} · 热度 ${item.role.heat}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.collection.importedRoleCardId != null) {
                ElevatedAssistChip(onClick = {}, label = { Text("已导入") })
            } else if (item.collection.isUnlocked) {
                ElevatedAssistChip(onClick = {}, label = { Text("已解锁") })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item.role.tags.forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
        }
    }
}

@Composable
private fun RoleDetailActions(
    item: DiscoverRoleCardItem,
    isGeneratingImage: Boolean,
    onFavorite: () -> Unit,
    onUnlock: () -> Unit,
    onGenerateImage: () -> Unit,
    onStartChat: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onStartChat,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("开始聊天")
            }
            OutlinedButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (item.collection.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onUnlock,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (item.collection.isUnlocked) "已解锁" else "收藏解锁")
            }
            OutlinedButton(
                onClick = onGenerateImage,
                enabled = !isGeneratingImage,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isGeneratingImage) "生成中" else "生成图片")
            }
        }
    }
}

@Composable
private fun CoverBlock(
    name: String,
    coverImageUri: String,
    contentRating: ContentRating,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant
    )
    Box(
        modifier = modifier.background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        if (coverImageUri.isNotBlank()) {
            AsyncImage(
                model = coverImageUri,
                contentDescription = "$name 封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        if (contentRating == ContentRating.MATURE) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = "私密",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (title == "语音") {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SortMenuItem(
    label: String,
    mode: RoleSortMode,
    selected: RoleSortMode,
    onSelect: (RoleSortMode) -> Unit
) {
    DropdownMenuItem(
        text = { Text(if (selected == mode) "$label ✓" else label) },
        onClick = { onSelect(mode) }
    )
}
