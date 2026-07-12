package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val serverAddr: String = "",
    val serverPort: Int = 7000,
    val serverToken: String = "",
    val autoStart: Boolean = false,
    val autoReconnect: Boolean = true,
    val notifications: Boolean = false,
    val timeoutSeconds: Int = 30,
    val logLevel: LogLevel = LogLevel.INFO,
    val theme: String = "system",
    val dynamicColor: Boolean = false,
    val hasCompletedOnboarding: Boolean = false
)
