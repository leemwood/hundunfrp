package cn.lemwood.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lemwood.theme.AppDimen

/**
 * Horizontally scrollable row of filter chips.
 *
 * @param options List of filter labels.
 * @param selected Set of currently selected options.
 * @param onSelect Called when a chip is toggled.
 */
@Composable
fun FilterChips(
    options: List<String>,
    selected: Set<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = AppDimen.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option in selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}
