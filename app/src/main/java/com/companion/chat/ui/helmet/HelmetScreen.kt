package com.companion.chat.ui.helmet

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.companion.chat.companion.readiness.CapabilityReadiness
import com.companion.chat.companion.readiness.CompanionReadinessLevel
import com.companion.chat.companion.readiness.CompanionReadinessSnapshot
import com.companion.chat.ui.components.MetricTile
import com.companion.chat.ui.components.ProductCard
import com.companion.chat.ui.components.ProductInnerShape
import com.companion.chat.ui.components.ProductProgress
import com.companion.chat.ui.components.SectionTitle
import com.companion.chat.ui.components.StatusChip
import com.companion.chat.ui.language.LocalAppLanguage
import com.companion.chat.ui.language.uiLabel
import com.companion.chat.ui.language.uiProvider
import com.companion.chat.ui.language.uiSummary
import com.companion.chat.ui.language.uiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelmetScreen(
    readinessSnapshot: CompanionReadinessSnapshot,
    modifier: Modifier = Modifier,
    onOpenModelSettings: () -> Unit = {},
    onOpenVoiceSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = uiText("Back", "返回"))
                    }
                },
                title = {
                    Text(uiText("Helmet Control & Diagnostics", "头盔控制与诊断"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onOpenProfile) {
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
            item { HelmetHero(readinessSnapshot, onOpenProfile) }
            item { RuntimeReadinessGrid(readinessSnapshot, onOpenModelSettings, onOpenVoiceSettings) }
            item { TemperatureAndSensors() }
            item { ControlsCard(onOpenVoiceSettings) }
            item { SafetyModesCard(onOpenProfile) }
            item { DiagnosticsCard(onOpenModelSettings) }
        }
    }
}

@Composable
private fun HelmetHero(
    readinessSnapshot: CompanionReadinessSnapshot,
    onOpenProfile: () -> Unit
) {
    ProductCard {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HelmetProductIllustration(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(0.48f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.48f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HardwareMetric(Icons.Default.BatteryChargingFull, uiText("Battery", "电量"), "84%", uiText("Charging", "充电中"))
                HardwareMetric(Icons.Default.Speed, uiText("Est. Runtime", "预计续航"), "12h 45m", null)
                HardwareMetric(Icons.Default.Bluetooth, uiText("Connection", "连接"), uiText("BLE Strong", "BLE 信号强"), null)
                HardwareMetric(Icons.Default.SystemUpdate, uiText("Firmware", "固件"), "v2.4.1", null)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.48f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(uiText("Recent Firmware Changes", "最近固件变更"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FirmwareBullet(uiText("Improved voice recognition accuracy", "提升语音识别准确率"))
                FirmwareBullet(uiText("Reduced power usage in standby", "降低待机功耗"))
                FirmwareBullet(uiText("Enhanced impact detection sensitivity", "增强碰撞检测灵敏度"))
                Text(uiText("View Full Changelog", "查看完整更新日志"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onOpenProfile,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(if (readinessSnapshot.isReadyForVoiceFirstTurn) uiText("Manage pairing", "管理配对") else uiText("Set up pairing", "设置配对"))
            }
            OutlinedButton(
                onClick = onOpenProfile,
                modifier = Modifier.height(48.dp)
            ) {
                Text(uiText("Manual code", "手动代码"))
            }
        }
    }
}

@Composable
private fun HelmetProductIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(118.dp),
            shape = RoundedCornerShape(topStart = 70.dp, topEnd = 22.dp, bottomStart = 24.dp, bottomEnd = 56.dp),
            color = MaterialTheme.colorScheme.onSurface
        ) {}
        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 42.dp)
                .size(76.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.Center) {
                Text("CompanionChat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(uiText("Hello!", "你好！"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                ProductProgress(progress = 0.72f, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 54.dp)
                .size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.HeadsetMic, contentDescription = null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun HardwareMetric(icon: ImageVector, label: String, value: String, status: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (status != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun FirmwareBullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RuntimeReadinessGrid(
    readinessSnapshot: CompanionReadinessSnapshot,
    onOpenModelSettings: () -> Unit,
    onOpenVoiceSettings: () -> Unit
) {
    ProductCard {
        SectionTitle(uiText("Model and voice readiness", "模型和语音就绪状态"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReadinessMiniCard(readinessSnapshot.llm, Modifier.weight(1f), onOpenModelSettings)
            ReadinessMiniCard(readinessSnapshot.asr, Modifier.weight(1f), onOpenVoiceSettings)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReadinessMiniCard(readinessSnapshot.tts, Modifier.weight(1f), onOpenVoiceSettings)
            ReadinessMiniCard(readinessSnapshot.image, Modifier.weight(1f), onOpenModelSettings)
        }
    }
}

@Composable
private fun ReadinessMiniCard(
    readiness: CapabilityReadiness,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val language = LocalAppLanguage.current
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(readiness.level.color())
                )
                Spacer(Modifier.width(8.dp))
                Text(readiness.uiProvider(language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(readiness.uiSummary(language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            StatusChip(readiness.level.uiLabel(language), readiness.level)
        }
    }
}

@Composable
private fun TemperatureAndSensors() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProductCard(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Thermostat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(uiText("Temperature", "温度"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("32.6", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(5.dp))
                        Text("°C", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(10.dp))
                        StatusChip(uiText("Normal", "正常"), CompanionReadinessLevel.READY)
                    }
                    Text(uiText("Optimal operating range", "最佳工作范围"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("15°C - 45°C", style = MaterialTheme.typography.bodyMedium)
                }
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { 0.72f },
                    modifier = Modifier.size(72.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    strokeWidth = 8.dp
                )
            }
        }
        ProductCard(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sensors, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(uiText("Sensors", "传感器"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            listOf(
                uiText("Accelerometer", "加速度计"),
                uiText("Gyroscope", "陀螺仪"),
                uiText("Proximity", "接近传感器"),
                uiText("Microphone", "麦克风")
            ).forEach {
                SensorStatusRow(it)
            }
        }
    }
}

@Composable
private fun SensorStatusRow(label: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        StatusChip(uiText("OK", "正常"), CompanionReadinessLevel.READY)
    }
}

@Composable
private fun ControlsCard(onOpenVoiceSettings: () -> Unit) {
    ProductCard {
        SectionTitle(uiText("Controls", "控制"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ControlSlider(Icons.Default.VolumeUp, uiText("Speaker Volume", "扬声器音量"), 0.70f, "70%")
                ControlSlider(Icons.Default.Mic, uiText("Mic Sensitivity", "麦克风灵敏度"), 0.60f, "60%")
                ToggleControl(Icons.Default.Campaign, uiText("ANC (Active Noise Canceling)", "ANC（主动降噪）"), true)
                ToggleControl(Icons.Default.HeadsetMic, uiText("Ambient Passthrough", "环境声透传"), true)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ProductInnerShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onOpenVoiceSettings)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LightMode, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(uiText("LED Personalization", "LED 个性化"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                WakeWordCard()
            }
        }
    }
}

@Composable
private fun ControlSlider(icon: ImageVector, label: String, value: Float, valueText: String) {
    Surface(
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Text(label, modifier = Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium)
                Text(valueText, style = MaterialTheme.typography.labelMedium)
            }
            Slider(value = value, onValueChange = {})
        }
    }
}

@Composable
private fun ToggleControl(icon: ImageVector, label: String, checked: Boolean) {
    Surface(
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(label, modifier = Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = checked, onCheckedChange = {})
        }
    }
}

@Composable
private fun WakeWordCard() {
    Surface(
        shape = ProductInnerShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SettingsInputComponent, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(uiText("Voice Wake-Word Sensitivity", "语音唤醒词灵敏度"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    uiText("Low", "低"),
                    uiText("Medium", "中"),
                    uiText("High", "高")
                ).forEach { label ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (label == "Medium") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (label == uiText("Medium", "中")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Text(uiText("Adjusts how sensitive the helmet is to your wake-word.", "调整头盔对唤醒词的敏感程度。"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SafetyModesCard(onOpenProfile: () -> Unit) {
    ProductCard {
        SectionTitle(uiText("Safety Modes", "安全模式"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SafetyMode(Icons.Default.HealthAndSafety, uiText("Ventilation Reminders", "通风提醒"), uiText("Reminds you to ventilate every 2 hours.", "每 2 小时提醒你通风。"), Modifier.weight(1f))
            SafetyMode(Icons.Default.PowerSettingsNew, uiText("Auto-Shutoff", "自动关机"), uiText("Turns off helmet after 30 min of inactivity.", "闲置 30 分钟后关闭头盔。"), Modifier.weight(1f))
            SafetyMode(Icons.Default.Shield, uiText("Impact Detection", "碰撞检测"), uiText("Alerts and logs on potential impacts.", "检测到潜在撞击时发出警报并记录。"), Modifier.weight(1f))
        }
        OutlinedButton(onClick = onOpenProfile, modifier = Modifier.fillMaxWidth()) {
            Text(uiText("Set emergency contacts", "设置紧急联系人"))
        }
    }
}

@Composable
private fun SafetyMode(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(25.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = true, onCheckedChange = {})
    }
}

@Composable
private fun DiagnosticsCard(onOpenModelSettings: () -> Unit) {
    ProductCard {
        SectionTitle(uiText("Diagnostics", "诊断"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(
                onClick = onOpenModelSettings,
                modifier = Modifier
                    .weight(0.75f)
                    .height(58.dp)
            ) {
                Icon(Icons.Default.HealthAndSafety, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(uiText("Run Health Check", "运行健康检查"))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(uiText("Running system check...", "正在运行系统检查..."), style = MaterialTheme.typography.bodyMedium)
                ProductProgress(progress = 0.68f, modifier = Modifier.padding(vertical = 8.dp))
                Text(uiText("Testing microphone...", "正在测试麦克风..."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("68%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Surface(
            shape = ProductInnerShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(uiText("Recent Logs", "最近日志"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(uiText("View All Logs", "查看全部日志"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                DiagnosticLog(Icons.Default.Check, uiText("May 24, 2025  8:57 AM", "2025 年 5 月 24 日 8:57"), uiText("Firmware updated successfully to v2.4.1", "固件已成功更新到 v2.4.1"))
                DiagnosticLog(Icons.Default.Check, uiText("May 24, 2025  8:56 AM", "2025 年 5 月 24 日 8:56"), uiText("Health check completed. All systems normal.", "健康检查完成，所有系统正常。"))
                DiagnosticLog(Icons.Default.Memory, uiText("Readiness", "就绪状态"), uiText("Open model settings if chat or image generation is unavailable.", "如果聊天或图片生成不可用，请打开模型设置。"))
            }
        }
    }
}

@Composable
private fun DiagnosticLog(icon: ImageVector, time: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text(time, modifier = Modifier.width(160.dp).padding(start = 12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(body, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CompanionReadinessLevel.color() = when (this) {
    CompanionReadinessLevel.READY -> MaterialTheme.colorScheme.primary
    CompanionReadinessLevel.DEGRADED -> MaterialTheme.colorScheme.tertiary
    CompanionReadinessLevel.NOT_READY -> MaterialTheme.colorScheme.error
}
