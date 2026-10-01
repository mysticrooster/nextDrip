package com.eveningoutpost.dexdrip.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Constants
import com.eveningoutpost.dexdrip.utilitymodels.pebble.PebbleActions
import com.eveningoutpost.dexdrip.utils.LocationHelper
import com.eveningoutpost.dexdrip.utils.framework.IncomingCallsReceiver
import com.eveningoutpost.dexdrip.watch.miband.MiBand
import com.eveningoutpost.dexdrip.watch.miband.MiBandEntry
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayAdapter
import com.eveningoutpost.dexdrip.wearintegration.Amazfitservice
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService

/**
 * S5a — Smart watch features (`pref_advanced_settings.xml` → `smart_watch_options`).
 *
 * Android Wear, Pebble, Amazfit, BlueJay, LeFun, MiBand and Smartwatch sensors. Preference
 * keys/types are unchanged; gating mirrors the legacy `removePreference` logic and side effects are
 * attached to the row callbacks (see `Documentation/technical/Settings_S5a_Advanced.md`).
 */

private fun Context.activityOrNull(): Activity? = this as? Activity

/**
 * Mirrors the legacy `AllPrefsFragment.checkReadPermission` (READ_EXTERNAL_STORAGE request on
 * MiBand enable). Kept local so the Compose host does not depend on the legacy settings class.
 */
private fun requestReadPermission(activity: Activity) {
    if (activity.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) !=
        android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
            Constants.GET_PHONE_READ_PERMISSION,
        )
    }
}

@Composable
internal fun SmartWatchOptionsScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    SettingsCategory(context.getString(R.string.smart_watch_features)) {
        SettingsActionRow(
            title = context.getString(R.string.android_wear_integration),
            icon = Icons.Outlined.Watch,
            onClick = { onNavigate(SettingsScreen.WearSettings) },
            modifier = Modifier.testTag("setting_watch_wear"),
        )
        SettingsActionRow(
            title = context.getString(R.string.pebble_integration),
            icon = Icons.Outlined.Watch,
            onClick = { onNavigate(SettingsScreen.PebbleSettings) },
            modifier = Modifier.testTag("setting_watch_pebble"),
        )
        SettingsActionRow(
            title = context.getString(R.string.amazfit_sync_service),
            icon = Icons.Outlined.Watch,
            onClick = { onNavigate(SettingsScreen.AmazfitSettings) },
            modifier = Modifier.testTag("setting_watch_amazfit"),
        )
        SettingsActionRow(
            title = "BlueJay Watch",
            icon = Icons.Outlined.Watch,
            onClick = { onNavigate(SettingsScreen.BlueJaySettings) },
            modifier = Modifier.testTag("setting_watch_bluejay"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_lefun_band),
            icon = Icons.Outlined.Watch,
            onClick = { onNavigate(SettingsScreen.LeFunSettings) },
            modifier = Modifier.testTag("setting_watch_lefun"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_miband),
            icon = Icons.Outlined.Watch,
            onClick = { onNavigate(SettingsScreen.MiBandSettings) },
            modifier = Modifier.testTag("setting_watch_miband"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_Smartwatch_Sensors),
            icon = Icons.Outlined.MonitorHeart,
            onClick = { onNavigate(SettingsScreen.SmartwatchSensors) },
            modifier = Modifier.testTag("setting_watch_sensors"),
        )
    }
}

/* ---------------------------------------------------------------- Smartwatch sensors */

@Composable
internal fun SmartwatchSensorsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val wearHeartRate = state.bool("use_wear_heartrate", true)
    SettingsCategory(context.getString(R.string.title_Smartwatch_Sensors)) {
        SwitchPref(
            state,
            "use_pebble_health",
            context.getString(R.string.title_Use_Health_Data),
            default = true,
            subtitle = context.getString(R.string.summary_Collect_and_display_Step_counter_and_Heart_rate_when_available),
            onCheckedChange = {
                state.setBool("use_pebble_health", it)
                PebbleActions.restartPebble(context)
            },
        )
        SwitchPref(
            state,
            "use_wear_heartrate",
            context.getString(R.string.title_Heart_Rate_Sensor),
            default = true,
            subtitle = context.getString(R.string.summary_Activate_heart_rate_sensor_if_available),
            onCheckedChange = {
                state.setBool("use_wear_heartrate", it)
                WatchUpdaterService.startSelf()
            },
        )
        SwitchPref(
            state,
            "smooth_heartrate",
            context.getString(R.string.summary_Smooth_heart_rate_graph),
            default = true,
            enabled = wearHeartRate,
        )
    }
}

/* ------------------------------------------------------------------------------ Wear */

@Composable
internal fun WearSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val wearSync = state.bool("wear_sync", false)
    val enableWearG5 = state.dependentEnabled("wear_sync", false)
    val forceWearG5 = state.dependentEnabled("enable_wearG5", false)
    val missedReadings = state.bool("disable_wearG5_on_missedreadings", false)
    SettingsCategory(context.getString(R.string.wear_integration)) {
        SwitchPref(
            state,
            "wear_sync",
            context.getString(R.string.android_wear_integration),
            default = false,
            subtitle = context.getString(R.string.send_data_to_android_wear_watch),
            tag = "setting_wear_sync",
            onCheckedChange = {
                state.setBool("wear_sync", it)
                WatchUpdaterService.startSelf()
            },
        )
        SwitchPref(state, "enable_wearG5", context.getString(R.string.pref_enable_wearG5), default = false, subtitle = context.getString(R.string.pref_summary_enable_wearG5), enabled = enableWearG5)
        SwitchPref(state, "force_wearG5", context.getString(R.string.pref_force_wearG5), default = false, subtitle = context.getString(R.string.pref_summary_force_wearG5), enabled = forceWearG5)
        SwitchPref(state, "disable_wearG5_on_lowbattery", context.getString(R.string.pref_disable_wearG5_on_lowbattery), default = false, subtitle = context.getString(R.string.pref_summary_disable_wearG5_on_lowbattery), enabled = forceWearG5)
        SwitchPref(state, "disable_wearG5_on_missedreadings", context.getString(R.string.pref_disable_wearG5_on_missedreadings), default = false, subtitle = context.getString(R.string.pref_summary_disable_wearG5_on_missedreadings), enabled = forceWearG5)
        EditPref(
            state,
            "disable_wearG5_on_missedreadings_level",
            context.getString(R.string.pref_disable_wearG5_on_missedreadings_level),
            default = "30",
            numeric = true,
            enabled = missedReadings,
        )
        SwitchPref(state, "only_ever_use_wear_collector", context.getString(R.string.title_Only_use_Wear), default = false, subtitle = context.getString(R.string.summary_If_you_plan_to_only_use_a_Wear_collector_and_never_your_phone), enabled = forceWearG5)
        SettingsInfoRow(
            title = context.getString(R.string.pref_node_wearG5),
            value = state.string("node_wearG5", ""),
            subtitle = context.getString(R.string.pref_summary_node_wearG5),
            enabled = forceWearG5,
            modifier = Modifier.testTag("setting_wear_node"),
        )
        SwitchPref(state, "sync_wear_logs", context.getString(R.string.pref_sync_wear_logs), default = false, subtitle = context.getString(R.string.pref_summary_sync_wear_logs), enabled = wearSync, tag = "setting_wear_sync_logs")
        EditPref(
            state,
            "wear_logs_prefix",
            context.getString(R.string.pref_wear_logs_prefix),
            default = "wear",
            subtitle = context.getString(R.string.pref_summary_wear_logs_prefix),
            enabled = state.dependentEnabled("sync_wear_logs", false),
            tag = "setting_wear_logs_prefix",
        )
        SwitchPref(state, "show_wear_treatments", context.getString(R.string.pref_show_treatments), default = false, subtitle = context.getString(R.string.pref_summary_show_treatments), enabled = wearSync)
        SwitchPref(state, "enable_wear_auto_update", context.getString(R.string.title_enable_wear_auto_update), default = true, subtitle = context.getString(R.string.summary_enable_wear_auto_update), enabled = wearSync)
    }
}

/* --------------------------------------------------------------------------- Amazfit */

@Composable
internal fun AmazfitSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("pref_amazfit_enable_key", false)
    SettingsCategory(context.getString(R.string.amazfit_sync_service)) {
        SwitchPref(
            state,
            "pref_amazfit_enable_key",
            context.getString(R.string.pref_amazfit_tile),
            default = false,
            subtitle = context.getString(R.string.pref_amazfit_enable_summary),
            tag = "setting_amazfit_enable",
            onCheckedChange = { checked ->
                state.setBool("pref_amazfit_enable_key", checked)
                val intent = Intent(context, Amazfitservice::class.java)
                if (checked) context.startService(intent) else context.stopService(intent)
            },
        )
        SwitchPref(state, "pref_amazfit_BG_alert_enable_key", context.getString(R.string.pref_amazfit_BG_alert_title), default = false, subtitle = context.getString(R.string.pref_amazfit_BG_alert_enable_summary), enabled = enabled)
        SwitchPref(state, "pref_amazfit_other_alert_enable_key", context.getString(R.string.pref_amazfit_other_alert_title), default = false, subtitle = context.getString(R.string.pref_amazfit_other_alert_enable_summary), enabled = enabled)
    }
    SettingsCategory(context.getString(R.string.pref_amazfit_watchface_settings)) {
        val graph = state.bool("pref_amazfit_watchface_graph", false)
        SwitchPref(state, "pref_amazfit_watchface_graph", context.getString(R.string.pref_amazfit_watchface_graph_title), default = false, subtitle = context.getString(R.string.pref_amazfit_watchface_graph_enable_summary), enabled = enabled)
        SwitchPref(state, "pref_amazfit_watchface_graph_dots", context.getString(R.string.pref_amazfit_watchface_graph_dots_title), default = false, subtitle = context.getString(R.string.pref_amazfit_watchface_graph_dots_summary), enabled = graph)
        EditPref(
            state,
            "amazfit_watchface_graph_hours",
            context.getString(R.string.pref_amazfit_watchface_graph_hours_title),
            default = "4",
            subtitle = context.getString(R.string.pref_amazfit_widget_graph_hours_summary),
            numeric = true,
            enabled = graph,
        )
    }
    SettingsCategory(context.getString(R.string.pref_amazfit_widget_settings)) {
        val graph = state.bool("pref_amazfit_widget_graph", false)
        SwitchPref(state, "pref_amazfit_widget_graph", context.getString(R.string.pref_amazfit_widget_graph_title), default = false, subtitle = context.getString(R.string.pref_amazfit_widget_graph_enable_summary), enabled = enabled)
        SwitchPref(state, "pref_amazfit_widget_graph_dots", context.getString(R.string.pref_amazfit_widget_graph_dots_title), default = false, subtitle = context.getString(R.string.pref_amazfit_widget_graph_dots_summary), enabled = graph)
        EditPref(
            state,
            "amazfit_widget_graph_hours",
            context.getString(R.string.pref_amazfit_watchface_graph_hours_title),
            default = "4",
            subtitle = context.getString(R.string.pref_amazfit_widget_graph_hours_summary),
            numeric = true,
            enabled = graph,
        )
    }
}

/* ---------------------------------------------------------------------------- LeFun */

@Composable
internal fun LeFunSettingsScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("lefun_enabled", false)
    SettingsCategory(context.getString(R.string.title_lefun_band)) {
        SwitchPref(state, "lefun_enabled", context.getString(R.string.title_lefun_enable), default = false, subtitle = context.getString(R.string.summary_lefun_enable), tag = "setting_lefun_enabled")
        EditPref(state, "lefun_mac", context.getString(R.string.title_lefun_mac), default = "")
        SwitchPref(state, "lefun_send_readings", context.getString(R.string.title_lefun_send_readings), default = true, subtitle = context.getString(R.string.summary_lefun_send_readings), enabled = enabled)
        SwitchPref(state, "lefun_send_alarms", context.getString(R.string.title_lefun_send_alarms), default = true, subtitle = context.getString(R.string.summary_lefun_send_alarms), enabled = enabled)
        SwitchPref(state, "lefun_option_shake_snoozes", context.getString(R.string.title_lefun_option_shake_snoozes), default = true, enabled = state.dependentEnabled("lefun_send_alarms", true))
        SwitchPref(
            state,
            "lefun_option_call_notifications",
            context.getString(R.string.title_lefun_send_calls),
            default = false,
            subtitle = context.getString(R.string.summary_lefun_send_calls),
            enabled = enabled,
            onCheckedChange = {
                state.setBool("lefun_option_call_notifications", it)
                context.activityOrNull()?.let { IncomingCallsReceiver.checkPermission(it) }
            },
        )
        SettingsActionRow(
            title = context.getString(R.string.title_lefun_screens_features),
            onClick = { onNavigate(SettingsScreen.LeFunFeatures) },
            modifier = Modifier.testTag("setting_lefun_features"),
        )
    }
}

@Composable
internal fun LeFunFeaturesScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.title_features)) {
        SwitchPref(state, "lefun_feature_lift_to_wake", context.getString(R.string.title_lefun_feature_lift_to_wak), default = true)
        SwitchPref(state, "lefun_feature_anti_lost", context.getString(R.string.title_lefun_feature_anti_lost), default = false)
        SwitchPref(state, "lefun_locale_12_hour", context.getString(R.string.title_lefun_locale_12_hour), default = true)
    }
    SettingsCategory(context.getString(R.string.title_screens)) {
        SwitchPref(state, "lefun_screen_step_counter", context.getString(R.string.title_lefun_screen_step_counter), default = false)
        SwitchPref(state, "lefun_screen_step_distance", context.getString(R.string.title_lefun_screen_step_distance), default = false)
        SwitchPref(state, "lefun_screen_step_calories", context.getString(R.string.title_lefun_screen_step_calories), default = false)
        SwitchPref(state, "lefun_screen_heart_rate", context.getString(R.string.title_lefun_screen_heart_rate), default = false)
        SwitchPref(state, "lefun_screen_heart_pressure", context.getString(R.string.title_lefun_screen_heart_pressure), default = false)
        SwitchPref(state, "lefun_screen_find_phone", context.getString(R.string.title_lefun_screen_find_phone), default = false)
        SwitchPref(state, "lefun_screen_mac_address", context.getString(R.string.title_lefun_screen_mac_address), default = false)
    }
}

/* --------------------------------------------------------------------------- BlueJay */

@Composable
internal fun BlueJaySettingsScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("bluejay_enabled", false)
    SettingsCategory("BlueJay Watch") {
        SwitchPref(state, "bluejay_enabled", "BlueJay Watch", default = false, tag = "setting_bluejay_enabled")
        SwitchPref(state, "bluejay_collector_enabled", "Run Collector", default = true, enabled = enabled)
        SwitchPref(state, "bluejay_local_alarms", "Local Alarms", default = true, subtitle = "Watch can alarm for high/low when not connected to phone.", enabled = enabled)
        SwitchPref(state, "bluejay_send_alarms", "Send Alarms", default = true, subtitle = "Send xDrip generated alarms to the watch", enabled = enabled)
        SwitchPref(
            state,
            "bluejay_option_call_notifications",
            "Send Calls",
            default = false,
            subtitle = "Send call notifications to the band (Android 6+)",
            enabled = enabled,
            onCheckedChange = {
                state.setBool("bluejay_option_call_notifications", it)
                context.activityOrNull()?.let { activity -> IncomingCallsReceiver.checkPermission(activity) }
            },
        )
        SwitchPref(state, "bluejay_send_readings", "Send Readings", default = true, subtitle = "Send glucose values received on the Phone to the Watch", enabled = enabled)
        SwitchPref(state, "bluejay_option_24_hour_clock", "24 Hour Clock", default = false, subtitle = "Show 24 hour clock on display", enabled = enabled)
        val timeout = state.int("bluejay_screen_timeout", 6)
        SettingsSliderRow(
            title = "Screen Timeout (${BlueJayAdapter.screenTimeoutValueToSeconds(timeout)} s)",
            value = timeout,
            onValueChange = { state.setInt("bluejay_screen_timeout", it) },
            valueRange = 0..7,
            subtitle = "",
            enabled = enabled,
        )
        SwitchPref(state, "bluejay_use_motion_wake", "Wake with motion", default = true, subtitle = "Use a shake of the wrist to wake BlueJay. Adjust sensitivity below.", enabled = enabled)
        SettingsSliderRow(
            title = "Wake up velocity",
            value = state.int("bluejay_wake_velocity", 6),
            onValueChange = { state.setInt("bluejay_wake_velocity", it) },
            valueRange = 0..15,
            subtitle = "",
            enabled = enabled,
        )
        SettingsActionRow(
            title = "Launch BlueJay Panel",
            onClick = { context.startActivity(Intent(context, com.eveningoutpost.dexdrip.ui.activities.ThinJamActivity::class.java)) },
        )
        SettingsActionRow(
            title = "BlueJay Advanced Settings",
            onClick = { onNavigate(SettingsScreen.BlueJayAdvanced) },
            modifier = Modifier.testTag("setting_bluejay_advanced"),
        )
    }
}

@Composable
internal fun BlueJayAdvancedScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("bluejay_enabled", false)
    SettingsCategory("BlueJay Advanced Settings") {
        EditPref(state, "bluejay_mac", "BlueJay Mac", default = "", tag = "setting_bluejay_mac")
        SwitchPref(
            state,
            "bluejay_run_phone_collector",
            "Run Phone Collector",
            default = true,
            subtitle = "Also run the standard collector on this phone. Only turn this off if you don't want this phone itself to be connecting to the transmitter.",
            enabled = enabled,
            tag = "setting_bluejay_run_phone_collector",
            onBeforeChange = { BlueJayAdapter.canRunPhoneCollector(it) },
        )
        SwitchPref(
            state,
            "bluejay_run_as_phone_collector",
            "BlueJay uses Phone Slot",
            default = false,
            subtitle = "This allows the BlueJay to occupy the phone slot on the transmitter.",
            enabled = enabled,
            onBeforeChange = { BlueJayAdapter.canUsePhoneSlot(it) },
        )
        SwitchPref(state, "bluejay_use_broadcast_api", "Enable remote API", default = false, subtitle = "Accept commands from other apps", enabled = enabled)
        SwitchPref(state, "bluejay_send_status_line", "Send Status Line", default = false, subtitle = "Show xDrip extra status line on X2 display", enabled = enabled)
        SwitchPref(state, "bluejay_beep_on_connect", "Debug sound", default = false, subtitle = "Play a sound when the bluejay connects", enabled = enabled)
        SwitchPref(state, "bluejay_delta_trend", "Attempt Delta Trend", default = false, subtitle = "Try to calculate xDrip style delta trend arrow instead of using transmitter estimate arrow.", enabled = false)
        SettingsSliderRow(
            title = "Backfill Hours",
            value = state.int("bluejay_backfill_hours", 3),
            onValueChange = { state.setInt("bluejay_backfill_hours", it) },
            valueRange = 0..12,
            subtitle = "Larger backfill scope can reduce battery life",
            enabled = enabled,
        )
        SwitchPref(state, "bluejay_timing_failsafe", "Timing fail-safe", default = false, subtitle = "Scan for longer periods of time if readings are missed. Uses more battery but can recover if watch gets out of sync", enabled = false)
        SwitchPref(state, "bluejay_button1_vibrate", "Vibrate button", default = true, subtitle = "Vibrate watch when a button long press is sent to the phone")
        SwitchPref(state, "bluejay_send_to_another_xdrip", "Send to another xDrip", default = false, subtitle = "If you have multiple xDrip instances on your phone you can send data to another one that has a BlueJay here")
    }
}

/* ---------------------------------------------------------------------------- MiBand */

@Composable
internal fun MiBandSettingsScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("miband_enabled", false)
    val mac = state.string("miband_data_mac", "")
    val type = remember(mac) { SettingsVisibility.mibandType() }
    var showUpdateBg by remember { mutableStateOf(false) }
    SettingsCategory(context.getString(R.string.title_miband)) {
        SwitchPref(
            state,
            "miband_enabled",
            context.getString(R.string.title_miband_enable),
            default = false,
            subtitle = context.getString(R.string.summary_miband_enable),
            tag = "setting_miband_enabled",
            onCheckedChange = { checked ->
                state.setBool("miband_enabled", checked)
                if (checked) {
                    context.activityOrNull()?.let { activity ->
                        LocationHelper.requestLocationForBluetooth(activity)
                        requestReadPermission(activity)
                    }
                }
            },
        )
        EditPref(state, "miband_data_mac", context.getString(R.string.title_lefun_mac), default = "", subtitle = context.getString(R.string.summary_miband_mac))
        if (type == MiBand.MiBandType.MI_BAND4) {
            EditPref(
                state,
                "miband_data_authkey",
                context.getString(R.string.title_miband_authkey),
                default = "",
                subtitle = context.getString(R.string.summary_miband_authkey),
                maxLength = 32,
                tag = "setting_miband_authkey",
            )
        }
        SwitchPref(state, "miband_send_readings", context.getString(R.string.title_lefun_send_readings), default = true, subtitle = context.getString(R.string.summary_lefun_send_readings), enabled = enabled)
        if (type == MiBand.MiBandType.MI_BAND4) {
            SwitchPref(state, "miband_send_readings_as_notification", context.getString(R.string.title_miband_send_readings_as_notification), default = true, subtitle = context.getString(R.string.summary_miband_send_readings_as_notification), enabled = state.dependentEnabled("miband_send_readings", true))
        }
        SwitchPref(state, "miband_vibrate_on_readings", context.getString(R.string.title_miband_vibrate_on_readings), default = true, enabled = state.dependentEnabled("miband_send_readings", true))
        SwitchPref(state, "miband_send_alarms", context.getString(R.string.title_lefun_send_alarms), default = true, subtitle = context.getString(R.string.summary_lefun_send_alarms), enabled = enabled)
        SwitchPref(
            state,
            "miband_option_call_notifications",
            context.getString(R.string.title_miband_send_calls),
            default = false,
            subtitle = context.getString(R.string.summary_miband_send_calls),
            enabled = enabled,
            onCheckedChange = {
                state.setBool("miband_option_call_notifications", it)
                context.activityOrNull()?.let { activity -> IncomingCallsReceiver.checkPermission(activity) }
            },
        )
        if (type == MiBand.MiBandType.MI_BAND4) {
            SettingsCategory(context.getString(R.string.title_miband_graph_category)) {
                val graph = state.bool("miband_graph_enable", true)
                SwitchPref(state, "miband_graph_enable", context.getString(R.string.title_miband_graph_enable), default = true, enabled = enabled)
                EditPref(
                    state,
                    "miband_graph_hours",
                    context.getString(R.string.pref_amazfit_watchface_graph_hours_title),
                    default = "4",
                    subtitle = context.getString(R.string.pref_amazfit_widget_graph_hours_summary),
                    numeric = true,
                    enabled = enabled && graph,
                )
                SwitchPref(state, "miband_graph_treatment_enable", context.getString(R.string.title_miband_graph_treatment_enable), default = true, enabled = enabled && graph)
            }
        }
        SettingsActionRow(
            title = context.getString(R.string.title_miband_update_bg),
            onClick = { showUpdateBg = true },
            enabled = enabled,
        )
        SettingsCategory(context.getString(R.string.title_miband_general_settings_category)) {
            SettingsActionRow(
                title = context.getString(R.string.title_miband_screens_features),
                onClick = { onNavigate(SettingsScreen.MiBandSubSettings) },
                enabled = enabled,
                modifier = Modifier.testTag("setting_miband_subscreen"),
            )
        }
    }
    if (showUpdateBg) {
        AlertDialog(
            onDismissRequest = { showUpdateBg = false },
            title = { Text(context.getString(R.string.miband_bg_dialog_title)) },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateBg = false
                    MiBandEntry.forceShowLatestBG()
                }) { Text(context.getString(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateBg = false }) { Text(context.getString(R.string.no)) }
            },
        )
    }
}

@Composable
internal fun MiBandSubSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val mac = state.string("miband_data_mac", "")
    val type = remember(mac) { SettingsVisibility.mibandType() }
    val engineering = SettingsVisibility.isEngineeringMode(state)
    val mibandEnabled = state.bool("miband_enabled", false)

    SettingsCategory(context.getString(R.string.title_features)) {
        SwitchPref(state, "miband_feature_lift_to_wake", context.getString(R.string.title_lefun_feature_lift_to_wak), default = true)
        SwitchPref(state, "miband_feature_switch_display_on_wrist", context.getString(R.string.title_miband_switch_display_on_wrist), default = true)
        SwitchPref(state, "miband_feature_units", context.getString(R.string.title_miband_units), default = true, subtitle = context.getString(R.string.summary_miband_units))
        SwitchPref(state, "miband_feature_goal_notification", context.getString(R.string.title_miband_goal_notification), default = true, subtitle = context.getString(R.string.summary_miband_goal_notification))
        SwitchPref(state, "miband_feature_anti_lost", context.getString(R.string.title_lefun_feature_anti_lost), default = false)
        SwitchPref(state, "miband_feature_locale_24_hour", context.getString(R.string.title_miband_locale_24_hour), default = true, subtitle = context.getString(R.string.summary_miband_locale_24_hour))
        SwitchPref(state, "miband_feature_show_date", context.getString(R.string.title_miband_show_date), default = true)
        SwitchPref(state, "miband_feature_visibility", context.getString(R.string.title_miband_visibility), default = true, subtitle = context.getString(R.string.summary_miband_visibility))
    }

    if (type != MiBand.MiBandType.MI_BAND2 && type != MiBand.MiBandType.UNKNOWN) {
        SettingsCategory(context.getString(R.string.title_miband_nightmode_category)) {
            val nightmode = state.bool("miband_nightmode_enabled", false)
            SwitchPref(state, "miband_nightmode_enabled", context.getString(R.string.title_miband_nightmode_enabled), default = false, subtitle = context.getString(R.string.summary_miband_nightmode_enabled))
            SettingsTimeRow(
                title = context.getString(R.string.title_miband_nightmode_start),
                valueMillis = state.long("miband_nightmode_start", 0L),
                onTimeChanged = { state.setLong("miband_nightmode_start", it) },
                enabled = nightmode,
            )
            SettingsTimeRow(
                title = context.getString(R.string.title_miband_nightmode_end),
                valueMillis = state.long("miband_nightmode_end", 0L),
                onTimeChanged = { state.setLong("miband_nightmode_end", it) },
                enabled = nightmode,
            )
            val interval = state.int("miband_nightmode_interval", 6)
            SettingsSliderRow(
                title = nightmodeTitle(context, interval),
                value = interval,
                onValueChange = { state.setInt("miband_nightmode_interval", it) },
                valueRange = 0..23,
                subtitle = context.getString(R.string.summary_miband_interval_in_nightmode),
                enabled = mibandEnabled,
            )
        }
    }

    SwitchPref(state, "miband_collect_heartrate", context.getString(R.string.title_miband_colect_heartrate), default = true, subtitle = context.getString(R.string.summary_miband_colect_heartrate))

    if (type == MiBand.MiBandType.MI_BAND2) {
        SettingsCategory(context.getString(R.string.title_screens)) {
            SwitchPref(state, "miband_screen_step_counter", context.getString(R.string.title_lefun_screen_step_counter), default = true)
            SwitchPref(state, "miband_screen_step_distance", context.getString(R.string.title_lefun_screen_step_distance), default = true)
            SwitchPref(state, "miband_screen_step_calories", context.getString(R.string.title_lefun_screen_step_calories), default = true)
            SwitchPref(state, "miband_screen_heart_rate", context.getString(R.string.title_lefun_screen_heart_rate), default = true)
            SwitchPref(state, "miband_screen_battery", context.getString(R.string.title_miband_screen_battery), default = true)
        }
    }

    if (type == MiBand.MiBandType.MI_BAND3 || type == MiBand.MiBandType.MI_BAND3_1 || type == MiBand.MiBandType.MI_BAND4) {
        SettingsCategory(context.getString(R.string.title_screens)) {
            SwitchPref(state, "miband_screen_notifications", context.getString(R.string.title_miband_screen_notifications), default = true)
            SwitchPref(state, "miband_screen_weather", context.getString(R.string.title_miband_screen_weather), default = true)
            SwitchPref(state, "miband_screen_activity", context.getString(R.string.title_miband_screen_activity), default = true)
            SwitchPref(state, "miband_screen_more", context.getString(R.string.title_miband_screen_more), default = true)
            SwitchPref(state, "miband_screen_status", context.getString(R.string.title_miband_screen_status), default = true)
            SwitchPref(state, "miband_screen_heart_rate", context.getString(R.string.title_miband_screen_heart_rate), default = true)
            SwitchPref(state, "miband_screen_timer", context.getString(R.string.title_miband_screen_timer), default = true)
            SwitchPref(state, "miband_screen_nfc", context.getString(R.string.title_miband_screen_nfc), default = false)
        }
    }

    if (engineering) {
        SettingsCategory("Experimental") {
            SwitchPref(state, "debug_miband_use_custom_watchface", "Allow to use custom watchface", default = false, subtitle = "You should place 'my_watchface.bin' and 'my_image.png' file into 'xdrip' folder on your phone", enabled = mibandEnabled)
            EditPref(
                state,
                "debug_miband_image_offset",
                "Image offset(px)",
                default = "0",
                subtitle = "Offset for autogenerated data from the beginning of 'my_image.png' file",
                numeric = true,
                enabled = state.dependentEnabled("debug_miband_use_custom_watchface", false),
            )
            SwitchPref(state, "debug_miband_disable_hight_mtu", "Disable hight MTU values", default = false, subtitle = "Enabling this option in some cases can fix watchface syncronization error", enabled = mibandEnabled)
        }
    }
}

private fun nightmodeTitle(context: Context, rawInterval: Int): String {
    val title = context.getString(R.string.title_miband_interval_in_nightmode)
    val interval = (rawInterval + 1) * MiBandEntry.NIGHT_MODE_INTERVAL_STEP
    return if (interval == MiBandEntry.NIGHT_MODE_INTERVAL_STEP) {
        "$title (live)"
    } else {
        "$title ($interval ${context.getString(R.string.unit_minutes)})"
    }
}

/* ---------------------------------------------------------------------------- Pebble */

@Composable
internal fun PebbleSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val master = state.bool("broadcast_to_pebble", false)
    val syncType = SettingsVisibility.pebbleSyncType(state)
    var pendingInstall by remember { mutableStateOf<Int?>(null) }
    var pendingSnooze by remember { mutableStateOf(false) }

    val restart = { PebbleActions.restartPebble(context) }

    SettingsCategory(context.getString(R.string.pebble_integration)) {
        SwitchPref(
            state,
            "broadcast_to_pebble",
            context.getString(R.string.pebble_watch_integration),
            default = false,
            subtitle = context.getString(R.string.send_data_to_pebble_watchface),
            tag = "setting_pebble_master",
            onCheckedChange = { checked ->
                state.setBool("broadcast_to_pebble", checked)
                val type = state.string("broadcast_to_pebble_type", "1").toIntOrNull() ?: 1
                if (checked && type != 1) pendingInstall = type
                PebbleActions.enablePebble(context, type, checked)
            },
        )
        ListPref(
            state,
            "broadcast_to_pebble_type",
            context.getString(R.string.choose_pebble_watchface),
            context.resources.getStringArray(R.array.SendToPebbleChoice).toList(),
            context.resources.getStringArray(R.array.SendToPebbleChoiceValues).toList(),
            "2",
            subtitle = context.getString(R.string.standard_or_trend_pebble_watchface),
            enabled = master,
            tag = "setting_pebble_type",
            onSelected = { newValue ->
                state.setString("broadcast_to_pebble_type", newValue)
                val type = newValue.toIntOrNull() ?: 1
                if (type != 1) pendingInstall = type
                PebbleActions.enablePebble(context, type, master)
            },
        )
        if (syncType == 3 || syncType == 4 || syncType == 5) {
            SwitchPref(state, "pebble_display_trend", context.getString(R.string.pref_pebble_display_trend), default = true, subtitle = context.getString(R.string.pref_summary_display_trend), enabled = master, tag = "setting_pebble_trend", onCheckedChange = { state.setBool("pebble_display_trend", it); restart() })
            SwitchPref(state, "pebble_filtered_line", context.getString(R.string.display_filtered_line), default = false, subtitle = context.getString(R.string.summary_Also_show_the_filtered_data_on_the_trend), enabled = master, onCheckedChange = { state.setBool("pebble_filtered_line", it); restart() })
            SwitchPref(state, "pebble_tiny_dots", context.getString(R.string.use_tiny_dots), default = false, subtitle = context.getString(R.string.show_tiny_dots_instead), enabled = master, onCheckedChange = { state.setBool("pebble_tiny_dots", it); restart() })
            SwitchPref(state, "pebble_high_line", context.getString(R.string.pref_pebble_display_high_line), default = false, subtitle = context.getString(R.string.displays_the_high_line), enabled = master, onCheckedChange = { state.setBool("pebble_high_line", it); restart() })
            SwitchPref(state, "pebble_low_line", context.getString(R.string.pref_pebble_display_low_line), default = false, subtitle = context.getString(R.string.displays_the_low_line), enabled = master, onCheckedChange = { state.setBool("pebble_low_line", it); restart() })
            ListPref(
                state,
                "pebble_trend_period",
                context.getString(R.string.trend_time_period),
                context.resources.getStringArray(R.array.PebbleTrendPeriods).toList(),
                context.resources.getStringArray(R.array.PebbleTrendPeriodValues).toList(),
                "3",
                subtitle = context.getString(R.string.set_the_trend_period_to_display),
                enabled = master,
                onSelected = { state.setString("pebble_trend_period", it); restart() },
            )
            SwitchPref(state, "pebble_show_delta", context.getString(R.string.display_delta), default = true, subtitle = context.getString(R.string.displays_the_delta_value), enabled = master, onCheckedChange = { state.setBool("pebble_show_delta", it); restart() })
            SwitchPref(state, "pebble_show_delta_units", context.getString(R.string.display_delta_units), default = false, subtitle = context.getString(R.string.displays_the_delta_units), enabled = master, onCheckedChange = { state.setBool("pebble_show_delta_units", it); restart() })
            SwitchPref(state, "pebble_show_arrows", context.getString(R.string.display_slope_arrows), default = true, subtitle = context.getString(R.string.displays_the_slope_arrows), enabled = master, onCheckedChange = { state.setBool("pebble_show_arrows", it); restart() })
            SwitchPref(state, "pebble_vibrate_no_signal", context.getString(R.string.vibrate_when_missed_signal), default = false, subtitle = context.getString(R.string.vibrate_watch_to_alert_no_data), enabled = master)
        }
        SwitchPref(state, "pebble_vibrate_no_bluetooth", context.getString(R.string.vibrate_when_no_bluetooth), default = true, subtitle = context.getString(R.string.vibrate_watch_to_alert_no_bluetooth), enabled = master, onCheckedChange = { state.setBool("pebble_vibrate_no_bluetooth", it); restart() })
        SwitchPref(state, "pebble_vibe_alerts", context.getString(R.string.vibrate_alerts), default = false, subtitle = context.getString(R.string.watch_vibrate_active_alerts))
        SwitchPref(state, "pebble_show_bwp", context.getString(R.string.title_Display_BWP_Insulin), default = false, subtitle = context.getString(R.string.summary_Show_Bolus_Wizard_Preview_Insulin_value_on_watch_when_appropriate), onCheckedChange = { state.setBool("pebble_show_bwp", it); restart() })
        if (syncType != 1) {
            EditPref(
                state,
                "pebble_special_value",
                context.getString(R.string.special_value),
                default = "99",
                subtitle = context.getString(R.string.special_glucose_value_to_display_message),
                enabled = master,
                tag = "setting_pebble_special_value",
            )
            EditPref(
                state,
                "pebble_special_text",
                context.getString(R.string.text_to_display_when_hitting_special_value),
                default = "BAZINGA!",
                subtitle = context.getString(R.string.message_to_display_when_bgl_hits),
                enabled = master,
            )
        }
    }
    SettingsCategory(context.getString(R.string.install_pebble_apps)) {
        SettingsActionRow(
            title = context.getString(R.string.install_pebble_snooze_control_app),
            subtitle = context.getString(R.string.attach_to_button),
            onClick = { context.startActivity(Intent(context, com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface.InstallPebbleSnoozeControlApp::class.java)) },
        )
    }

    val installType = pendingInstall
    if (installType != null) {
        AlertDialog(
            onDismissRequest = { pendingInstall = null },
            title = { Text("Pebble Install") },
            text = { Text(PebbleActions.installMessage(installType) ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    pendingInstall = null
                    PebbleActions.installActivity(installType)?.let { context.startActivity(Intent(context, it)) }
                    pendingSnooze = true
                }) { Text(context.getString(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingInstall = null }) { Text(context.getString(R.string.no)) }
            },
        )
    }
    if (pendingSnooze) {
        AlertDialog(
            onDismissRequest = { pendingSnooze = false },
            title = { Text("Snooze Control Install") },
            text = { Text("Install Pebble Snooze Button App?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingSnooze = false
                    context.startActivity(Intent(context, com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface.InstallPebbleSnoozeControlApp::class.java))
                }) { Text(context.getString(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingSnooze = false }) { Text(context.getString(R.string.no)) }
            },
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SmartWatchOptionsScreenPreview() {
    XdripPreview { SmartWatchOptionsScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SmartwatchSensorsScreenPreview() {
    XdripPreview { SmartwatchSensorsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun WearSettingsScreenPreview() {
    XdripPreview { WearSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AmazfitSettingsScreenPreview() {
    XdripPreview { AmazfitSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun LeFunSettingsScreenPreview() {
    XdripPreview { LeFunSettingsScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun LeFunFeaturesScreenPreview() {
    XdripPreview { LeFunFeaturesScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun BlueJaySettingsScreenPreview() {
    XdripPreview { BlueJaySettingsScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun BlueJayAdvancedScreenPreview() {
    XdripPreview { BlueJayAdvancedScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MiBandSettingsScreenPreview() {
    XdripPreview { MiBandSettingsScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MiBandSubSettingsScreenPreview() {
    XdripPreview { MiBandSubSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PebbleSettingsScreenPreview() {
    XdripPreview { PebbleSettingsScreen() }
}

// endregion
