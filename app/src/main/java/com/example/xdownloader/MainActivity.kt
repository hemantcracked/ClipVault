package com.example.xdownloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.xdownloader.data.local.UserPreferencesRepository
import com.example.xdownloader.domain.model.UserSettings
import com.example.xdownloader.ui.home.HomeScreen
import com.example.xdownloader.ui.home.HomeViewModel
import com.example.xdownloader.ui.library.LibraryScreen
import com.example.xdownloader.ui.library.LibraryViewModel
import com.example.xdownloader.ui.settings.SettingsScreen
import com.example.xdownloader.ui.settings.SettingsViewModel
import com.example.xdownloader.ui.statuses.StatusesScreen
import com.example.xdownloader.ui.statuses.StatusesViewModel
import com.example.xdownloader.ui.theme.ClipVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Statuses : Screen("statuses", "Statuses")
    object Library : Screen("library", "Saved")
    object Settings : Screen("settings", "Settings")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        } else ""

        setContent {
            val userSettings by preferencesRepository.userSettingsFlow.collectAsState(initial = UserSettings())

            ClipVaultTheme(themeMode = userSettings.themeMode) {
                MainApp(initialSharedText = sharedText)
            }
        }
    }
}

@Composable
fun MainApp(initialSharedText: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(Screen.Home, Screen.Statuses, Screen.Library, Screen.Settings)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
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
                            when (screen) {
                                Screen.Home -> Icon(Icons.Default.Download, contentDescription = null)
                                Screen.Statuses -> Icon(Icons.Default.FolderSpecial, contentDescription = null)
                                Screen.Library -> Icon(Icons.Default.VideoLibrary, contentDescription = null)
                                Screen.Settings -> Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                androidx.compose.runtime.LaunchedEffect(initialSharedText) {
                    if (initialSharedText.isNotBlank() && homeViewModel.inputUrl.value.isEmpty()) {
                        homeViewModel.inputUrl.value = initialSharedText
                        homeViewModel.resolveUrl(initialSharedText)
                    }
                }
                HomeScreen(viewModel = homeViewModel)
            }
            composable(Screen.Statuses.route) {
                val statusesViewModel: StatusesViewModel = hiltViewModel()
                StatusesScreen(viewModel = statusesViewModel)
            }
            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                LibraryScreen(viewModel = libraryViewModel)
            }
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
