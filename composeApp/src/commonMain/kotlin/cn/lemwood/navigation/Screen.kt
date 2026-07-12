package cn.lemwood.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val label: String,
    val icon: ImageVector,
) {
    TunnelList(
        label = "隧道",
        icon = Icons.Default.Home,
    ),
    Status(
        label = "状态",
        icon = Icons.Default.Analytics,
    ),
    Settings(
        label = "设置",
        icon = Icons.Default.Settings,
    ),
    Log(
        label = "日志",
        icon = Icons.AutoMirrored.Filled.List,
    ),
}
