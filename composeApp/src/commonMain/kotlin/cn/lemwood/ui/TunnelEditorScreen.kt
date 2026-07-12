package cn.lemwood.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lemwood.components.ErrorLine
import cn.lemwood.components.SectionHeader
import cn.lemwood.components.ConfirmDialog
import cn.lemwood.model.TunnelType
import cn.lemwood.model.TunnelUiState
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppDimen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelEditorScreen(
    tunnelId: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val existing = tunnelId?.let { id ->
        AppStateHolder.state.value.tunnels.find { it.id == id }
    }
    val isNew = existing == null

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: TunnelType.TCP) }
    var localAddr by remember { mutableStateOf(existing?.localAddr ?: "127.0.0.1") }
    var localPort by remember { mutableStateOf(existing?.localPort?.toString() ?: "") }
    var remotePort by remember { mutableStateOf(existing?.remotePort?.toString() ?: "") }
    var encryption by remember { mutableStateOf(existing?.encryption ?: false) }
    var compression by remember { mutableStateOf(existing?.compression ?: false) }
    var tls by remember { mutableStateOf(existing?.tls ?: false) }
    var customDomain by remember { mutableStateOf(existing?.customDomain ?: "") }
    var httpUser by remember { mutableStateOf(existing?.httpUser ?: "") }
    var httpPassword by remember { mutableStateOf(existing?.httpPassword ?: "") }
    var showAdvanced by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasChanged = remember(
        name, type, localAddr, localPort, remotePort,
        encryption, compression, tls, customDomain, httpUser, httpPassword,
        existing
    ) {
        existing == null || name != existing.name || type != existing.type ||
            localAddr != existing.localAddr || localPort != existing.localPort.toString() ||
            remotePort != existing.remotePort.toString() || encryption != existing.encryption ||
            compression != existing.compression || tls != existing.tls ||
            customDomain != existing.customDomain.orEmpty() ||
            httpUser != existing.httpUser.orEmpty() ||
            httpPassword != existing.httpPassword.orEmpty()
    }

    fun validate(): Map<String, String> {
        val e = mutableMapOf<String, String>()
        if (name.isBlank() || name.length > 32) e["name"] = "请输入 1-32 字符的隧道名称"
        if (localAddr.isBlank()) e["localAddr"] = "请输入本地地址"
        val lp = localPort.toIntOrNull()
        if (lp == null || lp < 1 || lp > 65535) e["localPort"] = "有效端口: 1-65535"
        val rp = remotePort.toIntOrNull()
        if (rp == null || rp < 1 || rp > 65535) e["remotePort"] = "有效端口: 1-65535"
        else {
            val conflicts = AppStateHolder.state.value.tunnels.any {
                it.remotePort == rp && it.id != tunnelId
            }
            if (conflicts) e["remotePort"] = "端口已被使用"
        }
        return e
    }

    fun save() {
        val validation = validate()
        errors = validation
        if (validation.isNotEmpty()) return

        val id = existing?.id ?: "tunnel-${name.lowercase().replace(" ", "-")}-${System.currentTimeMillis()}"
        val tunnel = TunnelUiState(
            id = id,
            name = name.trim(),
            type = type,
            localAddr = localAddr.trim(),
            localPort = localPort.toIntOrNull() ?: 0,
            remotePort = remotePort.toIntOrNull() ?: 0,
            status = existing?.status ?: cn.lemwood.model.TunnelStatus.OFFLINE,
            enabled = existing?.enabled ?: false,
            traffic = existing?.traffic ?: cn.lemwood.model.Traffic(),
            encryption = encryption,
            compression = compression,
            tls = tls,
            customDomain = customDomain.trim().ifEmpty { null },
            httpUser = httpUser.trim().ifEmpty { null },
            httpPassword = httpPassword.trim().ifEmpty { null },
        )

        if (isNew) {
            AppStateHolder.addTunnel(tunnel)
        } else {
            AppStateHolder.updateTunnel(tunnel)
        }
        onDismiss()
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TopAppBar(
            title = { Text(if (isNew) "新增隧道" else "编辑隧道") },
            navigationIcon = {
                IconButton(onClick = {
                    if (hasChanged) {
                        showDiscardDialog = true
                    } else {
                        onDismiss()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                TextButton(onClick = { save() }) {
                    Text("保存")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppDimen.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimen.CardPadding),
        ) {
            SectionHeader(title = "基本信息")

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                placeholder = { Text("例: mc-server") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = "name" in errors,
            )
            errors["name"]?.let { ErrorLine(message = it) }

            TunnelTypeDropdown(
                selected = type,
                onSelect = { type = it },
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader(title = "网络配置")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimen.CardPadding),
            ) {
                OutlinedTextField(
                    value = localAddr,
                    onValueChange = { localAddr = it },
                    label = { Text("本地地址") },
                    placeholder = { Text("127.0.0.1") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = "localAddr" in errors,
                )
                OutlinedTextField(
                    value = localPort,
                    onValueChange = { localPort = it },
                    label = { Text("本地端口") },
                    placeholder = { Text("25565") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = "localPort" in errors,
                )
            }
            errors["localAddr"]?.let { ErrorLine(message = it) }
            errors["localPort"]?.let { ErrorLine(message = it) }

            OutlinedTextField(
                value = remotePort,
                onValueChange = { remotePort = it },
                label = { Text("远程端口") },
                placeholder = { Text("25565") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = "remotePort" in errors,
            )
            errors["remotePort"]?.let { ErrorLine(message = it) }

            SectionHeader(title = "安全选项")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CheckboxRow(label = "加密传输", checked = encryption) { encryption = it }
                CheckboxRow(label = "压缩传输", checked = compression) { compression = it }
                CheckboxRow(label = "TLS", checked = tls) { tls = it }
            }

            SectionHeader(title = "高级选项")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { showAdvanced = !showAdvanced },
                ) {
                    Text("高级选项")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                    )
                }
            }

            AnimatedVisibility(
                visible = showAdvanced,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimen.CardPadding),
                ) {
                    val isHttp = type == TunnelType.HTTP || type == TunnelType.HTTPS
                    OutlinedTextField(
                        value = customDomain,
                        onValueChange = { customDomain = it },
                        label = { Text("自定义域名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = isHttp,
                    )
                    OutlinedTextField(
                        value = httpUser,
                        onValueChange = { httpUser = it },
                        label = { Text("HTTP 用户") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = isHttp,
                    )
                    OutlinedTextField(
                        value = httpPassword,
                        onValueChange = { httpPassword = it },
                        label = { Text("HTTP 密码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = isHttp,
                    )
                }
            }
        }

        if (showDiscardDialog) {
            ConfirmDialog(
                title = "放弃修改",
                text = "确定放弃当前修改？所有未保存的更改都将丢失。",
                confirmText = "放弃",
                dismissText = "继续编辑",
                onConfirm = {
                    showDiscardDialog = false
                    onDismiss()
                },
                onDismiss = { showDiscardDialog = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TunnelTypeDropdown(
    selected: TunnelType,
    onSelect: (TunnelType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("协议") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TunnelType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
