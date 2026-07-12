package cn.lemwood.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import cn.lemwood.theme.AppDimen

/**
 * Shimmer skeleton placeholder that mimics a tunnel card.
 */
@Composable
fun TunnelCardSkeleton(
    modifier: Modifier = Modifier,
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    ).value

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim - 200f, y = 0f),
        end = Offset(x = translateAnim, y = 0f),
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimen.CardHeight),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = AppDimen.CardElevation,
    ) {
        Row(
            modifier = Modifier.padding(AppDimen.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(
                brush = brush,
                modifier = Modifier
                    .size(AppDimen.StatusBadgeSize)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(AppDimen.CardPadding))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBox(
                    brush = brush,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                SkeletonBox(
                    brush = brush,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
            }
            Spacer(modifier = Modifier.width(AppDimen.CardPadding))
            SkeletonBox(
                brush = brush,
                modifier = Modifier
                    .size(width = 40.dp, height = 24.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun SkeletonBox(
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(brush = brush),
    )
}
