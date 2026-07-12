package cn.lemwood.navigation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class NavigationType {
    Compact,
    Medium,
    Expanded,
}

fun Dp.toNavigationType(): NavigationType = when {
    this < 600.dp -> NavigationType.Compact
    this in 600.dp..<840.dp -> NavigationType.Medium
    else -> NavigationType.Expanded
}
