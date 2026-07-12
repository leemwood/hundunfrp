package cn.lemwood.platform

import cn.lemwood.data.FrpConfigBuilder
import cn.lemwood.model.TunnelStatus
import cn.lemwood.state.AppStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

actual class FrpController {
    private var process: Process? = null
    private var configPath: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    actual fun connect(host: String, port: Int, token: String): Boolean {
        return try {
            disconnect()

            val state = AppStateHolder.state.value
            val config = FrpConfigBuilder.buildConfig(state.settings, state.tunnels)
            val configDir = File("D:\\.config\\frp-kmp")
            configDir.mkdirs()
            val configFile = File(configDir, "frpc.ini")
            configFile.writeText(config)
            configPath = configFile.absolutePath

            val frpcPath = findFrpcBinary()
            if (frpcPath == null) {
                AppStateHolder.addLog(
                    cn.lemwood.model.LogEntry(
                        level = cn.lemwood.model.LogLevel.ERROR,
                        message = "未找到 frpc 可执行文件，请将 frpc 放在系统 PATH 中或 D:\\.config\\frp-kmp\\ 目录下",
                        timestamp = System.currentTimeMillis(),
                    )
                )
                return false
            }

            val pb = ProcessBuilder(frpcPath, "-c", configFile.absolutePath)
            pb.redirectErrorStream(true)
            val p = pb.start()
            process = p

            scope.launch {
                val reader = BufferedReader(InputStreamReader(p.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val msg = line ?: continue
                    when {
                        msg.contains("login to server success", ignoreCase = true) ||
                        msg.contains("start proxy success", ignoreCase = true) -> {
                            AppStateHolder.addLog(
                                cn.lemwood.model.LogEntry(
                                    level = cn.lemwood.model.LogLevel.INFO,
                                    message = msg,
                                    timestamp = System.currentTimeMillis(),
                                )
                            )
                        }
                        msg.contains("error", ignoreCase = true) ||
                        msg.contains("failed", ignoreCase = true) -> {
                            AppStateHolder.addLog(
                                cn.lemwood.model.LogEntry(
                                    level = cn.lemwood.model.LogLevel.ERROR,
                                    message = msg,
                                    timestamp = System.currentTimeMillis(),
                                )
                            )
                        }
                        msg.contains("warn", ignoreCase = true) -> {
                            AppStateHolder.addLog(
                                cn.lemwood.model.LogEntry(
                                    level = cn.lemwood.model.LogLevel.WARN,
                                    message = msg,
                                    timestamp = System.currentTimeMillis(),
                                )
                            )
                        }
                        else -> {
                            AppStateHolder.addLog(
                                cn.lemwood.model.LogEntry(
                                    level = cn.lemwood.model.LogLevel.INFO,
                                    message = msg,
                                    timestamp = System.currentTimeMillis(),
                                )
                            )
                        }
                    }
                }
                val exitCode = p.waitFor()
                if (exitCode != 0) {
                    AppStateHolder.addLog(
                        cn.lemwood.model.LogEntry(
                            level = cn.lemwood.model.LogLevel.ERROR,
                            message = "frpc 进程退出，退出码: $exitCode",
                            timestamp = System.currentTimeMillis(),
                        )
                    )
                }
            }

            AppStateHolder.addLog(
                cn.lemwood.model.LogEntry(
                    level = cn.lemwood.model.LogLevel.INFO,
                    message = "frpc 启动中... (${frpcPath} -c ${configFile.absolutePath})",
                    timestamp = System.currentTimeMillis(),
                )
            )
            true
        } catch (e: Exception) {
            AppStateHolder.addLog(
                cn.lemwood.model.LogEntry(
                    level = cn.lemwood.model.LogLevel.ERROR,
                    message = "启动 frpc 失败: ${e.message}",
                    timestamp = System.currentTimeMillis(),
                )
            )
            false
        }
    }

    actual fun disconnect() {
        try {
            process?.apply {
                destroy()
            }
            process = null
            configPath = null
        } catch (_: Exception) {}
    }

    actual fun startTunnel(configJson: String): Boolean {
        return try {
            disconnect()
            val state = AppStateHolder.state.value
            connect(
                host = state.settings.serverAddr,
                port = state.settings.serverPort,
                token = state.settings.serverToken
            )
        } catch (_: Exception) {
            false
        }
    }

    actual fun stopTunnel(tunnelId: String): Boolean {
        disconnect()
        return true
    }

    actual fun getTunnelStatus(tunnelId: String): TunnelStatus {
        return if (process?.isAlive == true) TunnelStatus.CONNECTING else TunnelStatus.OFFLINE
    }

    actual fun reloadConfig(): Boolean {
        return try {
            disconnect()
            val state = AppStateHolder.state.value
            connect(
                host = state.settings.serverAddr,
                port = state.settings.serverPort,
                token = state.settings.serverToken
            )
        } catch (_: Exception) {
            false
        }
    }

    actual fun testConnection(host: String, port: Int, timeoutSeconds: Int): String? {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutSeconds * 1000)
            socket.close()
            null
        } catch (e: Exception) {
            e.message ?: "连接失败"
        }
    }

    private fun findFrpcBinary(): String? {
        val candidates = listOf(
            "D:\\.config\\frp-kmp\\frpc.exe",
            "D:\\.config\\frp-kmp\\frpc",
            "frpc.exe",
            "frpc",
        )
        for (path in candidates) {
            if (File(path).exists()) return path
        }
        try {
            val which = ProcessBuilder("where", "frpc").start()
            val reader = BufferedReader(InputStreamReader(which.inputStream))
            val found = reader.readLine()
            which.waitFor()
            if (found != null && found.isNotBlank()) return found.trim()
        } catch (_: Exception) {}
        try {
            val which = ProcessBuilder("where", "frpc.exe").start()
            val reader = BufferedReader(InputStreamReader(which.inputStream))
            val found = reader.readLine()
            which.waitFor()
            if (found != null && found.isNotBlank()) return found.trim()
        } catch (_: Exception) {}
        return null
    }
}
