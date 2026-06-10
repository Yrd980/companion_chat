package com.companion.chat.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.chat.data.local.entity.Skill
import com.companion.chat.ui.language.uiText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsManagementScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onActivateSkill: suspend (Long) -> Unit = {},
    skillsManagementViewModel: SkillsManagementViewModel = viewModel()
) {
    val uiState by skillsManagementViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var editingSkill by remember { mutableStateOf<Skill?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deletingSkill by remember { mutableStateOf<Skill?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(uiText("Companion Modes", "陪伴模式"), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = uiText("Back", "返回")
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = uiText("Add companion mode", "添加陪伴模式"))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = uiText(
                        "Add a lightweight mode to the current companion for translation, writing, study, or emotional reflection.",
                        "为当前角色叠加一个轻量模式，让它更适合翻译、创作、学习或情绪整理。"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            uiState.activeSkill?.let { activeSkill ->
                item { SkillsSectionTitle(uiText("Currently Active", "当前激活")) }
                item {
                    SkillItem(
                        skill = activeSkill,
                        isActive = true,
                        onActivate = {},
                        onEdit = if (activeSkill.isBuiltIn) null else ({ { editingSkill = activeSkill } }),
                        onDelete = if (activeSkill.isBuiltIn) null else ({ { deletingSkill = activeSkill } })
                    )
                }
            }

            item { SkillsSectionTitle(uiText("Built-in Modes", "内置模式")) }
            if (uiState.builtInSkills.isEmpty()) {
                item {
                    SkillsEmptyState(
                        uiText("No built-in modes", "当前没有内置模式"),
                        uiText("Built-in modes can be added during database initialization.", "后续可在数据库初始化中补充。")
                    )
                }
            } else {
                items(uiState.builtInSkills, key = { it.id }) { skill ->
                    SkillItem(
                        skill = skill,
                        isActive = skill.isActive,
                        onActivate = {
                            scope.launch {
                                onActivateSkill(skill.id)
                                skillsManagementViewModel.refresh()
                            }
                        },
                        onEdit = null,
                        onDelete = null
                    )
                }
            }

            item { SkillsSectionTitle(uiText("My Modes", "我的模式")) }
            if (uiState.customSkills.isEmpty()) {
                item {
                    SkillsEmptyState(
                        uiText("No custom modes yet", "还没有自定义模式"),
                        uiText("Tap the plus button in the top-right to create a custom companion mode.", "点击右上角“+”创建你的自定义陪伴模式。")
                    )
                }
            } else {
                items(uiState.customSkills, key = { it.id }) { skill ->
                    SkillItem(
                        skill = skill,
                        isActive = skill.isActive,
                        onActivate = {
                            scope.launch {
                                onActivateSkill(skill.id)
                                skillsManagementViewModel.refresh()
                            }
                        },
                        onEdit = { editingSkill = skill },
                        onDelete = { deletingSkill = skill }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        SkillEditorDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { name, description, systemPrompt ->
                if (name.isBlank() || systemPrompt.isBlank()) {
                    return@SkillEditorDialog
                }
                skillsManagementViewModel.createSkill(name, description, systemPrompt)
                showCreateDialog = false
            }
        )
    }

    editingSkill?.let { skill ->
        SkillEditorDialog(
            skill = skill,
            onDismiss = { editingSkill = null },
            onSave = { name, description, systemPrompt ->
                if (name.isBlank() || systemPrompt.isBlank()) {
                    return@SkillEditorDialog
                }
                skillsManagementViewModel.updateSkill(
                    id = skill.id,
                    name = name,
                    description = description,
                    systemPrompt = systemPrompt,
                    icon = skill.icon
                )
                editingSkill = null
            }
        )
    }

    deletingSkill?.let { skill ->
        AlertDialog(
            onDismissRequest = { deletingSkill = null },
            title = { Text(uiText("Delete Skill", "删除 Skill")) },
            text = { Text(uiText("Delete \"${skill.displayName()}\"?", "确认删除“${skill.displayName()}”吗？")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        skillsManagementViewModel.deleteSkill(skill.id)
                        deletingSkill = null
                    }
                ) {
                    Text(uiText("Delete", "删除"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSkill = null }) {
                    Text(uiText("Cancel", "取消"))
                }
            }
        )
    }
}

@Composable
private fun SkillsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SkillItem(
    skill: Skill,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = skill.displayName(), style = MaterialTheme.typography.titleMedium)
                    val description = skill.displayDescription()
                    if (description.isNotBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isActive) {
                    AssistChip(onClick = {}, label = { Text(uiText("Active", "使用中")) })
                }
            }

            Text(
                text = uiText("Used ${skill.usageCount} times", "已使用 ${skill.usageCount} 次"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isActive) {
                    TextButton(onClick = onActivate) {
                        Text(uiText("Enable", "启用"))
                    }
                }
                onEdit?.let {
                    TextButton(onClick = it) {
                        Text(uiText("Edit", "编辑"))
                    }
                }
                onDelete?.let {
                    TextButton(onClick = it) {
                        Text(uiText("Delete", "删除"))
                    }
                }
            }
        }
    }
}

@Composable
private fun Skill.displayName(): String {
    return if (isBuiltIn && name == "翻译助手") {
        uiText("Translation Helper", "翻译助手")
    } else {
        name
    }
}

@Composable
private fun Skill.displayDescription(): String {
    return if (isBuiltIn && name == "翻译助手") {
        uiText(
            "Professional translation that accounts for context, culture, and native-language differences.",
            "考虑语境、文化和母语差异的专业翻译"
        )
    } else {
        description
    }
}

@Composable
private fun SkillsEmptyState(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.Build, contentDescription = null)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
