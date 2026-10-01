package com.eveningoutpost.dexdrip.ui.settings

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SendToMobile
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Subject
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.AlertList
import com.eveningoutpost.dexdrip.EditAlertActivity
import com.eveningoutpost.dexdrip.EventLogActivity
import com.eveningoutpost.dexdrip.GcmActivity
import com.eveningoutpost.dexdrip.Home
import com.eveningoutpost.dexdrip.MissedReadingActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.Reminders
import com.eveningoutpost.dexdrip.eassist.EmergencyAssistActivity
import com.eveningoutpost.dexdrip.models.BgReading
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.models.Profile
import com.eveningoutpost.dexdrip.models.UserNotification
import com.eveningoutpost.dexdrip.profileeditor.BasalProfileEditor
import com.eveningoutpost.dexdrip.profileeditor.ProfileEditor
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.LibreTrendGraph
import com.eveningoutpost.dexdrip.utils.TestFeature
import com.eveningoutpost.dexdrip.wearintegration.Amazfitservice
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService
import com.eveningoutpost.dexdrip.ui.theme.LocalXdripColors
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

@Composable
internal fun titleFor(screen: SettingsScreen): String = when (screen) {
    SettingsScreen.Root -> "Settings"
    SettingsScreen.GeneralCategory -> stringResource(R.string.general_settings)
    SettingsScreen.AlarmsCategory -> stringResource(R.string.alarms_and_alerts)
    SettingsScreen.YourDataCategory -> "Your Data"
    SettingsScreen.ProfileCategory -> "Profile"
    SettingsScreen.DevicesCategory -> "Devices"
    SettingsScreen.AppearanceCategory -> "Appearance"
    SettingsScreen.AccessibilityCategory -> "Accessibility"
    SettingsScreen.AdvancedCategory -> "Advanced"
    SettingsScreen.Units -> stringResource(R.string.glucose_units)
    SettingsScreen.Theme -> stringResource(R.string.theme_colors)
    SettingsScreen.NotificationStyle -> "Notification Style"
    SettingsScreen.BgAlerts -> stringResource(R.string.glucose_alerts_settings)
    SettingsScreen.SuppressAlerts -> stringResource(R.string.suppress_alerts_if_missed_readings)
    SettingsScreen.AscendingVolume -> stringResource(R.string.title_ascending_volume)
    SettingsScreen.PersistentHigh -> stringResource(R.string.persistent_high_alert)
    SettingsScreen.ForecastLow -> stringResource(R.string.forecasted_low_alert)
    SettingsScreen.SensorExpiry -> stringResource(R.string.title_sens_expiry)
    SettingsScreen.CalibrationAlerts -> stringResource(R.string.calibration_alerts)
    SettingsScreen.OtherAlerts -> stringResource(R.string.other_alerts)
    SettingsScreen.DataSource -> stringResource(R.string.data_source_settings)
    SettingsScreen.DexcomDevice -> "Dexcom"
    SettingsScreen.LibreDevice -> "Libre / NFC"
    SettingsScreen.MedtrumDevice -> "Medtrum"
    SettingsScreen.ILetDevice -> "iLet"
    SettingsScreen.InsulinPumps -> "Insulin Pumps"
    SettingsScreen.BluetoothBridge -> "Bluetooth Bridge"
    SettingsScreen.WebFollow -> "Web Follower Settings"
    SettingsScreen.NfcSettings -> stringResource(R.string.nfc_scan_features)
    SettingsScreen.NsFollowDownload -> stringResource(R.string.title_nsfollow_download_treatments)
    SettingsScreen.G5Debug -> stringResource(R.string.g5_debug_settings)
    SettingsScreen.PreemptiveRestart -> stringResource(R.string.title_ob1_g5_preemptive_restart)
    SettingsScreen.DataSync -> stringResource(R.string.data_sync)
    SettingsScreen.Backups -> "Backups"
    SettingsScreen.About -> "About"
    SettingsScreen.Version -> "Version"
    SettingsScreen.HomeScreen -> "Home Screen"
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
        SettingsScreen.GeneralCategory -> GeneralCategoryScreen(onNavigate)
        SettingsScreen.AlarmsCategory -> AlarmsCategoryScreen(onNavigate)
        SettingsScreen.YourDataCategory -> YourDataCategoryScreen(onNavigate)
        SettingsScreen.ProfileCategory -> ProfileCategoryScreen(onNavigate)
        SettingsScreen.DevicesCategory -> DevicesCategoryScreen(onNavigate)
        SettingsScreen.AppearanceCategory -> AppearanceCategoryScreen(onNavigate)
        SettingsScreen.AccessibilityCategory -> AccessibilityCategoryScreen(onNavigate)
        SettingsScreen.AdvancedCategory -> AdvancedCategoryScreen(onNavigate)
        SettingsScreen.Units -> UnitsScreen()
        SettingsScreen.Theme -> ThemeEditorScreen()
        SettingsScreen.NotificationStyle -> NotificationStyleScreen()
        SettingsScreen.BgAlerts -> BgAlertsScreen(onNavigate)
        SettingsScreen.SuppressAlerts -> SuppressAlertsScreen()
        SettingsScreen.AscendingVolume -> AscendingVolumeScreen()
        SettingsScreen.PersistentHigh -> PersistentHighScreen()
        SettingsScreen.ForecastLow -> ForecastLowScreen()
        SettingsScreen.SensorExpiry -> SensorExpiryScreen()
        SettingsScreen.CalibrationAlerts -> CalibrationAlertsScreen()
        SettingsScreen.OtherAlerts -> OtherAlertsScreen()
        SettingsScreen.DataSource -> DataSourceScreen(onNavigate)
        SettingsScreen.DexcomDevice -> DexcomDeviceScreen(onNavigate)
        SettingsScreen.LibreDevice -> LibreDeviceScreen(onNavigate)
        SettingsScreen.MedtrumDevice -> MedtrumDeviceScreen()
        SettingsScreen.ILetDevice -> ILetDeviceScreen()
        SettingsScreen.InsulinPumps -> InsulinPumpsScreen(onNavigate)
        SettingsScreen.BluetoothBridge -> BluetoothBridgeScreen()
        SettingsScreen.WebFollow -> WebFollowScreen()
        SettingsScreen.NfcSettings -> NfcSettingsScreen()
        SettingsScreen.NsFollowDownload -> NsFollowDownloadScreen()
        SettingsScreen.G5Debug -> G5DebugScreen(onNavigate)
        SettingsScreen.PreemptiveRestart -> PreemptiveRestartScreen()
        SettingsScreen.DataSync -> DataSyncScreen(onNavigate)
        SettingsScreen.Backups -> BackupsScreen()
        SettingsScreen.About -> AboutScreen(onNavigate, onOpenClassic)
        SettingsScreen.Version -> VersionScreen()
        SettingsScreen.HomeScreen -> HomeScreenSettingsScreen()
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
        SettingsScreen.XdripPlusPrediction -> PredictionSettingsScreen()
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

    SettingsCategoryButton(
        title = context.getString(R.string.general_settings),
        subtitle = context.getString(R.string.mmol_or_mgdl_high_and_low),
        icon = Icons.Outlined.Tune,
        onClick = { onNavigate(SettingsScreen.GeneralCategory) },
        modifier = Modifier.testTag("setting_category_general"),
    )
    SettingsCategoryButton(
        title = context.getString(R.string.alarms_and_alerts),
        subtitle = context.getString(R.string.glucose_calibration_and_other_alerts),
        icon = Icons.Outlined.NotificationsActive,
        onClick = { onNavigate(SettingsScreen.AlarmsCategory) },
        modifier = Modifier.testTag("setting_category_alarms"),
    )
    SettingsCategoryButton(
        title = "Your Data",
        subtitle = "Cloud sync, backups and xDrip+ sync",
        icon = Icons.Outlined.CloudSync,
        onClick = { onNavigate(SettingsScreen.YourDataCategory) },
        modifier = Modifier.testTag("setting_category_data"),
    )
    SettingsCategoryButton(
        title = "Profile",
        subtitle = "Insulin, carb ratios and prediction",
        icon = Icons.Outlined.Person,
        onClick = { onNavigate(SettingsScreen.ProfileCategory) },
        modifier = Modifier.testTag("setting_category_profile"),
    )
    SettingsCategoryButton(
        title = "Devices",
        subtitle = "Data sources, meters, pens and watches",
        icon = Icons.Outlined.Devices,
        onClick = { onNavigate(SettingsScreen.DevicesCategory) },
        modifier = Modifier.testTag("setting_category_devices"),
    )
    SettingsCategoryButton(
        title = "Appearance",
        subtitle = "Theme, display, graph and home screen",
        icon = Icons.Outlined.Palette,
        onClick = { onNavigate(SettingsScreen.AppearanceCategory) },
        modifier = Modifier.testTag("setting_category_appearance"),
    )
    SettingsCategoryButton(
        title = "Accessibility",
        subtitle = "Spoken readings and always-on display",
        icon = Icons.Outlined.Accessibility,
        onClick = { onNavigate(SettingsScreen.AccessibilityCategory) },
        modifier = Modifier.testTag("setting_category_accessibility"),
    )
    SettingsCategoryButton(
        title = "Advanced",
        subtitle = "Bluetooth, logging, interop and maintenance",
        icon = Icons.Outlined.Settings,
        onClick = { onNavigate(SettingsScreen.AdvancedCategory) },
        modifier = Modifier.testTag("setting_category_advanced"),
    )
    SettingsCategoryButton(
        title = "About",
        subtitle = "Version, help, updates and licence",
        icon = Icons.Outlined.Info,
        onClick = { onNavigate(SettingsScreen.About) },
        modifier = Modifier.testTag("setting_category_about"),
    )
}

@Composable
private fun GeneralCategoryScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    SettingsCategory(context.getString(R.string.general_settings)) {
        SettingsActionRow(
            title = context.getString(R.string.glucose_units),
            subtitle = context.getString(R.string.mmol_or_mgdl_high_and_low),
            icon = Icons.Outlined.Straighten,
            onClick = { onNavigate(SettingsScreen.Units) },
            modifier = Modifier.testTag("setting_glucose_units"),
        )
    }
}

@Composable
private fun AlarmsCategoryScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    SettingsCategory(context.getString(R.string.alarms_and_alerts)) {
        SettingsActionRow(
            title = context.getString(R.string.glucose_level_alerts_list),
            icon = Icons.Outlined.Notifications,
            onClick = { context.startActivity(Intent(context, AlertList::class.java)) },
            modifier = Modifier.testTag("setting_alert_list"),
        )
        SettingsActionRow(
            title = context.getString(R.string.glucose_alerts_settings),
            icon = Icons.Outlined.NotificationsActive,
            onClick = { onNavigate(SettingsScreen.BgAlerts) },
            modifier = Modifier.testTag("setting_bg_alerts"),
        )
        SettingsActionRow(
            title = context.getString(R.string.persistent_high_alert),
            icon = Icons.Outlined.TrendingUp,
            onClick = { onNavigate(SettingsScreen.PersistentHigh) },
            modifier = Modifier.testTag("setting_persistent_high"),
        )
        SettingsActionRow(
            title = context.getString(R.string.forecasted_low_alert),
            icon = Icons.Outlined.TrendingDown,
            onClick = { onNavigate(SettingsScreen.ForecastLow) },
            modifier = Modifier.testTag("setting_forecast_low"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_sens_expiry),
            icon = Icons.Outlined.Timer,
            onClick = { onNavigate(SettingsScreen.SensorExpiry) },
            modifier = Modifier.testTag("setting_sensor_expiry"),
        )
        SettingsActionRow(
            title = context.getString(R.string.calibration_alerts),
            icon = Icons.Outlined.WaterDrop,
            onClick = { onNavigate(SettingsScreen.CalibrationAlerts) },
            modifier = Modifier.testTag("setting_calibration_alerts"),
        )
        SettingsActionRow(
            title = context.getString(R.string.missed_reading_alert),
            icon = Icons.Outlined.NotificationImportant,
            onClick = { context.startActivity(Intent(context, MissedReadingActivity::class.java)) },
            modifier = Modifier.testTag("setting_missed_reading"),
        )
        SettingsActionRow(
            title = context.getString(R.string.other_alerts),
            icon = Icons.Outlined.Campaign,
            onClick = { onNavigate(SettingsScreen.OtherAlerts) },
            modifier = Modifier.testTag("setting_other_alerts"),
        )
        SettingsActionRow(
            title = context.getString(R.string.reminders),
            icon = Icons.Outlined.Alarm,
            onClick = { context.startActivity(Intent(context, Reminders::class.java)) },
            modifier = Modifier.testTag("setting_reminders"),
        )
        SettingsActionRow(
            title = context.getString(R.string.emergency_messages),
            icon = Icons.Outlined.Emergency,
            onClick = { context.startActivity(Intent(context, EmergencyAssistActivity::class.java)) },
            modifier = Modifier.testTag("setting_emergency_messages"),
        )
        SettingsActionRow(
            title = "Notification Style",
            icon = Icons.Outlined.Style,
            onClick = { onNavigate(SettingsScreen.NotificationStyle) },
            modifier = Modifier.testTag("setting_notification_style"),
        )
    }
}

@Composable
private fun YourDataCategoryScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val engineering = SettingsVisibility.isEngineeringMode(state)
    SettingsCategory("Your Data") {
        SettingsActionRow(
            title = "Cloud sync",
            subtitle = context.getString(R.string.options_for_upload),
            icon = Icons.Outlined.CloudSync,
            onClick = { onNavigate(SettingsScreen.DataSync) },
            modifier = Modifier.testTag("setting_cloud_sync"),
        )
        SettingsActionRow(
            title = context.getString(R.string.copying_settings),
            icon = Icons.Outlined.QrCode,
            onClick = { onNavigate(SettingsScreen.XdripPlusCopying) },
            modifier = Modifier.testTag("setting_xdrip_copying"),
        )
        SettingsActionRow(
            title = "Backups",
            subtitle = "Cloud backup, export and import database, share config",
            icon = Icons.Outlined.Backup,
            onClick = { onNavigate(SettingsScreen.Backups) },
            modifier = Modifier.testTag("setting_backups"),
        )
        SettingsActionRow(
            title = context.getString(R.string.xdrip_plus_sync_settings),
            subtitle = context.getString(R.string.settings_for_syncing),
            icon = Icons.Outlined.Sync,
            enabled = state.bool("I_understand", false),
            onClick = { onNavigate(SettingsScreen.XdripPlusSync) },
            modifier = Modifier.testTag("setting_xdrip_sync"),
        )
        if (engineering) {
            SettingsActionRow(
                title = context.getString(R.string.send_bg_readings_to_backfill),
                icon = Icons.Outlined.Upload,
                onClick = { GcmActivity.syncBGTable2() },
                modifier = Modifier.testTag("setting_resend_backfill"),
            )
        }
    }
}

@Composable
private fun ProfileCategoryScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory("Profile") {
        SettingsActionRow(
            title = context.getString(R.string.title_multiple_insulin_types_settings),
            subtitle = context.getString(R.string.summary_multiple_insulin_types_settings),
            icon = Icons.Outlined.Vaccines,
            enabled = state.bool("I_understand", false),
            onClick = { onNavigate(SettingsScreen.XdripPlusMultipleInsulin) },
            modifier = Modifier.testTag("setting_insulin_types"),
        )
        SettingsActionRow(
            title = context.getString(R.string.carb_ratio),
            subtitle = context.getString(R.string.grams_of_carbohydrate_one_unit_covers),
            icon = Icons.Outlined.Restaurant,
            enabled = state.bool("I_understand", false),
            onClick = { context.startActivity(Intent(context, ProfileEditor::class.java)) },
            modifier = Modifier.testTag("setting_profile_carb_ratio"),
        )
        SettingsActionRow(
            title = context.getString(R.string.insulin_sensitivity),
            subtitle = context.getString(R.string.glucose_drop_for_one_unit),
            icon = Icons.Outlined.MonitorHeart,
            enabled = state.bool("I_understand", false),
            onClick = { context.startActivity(Intent(context, ProfileEditor::class.java)) },
            modifier = Modifier.testTag("setting_profile_insulin_sensitivity"),
        )
        SettingsActionRow(
            title = context.getString(R.string.basal_profile_editor),
            subtitle = context.getString(R.string.graphical_editor_for_pump_basal),
            icon = Icons.Outlined.Schedule,
            enabled = state.bool("I_understand", false),
            onClick = { context.startActivity(Intent(context, BasalProfileEditor::class.java)) },
            modifier = Modifier.testTag("setting_basal_profile_editor"),
        )
        EditPref(
            state = state,
            key = "profile_carb_absorption_default",
            title = context.getString(R.string.carb_absorption_rate),
            default = "35",
            subtitle = context.getString(R.string.linear_model_carbs_absorbed_per_hour),
            enabled = state.bool("I_understand", false),
            tag = "setting_profile_carb_absorption",
            onValueChange = { value ->
                if (SettingsPrefs.isNumeric(value)) {
                    state.setString("profile_carb_absorption_default", value)
                    Profile.reloadPreferences(Pref.getInstance())
                    Home.staticRefreshBGCharts()
                }
            },
        )
        SettingsActionRow(
            title = context.getString(R.string.xdrip_plus_prediction_settings),
            subtitle = context.getString(R.string.insulin_carb_ratios_etc_for_models),
            icon = Icons.Outlined.Insights,
            enabled = state.bool("I_understand", false),
            onClick = { onNavigate(SettingsScreen.XdripPlusPrediction) },
            modifier = Modifier.testTag("setting_xdrip_prediction"),
        )
        SettingsActionRow(
            title = context.getString(R.string.low_prediction_values),
            subtitle = context.getString(R.string.deel_settings_for_algs),
            icon = Icons.Outlined.ShowChart,
            enabled = state.bool("I_understand", false),
            onClick = { onNavigate(SettingsScreen.XdripPlusAdvPredict) },
            modifier = Modifier.testTag("setting_adv_predict"),
        )
    }
}

@Composable
private fun DevicesCategoryScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory("Devices") {
        SettingsActionRow(
            title = context.getString(R.string.hardware_data_source),
            subtitle = context.getString(R.string.how_receive_data),
            icon = Icons.Outlined.Sensors,
            onClick = { onNavigate(SettingsScreen.DataSource) },
            modifier = Modifier.testTag("setting_data_source"),
        )
        SettingsActionRow(
            title = context.getString(R.string.glucose_meters),
            subtitle = context.getString(R.string.glucose_meter_options),
            icon = Icons.Outlined.Bloodtype,
            onClick = { onNavigate(SettingsScreen.GlucoseMeters) },
            modifier = Modifier.testTag("setting_glucose_meters"),
        )
        SettingsActionRow(
            title = context.getString(R.string.insulin_pens),
            icon = Icons.Outlined.Edit,
            onClick = { onNavigate(SettingsScreen.XdripPlusPens) },
            modifier = Modifier.testTag("setting_xdrip_pens"),
        )
        SettingsActionRow(
            title = "Insulin Pumps",
            subtitle = "Select and configure an insulin pump",
            icon = Icons.Outlined.MonitorHeart,
            onClick = { onNavigate(SettingsScreen.InsulinPumps) },
            modifier = Modifier.testTag("setting_insulin_pumps"),
        )
        SettingsActionRow(
            title = context.getString(R.string.smart_watch_features),
            subtitle = context.getString(R.string.pebble_and_android_wear_options),
            icon = Icons.Outlined.Watch,
            onClick = { onNavigate(SettingsScreen.SmartWatchOptions) },
            modifier = Modifier.testTag("setting_smart_watch"),
        )
        if (SettingsVisibility.isBlueReader()) {
            SettingsActionRow(
                title = context.getString(R.string.advanced_bluereader_settings),
                icon = Icons.Outlined.Bluetooth,
                onClick = { onNavigate(SettingsScreen.BlueReaderSettings) },
                modifier = Modifier.testTag("setting_bluereader"),
            )
        }
        if (SettingsVisibility.isLibreReceiver()) {
            SettingsActionRow(
                title = context.getString(R.string.title_advanced_settings_4_Lib2),
                icon = Icons.Outlined.Sensors,
                onClick = { onNavigate(SettingsScreen.Libre2Settings) },
                modifier = Modifier.testTag("setting_libre2"),
            )
        }
        SettingsActionRow(
            title = context.getString(R.string.action_resend_to_watch),
            icon = Icons.Outlined.SendToMobile,
            enabled = state.bool("wear_sync", false) || state.bool("pref_amazfit_enable_key", false),
            onClick = {
                WatchUpdaterService.startServiceAndResendData(0)
                if (state.bool("pref_amazfit_enable_key", false)) {
                    Amazfitservice.start("xDrip_synced_SGV_data")
                }
            },
            modifier = Modifier.testTag("setting_resend_watch"),
        )
        SettingsActionRow(
            title = context.getString(R.string.action_open_watch_setting),
            icon = Icons.Outlined.Watch,
            enabled = state.bool("wear_sync", false),
            onClick = {
                context.startService(
                    Intent(context, WatchUpdaterService::class.java).setAction(WatchUpdaterService.ACTION_OPEN_SETTINGS)
                )
            },
            modifier = Modifier.testTag("setting_open_watch_settings"),
        )
        SettingsActionRow(
            title = context.getString(R.string.action_sync_watch_db),
            icon = Icons.Outlined.Sync,
            enabled = state.bool("wear_sync", false),
            onClick = {
                context.startService(
                    Intent(context, WatchUpdaterService::class.java).setAction(WatchUpdaterService.ACTION_RESET_DB)
                )
            },
            modifier = Modifier.testTag("setting_sync_watch_db"),
        )
    }
}

@Composable
private fun AppearanceCategoryScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    SettingsCategory("Appearance") {
        SettingsActionRow(
            title = context.getString(R.string.theme_colors),
            subtitle = context.getString(R.string.theme_colors_summary),
            icon = Icons.Outlined.Palette,
            onClick = { onNavigate(SettingsScreen.Theme) },
            modifier = Modifier.testTag("setting_theme"),
        )
        SettingsActionRow(
            title = context.getString(R.string.xdrip_plus_display_settings),
            subtitle = context.getString(R.string.display_customisations),
            icon = Icons.Outlined.TextFields,
            onClick = { onNavigate(SettingsScreen.XdripPlusDisplay) },
            modifier = Modifier.testTag("setting_xdrip_display"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_language),
            icon = Icons.Outlined.Language,
            onClick = { onNavigate(SettingsScreen.XdripPlusLanguage) },
            modifier = Modifier.testTag("setting_language"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_xdrip_plus_graph_display_settings),
            subtitle = context.getString(R.string.summary_xdrip_plus_graph_display_settings),
            icon = Icons.Outlined.ShowChart,
            onClick = { onNavigate(SettingsScreen.XdripPlusGraphDisplay) },
            modifier = Modifier.testTag("setting_graph_display"),
        )
        SettingsActionRow(
            title = context.getString(R.string.show_libre_trend),
            icon = Icons.Outlined.Timeline,
            onClick = { context.startActivity(Intent(context, LibreTrendGraph::class.java).putExtra("events", "")) },
            modifier = Modifier.testTag("setting_show_libre_trend"),
        )
        SettingsActionRow(
            title = context.getString(R.string.extra_status_line),
            icon = Icons.Outlined.Subject,
            onClick = { onNavigate(SettingsScreen.ExtraStatusLine) },
            modifier = Modifier.testTag("setting_nav_extra_status_line"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_xdrip_plus_number_wall),
            icon = Icons.Outlined.Wallpaper,
            onClick = { onNavigate(SettingsScreen.XdripPlusNumberWall) },
            modifier = Modifier.testTag("setting_number_wall"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_xdrip_plus_number_icon),
            icon = Icons.Outlined.Apps,
            onClick = { onNavigate(SettingsScreen.XdripPlusNumberIcon) },
            modifier = Modifier.testTag("setting_number_icon"),
        )
        SettingsActionRow(
            title = "Home screen",
            subtitle = "Choose which quick-access buttons and widgets appear on the home screen",
            icon = Icons.Outlined.Home,
            onClick = { onNavigate(SettingsScreen.HomeScreen) },
            modifier = Modifier.testTag("setting_home_screen"),
        )
    }
}

@Composable
private fun AccessibilityCategoryScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    SettingsCategory("Accessibility") {
        SettingsActionRow(
            title = context.getString(R.string.speak_readings),
            icon = Icons.Outlined.VolumeUp,
            onClick = { onNavigate(SettingsScreen.SpeakReadings) },
            modifier = Modifier.testTag("setting_speak_readings"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_xdrip_plus_accessibility),
            icon = Icons.Outlined.Brightness4,
            onClick = { onNavigate(SettingsScreen.XdripPlusAccessibility) },
            modifier = Modifier.testTag("setting_accessibility"),
        )
    }
}

@Composable
private fun AdvancedCategoryScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val engineering = SettingsVisibility.isEngineeringMode(state)
    var confirmDeleteBg by remember { mutableStateOf(false) }
    SettingsCategory("Advanced") {
        SettingsActionRow(
            title = context.getString(R.string.bluetooth_settings),
            icon = Icons.Outlined.Bluetooth,
            onClick = { onNavigate(SettingsScreen.BluetoothSettings) },
            modifier = Modifier.testTag("setting_bluetooth"),
        )
        SettingsActionRow(
            title = context.getString(R.string.view_events_log),
            icon = Icons.Outlined.ListAlt,
            onClick = { context.startActivity(Intent(context, EventLogActivity::class.java).putExtra("events", "")) },
            modifier = Modifier.testTag("setting_events_log"),
        )
        SettingsActionRow(
            title = context.getString(R.string.extra_logging),
            icon = Icons.Outlined.BugReport,
            onClick = { onNavigate(SettingsScreen.LoggingSettings) },
            modifier = Modifier.testTag("setting_logging"),
        )
        SettingsActionRow(
            title = context.getString(R.string.interapp_settings),
            icon = Icons.Outlined.Android,
            onClick = { onNavigate(SettingsScreen.InterApp) },
            modifier = Modifier.testTag("setting_interapp"),
        )
        SettingsActionRow(
            title = context.getString(R.string.title_Other_misc_options),
            icon = Icons.Outlined.MoreHoriz,
            onClick = { onNavigate(SettingsScreen.OtherMiscSettings) },
            modifier = Modifier.testTag("setting_other_options"),
        )
        SettingsActionRow(
            title = context.getString(R.string.advanced_calibration),
            icon = Icons.Outlined.Tune,
            onClick = { onNavigate(SettingsScreen.CalibrationSettings) },
            modifier = Modifier.testTag("setting_calibration"),
        )
        SettingsActionRow(
            title = context.getString(R.string.xdrip_motion_tracking),
            subtitle = context.getString(R.string.movement_detection_and_vehicle_mode),
            icon = Icons.Outlined.DirectionsCar,
            onClick = { onNavigate(SettingsScreen.XdripPlusMotion) },
            modifier = Modifier.testTag("setting_xdrip_motion"),
        )
        SettingsActionRow(
            title = context.getString(R.string.delete_all_bg_readings),
            icon = Icons.Outlined.DeleteForever,
            onClick = { confirmDeleteBg = true },
            modifier = Modifier.testTag("setting_delete_all_bg"),
        )
        SettingsActionRow(
            title = context.getString(R.string.xdrip_plus_update_settings),
            subtitle = context.getString(R.string.automatic_updates_crash_reports_and_feedback),
            icon = Icons.Outlined.SystemUpdate,
            onClick = { onNavigate(SettingsScreen.XdripPlusUpdate) },
            modifier = Modifier.testTag("setting_xdrip_update"),
        )
        if (engineering) {
            SettingsActionRow(
                title = context.getString(R.string.debugging_test_feature),
                icon = Icons.Outlined.BugReport,
                onClick = { TestFeature.testFeature1() },
                modifier = Modifier.testTag("setting_debugging"),
            )
        }
    }

    if (confirmDeleteBg) {
        AlertDialog(
            onDismissRequest = { confirmDeleteBg = false },
            title = { Text(context.getString(R.string.delete_all_bg_readings)) },
            text = { Text("Delete all glucose readings? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteBg = false
                        BgReading.deleteALL()
                        JoH.static_toast_long(context.getString(R.string.deleting_all_bg_readings))
                        Home.staticRefreshBGCharts()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteBg = false }) { Text("Cancel") }
            },
            modifier = Modifier.testTag("setting_delete_all_bg_confirm"),
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
private fun NotificationStyleScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory("Notification Style") {
        SwitchPref(state, "high_priority_notifications", context.getString(R.string.title_high_priority_notifications), default = true, subtitle = context.getString(R.string.summary_high_priority_notifications))
        SwitchPref(state, "public_notifications", context.getString(R.string.title_public_notifications), default = false, subtitle = context.getString(R.string.summary_public_notifications))
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
        SettingsActionRow(title = context.getString(R.string.suppress_alerts_if_missed_readings), icon = Icons.Outlined.Block, onClick = { onNavigate(SettingsScreen.SuppressAlerts) })
        SettingsActionRow(title = context.getString(R.string.title_ascending_volume), icon = Icons.Outlined.VolumeUp, onClick = { onNavigate(SettingsScreen.AscendingVolume) })
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

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SettingsScreenContentRootPreview() {
    XdripPreview {
        SettingsScreenContent(
            screen = SettingsScreen.Root,
            onNavigate = {},
            onOpenClassic = {},
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SettingsScreenContentCategoryPreview() {
    XdripPreview {
        SettingsScreenContent(
            screen = SettingsScreen.GeneralCategory,
            onNavigate = {},
            onOpenClassic = {},
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SwitchPrefPreview() {
    XdripPreview {
        SwitchPref(
            state = SettingsState(),
            key = "preview_switch",
            title = "Enable feature",
            subtitle = "Summary text",
            default = true,
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditPrefPreview() {
    XdripPreview {
        EditPref(
            state = SettingsState(),
            key = "preview_edit",
            title = "Display name",
            subtitle = "Summary text",
            default = "150",
            numeric = true,
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ListPrefPreview() {
    XdripPreview {
        ListPref(
            state = SettingsState(),
            key = "preview_list",
            title = "Units",
            entries = listOf("mg/dl", "mmol/L"),
            values = listOf("mgdl", "mmol"),
            default = "mgdl",
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RingtonePrefPreview() {
    XdripPreview {
        RingtonePref(
            state = SettingsState(),
            key = "preview_ringtone",
            title = "Alert sound",
            subtitle = "Summary text",
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun RootScreenPreview() {
    XdripPreview { RootScreen(onNavigate = {}, onOpenClassic = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun GeneralCategoryScreenPreview() {
    XdripPreview { GeneralCategoryScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AlarmsCategoryScreenPreview() {
    XdripPreview { AlarmsCategoryScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun YourDataCategoryScreenPreview() {
    XdripPreview { YourDataCategoryScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ProfileCategoryScreenPreview() {
    XdripPreview { ProfileCategoryScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun DevicesCategoryScreenPreview() {
    XdripPreview { DevicesCategoryScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AppearanceCategoryScreenPreview() {
    XdripPreview { AppearanceCategoryScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AccessibilityCategoryScreenPreview() {
    XdripPreview { AccessibilityCategoryScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AdvancedCategoryScreenPreview() {
    XdripPreview { AdvancedCategoryScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun UnitsScreenPreview() {
    XdripPreview { UnitsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun NotificationStyleScreenPreview() {
    XdripPreview { NotificationStyleScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun BgAlertsScreenPreview() {
    XdripPreview { BgAlertsScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SuppressAlertsScreenPreview() {
    XdripPreview { SuppressAlertsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AscendingVolumeScreenPreview() {
    XdripPreview { AscendingVolumeScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PersistentHighScreenPreview() {
    XdripPreview { PersistentHighScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ForecastLowScreenPreview() {
    XdripPreview { ForecastLowScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SensorExpiryScreenPreview() {
    XdripPreview { SensorExpiryScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun CalibrationAlertsScreenPreview() {
    XdripPreview { CalibrationAlertsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun OtherAlertsScreenPreview() {
    XdripPreview { OtherAlertsScreen() }
}

// endregion
