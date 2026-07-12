package cn.lemwood.data

import cn.lemwood.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Desktop 平台 [SettingsStore] 实现。
 * 配置持久化到 D:\\.config\\frp-kmp\\settings.json。
 */
actual class SettingsStore {

    private val configDir: File
        get() = File("D:\\.config\\frp-kmp")

    private val settingsFile: File
        get() = File(configDir, "settings.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    actual suspend fun load(): AppSettings = withContext(Dispatchers.IO) {
        configDir.mkdirs()
        if (!settingsFile.exists()) {
            return@withContext AppSettings()
        }
        try {
            val text = settingsFile.readText(Charsets.UTF_8)
            json.decodeFromString(AppSettings.serializer(), text)
        } catch (_: Exception) {
            AppSettings()
        }
    }

    actual suspend fun save(settings: AppSettings) = withContext(Dispatchers.IO) {
        configDir.mkdirs()
        val text = json.encodeToString(AppSettings.serializer(), settings)
        settingsFile.writeText(text, Charsets.UTF_8)
    }
}

actual fun createSettingsStore(context: Any?): SettingsStore = SettingsStore()
