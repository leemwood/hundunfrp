package cn.lemwood.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.lemwood.theme.AppDimen
import cn.lemwood.theme.ErrorColor

/**
 * Single log entry row colored by log level.
 *
 * @param level One of: DEBUG, INFO, WARN, ERROR.
 * @param message Log message text.
 * @param time Timestamp text.
 */
@Composable
fun LogEntryRow(
    level: String,
    message: String,
    time: String,
    modifier: Modifier = Modifier,
) {
    val upperLevel = level.uppercase()
    val containerColor = when (upperLevel) {
        "WARN" -> Color(0xFFFFA726).copy(alpha = 0.15f)
        "ERROR" -> ErrorColor.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    val contentColor = when (upperLevel) {
        "DEBUG" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        "INFO" -> MaterialTheme.colorScheme.onSurface
        "WARN" -> Color(0xFFFFA726)
        "ERROR" -> ErrorColor
        else -> MaterialTheme.colorScheme.onSurface
    }
    val startBorder = if (upperLevel == "ERROR") {
        Modifier.border(
            width = 3.dp,
            color = ErrorColor,
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(startBorder),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimen.ScreenPadding,
                vertical = AppDimen.CardPadding,
            ),
        ) {
            Text(
                text = "[$upperLevel]",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
            )
        }
    }
}
