package com.eveningoutpost.dexdrip.ui.settings

import android.content.res.Configuration
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.ui.theme.ThemeColor
import com.eveningoutpost.dexdrip.ui.theme.ThemeColorGroup
import com.eveningoutpost.dexdrip.ui.theme.ThemeColorStore
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.ui.theme.currentArgb
import com.eveningoutpost.dexdrip.ui.theme.legacyColorDefaults
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder
import com.eveningoutpost.dexdrip.utilitymodels.ColorCacheBridge
import lecho.lib.hellocharts.view.LineChartView

/**
 * Theme editor: the 1:1 Compose equivalent of the legacy `xdrip_plus_color_settings` screen.
 *
 * Colours are grouped and ordered exactly as the legacy XML (see [ThemeColorGroup]); the Material 3
 * chrome roles stay as an additional group. Unset colours follow Material You, any user pick
 * overrides it, and the "reset all" / per-colour "revert to default" actions map to the legacy
 * `theme_reset_all` / `revert_to_default` behaviour. Number-wall colours live on the Number Wall
 * settings screen instead, matching the legacy layout.
 */
@Composable
internal fun ThemeEditorScreen() {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val state = rememberSettingsState()
    // Recompose on any color change (including data colors that don't alter the scheme).
    val revision by ColorCacheBridge.revision.collectAsState()
    @Suppress("UNUSED_EXPRESSION")
    revision

    SettingsCategory(context.getString(R.string.theme_colors)) {
        SettingsActionRow(
            title = "Theme preset: Material You",
            subtitle = "Use the system wallpaper colours (recommended default)",
            onClick = {
                ThemeColorStore.clearAll()
                JoH.static_toast_short(context.getString(R.string.theme_reset_all_done))
            },
            modifier = Modifier.testTag("setting_theme_preset_material_you"),
        )
        SettingsActionRow(
            title = "Theme preset: Classic xDrip",
            subtitle = "Apply the classic xDrip chart colour set",
            onClick = {
                applyClassicPreset()
                JoH.static_toast_short("Classic xDrip colours applied")
            },
            modifier = Modifier.testTag("setting_theme_preset_classic"),
        )
        SettingsActionRow(
            title = context.getString(R.string.theme_reset_all),
            onClick = {
                ThemeColorStore.clearAll()
                JoH.static_toast_short(context.getString(R.string.theme_reset_all_done))
            },
            modifier = Modifier.testTag("setting_theme_reset_all"),
        )
        SettingsExampleChartView()
    }

    for (group in RENDERED_GROUPS) {
        SettingsCategory(context.getString(group.titleRes)) {
            if (group == ThemeColorGroup.Flair) {
                SwitchPref(
                    state,
                    "use_flair_colors",
                    context.getString(R.string.use_flair_colors),
                    default = false,
                    tag = "setting_theme_use_flair_colors",
                )
            }
            ThemeColor.entries.filter { it.group == group }.forEach { color ->
                val overridden = ThemeColorStore.isOverridden(color)
                val enabled = color != ThemeColor.SECONDARY_GLUCOSE_VALUE ||
                    state.bool("plugin_plot_on_graph", false)
                SettingsColorRow(
                    title = context.getString(color.labelRes),
                    subtitle = if (overridden) context.getString(R.string.theme_custom) else null,
                    color = color.currentArgb(scheme),
                    enabled = enabled,
                    showHex = color == ThemeColor.BASAL_TBR,
                    onColorChanged = { ThemeColorStore.setOverride(color, it) },
                    onReset = if (overridden) ({ ThemeColorStore.clearOverride(color) }) else null,
                    modifier = Modifier.testTag("setting_theme_${color.name}"),
                )
            }
        }
    }
}

/** Groups shown in the editor, in legacy order. [ThemeColorGroup.NumberWall] is not rendered. */
private val RENDERED_GROUPS = listOf(
    ThemeColorGroup.Chrome,
    ThemeColorGroup.GlucoseValues,
    ThemeColorGroup.BgValues,
    ThemeColorGroup.TreatmentsPrediction,
    ThemeColorGroup.AverageTarget,
    ThemeColorGroup.AnnotationsDots,
    ThemeColorGroup.Backgrounds,
    ThemeColorGroup.PluginsFeatures,
    ThemeColorGroup.InsulinColors,
    ThemeColorGroup.Flair,
)

private val ThemeColorGroup.titleRes: Int
    get() = when (this) {
        ThemeColorGroup.Chrome -> R.string.theme_group_chrome
        ThemeColorGroup.GlucoseValues -> R.string.glucose_values_and_lines
        ThemeColorGroup.BgValues -> R.string.color_bg_values
        ThemeColorGroup.TreatmentsPrediction -> R.string.treatments_prediction_curves
        ThemeColorGroup.AverageTarget -> R.string.average_and_target_lines
        ThemeColorGroup.AnnotationsDots -> R.string.annotations_and_dots
        ThemeColorGroup.Backgrounds -> R.string.backgrounds
        ThemeColorGroup.PluginsFeatures -> R.string.title_plugins_and_features
        ThemeColorGroup.InsulinColors -> R.string.title_insulin_colors
        ThemeColorGroup.Flair -> R.string.flair_colors
        ThemeColorGroup.NumberWall -> R.string.title_xdrip_plus_number_wall
    }

/**
 * Preview of the chart colours, replacing the legacy `ExampleChartPreferenceView`. Inflates the
 * same layout and drives the same `hellocharts` renderer (Phase 3 will move charts to Vico).
 */
@Composable
private fun SettingsExampleChartView() {
    Column(Modifier.fillMaxWidth().testTag("setting_theme_example_chart")) {
        Text(
            text = stringResource(R.string.example_chart),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            factory = { ctx ->
                val view = LayoutInflater.from(ctx)
                    .inflate(R.layout.prefs_example_chart_layout, null, false)
                runCatching {
                    val chart = view.findViewById<LineChartView>(R.id.example_linechart)
                    chart.setLineChartData(BgGraphBuilder(ctx).lineData())
                    val viewport = chart.maximumViewport
                    chart.isViewportCalculationEnabled = false
                    chart.isInteractive = false
                    chart.currentViewport = viewport
                    chart.setPadding(0, 0, 0, 0)
                    chart.setLeft(0)
                    chart.setTop(0)
                }
                view
            },
        )
    }
}

/**
 * Apply-once "Classic xDrip" preset: seeds [ThemeColorStore] with the legacy XML defaults for every
 * data colour. Chrome roles are left to Material You. No new colour keys or sources of truth.
 */
private fun applyClassicPreset() {
    for (color in ThemeColor.entries) {
        val legacy = color.legacyColor ?: continue
        val default = legacyColorDefaults[legacy] ?: continue
        ThemeColorStore.setOverride(color, default)
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ThemeEditorScreenPreview() {
    XdripPreview { ThemeEditorScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SettingsExampleChartViewPreview() {
    XdripPreview { SettingsExampleChartView() }
}

// endregion
