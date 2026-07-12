package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val read: Boolean = false
)
