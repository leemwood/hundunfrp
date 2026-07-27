package cn.lemwood.platform

import android.content.Intent
import android.os.Build

/**
 * Android 前台服务控制：连接期间启动 FrpForegroundService 保活进程。
 * 后台启动 FGS 等异常（API 31+ BackgroundServiceStartException）静默吞掉。
 */
actual object ForegroundServiceController {

    actual fun onServerConnected(server: String) {
        val ctx = AndroidFrpContext.appContext ?: return
        runCatching {
            val intent = Intent(ctx, FrpForegroundService::class.java)
                .putExtra(FrpForegroundService.EXTRA_SERVER, server)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }
    }

    actual fun onServerDisconnected() {
        val ctx = AndroidFrpContext.appContext ?: return
        runCatching {
            ctx.stopService(Intent(ctx, FrpForegroundService::class.java))
        }
    }
}
