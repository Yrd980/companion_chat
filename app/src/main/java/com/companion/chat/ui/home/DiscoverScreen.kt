package com.companion.chat.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.chat.ui.components.ProductCard
import com.companion.chat.ui.components.SectionTitle
import com.companion.chat.ui.language.uiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel(),
    onBack: () -> Unit = {},
    onOpenRole: (String) -> Unit = {},
    onCreateRole: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiText("Discover companions", "发现伙伴"))
                        Text(
                            uiText("Role cards for local relationships", "用于本地关系的角色卡"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = uiText("Back", "返回"))
                    }
                },
                actions = {
                    IconButton(onClick = onCreateRole) {
                        Icon(Icons.Default.Add, contentDescription = uiText("Create role", "创建角色"))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
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
            }
            item { RoleSortChips(selected = uiState.sortMode, onSelect = viewModel::setSortMode) }
            item { SectionTitle(uiText("Featured companions", "精选伙伴")) }
            if (uiState.items.isEmpty()) {
                item {
                    ProductCard {
                        Text(uiText("No roles found", "未找到角色"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            uiText("Try a different search or create a local role.", "换个搜索条件，或创建本地角色。"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.items.size) { index ->
                    val item = uiState.items[index]
                    DiscoverRoleCard(
                        item = item,
                        onOpen = { onOpenRole(item.role.id) },
                        onFavorite = { viewModel.toggleFavorite(item.role.id) }
                    )
                }
            }
        }
    }
}
