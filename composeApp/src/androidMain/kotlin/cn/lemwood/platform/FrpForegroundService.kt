package cn.lemwood.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cn.lemwood.R

/**
 * 前台服务保活：frpc 在进程内 JNI 运行，进程被杀隧道即断，
 * 连接期间以前台服务持有进程。所有系统调用静默降级不抛异常。
 */
class FrpForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching {
            ensureChannel()
            val server = intent?.getStringExtra(EXTRA_SERVER).orEmpty()
            val content = if (server.isNotBlank()) "隧道运行中 · $server" else "隧道运行中"
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Frp Tunnel")
                .setContentText(content)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .apply {
                    // 点击通知回到主界面
                    runCatching {
                        val launch = packageManager.getLaunchIntentForPackage(packageName)
                        if (launch != null) {
                            val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                            setContentIntent(PendingIntent.getActivity(this@FrpForegroundService, 0, launch, piFlags))
                        }
                    }
                }
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        super.onDestroy()
    }

    // API 26+ 需要通知渠道，重复创建是幂等的
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "前台服务",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_SERVER = "extra_server"
        private const val CHANNEL_ID = "frp_service"
        private const val NOTIFICATION_ID = 1001
    }
}
