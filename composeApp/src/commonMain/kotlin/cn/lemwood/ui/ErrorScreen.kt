package cn.lemwood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.lemwood.state.AppStateHolder
import cn.lemwood.theme.AppDimen

object CrashHandler {
    @Volatile
    var lastError: Throwable? = null

    fun record(error: Throwable) {
        lastError = error
        AppStateHolder.addLog(
            cn.lemwood.model.LogEntry(
                level = cn.lemwood.model.LogLevel.ERROR,
                message = "应用崩溃: ${error.message}",
                timestamp = System.currentTimeMillis(),
            )
        )
        try {
            AppStateHolder.persistSettingsForNow()
        } catch (_: Exception) {}
    }

    fun clear() {
        lastError = null
    }
}

@Composable
fun ErrorScreen(
    error: Throwable?,
    onRestart: () -> Unit,
    onResetData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDetails by remember { mutableStateOf(false) }
    val message = error?.message ?: "未知错误"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppDimen.ScreenPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "应用遇到了问题",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "抱歉，应用发生了未预期的错误。你可以尝试重启应用或清除数据后重试。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.8f),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = message.take(120),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (showDetails && error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error.stackTraceToString().lines()
                            .take(15).joinToString("\n"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }

                if (error != null) {
                    OutlinedButton(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (showDetails) "收起详情" else "查看详情")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                CrashHandler.clear()
                onRestart()
            },
            modifier = Modifier.fillMaxWidth(0.7f),
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("重启应用")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onResetData,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.fillMaxWidth(0.7f),
        ) {
            Text("清除数据并重启")
        }
    }
}
