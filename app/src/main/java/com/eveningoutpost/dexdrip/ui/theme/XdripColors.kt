package com.eveningoutpost.dexdrip.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache
import com.eveningoutpost.dexdrip.utilitymodels.ColorCacheBridge

/** Converts an ARGB color int (as stored by [ColorCache]) to a Compose [Color]. */
fun Int.asComposeColor(): Color = Color(
    red = (this shr 16 and 0xFF) / 255f,
    green = (this shr 8 and 0xFF) / 255f,
    blue = (this and 0xFF) / 255f,
    alpha = (this ushr 24) / 255f,
)

/**
 * Reactive read of a user-picked [ColorCache] data color.
 *
 * Resolves to the user's override when set, else the Material You default for that data role, so
 * unset colors follow the theme and user-picked colors always win. Recomputes whenever
 * [ColorCache.invalidateCache] is called (e.g. after a user picks a new color).
 */
@Composable
fun xdripColor(key: ColorCache.X): Color {
    val revision by ColorCacheBridge.revision.collectAsState()
    val scheme = MaterialTheme.colorScheme
    return remember(key, revision, scheme) {
        val role = ThemeColor.fromLegacyColor(key)
        val override = role?.let { ThemeColorStore.overrideArgb(it) }
        when {
            override != null -> override.asComposeColor()
            role != null -> role.defaultFrom(scheme)
            else -> ColorCache.getCol(key).asComposeColor()
        }
    }
}

/** Snapshot of the most commonly used data colors, sourced from [ColorCache]. */
@Immutable
data class XdripColors(
    val highValues: Color,
    val lowValues: Color,
    val inRangeValues: Color,
    val highBgValues: Color,
    val lowBgValues: Color,
    val inRangeBgValues: Color,
    val homeChartBackground: Color,
)

@Composable
fun currentXdripColors(): XdripColors {
    return XdripColors(
        highValues = xdripColor(ColorCache.X.color_high_values),
        lowValues = xdripColor(ColorCache.X.color_low_values),
        inRangeValues = xdripColor(ColorCache.X.color_inrange_values),
        highBgValues = xdripColor(ColorCache.X.color_high_bg_values),
        lowBgValues = xdripColor(ColorCache.X.color_low_bg_values),
        inRangeBgValues = xdripColor(ColorCache.X.color_inrange_bg_values),
        homeChartBackground = xdripColor(ColorCache.X.color_home_chart_background),
    )
}

val LocalXdripColors = staticCompositionLocalOf {
    XdripColors(
        highValues = Color.Unspecified,
        lowValues = Color.Unspecified,
        inRangeValues = Color.Unspecified,
        highBgValues = Color.Unspecified,
        lowBgValues = Color.Unspecified,
        inRangeBgValues = Color.Unspecified,
        homeChartBackground = Color.Unspecified,
    )
}
