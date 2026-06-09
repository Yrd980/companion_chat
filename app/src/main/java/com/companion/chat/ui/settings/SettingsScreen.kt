package com.companion.chat.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.companion.chat.CompanionChatApplication
import com.companion.chat.appContainer
import com.companion.chat.companion.readiness.CapabilityReadiness
import com.companion.chat.companion.readiness.CompanionCapability
import com.companion.chat.companion.readiness.CompanionReadinessLevel
import com.companion.chat.context.ContextConfigRepository

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
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "运行状态") {
                readinessSnapshot.capabilities.forEachIndexed { index, readiness ->
                    ReadinessSettingsItem(readiness = readiness)
                    if (index != readinessSnapshot.capabilities.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }

            SettingsSection(title = "角色") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "角色管理",
                    subtitle = "创建和切换陪伴角色卡",
                    onClick = onNavigateToCharacter
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    icon = Icons.Default.Psychology,
                    title = "陪伴模式",
                    subtitle = "切换翻译、创作、情绪整理等对话模式",
                    onClick = onNavigateToSkills
                )
            }

            SettingsSection(title = "记忆") {
                SettingsItem(
                    icon = Icons.Default.Psychology,
                    title = "记忆管理",
                    subtitle = "查看、编辑和提升短期记忆",
                    onClick = onNavigateToMemory
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsToggleItem(
                    icon = Icons.Default.Psychology,
                    title = "自动学习偏好",
                    subtitle = if (autoPreferenceLearningEnabled) {
                        "后台总结最近对话并逐步学习用户偏好"
                    } else {
                        "已关闭后台偏好总结，不会自动触发阶段四学习"
                    },
                    checked = autoPreferenceLearningEnabled,
                    onCheckedChange = { enabled ->
                        autoPreferenceLearningEnabled = enabled
                        contextConfigRepository.updateAutoPreferenceLearningEnabled(enabled)
                    }
                )
            }

            SettingsSection(title = "模型") {
                SettingsItem(
                    icon = Icons.Default.Memory,
                    title = "模型配置",
                    subtitle = "本地模型、上下文与高级推理参数",
                    onClick = onNavigateToModel
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    icon = Icons.Default.Memory,
                    title = "上下文窗口大小",
                    subtitle = "当前保留最近 $retainedRounds 轮对话",
                    onClick = onNavigateToModel
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    icon = Icons.Default.Photo,
                    title = "图片生成",
                    subtitle = "配置本地或 HTTP 图片生成",
                    onClick = onNavigateToModel
                )
            }

            SettingsSection(title = "语音") {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "语音设置",
                    subtitle = "语音输入输出、语速语调",
                    onClick = onNavigateToVoice
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "语言",
                    subtitle = "中文",
                    onClick = onNavigateToLanguage
                )
            }

            SettingsSection(title = "外观") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "深色模式",
                    subtitle = "跟随系统",
                    onClick = onNavigateToDarkMode
                )
            }

            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "关于",
                    subtitle = "版本 0.1.0",
                    onClick = onNavigateToAbout
                )
            }
        }
    }
}

@Composable
private fun ReadinessSettingsItem(readiness: CapabilityReadiness) {
    val color = when (readiness.level) {
        CompanionReadinessLevel.READY -> MaterialTheme.colorScheme.primary
        CompanionReadinessLevel.DEGRADED -> MaterialTheme.colorScheme.tertiary
        CompanionReadinessLevel.NOT_READY -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = RoundedCornerShape(8.dp),
            color = color
        ) {}
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = readiness.capability.displayName(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${readiness.provider} · ${readiness.summary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun CompanionCapability.displayName(): String {
    return when (this) {
        CompanionCapability.LLM -> "本地大模型"
        CompanionCapability.ASR -> "语音识别"
        CompanionCapability.TTS -> "语音输出"
        CompanionCapability.IMAGE -> "图片生成"
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 1.dp
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingsToggleItem(
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
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
