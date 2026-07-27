package cn.lemwood.platform

import cn.lemwood.data.FrpConfigBuilder
import cn.lemwood.model.LogEntry
import cn.lemwood.model.LogLevel
import cn.lemwood.model.TunnelStatus
import cn.lemwood.state.AppStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.Base64

actual class FrpController {
    private var process: Process? = null
    private var configPath: String? = null
    private var pollJob: Job? = null
    private var processStartTime: Long = 0L
    private var latencyMs: Long = -1L

    @Volatile
    private var manualStop: Boolean = false

    // 记录最近一次连接参数，供自动重连使用
    private var lastHost: String = ""
    private var lastPort: Int = 0
    private var lastToken: String = ""

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    actual fun connect(host: String, port: Int, token: String): Boolean {
        manualStop = false
        lastHost = host
        lastPort = port
        lastToken = token

        // 启动前先测一次 TCP 连接耗时作为延迟，失败也继续启动流程
        latencyMs = measureLatency(host, port)

        return startProcess()
    }

    actual fun disconnect() {
        manualStop = true
        pollJob?.cancel()
        pollJob = null
        try {
            process?.destroy()
            process = null
            configPath = null
        } catch (_: Exception) {}
        AppStateHolder.setServerDisconnected()
    }

    actual fun startTunnel(configJson: String): Boolean {
        return try {
            disconnect()
            val state = AppStateHolder.state.value
            connect(
                host = state.settings.serverAddr,
                port = state.settings.serverPort,
                token = state.settings.serverToken,
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
        return AppStateHolder.state.value.tunnels
            .firstOrNull { it.id == tunnelId }
            ?.status ?: TunnelStatus.OFFLINE
    }

    actual fun reloadConfig(): Boolean {
        return try {
            disconnect()
            val state = AppStateHolder.state.value
            connect(
                host = state.settings.serverAddr,
                port = state.settings.serverPort,
                token = state.settings.serverToken,
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

    /** 写配置并启动 frpc 子进程，同时启动日志读取与 admin API 轮询 */
    private fun startProcess(): Boolean {
        return try {
            stopProcessQuietly()

            val state = AppStateHolder.state.value
            val config = FrpConfigBuilder.buildConfig(state.settings, state.tunnels)
            val configDir = File("D:\\.config\\frp-kmp")
            configDir.mkdirs()
            val configFile = File(configDir, "frpc.ini")
            configFile.writeText(config)
            configPath = configFile.absolutePath

            val frpcPath = findFrpcBinary()
            if (frpcPath == null) {
                addLog(LogLevel.ERROR, "未找到 frpc 可执行文件，请将 frpc 放在系统 PATH 中或 D:\\.config\\frp-kmp\\ 目录下")
                return false
            }

            val pb = ProcessBuilder(frpcPath, "-c", configFile.absolutePath)
            pb.redirectErrorStream(true)
            val p = pb.start()
            process = p
            processStartTime = System.currentTimeMillis()

            startLogReader(p)
            startPolling()

            addLog(LogLevel.INFO, "frpc 启动中... (${frpcPath} -c ${configFile.absolutePath})")
            true
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, "启动 frpc 失败: ${e.message}")
            false
        }
    }

    /** 不触碰 manualStop 的静默停止，供内部重启使用 */
    private fun stopProcessQuietly() {
        pollJob?.cancel()
        pollJob = null
        try {
            process?.destroy()
            process = null
        } catch (_: Exception) {}
    }

    /** 读取 frpc stdout，按 FrpLogParser 解析结果回传状态 */
    private fun startLogReader(p: Process) {
        scope.launch {
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val msg = line ?: continue
                when (val event = FrpLogParser.parse(msg)) {
                    is FrpLogEvent.LoginSuccess -> {
                        AppStateHolder.setServerConnected(latencyMs.toInt())
                        addLog(LogLevel.INFO, msg)
                    }
                    is FrpLogEvent.LoginFailed -> {
                        addLog(LogLevel.ERROR, msg)
                        AppStateHolder.setServerDisconnected()
                    }
                    is FrpLogEvent.ProxyStarted -> {
                        AppStateHolder.updateTunnelStatus(event.name, TunnelStatus.ONLINE)
                        addLog(LogLevel.INFO, msg)
                    }
                    is FrpLogEvent.ProxyStartError -> {
                        AppStateHolder.updateTunnelStatus(event.name, TunnelStatus.ERROR, event.message)
                        addLog(LogLevel.ERROR, msg)
                    }
                    is FrpLogEvent.ProxyClosed -> {
                        AppStateHolder.updateTunnelStatus(event.name, TunnelStatus.OFFLINE)
                        addLog(LogLevel.INFO, msg)
                    }
                    is FrpLogEvent.Plain -> {
                        when {
                            msg.contains("error", ignoreCase = true) ||
                                msg.contains("failed", ignoreCase = true) -> addLog(LogLevel.ERROR, msg)
                            msg.contains("warn", ignoreCase = true) -> addLog(LogLevel.WARN, msg)
                            else -> addLog(LogLevel.INFO, msg)
                        }
                    }
                }
            }

            val exitCode = p.waitFor()
            if (manualStop) return@launch
            handleUnexpectedExit(exitCode)
        }
    }

    /** 进程意外退出：按设置决定是否自动重连（最多 5 次，每次间隔 5 秒） */
    private suspend fun handleUnexpectedExit(exitCode: Int) {
        addLog(LogLevel.ERROR, "frpc 进程意外退出，退出码: $exitCode")
        pollJob?.cancel()
        pollJob = null

        if (!AppStateHolder.state.value.settings.autoReconnect) {
            AppStateHolder.setServerDisconnected()
            return
        }

        var restarted = false
        for (attempt in 1..5) {
            delay(5000)
            if (manualStop) return
            AppStateHolder.incrementReconnectCount()
            addLog(LogLevel.INFO, "尝试自动重连 ($attempt/5)...")
            if (startProcess()) {
                restarted = true
                break
            }
        }
        if (!restarted) {
            addLog(LogLevel.ERROR, "自动重连失败，已放弃")
            AppStateHolder.setServerDisconnected()
        }
    }

    /** 每 3 秒轮询 frpc admin API，回传隧道状态与流量 */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val proxies = fetchAdminStatus()
                    if (proxies != null) {
                        var sumUp = 0L
                        var sumDown = 0L
                        for (proxy in proxies) {
                            val (tunnelStatus, lastError) = proxy.toTunnelStatus()
                            AppStateHolder.updateTunnelStatus(proxy.name, tunnelStatus, lastError)
                            // v0.70 起 /api/status 不再返回流量字段，有值才更新；frp 语义：out = 上行到服务端，in = 下行
                            if (proxy.trafficIn > 0 || proxy.trafficOut > 0) {
                                AppStateHolder.updateTunnelTraffic(proxy.name, proxy.trafficOut, proxy.trafficIn)
                            }
                            sumUp += proxy.trafficOut
                            sumDown += proxy.trafficIn
                        }
                        if (sumUp > 0 || sumDown > 0) {
                            AppStateHolder.updateTrafficTotals(sumUp, sumDown)
                        }
                        AppStateHolder.updateUptime((System.currentTimeMillis() - processStartTime) / 1000)
                    }
                } catch (_: Exception) {
                    // 连不上 admin 端口时静默忽略，继续下一轮
                }
                delay(3000)
            }
        }
    }

    /** GET frpc admin API，失败返回 null */
    private fun fetchAdminStatus(): List<ProxyStatus>? {
        return try {
            val conn = URL("http://127.0.0.1:7400/api/status").openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            val auth = Base64.getEncoder().encodeToString("admin:admin".toByteArray())
            conn.setRequestProperty("Authorization", "Basic $auth")
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            FrpAdminStatus.parse(body)
        } catch (_: Exception) {
            null
        }
    }

    /** TCP 连接计时，失败返回 -1 */
    private fun measureLatency(host: String, port: Int): Long {
        return try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 3000)
            socket.close()
            System.currentTimeMillis() - start
        } catch (_: Exception) {
            -1L
        }
    }

    private fun addLog(level: LogLevel, message: String) {
        AppStateHolder.addLog(
            LogEntry(
                level = level,
                message = message,
                timestamp = System.currentTimeMillis(),
            )
        )
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
