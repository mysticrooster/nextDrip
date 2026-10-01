package com.eveningoutpost.dexdrip.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.ui.theme.xdripColor
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache

/** Semantic glucose classification, mapped to the user's picked data colors. */
enum class GlucoseLevel { LOW, IN_RANGE, HIGH }

/** Resolves the user-picked [ColorCache] color for a [GlucoseLevel]. Reactive. */
@Composable
fun glucoseLevelColor(level: GlucoseLevel): Color = when (level) {
    GlucoseLevel.LOW -> xdripColor(ColorCache.X.color_low_bg_values)
    GlucoseLevel.IN_RANGE -> xdripColor(ColorCache.X.color_inrange_bg_values)
    GlucoseLevel.HIGH -> xdripColor(ColorCache.X.color_high_bg_values)
}

/**
 * The large current glucose value shown on the Home screen, with its optional
 * delta. Colored low / in-range / high using the user's picked colors.
 */
@Composable
fun CurrentGlucose(
    value: String,
    delta: String? = null,
    level: GlucoseLevel = GlucoseLevel.IN_RANGE,
    modifier: Modifier = Modifier,
) {
    val color = glucoseLevelColor(level)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            color = color,
            fontSize = 50.sp,
            fontWeight = FontWeight.Normal,
        )
        if (!delta.isNullOrEmpty()) {
            Text(
                text = delta,
                color = color,
                fontSize = 22.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// region Previews

@Preview(name = "Low", showBackground = true)
@Preview(name = "Low (dark)", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CurrentGlucoseLowPreview() {
    XdripPreview { CurrentGlucose(value = "3.4", delta = "-0.8", level = GlucoseLevel.LOW) }
}

@Preview(name = "In range", showBackground = true)
@Preview(name = "In range (dark)", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CurrentGlucoseInRangePreview() {
    XdripPreview { CurrentGlucose(value = "6.1", delta = "+0.2", level = GlucoseLevel.IN_RANGE) }
}

@Preview(name = "High", showBackground = true)
@Preview(name = "High (dark)", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CurrentGlucoseHighPreview() {
    XdripPreview { CurrentGlucose(value = "14.7", delta = "+1.6", level = GlucoseLevel.HIGH) }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun GlucoseLevelColorPreview() {
    XdripPreview {
        Column {
            GlucoseLevel.values().forEach { level ->
                Text(text = level.name, color = glucoseLevelColor(level))
            }
        }
    }
}

// endregion
