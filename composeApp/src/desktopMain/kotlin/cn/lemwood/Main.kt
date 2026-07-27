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
    // Ctrl+C / kill 兜底：断开 frpc 并清理托盘图标
    Runtime.getRuntime().addShutdownHook(Thread {
        runCatching { AppStateHolder.disconnectServer() }
        TrayController.removeAll()
        // disconnect 是异步协程，给 IO 调度留出执行窗口
        Thread.sleep(300)
    })

    application {
        var isVisible by remember { mutableStateOf(true) }
        val windowState = rememberWindowState(
            size = DpSize(1200.dp, 800.dp),
        )

        // 托盘可用时装托盘；不可用则后续关窗直接退出
        val trayInstalled = remember {
            TrayController.install(
                onShow = { isVisible = true },
                onExit = {
                    AppStateHolder.disconnectServer()
                    TrayController.removeAll()
                    exitApplication()
                },
            )
        }

        if (isVisible) {
            Window(
                onCloseRequest = {
                    if (trayInstalled) {
                        // 最小化到托盘，frpc 继续运行，可从托盘菜单唤回
                        isVisible = false
                    } else {
                        AppStateHolder.disconnectServer()
                        TrayController.removeAll()
                        exitApplication()
                    }
                },
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
    // 配置为异步加载，最多等 2 秒再读取
    var waited = 0
    while (waited < 2000) {
        val s = AppStateHolder.state.value
        if (s.settings.serverAddr.isNotBlank() || s.tunnels.isNotEmpty()) break
        Thread.sleep(100)
        waited += 100
    }
    val state = AppStateHolder.state.value
    val settings = state.settings

    if (settings.serverAddr.isBlank()) {
        println("未配置服务器地址，请先在 GUI 中设置")
        return
    }

    // 与 GUI 行为一致：全部隧道停用时不连接
    val enabledTunnels = state.tunnels.filter { it.enabled }
    if (enabledTunnels.isEmpty()) {
        println("没有已启用的隧道，请先在 GUI 中启用至少一条隧道")
        return
    }

    val controller = FrpController()
    println("启动 frp 客户端...")
    println("服务器: ${settings.serverAddr}:${settings.serverPort}")
    println("已启用隧道: ${enabledTunnels.size} 条")

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
            TrayController.removeAll()
        })
        while (true) {
            Thread.sleep(1000)
        }
    } else {
        println("启动失败，请检查配置和日志")
        System.exit(1)
    }
}
