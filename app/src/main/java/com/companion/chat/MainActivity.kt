package com.companion.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.companion.chat.ui.chat.ChatScreen
import com.companion.chat.ui.home.HomeScreen
import com.companion.chat.ui.memory.MemoryScreen
import com.companion.chat.ui.navigation.Screen
import com.companion.chat.ui.navigation.SettingsRoutes
import com.companion.chat.ui.settings.AboutScreen
import com.companion.chat.ui.settings.CharacterManagementScreen
import com.companion.chat.ui.settings.DarkModeSettingsScreen
import com.companion.chat.ui.settings.LanguageSettingsScreen
import com.companion.chat.ui.settings.ModelConfigScreen
import com.companion.chat.ui.settings.SettingsScreen
import com.companion.chat.ui.settings.VoiceSettingsScreen
import com.companion.chat.ui.theme.CompanionChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompanionChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = Screen.entries.toList()
    val showBottomBar = screens.any { screen ->
        currentRoute == screen.route
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    screens.forEach { screen ->
                        val selected = currentRoute == screen.route

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon
                                    else screen.unselectedIcon,
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.HOME.route) { HomeScreen() }
            composable(Screen.CHAT.route) { ChatScreen() }
            composable(Screen.MEMORY.route) { MemoryScreen() }
            composable(Screen.SETTINGS.route) {
                SettingsScreen(
                    onNavigateToCharacter = { navController.navigate(SettingsRoutes.CHARACTER) },
                    onNavigateToModel = { navController.navigate(SettingsRoutes.MODEL) },
                    onNavigateToVoice = { navController.navigate(SettingsRoutes.VOICE) },
                    onNavigateToLanguage = { navController.navigate(SettingsRoutes.LANGUAGE) },
                    onNavigateToDarkMode = { navController.navigate(SettingsRoutes.DARK_MODE) },
                    onNavigateToAbout = { navController.navigate(SettingsRoutes.ABOUT) }
                )
            }
            composable(SettingsRoutes.CHARACTER) {
                CharacterManagementScreen(onBack = { navController.popBackStack() })
            }
            composable(SettingsRoutes.MODEL) {
                ModelConfigScreen(onBack = { navController.popBackStack() })
            }
            composable(SettingsRoutes.VOICE) {
                VoiceSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(SettingsRoutes.LANGUAGE) {
                LanguageSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(SettingsRoutes.DARK_MODE) {
                DarkModeSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(SettingsRoutes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
