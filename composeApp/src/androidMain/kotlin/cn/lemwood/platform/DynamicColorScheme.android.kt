package cn.lemwood.platform

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    // 动态取色仅 Android 12 (SDK 31)+ 支持
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = AndroidFrpContext.appContext ?: return null
    return runCatching {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }.getOrNull()
}
