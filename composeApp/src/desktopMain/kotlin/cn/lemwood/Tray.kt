package cn.lemwood

import cn.lemwood.platform.AppNotifier
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/** Desktop 系统托盘：关窗最小化到托盘，菜单提供「显示主窗口 / 退出」 */
object TrayController {

    private var trayIcon: TrayIcon? = null

    val isSupported: Boolean
        get() = runCatching {
            !GraphicsEnvironment.isHeadless() && SystemTray.isSupported()
        }.getOrDefault(false)

    /** 安装托盘图标；环境不支持或出错时返回 false 静默降级 */
    @Synchronized
    fun install(onShow: () -> Unit, onExit: () -> Unit): Boolean {
        if (trayIcon != null) return true
        if (!isSupported) return false
        return runCatching {
            val popup = PopupMenu()
            val showItem = MenuItem("显示主窗口")
            showItem.addActionListener { onShow() }
            val exitItem = MenuItem("退出")
            exitItem.addActionListener { onExit() }
            popup.add(showItem)
            popup.add(exitItem)

            val icon = TrayIcon(createTrayImage(), "Frp Tunnel", popup)
            icon.isImageAutoSize = true
            // 单击托盘图标同样唤回主窗口
            icon.addActionListener { onShow() }
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
        }.isSuccess
    }

    /** 移除全部托盘图标（含 AppNotifier 创建的），退出前必须调用 */
    @Synchronized
    fun removeAll() {
        runCatching {
            trayIcon?.let { SystemTray.getSystemTray().remove(it) }
            trayIcon = null
        }
        AppNotifier.removeTrayIcons()
    }

    /** 无应用图标资源，画一个实心圆兜底 */
    private fun createTrayImage(): BufferedImage {
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color(0x3D, 0xDC, 0x84)
        g.fillOval(1, 1, 14, 14)
        g.dispose()
        return image
    }
}
