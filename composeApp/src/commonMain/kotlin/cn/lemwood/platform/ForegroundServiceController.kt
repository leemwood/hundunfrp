package cn.lemwood.platform

expect object ForegroundServiceController {
    /** 连接成功后调用：Android 启动前台服务保活，其他平台 no-op */
    fun onServerConnected(server: String)

    /** 断开后调用：停止前台服务，其他平台 no-op */
    fun onServerDisconnected()
}
