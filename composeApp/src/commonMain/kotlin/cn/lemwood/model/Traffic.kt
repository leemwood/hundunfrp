package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
data class Traffic(
    val up: Long = 0L,
    val down: Long = 0L
)
