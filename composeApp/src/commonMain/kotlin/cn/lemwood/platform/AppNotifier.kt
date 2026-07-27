package cn.lemwood.platform

expect class AppNotifier(context: Any? = null) {
    /** 发送一条系统通知；实现端在权限/环境不支持时静默降级，不得抛异常 */
    fun notify(title: String, message: String, notificationId: Int = 1)
}
