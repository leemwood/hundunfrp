package cn.lemwood

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cn.lemwood.navigation.NavigationType
import cn.lemwood.navigation.Screen
import cn.lemwood.navigation.toNavigationType
import cn.lemwood.theme.AppDimen
import cn.lemwood.ui.AddTunnelFab
import cn.lemwood.ui.LogScreen
import cn.lemwood.ui.SettingsScreen
import cn.lemwood.ui.StatusScreen
import cn.lemwood.ui.TunnelListScreen

@Composable
fun App() {
    AppScaffold()
}

@Composable
fun AppScaffold() {
    BoxWithConstraints {
        val navigationType = maxWidth.toNavigationType()
        var selectedScreen by remember { mutableStateOf(Screen.TunnelList) }
        val snackbarHostState = remember { SnackbarHostState() }

        val content: @Composable () -> Unit = {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                when (selectedScreen) {
                    Screen.TunnelList -> TunnelListScreen(modifier = Modifier.fillMaxSize())
                    Screen.Status -> StatusScreen(modifier = Modifier.fillMaxSize())
                    Screen.Settings -> SettingsScreen(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHostState = snackbarHostState,
                    )
                    Screen.Log -> LogScreen(modifier = Modifier.fillMaxSize())
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
                            AddTunnelFab(snackbarHostState = snackbarHostState)
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
                                    snackbarHostState = snackbarHostState,
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
