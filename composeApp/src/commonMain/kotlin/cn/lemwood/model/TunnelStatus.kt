package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
enum class TunnelStatus {
    ONLINE,
    OFFLINE,
    CONNECTING,
    ERROR
}
