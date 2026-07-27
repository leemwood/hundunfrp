package cn.lemwood.state

import cn.lemwood.model.AppSettings
import cn.lemwood.model.AppState
import cn.lemwood.model.LogEntry
import cn.lemwood.model.ServerStatus
import cn.lemwood.model.TrafficSample
import cn.lemwood.model.TunnelStatus
import cn.lemwood.model.TunnelUiState
import cn.lemwood.model.UIState
import cn.lemwood.data.SettingsStore
import cn.lemwood.data.TunnelConfigStore
import cn.lemwood.data.createSettingsStore
import cn.lemwood.data.createTunnelConfigStore
import cn.lemwood.platform.AppNotifier
import cn.lemwood.platform.ForegroundServiceController
import cn.lemwood.platform.FrpController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object AppStateHolder {
    val state = MutableStateFlow(AppState())

    var settingsStore: SettingsStore? = null
        private set
    var tunnelStore: TunnelConfigStore? = null
        private set

    private val controller by lazy { FrpController() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var initialized = false

    // 平台 context（Android 为 Activity），用于惰性创建系统通知器
    private var platformContext: Any? = null
    private val notifier by lazy { AppNotifier(platformContext) }

    /** 发送系统通知；遵守 notifications 开关，任何异常静默吞掉 */
    private fun sendNotification(title: String, message: String, notificationId: Int = 1) {
        if (!state.value.settings.notifications) return
        runCatching { notifier.notify(title, message, notificationId) }
    }

    // 上一次流量总量，用于计算速率
    private var lastTotalUp: Long = 0L
    private var lastTotalDown: Long = 0L
    private var lastSampleTime: Long = 0L

    fun init(context: Any? = null) {
        if (initialized) return
        initialized = true
        platformContext = context
        settingsStore = createSettingsStore(context)
        tunnelStore = createTunnelConfigStore(context)
        scope.launch {
            val settingsResult = runCatching { settingsStore?.load() }
            val tunnelsResult = runCatching { tunnelStore?.load() }
            val loadedSettings = settingsResult.getOrNull() ?: AppSettings()
            val loadedTunnels = tunnelsResult.getOrNull() ?: emptyList()
            state.value = state.value.copy(
                settings = loadedSettings,
                tunnels = loadedTunnels,
                uiState = UIState.Idle
            )
            // 仅真正首次启动（尚无持久化数据）时落盘默认值；load 失败时不覆盖
            val settingsHasData = runCatching { settingsStore?.hasData() }.getOrNull() ?: true
            val tunnelsHasData = runCatching { tunnelStore?.hasData() }.getOrNull() ?: true
            if (settingsResult.isSuccess && !settingsHasData) persistSettings()
            if (tunnelsResult.isSuccess && !tunnelsHasData) persistTunnels()
        }
    }

    private fun persistTunnels() {
        scope.launch { tunnelStore?.save(state.value.tunnels) }
    }

    private fun persistSettings() {
        scope.launch { settingsStore?.save(state.value.settings) }
    }

    fun addTunnel(tunnel: TunnelUiState) {
        state.update { current ->
            val filtered = current.tunnels.filter { it.id != tunnel.id }
            current.copy(tunnels = filtered + tunnel)
        }
        persistTunnels()
        // 已连接时重启 frpc 使新配置生效（防抖合并连续变更）
        if (state.value.serverStatus.connected) scheduleReconnect()
    }

    fun updateTunnel(tunnel: TunnelUiState) {
        state.update { current ->
            current.copy(
                tunnels = current.tunnels.map { if (it.id == tunnel.id) tunnel else it }
            )
        }
        persistTunnels()
        if (state.value.serverStatus.connected) scheduleReconnect()
    }

    fun deleteTunnel(id: String) {
        state.update { current ->
            current.copy(tunnels = current.tunnels.filter { it.id != id })
        }
        persistTunnels()
        if (state.value.serverStatus.connected) scheduleReconnect()
    }

    fun toggleTunnel(id: String) {
        state.update { current ->
            val updated = current.tunnels.map { tunnel ->
                if (tunnel.id == id) {
                    val newEnabled = !tunnel.enabled
                    tunnel.copy(
                        enabled = newEnabled,
                        status = if (newEnabled) TunnelStatus.CONNECTING else TunnelStatus.OFFLINE
                    )
                } else {
                    tunnel
                }
            }
            current.copy(tunnels = updated)
        }
        persistTunnels()
        // 有启用隧道则（重）连接，全部停用则断开
        val current = state.value
        if (current.tunnels.any { it.enabled }) {
            if (current.settings.serverAddr.isNotBlank()) scheduleReconnect()
        } else {
            reconnectJob?.cancel()
            disconnectServer()
        }
    }

    private var reconnectJob: kotlinx.coroutines.Job? = null

    /**
     * 防抖重启 frpc：短时间内的多次配置变更（新增/编辑/开关隧道）合并为一次重启，
     * 避免每次操作都掉线重连。
     */
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            kotlinx.coroutines.delay(800)
            connectServer()
        }
    }

    fun setServerStatus(status: ServerStatus) {
        state.update { it.copy(serverStatus = status) }
    }

    fun addLog(entry: LogEntry) {
        state.update { current ->
            val logs = (current.logs + entry).takeLast(200)
            current.copy(logs = logs)
        }
    }

    fun clearLogs() {
        state.update { it.copy(logs = emptyList()) }
    }

    fun updateSettings(settings: AppSettings) {
        state.update { it.copy(settings = settings) }
        persistSettings()
    }

    fun replaceTunnels(tunnels: List<TunnelUiState>) {
        state.update { it.copy(tunnels = tunnels) }
        persistTunnels()
    }

    fun replaceSettings(settings: AppSettings) {
        state.update { it.copy(settings = settings) }
        persistSettings()
    }

    fun resetToDefaults() {
        val defaultSettings = AppSettings()
        state.value = state.value.copy(
            tunnels = emptyList(),
            settings = defaultSettings,
            serverStatus = ServerStatus(),
            logs = emptyList(),
            uiState = UIState.Idle
        )
        scope.launch {
            runCatching { tunnelStore?.save(emptyList()) }
            runCatching { settingsStore?.save(defaultSettings) }
        }
    }

    fun persistSettingsForNow() {
        persistSettings()
        persistTunnels()
    }

    fun setUiState(uiState: UIState) {
        state.update { it.copy(uiState = uiState) }
    }

    /** 使用当前 settings 连接 frp 服务器（重启 frpc 子进程） */
    fun connectServer() {
        val settings = state.value.settings
        state.update { current ->
            current.copy(
                serverStatus = current.serverStatus.copy(
                    connected = false,
                    server = "${settings.serverAddr}:${settings.serverPort}",
                    latencyMs = -1,
                    uptimeSeconds = 0L
                )
            )
        }
        scope.launch(Dispatchers.IO) {
            runCatching {
                controller.connect(
                    host = settings.serverAddr,
                    port = settings.serverPort,
                    token = settings.serverToken
                )
            }
        }
    }

    /** 断开 frp 连接，所有启用隧道置为离线 */
    fun disconnectServer() {
        state.update { current ->
            current.copy(
                serverStatus = current.serverStatus.copy(connected = false, latencyMs = 0),
                tunnels = current.tunnels.map { tunnel ->
                    if (tunnel.enabled) tunnel.copy(status = TunnelStatus.OFFLINE) else tunnel
                }
            )
        }
        scope.launch(Dispatchers.IO) {
            runCatching { controller.disconnect() }
        }
        ForegroundServiceController.onServerDisconnected()
    }

    fun updateTunnelStatus(id: String, status: TunnelStatus, lastError: String? = null) {
        state.update { current ->
            current.copy(
                tunnels = current.tunnels.map { tunnel ->
                    if (tunnel.id == id) tunnel.copy(status = status, lastError = lastError) else tunnel
                }
            )
        }
        // 隧道进入错误状态时通知，id 取 hashCode 避免不同隧道互相覆盖
        if (status == TunnelStatus.ERROR && !lastError.isNullOrBlank()) {
            val tunnelName = state.value.tunnels.firstOrNull { it.id == id }?.name ?: id
            sendNotification(
                title = "隧道错误：$tunnelName",
                message = lastError.take(120),
                notificationId = id.hashCode()
            )
        }
    }

    fun updateTunnelTraffic(id: String, up: Long, down: Long) {
        state.update { current ->
            current.copy(
                tunnels = current.tunnels.map { tunnel ->
                    if (tunnel.id == id) {
                        tunnel.copy(traffic = tunnel.traffic.copy(up = up, down = down))
                    } else {
                        tunnel
                    }
                }
            )
        }
    }

    /** 更新流量总量，并根据与上一条样本的差值计算速率追加到历史 */
    fun updateTrafficTotals(up: Long, down: Long) {
        val now = System.currentTimeMillis()
        val lastSample = state.value.trafficHistory.lastOrNull()
        val intervalSec = (now - lastSampleTime) / 1000L
        val (upRate, downRate) = if (intervalSec > 0 && lastSampleTime > 0L) {
            val upDelta = (up - lastTotalUp).coerceAtLeast(0L) / intervalSec
            val downDelta = (down - lastTotalDown).coerceAtLeast(0L) / intervalSec
            upDelta to downDelta
        } else {
            // 间隔无效时沿用上一次速率，没有则为 0
            (lastSample?.upBytesPerSec ?: 0L) to (lastSample?.downBytesPerSec ?: 0L)
        }
        lastTotalUp = up
        lastTotalDown = down
        lastSampleTime = now
        state.update { current ->
            val history = (current.trafficHistory + TrafficSample(
                upBytesPerSec = upRate,
                downBytesPerSec = downRate,
                timestamp = now
            )).takeLast(120)
            current.copy(
                serverStatus = current.serverStatus.copy(
                    totalUploadBytes = up,
                    totalDownloadBytes = down
                ),
                trafficHistory = history
            )
        }
    }

    fun setServerConnected(latencyMs: Int) {
        val settings = state.value.settings
        val serverAddr = "${settings.serverAddr}:${settings.serverPort}"
        state.update { current ->
            current.copy(
                serverStatus = current.serverStatus.copy(
                    connected = true,
                    server = serverAddr,
                    latencyMs = latencyMs
                )
            )
        }
        sendNotification("已连接到服务器", "$serverAddr，延迟 ${latencyMs}ms")
        ForegroundServiceController.onServerConnected(serverAddr)
    }

    fun setServerDisconnected() {
        state.update { current ->
            current.copy(
                serverStatus = current.serverStatus.copy(connected = false, latencyMs = 0),
                tunnels = current.tunnels.map { it.copy(status = TunnelStatus.OFFLINE) }
            )
        }
        sendNotification("服务器连接已断开", "与 frp 服务器的连接已断开")
        ForegroundServiceController.onServerDisconnected()
    }

    fun updateUptime(seconds: Long) {
        state.update { current ->
            current.copy(serverStatus = current.serverStatus.copy(uptimeSeconds = seconds))
        }
    }

    fun incrementReconnectCount() {
        state.update { current ->
            current.copy(
                serverStatus = current.serverStatus.copy(
                    reconnectCount = current.serverStatus.reconnectCount + 1
                )
            )
        }
    }
}
