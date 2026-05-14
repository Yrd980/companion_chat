package com.companion.chat.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.companion.chat.data.context.ContextConfigRepository
import com.companion.chat.data.image.ImageGenerationConfig
import com.companion.chat.data.image.ImageGenerationConfigRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val contextConfigRepository = remember(context) { ContextConfigRepository(context) }
    val imageConfigRepository = remember(context) { ImageGenerationConfigRepository(context) }
    var retainedRounds by remember { mutableIntStateOf(contextConfigRepository.getSettings().retainedRounds) }
    var imageConfig by remember { mutableStateOf(imageConfigRepository.getConfig()) }
    val options = listOf(3, 5, 10, 15, 20)

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "模型配置",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
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
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "上下文窗口大小",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "当前保留最近 $retainedRounds 轮完整对话。\n修改后会在下一次发送消息时生效。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            options.forEach { option ->
                ContextWindowOptionItem(
                    rounds = option,
                    selected = retainedRounds == option,
                    onClick = {
                        retainedRounds = option
                        contextConfigRepository.updateRetainedRounds(option)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            Text(
                text = "建议范围 3~20。轮数越大，保留原始上下文越多；轮数越小，越早触发压缩。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "图片生成 HTTP 配置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                ImageConfigField("Base URL", imageConfig.baseUrl) {
                    imageConfig = imageConfig.copy(baseUrl = it)
                    imageConfigRepository.updateConfig(imageConfig)
                }
                ImageConfigField("API Key", imageConfig.apiKey) {
                    imageConfig = imageConfig.copy(apiKey = it)
                    imageConfigRepository.updateConfig(imageConfig)
                }
                ImageConfigField("Model", imageConfig.model) {
                    imageConfig = imageConfig.copy(model = it)
                    imageConfigRepository.updateConfig(imageConfig)
                }
                ImageConfigField("Request Template", imageConfig.requestTemplate, minLines = 3) {
                    imageConfig = imageConfig.copy(requestTemplate = it.ifBlank { ImageGenerationConfig.DEFAULT_REQUEST_TEMPLATE })
                    imageConfigRepository.updateConfig(imageConfig)
                }
                ImageConfigField("Response Image Field Path", imageConfig.responseImageFieldPath) {
                    imageConfig = imageConfig.copy(responseImageFieldPath = it.ifBlank { ImageGenerationConfig.DEFAULT_RESPONSE_FIELD_PATH })
                    imageConfigRepository.updateConfig(imageConfig)
                }
                ImageConfigField("Timeout Millis", imageConfig.timeoutMillis.toString()) {
                    val timeout = it.toIntOrNull() ?: imageConfig.timeoutMillis
                    imageConfig = imageConfig.copy(timeoutMillis = timeout)
                    imageConfigRepository.updateConfig(imageConfig)
                }
                Text(
                    text = "模板支持 {{model}} 与 {{prompt}}。响应字段示例：data.0.url 或 data.0.b64_json。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ImageConfigField(
    label: String,
    value: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = minLines,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}

@Composable
private fun ContextWindowOptionItem(
    rounds: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.height(0.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = "保留最近 $rounds 轮",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "压缩阈值约为 ${rounds * 2 + 10} 条消息",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
