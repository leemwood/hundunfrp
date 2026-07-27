package cn.lemwood

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import cn.lemwood.platform.AndroidFrpContext
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppTheme
import cn.lemwood.ui.CrashHandler

class MainActivity : ComponentActivity() {

    // Android 13+ 通知权限请求（结果无需处理，未授予时通知静默降级）
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            CrashHandler.record(e)
        }
        enableEdgeToEdge()
        AndroidFrpContext.appContext = applicationContext
        AppStateHolder.init(this)
        requestNotificationPermissionIfNeeded()
        setContent {
            AppTheme {
                App()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
