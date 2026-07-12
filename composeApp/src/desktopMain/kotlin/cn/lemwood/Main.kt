package cn.lemwood

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cn.lemwood.model.LogLevel
import cn.lemwood.platform.FrpController
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppTheme
import cn.lemwood.ui.CrashHandler

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        CrashHandler.record(e)
    }
    val args = System.getProperty("sun.java.command", "")
    val isHeadless = args.contains("--headless", ignoreCase = true) ||
        args.contains("--cli", ignoreCase = true)

    if (isHeadless) {
        runHeadless(args)
        return
    }

    AppStateHolder.init()
    application {
        var isVisible by remember { mutableStateOf(true) }
        val windowState = rememberWindowState(
            size = DpSize(1200.dp, 800.dp),
        )

        if (isVisible) {
            Window(
                onCloseRequest = { isVisible = false },
                state = windowState,
                title = "Frp Tunnel",
            ) {
                AppTheme {
                    App()
                }
            }
        }
    }
}

private fun runHeadless(args: String) {
    AppStateHolder.init()
    val state = AppStateHolder.state.value
    val settings = state.settings

    if (settings.serverAddr.isBlank()) {
        println("未配置服务器地址，请先在 GUI 中设置")
        return
    }

    val controller = FrpController()
    println("启动 frp 客户端...")
    println("服务器: ${settings.serverAddr}:${settings.serverPort}")

    val ok = controller.connect(
        host = settings.serverAddr,
        port = settings.serverPort,
        token = settings.serverToken
    )

    if (ok) {
        println("frp 客户端已启动 (PID: ${ProcessHandle.current().pid()})")
        println("按 Ctrl+C 退出")
        Runtime.getRuntime().addShutdownHook(Thread {
            controller.disconnect()
        })
        while (true) {
            Thread.sleep(1000)
        }
    } else {
        println("启动失败，请检查配置和日志")
        System.exit(1)
    }
}
