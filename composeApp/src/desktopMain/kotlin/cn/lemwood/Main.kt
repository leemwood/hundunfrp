package cn.lemwood

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppTheme

fun main() {
    AppStateHolder.init()
    application {
        val windowState = rememberWindowState(
            size = DpSize(1200.dp, 800.dp),
        )

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Frp Tunnel",
        ) {
            AppTheme {
                App()
            }
        }
    }
}
