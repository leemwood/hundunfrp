package cn.lemwood.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.lemwood.model.AppSettings
import cn.lemwood.model.LogLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Android 平台 [SettingsStore] 实现，基于 DataStore Preferences。
 */
actual class SettingsStore(private val dataStore: DataStore<Preferences>) {

    actual suspend fun load(): AppSettings {
        return dataStore.data.map { prefs ->
            AppSettings(
                serverAddr = prefs[SettingsKeys.SERVER_ADDR] ?: "",
                serverPort = prefs[SettingsKeys.SERVER_PORT] ?: 7000,
                serverToken = prefs[SettingsKeys.SERVER_TOKEN] ?: "",
                autoStart = prefs[SettingsKeys.AUTO_START] ?: false,
                autoReconnect = prefs[SettingsKeys.AUTO_RECONNECT] ?: true,
                notifications = prefs[SettingsKeys.NOTIFICATIONS] ?: false,
                timeoutSeconds = prefs[SettingsKeys.TIMEOUT_SECONDS] ?: 30,
                logLevel = prefs[SettingsKeys.LOG_LEVEL]?.let { runCatching { LogLevel.valueOf(it) }.getOrNull() } ?: LogLevel.INFO,
                theme = prefs[SettingsKeys.THEME] ?: "system",
                dynamicColor = prefs[SettingsKeys.DYNAMIC_COLOR] ?: false
            )
        }.first()
    }

    actual suspend fun save(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.SERVER_ADDR] = settings.serverAddr
            prefs[SettingsKeys.SERVER_PORT] = settings.serverPort
            prefs[SettingsKeys.SERVER_TOKEN] = settings.serverToken
            prefs[SettingsKeys.AUTO_START] = settings.autoStart
            prefs[SettingsKeys.AUTO_RECONNECT] = settings.autoReconnect
            prefs[SettingsKeys.NOTIFICATIONS] = settings.notifications
            prefs[SettingsKeys.TIMEOUT_SECONDS] = settings.timeoutSeconds
            prefs[SettingsKeys.LOG_LEVEL] = settings.logLevel.name
            prefs[SettingsKeys.THEME] = settings.theme
            prefs[SettingsKeys.DYNAMIC_COLOR] = settings.dynamicColor
        }
    }

    private object SettingsKeys {
        val SERVER_ADDR = stringPreferencesKey("server_addr")
        val SERVER_PORT = intPreferencesKey("server_port")
        val SERVER_TOKEN = stringPreferencesKey("server_token")
        val AUTO_START = booleanPreferencesKey("auto_start")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val LOG_LEVEL = stringPreferencesKey("log_level")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}

/**
 * 创建 Android 平台 [SettingsStore] 实例。
 *
 * @param context 必须是 [Context] 类型。
 */
actual fun createSettingsStore(context: Any?): SettingsStore {
    val appContext = requireNotNull(context as? Context) {
        "Android SettingsStore requires a Context instance"
    }.applicationContext
    return SettingsStore(appContext.settingsDataStore)
}
