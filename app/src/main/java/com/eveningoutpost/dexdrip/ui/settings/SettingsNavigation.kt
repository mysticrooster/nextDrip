package com.eveningoutpost.dexdrip.ui.settings

/**
 * Destinations in the Compose settings host.
 *
 * The migration keeps a lightweight in-Compose screen stack (see
 * `Documentation/technical/Settings_And_Secondary_Views_Compose.md`); this will become a
 * `navigation-compose` graph once the full set of categories is migrated.
 */
internal sealed interface SettingsScreen {
    data object Root : SettingsScreen
    data object Units : SettingsScreen
    data object Theme : SettingsScreen
    data object Notifications : SettingsScreen
    data object BgAlerts : SettingsScreen
    data object SuppressAlerts : SettingsScreen
    data object NotificationChannels : SettingsScreen
    data object AscendingVolume : SettingsScreen
    data object PersistentHigh : SettingsScreen
    data object ForecastLow : SettingsScreen
    data object SensorExpiry : SettingsScreen
    data object CalibrationAlerts : SettingsScreen
    data object OtherAlerts : SettingsScreen
    data object DataSource : SettingsScreen
    data object ILet : SettingsScreen
    data object WebFollow : SettingsScreen
    data object NfcSettings : SettingsScreen
    data object NsFollowDownload : SettingsScreen
    data object G5Debug : SettingsScreen
    data object PreemptiveRestart : SettingsScreen
    data object DataSync : SettingsScreen
    data object AutoConfig : SettingsScreen
    data object CloudUpload : SettingsScreen
    data object RestApi : SettingsScreen
    data object RestApiDownload : SettingsScreen
    data object RestApiExtra : SettingsScreen
    data object Mongo : SettingsScreen
    data object Influx : SettingsScreen
    data object DexcomUpload : SettingsScreen
    data object Tidepool : SettingsScreen
    data object WebDeposit : SettingsScreen
    data object NightLite : SettingsScreen
    data object Nocturne : SettingsScreen
    data object GlucoseMeters : SettingsScreen
    data object SpeakReadings : SettingsScreen
    data object InterApp : SettingsScreen
    data object HealthConnect : SettingsScreen
    data object LessCommon : SettingsScreen
    data object ExtraStatusLine : SettingsScreen
    data object CalibrationSettings : SettingsScreen
    data object BluetoothSettings : SettingsScreen
    data object BlueReaderSettings : SettingsScreen
    data object Libre2Settings : SettingsScreen
    data object LoggingSettings : SettingsScreen
    data object OtherMiscSettings : SettingsScreen
    data object CollectorInForeground : SettingsScreen
    data object SmartWatchOptions : SettingsScreen
    data object SmartwatchSensors : SettingsScreen
    data object WearSettings : SettingsScreen
    data object AmazfitSettings : SettingsScreen
    data object LeFunSettings : SettingsScreen
    data object LeFunFeatures : SettingsScreen
    data object BlueJaySettings : SettingsScreen
    data object BlueJayAdvanced : SettingsScreen
    data object MiBandSettings : SettingsScreen
    data object MiBandSubSettings : SettingsScreen
    data object PebbleSettings : SettingsScreen
    data object XdripPlusDisplay : SettingsScreen
    data object XdripPlusFont : SettingsScreen
    data object XdripPlusLanguage : SettingsScreen
    data object XdripPlusGraphDisplay : SettingsScreen
    data object XdripPlusGraphSmoothing : SettingsScreen
    data object XdripPlusYAxis : SettingsScreen
    data object XdripPlusAccessibility : SettingsScreen
    data object XdripPlusNumberWall : SettingsScreen
    data object XdripPlusNumberIcon : SettingsScreen
    data object XdripPlusCopying : SettingsScreen
    data object XdripPlusUpdate : SettingsScreen
    data object XdripPlusMotion : SettingsScreen
    data object XdripPlusPens : SettingsScreen
    data object XdripPlusNovopen : SettingsScreen
    data object XdripPlusInpen : SettingsScreen
    data object XdripPlusPendiq : SettingsScreen
    data object XdripPlusPrediction : SettingsScreen
    data object XdripPlusMultipleInsulin : SettingsScreen
    data object XdripPlusAdvPredict : SettingsScreen
    data object XdripPlusSync : SettingsScreen
    data object XdripPlusRemoteSnooze : SettingsScreen
    data object XdripPlusDesertSync : SettingsScreen
}

/** A searchable settings destination shown by the root search field. */
internal data class SettingsSearchEntry(
    val title: String,
    val screen: SettingsScreen,
    val aliases: List<String> = emptyList(),
)

/** Index of the migrated destinations; grows as categories move to Compose. */
internal val SETTINGS_SEARCH_INDEX = listOf(
    SettingsSearchEntry("Glucose Units", SettingsScreen.Units, listOf("mmol", "mg/dl", "bg", "high", "low", "target")),
    SettingsSearchEntry("Theme colours", SettingsScreen.Theme, listOf("colour", "color", "material you", "theme", "appearance")),
    SettingsSearchEntry("Data Source Settings", SettingsScreen.DataSource, listOf("hardware", "collector", "sensor", "dexcom", "libre", "nightscout", "medtrum")),
    SettingsSearchEntry("Web Follower Settings", SettingsScreen.WebFollow, listOf("webfollow", "proxy")),
    SettingsSearchEntry("NFC Scan Features", SettingsScreen.NfcSettings, listOf("nfc", "libre", "scan", "expiry")),
    SettingsSearchEntry("Data Sync", SettingsScreen.DataSync, listOf("cloud", "upload", "nightscout", "tidepool", "mongodb", "influxdb", "nocturne", "nightlite")),
    SettingsSearchEntry("Glucose Meters", SettingsScreen.GlucoseMeters, listOf("bluetooth meter", "nfc meter", "calibration meter")),
    SettingsSearchEntry("Advanced Calibration", SettingsScreen.CalibrationSettings, listOf("calibration plugin", "double calibration")),
    SettingsSearchEntry("Bluetooth Settings", SettingsScreen.BluetoothSettings, listOf("bluetooth", "gatt", "watchdog")),
    SettingsSearchEntry("Extra Status Line", SettingsScreen.ExtraStatusLine, listOf("status line", "a1c", "average")),
    SettingsSearchEntry("Alarms and Alerts", SettingsScreen.Notifications, listOf("notifications", "alerts", "sounds")),
    SettingsSearchEntry("Glucose Alerts Settings", SettingsScreen.BgAlerts, listOf("alert profile", "audio focus", "snooze")),
    SettingsSearchEntry("Suppress Alerts if Missed Readings", SettingsScreen.SuppressAlerts, listOf("stale data")),
    SettingsSearchEntry("Use Notification Channels", SettingsScreen.NotificationChannels, listOf("aod", "chip")),
    SettingsSearchEntry("Ascending Volume", SettingsScreen.AscendingVolume, listOf("escalating", "volume")),
    SettingsSearchEntry("Persistent High Alert", SettingsScreen.PersistentHigh, listOf("high alarm", "threshold")),
    SettingsSearchEntry("Forecasted Low Alert", SettingsScreen.ForecastLow, listOf("predicted low", "forecast")),
    SettingsSearchEntry("Sensor Expiry", SettingsScreen.SensorExpiry, listOf("sensor", "expiry")),
    SettingsSearchEntry("Calibration Alerts", SettingsScreen.CalibrationAlerts, listOf("calibration reminder", "snooze")),
    SettingsSearchEntry("Other Alerts", SettingsScreen.OtherAlerts, listOf("noisy readings", "falling", "rising")),
    SettingsSearchEntry("Smart Watch Features", SettingsScreen.SmartWatchOptions, listOf("watch", "wear", "pebble", "amazfit", "bluejay", "lefun", "miband")),
    SettingsSearchEntry("Android Wear Integration", SettingsScreen.WearSettings, listOf("wear", "wear os", "watch")),
    SettingsSearchEntry("Pebble", SettingsScreen.PebbleSettings, listOf("pebble", "watchface")),
    SettingsSearchEntry("Amazfit", SettingsScreen.AmazfitSettings, listOf("amazfit", "watchface", "widget")),
    SettingsSearchEntry("BlueJay Watch", SettingsScreen.BlueJaySettings, listOf("bluejay", "thinjam")),
    SettingsSearchEntry("LeFun Band", SettingsScreen.LeFunSettings, listOf("lefun", "band")),
    SettingsSearchEntry("MiBand", SettingsScreen.MiBandSettings, listOf("miband", "mi band")),
    SettingsSearchEntry("Smartwatch Sensors", SettingsScreen.SmartwatchSensors, listOf("heart rate", "step counter", "sensors")),
    SettingsSearchEntry("xDrip+ Extra Settings", SettingsScreen.XdripPlusDisplay, listOf("extra", "xdrip plus", "display", "graph", "number wall", "accessibility")),
    SettingsSearchEntry("Display Settings", SettingsScreen.XdripPlusDisplay, listOf("display", "font", "language", "graph", "number wall", "accessibility", "y axis", "smoothing")),
    SettingsSearchEntry("Font Settings", SettingsScreen.XdripPlusFont, listOf("font", "enlarge", "large screens")),
    SettingsSearchEntry("Language Settings", SettingsScreen.XdripPlusLanguage, listOf("language", "locale", "force english")),
    SettingsSearchEntry("Graph Display Settings", SettingsScreen.XdripPlusGraphDisplay, listOf("graph", "grid", "average", "target", "basal", "smb", "raw")),
    SettingsSearchEntry("Graph Smoothing", SettingsScreen.XdripPlusGraphSmoothing, listOf("smoothing", "unsmoothed")),
    SettingsSearchEntry("Y Axis Range", SettingsScreen.XdripPlusYAxis, listOf("y axis", "y range", "ymax", "ymin", "autopan")),
    SettingsSearchEntry("Accessibility", SettingsScreen.XdripPlusAccessibility, listOf("accessibility", "aod", "always on display")),
    SettingsSearchEntry("Number Wall", SettingsScreen.XdripPlusNumberWall, listOf("number wall", "lockscreen", "wallpaper", "time range")),
    SettingsSearchEntry("Number Icon", SettingsScreen.XdripPlusNumberIcon, listOf("number icon", "icon test")),
    SettingsSearchEntry("Copying Settings", SettingsScreen.XdripPlusCopying, listOf("copy", "qr", "export", "import", "sdcard")),
    SettingsSearchEntry("Update Settings", SettingsScreen.XdripPlusUpdate, listOf("update", "channel", "beta", "alpha", "crashlytics", "telemetry", "feedback")),
    SettingsSearchEntry("Motion Tracking", SettingsScreen.XdripPlusMotion, listOf("motion", "vehicle", "activity", "car audio")),
    SettingsSearchEntry("Insulin Pens", SettingsScreen.XdripPlusPens, listOf("pen", "novopen", "inpen", "pendiq")),
    SettingsSearchEntry("Novopen", SettingsScreen.XdripPlusNovopen, listOf("novopen", "opennov", "pen")),
    SettingsSearchEntry("InPen", SettingsScreen.XdripPlusInpen, listOf("inpen", "pen")),
    SettingsSearchEntry("Pendiq", SettingsScreen.XdripPlusPendiq, listOf("pendiq", "pen", "pin")),
    SettingsSearchEntry("Prediction Settings", SettingsScreen.XdripPlusPrediction, listOf("prediction", "profile", "carb ratio", "insulin sensitivity", "simulations")),
    SettingsSearchEntry("Multiple Insulin Types", SettingsScreen.XdripPlusMultipleInsulin, listOf("insulin types", "insulin profiles")),
    SettingsSearchEntry("Low Prediction Values", SettingsScreen.XdripPlusAdvPredict, listOf("low prediction", "target range", "liver sensitivity", "dia")),
    SettingsSearchEntry("Sync Settings", SettingsScreen.XdripPlusSync, listOf("sync", "cloud", "follow", "master", "sync key")),
    SettingsSearchEntry("Remote Snoozing", SettingsScreen.XdripPlusRemoteSnooze, listOf("remote snooze", "broadcast snooze")),
    SettingsSearchEntry("Desert Sync", SettingsScreen.XdripPlusDesertSync, listOf("desert sync", "master ip", "https")),
)

internal fun searchSettings(query: String): List<SettingsSearchEntry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return SETTINGS_SEARCH_INDEX.filter { entry ->
        entry.title.lowercase().contains(q) || entry.aliases.any { it.contains(q) }
    }
}
