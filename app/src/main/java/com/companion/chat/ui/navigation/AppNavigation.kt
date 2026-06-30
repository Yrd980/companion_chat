package com.companion.chat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.companion.chat.ui.language.AppLanguage
import com.companion.chat.ui.language.uiText

enum class Screen(
    val route: String,
    private val englishLabel: String,
    private val chineseLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = "home",
        englishLabel = "Home",
        chineseLabel = "首页",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    HELMET(
        route = "helmet",
        englishLabel = "Helmet",
        chineseLabel = "头盔",
        selectedIcon = Icons.Filled.HeadsetMic,
        unselectedIcon = Icons.Outlined.HeadsetMic
    ),
    MEMORY(
        route = "memory",
        englishLabel = "Memory",
        chineseLabel = "记忆",
        selectedIcon = Icons.Filled.Memory,
        unselectedIcon = Icons.Outlined.Memory
    ),
    PROFILE(
        route = "settings",
        englishLabel = "Profile",
        chineseLabel = "个人",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    );

    fun label(language: AppLanguage): String = uiText(language, englishLabel, chineseLabel)
}

object AppRoutes {
    const val CHAT = "chat"
}

object DiscoverRoutes {
    const val LIST = "discover"
    const val DETAIL = "discover/{roleId}"

    fun detail(roleId: String): String = "discover/$roleId"
}

object SetupRoutes {
    const val ONBOARDING = "setup/onboarding"
}

object SettingsRoutes {
    const val CHARACTER = "settings/character"
    const val SKILLS = "settings/skills"
    const val MODEL = "settings/model"
    const val VOICE = "settings/voice"
    const val LANGUAGE = "settings/language"
    const val DARK_MODE = "settings/dark_mode"
    const val ABOUT = "settings/about"
}
