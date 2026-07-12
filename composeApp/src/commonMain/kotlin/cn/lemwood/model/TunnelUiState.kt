package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
data class TunnelUiState(
    val id: String,
    val name: String,
    val type: TunnelType,
    val localAddr: String,
    val localPort: Int,
    val remotePort: Int,
    val status: TunnelStatus = TunnelStatus.OFFLINE,
    val enabled: Boolean = false,
    val traffic: Traffic = Traffic(),
    val lastError: String? = null,
    val encryption: Boolean = false,
    val compression: Boolean = false,
    val tls: Boolean = false,
    val customDomain: String? = null,
    val httpUser: String? = null,
    val httpPassword: String? = null
)
