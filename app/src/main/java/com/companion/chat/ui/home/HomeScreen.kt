package com.companion.chat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.chat.data.dashboard.HomeActivitySummary
import com.companion.chat.data.dashboard.HomeSuggestion
import com.companion.chat.data.discover.ContentRating
import com.companion.chat.data.discover.DiscoverRoleCard
import com.companion.chat.data.discover.DiscoverRoleCardItem
import com.companion.chat.ui.components.CompanionAvatar
import com.companion.chat.ui.components.ProductCard
import com.companion.chat.ui.components.ProductInnerShape
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
    onOpenDiscover: () -> Unit = {},
    onOpenRole: (String) -> Unit = {},
    onCreateRole: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    val activeItem = uiState.items.firstOrNull { it.collection.importedRoleCardId != null }
        ?: uiState.items.firstOrNull { it.collection.isFavorite }
        ?: uiState.items.firstOrNull()

    LaunchedEffect(Unit) {
        dashboardViewModel.refresh()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Default.MoreVert, contentDescription = uiText("Open profile", "打开个人页"))
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
                            Icon(Icons.Default.Notifications, contentDescription = uiText("Notifications", "通知"))
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
            item { HelmetReadinessCard(dashboardState = dashboardUiState, onOpenHelmet = onOpenHelmet) }
            item { DiscoverCompanionsSection(uiState.items, onOpenRole, onOpenDiscover, onCreateRole) }
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
            item { RecentMemories(memories = dashboardUiState.recentMemories, onOpenMemory = onOpenMemory) }
            item {
                SuggestionsRow(
                    suggestions = dashboardUiState.suggestions,
                    onOpenHelmet = onOpenHelmet,
                    onOpenMemory = onOpenMemory,
                    onStartChat = onStartChat,
                    onOpenProfile = onOpenProfile
                )
            }
            item { ActivityList(dashboardUiState.recentActivity, onOpenHelmet, onOpenMemory) }
        }
    }
}

@Composable
private fun DiscoverCompanionsSection(
    items: List<DiscoverRoleCardItem>,
    onOpenRole: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onCreateRole: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(
                title = uiText("Discover companions", "发现伙伴"),
                action = uiText("View market", "查看市场"),
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenDiscover)
            )
        }
        val safeItems = items.filter { it.role.contentRating != ContentRating.MATURE }.take(3)
        if (safeItems.isEmpty()) {
            OutlinedButton(
                onClick = onCreateRole,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(uiText("Create local companion", "创建本地伙伴"))
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(safeItems.size) { index ->
                    val item = safeItems[index]
                    CompactCompanionCard(item = item, onOpen = { onOpenRole(item.role.id) })
                }
                item { ViewMarketCard(onClick = onOpenDiscover) }
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
            .height(96.dp)
            .clickable(onClick = onOpen),
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompanionAvatar(item.role.coverImageUri, size = 58.dp)
            Column(
                modifier = Modifier.padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(roleText.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                StatusChip(
                    text = roleText.tags.firstOrNull() ?: uiText("Kind", "温和"),
                    level = com.companion.chat.companion.readiness.CompanionReadinessLevel.READY
                )
                Text(roleText.description, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ViewMarketCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(150.dp)
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(uiText("View all", "查看全部"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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

data class RoleDisplayText(
    val name: String,
    val tags: List<String>,
    val description: String,
    val persona: String,
    val voiceSummary: String
)

@Composable
fun Boolean.readyLabel(): String {
    return if (this) uiText("Ready", "就绪") else uiText("Needs setup", "需要设置")
}

@Composable
fun String.homeText(): String {
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

fun DiscoverRoleCard.displayText(language: AppLanguage): RoleDisplayText {
    if (language == AppLanguage.CHINESE) {
        return RoleDisplayText(name, tags, description, persona, voiceSummary)
    }
    return RoleDisplayText(
        name = name,
        tags = tags.map { it.displayTag(AppLanguage.ENGLISH) },
        description = description,
        persona = persona,
        voiceSummary = voiceSummary
    )
}

@Composable
fun String.displayTag(): String {
    return displayTag(LocalAppLanguage.current)
}

fun String.displayTag(language: AppLanguage): String {
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
        "巫女" -> "Shrine"
        "治愈" -> "Soothing"
        "魔法使" -> "Witch"
        "复盘" -> "Review"
        "月兔" -> "Moon Rabbit"
        "练习" -> "Practice"
        "妖怪" -> "Yokai"
        "花灵" -> "Flower Spirit"
        "边界" -> "Boundaries"
        "鸦天狗" -> "Crow Tengu"
        "行动" -> "Action"
        "冷静" -> "Calm"
        "成熟" -> "Mature"
        "私密" -> "Private"
        "轻松" -> "Relaxed"
        else -> this
    }
}
