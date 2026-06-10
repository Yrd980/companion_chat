package com.companion.chat.ui.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.companion.chat.CompanionChatApplication
import com.companion.chat.appContainer
import com.companion.chat.companion.readiness.CapabilityReadiness
import com.companion.chat.companion.readiness.CompanionCapability
import com.companion.chat.companion.readiness.CompanionReadinessLevel
import com.companion.chat.context.ContextConfigRepository
import com.companion.chat.ui.components.CompanionAvatar
import com.companion.chat.ui.components.ProductCard
import com.companion.chat.ui.components.ProductInnerShape
import com.companion.chat.ui.components.SectionTitle
import com.companion.chat.ui.components.StatusChip
import com.companion.chat.ui.language.LocalAppLanguage
import com.companion.chat.ui.language.uiLabel
import com.companion.chat.ui.language.uiProvider
import com.companion.chat.ui.language.uiSummary
import com.companion.chat.ui.language.uiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToCharacter: () -> Unit = {},
    onNavigateToSkills: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {},
    onNavigateToModel: () -> Unit = {},
    onNavigateToVoice: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToDarkMode: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    val contextConfigRepository = remember(context) { ContextConfigRepository(context) }
    val readinessRepository = remember(context) {
        (context.applicationContext as CompanionChatApplication).appContainer.companionReadinessRepository
    }
    val readinessSnapshot = readinessRepository.getSnapshot()
    var retainedRounds by remember { mutableIntStateOf(contextConfigRepository.getSettings().retainedRounds) }
    var autoPreferenceLearningEnabled by remember {
        mutableStateOf(contextConfigRepository.getAutoPreferenceLearningEnabled())
    }

    LaunchedEffect(Unit) {
        retainedRounds = contextConfigRepository.getSettings().retainedRounds
        autoPreferenceLearningEnabled = contextConfigRepository.getAutoPreferenceLearningEnabled()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateToCharacter) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = uiText("Back", "返回"))
                    }
                },
                title = {
                    Text(uiText("Profile", "个人"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Default.MoreVert, contentDescription = uiText("More", "更多"))
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
            item {
                ProfileSummaryCard(
                    onNavigateToCharacter = onNavigateToCharacter,
                    readiness = readinessSnapshot.asr
                )
            }
            item {
                PlanCard(onNavigateToAbout = onNavigateToAbout)
            }
            item {
                PrivacyControlsCard(
                    autoPreferenceLearningEnabled = autoPreferenceLearningEnabled,
                    onAutoPreferenceLearningChange = { enabled ->
                        autoPreferenceLearningEnabled = enabled
                        contextConfigRepository.updateAutoPreferenceLearningEnabled(enabled)
                    },
                    onNavigateToVoice = onNavigateToVoice,
                    onNavigateToModel = onNavigateToModel
                )
            }
            item {
                DataOwnershipCard(onNavigateToMemory = onNavigateToMemory)
            }
            item {
                EmergencyContactsCard()
            }
            item {
                AdvancedCard(
                    retainedRounds = retainedRounds,
                    onNavigateToModel = onNavigateToModel,
                    onNavigateToVoice = onNavigateToVoice,
                    onNavigateToLanguage = onNavigateToLanguage,
                    onNavigateToDarkMode = onNavigateToDarkMode,
                    onNavigateToSkills = onNavigateToSkills
                )
            }
            item {
                RuntimeReadinessCard(readinessSnapshot.capabilities)
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    onNavigateToCharacter: () -> Unit,
    readiness: CapabilityReadiness
) {
    ProductCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompanionAvatar(null, size = 96.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        uiText("Your Name", "你的名字"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        modifier = Modifier.clickable(onClick = onNavigateToCharacter),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = uiText("Edit profile", "编辑资料"), modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileMetric(Icons.Default.Star, uiText("Plan", "方案"), uiText("Premium", "高级版"), Modifier.weight(1f))
            ProfileMetric(Icons.Default.Person, uiText("Role", "角色"), "Aiko", Modifier.weight(1f))
            ProfileMetric(Icons.Default.HeadsetMic, uiText("Helmet", "头盔"), if (readiness.isUsable) uiText("Online", "在线") else uiText("Offline", "离线"), Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(46.dp), shape = ProductInnerShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.padding(11.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(uiText("Local-first", "本地优先"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(uiText("Your data stays on this device", "你的数据保留在本设备"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProfileMetric(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(7.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlanCard(modifier: Modifier = Modifier, onNavigateToAbout: () -> Unit) {
    ProductCard(modifier = modifier) {
        SectionTitle(uiText("Plan & Entitlement", "方案与权益"))
        SettingsRow(Icons.Default.Upgrade, uiText("Current Plan", "当前方案"), "CompanionChat Premium", uiText("Premium", "高级版"), onNavigateToAbout)
        SettingsRow(Icons.Default.CalendarMonth, uiText("Renews On", "续费日期"), uiText("May 28, 2025", "2025 年 5 月 28 日"), uiText("In 24 days", "24 天后"), onNavigateToAbout)
        SettingsRow(Icons.AutoMirrored.Filled.VolumeUp, uiText("Premium Voices", "高级语音"), uiText("High quality companion voices", "高质量陪伴声线"), uiText("Included", "已包含"), onNavigateToAbout)
        SettingsRow(Icons.Default.Cloud, uiText("Cloud Backup", "云备份"), uiText("Encrypted, optional", "加密，可选"), uiText("Off", "关闭"), onNavigateToAbout)
        SettingsRow(Icons.Default.Memory, uiText("Local Alternatives", "本地替代方案"), uiText("All core features available offline", "核心功能可离线使用"), uiText("Available", "可用"), onNavigateToAbout)
    }
}

@Composable
private fun PrivacyControlsCard(
    modifier: Modifier = Modifier,
    autoPreferenceLearningEnabled: Boolean,
    onAutoPreferenceLearningChange: (Boolean) -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToModel: () -> Unit
) {
    ProductCard(modifier = modifier) {
        SectionTitle(uiText("Privacy Controls", "隐私控制"))
        ToggleRow(Icons.Default.Mic, uiText("Voice Recording", "语音录制"), uiText("device only", "仅设备端"), true, {})
        ToggleRow(Icons.Default.Analytics, uiText("Usage Analytics", "使用分析"), uiText("anonymous diagnostics", "匿名诊断"), false, {})
        ToggleRow(Icons.Default.Groups, uiText("Partner Sharing", "伙伴共享"), uiText("shared with companion", "与伙伴共享"), false, {})
        ToggleRow(Icons.Default.Cloud, uiText("Cloud ASR", "云 ASR"), uiText("cloud optional", "云端可选"), false, { onNavigateToVoice() })
        ToggleRow(Icons.Default.Settings, uiText("HTTP Voice Clone", "HTTP 语音克隆"), uiText("server request only", "仅服务器请求"), false, { onNavigateToVoice() })
        ToggleRow(Icons.Default.Image, uiText("HTTP Image Generation", "HTTP 图片生成"), uiText("server request only", "仅服务器请求"), false, { onNavigateToModel() })
        ToggleRow(Icons.Default.Psychology, uiText("Memory Learning", "记忆学习"), uiText("local memory model", "本地记忆模型"), autoPreferenceLearningEnabled, onAutoPreferenceLearningChange)
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                uiText(
                    "You're in control. Data stays on your device unless you turn on a cloud or server feature.",
                    "你始终拥有控制权。除非开启云端或服务器功能，数据都会保留在本设备。"
                ),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DataOwnershipCard(modifier: Modifier = Modifier, onNavigateToMemory: () -> Unit) {
    ProductCard(modifier = modifier) {
        SectionTitle(uiText("Data Ownership", "数据所有权"))
        SettingsRow(Icons.Default.Description, uiText("Export Memories", "导出记忆"), uiText("Export local memory database", "导出本地记忆数据库"), null, onNavigateToMemory)
        SettingsRow(Icons.Default.Description, uiText("Export Conversations", "导出对话"), uiText("Export chat history", "导出聊天历史"), null, onNavigateToMemory)
        SettingsRow(Icons.Default.Description, uiText("Export Role Cards", "导出角色卡"), uiText("Export companion role cards", "导出陪伴角色卡"), null, onNavigateToMemory)
        SettingsRow(Icons.Default.Delete, uiText("Delete Local Data", "删除本地数据"), uiText("Permanently delete local data", "永久删除本地数据"), null, onNavigateToMemory, danger = true)
    }
}

@Composable
private fun EmergencyContactsCard(modifier: Modifier = Modifier) {
    ProductCard(modifier = modifier) {
        SectionTitle(uiText("Emergency Contacts", "紧急联系人"))
        SettingsRow(Icons.Default.Emergency, uiText("SOS Available", "SOS 可用"), uiText("Long press helmet button", "长按头盔按钮"), uiText("Enabled", "已启用"), {})
        SettingsRow(Icons.Default.Notifications, uiText("Impact Detection Notification", "碰撞检测通知"), uiText("Alerts will be sent to your contacts", "警报会发送给联系人"), uiText("On", "开启"), {})
        SettingsRow(Icons.Default.Send, uiText("Test Contact", "测试联系人"), uiText("Send a test SOS message", "发送测试 SOS 消息"), null, {})
    }
}

@Composable
private fun AdvancedCard(
    retainedRounds: Int,
    onNavigateToModel: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToDarkMode: () -> Unit,
    onNavigateToSkills: () -> Unit
) {
    ProductCard {
        SectionTitle(uiText("Advanced", "高级"))
        Text(uiText("Developer and troubleshooting options", "开发者和故障排查选项"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AdvancedTile(Icons.Default.Folder, uiText("Model Package Paths", "模型包路径"), "/storage/emulated/0/CompanionChat/models", Modifier.fillMaxWidth(), onNavigateToModel)
            AdvancedTile(Icons.Default.Language, uiText("Cloud Endpoint Templates", "云端 Endpoint 模板"), "https://api.companionchat.ai", Modifier.fillMaxWidth(), onNavigateToVoice)
            AdvancedTile(Icons.Default.Settings, uiText("Backend Diagnostics", "后端诊断"), uiText("Logs, network tests, and system info", "日志、网络测试和系统信息"), Modifier.fillMaxWidth(), onNavigateToModel)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(uiText("Context $retainedRounds turns", "上下文 $retainedRounds 轮"), CompanionReadinessLevel.DEGRADED)
            SurfaceChip(uiText("Language", "语言"), onNavigateToLanguage)
            SurfaceChip(uiText("Appearance", "外观"), onNavigateToDarkMode)
            SurfaceChip(uiText("Companion modes", "陪伴模式"), onNavigateToSkills)
        }
    }
}

@Composable
private fun RuntimeReadinessCard(readiness: List<CapabilityReadiness>) {
    ProductCard {
        SectionTitle(uiText("Runtime Readiness", "运行状态"))
        readiness.forEachIndexed { index, item ->
            RuntimeRow(item)
            if (index != readiness.lastIndex) HorizontalDivider()
        }
    }
}

@Composable
private fun RuntimeRow(readiness: CapabilityReadiness) {
    val language = LocalAppLanguage.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = readiness.level.color()) {}
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(readiness.capability.uiLabel(language), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("${readiness.uiProvider(language)} · ${readiness.uiSummary(language)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusChip(readiness.level.uiLabel(language), readiness.level)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String?,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) {
            val positiveValues = listOf(
                uiText("Premium", "高级版"),
                uiText("Available", "可用"),
                uiText("Enabled", "已启用"),
                uiText("On", "开启"),
                uiText("Included", "已包含")
            )
            Text(value, style = MaterialTheme.typography.labelLarge, color = if (value in positiveValues) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(25.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AdvancedTile(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(86.dp)
            .clickable(onClick = onClick),
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

@Composable
private fun SurfaceChip(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CompanionReadinessLevel.color() = when (this) {
    CompanionReadinessLevel.READY -> MaterialTheme.colorScheme.primary
    CompanionReadinessLevel.DEGRADED -> MaterialTheme.colorScheme.tertiary
    CompanionReadinessLevel.NOT_READY -> MaterialTheme.colorScheme.error
}
