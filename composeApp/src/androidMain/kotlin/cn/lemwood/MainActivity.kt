package cn.lemwood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.lemwood.platform.AndroidFrpContext
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppTheme
import cn.lemwood.ui.CrashHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            CrashHandler.record(e)
        }
        enableEdgeToEdge()
        AndroidFrpContext.appContext = applicationContext
        AppStateHolder.init(this)
        setContent {
            AppTheme {
                App()
            }
        }
    }
}
