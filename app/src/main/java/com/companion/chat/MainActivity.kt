package com.companion.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.companion.chat.ui.chat.ChatScreen
import com.companion.chat.ui.chat.ChatViewModel
import com.companion.chat.ui.helmet.HelmetScreen
import com.companion.chat.ui.home.DiscoverViewModel
import com.companion.chat.ui.home.DiscoverRoleDetailScreen
import com.companion.chat.ui.home.HomeDashboardViewModel
import com.companion.chat.ui.home.HomeScreen
import com.companion.chat.ui.language.AppLanguage
import com.companion.chat.ui.language.LocalAppLanguage
import com.companion.chat.ui.language.uiText
import com.companion.chat.ui.memory.MemoryScreen
import com.companion.chat.ui.navigation.AppRoutes
import com.companion.chat.ui.navigation.DiscoverRoutes
import com.companion.chat.ui.navigation.Screen
import com.companion.chat.ui.navigation.SettingsRoutes
import com.companion.chat.ui.navigation.SetupRoutes
import com.companion.chat.ui.settings.AboutScreen
import com.companion.chat.ui.settings.CharacterManagementScreen
import com.companion.chat.ui.settings.DarkModeSettingsScreen
import com.companion.chat.ui.settings.LanguageSettingsScreen
import com.companion.chat.ui.settings.ModelConfigScreen
import com.companion.chat.ui.settings.ProfileViewModel
import com.companion.chat.ui.settings.SettingsScreen
import com.companion.chat.ui.settings.SkillsManagementScreen
import com.companion.chat.ui.settings.VoiceSettingsScreen
import com.companion.chat.ui.setup.OnboardingScreen
import com.companion.chat.ui.theme.CompanionChatTheme
import kotlinx.coroutines.launch

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
    val application = LocalContext.current.applicationContext as CompanionChatApplication
    val viewModelFactory = remember(application) {
        AppViewModelFactory(application, application.appContainer)
    }
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current
    val chatViewModel: ChatViewModel = viewModel(factory = viewModelFactory)
    val discoverViewModel: DiscoverViewModel = viewModel(factory = viewModelFactory)
    val homeDashboardViewModel: HomeDashboardViewModel = viewModel(factory = viewModelFactory)
    val languageRepository = remember(application) { application.appContainer.appLanguageRepository }
    var appLanguage by remember { mutableStateOf(languageRepository.getLanguage()) }
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = Screen.entries.toList()
    val showBottomBar = screens.any { screen ->
        currentRoute == screen.route
    }

    DisposableEffect(lifecycleOwner, chatViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                chatViewModel.onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
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
                            val label = screen.label(appLanguage)

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
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.HOME.route,
                modifier = Modifier
                    .padding(innerPadding)
                    .imePadding()
            ) {
                composable(Screen.HOME.route) {
                    HomeScreen(
                        viewModel = discoverViewModel,
                        dashboardViewModel = homeDashboardViewModel,
                        onStartChat = { navController.navigate(AppRoutes.CHAT) },
                        onOpenHelmet = { navController.navigate(Screen.HELMET.route) },
                        onOpenMemory = { navController.navigate(Screen.MEMORY.route) },
                        onOpenProfile = { navController.navigate(Screen.PROFILE.route) },
                        onOpenRole = { roleId -> navController.navigate(DiscoverRoutes.detail(roleId)) },
                        onCreateRole = { navController.navigate(SettingsRoutes.CHARACTER) }
                    )
                }
                composable(
                    route = DiscoverRoutes.DETAIL,
                    arguments = listOf(navArgument("roleId") { type = NavType.StringType })
                ) { entry ->
                    val roleId = entry.arguments?.getString("roleId").orEmpty()
                    DiscoverRoleDetailScreen(
                        roleId = roleId,
                        viewModel = discoverViewModel,
                        onBack = { navController.popBackStack() },
                        onStartChat = { importedRoleId ->
                            coroutineScope.launch {
                                chatViewModel.startRoleConversation(importedRoleId)
                                navController.navigate(AppRoutes.CHAT) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
                composable(AppRoutes.CHAT) { ChatScreen(viewModel = chatViewModel) }
                composable(Screen.HELMET.route) {
                    HelmetScreen(
                        readinessSnapshot = application.appContainer.companionReadinessRepository.getSnapshot(),
                        onOpenModelSettings = { navController.navigate(SettingsRoutes.MODEL) },
                        onOpenVoiceSettings = { navController.navigate(SettingsRoutes.VOICE) },
                        onOpenProfile = { navController.navigate(Screen.PROFILE.route) }
                    )
                }
                composable(Screen.MEMORY.route) {
                    MemoryScreen(memoryViewModel = viewModel(factory = viewModelFactory))
                }
                composable(Screen.PROFILE.route) {
                    val profileViewModel: ProfileViewModel = viewModel(factory = viewModelFactory)
                    SettingsScreen(
                        viewModel = profileViewModel,
                        onNavigateToCharacter = { navController.navigate(SettingsRoutes.CHARACTER) },
                        onNavigateToSkills = { navController.navigate(SettingsRoutes.SKILLS) },
                        onNavigateToMemory = { navController.navigate(Screen.MEMORY.route) },
                        onNavigateToModel = { navController.navigate(SettingsRoutes.MODEL) },
                        onNavigateToVoice = { navController.navigate(SettingsRoutes.VOICE) },
                        onNavigateToLanguage = { navController.navigate(SettingsRoutes.LANGUAGE) },
                        onNavigateToDarkMode = { navController.navigate(SettingsRoutes.DARK_MODE) },
                        onNavigateToSetup = { navController.navigate(SetupRoutes.ONBOARDING) },
                        onNavigateToAbout = { navController.navigate(SettingsRoutes.ABOUT) }
                    )
                }
                composable(SetupRoutes.ONBOARDING) {
                    OnboardingScreen(
                        viewModel = viewModel(factory = viewModelFactory),
                        onBack = { navController.popBackStack() },
                        onOpenProfile = { navController.navigate(Screen.PROFILE.route) },
                        onOpenModelSettings = { navController.navigate(SettingsRoutes.MODEL) },
                        onOpenVoiceSettings = { navController.navigate(SettingsRoutes.VOICE) },
                        onOpenLanguage = { navController.navigate(SettingsRoutes.LANGUAGE) }
                    )
                }
                composable(SettingsRoutes.CHARACTER) {
                    CharacterManagementScreen(
                        onBack = { navController.popBackStack() },
                        onActivateRoleCard = { roleId -> chatViewModel.activateRoleCard(roleId) },
                        roleManagementViewModel = viewModel(factory = viewModelFactory),
                        onStartChat = { roleId ->
                            coroutineScope.launch {
                                chatViewModel.startRoleConversation(roleId)
                                navController.navigate(AppRoutes.CHAT) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
                composable(SettingsRoutes.SKILLS) {
                    SkillsManagementScreen(
                        onBack = { navController.popBackStack() },
                        onActivateSkill = { skillId -> chatViewModel.activateSkill(skillId) },
                        skillsManagementViewModel = viewModel(factory = viewModelFactory)
                    )
                }
                composable(SettingsRoutes.MODEL) {
                    ModelConfigScreen(
                        onBack = { navController.popBackStack() },
                        onModelConfigChanged = { chatViewModel.initializeEngine() },
                        viewModel = viewModel(factory = viewModelFactory)
                    )
                }
                composable(SettingsRoutes.VOICE) {
                    VoiceSettingsScreen(
                        onBack = { navController.popBackStack() },
                        viewModel = viewModel(factory = viewModelFactory)
                    )
                }
                composable(SettingsRoutes.LANGUAGE) {
                    LanguageSettingsScreen(
                        language = appLanguage,
                        onLanguageChange = { language: AppLanguage ->
                            appLanguage = language
                            languageRepository.updateLanguage(language)
                        },
                        onBack = { navController.popBackStack() }
                    )
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
}
