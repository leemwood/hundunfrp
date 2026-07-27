package cn.lemwood.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

// Desktop 无系统动态取色能力，恒返回 null
@Composable
actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
