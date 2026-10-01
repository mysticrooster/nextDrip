package com.eveningoutpost.dexdrip.ui.settings

import android.content.Context
import android.content.res.Configuration
import java.text.Normalizer
import java.util.Locale

/**
 * Destination-level settings search (AAPS-style).
 *
 * The index is derived from every non-[SettingsScreen.Root] destination, so it stays complete as
 * the Compose migration grows. Matching is relevance-ranked and diacritics-insensitive, and a
 * small keyword map adds aliases the titles do not cover. Only destinations that are unreachable
 * and not self-selectable are filtered out (see [isAvailable]); data-source provider screens stay
 * searchable so users can switch collection methods.
 */
internal data class SettingsSearchEntry(
    val screen: SettingsScreen,
    val localizedTitle: String,
    val englishTitle: String,
    val keywords: List<String>,
)

/** Builds the index once per [context] locale; callers should `remember(context)` the result. */
internal fun buildSettingsSearchIndex(context: Context): List<SettingsSearchEntry> {
    val englishContext = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) }
    )
    return SettingsScreen.entries
        .filter { it != SettingsScreen.Root }
        .map { screen ->
            SettingsSearchEntry(
                screen = screen,
                localizedTitle = titleFor(context, screen),
                englishTitle = titleFor(englishContext, screen),
                keywords = SETTINGS_SEARCH_KEYWORDS[screen].orEmpty(),
            )
        }
}

/**
 * Ranks [index] against [query] and drops hidden destinations. [state] is read for the reactive
 * engineering-mode gate. Blank queries return no results.
 */
internal fun searchSettings(
    index: List<SettingsSearchEntry>,
    query: String,
    state: SettingsState,
): List<SettingsSearchEntry> {
    val normalizedQuery = normalize(query.trim())
    if (normalizedQuery.isEmpty()) return emptyList()
    return index
        .filter { isAvailable(it.screen, state) }
        .mapNotNull { entry ->
            val score = relevance(entry, normalizedQuery)
            if (score > 0) entry to score else null
        }
        .sortedByDescending { it.second }
        .map { it.first }
}

/** Conservative availability: hide only destinations that are unreachable and not self-selectable. */
private fun isAvailable(screen: SettingsScreen, state: SettingsState): Boolean = when (screen) {
    SettingsScreen.WebDeposit -> SettingsVisibility.isEngineeringMode(state)
    SettingsScreen.BlueReaderSettings -> SettingsVisibility.isBlueReader()
    SettingsScreen.Libre2Settings -> SettingsVisibility.isLibreReceiver()
    else -> true
}

/** Mirrors the AAPS weights: localized title > English title > keyword. */
private fun relevance(entry: SettingsSearchEntry, normalizedQuery: String): Int {
    var score = 0
    val localized = normalize(entry.localizedTitle)
    if (localized.contains(normalizedQuery)) {
        score = 100 + if (localized.startsWith(normalizedQuery)) 50 else 0
    }
    val english = normalize(entry.englishTitle)
    if (english.contains(normalizedQuery)) {
        score = maxOf(score, 80)
    }
    entry.keywords.forEach { keyword ->
        val normalizedKeyword = normalize(keyword)
        val keywordScore = when {
            normalizedKeyword == normalizedQuery -> 80
            normalizedKeyword.startsWith(normalizedQuery) -> 60
            normalizedKeyword.contains(normalizedQuery) -> 40
            else -> 0
        }
        score = maxOf(score, keywordScore)
    }
    return score
}

/** Lowercases then strips combining marks so accented and unaccented queries match. */
private fun normalize(value: String): String {
    val lower = value.lowercase(Locale.ROOT)
    return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(COMBINING_MARKS, "")
}

private val COMBINING_MARKS = Regex("\\p{Mn}+")

/**
 * Aliases for destinations whose localized/English titles do not already cover common queries.
 * Ported from the retired `SETTINGS_SEARCH_INDEX`, keyed by screen; kept deliberately small.
 */
internal val SETTINGS_SEARCH_KEYWORDS: Map<SettingsScreen, List<String>> = mapOf(
    SettingsScreen.Units to listOf("mmol", "mg/dl", "bg", "high", "low", "target"),
    SettingsScreen.Theme to listOf(
        "colour",
        "color",
        "material you",
        "theme",
        "appearance",
        "preset",
        "classic"
    ),
    SettingsScreen.DataSource to listOf(
        "hardware",
        "collector",
        "sensor",
        "dexcom",
        "libre",
        "nightscout",
        "medtrum"
    ),
    SettingsScreen.DexcomDevice to listOf("dexcom", "g5", "g6", "g7", "share", "transmitter"),
    SettingsScreen.MedtrumDevice to listOf("medtrum", "sensor"),
    SettingsScreen.BluetoothBridge to listOf("bridge", "wixel", "parakeet"),
    SettingsScreen.WebFollow to listOf("webfollow", "proxy"),
    SettingsScreen.NfcSettings to listOf("nfc", "libre", "scan", "expiry"),
    SettingsScreen.DataSync to listOf(
        "cloud",
        "cloud sync",
        "upload",
        "sync",
        "nightscout",
        "tidepool",
        "mongodb",
        "influxdb",
        "nocturne",
        "nightlite"
    ),
    SettingsScreen.Backups to listOf(
        "backup",
        "cloud backup",
        "export",
        "import",
        "database",
        "sdcard",
        "csv",
        "share config",
        "qr"
    ),
    SettingsScreen.GlucoseMeters to listOf("bluetooth meter", "nfc meter", "calibration meter"),
    SettingsScreen.CalibrationSettings to listOf("calibration plugin", "double calibration"),
    SettingsScreen.BluetoothSettings to listOf("bluetooth", "gatt", "watchdog"),
    SettingsScreen.ExtraStatusLine to listOf("status line", "a1c", "average"),
    SettingsScreen.AlarmsCategory to listOf("notifications", "alerts", "sounds"),
    SettingsScreen.NotificationStyle to listOf(
        "notifications",
        "high priority",
        "public",
        "aod",
        "chip",
        "channels"
    ),
    SettingsScreen.BgAlerts to listOf("alert profile", "audio focus", "snooze"),
    SettingsScreen.SuppressAlerts to listOf("stale data"),
    SettingsScreen.AscendingVolume to listOf("escalating", "volume"),
    SettingsScreen.PersistentHigh to listOf("high alarm", "threshold"),
    SettingsScreen.ForecastLow to listOf("predicted low", "forecast"),
    SettingsScreen.SensorExpiry to listOf("sensor", "expiry"),
    SettingsScreen.CalibrationAlerts to listOf("calibration reminder", "snooze"),
    SettingsScreen.OtherAlerts to listOf("noisy readings", "falling", "rising"),
    SettingsScreen.SmartWatchOptions to listOf(
        "watch",
        "wear",
        "pebble",
        "amazfit",
        "bluejay",
        "lefun",
        "miband"
    ),
    SettingsScreen.WearSettings to listOf("wear", "wear os", "watch"),
    SettingsScreen.PebbleSettings to listOf("pebble", "watchface"),
    SettingsScreen.AmazfitSettings to listOf("amazfit", "watchface", "widget"),
    SettingsScreen.BlueJaySettings to listOf("bluejay", "thinjam"),
    SettingsScreen.LeFunSettings to listOf("lefun", "band"),
    SettingsScreen.MiBandSettings to listOf("miband", "mi band"),
    SettingsScreen.SmartwatchSensors to listOf("heart rate", "step counter", "sensors"),
    SettingsScreen.XdripPlusDisplay to listOf(
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
        "xdrip plus extra"
    ),
    SettingsScreen.XdripPlusFont to listOf("font", "enlarge", "large screens"),
    SettingsScreen.XdripPlusLanguage to listOf(
        "language",
        "language settings",
        "locale",
        "force english"
    ),
    SettingsScreen.XdripPlusGraphDisplay to listOf(
        "graph",
        "graph display settings",
        "grid",
        "average",
        "target",
        "basal",
        "smb",
        "raw"
    ),
    SettingsScreen.XdripPlusGraphSmoothing to listOf("smoothing", "unsmoothed"),
    SettingsScreen.XdripPlusYAxis to listOf(
        "y axis",
        "y axis range",
        "y range",
        "ymax",
        "ymin",
        "autopan"
    ),
    SettingsScreen.XdripPlusAccessibility to listOf("accessibility", "aod", "always on display"),
    SettingsScreen.XdripPlusNumberWall to listOf(
        "number wall",
        "lockscreen",
        "wallpaper",
        "time range"
    ),
    SettingsScreen.XdripPlusNumberIcon to listOf("number icon", "icon test"),
    SettingsScreen.XdripPlusCopying to listOf("copy", "qr", "export", "import", "sdcard"),
    SettingsScreen.XdripPlusUpdate to listOf(
        "update",
        "channel",
        "beta",
        "alpha",
        "crashlytics",
        "telemetry",
        "feedback"
    ),
    SettingsScreen.XdripPlusMotion to listOf("motion", "vehicle", "activity", "car audio"),
    SettingsScreen.XdripPlusPens to listOf("pen", "novopen", "inpen", "pendiq"),
    SettingsScreen.XdripPlusNovopen to listOf("novopen", "opennov", "pen"),
    SettingsScreen.XdripPlusInpen to listOf("inpen", "pen"),
    SettingsScreen.XdripPlusPendiq to listOf("pendiq", "pen", "pin"),
    SettingsScreen.XdripPlusPrediction to listOf(
        "prediction",
        "prediction settings",
        "profile",
        "carb ratio",
        "insulin sensitivity",
        "simulations"
    ),
    SettingsScreen.XdripPlusMultipleInsulin to listOf("insulin types", "insulin profiles"),
    SettingsScreen.XdripPlusAdvPredict to listOf(
        "low prediction",
        "low prediction values",
        "target range",
        "liver sensitivity",
        "dia"
    ),
    SettingsScreen.XdripPlusSync to listOf(
        "sync",
        "sync settings",
        "cloud",
        "follow",
        "master",
        "sync key"
    ),
    SettingsScreen.XdripPlusRemoteSnooze to listOf("remote snooze", "broadcast snooze"),
    SettingsScreen.XdripPlusDesertSync to listOf("desert sync", "master ip", "https"),
    SettingsScreen.HomeScreen to listOf(
        "home screen",
        "shelf",
        "chart preview",
        "time buttons",
        "trend arrow",
        "collector status"
    ),
    SettingsScreen.About to listOf(
        "about",
        "help",
        "feedback",
        "license",
        "eula",
        "translation",
        "classic settings"
    ),
    SettingsScreen.Version to listOf("version", "build", "release"),
    SettingsScreen.OtherMiscSettings to listOf(
        "advanced settings",
        "extra settings",
        "less common",
        "other settings",
        "experimental",
        "misc"
    ),
    SettingsScreen.Mongo to listOf("mongodb"),
    SettingsScreen.Influx to listOf("influxdb"),
    SettingsScreen.AutoConfig to listOf("auto configure", "qr"),
    SettingsScreen.RestApi to listOf("api"),
    SettingsScreen.HealthConnect to listOf("google fit", "fit"),
    SettingsScreen.CollectorInForeground to listOf("foreground service"),
    SettingsScreen.LoggingSettings to listOf("logs", "debug"),
)
