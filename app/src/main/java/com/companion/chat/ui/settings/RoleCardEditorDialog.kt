package com.companion.chat.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.companion.chat.data.local.entity.RoleCard

@Composable
fun RoleCardEditorDialog(
    roleCard: RoleCard? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        description: String,
        avatar: String,
        persona: String,
        speakingStyle: String,
        background: String,
        rules: String,
        taboos: String,
        openingMessage: String,
        exampleDialogue: String
    ) -> Unit
) {
    var name by remember(roleCard) { mutableStateOf(roleCard?.name.orEmpty()) }
    var description by remember(roleCard) { mutableStateOf(roleCard?.description.orEmpty()) }
    var avatar by remember(roleCard) { mutableStateOf(roleCard?.avatar.orEmpty()) }
    var persona by remember(roleCard) { mutableStateOf(roleCard?.persona.orEmpty()) }
    var speakingStyle by remember(roleCard) { mutableStateOf(roleCard?.speakingStyle.orEmpty()) }
    var background by remember(roleCard) { mutableStateOf(roleCard?.background.orEmpty()) }
    var rules by remember(roleCard) { mutableStateOf(roleCard?.rules.orEmpty()) }
    var taboos by remember(roleCard) { mutableStateOf(roleCard?.taboos.orEmpty()) }
    var openingMessage by remember(roleCard) { mutableStateOf(roleCard?.openingMessage.orEmpty()) }
    var exampleDialogue by remember(roleCard) { mutableStateOf(roleCard?.exampleDialogue.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (roleCard == null) "新建角色卡" else "编辑角色卡",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoleCardField(label = "名称", value = name, onValueChange = { name = it })
                RoleCardField(label = "简介", value = description, onValueChange = { description = it })
                RoleCardField(label = "头像/图标标识", value = avatar, onValueChange = { avatar = it })
                RoleCardField(label = "核心人设", value = persona, onValueChange = { persona = it }, minLines = 2)
                RoleCardField(label = "说话风格", value = speakingStyle, onValueChange = { speakingStyle = it }, minLines = 2)
                RoleCardField(label = "背景设定", value = background, onValueChange = { background = it }, minLines = 2)
                RoleCardField(label = "行为规则", value = rules, onValueChange = { rules = it }, minLines = 2)
                RoleCardField(label = "禁止项", value = taboos, onValueChange = { taboos = it }, minLines = 2)
                RoleCardField(label = "开场白", value = openingMessage, onValueChange = { openingMessage = it }, minLines = 2)
                RoleCardField(label = "示例对话", value = exampleDialogue, onValueChange = { exampleDialogue = it }, minLines = 3)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        description,
                        avatar,
                        persona,
                        speakingStyle,
                        background,
                        rules,
                        taboos,
                        openingMessage,
                        exampleDialogue
                    )
                }
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
private fun RoleCardField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        minLines = minLines
    )
}
