package com.eveningoutpost.dexdrip.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.AlertList
import com.eveningoutpost.dexdrip.EditAlertActivity
import com.eveningoutpost.dexdrip.MissedReadingActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.models.UserNotification
import com.eveningoutpost.dexdrip.ui.theme.LocalXdripColors

@Composable
internal fun titleFor(screen: SettingsScreen): String = when (screen) {
    SettingsScreen.Root -> "Settings"
    SettingsScreen.Units -> stringResource(R.string.glucose_units)
    SettingsScreen.Theme -> stringResource(R.string.theme_colors)
    SettingsScreen.Notifications -> stringResource(R.string.alarms_and_alerts)
    SettingsScreen.BgAlerts -> stringResource(R.string.glucose_alerts_settings)
    SettingsScreen.SuppressAlerts -> stringResource(R.string.suppress_alerts_if_missed_readings)
    SettingsScreen.NotificationChannels -> stringResource(R.string.title_use_notification_channels)
    SettingsScreen.AscendingVolume -> stringResource(R.string.title_ascending_volume)
    SettingsScreen.PersistentHigh -> stringResource(R.string.persistent_high_alert)
    SettingsScreen.ForecastLow -> stringResource(R.string.forecasted_low_alert)
    SettingsScreen.SensorExpiry -> stringResource(R.string.title_sens_expiry)
    SettingsScreen.CalibrationAlerts -> stringResource(R.string.calibration_alerts)
    SettingsScreen.OtherAlerts -> stringResource(R.string.other_alerts)
    SettingsScreen.DataSource -> stringResource(R.string.data_source_settings)
    SettingsScreen.ILet -> "iLet"
    SettingsScreen.WebFollow -> "Web Follower Settings"
    SettingsScreen.NfcSettings -> stringResource(R.string.nfc_scan_features)
    SettingsScreen.NsFollowDownload -> stringResource(R.string.title_nsfollow_download_treatments)
    SettingsScreen.G5Debug -> stringResource(R.string.g5_debug_settings)
    SettingsScreen.PreemptiveRestart -> stringResource(R.string.title_ob1_g5_preemptive_restart)
    SettingsScreen.DataSync -> stringResource(R.string.data_sync)
    SettingsScreen.AutoConfig -> stringResource(R.string.auto_configure_title)
    SettingsScreen.CloudUpload -> stringResource(R.string.cloud_upload)
    SettingsScreen.RestApi -> stringResource(R.string.pref_title_api)
    SettingsScreen.RestApiDownload -> stringResource(R.string.title_cloud_storage_api_download_enable)
    SettingsScreen.RestApiExtra -> stringResource(R.string.title_rest_api_extra_options)
    SettingsScreen.Mongo -> stringResource(R.string.pref_title_mongodb)
    SettingsScreen.Influx -> stringResource(R.string.pref_title_influxdb)
    SettingsScreen.DexcomUpload -> stringResource(R.string.dexcom_share_server_upload)
    SettingsScreen.Tidepool -> stringResource(R.string.title_tidepool)
    SettingsScreen.WebDeposit -> "Web Deposit"
    SettingsScreen.NightLite -> "NightLite"
    SettingsScreen.Nocturne -> stringResource(R.string.nocturne)
    SettingsScreen.GlucoseMeters -> stringResource(R.string.glucose_meters)
    SettingsScreen.SpeakReadings -> stringResource(R.string.speak_readings)
    SettingsScreen.InterApp -> stringResource(R.string.interapp_settings)
    SettingsScreen.HealthConnect -> stringResource(R.string.google_health_connect)
    SettingsScreen.LessCommon -> stringResource(R.string.less_common_settings)
    SettingsScreen.ExtraStatusLine -> stringResource(R.string.extra_status_line)
    SettingsScreen.CalibrationSettings -> stringResource(R.string.advanced_calibration)
    SettingsScreen.BluetoothSettings -> stringResource(R.string.bluetooth_settings)
    SettingsScreen.BlueReaderSettings -> stringResource(R.string.advanced_bluereader_settings)
    SettingsScreen.Libre2Settings -> stringResource(R.string.title_advanced_settings_4_Lib2)
    SettingsScreen.LoggingSettings -> stringResource(R.string.extra_logging)
    SettingsScreen.OtherMiscSettings -> stringResource(R.string.title_Other_misc_options)
    SettingsScreen.CollectorInForeground -> stringResource(R.string.title_collector_in_foreground)
    SettingsScreen.SmartWatchOptions -> stringResource(R.string.smart_watch_features)
    SettingsScreen.SmartwatchSensors -> stringResource(R.string.title_Smartwatch_Sensors)
    SettingsScreen.WearSettings -> stringResource(R.string.android_wear_integration)
    SettingsScreen.AmazfitSettings -> stringResource(R.string.amazfit_sync_service)
    SettingsScreen.LeFunSettings -> stringResource(R.string.title_lefun_band)
    SettingsScreen.LeFunFeatures -> stringResource(R.string.title_lefun_screens_features)
    SettingsScreen.BlueJaySettings -> "BlueJay Watch"
    SettingsScreen.BlueJayAdvanced -> "BlueJay Advanced Settings"
    SettingsScreen.MiBandSettings -> stringResource(R.string.title_miband)
    SettingsScreen.MiBandSubSettings -> stringResource(R.string.title_miband_screens_features)
    SettingsScreen.PebbleSettings -> stringResource(R.string.pebble_integration)
    SettingsScreen.XdripPlusDisplay -> stringResource(R.string.xdrip_plus_display_settings)
    SettingsScreen.XdripPlusFont -> stringResource(R.string.title_font_settings)
    SettingsScreen.XdripPlusLanguage -> stringResource(R.string.title_language)
    SettingsScreen.XdripPlusGraphDisplay -> stringResource(R.string.title_xdrip_plus_graph_display_settings)
    SettingsScreen.XdripPlusGraphSmoothing -> stringResource(R.string.graph_smoothing)
    SettingsScreen.XdripPlusYAxis -> stringResource(R.string.title_yRange)
    SettingsScreen.XdripPlusAccessibility -> stringResource(R.string.title_xdrip_plus_accessibility)
    SettingsScreen.XdripPlusNumberWall -> stringResource(R.string.title_xdrip_plus_number_wall)
    SettingsScreen.XdripPlusNumberIcon -> stringResource(R.string.title_xdrip_plus_number_icon)
    SettingsScreen.XdripPlusCopying -> stringResource(R.string.copying_settings)
    SettingsScreen.XdripPlusUpdate -> stringResource(R.string.xdrip_plus_update_settings)
    SettingsScreen.XdripPlusMotion -> stringResource(R.string.xdrip_motion_tracking)
    SettingsScreen.XdripPlusPens -> stringResource(R.string.insulin_pens)
    SettingsScreen.XdripPlusNovopen -> stringResource(R.string.title_novopen_insulin_pen)
    SettingsScreen.XdripPlusInpen -> stringResource(R.string.title_inpen_screen)
    SettingsScreen.XdripPlusPendiq -> stringResource(R.string.title_pendiq_screen)
    SettingsScreen.XdripPlusPrediction -> stringResource(R.string.xdrip_plus_prediction_settings)
    SettingsScreen.XdripPlusMultipleInsulin -> stringResource(R.string.title_multiple_insulin_types_settings)
    SettingsScreen.XdripPlusAdvPredict -> stringResource(R.string.low_prediction_values)
    SettingsScreen.XdripPlusSync -> stringResource(R.string.xdrip_plus_sync_settings)
    SettingsScreen.XdripPlusRemoteSnooze -> stringResource(R.string.remote_snoozing)
    SettingsScreen.XdripPlusDesertSync -> stringResource(R.string.title_xdrip_plus_desert_sync_settings)
}

@Composable
internal fun SettingsScreenContent(
    screen: SettingsScreen,
    onNavigate: (SettingsScreen) -> Unit,
    onOpenClassic: () -> Unit,
) {
    when (screen) {
        SettingsScreen.Root -> RootScreen(onNavigate, onOpenClassic)
        SettingsScreen.Units -> UnitsScreen()
        SettingsScreen.Theme -> ThemeEditorScreen()
        SettingsScreen.Notifications -> NotificationsScreen(onNavigate)
        SettingsScreen.BgAlerts -> BgAlertsScreen(onNavigate)
        SettingsScreen.SuppressAlerts -> SuppressAlertsScreen()
        SettingsScreen.NotificationChannels -> NotificationChannelsScreen()
        SettingsScreen.AscendingVolume -> AscendingVolumeScreen()
        SettingsScreen.PersistentHigh -> PersistentHighScreen()
        SettingsScreen.ForecastLow -> ForecastLowScreen()
        SettingsScreen.SensorExpiry -> SensorExpiryScreen()
        SettingsScreen.CalibrationAlerts -> CalibrationAlertsScreen()
        SettingsScreen.OtherAlerts -> OtherAlertsScreen()
        SettingsScreen.DataSource -> DataSourceScreen(onNavigate)
        SettingsScreen.ILet -> ILetScreen()
        SettingsScreen.WebFollow -> WebFollowScreen()
        SettingsScreen.NfcSettings -> NfcSettingsScreen()
        SettingsScreen.NsFollowDownload -> NsFollowDownloadScreen()
        SettingsScreen.G5Debug -> G5DebugScreen(onNavigate)
        SettingsScreen.PreemptiveRestart -> PreemptiveRestartScreen()
        SettingsScreen.DataSync -> DataSyncScreen(onNavigate)
        SettingsScreen.AutoConfig -> AutoConfigScreen()
        SettingsScreen.CloudUpload -> CloudUploadScreen(onNavigate)
        SettingsScreen.RestApi -> RestApiScreen(onNavigate)
        SettingsScreen.RestApiDownload -> RestApiDownloadScreen()
        SettingsScreen.RestApiExtra -> RestApiExtraScreen()
        SettingsScreen.Mongo -> MongoScreen()
        SettingsScreen.Influx -> InfluxScreen()
        SettingsScreen.DexcomUpload -> DexcomUploadScreen()
        SettingsScreen.Tidepool -> TidepoolScreen()
        SettingsScreen.WebDeposit -> WebDepositScreen()
        SettingsScreen.NightLite -> NightLiteScreen()
        SettingsScreen.Nocturne -> NocturneScreen()
        SettingsScreen.GlucoseMeters -> GlucoseMetersScreen()
        SettingsScreen.SpeakReadings -> SpeakReadingsScreen()
        SettingsScreen.InterApp -> InterAppScreen(onNavigate)
        SettingsScreen.HealthConnect -> HealthConnectScreen()
        SettingsScreen.LessCommon -> LessCommonScreen(onNavigate)
        SettingsScreen.ExtraStatusLine -> ExtraStatusLineScreen()
        SettingsScreen.CalibrationSettings -> CalibrationSettingsScreen()
        SettingsScreen.BluetoothSettings -> BluetoothSettingsScreen()
        SettingsScreen.BlueReaderSettings -> BlueReaderSettingsScreen()
        SettingsScreen.Libre2Settings -> Libre2SettingsScreen()
        SettingsScreen.LoggingSettings -> LoggingSettingsScreen()
        SettingsScreen.OtherMiscSettings -> OtherMiscSettingsScreen(onNavigate)
        SettingsScreen.CollectorInForeground -> CollectorInForegroundScreen()
        SettingsScreen.SmartWatchOptions -> SmartWatchOptionsScreen(onNavigate)
        SettingsScreen.SmartwatchSensors -> SmartwatchSensorsScreen()
        SettingsScreen.WearSettings -> WearSettingsScreen()
        SettingsScreen.AmazfitSettings -> AmazfitSettingsScreen()
        SettingsScreen.LeFunSettings -> LeFunSettingsScreen(onNavigate)
        SettingsScreen.LeFunFeatures -> LeFunFeaturesScreen()
        SettingsScreen.BlueJaySettings -> BlueJaySettingsScreen(onNavigate)
        SettingsScreen.BlueJayAdvanced -> BlueJayAdvancedScreen()
        SettingsScreen.MiBandSettings -> MiBandSettingsScreen(onNavigate)
        SettingsScreen.MiBandSubSettings -> MiBandSubSettingsScreen()
        SettingsScreen.PebbleSettings -> PebbleSettingsScreen()
        SettingsScreen.XdripPlusDisplay -> XdripPlusDisplayScreen(onNavigate)
        SettingsScreen.XdripPlusFont -> FontSettingsScreen()
        SettingsScreen.XdripPlusLanguage -> LanguageSettingsScreen()
        SettingsScreen.XdripPlusGraphDisplay -> GraphDisplayScreen(onNavigate)
        SettingsScreen.XdripPlusGraphSmoothing -> GraphSmoothingScreen()
        SettingsScreen.XdripPlusYAxis -> YAxisScreen()
        SettingsScreen.XdripPlusAccessibility -> AccessibilityScreen()
        SettingsScreen.XdripPlusNumberWall -> NumberWallScreen()
        SettingsScreen.XdripPlusNumberIcon -> NumberIconScreen()
        SettingsScreen.XdripPlusCopying -> CopyingSettingsScreen()
        SettingsScreen.XdripPlusUpdate -> UpdateSettingsScreen()
        SettingsScreen.XdripPlusMotion -> MotionSettingsScreen()
        SettingsScreen.XdripPlusPens -> PensScreen(onNavigate)
        SettingsScreen.XdripPlusNovopen -> NovopenScreen()
        SettingsScreen.XdripPlusInpen -> InpenScreen()
        SettingsScreen.XdripPlusPendiq -> PendiqScreen()
        SettingsScreen.XdripPlusPrediction -> PredictionSettingsScreen(onNavigate)
        SettingsScreen.XdripPlusMultipleInsulin -> MultipleInsulinScreen()
        SettingsScreen.XdripPlusAdvPredict -> AdvPredictScreen()
        SettingsScreen.XdripPlusSync -> SyncSettingsScreen(onNavigate)
        SettingsScreen.XdripPlusRemoteSnooze -> RemoteSnoozeScreen()
        SettingsScreen.XdripPlusDesertSync -> DesertSyncScreen()
    }
}

@Composable
private fun RootScreen(onNavigate: (SettingsScreen) -> Unit, onOpenClassic: () -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    var query by remember { mutableStateOf("") }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        singleLine = true,
        placeholder = { Text("Search settings") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("setting_search"),
    )

    if (query.isNotBlank()) {
        val results = searchSettings(query)
        if (results.isEmpty()) {
            SettingsCategory("No matches") {
                SettingsActionRow(
                    title = "No migrated setting matches \"$query\"",
                    subtitle = "Open classic settings to search everything",
                    onClick = onOpenClassic,
                )
            }
        } else {
            SettingsCategory("Results") {
                results.forEach { entry ->
                    SettingsActionRow(title = entry.title, onClick = { onNavigate(entry.screen) })
                }
            }
        }
        return
    }

    SettingsCategory(context.getString(R.string.general_settings)) {
        SettingsActionRow(
            title = context.getString(R.string.glucose_units),
            subtitle = context.getString(R.string.mmol_or_mgdl_high_and_low),
            onClick = { onNavigate(SettingsScreen.Units) },
            modifier = Modifier.testTag("setting_glucose_units"),
        )
        SettingsActionRow(
            title = context.getString(R.string.theme_colors),
            subtitle = context.getString(R.string.theme_colors_summary),
            onClick = { onNavigate(SettingsScreen.Theme) },
            modifier = Modifier.testTag("setting_theme"),
        )
    }
    SettingsCategory(context.getString(R.string.alerts_and_notifications)) {
        SettingsActionRow(
            title = context.getString(R.string.alarms_and_alerts),
            subtitle = context.getString(R.string.glucose_calibration_and_other_alerts),
            onClick = { onNavigate(SettingsScreen.Notifications) },
            modifier = Modifier.testTag("setting_notifications"),
        )
    }
    SettingsCategory(context.getString(R.string.data_source_settings)) {
        SettingsActionRow(
            title = context.getString(R.string.hardware_data_source),
            subtitle = context.getString(R.string.how_receive_data),
            onClick = { onNavigate(SettingsScreen.DataSource) },
            modifier = Modifier.testTag("setting_data_source"),
        )
    }
    SettingsCategory(context.getString(R.string.data_sync)) {
        SettingsActionRow(
            title = context.getString(R.string.cloud_upload),
            subtitle = context.getString(R.string.options_for_upload),
            onClick = { onNavigate(SettingsScreen.DataSync) },
            modifier = Modifier.testTag("setting_data_sync"),
        )
    }
    SettingsCategory(context.getString(R.string.smart_watch_features)) {
        SettingsActionRow(
            title = context.getString(R.string.smart_watch_features),
            subtitle = context.getString(R.string.pebble_and_android_wear_options),
            onClick = { onNavigate(SettingsScreen.SmartWatchOptions) },
            modifier = Modifier.testTag("setting_smart_watch"),
        )
    }
    SettingsCategory(context.getString(R.string.xdrip_plus_extra_settings)) {
        SettingsActionRow(
            title = context.getString(R.string.xdrip_plus_display_settings),
            subtitle = context.getString(R.string.display_customisations),
            onClick = { onNavigate(SettingsScreen.XdripPlusDisplay) },
            modifier = Modifier.testTag("setting_xdrip_display"),
        )
        SettingsActionRow(
            title = context.getString(R.string.xdrip_plus_prediction_settings),
            subtitle = context.getString(R.string.insulin_carb_ratios_etc_for_models),
            enabled = state.bool("I_understand", false),
            onClick = { onNavigate(SettingsScreen.XdripPlusPrediction) },
            modifier = Modifier.testTag("setting_xdrip_prediction"),
        )
        SettingsActionRow(
            title = context.getString(R.string.xdrip_plus_sync_settings),
            subtitle = context.getString(R.string.settings_for_syncing),
            enabled = state.bool("I_understand", false),
            onClick = { onNavigate(SettingsScreen.XdripPlusSync) },
            modifier = Modifier.testTag("setting_xdrip_sync"),
        )
        SettingsActionRow(
            title = context.getString(R.string.xdrip_motion_tracking),
            subtitle = context.getString(R.string.movement_detection_and_vehicle_mode),
            onClick = { onNavigate(SettingsScreen.XdripPlusMotion) },
            modifier = Modifier.testTag("setting_xdrip_motion"),
        )
        SettingsActionRow(
            title = context.getString(R.string.xdrip_plus_update_settings),
            subtitle = context.getString(R.string.automatic_updates_crash_reports_and_feedback),
            onClick = { onNavigate(SettingsScreen.XdripPlusUpdate) },
            modifier = Modifier.testTag("setting_xdrip_update"),
        )
        SettingsActionRow(
            title = context.getString(R.string.copying_settings),
            onClick = { onNavigate(SettingsScreen.XdripPlusCopying) },
            modifier = Modifier.testTag("setting_xdrip_copying"),
        )
        SettingsActionRow(
            title = context.getString(R.string.insulin_pens),
            onClick = { onNavigate(SettingsScreen.XdripPlusPens) },
            modifier = Modifier.testTag("setting_xdrip_pens"),
        )
    }
    SettingsCategory(context.getString(R.string.other_settings)) {
        SettingsActionRow(
            title = context.getString(R.string.speak_readings),
            onClick = { onNavigate(SettingsScreen.SpeakReadings) },
            modifier = Modifier.testTag("setting_speak_readings"),
        )
        SettingsActionRow(
            title = context.getString(R.string.interapp_settings),
            onClick = { onNavigate(SettingsScreen.InterApp) },
            modifier = Modifier.testTag("setting_interapp"),
        )
        SettingsActionRow(
            title = context.getString(R.string.less_common_settings),
            onClick = { onNavigate(SettingsScreen.LessCommon) },
            modifier = Modifier.testTag("setting_less_common"),
        )
    }
    SettingsCategory("About") {
        SettingsActionRow(
            title = context.getString(R.string.end_user_license_agreement),
            subtitle = context.getString(R.string.not_for_medical_use),
            onClick = { context.startActivity(Intent(context, com.eveningoutpost.dexdrip.LicenseAgreementActivity::class.java)) },
            modifier = Modifier.testTag("setting_license"),
        )
        SettingsActionRow(
            title = "Classic settings",
            subtitle = "Screens not yet migrated to the new UI",
            onClick = onOpenClassic,
            modifier = Modifier.testTag("setting_classic"),
        )
    }
}

@Composable
private fun UnitsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val colors = LocalXdripColors.current
    val entries = remember { context.resources.getStringArray(R.array.bgUnitEntries).toList() }
    val values = remember { context.resources.getStringArray(R.array.bgUnitValues).toList() }

    SettingsCategory(context.getString(R.string.general_settings)) {
        SettingsListRow(
            title = context.getString(R.string.glucose_units),
            entries = entries,
            values = values,
            selectedValue = state.string("units", SettingsPrefs.UNIT_MGDL),
            onSelected = { state.setString("units", it) },
            modifier = Modifier.testTag("setting_units"),
        )
        SettingsEditTextRow(
            title = context.getString(R.string.high_value),
            subtitle = context.getString(R.string.maximum_value),
            value = state.string("highValue", "170"),
            valueColor = colors.highValues,
            numeric = true,
            onValueChange = { state.setString("highValue", it) },
            modifier = Modifier.testTag("setting_highValue"),
        )
        SettingsEditTextRow(
            title = context.getString(R.string.low_value),
            subtitle = context.getString(R.string.minimum_value),
            value = state.string("lowValue", "70"),
            valueColor = colors.lowValues,
            numeric = true,
            onValueChange = { state.setString("lowValue", it) },
            modifier = Modifier.testTag("setting_lowValue"),
        )
    }
}

@Composable
private fun NotificationsScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    SettingsCategory(context.getString(R.string.alerts_and_notifications)) {
        SettingsActionRow(
            title = context.getString(R.string.glucose_level_alerts_list),
            onClick = { context.startActivity(Intent(context, AlertList::class.java)) },
            modifier = Modifier.testTag("setting_alert_list"),
        )
        SettingsActionRow(title = context.getString(R.string.glucose_alerts_settings), onClick = { onNavigate(SettingsScreen.BgAlerts) })
        SettingsActionRow(title = context.getString(R.string.persistent_high_alert), onClick = { onNavigate(SettingsScreen.PersistentHigh) })
        SettingsActionRow(title = context.getString(R.string.forecasted_low_alert), onClick = { onNavigate(SettingsScreen.ForecastLow) })
        SettingsActionRow(title = context.getString(R.string.title_sens_expiry), onClick = { onNavigate(SettingsScreen.SensorExpiry) })
        SettingsActionRow(title = context.getString(R.string.calibration_alerts), onClick = { onNavigate(SettingsScreen.CalibrationAlerts) })
        SettingsActionRow(
            title = context.getString(R.string.missed_reading_alert),
            onClick = { context.startActivity(Intent(context, MissedReadingActivity::class.java)) },
        )
        SettingsActionRow(title = context.getString(R.string.other_alerts), onClick = { onNavigate(SettingsScreen.OtherAlerts) })
    }
}

@Composable
private fun BgAlertsScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.alerts_and_notifications)) {
        ListPref(
            state = state,
            key = "bg_alert_profile",
            title = context.getString(R.string.alert_volume_profile),
            entries = context.resources.getStringArray(R.array.BgAlertProfileEntries).toList(),
            values = context.resources.getStringArray(R.array.BgAlertProfileValues).toList(),
            default = "High",
            tag = "setting_bg_alert_profile",
        )
        ListPref(
            state = state,
            key = "alert_audio_focus",
            title = context.getString(R.string.audio_focus),
            subtitle = context.getString(R.string.audio_focus_summary),
            entries = context.resources.getStringArray(R.array.AudioFocus).toList(),
            values = context.resources.getStringArray(R.array.AudioFocusValues).toList(),
            default = "AUDIOFOCUS_NONE",
        )
        SwitchPref(state, "smart_snoozing", context.getString(R.string.smart_snoozing), default = true, subtitle = context.getString(R.string.keep_snoozing_if_glucose_is_heading_in_right_direction))
        SwitchPref(state, "smart_alerting", context.getString(R.string.smart_alerting), default = true, subtitle = context.getString(R.string.dont_alert_if_glucose_in_right_direction))
        SwitchPref(state, "no_alarms_during_calls", context.getString(R.string.dont_alarm_during_phone_calls), default = true, subtitle = context.getString(R.string.alarms_silenced_during_telephone_calls))
        SwitchPref(state, "buttons_silence_alert", context.getString(R.string.buttons_silence_alarms), default = true, subtitle = context.getString(R.string.volume_buttons_snooze))
        SwitchPref(state, "show_buttons_in_alerts", context.getString(R.string.alert_buttons), default = false, subtitle = context.getString(R.string.show_action_buttons_within_alerts))
        SwitchPref(state, "start_snoozed", context.getString(R.string.start_snoozed), default = false, subtitle = context.getString(R.string.alerts_start_out_snoozed_and_must_persist_for_a_while))
        SwitchPref(state, "wake_phone_during_alerts", context.getString(R.string.wake_up_screen), default = false, subtitle = context.getString(R.string.wake_up_screen_summary))
        SwitchPref(state, "flash_torch_alerts_charging", context.getString(R.string.use_camera_light), default = false, subtitle = context.getString(R.string.use_camera_light_summary))
        SwitchPref(state, "bg_alerts_from_main_menu", context.getString(R.string.shortcut_to_bg_alerts), default = false, subtitle = context.getString(R.string.create_shortcut))
        SettingsActionRow(title = context.getString(R.string.suppress_alerts_if_missed_readings), onClick = { onNavigate(SettingsScreen.SuppressAlerts) })
        SettingsActionRow(title = context.getString(R.string.title_use_notification_channels), onClick = { onNavigate(SettingsScreen.NotificationChannels) })
        SettingsActionRow(title = context.getString(R.string.title_ascending_volume), onClick = { onNavigate(SettingsScreen.AscendingVolume) })
    }
}

@Composable
private fun SuppressAlertsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.suppress_alerts_if_missed_readings)) {
        SwitchPref(state, "disable_alerts_stale_data", context.getString(R.string.suppress_alerts_if_missed_readings), default = false, subtitle = context.getString(R.string.suppress_alerts_missed_readings), tag = "setting_stale_enabled")
        EditPref(
            state = state,
            key = "disable_alerts_stale_data_minutes",
            title = context.getString(R.string.suppress_alerts_after),
            subtitle = context.getString(R.string.suppress_alerts_after),
            default = "15",
            numeric = true,
            enabled = state.dependentEnabled("disable_alerts_stale_data", false),
            tag = "setting_stale_minutes",
        )
    }
}

@Composable
private fun NotificationChannelsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.title_use_notification_channels)) {
        SwitchPref(
            state = state,
            key = "ongoing_notification_aodchipstyle",
            title = "Use AOD chip style",
            default = false,
            subtitle = "Display notification chip and lockscreen notification. Android 16+ only",
        )
    }
}

@Composable
private fun AscendingVolumeScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.title_ascending_volume)) {
        SwitchPref(state, "delay_ascending_3min", context.getString(R.string.title_delay_ascending_3min), default = true, subtitle = context.getString(R.string.summary_delay_ascending_3min))
        SwitchPref(state, "ascending_volume_to_medium", context.getString(R.string.title_ascending_volume_to_medium), default = false, subtitle = context.getString(R.string.summary_ascending_volume_to_medium))
    }
}

@Composable
private fun PersistentHighScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.persistent_high_alert)) {
        SwitchPref(state, "persistent_high_alert_enabled", context.getString(R.string.persistent_high_alert_enable), default = false, subtitle = context.getString(R.string.alarm_if_above_high_value))
        SwitchPref(state, "high_value_is_persistent_high_threshold", context.getString(R.string.title_persistent_high_threshold_link), default = true, subtitle = context.getString(R.string.summary_persistent_high_threshold_link))
        EditPref(
            state = state,
            key = "persistent_high_threshold",
            title = context.getString(R.string.title_persistent_high_threshold),
            subtitle = SettingsPrefs.unitizedSummary(state.string("persistent_high_threshold", "170")),
            default = "170",
            numeric = true,
            enabled = state.dependentEnabled("high_value_is_persistent_high_threshold", true, disableDependentsState = true),
            tag = "setting_persistent_high_threshold",
            validate = glucoseInputValidator(context),
        )
        EditPref(
            state = state,
            key = "persistent_high_threshold_mins",
            title = context.getString(R.string.for_longer_than_minutes),
            default = "60",
            numeric = true,
            enabled = state.dependentEnabled("persistent_high_alert_enabled", false),
            tag = "setting_persistent_high_mins",
        )
        EditPref(
            state = state,
            key = "persistent_high_repeat_mins",
            title = context.getString(R.string.persistent_repeat_max),
            default = "20",
            numeric = true,
            enabled = state.dependentEnabled("persistent_high_alert_enabled", false),
        )
        RingtonePref(
            state = state,
            key = "persistent_high_alert_sound",
            title = context.getString(R.string.persistent_high_sound),
            subtitle = context.getString(R.string.choose_sound_used_for_persistent_high_alarm),
            enabled = state.dependentEnabled("persistent_high_alert_enabled", false),
        )
        SwitchPref(state, "persistent_high_alert_override_silent", context.getString(R.string.override_silent_mode), default = false)
        SwitchPref(state, "persistent_high_alert_vibrate_on_alert", context.getString(R.string.vibrate_on_alert), default = true)
    }
}

@Composable
private fun ForecastLowScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.forecasted_low_alert)) {
        SwitchPref(state, "predict_lows", context.getString(R.string.forecast_lows), default = true, subtitle = context.getString(R.string.extrapolate_data_to_try_to_predict_lows))
        SwitchPref(state, "predict_lows_alarm", context.getString(R.string.raise_alarm_on_forecast_low), default = false, subtitle = context.getString(R.string.notify_when_predicted_low_time_reaches_threshold), enabled = state.dependentEnabled("predict_lows", true))
        SwitchPref(state, "low_value_is_forecast_low_threshold", context.getString(R.string.title_forecast_low_threshold_link), default = true, subtitle = context.getString(R.string.summary_forecast_low_threshold_link))
        EditPref(
            state = state,
            key = "forecast_low_threshold",
            title = context.getString(R.string.title_forecast_low_threshold),
            subtitle = SettingsPrefs.unitizedSummary(state.string("forecast_low_threshold", "70")),
            default = "70",
            numeric = true,
            enabled = state.dependentEnabled("low_value_is_forecast_low_threshold", true, disableDependentsState = true),
            tag = "setting_forecast_low_threshold",
            validate = glucoseInputValidator(context),
        )
        EditPref(
            state = state,
            key = "low_predict_alarm_level",
            title = context.getString(R.string.alarm_at_forecasted_low_mins),
            default = "40",
            numeric = true,
            enabled = state.dependentEnabled("predict_lows_alarm", false),
        )
        RingtonePref(
            state = state,
            key = "bg_predict_alert_sound",
            title = context.getString(R.string.predicted_low_sound),
            subtitle = context.getString(R.string.choose_sound_used_for_predicted_low_alarm),
            enabled = state.dependentEnabled("predict_lows", true),
        )
        SwitchPref(state, "bg_predict_alert_override_silent", context.getString(R.string.override_silent_mode), default = false)
        SwitchPref(state, "bg_predict_alert_vibrate_on_alert", context.getString(R.string.vibrate_on_alert), default = true)
    }
}

@Composable
private fun SensorExpiryScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.title_sens_expiry)) {
        SwitchPref(state, "alert_raise_for_sensor_expiry", context.getString(R.string.title_sens_expiry_notify), default = false, subtitle = context.getString(R.string.summary_sens_expiry_notify))
    }
}

@Composable
private fun CalibrationAlertsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.calibration_alerts)) {
        SettingsSwitchRow(
            title = context.getString(R.string.calibration_alerts),
            subtitle = context.getString(R.string.alert_calibration_request),
            checked = state.bool("calibration_notifications", false),
            onCheckedChange = {
                state.setBool("calibration_notifications", it)
                UserNotification.lastCalibrationAlert()?.delete()
            },
            modifier = Modifier.testTag("setting_calibration_notifications"),
        )
        EditPref(
            state = state,
            key = "calibration_reminder_hours",
            title = context.getString(R.string.hours_between_calibrations),
            subtitle = context.getString(R.string.calibrations_request_time_time_difference),
            default = "24",
            numeric = true,
            enabled = state.dependentEnabled("calibration_notifications", false),
        )
        RingtonePref(
            state = state,
            key = "calibration_notification_sound",
            title = context.getString(R.string.calibration_request_sound),
            subtitle = context.getString(R.string.calibrations_sound),
            enabled = state.dependentEnabled("calibration_notifications", false),
        )
        SwitchPref(state, "calibration_alerts_override_silent", context.getString(R.string.override_silent_mode), default = true, enabled = state.dependentEnabled("calibration_notifications", false))
        SwitchPref(state, "calibration_alerts_while_charging", context.getString(R.string.even_when_charging), default = true, subtitle = context.getString(R.string.no_calibration_requests_charging), enabled = state.dependentEnabled("calibration_notifications", false))
        SwitchPref(state, "calibration_alerts_repeat", context.getString(R.string.repeat_alerts), default = true, subtitle = context.getString(R.string.keep_alert_no_calibration), enabled = state.dependentEnabled("calibration_notifications", false))
        EditPref(
            state = state,
            key = "calibration_snooze",
            title = context.getString(R.string.alert_repeat_minutes),
            subtitle = context.getString(R.string.calibration_minutes_reraise),
            default = "20",
            numeric = true,
            enabled = state.dependentEnabled("calibration_alerts_repeat", true),
        )
        SwitchPref(state, "play_sound_for_initial_calibration", context.getString(R.string.title_play_sound_for_initial_calibration), default = true, subtitle = context.getString(R.string.summary_play_sound_for_initial_calibration))
    }
}

@Composable
private fun OtherAlertsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val risingEntries = context.resources.getStringArray(R.array.risingEntries).toList()
    val risingValues = context.resources.getStringArray(R.array.risingValues).toList()

    SettingsCategory(context.getString(R.string.category_noisy_readings)) {
        SwitchPref(state, "bg_unclear_readings_alerts", context.getString(R.string.bad_noisy_value_alerts), default = false)
        EditPref(
            state = state,
            key = "bg_unclear_readings_minutes",
            title = context.getString(R.string.alert_noisy_values),
            default = "90",
            numeric = true,
            enabled = state.dependentEnabled("bg_unclear_readings_alerts", false),
        )
        EditPref(
            state = state,
            key = "bg_unclear_readings_alert_snooze",
            title = context.getString(R.string.alert_snooze),
            subtitle = context.getString(R.string.alert_minutes_reraise),
            default = "20",
            numeric = true,
            enabled = state.dependentEnabled("bg_unclear_readings_alerts", false),
        )
        SwitchPref(state, "bg_unclear_readings_alert_enable_alerts_reraise", context.getString(R.string.reraise_before_snooze), default = false, subtitle = context.getString(R.string.reraise_not_snoozed_sooner), enabled = state.dependentEnabled("bg_unclear_readings_alerts", false))
        EditPref(
            state = state,
            key = "bg_unclear_readings_alert_reraise_sec",
            title = context.getString(R.string.alert_reraise_time),
            subtitle = context.getString(R.string.alert_seconds_reraise),
            default = "60",
            numeric = true,
            enabled = state.dependentEnabled("bg_unclear_readings_alert_enable_alerts_reraise", false),
        )
    }
    SettingsCategory(context.getString(R.string.category_falling_rising_bg)) {
        SwitchPref(state, "falling_alert", context.getString(R.string.bg_falling_fast), default = false)
        ListPref(state, "falling_bg_val", context.getString(R.string.falling_threshold), risingEntries, risingValues, "3", enabled = state.dependentEnabled("falling_alert", false))
        SwitchPref(state, "rising_alert", context.getString(R.string.bg_rising_fast), default = false)
        ListPref(state, "rising_bg_val", context.getString(R.string.rising_threshold), risingEntries, risingValues, "3", enabled = state.dependentEnabled("rising_alert", false))
    }
    SettingsCategory(context.getString(R.string.category_alert_prefs)) {
        RingtonePref(state, "other_alerts_sound", context.getString(R.string.alert_sound), subtitle = context.getString(R.string.set_sound_for_bg_alerts))
        SwitchPref(state, "other_alerts_override_silent", context.getString(R.string.override_silent_mode_these), default = false)
        SwitchPref(state, "other_alerts_vibrate_on_alert", context.getString(R.string.vibrate_on_alert), default = true)
    }
}

@Composable
internal fun SwitchPref(
    state: SettingsState,
    key: String,
    title: String,
    default: Boolean = false,
    subtitle: String? = null,
    enabled: Boolean = true,
    tag: String? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onBeforeChange: ((Boolean) -> Boolean)? = null,
) {
    SettingsSwitchRow(
        title = title,
        subtitle = subtitle,
        checked = state.bool(key, default),
        onCheckedChange = { checked ->
            if (onCheckedChange != null) onCheckedChange(checked) else state.setBool(key, checked)
        },
        enabled = enabled,
        onBeforeChange = onBeforeChange,
        modifier = tag?.let { Modifier.testTag(it) } ?: Modifier,
    )
}

@Composable
internal fun EditPref(
    state: SettingsState,
    key: String,
    title: String,
    default: String,
    subtitle: String? = null,
    numeric: Boolean = false,
    decimal: Boolean = false,
    masked: Boolean = false,
    enabled: Boolean = true,
    tag: String? = null,
    validate: ((String) -> Boolean)? = null,
    valueColor: Color = Color.Unspecified,
    maxLength: Int? = null,
    onValueChange: ((String) -> Unit)? = null,
) {
    SettingsEditTextRow(
        title = title,
        subtitle = subtitle,
        value = state.string(key, default),
        numeric = numeric,
        decimal = decimal,
        masked = masked,
        enabled = enabled,
        valueColor = valueColor,
        maxLength = maxLength,
        onValueChange = { newValue ->
            when {
                onValueChange != null -> onValueChange(newValue)
                validate == null || validate(newValue) -> state.setString(key, newValue)
            }
        },
        modifier = tag?.let { Modifier.testTag(it) } ?: Modifier,
    )
}

@Composable
internal fun ListPref(
    state: SettingsState,
    key: String,
    title: String,
    entries: List<String>,
    values: List<String>,
    default: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    tag: String? = null,
    onSelected: ((String) -> Unit)? = null,
) {
    SettingsListRow(
        title = title,
        subtitle = subtitle,
        entries = entries,
        values = values,
        selectedValue = state.string(key, default),
        onSelected = { newValue ->
            if (onSelected != null) onSelected(newValue) else state.setString(key, newValue)
        },
        enabled = enabled,
        modifier = tag?.let { Modifier.testTag(it) } ?: Modifier,
    )
}

@Composable
internal fun RingtonePref(
    state: SettingsState,
    key: String,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    tag: String? = null,
) {
    SettingsRingtoneRow(
        title = title,
        subtitle = subtitle,
        value = state.string(key, "default"),
        onPicked = { state.setString(key, it) },
        enabled = enabled,
        modifier = tag?.let { Modifier.testTag(it) } ?: Modifier,
    )
}

private fun glucoseInputValidator(context: android.content.Context): (String) -> Boolean = { value ->
    val valid = SettingsPrefs.isValidGlucoseInput(value)
    if (!valid) {
        val doMgdl = SettingsPrefs.isMgdl()
        JoH.static_toast_long(
            context.getString(
                R.string.the_value_must_be_between_min_and_max,
                EditAlertActivity.unitsConvert2Disp(doMgdl, com.eveningoutpost.dexdrip.utils.Preferences.MIN_GLUCOSE_INPUT),
                EditAlertActivity.unitsConvert2Disp(doMgdl, com.eveningoutpost.dexdrip.utils.Preferences.MAX_GLUCOSE_INPUT),
            )
        )
    }
    valid
}
