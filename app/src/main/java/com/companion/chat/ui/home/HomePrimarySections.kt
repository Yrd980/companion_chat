package com.companion.chat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.companion.chat.companion.readiness.CompanionReadinessLevel
import com.companion.chat.data.dashboard.HomeDashboardUiState
import com.companion.chat.data.dashboard.HomeMemorySummary
import com.companion.chat.data.dashboard.HomeQuickAction
import com.companion.chat.data.discover.DiscoverRoleCardItem
import com.companion.chat.ui.components.CompanionAvatar
import com.companion.chat.ui.components.ProductCard
import com.companion.chat.ui.components.ProductInnerShape
import com.companion.chat.ui.components.SectionTitle
import com.companion.chat.ui.components.StatusChip
import com.companion.chat.ui.language.AppLanguage
import com.companion.chat.ui.language.LocalAppLanguage
import com.companion.chat.ui.language.uiText

@Composable
fun CompanionHeroCard(
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
fun HelmetReadinessCard(
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
fun ModeSelector() {
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
fun ActionGrid(
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
fun RecentMemories(
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
