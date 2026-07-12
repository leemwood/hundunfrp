package cn.lemwood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lemwood.components.ConfirmDialog
import cn.lemwood.components.FilterChips
import cn.lemwood.components.SectionHeader
import cn.lemwood.components.SettingsRow
import cn.lemwood.data.ExportImportManager
import cn.lemwood.data.showFileOpenDialog
import cn.lemwood.data.showFileSaveDialog
import cn.lemwood.model.AppSettings
import cn.lemwood.model.AppState
import cn.lemwood.model.LogLevel
import cn.lemwood.platform.FrpController
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppDimen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val appState by AppStateHolder.state.map { it }.collectAsStateWithLifecycle(initial = AppState())
    val settings = appState.settings
    val frpController = remember { FrpController() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppDimen.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimen.ScreenPadding),
    ) {
        SettingsSection(title = "服务端连接") {
            ServerConnectionSettings(
                settings = settings,
                frpController = frpController,
                snackbarHostState = snackbarHostState,
                scope = scope,
            )
        }

        SettingsSection(title = "全局行为") {
            GlobalBehaviorSettings(settings = settings)
        }

        SettingsSection(title = "外观") {
            AppearanceSettings(settings = settings)
        }

        SettingsSection(title = "数据管理") {
            DataManagementSettings(snackbarHostState = snackbarHostState)
        }

        SettingsSection(title = "关于") {
            AboutSection()
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(AppDimen.ScreenPadding)) {
                content()
            }
        }
    }
}

@Composable
private fun ServerConnectionSettings(
    settings: AppSettings,
    frpController: FrpController,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    var isTesting by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = settings.serverAddr,
        onValueChange = { AppStateHolder.updateSettings(settings.copy(serverAddr = it)) },
        label = { Text("服务器地址") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(AppDimen.CardPadding))
    OutlinedTextField(
        value = settings.serverPort.toString(),
        onValueChange = { value ->
            value.toIntOrNull()?.let { port ->
                AppStateHolder.updateSettings(settings.copy(serverPort = port))
            }
        },
        label = { Text("端口") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(AppDimen.CardPadding))
    OutlinedTextField(
        value = settings.serverToken,
        onValueChange = { AppStateHolder.updateSettings(settings.copy(serverToken = it)) },
        label = { Text("令牌") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(AppDimen.CardPadding))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Button(
            onClick = {
                if (isTesting || settings.serverAddr.isBlank()) return@Button
                isTesting = true
                scope.launch {
                    val error = withContext(Dispatchers.IO) {
                        frpController.testConnection(
                            host = settings.serverAddr,
                            port = settings.serverPort,
                            timeoutSeconds = settings.timeoutSeconds.coerceAtLeast(5),
                        )
                    }
                    isTesting = false
                    if (error == null) {
                        snackbarHostState.showSnackbar("连接成功 — ${settings.serverAddr}:${settings.serverPort}")
                    } else {
                        snackbarHostState.showSnackbar("连接失败: $error")
                    }
                }
            },
            enabled = !isTesting && settings.serverAddr.isNotBlank(),
        ) {
            Text(if (isTesting) "测试中..." else "测试连接")
        }
    }
}

@Composable
private fun GlobalBehaviorSettings(settings: AppSettings) {
    SettingsRow(
        label = "开机自启",
        checked = settings.autoStart,
        onCheckedChange = { AppStateHolder.updateSettings(settings.copy(autoStart = it)) },
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = AppDimen.CardPadding / 2))
    SettingsRow(
        label = "自动重连",
        checked = settings.autoReconnect,
        onCheckedChange = { AppStateHolder.updateSettings(settings.copy(autoReconnect = it)) },
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = AppDimen.CardPadding / 2))
    SettingsRow(
        label = "通知",
        checked = settings.notifications,
        onCheckedChange = { AppStateHolder.updateSettings(settings.copy(notifications = it)) },
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = AppDimen.CardPadding / 2))
    OutlinedTextField(
        value = settings.timeoutSeconds.toString(),
        onValueChange = { value ->
            value.toIntOrNull()?.let { seconds ->
                AppStateHolder.updateSettings(settings.copy(timeoutSeconds = seconds))
            }
        },
        label = { Text("超时时间 (秒)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(AppDimen.CardPadding))
    Text(
        text = "日志级别",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = AppDimen.CardPadding / 2),
    )
    FilterChips(
        options = LogLevel.entries.map { it.name },
        selected = setOf(settings.logLevel.name),
        onSelect = { selected ->
            LogLevel.entries.find { it.name == selected }?.let { level ->
                AppStateHolder.updateSettings(settings.copy(logLevel = level))
            }
        },
    )
}

private val themeOptions = listOf(
    "system" to "跟随系统",
    "light" to "浅色",
    "dark" to "深色",
)

@Composable
private fun AppearanceSettings(settings: AppSettings) {
    Text(
        text = "主题",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = AppDimen.CardPadding / 2),
    )
    FilterChips(
        options = themeOptions.map { it.second },
        selected = setOf(themeOptions.find { it.first == settings.theme }?.second ?: "跟随系统"),
        onSelect = { selected ->
            themeOptions.find { it.second == selected }?.first?.let { theme ->
                AppStateHolder.updateSettings(settings.copy(theme = theme))
            }
        },
    )
    Spacer(modifier = Modifier.height(AppDimen.CardPadding))
    SettingsRow(
        label = "动态取色",
        checked = settings.dynamicColor,
        onCheckedChange = { AppStateHolder.updateSettings(settings.copy(dynamicColor = it)) },
    )
}

@Composable
private fun DataManagementSettings(
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    val exportImport = remember { ExportImportManager(AppStateHolder) }
    val defaultPath = "D:\\.config\\frp-kmp\\frp-kmp-backup.json"
    var exportPath by remember { mutableStateOf(defaultPath) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    OutlinedTextField(
        value = exportPath,
        onValueChange = { exportPath = it },
        label = { Text("文件路径") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(AppDimen.CardPadding))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimen.CardPadding),
    ) {
        TextButton(
            onClick = {
                scope.launch {
                    val path = withContext(Dispatchers.IO) {
                        showFileSaveDialog("导出配置", exportPath)
                    } ?: return@launch
                    exportPath = path
                    val result = runCatching { exportImport.exportToFile(path) }
                    if (result.isSuccess) {
                        showMessage("导出成功: $path")
                    } else {
                        showMessage("导出失败: ${result.exceptionOrNull()?.message}")
                    }
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text("导出")
        }
        TextButton(
            onClick = {
                scope.launch {
                    val path = withContext(Dispatchers.IO) {
                        showFileOpenDialog("导入配置")
                    } ?: return@launch
                    exportPath = path
                    val ok = exportImport.importFromFile(path)
                    if (ok) {
                        showMessage("导入成功: $path")
                    } else {
                        showMessage("导入失败，请检查文件内容或路径")
                    }
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text("导入")
        }
        TextButton(
            onClick = { showClearDialog = true },
            modifier = Modifier.weight(1f),
        ) {
            Text("清除")
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = "清除数据",
            text = "确定删除所有隧道和设置？此操作不可撤销。",
            confirmText = "清除",
            dismissText = "取消",
            onConfirm = {
                AppStateHolder.resetToDefaults()
                showClearDialog = false
                showMessage("数据已清除")
            },
            onDismiss = { showClearDialog = false },
        )
    }
}

@Composable
private fun AboutSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimen.CardPadding),
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column {
            Text(
                text = "版本 v1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "frp v0.62.x",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "leemwood",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Helper to collect state with lifecycle semantics in commonMain.
@Composable
private fun <T> kotlinx.coroutines.flow.Flow<T>.collectAsStateWithLifecycle(
    initial: T,
): androidx.compose.runtime.State<T> {
    return collectAsState(initial = initial)
}
