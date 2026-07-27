package cn.lemwood.platform

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

actual object BatteryOptimizer {
    actual val isSupported: Boolean = true

    private val context: Context?
        get() = AndroidFrpContext.appContext

    actual fun isIgnoringBatteryOptimizations(): Boolean = runCatching {
        val ctx = context ?: return false
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }.getOrDefault(false)

    actual fun requestExemption() {
        val ctx = context ?: return
        runCatching {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${ctx.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        }
    }

    /** 厂商自启动/后台管理页面 ComponentName 清单（按厂商分组，参考 dontkillmyapp） */
    private val oemIntents: Map<List<String>, List<ComponentName>> = mapOf(
        listOf("xiaomi", "redmi") to listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
        ),
        listOf("huawei", "honor") to listOf(
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
        ),
        listOf("oppo", "oneplus", "realme") to listOf(
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
        ),
        listOf("vivo", "iqoo") to listOf(
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
        ),
        listOf("samsung") to listOf(
            ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")
        ),
        listOf("meizu") to listOf(
            ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity"),
            ComponentName("com.meizu.safe", "com.meizu.safe.security.SHOW_APPSEC")
        )
    )

    actual fun openOemBackgroundSettings() {
        val ctx = context ?: return
        runCatching {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val candidates = oemIntents.entries
                .firstOrNull { (keys, _) -> keys.any { manufacturer.contains(it) } }
                ?.value.orEmpty()
            for (component in candidates) {
                if (tryStartActivity(ctx, Intent().setComponent(component))) return
            }
            // 无匹配厂商页面时回退到应用详情设置页
            tryStartActivity(
                ctx,
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${ctx.packageName}")
                )
            )
        }
    }

    /** 尝试拉起页面，静默处理失败；返回是否成功 */
    private fun tryStartActivity(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }
}
