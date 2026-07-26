package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
data class TrafficSample(
    val upBytesPerSec: Long = 0L,
    val downBytesPerSec: Long = 0L,
    val timestamp: Long = 0L
)
