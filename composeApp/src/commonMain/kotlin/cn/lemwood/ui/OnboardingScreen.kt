package cn.lemwood.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.lemwood.theme.AppDimen

private data class OnboardingPage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.AutoMirrored.Filled.List,
        title = "管理隧道",
        description = "轻松创建和管理 frp 隧道配置，支持 TCP/UDP/HTTP 等多种协议",
    ),
    OnboardingPage(
        icon = Icons.Default.Speed,
        title = "实时监控",
        description = "查看连接状态、流量统计和服务器信息，一切尽在掌控",
    ),
    OnboardingPage(
        icon = Icons.Default.Share,
        title = "开始使用",
        description = "配置服务器地址后即可连接，支持开机自启和后台运行",
    ),
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPage by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppDimen.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val page = pages[currentPage]

        Icon(
            imageVector = page.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.8f),
        )

        Spacer(modifier = Modifier.height(48.dp))

        val isLast = currentPage == pages.lastIndex
        Button(
            onClick = {
                if (isLast) {
                    onComplete()
                } else {
                    currentPage++
                }
            },
            modifier = Modifier.fillMaxWidth(0.6f),
        ) {
            if (isLast) {
                Icon(
                    Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("开始使用")
            } else {
                Text("下一步")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val primaryColor = MaterialTheme.colorScheme.primary
        val outlineColor = MaterialTheme.colorScheme.outline

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(modifier = Modifier.size(if (0 == currentPage) 12.dp else 8.dp)) {
                drawCircle(if (0 == currentPage) primaryColor else outlineColor)
            }
            Canvas(modifier = Modifier.size(if (1 == currentPage) 12.dp else 8.dp)) {
                drawCircle(if (1 == currentPage) primaryColor else outlineColor)
            }
            Canvas(modifier = Modifier.size(if (2 == currentPage) 12.dp else 8.dp)) {
                drawCircle(if (2 == currentPage) primaryColor else outlineColor)
            }
        }
    }
}
