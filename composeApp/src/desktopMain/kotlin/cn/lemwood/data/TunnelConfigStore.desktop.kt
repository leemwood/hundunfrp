package cn.lemwood.data

import cn.lemwood.model.TunnelUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Desktop 平台 [TunnelConfigStore] 实现。
 * 隧道配置持久化到 D:\\.config\\frp-kmp\\tunnels.json。
 */
actual class TunnelConfigStore {

    private val configDir: File
        get() = File("D:\\.config\\frp-kmp")

    private val tunnelsFile: File
        get() = File(configDir, "tunnels.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    actual suspend fun load(): List<TunnelUiState> = withContext(Dispatchers.IO) {
        configDir.mkdirs()
        if (!tunnelsFile.exists()) {
            return@withContext emptyList()
        }
        try {
            val text = tunnelsFile.readText(Charsets.UTF_8)
            json.decodeFromString(ListSerializer(TunnelUiState.serializer()), text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    actual suspend fun save(tunnels: List<TunnelUiState>) = withContext(Dispatchers.IO) {
        configDir.mkdirs()
        val text = json.encodeToString(ListSerializer(TunnelUiState.serializer()), tunnels)
        tunnelsFile.writeText(text, Charsets.UTF_8)
    }

    actual suspend fun hasData(): Boolean = withContext(Dispatchers.IO) {
        tunnelsFile.exists() && tunnelsFile.length() > 0L
    }
}

actual fun createTunnelConfigStore(context: Any?): TunnelConfigStore = TunnelConfigStore()
