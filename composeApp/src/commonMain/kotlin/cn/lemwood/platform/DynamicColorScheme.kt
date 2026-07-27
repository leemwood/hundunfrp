package cn.lemwood.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * 平台动态取色方案；平台不支持时（如 Desktop、Android 12 以下）返回 null
 */
@Composable
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?
