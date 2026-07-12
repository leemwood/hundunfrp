package cn.lemwood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppStateHolder.init(this)
        setContent {
            AppTheme {
                App()
            }
        }
    }
}
