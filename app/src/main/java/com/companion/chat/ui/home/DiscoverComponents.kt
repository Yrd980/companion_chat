package com.companion.chat.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.companion.chat.data.discover.ContentRating
import com.companion.chat.data.discover.DiscoverRoleCardItem
import com.companion.chat.data.discover.RoleSortMode
import com.companion.chat.ui.components.ProductCard
import com.companion.chat.ui.components.ProductInnerShape
import com.companion.chat.ui.language.LocalAppLanguage
import com.companion.chat.ui.language.uiText

@Composable
fun RoleSortChips(
    selected: RoleSortMode,
    onSelect: (RoleSortMode) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            RoleSortMode.HOT to uiText("Hot", "热门"),
            RoleSortMode.NEWEST to uiText("Newest", "最新"),
            RoleSortMode.NAME to uiText("Name", "名称")
        ).forEach { (mode, label) ->
            FilterChip(selected = selected == mode, onClick = { onSelect(mode) }, label = { Text(label) })
        }
    }
}

@Composable
fun DiscoverControls(
    query: String,
    onQueryChange: (String) -> Unit,
    tags: List<String>,
    selectedTag: String?,
    onTagSelected: (String) -> Unit,
    includeMature: Boolean,
    onIncludeMatureChange: (Boolean) -> Unit,
    onCreateRole: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            placeholder = { Text(uiText("Search roles, authors, or tags", "搜索角色、作者、标签")) },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        ProductCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiText("Create your role", "创建你的角色"), style = MaterialTheme.typography.titleSmall)
                    Text(
                        uiText("Persona, avatar, and voice are saved to a role card.", "人设、头像、语音会保存到角色卡"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onCreateRole) { Text(uiText("Create", "创建")) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(uiText("Show mature", "显示成熟内容"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = includeMature, onCheckedChange = onIncludeMatureChange)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags.size) { index ->
                val tag = tags[index]
                FilterChip(selected = selectedTag == tag, onClick = { onTagSelected(tag) }, label = { Text(tag.displayTag()) })
            }
        }
    }
}

@Composable
fun DiscoverRoleCard(
    item: DiscoverRoleCardItem,
    onOpen: () -> Unit,
    onFavorite: () -> Unit
) {
    val language = LocalAppLanguage.current
    val roleText = item.role.displayText(language)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = ProductInnerShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverBlock(
                name = roleText.name,
                coverImageUri = item.role.coverImageUri,
                contentRating = item.role.contentRating,
                modifier = Modifier
                    .size(88.dp)
                    .clip(ProductInnerShape)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(roleText.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("by ${item.role.author}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(roleText.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (item.collection.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = uiText("Favorite", "收藏"),
                    tint = if (item.collection.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    val language = LocalAppLanguage.current
    val item = uiState.selectedItem
    val roleText = item?.role?.displayText(language)

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
                title = { Text(roleText?.name ?: uiText("Role Details", "角色详情")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = uiText("Back", "返回"))
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
                Text(uiText("Role not found", "未找到角色"))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { RoleHero(item) }
                item {
                    RoleDetailActions(
                        item = item,
                        onFavorite = { viewModel.toggleFavorite(item.role.id) },
                        onStartChat = {
                            viewModel.copyAndActivate(item.role.id, onReady = onStartChat)
                        },
                        onAddOnly = {
                            viewModel.copyToMyCompanions(item.role.id)
                        }
                    )
                }
                item { HorizontalDivider() }
                item {
                    Text(
                        text = roleText?.description ?: item.role.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                item { DetailSection(uiText("Persona Summary", "人设摘要"), roleText?.persona ?: item.role.persona) }
                item { DetailSection(uiText("Voice", "语音"), roleText?.voiceSummary ?: item.role.voiceSummary) }
                item { DetailSection(uiText("Image Style", "图片风格"), item.role.imageStyle.ifBlank { uiText("Not configured", "未配置") }) }
            }
        }
    }
}

@Composable
private fun RoleHero(item: DiscoverRoleCardItem) {
    val language = LocalAppLanguage.current
    val roleText = item.role.displayText(language)
    ProductCard {
        CoverBlock(
            name = roleText.name,
            coverImageUri = item.role.coverImageUri,
            contentRating = item.role.contentRating,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(ProductInnerShape)
        )
        Text(text = roleText.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (item.role.openingMessage.isNotBlank()) {
            Surface(shape = ProductInnerShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                Text(
                    text = "\"${item.role.openingMessage}\"",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(roleText.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    uiText("by ${item.role.author} · Heat ${item.role.heat}", "by ${item.role.author} · 热度 ${item.role.heat}"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.collection.importedRoleCardId != null) {
                ElevatedAssistChip(onClick = {}, label = { Text(uiText("Imported", "已导入")) })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            roleText.tags.forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
        }
    }
}

@Composable
private fun RoleDetailActions(
    item: DiscoverRoleCardItem,
    onFavorite: () -> Unit,
    onStartChat: () -> Unit,
    onAddOnly: () -> Unit
) {
    val imported = item.collection.importedRoleCardId != null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onStartChat, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (imported) uiText("Start chat", "开始聊天") else uiText("Start with this companion", "和这个伙伴开始"))
            }
            OutlinedButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (item.collection.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = uiText("Favorite", "收藏")
                )
            }
        }
        if (!imported) {
            OutlinedButton(onClick = onAddOnly, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(uiText("Add to My Companions", "添加到我的角色"))
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
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.tertiaryContainer
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        if (coverImageUri.isNotBlank()) {
            AsyncImage(
                model = coverImageUri,
                contentDescription = uiText("$name cover", "$name 封面"),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                    text = uiText("Mature", "成熟内容"),
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
            if (title == uiText("Voice", "语音")) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
