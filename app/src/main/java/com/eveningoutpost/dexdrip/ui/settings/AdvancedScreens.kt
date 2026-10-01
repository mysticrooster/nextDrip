package com.eveningoutpost.dexdrip.ui.settings

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration
import com.eveningoutpost.dexdrip.healthconnect.HealthGamut
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

/**
 * S5a — Other settings (`pref_advanced_settings.xml` → `other_category`).
 *
 * Text-to-speech, inter-app, extra status line, calibration, Bluetooth, BlueReader/Libre2
 * advanced, logging and misc options. Keys/types unchanged; gating mirrors the legacy
 * `removePreference` logic and side effects are attached to the row callbacks.
 */

@Composable
internal fun SpeakReadingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("bg_to_speech", false)
    SettingsCategory(context.getString(R.string.speak_readings)) {
        SwitchPref(
            state,
            "bg_to_speech",
            context.getString(R.string.speak_readings),
            default = false,
            subtitle = context.getString(R.string.if_the_phone_has_text_to_speech)
        )
        SettingsSliderRow(
            context.getString(R.string.title_When_changes_by),
            state.int("speak_readings_change_threshold", 0),
            { state.setInt("speak_readings_change_threshold", it) },
            valueRange = 0..300,
            subtitle = context.getString(R.string.title_When_changes_by),
            enabled = enabled
        )
        SettingsSliderRow(
            context.getString(R.string.title_Or_per_number_of_minutes),
            state.int("speak_readings_change_time", 0),
            { state.setInt("speak_readings_change_time", it) },
            valueRange = 0..300,
            subtitle = context.getString(R.string.title_Or_per_number_of_minutes),
            enabled = enabled
        )
        SwitchPref(
            state,
            "speak_alerts",
            context.getString(R.string.Speak_Alerts),
            default = false,
            subtitle = context.getString(R.string.Speak_Alerts_Summary)
        )
        SwitchPref(
            state,
            "bg_to_speech_trend",
            context.getString(R.string.Speak_trend_arrow_name),
            default = true,
            enabled = enabled
        )
        SwitchPref(
            state,
            "bg_to_speech_repeat_twice",
            context.getString(R.string.speak_glucose_twice),
            default = false,
            enabled = enabled
        )
        SwitchPref(
            state,
            "speak_twice",
            context.getString(R.string.Speak_everything_twice),
            default = false
        )
        EditPref(
            state,
            "speak_readings_custom_language",
            context.getString(R.string.spoken_readings_locale),
            default = ""
        )
        SettingsSliderRow(
            context.getString(R.string.Speech_speed),
            state.int("speech_speed", 7),
            { state.setInt("speech_speed", it) },
            valueRange = 2..20
        )
        SettingsSliderRow(
            context.getString(R.string.Speech_pitch),
            state.int("speech_pitch", 12),
            { state.setInt("speech_pitch", it) },
            valueRange = 4..20
        )
        SwitchPref(
            state,
            "bg_to_speech_shortcut",
            context.getString(R.string.speak_readings_shortcut),
            default = false,
            subtitle = context.getString(R.string.short_speak_readings_shortcut)
        )
    }
}

@Composable
internal fun InterAppScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val broadcast = state.bool("broadcast_data_through_intents", false)
    val webservice = state.bool("xdrip_webservice", false)
    SettingsCategory(context.getString(R.string.interapp_settings)) {
        SwitchPref(
            state,
            "broadcast_data_through_intents",
            context.getString(R.string.pref_title_broadcast_enabled),
            default = false,
            subtitle = context.getString(R.string.pref_summary_broadcast_enabled)
        )
        SwitchPref(
            state,
            "broadcast_data_use_best_glucose",
            context.getString(R.string.send_display_glucose),
            default = false,
            subtitle = context.getString(R.string.use_plugins_for_brodcast),
            enabled = broadcast
        )
        ListPref(
            state,
            "noise_block_level",
            context.getString(R.string.noise_blocking),
            context.resources.getStringArray(R.array.NoiseLevelBlock).toList(),
            context.resources.getStringArray(R.array.NoiseLevelBlockValues).toList(),
            "200",
            subtitle = context.getString(R.string.level_at_which_noisy_data_should_not_be_broadcast),
            enabled = broadcast
        )
        SwitchPref(
            state,
            "broadcast_data_through_intents_without_permission",
            context.getString(R.string.compatible_broadcast),
            default = true,
            subtitle = context.getString(R.string.send_broadcasts_old_permission_model),
            enabled = broadcast
        )
        EditPref(
            state,
            "local_broadcast_specific_package_destination",
            context.getString(R.string.title_Identify_receiver),
            default = "",
            subtitle = context.getString(R.string.summary_Only_send_to_named_package),
            enabled = broadcast
        )
        SwitchPref(
            state,
            "accept_nsclient_sgv",
            context.getString(R.string.accept_glucose),
            default = true,
            subtitle = context.getString(R.string.process_glucose_data_received)
        )
        SwitchPref(
            state,
            "accept_nsclient_treatments",
            context.getString(R.string.accept_treatments),
            default = true,
            subtitle = context.getString(R.string.process_nsclient_treatments)
        )
        SwitchPref(
            state,
            "profile_import_sound",
            context.getString(R.string.import_sounds),
            default = false,
            subtitle = context.getString(R.string.play_sounds_when_importing),
            enabled = state.bool("accept_nsclient_treatments", true)
        )
        SwitchPref(
            state,
            "accept_broadcast_calibrations",
            context.getString(R.string.title_Accept_Calibrations),
            default = true,
            subtitle = context.getString(R.string.process_broadcasted_calibrations)
        )
        SwitchPref(
            state,
            "accept_external_status",
            context.getString(R.string.accept_external_status),
            default = true,
            subtitle = context.getString(R.string.external_status_strings_eg_tbr)
        )
        SwitchPref(
            state,
            "xdrip_webservice",
            context.getString(R.string.title_xDrip_Web_Service),
            default = false,
            subtitle = context.getString(R.string.summary_Operate_a_local_web_server_for_interacting_with_Fitbit_Ionic_etc)
        )
        SwitchPref(
            state,
            "xdrip_webservice_open",
            context.getString(R.string.title_Open_Web_Service),
            default = false,
            subtitle = context.getString(R.string.summary_Accept_connections_from_any_network_instead_of_just_internally),
            enabled = webservice
        )
        EditPref(
            state,
            "xdrip_webservice_secret",
            context.getString(R.string.title_xDrip_Web_Service_Secret),
            default = "",
            subtitle = context.getString(R.string.summary_Shared_Secret_for_open_web_service),
            enabled = webservice
        )
        SwitchPref(
            state,
            "broadcast_service_enabled",
            context.getString(R.string.title_broadcast_service_api),
            default = false,
            subtitle = context.getString(R.string.summary_broadcast_service_api)
        )
        SwitchPref(
            state,
            "enable_iob_in_api_endpoint",
            context.getString(R.string.title_enable_iob_in_api_endpoint),
            default = true,
            subtitle = context.getString(R.string.summary_enable_iob_in_api_endpoint)
        )
        SwitchPref(
            state,
            "fetch_iob_from_companion_app",
            context.getString(R.string.title_fetch_iob_from_companion_app),
            default = false,
            subtitle = context.getString(R.string.summary_fetch_iob_from_companion_app)
        )
        SettingsActionRow(
            title = context.getString(R.string.google_health_connect),
            onClick = { onNavigate(SettingsScreen.HealthConnect) },
            modifier = Modifier.testTag("setting_health_connect")
        )
    }
}

@Composable
internal fun HealthConnectScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("health_connect_enable", false)
    SettingsCategory(context.getString(R.string.google_health_connect)) {
        SwitchPref(
            state,
            "health_connect_enable",
            context.getString(R.string.use_health_connect),
            default = false,
            subtitle = context.getString(R.string.requires_android_8_and_google_companion_application_or_android_14),
            onCheckedChange = { checked ->
                state.setBool("health_connect_enable", checked)
                if (checked) (context as? Activity)?.let { HealthGamut.init(it) }
            },
        )
        SwitchPref(
            state,
            "health_connect_receive",
            context.getString(R.string.get_data_from_health_connect),
            default = true,
            subtitle = context.getString(R.string.get_data_from_health_connect),
            enabled = enabled
        )
        SwitchPref(
            state,
            "health_connect_send",
            context.getString(R.string.send_data_to_health_connect),
            default = false,
            subtitle = context.getString(R.string.send_data_to_health_connect),
            enabled = enabled
        )
        SettingsActionRow(
            title = context.getString(R.string.manage_permissions),
            subtitle = context.getString(R.string.open_health_connect_settings_to_manually_manage_permissions),
            onClick = {
                (context as? Activity)?.let {
                    HealthGamut.init(it).openPermissionManager()
                }
            },
            enabled = enabled,
        )
    }
}

@Composable
internal fun ExtraStatusLineScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("extra_status_line", false)
    SettingsCategory(context.getString(R.string.extra_status_line)) {
        SwitchPref(
            state,
            "extra_status_line",
            context.getString(R.string.extra_status_line),
            default = false,
            subtitle = context.getString(R.string.additional_text_status),
            tag = "setting_extra_status_line"
        )
        SwitchPref(
            state,
            "extra_status_stats_24h",
            context.getString(R.string.sliding_24_hour_window),
            default = false,
            subtitle = context.getString(R.string.sliding_24_hour_window_summary),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_avg",
            context.getString(R.string.average),
            default = false,
            subtitle = context.getString(R.string.todays_average_value),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_a1c_dcct",
            context.getString(R.string.a1c_dcct),
            default = false,
            subtitle = context.getString(R.string.ac1_estimation_dcct),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_a1c_ifcc",
            context.getString(R.string.a1c_ifcc),
            default = false,
            subtitle = context.getString(R.string.ac1_estimation_ifcc),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_in",
            context.getString(R.string.in_percentage),
            default = false,
            subtitle = context.getString(R.string.percentage_in_range),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_high",
            context.getString(R.string.high_percentage),
            default = false,
            subtitle = context.getString(R.string.percentage_above_range),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_low",
            context.getString(R.string.low_percentage),
            default = false,
            subtitle = context.getString(R.string.percentage_below_range),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_stdev",
            context.getString(R.string.standard_deviation),
            default = false,
            subtitle = context.getString(R.string.show_standard_deviation),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_carbs",
            context.getString(R.string.total_carbs),
            default = false,
            subtitle = context.getString(R.string.show_total_carbs),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_insulin",
            context.getString(R.string.total_insulin),
            default = false,
            subtitle = context.getString(R.string.show_total_insulin),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_external_status",
            context.getString(R.string.title_External_Status),
            default = false,
            subtitle = context.getString(R.string.summary_Display_status_from_other_apps_like_AndroidAPS),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_pump_reservoir",
            context.getString(R.string.title_Pump_Status),
            default = false,
            subtitle = context.getString(R.string.summary_Display_pump_status_information_if_available),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_royce_ratio",
            context.getString(R.string.title_Carb_Insulin_Ratio),
            default = false,
            subtitle = context.getString(R.string.summary_Show_treatment_carb_insulin_ratio),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_capture_percentage",
            context.getString(R.string.capture_percentage),
            default = false,
            subtitle = context.getString(R.string.received_readings_percentage),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_realtime_capture_percentage",
            context.getString(R.string.realtime_capture_percentage),
            default = false,
            subtitle = context.getString(R.string.received_realtime_readings_percentage),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_accuracy",
            context.getString(R.string.accuracy_evaluation),
            default = false,
            subtitle = context.getString(R.string.show_calibration_accuracy),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_time",
            context.getString(R.string.time),
            default = false,
            subtitle = context.getString(R.string.the_current_time),
            enabled = enabled
        )
        SwitchPref(
            state,
            "widget_status_line",
            context.getString(R.string.show_on_widget),
            default = false,
            subtitle = context.getString(R.string.show_extra_status_on_widget),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_calibration_long",
            context.getString(R.string.calibration_data_long),
            default = true,
            subtitle = context.getString(R.string.show_long_calibration_data),
            enabled = enabled
        )
        SwitchPref(
            state,
            "status_line_calibration_short",
            context.getString(R.string.calibration_data_short),
            default = false,
            subtitle = context.getString(R.string.show_short_calibration_data),
            enabled = enabled
        )
        SwitchPref(
            state,
            "extra_status_calibration_plugin",
            context.getString(R.string.calibration_plugin),
            default = false,
            subtitle = context.getString(R.string.show_plugin_data),
            enabled = enabled
        )
    }
}

@Composable
internal fun CalibrationSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val engineering = SettingsVisibility.isEngineeringMode(state)
    val pluginChoices = remember { calibrationPluginChoices(context) }
    SettingsCategory(context.getString(R.string.advanced_calibration)) {
        ListPref(
            state,
            "treatment_fingerstick_calibration_usage",
            context.getString(R.string.usw_treatment_bg),
            context.resources.getStringArray(R.array.TreatmentCalibrationChoiceDetail).toList(),
            context.resources.getStringArray(R.array.TreatmentCalibrationChoice).toList(),
            "auto",
            subtitle = context.getString(R.string.choose_use_treament_bg)
        )
        SwitchPref(
            state,
            "bluetooth_meter_for_calibrations_auto",
            context.getString(R.string.automatic_calibration),
            default = false,
            subtitle = context.getString(R.string.auto_calibration_good_conditions)
        )
        SettingsListRow(
            title = context.getString(R.string.calibration_plugin),
            entries = pluginChoices.first,
            values = pluginChoices.second,
            selectedValue = state.string("current_calibration_plugin", "None"),
            onSelected = { newValue ->
                PluggableCalibration.invalidateCache()
                PluggableCalibration.invalidateCache(newValue)
                PluggableCalibration.invalidatePluginCache()
                state.setString("current_calibration_plugin", newValue)
            },
            modifier = Modifier.testTag("setting_calibration_plugin"),
            subtitle = context.getString(R.string.experimental_calibration_plugin),
        )
        SwitchPref(
            state,
            "plugin_plot_on_graph",
            context.getString(R.string.plugin_plot_on_graph),
            default = false,
            subtitle = context.getString(R.string.show_plugin_results_on_graph)
        )
        SwitchPref(
            state,
            "display_glucose_from_plugin",
            context.getString(R.string.use_plugin_glucose),
            default = false,
            subtitle = context.getString(R.string.main_glucose_plugin)
        )
        SwitchPref(
            state,
            "use_pluggable_alg_as_primary",
            context.getString(R.string.plugin_override_all),
            default = false,
            subtitle = context.getString(R.string.all_new_glucose_data_plugin),
            enabled = engineering
        )
        SwitchPref(
            state,
            "use_double_calibrations",
            context.getString(R.string.title_Double_Calibrations),
            default = true,
            subtitle = context.getString(R.string.summary_Ask_for_a_second_optional_initial_calibration_blood_test)
        )
        SwitchPref(
            state,
            "old_school_calibration_mode",
            context.getString(R.string.old_school_calibration_mode),
            default = false,
            subtitle = context.getString(R.string.old_school_calibrations),
            enabled = engineering
        )
        SwitchPref(
            state,
            "infrequent_calibration",
            context.getString(R.string.infrequent_calibrations),
            default = true,
            subtitle = context.getString(R.string.use_infrequent_calibrations)
        )
        SwitchPref(
            state,
            "use_non_fixed_li_parameters",
            context.getString(R.string.title_Non_fixed_Libre_slopes),
            default = true,
            subtitle = context.getString(R.string.summary_Enable_to_allow_variable_slopes_with_Libre_collection_methods)
        )
        SwitchPref(
            state,
            "detect_libre_sn_changes",
            "Check Libre Serial",
            default = true,
            subtitle = "Automatically stop if Libre serial changes unexpectedly"
        )
        SwitchPref(
            state,
            "bypass_calibration_quality_check",
            context.getString(R.string.title_Bypass_quality_check),
            default = false,
            subtitle = context.getString(R.string.summary_Allow_initial_calibration_no_good_enough_data)
        )
    }
}

@Composable
internal fun BluetoothSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val engineering = SettingsVisibility.isEngineeringMode(state)
    SettingsCategory(context.getString(R.string.bluetooth_settings)) {
        SwitchPref(
            state,
            "automatically_turn_bluetooth_on",
            context.getString(R.string.turn_bluetooth_on),
            default = true,
            subtitle = context.getString(R.string.auto_turn_on_bluetooth)
        )
        SwitchPref(
            state,
            "bluetooth_watchdog",
            context.getString(R.string.bluetooth_watchdog),
            default = true,
            subtitle = context.getString(R.string.reset_bluetooth)
        )
        ListPref(
            state,
            "bluetooth_watchdog_timer",
            context.getString(R.string.bluetooth_watchdog_timers),
            context.resources.getStringArray(R.array.bluetooth_watchdog_timer_entries).toList(),
            context.resources.getStringArray(R.array.bluetooth_watchdog_timer_values).toList(),
            "20",
            subtitle = context.getString(R.string.bluetooth_watchdog_timer_sums),
            enabled = state.bool("bluetooth_watchdog", true)
        )
        SwitchPref(
            state,
            "close_gatt_on_ble_disconnect",
            context.getString(R.string.close_gatt_on_ble_disconnect),
            default = false,
            subtitle = context.getString(R.string.close_gatt)
        )
        SwitchPref(
            state,
            "bluetooth_use_scan",
            context.getString(R.string.title_Use_scanning),
            default = false,
            subtitle = context.getString(R.string.summary_Scan_before_connecting_on_xBridge_and_Libre_bluetooth)
        )
        SwitchPref(
            state,
            "bluetooth_use_blemanager",
            "Use BLE manager",
            default = false,
            subtitle = "Use a more modern BLE manager system"
        )
        SwitchPref(
            state,
            "bluetooth_trust_autoconnect",
            context.getString(R.string.title_Trust_Auto_Connect),
            default = true,
            subtitle = context.getString(R.string.summary_Use_and_trust_Android_bluetooth_auto_connect_feature)
        )
        SwitchPref(
            state,
            "bluetooth_allow_background_scans",
            context.getString(R.string.use_background_scans),
            default = true,
            subtitle = context.getString(R.string.android_8_background_scanning)
        )
        SwitchPref(
            state,
            "bluetails_enabled",
            "Companion Bluetooth",
            default = false,
            subtitle = "Use Bluetooth with companion app data source"
        )
        SwitchPref(
            state,
            "bluetooth_excessive_wakelocks",
            context.getString(R.string.bluetooth_wakelocks),
            default = false,
            subtitle = context.getString(R.string.older_bluetooth_wakelocks)
        )
        SwitchPref(
            state,
            "pref_dex_collection_polling",
            context.getString(R.string.title_xBridge_Polling_Mode),
            default = false,
            subtitle = context.getString(R.string.summary_Experimental_support_for_xBridge_polling_feature)
        )
        SwitchPref(
            state,
            "always_discover_services",
            context.getString(R.string.title_Always_discover_services),
            default = true,
            subtitle = context.getString(R.string.summary_Probe_Bluetooth_services_on_every_connect),
            enabled = engineering
        )
        SwitchPref(
            state,
            "use_gatt_refresh",
            "Use GATT refresh",
            default = true,
            subtitle = "Whether to use the low level gatt refresh mechanism",
            enabled = engineering
        )
        SwitchPref(
            state,
            "blukon_unbonding",
            context.getString(R.string.title_Allow_blucon_unbonding),
            default = true,
            subtitle = context.getString(R.string.summary_Only_suitable_for_phones_which_support_automatic_pairing)
        )
    }
}

@Composable
internal fun BlueReaderSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.advanced_bluereader_settings)) {
        SwitchPref(
            state,
            "blueReader_restdays_on_home",
            context.getString(R.string.bluereader_restdays_onhome),
            default = true,
            subtitle = context.getString(R.string.bluereader_restdays_onhome_sum)
        )
        SwitchPref(
            state,
            "blueReader_turn_off",
            context.getString(R.string.bluereader_turnoff),
            default = false,
            subtitle = context.getString(R.string.bluereader_turnoff_sum)
        )
        SettingsSliderRow(
            title = "${context.getString(R.string.blueReader_turnoffvalue)} (${
                state.int(
                    "blueReader_turn_off_value",
                    5
                )
            })",
            value = state.int("blueReader_turn_off_value", 5),
            onValueChange = { state.setInt("blueReader_turn_off_value", it) },
            valueRange = 1..20,
            subtitle = context.getString(R.string.blueReader_turnoffvalue_sum),
            enabled = state.bool("blueReader_turn_off", false),
        )
        SwitchPref(
            state,
            "blueReader_suppressuglystatemsg",
            context.getString(R.string.title_supress_ugly_state_message),
            default = false,
            subtitle = context.getString(R.string.summary_Suppress_the_ugly_state_message_if_Problem_appears)
        )
        SwitchPref(
            state,
            "blueReader_writebatterylog",
            context.getString(R.string.title_Batterylog),
            default = false,
            subtitle = context.getString(R.string.summary_write_Battery_Information_for_additional_analytic)
        )
    }
}

@Composable
internal fun Libre2SettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.title_advanced_settings_4_Lib2)) {
        SwitchPref(
            state,
            "Libre2_showRawGraph",
            context.getString(R.string.title_Lib2_show_raw_values),
            default = false,
            subtitle = context.getString(R.string.summary_Lib2_show_raw_values)
        )
        SwitchPref(
            state,
            "Libre2_showSensors",
            context.getString(R.string.title_Lib2_show_sense_on_status),
            default = false,
            subtitle = context.getString(R.string.summary_Lib2_show_sense_on_status)
        )
    }
}

@Composable
internal fun LoggingSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val engineering = SettingsVisibility.isEngineeringMode(state)
    SettingsCategory(context.getString(R.string.extra_logging)) {
        SwitchPref(
            state,
            "enable_bugfender",
            context.getString(R.string.enable_remote_logging),
            default = false,
            subtitle = context.getString(R.string.send_logs_to_developer)
        )
        if (state.bool("enable_bugfender", false)) {
            EditPref(
                state,
                "bugfender_appid",
                context.getString(R.string.set_logging_appid),
                default = ""
            )
        }
        SwitchPref(
            state,
            "store_logs",
            context.getString(R.string.store_logs),
            default = false,
            subtitle = context.getString(R.string.only_enable_on_trouble),
            enabled = engineering
        )
        EditPref(
            state,
            "extra_tags_for_logging",
            context.getString(R.string.extra_tags_for_logging),
            default = ""
        )
    }
}

@Composable
internal fun OtherMiscSettingsScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val engineering = SettingsVisibility.isEngineeringMode(state)
    SettingsCategory(context.getString(R.string.title_Other_misc_options)) {
        SettingsActionRow(
            title = context.getString(R.string.title_collector_in_foreground),
            onClick = { onNavigate(SettingsScreen.CollectorInForeground) })
        SwitchPref(
            state,
            "engineering_mode",
            context.getString(R.string.engineering_mode),
            default = false,
            subtitle = context.getString(R.string.allow_unsafe_settings)
        )
        SwitchPref(
            state,
            "requested_ignore_battery_optimizations_new",
            context.getString(R.string.battery_optimization_prompt),
            default = false,
            subtitle = context.getString(R.string.battery_optimization_on)
        )
        SwitchPref(
            state,
            "allow_samsung_workaround",
            context.getString(R.string.title_allow_samsung_workaround),
            default = true,
            subtitle = context.getString(R.string.summary_allow_samsung_workaround)
        )
        SwitchPref(
            state,
            "excessive_wakelocks",
            context.getString(R.string.use_excessive_wakelocks),
            default = false,
            subtitle = context.getString(R.string.older_gratuitous_wakelocks)
        )
        SwitchPref(
            state,
            "predictive_bg",
            context.getString(R.string.display_predictive_values),
            default = false,
            subtitle = context.getString(R.string.predictive_readings_old),
            enabled = engineering
        )
        SwitchPref(
            state,
            "use_proper_ongoing",
            "Proper ongoing",
            default = true,
            subtitle = "Use proper ongoing notification. Disabling this causes collector problems on Android 8+",
            enabled = engineering
        )
        SwitchPref(
            state,
            "external_blukon_algorithm",
            context.getString(R.string.use_external_blukon_algorithm),
            default = false,
            subtitle = context.getString(R.string.use_external_blukon_algorithm_summary)
        )
        ListPref(
            state,
            "calibrate_external_libre_2_algorithm_type",
            context.getString(R.string.calibrate_external_libre_2_algorithm_title),
            context.resources.getStringArray(R.array.CalibrateExternalLibre2).toList(),
            context.resources.getStringArray(R.array.CalibrateExternalLibre2Values).toList(),
            "calibrate_raw",
            subtitle = context.getString(R.string.calibrate_external_libre_2_algorithm_summary)
        )
        SwitchPref(
            state,
            "libre_use_smoothed_data",
            context.getString(R.string.libre_use_smoothed_data_title),
            default = false,
            subtitle = context.getString(R.string.libre_use_smoothed_data_summary)
        )
        SwitchPref(
            state,
            "retrieve_blukon_history",
            context.getString(R.string.retrieve_blukon_history_title),
            default = false,
            subtitle = context.getString(R.string.retrieve_blukon_history_summary),
            enabled = !state.bool("external_blukon_algorithm", false)
        )
        SwitchPref(
            state,
            "libre_one_minute",
            context.getString(R.string.title_libre_one_minute_interval),
            default = false,
            subtitle = context.getString(R.string.summary_libre_one_minute_interval)
        )
        SwitchPref(
            state,
            "Eversense_one_minute",
            "1-min Inter-app broadcast",
            default = false,
            subtitle = "Accept data at one-minute intervals from Inter-app broadcast. Reboot your phone for changes to take effect."
        )
        SwitchPref(
            state,
            "allow_testing_with_dead_sensor",
            context.getString(R.string.title_NOT_FOR_PRODUCTION_USE),
            default = false,
            subtitle = context.getString(R.string.summary_allow_testing_with_dead_sensor)
        )
        SwitchPref(
            state,
            "aggressive_service_restart",
            context.getString(R.string.aggressive_service_restarts),
            default = true,
            subtitle = context.getString(R.string.repeatedly_restart_collection_service)
        )
        SwitchPref(
            state,
            "interpret_raw",
            context.getString(R.string.interpret_raw),
            default = false,
            subtitle = context.getString(R.string.interpret_share_raw)
        )
        SwitchPref(
            state,
            "show_data_tables",
            context.getString(R.string.show_datatables),
            default = false,
            subtitle = context.getString(R.string.show_datatables_in_app_drawer)
        )
        SwitchPref(
            state,
            "display_bridge_battery",
            context.getString(R.string.display_bridge_battery),
            default = true,
            subtitle = context.getString(R.string.summary_Choose_to_display_the_bridge_battery_level)
        )
        SwitchPref(
            state,
            "disable_battery_warning",
            context.getString(R.string.disable_battery_warning),
            default = false,
            subtitle = context.getString(R.string.disable_log_transmitter_battery_warning)
        )
        SwitchPref(
            state,
            "save_db_ondemand",
            context.getString(R.string.daily_save_db),
            default = false,
            subtitle = context.getString(R.string.allow_daily_db_save)
        )
        EditPref(
            state,
            "retention_days_bg_reading",
            "${context.getString(R.string.title_Glucose_Retention)} (${
                state.string(
                    "retention_days_bg_reading",
                    "180"
                )
            })",
            default = "180",
            numeric = true,
            subtitle = context.getString(R.string.summary_Erase_data_older_than_this_many_days),
        )
    }
}

@Composable
internal fun CollectorInForegroundScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.title_collector_in_foreground)) {
        SwitchPref(
            state,
            "compact_persistent_notification",
            context.getString(R.string.title_compact_ongoing_notification),
            default = false,
            subtitle = context.getString(R.string.summary_compact_ongoing_notification)
        )
    }
}

/** Calibration plugin entries/values, reusing the legacy `PluggableCalibration` population. */
private fun calibrationPluginChoices(context: Context): Pair<List<String>, List<String>> {
    val listPreference = android.preference.ListPreference(context)
    PluggableCalibration.setListPreferenceData(listPreference)
    val entries = listPreference.entries?.map { it.toString() } ?: emptyList()
    val values = listPreference.entryValues?.map { it.toString() } ?: emptyList()
    return entries to values
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun SpeakReadingsScreenPreview() {
    XdripPreview { SpeakReadingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun InterAppScreenPreview() {
    XdripPreview { InterAppScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun HealthConnectScreenPreview() {
    XdripPreview { HealthConnectScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun ExtraStatusLineScreenPreview() {
    XdripPreview { ExtraStatusLineScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun CalibrationSettingsScreenPreview() {
    XdripPreview { CalibrationSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun BluetoothSettingsScreenPreview() {
    XdripPreview { BluetoothSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun BlueReaderSettingsScreenPreview() {
    XdripPreview { BlueReaderSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun Libre2SettingsScreenPreview() {
    XdripPreview { Libre2SettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun LoggingSettingsScreenPreview() {
    XdripPreview { LoggingSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun OtherMiscSettingsScreenPreview() {
    XdripPreview { OtherMiscSettingsScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun CollectorInForegroundScreenPreview() {
    XdripPreview { CollectorInForegroundScreen() }
}

// endregion
