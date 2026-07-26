package cn.lemwood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lemwood.components.StatusBadge
import cn.lemwood.components.TrafficChart
import cn.lemwood.components.TrafficDataPoint
import cn.lemwood.model.AppState
import cn.lemwood.model.LogLevel
import cn.lemwood.model.TunnelStatus
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppDimen
import cn.lemwood.theme.ConnectingColor
import cn.lemwood.theme.ErrorColor
import cn.lemwood.theme.OnlineColor
import kotlinx.coroutines.flow.map

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
) {
    val appState by AppStateHolder.state.map { it }.collectAsStateWithLifecycle(initial = AppState())
    val server = appState.serverStatus
    val tunnels = appState.tunnels
    val onlineCount = tunnels.count { it.status == TunnelStatus.ONLINE }
    val totalCount = tunnels.size
    val activityRatio = if (totalCount > 0) onlineCount.toFloat() / totalCount else 0f
    val trafficHistory = appState.trafficHistory
    val peakBytesPerSecond = trafficHistory.maxOfOrNull { it.upBytesPerSec + it.downBytesPerSec } ?: 0L
    val dataPoints = trafficHistory.map { sample ->
        TrafficDataPoint(
            label = formatTimeHm(sample.timestamp),
            upload = sample.upBytesPerSec.toFloat(),
            download = sample.downBytesPerSec.toFloat(),
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppDimen.ScreenPadding),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(AppDimen.ScreenPadding),
    ) {
        item {
            ServerConnectionCard(server = server)
        }

        item {
            TrafficSummaryCard(
                uploadBytes = server.totalUploadBytes,
                downloadBytes = server.totalDownloadBytes,
                peakBytesPerSecond = peakBytesPerSecond,
                dataPoints = dataPoints,
            )
        }

        item {
            TunnelActivityCard(
                onlineCount = onlineCount,
                totalCount = totalCount,
                ratio = activityRatio,
            )
        }

        item {
            Text(
                text = "最近事件",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        items(
            items = appState.logs.takeLast(5).reversed(),
            key = { "${it.timestamp}_${it.message.hashCode()}" },
        ) { entry ->
            Text(
                text = "${formatTimeHm(entry.timestamp)} ${entry.message}",
                style = MaterialTheme.typography.bodyMedium,
                color = when (entry.level) {
                    LogLevel.ERROR -> ErrorColor
                    LogLevel.WARN -> ConnectingColor
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ServerConnectionCard(
    server: cn.lemwood.model.ServerStatus,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(AppDimen.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusBadge(status = if (server.connected) "online" else "offline")
                Text(
                    text = if (server.connected) "已连接" else "未连接",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = server.server,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "延迟 ${server.latencyMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "运行 ${formatDuration(server.uptimeSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrafficSummaryCard(
    uploadBytes: Long,
    downloadBytes: Long,
    peakBytesPerSecond: Long,
    dataPoints: List<TrafficDataPoint>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(AppDimen.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "今日流量",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TrafficMetric(label = "上行", value = formatBytes(uploadBytes))
                TrafficMetric(label = "下行", value = formatBytes(downloadBytes))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TrafficMetric(label = "总计", value = formatBytes(uploadBytes + downloadBytes))
                TrafficMetric(label = "峰值", value = "${formatBytes(peakBytesPerSecond)}/s")
            }
            Text(
                text = "流量趋势",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (dataPoints.size >= 2) {
                TrafficChart(
                    dataPoints = dataPoints,
                )
            } else {
                Text(
                    text = "暂无流量数据，连接后自动采集",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TrafficMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TunnelActivityCard(
    onlineCount: Int,
    totalCount: Int,
    ratio: Float,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(AppDimen.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "隧道活跃度",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "$onlineCount/$totalCount 在线",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.weight(1f),
                    color = OnlineColor,
                )
                Text(
                    text = "${(ratio * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return "%02d:%02d".format(hours, minutes)
}

// commonMain 无 kotlinx.datetime 依赖，用取模换算 HH:mm（按 UTC 计）
private fun formatTimeHm(timestamp: Long): String {
    val totalMinutes = (timestamp / 60_000L) % (24 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "%02d:%02d".format(hours, minutes)
}

// Helper to collect state with lifecycle semantics in commonMain.
@Composable
private fun <T> kotlinx.coroutines.flow.Flow<T>.collectAsStateWithLifecycle(
    initial: T,
): androidx.compose.runtime.State<T> {
    return collectAsState(initial = initial)
}
