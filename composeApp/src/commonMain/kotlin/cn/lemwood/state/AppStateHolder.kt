package cn.lemwood.state

import cn.lemwood.model.AppSettings
import cn.lemwood.model.AppState
import cn.lemwood.model.LogEntry
import cn.lemwood.model.LogLevel
import cn.lemwood.model.ServerStatus
import cn.lemwood.model.Traffic
import cn.lemwood.model.TunnelStatus
import cn.lemwood.model.TunnelType
import cn.lemwood.model.TunnelUiState
import cn.lemwood.model.UIState
import cn.lemwood.data.SettingsStore
import cn.lemwood.data.TunnelConfigStore
import cn.lemwood.data.createSettingsStore
import cn.lemwood.data.createTunnelConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object AppStateHolder {
    val state = MutableStateFlow(initialAppState())

    var settingsStore: SettingsStore? = null
        private set
    var tunnelStore: TunnelConfigStore? = null
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var initialized = false

    fun init(context: Any? = null) {
        if (initialized) return
        initialized = true
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
            // 首次启动或配置缺失时，确保默认值落盘，便于后续验证与恢复
            if (settingsResult.isSuccess) persistSettings()
            if (tunnelsResult.isSuccess) persistTunnels()
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
    }

    fun updateTunnel(tunnel: TunnelUiState) {
        state.update { current ->
            current.copy(
                tunnels = current.tunnels.map { if (it.id == tunnel.id) tunnel else it }
            )
        }
        persistTunnels()
    }

    fun deleteTunnel(id: String) {
        state.update { current ->
            current.copy(tunnels = current.tunnels.filter { it.id != id })
        }
        persistTunnels()
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

    private fun initialAppState(): AppState = AppState(
        tunnels = listOf(
            TunnelUiState(
                id = "mc-server",
                name = "mc-server",
                type = TunnelType.TCP,
                localAddr = "127.0.0.1",
                localPort = 25565,
                remotePort = 25565,
                status = TunnelStatus.ONLINE,
                enabled = true,
                traffic = Traffic(up = 1_228_800, down = 3_670_400)
            ),
            TunnelUiState(
                id = "ssh-dev",
                name = "ssh-dev",
                type = TunnelType.TCP,
                localAddr = "127.0.0.1",
                localPort = 22,
                remotePort = 7022,
                status = TunnelStatus.OFFLINE,
                enabled = false
            ),
            TunnelUiState(
                id = "web-demo",
                name = "web-demo",
                type = TunnelType.HTTP,
                localAddr = "127.0.0.1",
                localPort = 8080,
                remotePort = 80,
                status = TunnelStatus.CONNECTING,
                enabled = true
            ),
            TunnelUiState(
                id = "db-tunnel",
                name = "db-tunnel",
                type = TunnelType.TCP,
                localAddr = "127.0.0.1",
                localPort = 3306,
                remotePort = 7306,
                status = TunnelStatus.ERROR,
                enabled = true,
                lastError = "dial tcp 127.0.0.1:3306: connect: connection refused"
            ),
            TunnelUiState(
                id = "udp-voice",
                name = "udp-voice",
                type = TunnelType.UDP,
                localAddr = "127.0.0.1",
                localPort = 5060,
                remotePort = 5060,
                status = TunnelStatus.OFFLINE,
                enabled = false
            )
        ),
        serverStatus = ServerStatus(
            connected = true,
            server = "frp.example.com:7000",
            latencyMs = 23,
            uptimeSeconds = 45_240,
            reconnectCount = 0
        ),
        settings = AppSettings(
            serverAddr = "frp.example.com",
            serverPort = 7000,
            serverToken = "",
            autoStart = true,
            autoReconnect = true,
            notifications = false,
            timeoutSeconds = 30,
            logLevel = LogLevel.INFO,
            theme = "system",
            dynamicColor = false
        ),
        logs = listOf(
            LogEntry(
                level = LogLevel.INFO,
                message = "连接成功",
                timestamp = 1_719_812_340_000L,
            ),
            LogEntry(
                level = LogLevel.WARN,
                message = "ssh-dev 断开 | 重连 1/5",
                timestamp = 1_719_812_350_000L,
            ),
            LogEntry(
                level = LogLevel.ERROR,
                message = "连接超时",
                timestamp = 1_719_812_352_000L,
            ),
            LogEntry(
                level = LogLevel.INFO,
                message = "mc-server 流量异常恢复",
                timestamp = 1_719_812_400_000L,
            ),
        ),
        uiState = UIState.Idle
    )
}
