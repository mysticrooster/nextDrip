package com.eveningoutpost.dexdrip.ui.settings

import android.content.Context
import androidx.annotation.StringRes
import com.eveningoutpost.dexdrip.R

/**
 * Destinations in the Compose settings host.
 *
 * Each destination carries its own search-relevant metadata so that titles, search keywords and
 * availability live in one place. `titleFor`/`SETTINGS_SEARCH_KEYWORDS`/`isAvailable` are derived
 * from this declaration.
 *
 * The migration keeps a lightweight in-Compose screen stack (see
 * `Documentation/technical/Settings_And_Secondary_Views_Compose.md`); this will become a
 * `navigation-compose` graph once the full set of categories is migrated.
 *
 * Entries set exactly one of [titleRes]/[titleLiteral]. Keyword lists are inlined deliberately:
 * a top-level val referenced from an enum initializer can be uninitialized at class-load time.
 */
internal enum class SettingsScreen(
    @StringRes val titleRes: Int = 0,
    val titleLiteral: String? = null,
    val keywords: List<String> = emptyList(),
    val isAvailable: (SettingsState) -> Boolean = { true },
) {
    Root(titleLiteral = "Settings"),
    GeneralCategory(titleRes = R.string.general_settings),
    AlarmsCategory(
        titleRes = R.string.alarms_and_alerts,
        keywords = listOf("notifications", "alerts", "sounds"),
    ),
    YourDataCategory(titleLiteral = "Your Data"),
    ProfileCategory(titleLiteral = "Profile"),
    DevicesCategory(titleLiteral = "Devices"),
    AppearanceCategory(titleLiteral = "Appearance"),
    AccessibilityCategory(titleLiteral = "Accessibility"),
    AdvancedCategory(titleLiteral = "Advanced"),
    Units(
        titleRes = R.string.glucose_units,
        keywords = listOf("mmol", "mg/dl", "bg", "high", "low", "target"),
    ),
    Theme(
        titleRes = R.string.theme_colors,
        keywords = listOf(
            "colour",
            "color",
            "material you",
            "theme",
            "appearance",
            "preset",
            "classic",
        ),
    ),
    Notifications(
        titleLiteral = "Notifications",
        keywords = listOf(
            "notifications",
            "high priority",
            "public",
            "aod",
            "chip",
            "channels",
            "compact ongoing",
            "proper ongoing",
        ),
    ),
    BgAlerts(
        titleRes = R.string.glucose_alerts_settings,
        keywords = listOf("alert profile", "audio focus", "snooze"),
    ),
    SuppressAlerts(
        titleRes = R.string.suppress_alerts_if_missed_readings,
        keywords = listOf("stale data"),
    ),
    AscendingVolume(
        titleRes = R.string.title_ascending_volume,
        keywords = listOf("escalating", "volume"),
    ),
    PersistentHigh(
        titleRes = R.string.persistent_high_alert,
        keywords = listOf("high alarm", "threshold"),
    ),
    ForecastLow(
        titleRes = R.string.forecasted_low_alert,
        keywords = listOf("predicted low", "forecast"),
    ),
    SensorExpiry(
        titleRes = R.string.title_sens_expiry,
        keywords = listOf("sensor", "expiry"),
    ),
    CalibrationAlerts(
        titleRes = R.string.calibration_alerts,
        keywords = listOf("calibration reminder", "snooze"),
    ),
    OtherAlerts(
        titleRes = R.string.other_alerts,
        keywords = listOf("noisy readings", "falling", "rising"),
    ),
    DataSource(
        titleRes = R.string.data_source_settings,
        keywords = listOf(
            "hardware",
            "collector",
            "sensor",
            "dexcom",
            "libre",
            "nightscout",
            "medtrum",
        ),
    ),
    DexcomDevice(
        titleLiteral = "Dexcom",
        keywords = listOf("dexcom", "g5", "g6", "g7", "share", "transmitter"),
    ),
    MedtrumDevice(
        titleLiteral = "Medtrum",
        keywords = listOf("medtrum", "sensor"),
    ),
    BluetoothBridge(
        titleLiteral = "Bluetooth Bridge",
        keywords = listOf("bridge", "wixel", "parakeet"),
    ),
    WebFollow(
        titleLiteral = "Web Follower Settings",
        keywords = listOf("webfollow", "proxy"),
    ),
    NfcSettings(
        titleRes = R.string.nfc_scan_features,
        keywords = listOf("nfc", "libre", "scan", "expiry"),
    ),
    NsFollowDownload(titleRes = R.string.title_nsfollow_download_treatments),
    G5Debug(titleRes = R.string.g5_debug_settings),
    PreemptiveRestart(titleRes = R.string.title_ob1_g5_preemptive_restart),
    Backups(
        titleLiteral = "Backups",
        keywords = listOf(
            "backup",
            "cloud backup",
            "export",
            "import",
            "database",
            "sdcard",
            "csv",
            "share config",
            "qr",
        ),
    ),
    About(
        titleLiteral = "About",
        keywords = listOf(
            "about",
            "help",
            "feedback",
            "license",
            "eula",
            "translation",
            "classic settings",
        ),
    ),
    Version(
        titleLiteral = "Version",
        keywords = listOf("version", "build", "release"),
    ),
    HomeScreen(
        titleLiteral = "Home Screen",
        keywords = listOf(
            "home screen",
            "shelf",
            "chart preview",
            "time buttons",
            "trend arrow",
            "collector status",
        ),
    ),
    DataSync(
        titleRes = R.string.data_sync,
        keywords = listOf(
            "cloud",
            "cloud sync",
            "upload",
            "sync",
            "nightscout",
            "tidepool",
            "mongodb",
            "influxdb",
            "nocturne",
            "nightlite",
        ),
    ),
    AutoConfig(
        titleRes = R.string.auto_configure_title,
        keywords = listOf("auto configure", "qr"),
    ),
    CloudUpload(titleRes = R.string.cloud_upload),
    RestApi(
        titleRes = R.string.pref_title_api,
        keywords = listOf("api"),
    ),
    RestApiDownload(titleRes = R.string.title_cloud_storage_api_download_enable),
    RestApiExtra(titleRes = R.string.title_rest_api_extra_options),
    Mongo(
        titleRes = R.string.pref_title_mongodb,
        keywords = listOf("mongodb"),
    ),
    Influx(
        titleRes = R.string.pref_title_influxdb,
        keywords = listOf("influxdb"),
    ),
    DexcomUpload(titleRes = R.string.dexcom_share_server_upload),
    Tidepool(titleRes = R.string.title_tidepool),
    WebDeposit(
        titleLiteral = "Web Deposit",
        isAvailable = { SettingsVisibility.isEngineeringMode(it) },
    ),
    NightLite(titleLiteral = "NightLite"),
    Nocturne(titleRes = R.string.nocturne),
    GlucoseMeters(
        titleRes = R.string.glucose_meters,
        keywords = listOf("bluetooth meter", "nfc meter", "calibration meter"),
    ),
    SpeakReadings(titleRes = R.string.speak_readings),
    InterApp(titleRes = R.string.interapp_settings),
    HealthConnect(
        titleRes = R.string.google_health_connect,
        keywords = listOf("google fit", "fit"),
    ),
    ExtraStatusLine(
        titleRes = R.string.extra_status_line,
        keywords = listOf("status line", "a1c", "average"),
    ),
    CalibrationSettings(
        titleRes = R.string.advanced_calibration,
        keywords = listOf("calibration plugin", "double calibration"),
    ),
    BluetoothSettings(
        titleRes = R.string.bluetooth_settings,
        keywords = listOf("bluetooth", "gatt", "watchdog"),
    ),
    BlueReaderSettings(
        titleRes = R.string.advanced_bluereader_settings,
        isAvailable = { SettingsVisibility.isBlueReader() },
    ),
    LibreOptions(
        titleLiteral = "Advanced Libre options",
        keywords = listOf(
            "libre",
            "libre 2",
            "algorithm",
            "external",
            "blukon",
            "serial",
            "smoothed",
            "one minute",
            "smoothing",
        ),
        isAvailable = { SettingsVisibility.hasLibre(SettingsVisibility.collectionType(it)) },
    ),
    LoggingSettings(
        titleRes = R.string.extra_logging,
        keywords = listOf("logs", "debug"),
    ),
    OtherMiscSettings(
        titleRes = R.string.title_Other_misc_options,
        keywords = listOf(
            "advanced settings",
            "extra settings",
            "less common",
            "other settings",
            "experimental",
            "misc",
        ),
    ),
    SmartWatchOptions(
        titleRes = R.string.smart_watch_features,
        keywords = listOf(
            "watch",
            "wear",
            "pebble",
            "amazfit",
            "bluejay",
            "lefun",
            "miband",
        ),
    ),
    SmartwatchSensors(
        titleRes = R.string.title_Smartwatch_Sensors,
        keywords = listOf("heart rate", "step counter", "sensors"),
    ),
    WearSettings(
        titleRes = R.string.android_wear_integration,
        keywords = listOf("wear", "wear os", "watch"),
    ),
    AmazfitSettings(
        titleRes = R.string.amazfit_sync_service,
        keywords = listOf("amazfit", "watchface", "widget"),
    ),
    LeFunSettings(
        titleRes = R.string.title_lefun_band,
        keywords = listOf("lefun", "band"),
    ),
    LeFunFeatures(titleRes = R.string.title_lefun_screens_features),
    BlueJaySettings(
        titleLiteral = "BlueJay Watch",
        keywords = listOf("bluejay", "thinjam"),
    ),
    BlueJayAdvanced(titleLiteral = "BlueJay Advanced Settings"),
    MiBandSettings(
        titleRes = R.string.title_miband,
        keywords = listOf("miband", "mi band"),
    ),
    MiBandSubSettings(titleRes = R.string.title_miband_screens_features),
    PebbleSettings(
        titleRes = R.string.pebble_integration,
        keywords = listOf("pebble", "watchface"),
    ),
    XdripPlusDisplay(
        titleRes = R.string.xdrip_plus_display_settings,
        keywords = listOf(
            "display",
            "display settings",
            "font",
            "language",
            "graph",
            "number wall",
            "accessibility",
            "y axis",
            "smoothing",
            "extra settings",
            "xdrip plus extra",
        ),
    ),
    XdripPlusFont(
        titleRes = R.string.title_font_settings,
        keywords = listOf("font", "enlarge", "large screens"),
    ),
    XdripPlusLanguage(
        titleRes = R.string.title_language,
        keywords = listOf(
            "language",
            "language settings",
            "locale",
            "force english",
        ),
    ),
    XdripPlusGraphDisplay(
        titleRes = R.string.title_xdrip_plus_graph_display_settings,
        keywords = listOf(
            "graph",
            "graph display settings",
            "grid",
            "average",
            "target",
            "basal",
            "smb",
            "raw",
        ),
    ),
    XdripPlusGraphSmoothing(
        titleRes = R.string.graph_smoothing,
        keywords = listOf("smoothing", "unsmoothed"),
    ),
    XdripPlusYAxis(
        titleRes = R.string.title_yRange,
        keywords = listOf(
            "y axis",
            "y axis range",
            "y range",
            "ymax",
            "ymin",
            "autopan",
        ),
    ),
    XdripPlusAccessibility(
        titleRes = R.string.title_xdrip_plus_accessibility,
        keywords = listOf("accessibility", "aod", "always on display"),
    ),
    XdripPlusNumberWall(
        titleRes = R.string.title_xdrip_plus_number_wall,
        keywords = listOf(
            "number wall",
            "lockscreen",
            "wallpaper",
            "time range",
        ),
    ),
    XdripPlusNumberIcon(
        titleRes = R.string.title_xdrip_plus_number_icon,
        keywords = listOf("number icon", "icon test"),
    ),
    XdripPlusCopying(
        titleRes = R.string.copying_settings,
        keywords = listOf("copy", "qr", "export", "import", "sdcard"),
    ),
    XdripPlusUpdate(
        titleRes = R.string.xdrip_plus_update_settings,
        keywords = listOf(
            "update",
            "channel",
            "beta",
            "alpha",
            "crashlytics",
            "telemetry",
            "feedback",
        ),
    ),
    XdripPlusMotion(
        titleRes = R.string.xdrip_motion_tracking,
        keywords = listOf("motion", "vehicle", "activity", "car audio"),
    ),
    XdripPlusPens(
        titleRes = R.string.insulin_pens,
        keywords = listOf("pen", "novopen", "inpen", "pendiq"),
    ),
    XdripPlusNovopen(
        titleRes = R.string.title_novopen_insulin_pen,
        keywords = listOf("novopen", "opennov", "pen"),
    ),
    XdripPlusInpen(
        titleRes = R.string.title_inpen_screen,
        keywords = listOf("inpen", "pen"),
    ),
    XdripPlusPendiq(
        titleRes = R.string.title_pendiq_screen,
        keywords = listOf("pendiq", "pen", "pin"),
    ),
    XdripPlusPrediction(
        titleRes = R.string.xdrip_plus_prediction_settings,
        keywords = listOf(
            "prediction",
            "prediction settings",
            "profile",
            "carb ratio",
            "insulin sensitivity",
            "simulations",
        ),
    ),
    XdripPlusMultipleInsulin(
        titleRes = R.string.title_multiple_insulin_types_settings,
        keywords = listOf("insulin types", "insulin profiles"),
    ),
    XdripPlusAdvPredict(
        titleRes = R.string.low_prediction_values,
        keywords = listOf(
            "low prediction",
            "low prediction values",
            "target range",
            "liver sensitivity",
            "dia",
        ),
    ),
    XdripPlusSync(
        titleRes = R.string.xdrip_plus_sync_settings,
        keywords = listOf(
            "sync",
            "sync settings",
            "cloud",
            "follow",
            "master",
            "sync key",
        ),
    ),
    XdripPlusRemoteSnooze(
        titleRes = R.string.remote_snoozing,
        keywords = listOf("remote snooze", "broadcast snooze"),
    ),
    XdripPlusDesertSync(
        titleRes = R.string.title_xdrip_plus_desert_sync_settings,
        keywords = listOf("desert sync", "master ip", "https"),
    ),
}

internal fun SettingsScreen.title(context: Context): String =
    titleLiteral ?: context.getString(titleRes)
