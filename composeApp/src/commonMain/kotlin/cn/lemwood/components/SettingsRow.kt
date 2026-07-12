package cn.lemwood.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cn.lemwood.theme.AppDimen

@Composable
fun SettingsRow(
    label: String,
    value: String? = null,
    checked: Boolean? = null,
    buttonText: String? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onCheckedChange != null && checked != null) {
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
            if (onButtonClick != null && buttonText != null) {
                TextButton(onClick = onButtonClick) {
                    Text(buttonText)
                }
            }
        }
        Spacer(modifier = Modifier.height(AppDimen.CardPadding))
    }
}
