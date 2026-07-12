package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
enum class TunnelType(val displayName: String) {
    TCP("TCP"),
    UDP("UDP"),
    HTTP("HTTP"),
    HTTPS("HTTPS"),
    STCP("STCP"),
    XTCP("XTCP")
}
