package cn.lemwood.data

import android.content.Context
import cn.lemwood.model.TunnelUiState
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val TUNNELS_FILE_NAME = "tunnels.json"

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

/**
 * Android 平台 [TunnelConfigStore] 实现，基于应用私有文件目录的 JSON 文件。
 */
actual class TunnelConfigStore(private val file: File) {

    actual suspend fun load(): List<TunnelUiState> {
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText()
            if (content.isBlank()) return emptyList()
            json.decodeFromString<List<TunnelUiState>>(content)
        } catch (_: Exception) {
            emptyList()
        }
    }

    actual suspend fun save(tunnels: List<TunnelUiState>) {
        try {
            val content = json.encodeToString(ListSerializer(TunnelUiState.serializer()), tunnels)
            file.writeText(content)
        } catch (_: Exception) {
            // 写入失败时静默丢弃，避免崩溃；上层可通过日志或返回结果扩展处理。
        }
    }
}

/**
 * 创建 Android 平台 [TunnelConfigStore] 实例。
 *
 * @param context 必须是 [Context] 类型。
 */
actual fun createTunnelConfigStore(context: Any?): TunnelConfigStore {
    val appContext = requireNotNull(context as? Context) {
        "Android TunnelConfigStore requires a Context instance"
    }.applicationContext
    return TunnelConfigStore(File(appContext.filesDir, TUNNELS_FILE_NAME))
}
