package com.companion.chat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.companion.chat.companion.readiness.CompanionReadinessLevel
import com.companion.chat.data.dashboard.HomeActivitySummary
import com.companion.chat.data.dashboard.HomeDashboardUiState
import com.companion.chat.data.dashboard.HomeMemorySummary
import com.companion.chat.data.dashboard.HomeQuickAction
import com.companion.chat.data.dashboard.HomeSuggestion
import com.companion.chat.data.discover.ContentRating
import com.companion.chat.data.discover.DiscoverRoleCard
import com.companion.chat.data.discover.DiscoverRoleCardItem
import com.companion.chat.data.discover.RoleSortMode
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel(),
    dashboardViewModel: HomeDashboardViewModel = viewModel(),
    onStartChat: () -> Unit = {},
    onOpenHelmet: () -> Unit = {},
    onOpenMemory: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenRole: (String) -> Unit = {},
    onCreateRole: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    val activeItem = uiState.items.firstOrNull { it.collection.importedRoleCardId != null }
        ?: uiState.items.firstOrNull { it.collection.isFavorite }
        ?: uiState.items.firstOrNull()
    var showRoleLibrary by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dashboardViewModel.refresh()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Open profile")
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CompanionChat", style = MaterialTheme.typography.titleLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                uiText("Try voice wake word", "试试语音唤醒词"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Box {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CompanionHeroCard(
                    activeItem = activeItem,
                    language = language,
                    dashboardState = dashboardUiState,
                    onStartChat = onStartChat,
                    onCreateRole = onCreateRole
                )
            }
            item {
                HelmetReadinessCard(
                    dashboardState = dashboardUiState,
                    onOpenHelmet = onOpenHelmet
                )
            }
            item { ModeSelector() }
            item {
                ActionGrid(
                    dashboardState = dashboardUiState,
                    onStartChat = onStartChat,
                    onOpenHelmet = onOpenHelmet,
                    onOpenMemory = onOpenMemory,
                    onOpenProfile = onOpenProfile
                )
            }
            item {
                RecentMemories(
                    memories = dashboardUiState.recentMemories,
                    onOpenMemory = onOpenMemory
                )
            }
            item {
                RecommendedCompanions(
                    items = uiState.items,
                    onOpenRole = onOpenRole,
                    onCreateRole = onCreateRole,
                    expanded = showRoleLibrary,
                    onToggleExpanded = { showRoleLibrary = !showRoleLibrary }
                )
            }
            if (showRoleLibrary) {
                item {
                    RoleLibraryExpanded(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenRole = onOpenRole,
                        onCreateRole = onCreateRole
                    )
                }
            }
            item {
                SuggestionsRow(
                    suggestions = dashboardUiState.suggestions,
                    onOpenHelmet = onOpenHelmet,
                    onOpenMemory = onOpenMemory,
                    onStartChat = onStartChat,
                    onOpenProfile = onOpenProfile
                )
            }
            item {
                ActivityList(
                    activities = dashboardUiState.recentActivity,
                    onOpenHelmet = onOpenHelmet,
                    onOpenMemory = onOpenMemory
                )
            }
        }
    }
}

@Composable
private fun CompanionHeroCard(
    activeItem: DiscoverRoleCardItem?,
    language: AppLanguage,
    dashboardState: HomeDashboardUiState,
    onStartChat: () -> Unit,
    onCreateRole: () -> Unit
) {
    val activeRoleText = activeItem?.role?.displayText(language)
    val relationship = dashboardState.relationship
    val progressPercent = if (relationship.nextLevelXp > 0) {
        ((relationship.xp.toFloat() / relationship.nextLevelXp.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else {
        0
    }
    ProductCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                CompanionAvatar(activeItem?.role?.coverImageUri, size = 92.dp)
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = activeRoleText?.name ?: relationship.companionName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusChip(
                    text = relationship.companionMood,
                    level = if (dashboardState.localDevice.modelReady) {
                        CompanionReadinessLevel.READY
                    } else {
                        CompanionReadinessLevel.DEGRADED
                    }
                )
                Text(
                    text = activeItem?.role?.description
                        ?.let { activeRoleText?.description ?: it }
                        ?: relationship.closenessLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CompanionLevelRing(
                percent = progressPercent,
                label = uiText("Lv. ${relationship.level}", "${relationship.level} 级")
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onStartChat,
                enabled = dashboardState.localDevice.modelReady,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(uiText("Start Chat", "开始聊天"))
            }
            OutlinedButton(
                onClick = onCreateRole,
                modifier = Modifier.height(48.dp)
            ) {
                Text(if (activeItem == null) uiText("Create Role", "创建角色") else uiText("Role Card", "角色卡"))
            }
        }
    }
}

@Composable
private fun CompanionLevelRing(percent: Int, label: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.width(88.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.size(66.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    strokeWidth = 7.dp
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HelmetReadinessCard(
    dashboardState: HomeDashboardUiState,
    onOpenHelmet: () -> Unit
) {
    val localDevice = dashboardState.localDevice
    ProductCard(
        modifier = Modifier.clickable(onClick = onOpenHelmet)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = ProductInnerShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Icon(
                        imageVector = Icons.Default.HeadsetMic,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        uiText("Helmet & Local Readiness", "头盔与本地就绪状态"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (localDevice.noHelmetMode) {
                            uiText("Helmet not connected - local checks available", "头盔未连接 - 可用本地检查")
                        } else {
                            localDevice.statusLabel
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricInline(uiText("Model", "模型"), localDevice.modelReady.readyLabel(), Modifier.weight(1f))
                MetricInline(uiText("Voice", "语音"), localDevice.voiceReady.readyLabel(), Modifier.weight(1f))
                MetricInline(uiText("Image", "图片"), localDevice.imageReady.readyLabel(), Modifier.weight(1f))
            }
            Text(
                text = uiText(
                    "Pairing is skipped in this build. Model, voice, image, and memory remain local.",
                    "此构建暂时跳过配对。模型、语音、图片和记忆保留在本机。"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricInline(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ModeSelector() {
    val language = LocalAppLanguage.current
    val modes = listOf(
        Triple(Icons.Default.Home, uiText(language, "Idle", "待机"), true),
        Triple(Icons.Default.WbSunny, uiText(language, "Active", "活跃"), false),
        Triple(Icons.Default.DirectionsBike, uiText(language, "Driving", "骑行"), false),
        Triple(Icons.Default.Shield, uiText(language, "Sleep-safe", "睡眠安全"), false)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        modes.chunked(2).forEach { rowModes ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowModes.forEach { (icon, label, selected) ->
                    ModeChip(icon = icon, label = label, selected = selected, modifier = Modifier.weight(1f))
                }
                if (rowModes.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ModeChip(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.height(54.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionGrid(
    dashboardState: HomeDashboardUiState,
    onStartChat: () -> Unit,
    onOpenHelmet: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val actions = dashboardState.quickActions.ifEmpty {
        listOf(
            HomeQuickAction("chat", uiText("Start chat", "开始聊天"), uiText("Continue locally", "本地继续")),
            HomeQuickAction("voice", uiText("Voice check", "语音检查"), uiText("Review voice readiness", "检查语音就绪状态")),
            HomeQuickAction("memory", uiText("Review memories", "查看记忆"), uiText("Inspect local memory", "检查本地记忆")),
            HomeQuickAction("image", uiText("Generate image", "生成图片"), uiText("Review image readiness", "检查图片就绪状态"))
        )
    }
    val actionHandlers = mapOf(
        "chat" to onStartChat,
        "voice" to onOpenHelmet,
        "memory" to onOpenMemory,
        "image" to onOpenHelmet
    )
    val actionIcons = mapOf(
        "chat" to Icons.AutoMirrored.Filled.Chat,
        "voice" to Icons.AutoMirrored.Filled.VolumeUp,
        "memory" to Icons.Default.BookmarkBorder,
        "image" to Icons.Default.Image
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.chunked(2).forEachIndexed { rowIndex, rowActions ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowActions.forEach { action ->
                    HomeAction(
                        title = action.title.homeText(),
                        subtitle = if (action.enabled) action.subtitle.homeText()
                        else action.disabledReason.ifBlank { action.subtitle }.homeText(),
                        icon = actionIcons[action.id] ?: Icons.Default.Home,
                        filled = rowIndex == 0 && action.id == "chat",
                        enabled = action.enabled,
                        modifier = Modifier.weight(1f),
                        onClick = actionHandlers[action.id] ?: onOpenProfile
                    )
                }
                if (rowActions.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        HomeAction(
            title = uiText("Emergency SOS", "紧急 SOS"),
            subtitle = uiText("Configure contact first", "请先配置联系人"),
            icon = Icons.Default.Emergency,
            alert = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenProfile
        )
    }
}

@Composable
private fun HomeAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    alert: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val container = when {
        alert -> MaterialTheme.colorScheme.error
        filled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val content = if (alert || filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier
            .height(86.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = ProductInnerShape,
        color = if (enabled) container else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (enabled) content else MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = if (alert || filled) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = if (alert || filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RecentMemories(
    memories: List<HomeMemorySummary>,
    onOpenMemory: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(uiText("Recent Memories", "最近记忆"), action = uiText("View all", "查看全部"))
        if (memories.isEmpty()) {
            ProductCard(modifier = Modifier.clickable(onClick = onOpenMemory)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(uiText("No memories yet", "还没有记忆"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            uiText("Add a local memory to build continuity.", "添加本地记忆来建立连续感。"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(memories.size) { index ->
                    val memory = memories[index]
                    MemoryStoryCard(memory, onOpenMemory)
                }
            }
        }
    }
}

@Composable
private fun MemoryStoryCard(
    memory: HomeMemorySummary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(246.dp)
            .clickable(onClick = onClick),
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(ProductInnerShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(memory.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(memory.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(memory.category, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun RecommendedCompanions(
    items: List<DiscoverRoleCardItem>,
    onOpenRole: (String) -> Unit,
    onCreateRole: () -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(uiText("Recommended Companions", "推荐伙伴"), action = if (expanded) uiText("Hide", "收起") else uiText("See all", "查看全部"), modifier = Modifier.weight(1f))
            Spacer(
                Modifier
                    .width(1.dp)
                    .clickable(onClick = onToggleExpanded)
            )
        }
        if (items.isEmpty()) {
            OutlinedButton(
                onClick = onCreateRole,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(uiText("Create local companion", "创建本地伙伴"))
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items.take(6).size) { index ->
                    val item = items[index]
                    CompactCompanionCard(item = item, onOpen = { onOpenRole(item.role.id) })
                }
            }
        }
    }
}

@Composable
private fun CompactCompanionCard(
    item: DiscoverRoleCardItem,
    onOpen: () -> Unit
) {
    val language = LocalAppLanguage.current
    val roleText = item.role.displayText(language)
    Surface(
        modifier = Modifier
            .width(166.dp)
            .height(84.dp)
            .clickable(onClick = onOpen),
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompanionAvatar(item.role.coverImageUri, size = 54.dp)
            Column(
                modifier = Modifier.padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(roleText.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                StatusChip(
                    text = roleText.tags.firstOrNull() ?: uiText("Kind", "温和"),
                    level = CompanionReadinessLevel.READY
                )
            }
        }
    }
}

@Composable
private fun SuggestionsRow(
    suggestions: List<HomeSuggestion>,
    onOpenHelmet: () -> Unit,
    onOpenMemory: () -> Unit,
    onStartChat: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val fallbackSuggestions = listOf(
        HomeSuggestion("privacy", uiText("Review local-only privacy controls.", "查看本地优先隐私控制。"), "profile"),
        HomeSuggestion("first_memory", uiText("Add a memory so your companion can keep continuity.", "添加一条记忆，让陪伴保持连续。"), "memory"),
        HomeSuggestion("voice_setup", uiText("Check local voice input and output readiness.", "检查本地语音输入和输出状态。"), "settings/voice")
    )
    val rows = suggestions.ifEmpty { fallbackSuggestions }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(uiText("Suggestions for You", "给你的建议"))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rows.size) { index ->
                val suggestion = rows[index]
                val action = when (suggestion.routeHint) {
                    "memory" -> onOpenMemory
                    "profile" -> onOpenProfile
                    "settings/model", "settings/voice" -> onOpenHelmet
                    else -> onStartChat
                }
                Surface(
                    modifier = Modifier
                        .width(218.dp)
                        .height(74.dp)
                        .clickable(onClick = action),
                    shape = ProductInnerShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                suggestionIcon(suggestion.id),
                                contentDescription = null,
                                modifier = Modifier.padding(9.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = suggestion.text.homeText(),
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityList(
    activities: List<HomeActivitySummary>,
    onOpenHelmet: () -> Unit,
    onOpenMemory: () -> Unit
) {
    ProductCard {
        SectionTitle(uiText("Recent Activity", "最近动态"), action = uiText("View all", "查看全部"))
        if (activities.isEmpty()) {
            ActivityRow(
                Icons.Default.HeadsetMic,
                uiText("Helmet", "头盔"),
                uiText("Helmet not connected. Local diagnostics are available.", "头盔未连接，可查看本地诊断。"),
                uiText("Now", "现在"),
                onOpenHelmet
            )
            ActivityRow(
                Icons.Default.Memory,
                uiText("Memory", "记忆"),
                uiText("Local memories will appear here after you save them.", "保存本地记忆后会显示在这里。"),
                uiText("Ready", "待命"),
                onOpenMemory
            )
        } else {
            activities.forEach { activity ->
                ActivityRow(
                    Icons.AutoMirrored.Filled.Chat,
                    activity.title.homeText(),
                    activity.detail.homeText(),
                    activity.timestampLabel.homeText(),
                    onOpenMemory
                )
            }
        }
    }
}

@Composable
private fun ActivityRow(
    icon: ImageVector,
    title: String,
    body: String,
    time: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            title,
            modifier = Modifier
                .width(84.dp)
                .padding(start = 12.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            body,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RoleLibraryExpanded(
    uiState: DiscoverUiState,
    viewModel: DiscoverViewModel,
    onOpenRole: (String) -> Unit,
    onCreateRole: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        RoleSortChips(selected = uiState.sortMode, onSelect = viewModel::setSortMode)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            uiState.items.forEach { item ->
                DiscoverRoleCard(
                    item = item,
                    onOpen = { onOpenRole(item.role.id) },
                    onFavorite = { viewModel.toggleFavorite(item.role.id) }
                )
            }
        }
    }
}

@Composable
private fun RoleSortChips(
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
                        isGeneratingImage = uiState.isGeneratingImage,
                        onFavorite = { viewModel.toggleFavorite(item.role.id) },
                        onUnlock = { viewModel.unlock(item.role.id) },
                        onGenerateImage = { viewModel.generateRoleImage(item.role.id) },
                        onStartChat = {
                            viewModel.copyAndActivate(item.role.id, onReady = onStartChat)
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
                    Text(uiText("Persona, avatar, and voice are saved to a role card.", "人设、头像、语音会保存到角色卡"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onCreateRole) { Text(uiText("Create", "创建")) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(uiText("Show private roles", "显示私密"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
private fun DiscoverRoleCard(
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(roleText.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(uiText("by ${item.role.author} · Heat ${item.role.heat}", "by ${item.role.author} · 热度 ${item.role.heat}"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.collection.importedRoleCardId != null) {
                ElevatedAssistChip(onClick = {}, label = { Text(uiText("Imported", "已导入")) })
            } else if (item.collection.isUnlocked) {
                ElevatedAssistChip(onClick = {}, label = { Text(uiText("Unlocked", "已解锁")) })
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
    isGeneratingImage: Boolean,
    onFavorite: () -> Unit,
    onUnlock: () -> Unit,
    onGenerateImage: () -> Unit,
    onStartChat: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onStartChat, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(uiText("Start Chat", "开始聊天"))
            }
            OutlinedButton(onClick = onFavorite) {
                Icon(imageVector = if (item.collection.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onUnlock, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (item.collection.isUnlocked) uiText("Unlocked", "已解锁") else uiText("Favorite to Unlock", "收藏解锁"))
            }
            OutlinedButton(onClick = onGenerateImage, enabled = !isGeneratingImage, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isGeneratingImage) uiText("Generating", "生成中") else uiText("Generate Image", "生成图片"))
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
                    text = uiText("Private", "私密"),
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
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class RoleDisplayText(
    val name: String,
    val tags: List<String>,
    val description: String,
    val persona: String,
    val voiceSummary: String
)

@Composable
private fun Boolean.readyLabel(): String {
    return if (this) uiText("Ready", "就绪") else uiText("Needs setup", "需要设置")
}

@Composable
private fun String.homeText(): String {
    val language = LocalAppLanguage.current
    if (language == AppLanguage.ENGLISH) return this
    return when (this) {
        "Start chat", "Start Chat" -> "开始聊天"
        "Continue locally" -> "本地继续"
        "Set up the text model first" -> "先设置文本模型"
        "Text model package is not ready" -> "文本模型包未就绪"
        "Voice check" -> "语音检查"
        "Voice input and output are usable" -> "语音输入和输出可用"
        "Review voice setup" -> "检查语音设置"
        "Voice readiness needs attention" -> "语音就绪状态需要处理"
        "Review memories" -> "查看记忆"
        "Inspect local companion memory" -> "检查本地陪伴记忆"
        "Generate image" -> "生成图片"
        "Image generation is ready" -> "图片生成已就绪"
        "Review image model setup" -> "检查图片模型设置"
        "Image generation is not ready" -> "图片生成未就绪"
        "Finish local text model setup." -> "完成本地文本模型设置。"
        "Check local voice input and output readiness." -> "检查本地语音输入和输出状态。"
        "Add a memory so your companion can keep continuity." -> "添加一条记忆，让陪伴保持连续。"
        "Review local-only privacy controls." -> "查看本地优先隐私控制。"
        "Just now" -> "刚刚"
        "Earlier" -> "更早"
        else -> when {
            endsWith("m ago") -> "${removeSuffix("m ago")} 分钟前"
            endsWith("h ago") -> "${removeSuffix("h ago")} 小时前"
            endsWith("d ago") -> "${removeSuffix("d ago")} 天前"
            else -> this
        }
    }
}

private fun suggestionIcon(id: String): ImageVector {
    return when (id) {
        "model_setup" -> Icons.Default.Memory
        "voice_setup" -> Icons.AutoMirrored.Filled.VolumeUp
        "first_memory" -> Icons.Default.BookmarkBorder
        "privacy" -> Icons.Default.Shield
        else -> Icons.Default.WbSunny
    }
}

private fun DiscoverRoleCard.displayText(language: AppLanguage): RoleDisplayText {
    if (language == AppLanguage.CHINESE) {
        return RoleDisplayText(
            name = name,
            tags = tags,
            description = description,
            persona = persona,
            voiceSummary = voiceSummary
        )
    }

    return when (id) {
        "xia-urban" -> RoleDisplayText(
            name = "Xia",
            tags = listOf("Female", "Romance", "Daily", "Chinese"),
            description = "A warm daily companion who keeps casual conversations light without being clingy.",
            persona = "Xia is gentle, perceptive, and respects boundaries. She remembers preferences, listens more than lectures, and responds naturally.",
            voiceSummary = "Xiaoyu sweet voice, MOSS local synthesis first, system TTS fallback"
        )
        "chen-nocturne" -> RoleDisplayText(
            name = "Chen",
            tags = listOf("Male", "Drama", "Calm", "Chinese"),
            description = "A restrained, reliable night-radio companion for long talks and reflection.",
            persona = "Chen is calm, reliable, and observant. He respects personal space while helping the user sort through emotions and plans.",
            voiceSummary = "Low male voice, system TTS fallback"
        )
        "mira-adventure" -> RoleDisplayText(
            name = name,
            tags = listOf("Female", "Adventure", "English", "Drama"),
            description = "A lively adventure partner for roleplay, English practice, and travel-style conversation.",
            persona = persona,
            voiceSummary = voiceSummary
        )
        "rin-mature" -> RoleDisplayText(
            name = "Rin",
            tags = listOf("Female", "Romance", "Mature", "Private"),
            description = "A more mature, direct intimacy companion whose content boundaries stay in local settings.",
            persona = "Rin is mature, direct, and careful with private boundaries. She prioritizes consent and builds closeness with restraint.",
            voiceSummary = "Clone placeholder, system TTS fallback"
        )
        "niko-anime" -> RoleDisplayText(
            name = name,
            tags = listOf("Anime", "Relaxed", "Adventure", "Chinese"),
            description = "A bright but not noisy anime partner who breaks tasks down and enjoys imaginative play.",
            persona = "Niko is bright, quick, and mindful of the user's pace. He breaks pressure into small steps and can shift into light fantasy chat.",
            voiceSummary = "Lively system TTS"
        )
        else -> RoleDisplayText(
            name = name,
            tags = tags.map { it.displayTag(AppLanguage.ENGLISH) },
            description = description,
            persona = persona,
            voiceSummary = voiceSummary
        )
    }
}

@Composable
private fun String.displayTag(): String {
    return displayTag(LocalAppLanguage.current)
}

private fun String.displayTag(language: AppLanguage): String {
    if (language == AppLanguage.CHINESE) return this
    return when (this) {
        "男性" -> "Male"
        "女性" -> "Female"
        "二次元" -> "Anime"
        "恋爱" -> "Romance"
        "冒险" -> "Adventure"
        "剧情" -> "Drama"
        "英语" -> "English"
        "中文" -> "Chinese"
        "日常" -> "Daily"
        "冷静" -> "Calm"
        "成熟" -> "Mature"
        "私密" -> "Private"
        "轻松" -> "Relaxed"
        else -> this
    }
}
