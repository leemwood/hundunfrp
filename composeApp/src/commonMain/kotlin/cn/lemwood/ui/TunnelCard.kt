package cn.lemwood.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemwood.components.AddressLine
import cn.lemwood.components.StatusBadge
import cn.lemwood.components.TrafficRow
import cn.lemwood.components.TypeChip
import cn.lemwood.model.TunnelStatus
import cn.lemwood.model.TunnelUiState
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppDimen
import cn.lemwood.theme.ConnectingColor
import cn.lemwood.theme.ErrorColor
import cn.lemwood.theme.OfflineColor
import cn.lemwood.theme.OnlineColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TunnelCard(
    tunnel: TunnelUiState,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val statusColor = statusColor(tunnel.status)
    val containerColor = when (tunnel.status) {
        TunnelStatus.ONLINE -> MaterialTheme.colorScheme.surface
        TunnelStatus.OFFLINE -> MaterialTheme.colorScheme.surfaceVariant
        TunnelStatus.CONNECTING -> MaterialTheme.colorScheme.surface
        TunnelStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimen.CardHeight)
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onSelectionChange(!selected)
                    } else {
                        AppStateHolder.toggleTunnel(tunnel.id)
                    }
                },
                onLongClick = onLongClick,
            ),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = if (tunnel.status == TunnelStatus.ONLINE) AppDimen.CardElevation else 0.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(statusColor),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimen.CardPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onSelectionChange(it) },
                    )
                    Spacer(modifier = Modifier.width(AppDimen.CardPadding / 2))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = tunnel.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        StatusBadge(status = tunnel.status.name.lowercase())
                        TypeChip(type = tunnel.type.displayName)
                    }

                    AddressLine(
                        local = "${tunnel.localAddr}:${tunnel.localPort}",
                        remote = ":${tunnel.remotePort}",
                    )

                    when (tunnel.status) {
                        TunnelStatus.ONLINE -> TrafficRow(up = tunnel.traffic.up, down = tunnel.traffic.down)
                        TunnelStatus.OFFLINE -> Text(
                            text = "上次在线: 2小时前",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TunnelStatus.CONNECTING -> Text(
                            text = "连接中...",
                            style = MaterialTheme.typography.bodySmall,
                            color = ConnectingColor,
                        )
                        TunnelStatus.ERROR -> Text(
                            text = tunnel.lastError ?: "隧道错误",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AppDimen.CardPadding))

                Switch(
                    checked = tunnel.enabled,
                    onCheckedChange = { AppStateHolder.toggleTunnel(tunnel.id) },
                    enabled = tunnel.status != TunnelStatus.CONNECTING,
                )
            }
        }
    }
}

@Composable
private fun statusColor(status: TunnelStatus): Color = when (status) {
    TunnelStatus.ONLINE -> OnlineColor
    TunnelStatus.OFFLINE -> OfflineColor
    TunnelStatus.CONNECTING -> ConnectingColor
    TunnelStatus.ERROR -> ErrorColor
}
