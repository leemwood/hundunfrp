package cn.lemwood.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemwood.components.ConfirmDialog
import cn.lemwood.components.EmptyState
import cn.lemwood.components.FilterChips
import cn.lemwood.components.SearchBar
import cn.lemwood.components.StatusBadge
import cn.lemwood.model.AppState
import cn.lemwood.model.TunnelStatus
import cn.lemwood.model.TunnelUiState
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppDimen
import cn.lemwood.theme.ErrorColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val statusOptions = listOf("全部", "在线", "离线", "错误")
private val typeOptions = listOf("全部", "TCP", "UDP", "HTTP", "HTTPS", "STCP", "XTCP")

/**
 * 筛选标签切换逻辑：点「全部」重置为全部；点其他标签时切换其选中态
 * （已选则取消），全部取消后回落到「全部」。
 */
private fun toggleChipOption(selected: Set<String>, option: String): Set<String> {
    if (option == "全部") return setOf("全部")
    val base = selected - "全部"
    val updated = if (option in base) base - option else base + option
    return if (updated.isEmpty()) setOf("全部") else updated
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelListTopBar(connected: Boolean) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Frp Tunnel",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusBadge(status = if (connected) "online" else "offline")
            }
        },
    )
}

@Composable
fun AddTunnelFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(Icons.Default.Add, contentDescription = "新增隧道")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelListScreen(
    modifier: Modifier = Modifier,
    onAddTunnel: (() -> Unit)? = null,
    onEditTunnel: ((String) -> Unit)? = null,
) {
    val appState by AppStateHolder.state.map { it }.collectAsStateWithLifecycle(initial = AppState())
    val tunnels = appState.tunnels

    var query by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(setOf("全部")) }
    var selectedType by remember { mutableStateOf(setOf("全部")) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }
    var batchDeleteConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredTunnels = remember(tunnels, query, selectedStatus, selectedType) {
        tunnels.filter { tunnel ->
            val matchesQuery = query.isBlank() ||
                tunnel.name.contains(query, ignoreCase = true) ||
                tunnel.localAddr.contains(query, ignoreCase = true) ||
                tunnel.localPort.toString().contains(query) ||
                tunnel.remotePort.toString().contains(query)

            val matchesStatus = "全部" in selectedStatus || when (tunnel.status) {
                TunnelStatus.ONLINE -> "在线" in selectedStatus
                TunnelStatus.OFFLINE -> "离线" in selectedStatus
                TunnelStatus.ERROR -> "错误" in selectedStatus
                TunnelStatus.CONNECTING -> false
            }

            val matchesType = "全部" in selectedType || tunnel.type.displayName in selectedType

            matchesQuery && matchesStatus && matchesType
        }.sortedWith(
            compareBy<TunnelUiState> {
                when (it.status) {
                    TunnelStatus.ONLINE -> 0
                    TunnelStatus.CONNECTING -> 1
                    TunnelStatus.OFFLINE -> 2
                    TunnelStatus.ERROR -> 3
                }
            }.thenBy { it.name }
        )
    }

    val onStatusSelect: (String) -> Unit = { option ->
        selectedStatus = toggleChipOption(selectedStatus, option)
    }

    val onTypeSelect: (String) -> Unit = { option ->
        selectedType = toggleChipOption(selectedType, option)
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    delay(800)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (tunnels.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "还没有隧道",
                    subtitle = "点击右下角 + 按钮添加第一条隧道",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (selectionMode) {
                        SelectionTopBar(
                            selectedCount = selectedIds.size,
                            onDismiss = {
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                            onSelectAll = {
                                selectedIds = filteredTunnels.map { it.id }.toSet()
                            },
                            onBatchToggle = {
                                filteredTunnels.forEach { t ->
                                    if (t.id in selectedIds) {
                                        AppStateHolder.toggleTunnel(t.id)
                                    }
                                }
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                            onBatchDelete = {
                                batchDeleteConfirm = true
                            },
                        )
                    } else {
                        TunnelListTopBar(connected = appState.serverStatus.connected)
                    }

                    SearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = "搜索名称/端口...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimen.ScreenPadding),
                    )

                    Spacer(modifier = Modifier.height(AppDimen.CardPadding))

                    FilterChips(
                        options = statusOptions,
                        selected = selectedStatus,
                        onSelect = onStatusSelect,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FilterChips(
                        options = typeOptions,
                        selected = selectedType,
                        onSelect = onTypeSelect,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(AppDimen.CardPadding))

                    if (filteredTunnels.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Search,
                            title = "没有匹配结果",
                            subtitle = "尝试清除搜索或过滤条件",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(AppDimen.CardPadding),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = AppDimen.ScreenPadding,
                                end = AppDimen.ScreenPadding,
                                bottom = AppDimen.ScreenPadding,
                            ),
                        ) {
                            items(
                                items = filteredTunnels,
                                key = { it.id },
                            ) { tunnel ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            deleteConfirmId = tunnel.id
                                            false
                                        } else {
                                            false
                                        }
                                    },
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            targetValue = when (dismissState.targetValue) {
                                                SwipeToDismissBoxValue.EndToStart -> ErrorColor
                                                else -> Color.Transparent
                                            },
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(color),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .padding(end = AppDimen.ScreenPadding),
                                            )
                                        }
                                    },
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = true,
                                ) {
                                    TunnelCard(
                                        tunnel = tunnel,
                                        selectionMode = selectionMode,
                                        selected = tunnel.id in selectedIds,
                                        onSelectionChange = { checked ->
                                            selectedIds = if (checked) {
                                                selectedIds + tunnel.id
                                            } else {
                                                selectedIds - tunnel.id
                                            }
                                            if (selectedIds.isEmpty()) {
                                                selectionMode = false
                                            }
                                        },
                                        onLongClick = {
                                            selectionMode = true
                                            selectedIds = selectedIds + tunnel.id
                                        },
                                        onEdit = { onEditTunnel?.invoke(tunnel.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (deleteConfirmId != null) {
        val tunnelName = tunnels.find { it.id == deleteConfirmId }?.name ?: ""
        ConfirmDialog(
            title = "删除隧道",
            text = "确定删除 \"$tunnelName\"？此操作不可撤销。",
            confirmText = "删除",
            dismissText = "取消",
            onConfirm = {
                val id = deleteConfirmId ?: return@ConfirmDialog
                val deleted = tunnels.find { it.id == id }
                AppStateHolder.deleteTunnel(id)
                deleteConfirmId = null
                if (deleted != null) {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "\"${deleted.name}\" 已删除",
                            actionLabel = "撤销",
                        )
                        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                            AppStateHolder.addTunnel(deleted)
                        }
                    }
                }
            },
            onDismiss = { deleteConfirmId = null },
        )
    }

    if (batchDeleteConfirm) {
        val selectedNames = tunnels.filter { it.id in selectedIds }.map { it.name }
        ConfirmDialog(
            title = "批量删除",
            text = "确定删除 ${selectedIds.size} 条隧道？\n${selectedNames.joinToString(", ")}\n此操作不可撤销。",
            confirmText = "删除",
            dismissText = "取消",
            onConfirm = {
                selectedIds.forEach { id -> AppStateHolder.deleteTunnel(id) }
                batchDeleteConfirm = false
                selectionMode = false
                selectedIds = emptySet()
                scope.launch {
                    snackbarHostState.showSnackbar("${selectedIds.size} 条隧道已删除")
                }
            },
            onDismiss = { batchDeleteConfirm = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onBatchToggle: () -> Unit,
    onBatchDelete: () -> Unit,
) {
    TopAppBar(
        title = { Text("已选择 $selectedCount 项") },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "取消选择")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "全选")
            }
            IconButton(onClick = onBatchToggle) {
                Icon(Icons.Default.DoneAll, contentDescription = "批量开关")
            }
            IconButton(onClick = onBatchDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "批量删除",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
private fun <T> kotlinx.coroutines.flow.Flow<T>.collectAsStateWithLifecycle(
    initial: T,
): androidx.compose.runtime.State<T> {
    return collectAsState(initial = initial)
}
