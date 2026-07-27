package cn.lemwood.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cn.lemwood.R

/**
 * Android 系统通知实现。
 * 权限未授予或环境不支持时静默降级，绝不抛异常。
 */
actual class AppNotifier actual constructor(context: Any?) {

    // 优先用传入的 Context，兜底用全局 applicationContext
    private val appContext: Context? =
        (context as? Context)?.applicationContext ?: AndroidFrpContext.appContext

    actual fun notify(title: String, message: String, notificationId: Int) {
        val ctx = appContext ?: return
        runCatching {
            val manager = NotificationManagerCompat.from(ctx)
            if (!manager.areNotificationsEnabled()) return
            ensureChannel(ctx)
            val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .build()
            manager.notify(notificationId, notification)
        }
    }

    // API 26+ 需要通知渠道，重复创建是幂等的
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "隧道状态",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    private companion object {
        const val CHANNEL_ID = "frp_status"
    }
}
