package cn.lemwood.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import cn.lemwood.components.FilterChips
import cn.lemwood.theme.AppDimen
import cn.lemwood.theme.DownloadTrafficColor
import cn.lemwood.theme.UploadTrafficColor
import kotlin.math.roundToInt

data class TrafficDataPoint(
    val label: String,
    val upload: Float,
    val download: Float,
)

@Composable
fun TrafficChart(
    dataPoints: List<TrafficDataPoint>,
    modifier: Modifier = Modifier,
) {
    val timeRanges = listOf("5min", "1h", "今日", "本周")
    var selectedRange by remember { mutableStateOf(setOf("今日")) }

    Column(
        modifier = modifier,
    ) {
        FilterChips(
            options = timeRanges,
            selected = selectedRange,
            onSelect = { selectedRange = setOf(it) },
        )
        Spacer(modifier = Modifier.height(AppDimen.CardPadding))

        val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
        val textColor = MaterialTheme.colorScheme.onSurfaceVariant

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        ) {
            val paddingLeft = 0f
            val paddingBottom = 16f
            val paddingTop = 4f
            val chartWidth = size.width - paddingLeft
            val chartHeight = size.height - paddingBottom - paddingTop

            val maxVal = (dataPoints.maxOf { maxOf(it.upload, it.download) } * 1.2f).coerceAtLeast(1f)

            if (dataPoints.size < 2) return@Canvas

            val stepX = chartWidth / (dataPoints.size - 1)

            val uploadPath = Path()
            val downloadPath = Path()

            dataPoints.forEachIndexed { index, point ->
                val x = paddingLeft + index * stepX
                val uploadY = paddingTop + chartHeight - (point.upload / maxVal * chartHeight)
                val downloadY = paddingTop + chartHeight - (point.download / maxVal * chartHeight)

                if (index == 0) {
                    uploadPath.moveTo(x, uploadY)
                    downloadPath.moveTo(x, downloadY)
                } else {
                    uploadPath.lineTo(x, uploadY)
                    downloadPath.lineTo(x, downloadY)
                }
            }

            drawPath(
                path = uploadPath,
                color = UploadTrafficColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            drawPath(
                path = downloadPath,
                color = DownloadTrafficColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            LegendItem(color = UploadTrafficColor, label = "上行")
            Spacer(modifier = Modifier.height(16.dp))
            LegendItem(color = DownloadTrafficColor, label = "下行")
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(modifier = Modifier.height(10.dp).fillMaxWidth(0.04f)) {
            drawCircle(color = color, radius = 4.dp.toPx())
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun generateMockTrafficData(count: Int): List<TrafficDataPoint> {
    val random = kotlin.random.Random(42)
    return (0 until count).map { i ->
        TrafficDataPoint(
            label = "${i * 5}min",
            upload = random.nextFloat() * 2_000_000f,
            download = random.nextFloat() * 4_000_000f,
        )
    }
}
