package cn.lemwood.platform

/** Desktop 无需前台服务保活，no-op */
actual object ForegroundServiceController {
    actual fun onServerConnected(server: String) = Unit
    actual fun onServerDisconnected() = Unit
}
