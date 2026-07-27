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
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * Android frp 控制器（JNI 方案）
 * frpc 通过 [FrpcNative] 在 App 进程内运行，无子进程；
 * 日志经 frpc 写入 <filesDir>/frp/frpc.log，本类 tail 增量解析；
 * 隧道状态/流量仍走 frpc admin API（127.0.0.1:7400）轮询。
 */
actual class FrpController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    private var logTailJob: Job? = null

    @Volatile
    private var manualStop = false

    @Volatile
    private var frpcStartTime = 0L

    @Volatile
    private var lastLatencyMs = -1

    @Volatile
    private var reconnectAttempts = 0

    @Volatile
    private var pollFailures = 0

    @Volatile
    private var lastPollError: String? = null

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

        if (!FrpcNative.available) {
            AppStateHolder.addLog(
                LogEntry(
                    level = LogLevel.ERROR,
                    message = "frpc 原生库加载失败，当前设备可能不是 arm64-v8a",
                    timestamp = System.currentTimeMillis(),
                )
            )
            return false
        }

        return try {
            // 静默停掉旧实例，避免触发自动重连与状态回写
            manualStop = true
            stopFrpc()
            manualStop = false

            // 启动前 TCP 计时测延迟，供登录成功后回写
            lastLatencyMs = measureLatencyMs(host, port)

            val state = AppStateHolder.state.value
            val configDir = File(ctx.filesDir, "frp")
            configDir.mkdirs()

            // 截断旧日志，tail 从头开始读
            val logFile = File(configDir, "frpc.log")
            logFile.writeText("")

            // log.* 必须落在 [common] 段：插到第一个隧道段之前，无隧道则追加到末尾
            // 注意：frp legacy ini 用下划线键（log_file/log_way），圆点键 log.to 是 toml 语法，ini 里会被忽略
            val baseConfig = FrpConfigBuilder.buildConfig(state.settings, state.tunnels)
            val logConfig = "log_way = file\nlog_file = ${logFile.absolutePath}\nlog_level = info\n"
            val config = when (val idx = baseConfig.indexOf("\n[")) {
                -1 -> baseConfig + logConfig
                else -> baseConfig.substring(0, idx + 1) + logConfig + baseConfig.substring(idx + 1)
            }
            val configFile = File(configDir, "frpc.ini")
            configFile.writeText(config)

            val rc = FrpcNative.nativeStart(configFile.absolutePath)
            if (rc != 0) {
                AppStateHolder.addLog(
                    LogEntry(
                        level = LogLevel.ERROR,
                        message = "frpc 启动失败，返回码: $rc",
                        timestamp = System.currentTimeMillis(),
                    )
                )
                return false
            }

            frpcStartTime = System.currentTimeMillis()
            pollFailures = 0

            startLogTail(logFile)
            startPolling()

            AppStateHolder.addLog(
                LogEntry(
                    level = LogLevel.INFO,
                    message = "frpc 启动中（进程内 JNI 模式）...",
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
        stopFrpc()
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

    private fun stopFrpc() {
        try {
            pollingJob?.cancel()
            pollingJob = null
            logTailJob?.cancel()
            logTailJob = null
            if (FrpcNative.available) {
                FrpcNative.nativeStop()
            }
        } catch (_: Exception) {}
    }

    /**
     * tail frpc.log：记录 offset 增量读取，按行切分后走 FrpLogParser 管线。
     * 未遇到换行符的残行留在缓冲里，等下一轮补全。
     */
    private fun startLogTail(logFile: File) {
        logTailJob?.cancel()
        logTailJob = scope.launch {
            var offset = 0L
            val pending = StringBuilder()
            while (isActive) {
                try {
                    val len = logFile.length()
                    if (len > offset) {
                        RandomAccessFile(logFile, "r").use { raf ->
                            raf.seek(offset)
                            val bytes = ByteArray((len - offset).toInt())
                            raf.readFully(bytes)
                            pending.append(String(bytes, Charsets.UTF_8))
                        }
                        offset = len
                        var newlineIdx = pending.indexOf('\n')
                        while (newlineIdx >= 0) {
                            val line = pending.substring(0, newlineIdx).trimEnd('\r')
                            pending.delete(0, newlineIdx + 1)
                            if (line.isNotBlank()) {
                                handleLogEvent(FrpLogParser.parse(line), line)
                            }
                            newlineIdx = pending.indexOf('\n')
                        }
                    }
                } catch (_: Exception) {}
                delay(LOG_TAIL_INTERVAL_MS)
            }
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

    /**
     * admin API 连续失败判定为 frpc 异常：autoReconnect 时重启（nativeStop + nativeStart），
     * 最多 MAX_RECONNECT_ATTEMPTS 次，间隔 5 秒；LoginSuccess 时重置计数。
     */
    private suspend fun handleUnexpectedExit() {
        val settings = AppStateHolder.state.value.settings
        if (settings.autoReconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            AppStateHolder.incrementReconnectCount()
            AppStateHolder.addLog(
                LogEntry(
                    level = LogLevel.WARN,
                    message = "frpc 连接异常，5 秒后重连（第 $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS 次）",
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
                if (pollAdminStatus()) {
                    pollFailures = 0
                } else {
                    pollFailures++
                    // 首次失败时记录原因，便于诊断（cleartext 拦截/端口未起等）
                    if (pollFailures == 1) {
                        AppStateHolder.addLog(
                            LogEntry(
                                level = LogLevel.WARN,
                                message = "admin API 轮询失败: $lastPollError",
                                timestamp = System.currentTimeMillis(),
                            )
                        )
                    }
                    if (pollFailures >= MAX_POLL_FAILURES && !manualStop) {
                        pollFailures = 0
                        handleUnexpectedExit()
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /** @return admin API 是否可达 */
    private fun pollAdminStatus(): Boolean {
        val json = fetchAdminStatus() ?: return false
        val proxies = FrpAdminStatus.parse(json)
        var totalUp = 0L
        var totalDown = 0L
        for (proxy in proxies) {
            val (tunnelStatus, lastError) = proxy.toTunnelStatus()
            AppStateHolder.updateTunnelStatus(proxy.name, tunnelStatus, lastError)
            // v0.70 起 /api/status 不再返回流量字段，有值才更新；流量方向：up = trafficOut，down = trafficIn
            if (proxy.trafficIn > 0 || proxy.trafficOut > 0) {
                AppStateHolder.updateTunnelTraffic(proxy.name, proxy.trafficOut, proxy.trafficIn)
            }
            totalUp += proxy.trafficOut
            totalDown += proxy.trafficIn
        }
        if (totalUp > 0 || totalDown > 0) {
            AppStateHolder.updateTrafficTotals(totalUp, totalDown)
        }
        AppStateHolder.updateUptime((System.currentTimeMillis() - frpcStartTime) / 1000)
        return true
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
                lastPollError = "HTTP ${conn.responseCode}"
                null
            }
        } catch (e: Exception) {
            lastPollError = e.message ?: e.javaClass.simpleName
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

    companion object {
        private const val ADMIN_STATUS_URL = "http://127.0.0.1:7400/api/status"
        private const val POLL_INTERVAL_MS = 3000L
        private const val LOG_TAIL_INTERVAL_MS = 1000L
        private const val RECONNECT_DELAY_MS = 5000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val MAX_POLL_FAILURES = 5
    }
}
