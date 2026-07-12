package cn.lemwood.model

import kotlinx.serialization.Serializable

@Serializable
sealed class UIState {
    @Serializable
    data object Idle : UIState()

    @Serializable
    data object Loading : UIState()

    @Serializable
    data class Error(val message: String) : UIState()
}
