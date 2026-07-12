package cn.lemwood.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.lemwood.theme.DownloadTrafficColor
import cn.lemwood.theme.UploadTrafficColor

/**
 * Row that displays formatted upload/download traffic values.
 *
 * @param up Upload bytes.
 * @param down Download bytes.
 */
@Composable
fun TrafficRow(
    up: Long,
    down: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrafficItem(
            icon = Icons.Default.ArrowUpward,
            value = up,
            color = UploadTrafficColor,
        )
        TrafficItem(
            icon = Icons.Default.ArrowDownward,
            value = down,
            color = DownloadTrafficColor,
        )
    }
}

@Composable
private fun TrafficItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Long,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier,
        )
        Text(
            text = formatBytes(value),
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.size - 1) {
        value /= 1024
        index++
    }
    return when {
        index == 0 -> "${value.toInt()} ${units[index]}"
        value % 1.0 == 0.0 -> "${value.toInt()} ${units[index]}"
        else -> "%.1f ${units[index]}".format(value)
    }
}
