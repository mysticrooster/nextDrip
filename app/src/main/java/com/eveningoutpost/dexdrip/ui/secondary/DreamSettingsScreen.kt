@file:JvmName("DreamSettingsScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.settings.SettingsSwitchRow
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Pref

/**
 * Track V pass 2 — `XDripDreamSettingsActivity`: daydream (screensaver) options. Java entry point:
 * [installDreamSettings].
 */
fun installDreamSettings(activity: ComponentActivity) {
    activity.setContent {
        DreamSettingsScreen(onBack = { activity.finish() })
    }
}

@Composable
internal fun DreamSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var gravity by remember { mutableStateOf(Pref.getBoolean("daydream_use_gravity_sensor", false)) }

    SecondaryScreen(title = context.getString(R.string.title_activity_dream_settings), onBack = onBack) {
        SettingsSwitchRow(
            title = context.getString(R.string.use_gravity_sensor_to_rotate_items),
            checked = gravity,
            onCheckedChange = {
                gravity = it
                Pref.setBoolean("daydream_use_gravity_sensor", it)
            },
            modifier = Modifier.testTag("dream_gravity_switch"),
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun DreamSettingsScreenPreview() {
    XdripPreview {
        DreamSettingsScreen(onBack = {})
    }
}

// endregion
