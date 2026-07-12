package cn.lemwood.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import cn.lemwood.theme.AppDimen
import cn.lemwood.theme.ConnectingColor
import cn.lemwood.theme.ErrorColor
import cn.lemwood.theme.OfflineColor
import cn.lemwood.theme.OnlineColor

/**
 * 10dp circular status indicator.
 *
 * @param status One of: "online", "offline", "connecting", "error".
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
) {
    val color = when (status.lowercase()) {
        "online" -> OnlineColor
        "offline" -> OfflineColor
        "connecting" -> ConnectingColor
        "error" -> ErrorColor
        else -> OfflineColor
    }

    val isConnecting = status.lowercase() == "connecting"
    val scale = if (isConnecting) {
        val infiniteTransition = rememberInfiniteTransition(label = "status_badge_pulse")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1500
                    0.8f at 0
                    1.2f at 750
                    0.8f at 1500
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "scale",
        )
        animatedScale
    } else {
        1f
    }

    Box(
        modifier = modifier
            .size(AppDimen.StatusBadgeSize)
            .scale(scale)
            .background(color = color, shape = CircleShape),
    )
}
