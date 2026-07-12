package cn.lemwood.platform

import cn.lemwood.model.TunnelStatus

actual class FrpController {
    actual fun connect(host: String, port: Int, token: String): Boolean = true
    actual fun disconnect() = Unit
    actual fun startTunnel(configJson: String): Boolean = true
    actual fun stopTunnel(tunnelId: String): Boolean = true
    actual fun getTunnelStatus(tunnelId: String): TunnelStatus = TunnelStatus.OFFLINE
    actual fun reloadConfig(): Boolean = true
}
