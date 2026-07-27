package cn.lemwood

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cn.lemwood.navigation.NavigationType
import cn.lemwood.navigation.Screen
import cn.lemwood.navigation.toNavigationType
import cn.lemwood.model.AppSettings
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppDimen
import kotlinx.coroutines.flow.map
import cn.lemwood.ui.AddTunnelFab
import cn.lemwood.ui.ErrorScreen
import cn.lemwood.ui.LogScreen
import cn.lemwood.ui.OnboardingScreen
import cn.lemwood.ui.SettingsScreen
import cn.lemwood.ui.StatusScreen
import cn.lemwood.ui.TunnelListScreen
import cn.lemwood.ui.TunnelEditorScreen
import cn.lemwood.ui.CrashHandler

@Composable
fun App() {
    val crashError = CrashHandler.lastError

    if (crashError != null) {
        ErrorScreen(
            error = crashError,
            onRestart = { CrashHandler.clear() },
            onResetData = {
                AppStateHolder.resetToDefaults()
                CrashHandler.clear()
            },
        )
        return
    }

    AppScaffold()
}

@Composable
fun AppScaffold() {
    BoxWithConstraints {
        val navigationType = maxWidth.toNavigationType()
        var selectedScreen by remember { mutableStateOf(Screen.TunnelList) }
        val snackbarHostState = remember { SnackbarHostState() }
        var editingTunnelId by remember { mutableStateOf<String?>(null) }
        // 编辑器打开状态需独立于 tunnelId：新增隧道时 tunnelId 为 null 也要打开
        var isEditorOpen by remember { mutableStateOf(false) }
        var hasAutoStarted by remember { mutableStateOf(false) }

        val settings by AppStateHolder.state.map { it.settings }.collectAsState(AppSettings())
        if (!settings.hasCompletedOnboarding) {
            OnboardingScreen(
                onComplete = {
                    AppStateHolder.updateSettings(settings.copy(hasCompletedOnboarding = true))
                },
            )
            return@BoxWithConstraints
        }

        LaunchedEffect(settings) {
            if (!hasAutoStarted && settings.autoStart && settings.serverAddr.isNotBlank()) {
                hasAutoStarted = true
                AppStateHolder.connectServer()
            }
        }

        if (isEditorOpen) {
            TunnelEditorScreen(
                tunnelId = editingTunnelId,
                onDismiss = { isEditorOpen = false },
            )
            return@BoxWithConstraints
        }

        val content: @Composable () -> Unit = {
            AnimatedContent(
                targetState = selectedScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { screen ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.TunnelList -> TunnelListScreen(
                            modifier = Modifier.fillMaxSize(),
                            onAddTunnel = {
                                editingTunnelId = null
                                isEditorOpen = true
                            },
                            onEditTunnel = { id ->
                                editingTunnelId = id
                                isEditorOpen = true
                            },
                        )
                        Screen.Status -> StatusScreen(modifier = Modifier.fillMaxSize())
                        Screen.Settings -> SettingsScreen(
                            modifier = Modifier.fillMaxSize(),
                            snackbarHostState = snackbarHostState,
                        )
                        Screen.Log -> LogScreen(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        when (navigationType) {
            NavigationType.Compact -> {
                Scaffold(
                    bottomBar = {
                        BottomAppBar {
                            Screen.entries.forEach { screen ->
                                NavigationBarItem(
                                    selected = selectedScreen == screen,
                                    onClick = { selectedScreen = screen },
                                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                                    label = { Text(screen.label) },
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (selectedScreen == Screen.TunnelList) {
                            AddTunnelFab(onClick = {
                                editingTunnelId = null
                                isEditorOpen = true
                            })
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        content()
                    }
                }
            }

            NavigationType.Medium,
            NavigationType.Expanded -> {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        NavigationRail(
                            header = {
                                AddTunnelFab(
                                    onClick = {
                                        editingTunnelId = null
                                        isEditorOpen = true
                                    },
                                    modifier = Modifier.padding(top = AppDimen.ScreenPadding),
                                )
                            },
                        ) {
                            Screen.entries.forEach { screen ->
                                NavigationRailItem(
                                    selected = selectedScreen == screen,
                                    onClick = { selectedScreen = screen },
                                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                                    label = { Text(screen.label) },
                                )
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                            content()
                        }
                    }
                }
            }
        }
    }
}
