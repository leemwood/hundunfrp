package cn.lemwood.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Single line that renders "local -> remote".
 */
@Composable
fun AddressLine(
    local: String,
    remote: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "$local → $remote",
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
