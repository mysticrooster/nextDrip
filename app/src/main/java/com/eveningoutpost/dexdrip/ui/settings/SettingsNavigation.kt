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
    data object WebFollow : SettingsScreen
    data object NfcSettings : SettingsScreen
    data object NsFollowDownload : SettingsScreen
    data object G5Debug : SettingsScreen
    data object PreemptiveRestart : SettingsScreen
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
)

internal fun searchSettings(query: String): List<SettingsSearchEntry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return SETTINGS_SEARCH_INDEX.filter { entry ->
        entry.title.lowercase().contains(q) || entry.aliases.any { it.contains(q) }
    }
}
