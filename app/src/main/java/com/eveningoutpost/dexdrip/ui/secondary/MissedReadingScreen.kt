@file:JvmName("MissedReadingScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.services.MissedReadingService
import com.eveningoutpost.dexdrip.ui.settings.EditPref
import com.eveningoutpost.dexdrip.ui.settings.SettingsMinutesOfDayRow
import com.eveningoutpost.dexdrip.ui.settings.SettingsRingtoneRow
import com.eveningoutpost.dexdrip.ui.settings.SettingsSwitchRow
import com.eveningoutpost.dexdrip.ui.settings.rememberSettingsState
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Pref

/**
 * Track V — `MissedReadingActivity`: the missed-reading alert form. Preferences are written on
 * change (the legacy activity wrote them in `onDestroy`); the service restart is triggered on leave.
 * Java entry point: [installMissedReading].
 */
fun installMissedReading(activity: ComponentActivity) {
    // Legacy seeds the missed-reading sound from the other-alerts sound on first open.
    if (Pref.getString("bg_missed_alerts_sound", null) == null) {
        Pref.setString("bg_missed_alerts_sound", Pref.getString("other_alerts_sound", "default"))
    }
    activity.setContent {
        MissedReadingScreen(onDone = { activity.finish() })
    }
}

@Composable
internal fun MissedReadingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("bg_missed_alerts", false)
    val allDay = state.bool("missed_readings_all_day", true)
    val reraise = state.bool("bg_missed_alerts_enable_alerts_reraise", false)
    val snoozeDefault = remember {
        MissedReadingService.getOtherAlertSnoozeMinutes(Pref.getInstance(), "bg_missed_alerts").toString()
    }

    DisposableEffect(Unit) {
        onDispose { MissedReadingService.delayedLaunch() }
    }

    SecondaryScreen(title = context.getString(R.string.title_missed_reading), onBack = onDone) {
        SettingsSwitchRow(
            title = context.getString(R.string.enable_missed_reading_alert),
            checked = enabled,
            onCheckedChange = { state.setBool("bg_missed_alerts", it) },
            modifier = Modifier.testTag("mra_enable"),
        )
        EditPref(
            state = state,
            key = "bg_missed_minutes",
            title = context.getString(R.string.alert_if_no_data_received_in_colon),
            default = "30",
            numeric = true,
            maxLength = 3,
            enabled = enabled,
            tag = "mra_minutes",
        )
        Text(
            text = context.getString(R.string.select_time_for_alert_colon),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SettingsSwitchRow(
            title = context.getString(R.string.all_day),
            checked = allDay,
            enabled = enabled,
            onCheckedChange = { state.setBool("missed_readings_all_day", it) },
            modifier = Modifier.testTag("mra_all_day"),
        )
        if (!allDay) {
            SettingsMinutesOfDayRow(
                title = context.getString(R.string.select_start_time),
                minutes = state.int("missed_readings_start", 0),
                onTimeChanged = { state.setInt("missed_readings_start", it) },
                enabled = enabled,
                modifier = Modifier.testTag("mra_start"),
            )
            SettingsMinutesOfDayRow(
                title = context.getString(R.string.select_end_time),
                minutes = state.int("missed_readings_end", 0),
                onTimeChanged = { state.setInt("missed_readings_end", it) },
                enabled = enabled,
                modifier = Modifier.testTag("mra_end"),
            )
        }
        EditPref(
            state = state,
            key = "bg_missed_alerts_snooze",
            title = context.getString(R.string.wait_before_raising_the_same_alert_after_snooze_colon),
            default = snoozeDefault,
            numeric = true,
            maxLength = 4,
            enabled = enabled,
            tag = "mra_snooze",
        )
        SettingsSwitchRow(
            title = context.getString(R.string.reraise_alerts_before_snooze_time),
            checked = reraise,
            enabled = enabled,
            onCheckedChange = { state.setBool("bg_missed_alerts_enable_alerts_reraise", it) },
            modifier = Modifier.testTag("mra_reraise"),
        )
        EditPref(
            state = state,
            key = "bg_missed_alerts_reraise_sec",
            title = context.getString(R.string.alert_reraise_time_colon),
            default = "60",
            numeric = true,
            maxLength = 4,
            enabled = enabled && reraise,
            tag = "mra_reraise_sec",
        )
        SettingsRingtoneRow(
            title = context.getString(R.string.alert_tone_colon),
            value = state.string("bg_missed_alerts_sound", "default"),
            onPicked = { state.setString("bg_missed_alerts_sound", it) },
            enabled = enabled,
            modifier = Modifier.testTag("mra_sound"),
        )
        SettingsSwitchRow(
            title = context.getString(R.string.override_silent_mode),
            checked = state.bool("bg_missed_alerts_override_silent", false),
            enabled = enabled,
            onCheckedChange = { state.setBool("bg_missed_alerts_override_silent", it) },
            modifier = Modifier.testTag("mra_override_silent"),
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MissedReadingScreenPreview() {
    XdripPreview {
        MissedReadingScreen(onDone = {})
    }
}

// endregion
