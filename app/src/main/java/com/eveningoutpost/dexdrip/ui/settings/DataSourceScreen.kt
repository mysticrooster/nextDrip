package com.eveningoutpost.dexdrip.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.GcmActivity
import com.eveningoutpost.dexdrip.NFCReaderX
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.cgm.ilet.ILetEntry
import com.eveningoutpost.dexdrip.cgm.ilet.ILetLoginActivity
import com.eveningoutpost.dexdrip.cgm.ilet.IletPrefs
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.services.G5BaseService
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.AndroidBarcode
import com.eveningoutpost.dexdrip.utils.DexCollectionType
import com.eveningoutpost.dexdrip.utils.SdcardImportExport

/**
 * S3 — Data Source (`pref_data_source.xml`).
 *
 * Faithful port of the legacy category: the collection-method list (with its change side effects)
 * plus the sub-screens and leaves that were conditionally added/removed per [DexCollectionType].
 */
@Composable
internal fun DataSourceScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val ct = SettingsVisibility.collectionType(state)
    val entries = context.resources.getStringArray(R.array.DexCollectionMethods).toList()
    val values = context.resources.getStringArray(R.array.DexCollectionMethodValues).toList()

    SettingsCategory(context.getString(R.string.data_source_settings)) {
        SettingsListRow(
            title = context.getString(R.string.hardware_data_source),
            subtitle = context.getString(R.string.how_receive_data),
            entries = entries,
            values = values,
            selectedValue = state.string("dex_collection_method", "Disabled"),
            onSelected = { newValue ->
                applyCollectionMethodChange(newValue)
                state.setString("dex_collection_method", newValue)
            },
            modifier = Modifier.testTag("setting_dex_collection_method"),
        )

        if (ct == DexCollectionType.WebFollow) {
            SettingsActionRow(
                title = "Web Follower Settings",
                subtitle = "Configuration options for web follower data source",
                icon = Icons.Outlined.Language,
                onClick = { onNavigate(SettingsScreen.WebFollow) },
                modifier = Modifier.testTag("setting_web_follow"),
            )
        }

        if (SettingsVisibility.hasLibre(ct)) {
            SettingsActionRow(
                title = "Libre / NFC",
                subtitle = context.getString(R.string.nfc_options),
                icon = Icons.Outlined.Nfc,
                onClick = { onNavigate(SettingsScreen.LibreDevice) },
                modifier = Modifier.testTag("setting_libre_device"),
            )
        }

        if (ct == DexCollectionType.DexcomShare || ct == DexCollectionType.DexcomG5) {
            SettingsActionRow(
                title = "Dexcom",
                subtitle = context.getString(R.string.advanced_g5_settings),
                icon = Icons.Outlined.Sensors,
                onClick = { onNavigate(SettingsScreen.DexcomDevice) },
                modifier = Modifier.testTag("setting_dexcom_device"),
            )
        }

        if (ct == DexCollectionType.DexbridgeWixel || ct == DexCollectionType.WifiDexBridgeWixel) {
            SettingsActionRow(
                title = "Bluetooth Bridge",
                subtitle = "Transmitter id and bridge receiver options",
                icon = Icons.Outlined.Bluetooth,
                onClick = { onNavigate(SettingsScreen.BluetoothBridge) },
                modifier = Modifier.testTag("setting_bluetooth_bridge"),
            )
        }

        if (ct == DexCollectionType.Medtrum) {
            SettingsActionRow(
                title = "Medtrum",
                subtitle = context.getString(R.string.summary_medtrum_use_native),
                icon = Icons.Outlined.Sensors,
                onClick = { onNavigate(SettingsScreen.MedtrumDevice) },
                modifier = Modifier.testTag("setting_medtrum_device"),
            )
        }

        if (ct == DexCollectionType.ILet) {
            SettingsActionRow(
                title = "iLet",
                subtitle = "iLet account, pump data and read-only notice",
                icon = Icons.Outlined.Sensors,
                onClick = { onNavigate(SettingsScreen.ILetDevice) },
                modifier = Modifier.testTag("setting_ilet"),
            )
        }

        if (ct == DexCollectionType.NSFollow) {
            EditPref(state, "nsfollow_url", context.getString(R.string.title_nsfollow_url), default = "", subtitle = context.getString(R.string.summary_nsfollow_url), tag = "setting_nsfollow_url")
            SettingsActionRow(
                title = context.getString(R.string.title_nsfollow_download_treatments),
                icon = Icons.Outlined.Download,
                onClick = { onNavigate(SettingsScreen.NsFollowDownload) },
                modifier = Modifier.testTag("setting_nsfollow_download"),
            )
            ListPref(
                state,
                "nsfollow_sample_period_in_minutes",
                context.getString(R.string.title_sample_period),
                context.resources.getStringArray(R.array.nsfollow_sample_period_entries).toList(),
                context.resources.getStringArray(R.array.nsfollow_sample_period_values).toList(),
                "5",
                subtitle = context.getString(R.string.summary_nsfollow_sample_period),
            )
            ListPref(
                state,
                "nsfollow_lag",
                context.getString(R.string.title_nsfollow_lag),
                context.resources.getStringArray(R.array.nsfollowlag_entries).toList(),
                context.resources.getStringArray(R.array.nsfollowlag_values).toList(),
                "0",
                subtitle = context.getString(R.string.summary_nsfollow_lag),
            )
        }

        if (ct == DexCollectionType.SHFollow) {
            EditPref(state, "shfollow_user", "Share Username", default = "", subtitle = "Login username for Dex Share Following")
            EditPref(state, "shfollow_pass", "Share Password", default = "", subtitle = "Login password for Dex Share Following")
            SwitchPref(state, "dex_share_us_acct", "US Servers", default = true, subtitle = "Your account and follower app are from the USA")
        }

        if (ct == DexCollectionType.Follower) {
            SwitchPref(state, "follower_chime", context.getString(R.string.follower_chime_new), default = false, subtitle = context.getString(R.string.notify_data_arrives_master))
        }

        if (ct == DexCollectionType.CLFollow) {
            ListPref(
                state,
                "clfollow_country",
                context.getString(R.string.title_clfollow_country),
                context.resources.getStringArray(R.array.carelinkCountryEntries).toList(),
                context.resources.getStringArray(R.array.carelinkCountryValues).toList(),
                "gb",
                subtitle = context.getString(R.string.summary_clfollow_country),
            )
            EditPref(state, "clfollow_patient", context.getString(R.string.title_clfollow_patient), default = "", subtitle = context.getString(R.string.summary_clfollow_patient))
            SettingsActionRow(
                title = context.getString(R.string.title_clfollow_login),
                subtitle = context.getString(R.string.summary_clfollow_login),
                onClick = {},
                modifier = Modifier.testTag("setting_clfollow_login"),
            )
            EditPref(state, "clfollow_grace_period", context.getString(R.string.title_clfollow_grace_period), default = "30", subtitle = context.getString(R.string.summary_clfollow_grace_period))
            EditPref(state, "clfollow_missed_poll_interval", context.getString(R.string.title_clfollow_missed_poll_interval), default = "1", subtitle = context.getString(R.string.summary_clfollow_missed_poll_interval))
            SwitchPref(state, "clfollow_download_finger_bgs", context.getString(R.string.title_clfollow_download_finger_bgs), default = true, subtitle = context.getString(R.string.summary_clfollow_download_finger_bgs))
            SwitchPref(state, "clfollow_download_boluses", context.getString(R.string.title_clfollow_download_boluses), default = true, subtitle = context.getString(R.string.summary_clfollow_download_boluses))
            SwitchPref(state, "clfollow_download_meals", context.getString(R.string.title_clfollow_download_meals), default = true, subtitle = context.getString(R.string.summary_clfollow_download_meals))
            SwitchPref(state, "clfollow_download_notifications", context.getString(R.string.title_clfollow_download_notifications), default = false, subtitle = context.getString(R.string.summary_clfollow_download_notifications))
        }

        if (SettingsVisibility.hasWifi() || state.string("wifi_recievers_addresses", "").trim().isNotEmpty()) {
            EditPref(
                state,
                "wifi_recievers_addresses",
                context.getString(R.string.list_of_receivers),
                default = "",
                tag = "setting_wifi_receivers",
            )
        }
    }
}

/**
 * Reproduces the legacy `dex_collection_method` change listener: follower battery resets, turning
 * off xDrip Sync master for followers, and always restarting the collection service.
 */
private fun applyCollectionMethodChange(newValue: String) {
    val type = DexCollectionType.getType(newValue)
    if (type == DexCollectionType.DexcomShare) {
        Pref.setBoolean("calibration_notifications", false)
    }
    if (type == DexCollectionType.Follower) {
        Pref.setInt("bridge_battery", 0)
        Pref.setInt("parakeet_battery", 0)
        if (Pref.getBoolean("plus_follow_master", false)) {
            Pref.setBoolean("plus_follow_master", false)
            JoH.static_toast_long("Turning off xDrip Sync Master for Followers!")
        }
        GcmActivity.requestBGsync()
    }
    CollectionServiceStarter.restartCollectionServiceBackground()
}

@Composable
internal fun WebFollowScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val useProxy = state.bool("webfollow_use_proxy", false)

    SettingsCategory("Web Follower") {
        EditPref(state, "webfollow_master_domain", "Configuration Script", default = "beonlabs", subtitle = "This is the community helper address or keyword")
        EditPref(state, "webfollow_username", "Service logon user name", default = "", subtitle = "This is your registered user name or email address with the service")
        EditPref(state, "webfollow_password", "Service logon password", default = "", subtitle = "This is your registered password with the service")
        SwitchPref(state, "webfollow_use_proxy", "Use proxy server (advanced)", default = false)
        EditPref(state, "webfollow_proxy_address", "Proxy host address", default = "", enabled = useProxy)
        EditPref(state, "webfollow_proxy_port", "Proxy host port", default = "", numeric = true, enabled = useProxy)
        EditPref(state, "webfollow_proxy_username", "Proxy username", default = "", enabled = useProxy)
        EditPref(state, "webfollow_proxy_password", "Proxy password", default = "", enabled = useProxy)
        SwitchPref(
            state,
            "webfollow_proxy_type_http",
            "HTTP proxy",
            default = false,
            subtitle = if (state.bool("webfollow_proxy_type_http", false)) "Proxy type is HTTP (toggle to change)" else "Proxy type is SOCKS (toggle to change)",
            enabled = useProxy,
        )
    }
}

@Composable
internal fun NfcSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val useNfc = state.bool("use_nfc_scan", false)
    val engineering = SettingsVisibility.isEngineeringMode(state)

    SettingsCategory(context.getString(R.string.nfc_scan_features)) {
        SwitchPref(
            state,
            "use_nfc_scan",
            context.getString(R.string.use_nfc_feature),
            default = false,
            subtitle = context.getString(R.string.allow_sensor_scanning),
            tag = "setting_use_nfc_scan",
            onCheckedChange = { checked ->
                state.setBool("use_nfc_scan", checked)
                NFCReaderX.handleHomeScreenScanPreference(context, checked && state.bool("nfc_scan_homescreen", false))
            },
        )
        ListPref(
            state,
            "libre2_enable_bluetooth_streaming",
            context.getString(R.string.enable_streaming_title),
            context.resources.getStringArray(R.array.EnableStreamingMethods).toList(),
            context.resources.getStringArray(R.array.EnableStreamingValues).toList(),
            "enable_streaming_ask",
            subtitle = context.getString(R.string.enable_streaming_summary),
        )
        ListPref(
            state,
            "libre_filter_length",
            context.getString(R.string.libre_filter_length_title),
            context.resources.getStringArray(R.array.libreFilterLengthMethods).toList(),
            context.resources.getStringArray(R.array.libreFilterLengthValues).toList(),
            "25",
            subtitle = context.getString(R.string.libre_filter_length_summary),
        )
        SwitchPref(
            state,
            "nfc_show_age",
            context.getString(R.string.sensor_age_or_expiry),
            default = true,
            subtitle = if (state.bool("nfc_show_age", true)) context.getString(R.string.show_sensor_age) else context.getString(R.string.show_expiry_time),
        )
        EditPref(state, "nfc_expiry_days", context.getString(R.string.change_sensor_total_days), default = "14.5", numeric = true)
        SwitchPref(
            state,
            "nfc_scan_homescreen",
            context.getString(R.string.scan_when_app_closed),
            default = false,
            subtitle = if (state.bool("nfc_scan_homescreen", false)) context.getString(R.string.nfc_scanning_launcher) else context.getString(R.string.nfc_scanning_xdrip_open),
            enabled = useNfc,
            onCheckedChange = { checked ->
                state.setBool("nfc_scan_homescreen", checked)
                NFCReaderX.handleHomeScreenScanPreference(context, checked && state.bool("use_nfc_scan", false))
            },
        )
        SwitchPref(state, "nfc_scan_vibrate", context.getString(R.string.vibrate_scanning_status), default = true, enabled = useNfc)
        SwitchPref(state, "nfc_scan_beep", context.getString(R.string.beep_when_scanning_within_app), default = false, enabled = engineering)
        SwitchPref(state, "use_nfc_multiblock", context.getString(R.string.use_multi_block), default = true, enabled = useNfc)
        SwitchPref(state, "use_nfc_any_tag", context.getString(R.string.use_any_tag), default = true, enabled = useNfc)
        EditPref(state, "nfc_test_diagnostic", context.getString(R.string.low_level_value), default = "", enabled = engineering)
    }
}

@Composable
internal fun NsFollowDownloadScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.title_nsfollow_download_treatments)) {
        SwitchPref(state, "nsfollow_download_treatments", context.getString(R.string.title_nsfollow_download_treatments), default = false, subtitle = context.getString(R.string.summary_nsfollow_download_treatments))
        SwitchPref(
            state,
            "cloud_storage_api_skip_download_from_xdrip",
            context.getString(R.string.title_cloud_storage_api_download_from_xdrip),
            default = true,
            subtitle = context.getString(R.string.summary_cloud_storage_api_download_from_xdrip),
            enabled = state.dependentEnabled("nsfollow_download_treatments", false),
        )
    }
}

@Composable
internal fun G5DebugScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val engineering = SettingsVisibility.isEngineeringMode(state)
    val useTransmitterAlg = state.bool("ob1_g5_use_transmitter_alg", true)
    val minimizeScanning = state.bool("ob1_minimize_scanning", false)

    SettingsCategory(context.getString(R.string.title_ob1_options)) {
        SwitchPref(state, "ob1_g5_use_transmitter_alg", context.getString(R.string.title_ob1_g5_use_transmitter_alg), default = true, subtitle = context.getString(R.string.summary_ob1_g5_use_transmitter_alg))
        SwitchPref(state, "ob1_g5_restart_sensor", context.getString(R.string.title_ob1_g5_restart_sensor), default = false, subtitle = context.getString(R.string.summary_ob1_g5_restart_sensor), enabled = useTransmitterAlg)
        SettingsActionRow(
            title = context.getString(R.string.title_ob1_g5_preemptive_restart),
            subtitle = context.getString(R.string.collection_summary_ob1_g5_preemptive_restart),
            onClick = { onNavigate(SettingsScreen.PreemptiveRestart) },
            enabled = useTransmitterAlg,
            modifier = Modifier.testTag("setting_preemptive_restart"),
        )
        SwitchPref(state, "ob1_g5_use_insufficiently_calibrated", context.getString(R.string.title_ob1_g5_use_insufficiently_calibrated), default = true, subtitle = context.getString(R.string.summary_ob1_g5_use_insufficiently_calibrated), enabled = useTransmitterAlg)
        SwitchPref(state, "ob1_minimize_scanning", context.getString(R.string.title_ob1_minimize_scanning), default = false, subtitle = context.getString(R.string.summary_ob1_minimize_scanning))
        SwitchPref(state, "ob1_avoid_scanning", context.getString(R.string.title_ob1_avoid_scanning), default = false, subtitle = context.getString(R.string.summary_ob1_avoid_scanning), enabled = minimizeScanning)
        SwitchPref(state, "ob1_g5_allow_resetbond", context.getString(R.string.title_ob1_g5_allow_resetbond), default = false, subtitle = context.getString(R.string.summary_ob1_g5_allow_resetbond))
        SwitchPref(state, "ob1_special_pairing_workaround", context.getString(R.string.special_pairing_workaround), default = false, subtitle = "Some Samsung devices can have an error where they lose the pairing information. This attempts to work around the issue.")
        EditPref(state, "dex_specified_slot", context.getString(R.string.title_dex_specified_slot), default = "", subtitle = context.getString(R.string.summary_dex_specified_slot), numeric = true, enabled = engineering)
    }
}

@Composable
internal fun PreemptiveRestartScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val useTransmitterAlg = state.bool("ob1_g5_use_transmitter_alg", true)
    val enabled = state.bool("ob1_g5_preemptive_restart", false)
    SettingsCategory(context.getString(R.string.title_ob1_g5_preemptive_restart)) {
        SwitchPref(state, "ob1_g5_preemptive_restart", context.getString(R.string.title_ob1_g5_preemptive_restart), default = false, subtitle = context.getString(R.string.summary_ob1_g5_preemptive_restart), enabled = useTransmitterAlg)
        SwitchPref(state, "ob1_g5_preemptive_restart_alert", context.getString(R.string.title_ob1_g5_preemptive_restart_alert), default = true, subtitle = context.getString(R.string.summary_ob1_g5_preemptive_restart_alert), enabled = enabled)
        SwitchPref(state, "ob1_g5_preemptive_restart_extended_time_travel", context.getString(R.string.title_ob1_g5_preemptive_restart_extended_time_travel), default = false, subtitle = context.getString(R.string.summary_ob1_g5_preemptive_restart_extended_time_travel), enabled = enabled)
        SwitchPref(state, "ob1_g5_defer_preemptive_restart_all_firmwares", context.getString(R.string.title_ob1_g5_preemptive_restart_extended_time_travel_all_firmwares), default = false, subtitle = context.getString(R.string.summary_ob1_g5_preemptive_restart_extended_time_travel_all_firmwares), enabled = enabled)
    }
}

/* ------------------------------------------------------ Per-device bounded settings screens */

@Composable
internal fun DexcomDeviceScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val ct = SettingsVisibility.collectionType(state)

    SettingsCategory("Dexcom") {
        if (ct == DexCollectionType.DexcomShare) {
            EditPref(
                state = state,
                key = "share_key",
                title = context.getString(R.string.enter_ten_character_dexcom_receiver_serial),
                default = "SM00000000",
                tag = "setting_share_key",
                onValueChange = {
                    Pref.removeItem("dexcom_share_session_id")
                    state.setString("share_key", it)
                },
            )
            SettingsActionRow(
                title = context.getString(R.string.scan_share2_barcode),
                subtitle = context.getString(R.string.pref_share2_scan_barcode_summary),
                onClick = { (context as? Activity)?.let { AndroidBarcode(it).scan() } },
                modifier = Modifier.testTag("setting_scan_share2_barcode"),
            )
        }
        if (ct == DexCollectionType.DexcomG5) {
            EditPref(
                state = state,
                key = "dex_txid",
                title = context.getString(R.string.dexcom_transmitter_id),
                subtitle = context.getString(R.string.transmitter_id),
                default = "ABCDEF",
                tag = "setting_dex_txid",
            )
            SettingsActionRow(
                title = context.getString(R.string.g5_debug_settings),
                subtitle = context.getString(R.string.advanced_g5_settings),
                icon = Icons.Outlined.BugReport,
                onClick = { onNavigate(SettingsScreen.G5Debug) },
                modifier = Modifier.testTag("setting_g5_debug"),
            )
            SettingsActionRow(
                title = context.getString(R.string.title_ob1_g5_preemptive_restart),
                subtitle = context.getString(R.string.collection_summary_ob1_g5_preemptive_restart),
                icon = Icons.Outlined.RestartAlt,
                onClick = { onNavigate(SettingsScreen.PreemptiveRestart) },
                enabled = state.bool("ob1_g5_use_transmitter_alg", true),
                modifier = Modifier.testTag("setting_preemptive_restart"),
            )
        }
    }

    if (ct == DexCollectionType.DexcomG5 && SettingsVisibility.bestCollectorHardwareName() != "G7") {
        SettingsCategory(context.getString(R.string.title_g5g6_battery_options)) {
            SettingsEditTextRow(
                title = context.getString(R.string.title_g5_battery_warning_level),
                value = state.string("g5-battery-warning-level", "300"),
                numeric = true,
                onValueChange = {
                    state.setString("g5-battery-warning-level", it)
                    G5BaseService.resetTransmitterBatteryStatus()
                    SdcardImportExport.hardReset()
                },
                modifier = Modifier.testTag("setting_g5_battery_warning"),
            )
        }
    }
}

@Composable
internal fun LibreDeviceScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    SettingsCategory("Libre / NFC") {
        SettingsActionRow(
            title = context.getString(R.string.nfc_scan_features),
            subtitle = context.getString(R.string.nfc_options),
            icon = Icons.Outlined.Nfc,
            onClick = { onNavigate(SettingsScreen.NfcSettings) },
            modifier = Modifier.testTag("setting_nfc"),
        )
        if (SettingsVisibility.isLibreReceiver()) {
            SettingsActionRow(
                title = context.getString(R.string.title_advanced_settings_4_Lib2),
                onClick = { onNavigate(SettingsScreen.Libre2Settings) },
                modifier = Modifier.testTag("setting_libre2"),
            )
        }
    }
}

@Composable
internal fun MedtrumDeviceScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory("Medtrum") {
        SwitchPref(state, "medtrum_use_native", context.getString(R.string.title_medtrum_use_native), default = true, subtitle = context.getString(R.string.summary_medtrum_use_native))
        EditPref(state, "medtrum_a_hex", context.getString(R.string.title_medtrum_a_hex), default = "", subtitle = context.getString(R.string.summary_medtrum_a_hex))
    }
}

@Composable
internal fun ILetDeviceScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()

    SettingsCategory("iLet") {
        Text(
            "Read-only. This is not medical software and cannot deliver insulin or change " +
                "therapy. Credentials are stored encrypted on this device and never logged.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        SwitchPref(
            state = state,
            key = IletPrefs.ENABLED,
            title = "Enable iLet",
            subtitle = "Read pump data; off by default",
            onCheckedChange = { enabled ->
                state.setBool(IletPrefs.ENABLED, enabled)
                if (enabled) ILetEntry.startIfEnabled(context) else ILetEntry.stop(context)
            },
            tag = "setting_ilet_enabled",
        )

        EditPref(
            state = state,
            key = IletPrefs.LAST_ADDRESS,
            title = "Pump Bluetooth address",
            default = "",
            subtitle = "Optional. Leave blank to scan for the iLet on first connection.",
            tag = "setting_ilet_address",
        )

        val serial = IletPrefs.deviceSerial()
        val address = state.string(IletPrefs.LAST_ADDRESS, "")
        Text(
            buildString {
                append("Pump serial: ")
                append(serial.ifEmpty { "detected on first connection" })
                append("\nBluetooth address: ")
                append(address.ifEmpty { "scan on first connection" })
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        SwitchPref(state, IletPrefs.DOWNLOAD_GLUCOSE, "Use iLet as glucose source", default = true, tag = "setting_ilet_glucose")
        SwitchPref(state, IletPrefs.DOWNLOAD_BOLUSES, "Download boluses", default = true, tag = "setting_ilet_boluses")
        SwitchPref(state, IletPrefs.DOWNLOAD_MEALS, "Download meals", default = true, tag = "setting_ilet_meals")
        SwitchPref(state, IletPrefs.DOWNLOAD_BASAL, "Download basal", default = true, tag = "setting_ilet_basal")
        SwitchPref(
            state,
            IletPrefs.SHOW_PUMP_IOB_LINE,
            "Show pump IOB line",
            default = false,
            subtitle = "Separate from xDrip's treatment-derived IOB",
            tag = "setting_ilet_iob_line",
        )

        SettingsActionRow(
            title = "iLet account",
            subtitle = "Sign in, show status, reset credentials",
            icon = Icons.Outlined.AccountCircle,
            onClick = { context.startActivity(Intent(context, ILetLoginActivity::class.java)) },
            modifier = Modifier.testTag("setting_ilet_account"),
        )
    }
}

@Composable
internal fun BluetoothBridgeScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory("Bluetooth Bridge") {
        EditPref(
            state = state,
            key = "dex_txid",
            title = context.getString(R.string.dexcom_transmitter_id),
            subtitle = context.getString(R.string.transmitter_id),
            default = "ABCDEF",
            tag = "setting_dex_txid",
        )
        // Owned by the Data Source screen when the active collector uses WiFi; only render here
        // for the Bluetooth-only bridge so the pref is never shown on two screens at once.
        if (!SettingsVisibility.hasWifi()) {
            EditPref(
                state,
                "wifi_recievers_addresses",
                context.getString(R.string.list_of_receivers),
                default = "",
                tag = "setting_wifi_receivers",
            )
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun DataSourceScreenPreview() {
    XdripPreview { DataSourceScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun WebFollowScreenPreview() {
    XdripPreview { WebFollowScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun NfcSettingsScreenPreview() {
    XdripPreview { NfcSettingsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun NsFollowDownloadScreenPreview() {
    XdripPreview { NsFollowDownloadScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun G5DebugScreenPreview() {
    XdripPreview { G5DebugScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PreemptiveRestartScreenPreview() {
    XdripPreview { PreemptiveRestartScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun DexcomDeviceScreenPreview() {
    XdripPreview { DexcomDeviceScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun LibreDeviceScreenPreview() {
    XdripPreview { LibreDeviceScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MedtrumDeviceScreenPreview() {
    XdripPreview { MedtrumDeviceScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ILetDeviceScreenPreview() {
    XdripPreview { ILetDeviceScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun BluetoothBridgeScreenPreview() {
    XdripPreview { BluetoothBridgeScreen() }
}

// endregion
