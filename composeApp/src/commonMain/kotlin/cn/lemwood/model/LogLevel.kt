package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}
