package com.companion.chat.data.dashboard

data class HomeDashboardUiState(
    val relationship: RelationshipSummary = RelationshipSummary(),
    val localDevice: LocalDeviceSummary = LocalDeviceSummary(),
    val quickActions: List<HomeQuickAction> = emptyList(),
    val recentMemories: List<HomeMemorySummary> = emptyList(),
    val recentActivity: List<HomeActivitySummary> = emptyList(),
    val suggestions: List<HomeSuggestion> = emptyList(),
    val isLoading: Boolean = true
)

data class RelationshipSummary(
    val companionName: String = "Aiko Hoshizora",
    val companionMood: String = "Bright",
    val level: Int = 1,
    val xp: Int = 0,
    val nextLevelXp: Int = 100,
    val closenessLabel: String = "New companion"
)

data class LocalDeviceSummary(
    val modelReady: Boolean = false,
    val voiceReady: Boolean = false,
    val imageReady: Boolean = false,
    val noHelmetMode: Boolean = true,
    val statusLabel: String = "Local companion mode"
)

data class HomeQuickAction(
    val id: String,
    val title: String,
    val subtitle: String,
    val enabled: Boolean = true,
    val disabledReason: String = ""
)

data class HomeMemorySummary(
    val id: Long,
    val title: String,
    val detail: String,
    val category: String,
    val mediaUri: String? = null
)

data class HomeActivitySummary(
    val id: String,
    val title: String,
    val detail: String,
    val timestampLabel: String
)

data class HomeSuggestion(
    val id: String,
    val text: String,
    val routeHint: String
)
