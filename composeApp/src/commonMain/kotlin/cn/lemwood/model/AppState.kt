package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
data class AppState(
    val tunnels: List<TunnelUiState> = emptyList(),
    val serverStatus: ServerStatus = ServerStatus(),
    val logs: List<LogEntry> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val uiState: UIState = UIState.Idle,
    val notifications: List<Notification> = emptyList(),
    val trafficHistory: List<TrafficSample> = emptyList()
)
