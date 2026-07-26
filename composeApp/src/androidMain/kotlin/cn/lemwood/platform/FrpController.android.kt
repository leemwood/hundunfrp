package cn.lemwood.platform

import android.content.Context
import android.util.Base64
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
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

actual class FrpController {
    private var process: java.lang.Process? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    @Volatile
    private var manualStop = false

    @Volatile
    private var processStartTime = 0L

    @Volatile
    private var lastLatencyMs = -1

    @Volatile
    private var reconnectAttempts = 0

    private val context: Context?
        get() = AndroidFrpContext.appContext

    actual fun connect(host: String, port: Int, token: String): Boolean {
        val ctx = context
        if (ctx == null) {
            AppStateHolder.addLog(
                LogEntry(
                    level = LogLevel.ERROR,
                    message = "FrpController 未初始化，缺少 Context",
                    timestamp = System.currentTimeMillis(),
                )
            )
            return false
        }

        return try {
            // 静默停掉旧进程，避免触发自动重连与状态回写
            manualStop = true
            stopProcess()
            manualStop = false

            // 启动前 TCP 计时测延迟，供登录成功后回写
            lastLatencyMs = measureLatencyMs(host, port)

            val state = AppStateHolder.state.value
            val config = FrpConfigBuilder.buildConfig(state.settings, state.tunnels)
            val configDir = File(ctx.filesDir, "frp")
            configDir.mkdirs()
            val configFile = File(configDir, "frpc.ini")
            configFile.writeText(config)

            val frpcBinary = extractFrpcBinary(ctx) ?: run {
                AppStateHolder.addLog(
                    LogEntry(
                        level = LogLevel.ERROR,
                        message = "未找到 frpc 二进制文件，请将对应架构的 frpc 放入 jniLibs/",
                        timestamp = System.currentTimeMillis(),
                    )
                )
                return false
            }
            frpcBinary.setExecutable(true)

            val pb = ProcessBuilder(frpcBinary.absolutePath, "-c", configFile.absolutePath)
            pb.directory(configDir)
            pb.redirectErrorStream(true)
            val p = pb.start()
            process = p
            processStartTime = System.currentTimeMillis()

            startLogReader(p)
            startPolling()

            AppStateHolder.addLog(
                LogEntry(
                    level = LogLevel.INFO,
                    message = "frpc 启动中...",
                    timestamp = System.currentTimeMillis(),
                )
            )
            true
        } catch (e: Exception) {
            AppStateHolder.addLog(
                LogEntry(
                    level = LogLevel.ERROR,
                    message = "启动 frpc 失败: ${e.message}",
                    timestamp = System.currentTimeMillis(),
                )
            )
            false
        }
    }

    actual fun disconnect() {
        manualStop = true
        stopProcess()
        AppStateHolder.setServerDisconnected()
    }

    actual fun startTunnel(configJson: String): Boolean {
        return try {
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
        return AppStateHolder.state.value.tunnels
            .firstOrNull { it.id == tunnelId }?.status ?: TunnelStatus.OFFLINE
    }

    actual fun reloadConfig(): Boolean {
        val ctx = context ?: return false
        val state = AppStateHolder.state.value
        return connect(
            host = state.settings.serverAddr,
            port = state.settings.serverPort,
            token = state.settings.serverToken
        )
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

    private fun stopProcess() {
        try {
            pollingJob?.cancel()
            pollingJob = null
            process?.destroy()
            process = null
        } catch (_: Exception) {}
    }

    private fun startLogReader(p: java.lang.Process) {
        scope.launch {
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val msg = line ?: continue
                handleLogEvent(FrpLogParser.parse(msg), msg)
            }
            val exitCode = p.waitFor()
            pollingJob?.cancel()
            if (exitCode != 0) {
                AppStateHolder.addLog(
                    LogEntry(
                        level = LogLevel.ERROR,
                        message = "frpc 进程退出，退出码: $exitCode",
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }
            // 主动停止或进程已被新实例替换时不做善后处理
            if (manualStop || p !== process) return@launch
            handleUnexpectedExit()
        }
    }

    private fun handleLogEvent(event: FrpLogEvent, rawLine: String) {
        when (event) {
            is FrpLogEvent.LoginSuccess -> {
                reconnectAttempts = 0
                AppStateHolder.setServerConnected(lastLatencyMs)
                AppStateHolder.addLog(
                    LogEntry(
                        level = LogLevel.INFO,
                        message = rawLine,
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }
            is FrpLogEvent.LoginFailed -> {
                AppStateHolder.addLog(
                    LogEntry(
                        level = LogLevel.ERROR,
                        message = event.message,
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }
            is FrpLogEvent.ProxyStarted -> {
                AppStateHolder.updateTunnelStatus(event.name, TunnelStatus.ONLINE)
                AppStateHolder.addLog(
                    LogEntry(
                        level = LogLevel.INFO,
                        message = rawLine,
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }
            is FrpLogEvent.ProxyStartError -> {
                AppStateHolder.updateTunnelStatus(event.name, TunnelStatus.ERROR, event.message)
                AppStateHolder.addLog(
                    LogEntry(
                        level = LogLevel.ERROR,
                        message = event.message,
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }
            is FrpLogEvent.ProxyClosed -> {
                AppStateHolder.updateTunnelStatus(event.name, TunnelStatus.OFFLINE)
                AppStateHolder.addLog(
                    LogEntry(
                        level = LogLevel.INFO,
                        message = rawLine,
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }
            is FrpLogEvent.Plain -> {
                val level = when {
                    rawLine.contains("error", ignoreCase = true) ||
                        rawLine.contains("failed", ignoreCase = true) -> LogLevel.ERROR
                    rawLine.contains("warn", ignoreCase = true) -> LogLevel.WARN
                    else -> LogLevel.INFO
                }
                AppStateHolder.addLog(
                    LogEntry(
                        level = level,
                        message = rawLine,
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }
        }
    }

    private suspend fun handleUnexpectedExit() {
        val settings = AppStateHolder.state.value.settings
        if (settings.autoReconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            AppStateHolder.incrementReconnectCount()
            AppStateHolder.addLog(
                LogEntry(
                    level = LogLevel.WARN,
                    message = "frpc 意外退出，5 秒后重连（第 $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS 次）",
                    timestamp = System.currentTimeMillis(),
                )
            )
            delay(RECONNECT_DELAY_MS)
            if (!manualStop) {
                connect(
                    host = settings.serverAddr,
                    port = settings.serverPort,
                    token = settings.serverToken
                )
            }
        } else {
            if (settings.autoReconnect) {
                AppStateHolder.addLog(
                    LogEntry(
                        level = LogLevel.ERROR,
                        message = "已达最大重连次数，放弃重连",
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }
            AppStateHolder.setServerDisconnected()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                pollAdminStatus()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun pollAdminStatus() {
        val json = fetchAdminStatus() ?: return
        val proxies = FrpAdminStatus.parse(json)
        var totalUp = 0L
        var totalDown = 0L
        for (proxy in proxies) {
            AppStateHolder.updateTunnelStatus(
                proxy.name,
                if (proxy.online) TunnelStatus.ONLINE else TunnelStatus.OFFLINE,
            )
            // 流量方向：up = trafficOut，down = trafficIn
            AppStateHolder.updateTunnelTraffic(proxy.name, proxy.trafficOut, proxy.trafficIn)
            totalUp += proxy.trafficOut
            totalDown += proxy.trafficIn
        }
        AppStateHolder.updateTrafficTotals(totalUp, totalDown)
        AppStateHolder.updateUptime((System.currentTimeMillis() - processStartTime) / 1000)
    }

    private fun fetchAdminStatus(): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(ADMIN_STATUS_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            val auth = Base64.encodeToString("admin:admin".toByteArray(), Base64.NO_WRAP)
            conn.setRequestProperty("Authorization", "Basic $auth")
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun measureLatencyMs(host: String, port: Int): Int {
        return try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 5000)
            socket.close()
            (System.currentTimeMillis() - start).toInt()
        } catch (_: Exception) {
            -1
        }
    }

    private fun extractFrpcBinary(ctx: Context): File? {
        val arch = android.os.Build.SUPPORTED_ABIS.firstOrNull()?.takeIf {
            it in listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        } ?: return null

        val binaryFile = File(ctx.filesDir, "frp/frpc-$arch")
        if (binaryFile.exists() && binaryFile.canExecute()) {
            return binaryFile
        }

        try {
            val libPath = "jniLibs/$arch/"
            val libNames = listOf("libfrpc.so", "frpc")

            for (libName in libNames) {
                try {
                    val inputStream = ctx.assets.open("$libPath$libName")
                    inputStream.use { input ->
                        FileOutputStream(binaryFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    binaryFile.setExecutable(true)
                    return binaryFile
                } catch (_: Exception) {}
            }

            try {
                val libDir = File(ctx.applicationInfo.nativeLibraryDir, "../$arch/")
                for (libName in libNames) {
                    val libFile = File(libDir, libName)
                    if (libFile.exists()) {
                        libFile.copyTo(binaryFile, overwrite = true)
                        binaryFile.setExecutable(true)
                        return binaryFile
                    }
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}

        return null
    }

    companion object {
        private const val ADMIN_STATUS_URL = "http://127.0.0.1:7400/api/status"
        private const val POLL_INTERVAL_MS = 3000L
        private const val RECONNECT_DELAY_MS = 5000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }
}
