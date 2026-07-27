package cn.lemwood.platform

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/** Desktop 系统通知：优先走 AWT SystemTray，不支持或出错时静默降级 */
actual class AppNotifier actual constructor(context: Any?) {

    // TrayIcon 只创建一次并复用；环境不支持时保持 null
    private var trayIcon: TrayIcon? = null
    private var trayChecked = false

    actual fun notify(title: String, message: String, notificationId: Int) {
        try {
            val icon = ensureTrayIcon() ?: return
            icon.displayMessage(title, message, TrayIcon.MessageType.INFO)
        } catch (e: Exception) {
            System.err.println("发送系统通知失败: ${e.message}")
        }
    }

    /** 惰性初始化托盘图标，任何失败都返回 null 并保持静默 */
    @Synchronized
    private fun ensureTrayIcon(): TrayIcon? {
        if (trayChecked) return trayIcon
        trayChecked = true
        try {
            if (!SystemTray.isSupported()) return null
            // 没有应用图标资源，用空白图兜底
            val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
            val icon = TrayIcon(image, "Frp Tunnel")
            icon.isImageAutoSize = true
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
            createdIcons.add(icon)
        } catch (e: Exception) {
            System.err.println("系统托盘不可用，通知已禁用: ${e.message}")
        }
        return trayIcon
    }

    companion object {
        // 记录所有创建过的托盘图标，退出时由 TrayController 统一清理
        private val createdIcons = java.util.concurrent.CopyOnWriteArrayList<TrayIcon>()

        /** 移除全部已创建的托盘图标，任何异常静默吞掉 */
        fun removeTrayIcons() {
            createdIcons.forEach { icon ->
                runCatching { SystemTray.getSystemTray().remove(icon) }
            }
            createdIcons.clear()
        }
    }
}
