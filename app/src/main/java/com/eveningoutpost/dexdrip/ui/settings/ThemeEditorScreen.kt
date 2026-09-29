package com.eveningoutpost.dexdrip.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.ui.theme.ThemeColor
import com.eveningoutpost.dexdrip.ui.theme.ThemeColorGroup
import com.eveningoutpost.dexdrip.ui.theme.ThemeColorStore
import com.eveningoutpost.dexdrip.ui.theme.currentArgb
import com.eveningoutpost.dexdrip.utilitymodels.ColorCacheBridge

/**
 * Theme editor: lists every [ThemeColor] with a swatch, opens the Compose picker and offers a
 * per-color "use Material You default" plus a global reset. Unset colors follow Material You;
 * any user pick overrides it.
 */
@Composable
internal fun ThemeEditorScreen() {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    // Recompose on any color change (including data colors that don't alter the scheme).
    val revision by ColorCacheBridge.revision.collectAsState()
    @Suppress("UNUSED_EXPRESSION")
    revision

    SettingsCategory(context.getString(R.string.theme_colors)) {
        SettingsActionRow(
            title = context.getString(R.string.theme_reset_all),
            onClick = {
                ThemeColorStore.clearAll()
                JoH.static_toast_short(context.getString(R.string.theme_reset_all_done))
            },
            modifier = Modifier.testTag("setting_theme_reset_all"),
        )
    }

    for (group in ThemeColorGroup.entries) {
        SettingsCategory(
            context.getString(
                when (group) {
                    ThemeColorGroup.Chrome -> R.string.theme_group_chrome
                    ThemeColorGroup.Data -> R.string.theme_group_data
                }
            )
        ) {
            ThemeColor.entries.filter { it.group == group }.forEach { color ->
                val overridden = ThemeColorStore.isOverridden(color)
                SettingsColorRow(
                    title = context.getString(color.labelRes),
                    subtitle = if (overridden) context.getString(R.string.theme_custom) else null,
                    color = color.currentArgb(scheme),
                    onColorChanged = { ThemeColorStore.setOverride(color, it) },
                    onReset = if (overridden) ({ ThemeColorStore.clearOverride(color) }) else null,
                    modifier = Modifier.testTag("setting_theme_${color.name}"),
                )
            }
        }
    }
}
