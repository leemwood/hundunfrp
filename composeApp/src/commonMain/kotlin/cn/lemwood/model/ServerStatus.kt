package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerStatus(
    val connected: Boolean = false,
    val server: String = "",
    val latencyMs: Int = -1,
    val uptimeSeconds: Long = 0L,
    val reconnectCount: Int = 0
)
