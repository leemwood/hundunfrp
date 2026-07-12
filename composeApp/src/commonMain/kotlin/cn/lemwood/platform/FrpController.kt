package cn.lemwood.platform

import cn.lemwood.model.TunnelStatus

expect class FrpController {
    fun connect(host: String, port: Int, token: String): Boolean
    fun disconnect()
    fun startTunnel(configJson: String): Boolean
    fun stopTunnel(tunnelId: String): Boolean
    fun getTunnelStatus(tunnelId: String): TunnelStatus
    fun reloadConfig(): Boolean
}
