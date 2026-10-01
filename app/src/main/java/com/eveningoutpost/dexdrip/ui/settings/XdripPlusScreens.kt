package com.eveningoutpost.dexdrip.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.eveningoutpost.dexdrip.Home
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.WidgetUpdateService
import com.eveningoutpost.dexdrip.insulin.InsulinProfileEditor
import com.eveningoutpost.dexdrip.insulin.inpen.InPenEntry
import com.eveningoutpost.dexdrip.models.DesertSync
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.services.PlusSyncService
import com.eveningoutpost.dexdrip.ui.activities.NumberWallPreview
import com.eveningoutpost.dexdrip.ui.activities.SelectAudioDevice
import com.eveningoutpost.dexdrip.ui.activities.TimePickerPrefActivity
import com.eveningoutpost.dexdrip.ui.theme.ThemeColor
import com.eveningoutpost.dexdrip.ui.theme.ThemeColorStore
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.ui.theme.currentArgb
import com.eveningoutpost.dexdrip.utilitymodels.ColorCacheBridge
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utilitymodels.ShotStateStore
import com.eveningoutpost.dexdrip.utils.CipherUtils
import com.eveningoutpost.dexdrip.utils.DisplayQRCode
import com.eveningoutpost.dexdrip.utils.LocationHelper
import com.eveningoutpost.dexdrip.utils.SdcardImportExport
import com.eveningoutpost.dexdrip.utils.time.TimeRangeUtils
import com.eveningoutpost.dexdrip.xDripWidget

/**
 * S5b — xDrip+ Extra Settings (`xdrip_plus_prefs.xml`) in Compose.
 *
 * Copying, Update, Motion, Experimental → Pens, Prediction, Sync and Display screens. Preference
 * keys/types/defaults are unchanged; side effects mirror the legacy `AllPrefsFragment` listeners
 * (see `Documentation/technical/Settings_Migration.md`). The theme/colour screen is the legacy
 * `xdrip_plus_color_settings` equivalent and lives in [ThemeEditorScreen].
 */

/* -------------------------------------------------------------------------- Slice 2 — Copying */

@Composable
internal fun CopyingSettingsScreen() {
    val context = LocalContext.current
    SettingsCategory(context.getString(R.string.copying_settings)) {
        SettingsActionRow(
            title = context.getString(R.string.show_settings_qr_codes),
            onClick = { context.startActivity(Intent(context, DisplayQRCode::class.java)) },
            modifier = Modifier.testTag("setting_show_qr_codes"),
        )
        SettingsActionRow(
            title = context.getString(R.string.load_save_settings_to_sdcard),
            onClick = { context.startActivity(Intent(context, SdcardImportExport::class.java)) },
            modifier = Modifier.testTag("setting_show_external_save"),
        )
    }
}

/* --------------------------------------------------------------------------- Slice 3 — Update */

@Composable
internal fun UpdateSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val engineering = SettingsVisibility.isEngineeringMode(state)
    val currentChannel = state.string("update_channel", "beta")
    val extended = engineering || currentChannel.matches(Regex("alpha|nightly"))
    val entries = remember(extended) {
        val res = if (extended) R.array.UpdateChannelDetailE else R.array.UpdateChannelDetail
        context.resources.getStringArray(res).toList()
    }
    val values = remember(extended) {
        val res = if (extended) R.array.UpdateChannelE else R.array.UpdateChannel
        context.resources.getStringArray(res).toList()
    }

    SettingsCategory(context.getString(R.string.xdrip_plus_update_settings)) {
        val autoUpdate = state.bool("auto_update_download", true)
        SwitchPref(
            state,
            "auto_update_download",
            context.getString(R.string.automatic_update_check),
            default = true,
            subtitle = if (autoUpdate) {
                context.getString(R.string.get_notified_of_new_apk_releases)
            } else {
                "New releases often have bugs fixed. It is recommended to switch on update notifications!"
            },
            tag = "setting_auto_update_download",
        )
        ListPref(
            state,
            "update_channel",
            context.getString(R.string.update_channel),
            entries = entries,
            values = values,
            default = "beta",
            subtitle = context.getString(R.string.choose_stable_beta_or_alpha_releases),
            tag = "setting_update_channel",
        )
        SwitchPref(
            state,
            "enable_crashlytics",
            context.getString(R.string.automatic_crash_reporting),
            default = true,
            subtitle = context.getString(R.string.send_crash_errors_to_developer),
            tag = "setting_enable_crashlytics",
            onCheckedChange = { checked ->
                state.setBool("enable_crashlytics", checked)
                JoH.static_toast_long("Crash Setting takes effect on next restart")
            },
        )
        SwitchPref(
            state,
            "enable_telemetry",
            context.getString(R.string.enable_telemetry),
            default = true,
            subtitle = context.getString(R.string.send_data_to_developers),
            enabled = state.dependentEnabled("enable_crashlytics", true),
            tag = "setting_enable_telemetry",
        )
    }
}

/* --------------------------------------------------------------------------- Slice 4 — Motion */

@Composable
internal fun MotionSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val motion = state.bool("motion_tracking_enabled", false)
    val vehicle = state.bool("vehicle_mode_enabled", false)

    SettingsCategory(context.getString(R.string.xdrip_motion_tracking)) {
        SwitchPref(
            state,
            "motion_tracking_enabled",
            context.getString(R.string.enable_motion_tracking),
            default = false,
            subtitle = context.getString(R.string.detect_motion_types),
            tag = "setting_motion_enabled",
        )
        SwitchPref(
            state,
            "plot_motion",
            context.getString(R.string.log_and_plot_motion),
            default = true,
            subtitle = context.getString(R.string.display_motion_types),
            enabled = motion,
            tag = "setting_plot_motion",
        )
        SwitchPref(
            state,
            "use_remote_motion",
            context.getString(R.string.use_remote_motion),
            default = false,
            subtitle = context.getString(R.string.use_remote_motion_data),
            enabled = motion,
            tag = "setting_use_remote_motion",
        )
        SwitchPref(
            state,
            "act_as_motion_master",
            context.getString(R.string.act_as_motion_master),
            default = false,
            subtitle = context.getString(R.string.be_motion_master),
            enabled = motion,
            tag = "setting_act_as_motion_master",
        )
        SwitchPref(
            state,
            "vehicle_mode_enabled",
            context.getString(R.string.enable_vehicle_mode),
            default = false,
            subtitle = context.getString(R.string.vehicle_motion_extras),
            tag = "setting_vehicle_enabled",
        )
        SwitchPref(
            state,
            "vehicle_mode_via_car_audio",
            context.getString(R.string.detect_car_audio),
            default = false,
            subtitle = context.getString(R.string.automatically_enable_when_connected_to_car_bluetooth),
            enabled = vehicle,
            tag = "setting_vehicle_car_audio",
        )
        SettingsActionRow(
            title = context.getString(R.string.learn_current_car_audio),
            subtitle = context.getString(R.string.tap_to_set_current_car_audio_device),
            enabled = vehicle,
            onClick = {
                context.startActivity(
                    Intent(context, SelectAudioDevice::class.java).putExtra("none", "none")
                )
            },
            modifier = Modifier.testTag("setting_learn_audio"),
        )
        SwitchPref(
            state,
            "raise_low_limit_in_vehicle_mode",
            context.getString(R.string.raise_low_threshold),
            default = false,
            subtitle = context.getString(R.string.increase_low_alarms_vehicle_mode),
            enabled = vehicle,
            tag = "setting_raise_low_vehicle",
        )
        SwitchPref(
            state,
            "speak_readings_in_vehicle_mode",
            context.getString(R.string.speak_readings),
            default = false,
            subtitle = context.getString(R.string.enable_speak_readings_when_in_vehicle),
            enabled = vehicle,
            tag = "setting_speak_vehicle",
        )
        SwitchPref(
            state,
            "play_sound_in_vehicle_mode",
            context.getString(R.string.play_sound),
            default = true,
            subtitle = context.getString(R.string.notification_sound_vehicle_mode),
            enabled = vehicle,
            tag = "setting_play_sound_vehicle",
        )
        SwitchPref(
            state,
            "repeat_sound_in_vehicle_mode",
            context.getString(R.string.repeat_sound),
            default = true,
            subtitle = context.getString(R.string.repeat_notification_every_90_minutes),
            enabled = vehicle,
            tag = "setting_repeat_sound_vehicle",
        )
    }
}

/* ---------------------------------------------------------- Slice 5 — Experimental → Pens */

@Composable
internal fun PensScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    SettingsCategory(context.getString(R.string.insulin_pens)) {
        SettingsActionRow(
            title = context.getString(R.string.title_novopen_insulin_pen),
            subtitle = "NFC dose downloading of models 6 and Echo Plus",
            onClick = { onNavigate(SettingsScreen.XdripPlusNovopen) },
            modifier = Modifier.testTag("setting_pens_novopen"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_inpen_screen),
            subtitle = context.getString(R.string.summary_inpen_screen),
            onClick = { onNavigate(SettingsScreen.XdripPlusInpen) },
            modifier = Modifier.testTag("setting_pens_inpen"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_pendiq_screen),
            subtitle = context.getString(R.string.summary_pendiq_screen),
            onClick = { onNavigate(SettingsScreen.XdripPlusPendiq) },
            modifier = Modifier.testTag("setting_pens_pendiq"),
        )
    }
}

@Composable
internal fun NovopenScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("opennov_enabled", false)
    val engineering = SettingsVisibility.isEngineeringMode(state)

    SettingsCategory(context.getString(R.string.title_novopen_insulin_pen)) {
        SwitchPref(state, "opennov_enabled", context.getString(R.string.enable), default = false, subtitle = context.getString(R.string.summary_inpen_open_nov_support), tag = "setting_opennov_enabled")
        SwitchPref(state, "opennov_play_sounds", context.getString(R.string.title_inpen_sounds), default = true, subtitle = context.getString(R.string.summary_inpen_sounds), enabled = enabled)
        SwitchPref(state, "opennov_remove_priming", context.getString(R.string.title_inpen_remove_priming_doses), default = false, subtitle = context.getString(R.string.summary_inpen_remove_priming_doses), enabled = enabled)
        SwitchPref(state, "opennov_hide_priming", context.getString(R.string.title_inpen_hide_priming_doses), default = false, subtitle = context.getString(R.string.summary_inpen_hide_priming_doses), enabled = enabled)
        EditPref(
            state,
            "opennov_prime_units",
            context.getString(R.string.title_inpen_prime_units),
            default = "2.0",
            subtitle = context.getString(R.string.summary_inpen_prime_units),
            masked = true,
            maxLength = 3,
            enabled = enabled,
            tag = "setting_opennov_prime_units",
        )
        EditPref(
            state,
            "opennov_prime_minutes",
            context.getString(R.string.title_inpen_prime_minutes),
            default = "2",
            subtitle = context.getString(R.string.summary_inpen_prime_minutes),
            masked = true,
            maxLength = 3,
            enabled = enabled,
            tag = "setting_opennov_prime_minutes",
        )
        if (engineering) {
            SwitchPref(state, "opennov_debug", context.getString(R.string.title_inpen_additional_debug_logs), default = false, subtitle = context.getString(R.string.summary_inpen_additional_debug_logs), tag = "setting_opennov_debug")
        }
    }
}

@Composable
internal fun InpenScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()

    SettingsCategory(context.getString(R.string.title_inpen_screen)) {
        SwitchPref(
            state,
            "inpen_enabled",
            context.getString(R.string.title_inpen_enabled),
            default = false,
            tag = "setting_inpen_enabled",
            onCheckedChange = { checked ->
                state.setBool("inpen_enabled", checked)
                if (checked) {
                    (context as? android.app.Activity)?.let { LocationHelper.requestLocationForBluetooth(it) }
                }
                InPenEntry.startWithRefresh()
            },
        )
        SwitchPref(state, "inpen_detect_priming", context.getString(R.string.title_inpen_detect_priming), default = true, subtitle = context.getString(R.string.summary_inpen_detect_priming))
        EditPref(state, "inpen_prime_units", context.getString(R.string.title_inpen_prime_units), default = "2.0", masked = true, maxLength = 4, tag = "setting_inpen_prime_units")
        EditPref(state, "inpen_prime_minutes", context.getString(R.string.title_inpen_prime_minutes), default = "2", masked = true, maxLength = 3, tag = "setting_inpen_prime_minutes")
        SwitchPref(state, "inpen_orphan_primes_ignored", context.getString(R.string.title_inpen_orphan_primes_ignored), default = true, subtitle = context.getString(R.string.summary_inpen_orphan_primes_ignored))
        SwitchPref(state, "inpen_sounds", context.getString(R.string.title_inpen_sounds), default = true, subtitle = context.getString(R.string.summary_inpen_sounds))
        SettingsActionRow(
            title = context.getString(R.string.title_inpen_reset),
            subtitle = context.getString(R.string.summary_inpen_reset),
            onClick = {
                context.startActivity(Intent(context, Home::class.java).putExtra("inpen-reset", "inpen-reset"))
            },
            modifier = Modifier.testTag("setting_inpen_reset"),
        )
    }
}

@Composable
internal fun PendiqScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("use_pendiq", false)
    val pin = state.string("pendiq_pin", "0000")

    SettingsCategory(context.getString(R.string.title_pendiq_screen)) {
        SwitchPref(state, "use_pendiq", context.getString(R.string.title_use_pendiq), default = false, tag = "setting_use_pendiq")
        SwitchPref(state, "pendiq_send_treatments", context.getString(R.string.title_pendiq_send_treatments), default = false, subtitle = context.getString(R.string.summary_pendiq_send_treatments), enabled = enabled)
        EditPref(
            state,
            "pendiq_pin",
            "${context.getString(R.string.title_pendiq_pin)} ($pin)",
            default = "0000",
            masked = true,
            maxLength = 4,
            enabled = enabled,
            tag = "setting_pendiq_pin",
        )
    }
}

/* ----------------------------------------------------------------------- Slice 6 — Prediction */

@Composable
internal fun PredictionSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val gate = state.bool("I_understand", false)

    SettingsCategory(context.getString(R.string.xdrip_plus_prediction_settings)) {
        SwitchPref(state, "simulations_enabled", context.getString(R.string.predictive_simulations), default = true, subtitle = context.getString(R.string.display_mathamatical_simulations), enabled = gate, tag = "setting_simulations_enabled")
        SwitchPref(state, "predict_use_momentum", context.getString(R.string.use_trend_momentum), default = true, subtitle = context.getString(R.string.calculate_including_glucose_trend), enabled = gate)
    }
}

@Composable
internal fun MultipleInsulinScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("multiple_insulin_types", false)

    SettingsCategory(context.getString(R.string.title_multiple_insulin_types_settings)) {
        SwitchPref(state, "multiple_insulin_types", context.getString(R.string.title_multiple_insulin_types), default = false, tag = "setting_multiple_insulin_types")
        SettingsActionRow(
            title = context.getString(R.string.enable_dedicated_insulin_profiles),
            subtitle = context.getString(R.string.desc_enable_dedicated_insulin_profiles),
            enabled = enabled,
            onClick = { context.startActivity(Intent(context, InsulinProfileEditor::class.java)) },
            modifier = Modifier.testTag("setting_insulin_profiles"),
        )
        SwitchPref(state, "multiple_insulin_use_basal_activity", context.getString(R.string.title_multiple_insulin_use_basal_activity), default = false, subtitle = context.getString(R.string.summary_multiple_insulin_use_basal_activity))
    }
}

@Composable
internal fun AdvPredictScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val gate = state.bool("I_understand", false)

    SettingsCategory(context.getString(R.string.low_level_prediction_values)) {
        EditPref(state, "plus_target_range", context.getString(R.string.target_glucose_default), default = "100", decimal = true, enabled = gate, tag = "setting_plus_target_range")
        EditPref(state, "xplus_insulin_dia", context.getString(R.string.insulin_duration_hours), default = "3.0", decimal = true, enabled = gate, tag = "setting_xplus_insulin_dia")
        EditPref(state, "xplus_liver_sensitivity", context.getString(R.string.default_liver_sensitivity_ratio), default = "2", decimal = true, enabled = gate, tag = "setting_xplus_liver_sensitivity")
        EditPref(state, "xplus_liver_maximpact", context.getString(R.string.default_liver_maximum_impact), default = "0.8", decimal = true, enabled = gate, tag = "setting_xplus_liver_maximpact")
    }
}

/* ---------------------------------------------------------------------------- Slice 7 — Sync */

@Composable
internal fun SyncSettingsScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val gate = state.bool("I_understand", false)
    val followMaster = state.bool("plus_follow_master", false)

    // Legacy AllPrefsFragment generates a random sync key when the screen opens and the key is empty.
    LaunchedEffect(Unit) {
        if (state.string("custom_sync_key", "").isEmpty()) {
            state.setString("custom_sync_key", CipherUtils.getRandomHexKey())
        }
    }

    SettingsCategory(context.getString(R.string.xdrip_plus_sync_settings)) {
        SwitchPref(
            state,
            "use_custom_sync_key",
            context.getString(R.string.sync_using_custom_security_key),
            default = true,
            subtitle = context.getString(R.string.key_is_used_instead_of_google_account),
            enabled = false,
            tag = "setting_use_custom_sync_key",
        )
        EditPref(
            state,
            "custom_sync_key",
            context.getString(R.string.handset_group_security_key),
            default = "",
            subtitle = context.getString(R.string.handset_sync_grouping_key),
            enabled = gate,
            tag = "setting_custom_sync_key",
            onValueChange = { value ->
                state.setString("custom_sync_key", value)
                PlusSyncService.clearandRestartSyncService(context)
            },
        )
        SwitchPref(state, "plus_follow_master", context.getString(R.string.be_master_for_followers), default = false, subtitle = context.getString(R.string.this_device_will_send_data_to_followers), enabled = gate, tag = "setting_plus_follow_master")
        SwitchPref(state, "plus_accept_follower_actions", context.getString(R.string.title_plus_accept_follower_actions), default = true, subtitle = context.getString(R.string.summary_plus_accept_follower_actions), enabled = gate && followMaster)
        SwitchPref(state, "use_xdrip_cloud_sync", context.getString(R.string.use_xdrip_cloud), default = false, subtitle = context.getString(R.string.summary_plus_xdrip_cloud), enabled = gate, tag = "setting_use_xdrip_cloud")
        SwitchPref(state, "plus_follower_save_power", context.getString(R.string.save_power), default = true, subtitle = context.getString(R.string.reduce_battery_and_network_overhead), enabled = gate)
        SwitchPref(state, "plus_whole_house", context.getString(R.string.title_plus_whole_house), default = false, subtitle = context.getString(R.string.summary_plus_whole_house), enabled = gate && followMaster)
        SwitchPref(state, "libre_whole_house_collector", context.getString(R.string.title_libre_whole_house_collector), default = false, subtitle = context.getString(R.string.summary_libre_whole_house_collector), enabled = gate && followMaster)
        SettingsActionRow(
            title = context.getString(R.string.remote_snoozing),
            subtitle = context.getString(R.string.remote_snoozes),
            enabled = gate,
            onClick = { onNavigate(SettingsScreen.XdripPlusRemoteSnooze) },
            modifier = Modifier.testTag("setting_remote_snooze"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_xdrip_plus_desert_sync_settings),
            subtitle = context.getString(R.string.summary_xdrip_plus_desert_sync_settings),
            enabled = gate,
            onClick = { onNavigate(SettingsScreen.XdripPlusDesertSync) },
            modifier = Modifier.testTag("setting_desert_sync"),
        )
        SwitchPref(
            state,
            "disable_all_sync",
            context.getString(R.string.disable_all_sync_features),
            default = false,
            subtitle = context.getString(R.string.temporary_work_around_disable_all_sync_detail),
            enabled = gate,
            tag = "setting_disable_all_sync",
            onCheckedChange = { checked ->
                state.setBool("disable_all_sync", checked)
                SdcardImportExport.hardReset()
            },
        )
    }
}

@Composable
internal fun RemoteSnoozeScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.remote_snoozing)) {
        SwitchPref(state, "send_snooze_to_remote", context.getString(R.string.send_snooze_to_all), default = false, subtitle = context.getString(R.string.snoozes_will_silence_all))
        SwitchPref(state, "confirm_snooze_to_remote", context.getString(R.string.confirm_sensing_snooze), default = false, subtitle = context.getString(R.string.confirm_remote_snoozes), enabled = state.dependentEnabled("send_snooze_to_remote", false))
        SwitchPref(state, "accept_remote_snoozes", context.getString(R.string.acccept_remote_snoozes), default = false, subtitle = context.getString(R.string.allow_remotes_silence))
        SwitchPref(state, "remote_snoozes_wifi_match", context.getString(R.string.wifi_name_must_match), default = true, subtitle = context.getString(R.string.only_accept_same_network), enabled = state.dependentEnabled("accept_remote_snoozes", false))
    }
    SettingsCategory(context.getString(R.string.title_xdrip_plus_snooze_broadcast)) {
        SwitchPref(state, "broadcast_snooze", context.getString(R.string.title_broadcast_snooze), default = false, subtitle = context.getString(R.string.summary_broadcast_snooze))
        SwitchPref(state, "accept_broadcast_snooze", context.getString(R.string.title_accept_broadcast_snooze), default = false, subtitle = context.getString(R.string.summary_accept_broadcast_snooze))
    }
}

@Composable
internal fun DesertSyncScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("desert_sync_enabled", false)
    val isMaster = Home.get_master()

    SettingsCategory(context.getString(R.string.title_xdrip_plus_desert_sync_settings)) {
        SwitchPref(
            state,
            "desert_sync_enabled",
            context.getString(R.string.title_desert_sync_enabled),
            default = false,
            subtitle = context.getString(R.string.summary_desert_sync_enabled),
            tag = "setting_desert_sync_enabled",
            onCheckedChange = { checked ->
                state.setBool("desert_sync_enabled", checked)
                DesertSync.settingsChanged()
            },
        )
        if (!isMaster) {
            EditPref(
                state,
                "desert_sync_master_ip",
                context.getString(R.string.title_desert_sync_master_ip),
                default = "",
                maxLength = 15,
                enabled = enabled,
                tag = "setting_desert_sync_master_ip",
            )
        }
        SettingsActionRow(
            title = context.getString(R.string.title_send_sync_show_qr),
            subtitle = context.getString(R.string.summary_send_sync_show_qr),
            enabled = enabled,
            onClick = {
                context.startActivity(
                    Intent(context, DisplayQRCode::class.java).setAction("xdrip_plus_desert_sync_qr")
                )
            },
            modifier = Modifier.testTag("setting_send_sync_qr"),
        )
        SwitchPref(state, "desert_use_https", context.getString(R.string.title_desert_use_https), default = false, subtitle = context.getString(R.string.summary_desert_use_https))
    }
}

/* ------------------------------------------------------------------------- Slice 8 — Display */

@Composable
internal fun XdripPlusDisplayScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val engineering = SettingsVisibility.isEngineeringMode(state)
    val noise = state.bool("bg_compensate_noise", true)
    val bwp = state.bool("show_bwp", false)

    SettingsCategory(context.getString(R.string.xdrip_plus_display_settings)) {
        SettingsActionRow(
            title = context.getString(R.string.title_font_settings),
            subtitle = context.getString(R.string.summary_font_settings),
            onClick = { onNavigate(SettingsScreen.XdripPlusFont) },
            modifier = Modifier.testTag("setting_display_font"),
        )
        SwitchPref(state, "bg_compensate_noise", context.getString(R.string.title_bg_compensate_noise), default = true, subtitle = context.getString(R.string.try_to_work_around_noisy_readings), tag = "setting_bg_compensate_noise")
        if (engineering) {
            SwitchPref(
                state,
                "bg_compensate_noise_ultrasensitive",
                context.getString(R.string.title_bg_compensate_noise_ultrasensitive),
                default = false,
                subtitle = context.getString(R.string.try_to_work_around_noisy_readings_ultrasensitive),
                enabled = noise,
                tag = "setting_bg_compensate_noise_ultrasensitive",
            )
        }
        SwitchPref(
            state,
            "show_showcase",
            context.getString(R.string.show_interface_hints),
            default = true,
            subtitle = context.getString(R.string.show_tips_hints),
            tag = "setting_show_showcase",
            onCheckedChange = { checked ->
                state.setBool("show_showcase", checked)
                if (checked) {
                    ShotStateStore.resetAllShots()
                    JoH.static_toast_long(context.getString(R.string.interface_tips_from_start))
                }
            },
        )
        SwitchPref(state, "bg_from_filtered", context.getString(R.string.glucose_number_from_filtered), default = false, subtitle = context.getString(R.string.delayed_but_more_stable))
        SwitchPref(state, "show_bwp", context.getString(R.string.show_bolus_wizard_preview), default = false, subtitle = context.getString(R.string.display_calculations), tag = "setting_show_bwp")
        SwitchPref(state, "always_show_bwp", context.getString(R.string.always_show_bolus_wizard_preview), default = false, subtitle = context.getString(R.string.display_calculations_everytime), enabled = bwp)
        SwitchPref(state, "show_home_on_boot", context.getString(R.string.title_show_home_on_boot), default = false, subtitle = context.getString(R.string.summary_show_home_on_boot))
    }
}

@Composable
internal fun FontSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.title_font_settings)) {
        SwitchPref(state, "enlarge_fonts_on_large_screens", context.getString(R.string.title_enlarge_fonts_on_large_screens), default = true, subtitle = context.getString(R.string.summary_enlarge_fonts_on_large_screens), tag = "setting_enlarge_fonts")
    }
}

@Composable
internal fun LanguageSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val forceEnglish = state.bool("force_english", false)
    val language = state.string("forced_language", "en")
    val localeEntries = remember { context.resources.getStringArray(R.array.LocaleChoices).toList() }
    val localeValues = remember { context.resources.getStringArray(R.array.LocaleChoicesValues).toList() }
    val languageName = localeEntries.getOrElse(localeValues.indexOf(language)) { language }

    SettingsCategory(context.getString(R.string.title_language)) {
        SettingsSwitchRow(
            title = "Force $languageName Text",
            subtitle = if (forceEnglish) context.getString(R.string.forcing_alternate_language) else context.getString(R.string.using_local_language),
            checked = forceEnglish,
            onCheckedChange = { checked ->
                state.setBool("force_english", checked)
                SdcardImportExport.hardReset()
            },
            modifier = Modifier.testTag("setting_force_english"),
        )
        SettingsListRow(
            title = context.getString(R.string.chosse_language),
            subtitle = context.getString(R.string.need_alternate_language),
            entries = localeEntries,
            values = localeValues,
            selectedValue = language,
            onSelected = { value ->
                state.setString("forced_language", value)
                if (state.bool("force_english", false)) SdcardImportExport.hardReset()
            },
            modifier = Modifier.testTag("setting_forced_language"),
        )
    }
}

@Composable
internal fun GraphDisplayScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val engineering = SettingsVisibility.isEngineeringMode(state)
    val filtered = state.bool("show_filtered_curve", true)

    SettingsCategory(context.getString(R.string.title_xdrip_plus_graph_display_settings)) {
        SettingsActionRow(
            title = context.getString(R.string.graph_smoothing),
            onClick = { onNavigate(SettingsScreen.XdripPlusGraphSmoothing) },
            modifier = Modifier.testTag("setting_graph_smoothing_screen"),
        )
        SwitchPref(state, "rewrite_history", context.getString(R.string.rewrite_history), default = false, subtitle = context.getString(R.string.after_calibration_rewrite_history))
        SwitchPref(
            state,
            "widget_range_lines",
            context.getString(R.string.widget_range_lines),
            default = false,
            subtitle = context.getString(R.string.show_range_on_widget),
            tag = "setting_widget_range_lines",
            onCheckedChange = { checked ->
                state.setBool("widget_range_lines", checked)
                restartWidgetIfPresent(context)
            },
        )
        SwitchPref(state, "show_graph_grid_time", context.getString(R.string.show_graph_time_lines), default = true, subtitle = context.getString(R.string.grid_time_lines_visible))
        SwitchPref(state, "show_graph_grid_glucose", context.getString(R.string.show_graph_glucose_lines), default = true, subtitle = context.getString(R.string.grid_glucose_lines_visible))
        SwitchPref(state, "show_filtered_curve", context.getString(R.string.display_filtered_plot), default = true, subtitle = context.getString(R.string.useful_for_noise_and_missed_readings), tag = "setting_show_filtered_curve")
        SwitchPref(state, "show_pseudo_filtered", context.getString(R.string.title_create_missing_filtered), default = true, subtitle = context.getString(R.string.summary_create_missing_filtered), enabled = filtered, tag = "setting_show_pseudo_filtered")
        SwitchPref(state, "show_raw_plot", context.getString(R.string.display_raw_plot_data), default = true, subtitle = context.getString(R.string.the_standard_xdrip_calculated_value))
        SwitchPref(state, "show_basal_line", context.getString(R.string.title_show_basal_line), default = true, subtitle = context.getString(R.string.summary_show_basal_line))
        SwitchPref(state, "show_target_line", context.getString(R.string.show_graph_target_line), default = false, subtitle = context.getString(R.string.display_ideal_glucose_target_line))
        SwitchPref(state, "show_recent_average_line", context.getString(R.string.show_graph_recent_average), default = true, subtitle = context.getString(R.string.display_eight_hour_average_line))
        SwitchPref(state, "show_full_average_line", context.getString(R.string.show_graph_total_average), default = false, subtitle = context.getString(R.string.display_twenty_four_hour_average_line))
        SwitchPref(state, "show_libre_trend_line", context.getString(R.string.show_libre_trend), default = false)
        SwitchPref(state, "show_g_prediction", context.getString(R.string.title_show_g_prediction), default = false, subtitle = context.getString(R.string.summary_show_g_prediction))
        SwitchPref(state, "show_smb_icons", context.getString(R.string.title_show_smb_icons), default = true, subtitle = context.getString(R.string.summary_show_smb_icons))
        SwitchPref(state, "show_medtrum_secondary", context.getString(R.string.title_show_medtrum_secondary), default = true, subtitle = context.getString(R.string.summary_show_medtrum_secondary))
        SwitchPref(state, "show_momentum_working_line", context.getString(R.string.show_momentum_working_curve), default = false, subtitle = context.getString(R.string.predictive_model_inner_workings))
        SwitchPref(state, "show_noise_workings", context.getString(R.string.show_noise_workings), default = false, subtitle = context.getString(R.string.noise_model_inner_workings))
        SwitchPref(state, "illustrate_backfilled_data", context.getString(R.string.title_illustrate_backfilled_data), default = false, subtitle = context.getString(R.string.summary_illustrate_backfilled_data))
        SwitchPref(state, "widget_hide_graph", context.getString(R.string.title_widget_hide_graph), default = false, subtitle = context.getString(R.string.summary_widget_hide_graph))
        if (engineering) {
            SwitchPref(state, "illustrate_remote_data", context.getString(R.string.title_illustrate_remote_data), default = false, subtitle = context.getString(R.string.summary_illustrate_remote_data), enabled = engineering, tag = "setting_illustrate_remote_data")
        }
        SettingsActionRow(
            title = context.getString(R.string.title_yRange),
            onClick = { onNavigate(SettingsScreen.XdripPlusYAxis) },
            modifier = Modifier.testTag("setting_y_axis"),
        )
    }
}

@Composable
internal fun GraphSmoothingScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.graph_smoothing)) {
        SwitchPref(state, "graph_smoothing", context.getString(R.string.enable_graph_smoothing), default = false, subtitle = context.getString(R.string.simplify_graphs_by_smoothing_out_irregularities), tag = "setting_graph_smoothing")
        SwitchPref(state, "show-unsmoothed-values-as-plugin", context.getString(R.string.show_unsmoothed), default = false, subtitle = context.getString(R.string.show_unsmoothed_summary), enabled = state.dependentEnabled("graph_smoothing", false))
    }
}

@Composable
internal fun YAxisScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val custom = state.bool("Customize_yRange", false)
    SettingsCategory(context.getString(R.string.titleInside_yRange)) {
        SwitchPref(state, "Customize_yRange", context.getString(R.string.titleInside_yRange), default = false, subtitle = context.getString(R.string.summaryInside_yRange), tag = "setting_customize_yrange")
        ListPref(
            state,
            "default_ymax",
            context.getString(R.string.title_ymax),
            context.resources.getStringArray(R.array.ymax_entries).toList(),
            context.resources.getStringArray(R.array.ymax_values).toList(),
            "250",
            enabled = custom,
            tag = "setting_default_ymax",
        )
        ListPref(
            state,
            "default_ymin",
            context.getString(R.string.title_ymin),
            context.resources.getStringArray(R.array.ymin_entries).toList(),
            context.resources.getStringArray(R.array.ymin_values).toList(),
            "40",
            enabled = custom,
            tag = "setting_default_ymin",
        )
    }
    SettingsCategory(context.getString(R.string.title_auto_y_pan)) {
        SwitchPref(state, "auto_y_pan", context.getString(R.string.title_auto_y_pan), default = true, subtitle = context.getString(R.string.summary_auto_y_pan))
    }
}

@Composable
internal fun AccessibilityScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.title_xdrip_plus_accessibility)) {
        SettingsActionRow(
            title = context.getString(R.string.title_show_accessibility_settings),
            subtitle = context.getString(R.string.summary_show_accessibility_settings),
            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            modifier = Modifier.testTag("setting_show_accessibility"),
        )
        SwitchPref(state, "aod_use_top", context.getString(R.string.title_aod_use_top), default = true, subtitle = context.getString(R.string.summary_aod_use_top))
        SwitchPref(state, "aod_use_top_center", context.getString(R.string.title_aod_use_top_center), default = true, subtitle = context.getString(R.string.summary_aod_use_top_center))
        SwitchPref(state, "aod_use_center", context.getString(R.string.title_aod_use_center), default = true, subtitle = context.getString(R.string.summary_aod_use_center))
        SwitchPref(state, "aod_use_center_bottom", context.getString(R.string.title_aod_use_center_bottom), default = true, subtitle = context.getString(R.string.summary_aod_use_center_bottom))
        SwitchPref(state, "aod_use_bottom", context.getString(R.string.title_aod_use_bottom), default = true, subtitle = context.getString(R.string.summary_aod_use_bottom))
    }
}

@Composable
internal fun NumberWallScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val scheme = MaterialTheme.colorScheme
    val revision by ColorCacheBridge.revision.collectAsState()
    @Suppress("UNUSED_EXPRESSION")
    revision
    // The start/stop times are written by TimePickerPrefActivity while this screen is paused; watch
    // the keys so the summaries recompose when we return.
    val timeRevision = rememberPrefRevision(setOf("number_wall_start_time", "number_wall_stop_time"))
    val forceColor = state.bool("force_lock_screen_text_color", true)
    val timeRange = remember(timeRevision) { TimeRangeUtils.getNiceStartStopString("number_wall") }

    SettingsCategory(context.getString(R.string.title_xdrip_plus_number_wall)) {
        SettingsActionRow(
            title = context.getString(R.string.title_do_number_wall_configuration),
            subtitle = context.getString(R.string.summary_do_number_wall_configuration),
            onClick = { context.startActivity(Intent(context, NumberWallPreview::class.java)) },
            modifier = Modifier.testTag("setting_number_wall_config"),
        )
        SwitchPref(state, "number_wall_on_lockscreen", context.getString(R.string.enable), default = false, subtitle = context.getString(R.string.summary_number_wall_on_lockscreen), tag = "setting_number_wall_lockscreen")
        SwitchPref(state, "force_lock_screen_text_color", context.getString(R.string.title_force_lockscreen_text_color), default = true, subtitle = context.getString(R.string.summary_force_lockscreen_text_color), tag = "setting_force_lock_text_color")
        SettingsColorRow(
            title = context.getString(R.string.title_color_number_wall),
            color = ThemeColor.NUMBER_WALL.currentArgb(scheme),
            enabled = forceColor,
            onColorChanged = { ThemeColorStore.setOverride(ThemeColor.NUMBER_WALL, it) },
            onReset = if (ThemeColorStore.isOverridden(ThemeColor.NUMBER_WALL)) ({ ThemeColorStore.clearOverride(ThemeColor.NUMBER_WALL) }) else null,
            modifier = Modifier.testTag("setting_color_number_wall"),
        )
        SettingsColorRow(
            title = context.getString(R.string.title_color_number_wall_shadow),
            color = ThemeColor.NUMBER_WALL_SHADOW.currentArgb(scheme),
            onColorChanged = { ThemeColorStore.setOverride(ThemeColor.NUMBER_WALL_SHADOW, it) },
            onReset = if (ThemeColorStore.isOverridden(ThemeColor.NUMBER_WALL_SHADOW)) ({ ThemeColorStore.clearOverride(ThemeColor.NUMBER_WALL_SHADOW) }) else null,
            modifier = Modifier.testTag("setting_color_number_wall_shadow"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_pick_numberwall_start),
            subtitle = timeRange.ifEmpty { context.getString(R.string.summary_pick_numberwall_start) },
            onClick = { startTimePicker(context, "number_wall_start_time") },
            modifier = Modifier.testTag("setting_pick_numberwall_start"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_pick_numberwall_stop),
            subtitle = timeRange.ifEmpty { context.getString(R.string.summary_pick_numberwall_stop) },
            onClick = { startTimePicker(context, "number_wall_stop_time") },
            modifier = Modifier.testTag("setting_pick_numberwall_stop"),
        )
    }
}

@Composable
internal fun NumberIconScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val tested = state.bool("number_icon_tested", false)
    SettingsCategory(context.getString(R.string.title_xdrip_plus_number_icon)) {
        SettingsActionRow(
            title = context.getString(R.string.title_do_number_icon_test),
            subtitle = context.getString(R.string.summary_do_number_icon_test),
            onClick = {
                context.startActivity(Intent(context, Home::class.java).putExtra("numberIconTest", "numberIconTest"))
            },
            modifier = Modifier.testTag("setting_do_number_icon_test"),
        )
        SwitchPref(state, "number_icon_tested", context.getString(R.string.title_number_icon_tested), default = false, subtitle = context.getString(R.string.summary_number_icon_tested), tag = "setting_number_icon_tested")
        SwitchPref(state, "use_number_icon", context.getString(R.string.title_use_number_icon), default = false, subtitle = context.getString(R.string.summary_use_number_icon), enabled = tested)
    }
}

/* -------------------------------------------------------------------------------- helpers */

private fun startTimePicker(context: Context, prefName: String) {
    context.startActivity(
        Intent(context, TimePickerPrefActivity::class.java).putExtra("pref-name", prefName)
    )
}

private fun restartWidgetIfPresent(context: Context) {
    if (AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, xDripWidget::class.java)).isNotEmpty()) {
        context.startService(Intent(context, WidgetUpdateService::class.java))
    }
}

/** Local recompose trigger for preference keys written outside the Compose host. */
@Composable
private fun rememberPrefRevision(keys: Set<String>): Int {
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(keys) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null && key in keys) revision++
        }
        val prefs = Pref.getInstance()
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return revision
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun CopyingSettingsScreenPreview() {
    XdripPreview { CopyingSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun UpdateSettingsScreenPreview() {
    XdripPreview { UpdateSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MotionSettingsScreenPreview() {
    XdripPreview { MotionSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PensScreenPreview() {
    XdripPreview { PensScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun NovopenScreenPreview() {
    XdripPreview { NovopenScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun InpenScreenPreview() {
    XdripPreview { InpenScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PendiqScreenPreview() {
    XdripPreview { PendiqScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PredictionSettingsScreenPreview() {
    XdripPreview { PredictionSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MultipleInsulinScreenPreview() {
    XdripPreview { MultipleInsulinScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AdvPredictScreenPreview() {
    XdripPreview { AdvPredictScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SyncSettingsScreenPreview() {
    XdripPreview { SyncSettingsScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun RemoteSnoozeScreenPreview() {
    XdripPreview { RemoteSnoozeScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun DesertSyncScreenPreview() {
    XdripPreview { DesertSyncScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun XdripPlusDisplayScreenPreview() {
    XdripPreview { XdripPlusDisplayScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun FontSettingsScreenPreview() {
    XdripPreview { FontSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun LanguageSettingsScreenPreview() {
    XdripPreview { LanguageSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun GraphDisplayScreenPreview() {
    XdripPreview { GraphDisplayScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun GraphSmoothingScreenPreview() {
    XdripPreview { GraphSmoothingScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun YAxisScreenPreview() {
    XdripPreview { YAxisScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AccessibilityScreenPreview() {
    XdripPreview { AccessibilityScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun NumberWallScreenPreview() {
    XdripPreview { NumberWallScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun NumberIconScreenPreview() {
    XdripPreview { NumberIconScreen() }
}

// endregion
