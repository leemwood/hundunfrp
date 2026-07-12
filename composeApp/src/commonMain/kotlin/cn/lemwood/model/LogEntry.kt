package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
data class LogEntry(
    val level: LogLevel,
    val message: String,
    val timestamp: Long
)
