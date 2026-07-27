package cn.lemwood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cn.lemwood.components.EmptyState
import cn.lemwood.components.FilterChips
import cn.lemwood.components.LogEntryRow
import cn.lemwood.model.AppState
import cn.lemwood.platform.uploadToLogShare
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppDimen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val logLevelOptions = listOf("ALL", "INFO", "WARN", "ERROR")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    modifier: Modifier = Modifier,
) {
    val appState by AppStateHolder.state.map { it }.collectAsStateWithLifecycle(initial = AppState())
    val logs = appState.logs

    var selectedLevels by remember { mutableStateOf(setOf("ALL")) }
    var autoScroll by remember { mutableStateOf(true) }
    var copied by remember { mutableStateOf(false) }
    // 0 空闲 1 上传中 2 成功 3 失败
    var shareState by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val filteredLogs = remember(logs, selectedLevels) {
        if ("ALL" in selectedLevels) logs else {
            logs.filter { it.level.name in selectedLevels }
        }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    // 复制成功后将图标短暂变为对勾
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }

    // 分享结果图标 2 秒后复位
    LaunchedEffect(shareState) {
        if (shareState == 2 || shareState == 3) {
            kotlinx.coroutines.delay(2000)
            shareState = 0
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("日志") },
            actions = {
                IconButton(
                    enabled = shareState != 1 && filteredLogs.isNotEmpty(),
                    onClick = {
                        shareState = 1
                        val text = filteredLogs.joinToString("\n") { entry ->
                            "${formatTime(entry.timestamp)} [${entry.level.name}] ${entry.message}"
                        }
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { uploadToLogShare(text) }
                            if (result.url != null) {
                                clipboardManager.setText(AnnotatedString(result.url))
                                shareState = 2
                            } else {
                                shareState = 3
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = when (shareState) {
                            1 -> Icons.Default.CloudUpload
                            2 -> Icons.Default.Check
                            3 -> Icons.Default.CloudOff
                            else -> Icons.Default.Share
                        },
                        contentDescription = when (shareState) {
                            1 -> "上传中"
                            2 -> "链接已复制"
                            3 -> "分享失败"
                            else -> "分享到 logshare.cn"
                        },
                        tint = when (shareState) {
                            2 -> MaterialTheme.colorScheme.primary
                            3 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(
                    onClick = {
                        if (filteredLogs.isNotEmpty()) {
                            val text = filteredLogs.joinToString("\n") { entry ->
                                "${formatTime(entry.timestamp)} [${entry.level.name}] ${entry.message}"
                            }
                            clipboardManager.setText(AnnotatedString(text))
                            copied = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "复制日志",
                        tint = if (copied) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(onClick = { AppStateHolder.clearLogs() }) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "清空日志",
                    )
                }
                IconButton(onClick = { autoScroll = !autoScroll }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = if (autoScroll) "自动滚动开启" else "自动滚动关闭",
                        tint = if (autoScroll) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            },
        )

        FilterChips(
            options = logLevelOptions,
            selected = selectedLevels,
            onSelect = { option ->
                selectedLevels = if (option == "ALL") {
                    setOf("ALL")
                } else {
                    val updated = selectedLevels - "ALL" + option
                    if (updated.isEmpty()) setOf("ALL") else updated
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(AppDimen.CardPadding))

        if (filteredLogs.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = "暂无日志",
                subtitle = "日志将在这里显示",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = AppDimen.ScreenPadding,
                    end = AppDimen.ScreenPadding,
                    bottom = AppDimen.ScreenPadding,
                ),
            ) {
                items(
                    items = filteredLogs,
                    key = { it.timestamp + it.message.hashCode() },
                ) { entry ->
                    LogEntryRow(
                        level = entry.level.name,
                        message = entry.message,
                        time = formatTime(entry.timestamp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val totalSeconds = timestamp / 1000
    val secondsOfDay = totalSeconds % 86400
    val hour = secondsOfDay / 3600
    val minute = (secondsOfDay % 3600) / 60
    val second = secondsOfDay % 60
    return "%02d:%02d:%02d".format(hour, minute, second)
}

// Helper to collect state with lifecycle semantics in commonMain.
@Composable
private fun <T> kotlinx.coroutines.flow.Flow<T>.collectAsStateWithLifecycle(
    initial: T,
): androidx.compose.runtime.State<T> {
    return collectAsState(initial = initial)
}
