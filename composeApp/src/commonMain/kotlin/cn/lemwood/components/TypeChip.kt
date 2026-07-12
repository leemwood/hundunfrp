package cn.lemwood.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import cn.lemwood.theme.AppDimen

/**
 * Small rounded chip that displays a protocol type such as TCP, UDP, HTTP, etc.
 */
@Composable
fun TypeChip(
    type: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(AppDimen.ChipHeight),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = type.uppercase(),
            modifier = Modifier.padding(horizontal = AppDimen.CardPadding / 2),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
        )
    }
}
