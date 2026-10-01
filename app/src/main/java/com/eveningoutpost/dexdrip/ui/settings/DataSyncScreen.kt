package com.eveningoutpost.dexdrip.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.eveningoutpost.dexdrip.NightscoutBackfillActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.deposit.DepositActivity
import com.eveningoutpost.dexdrip.glucosemeter.BTGlucoseMeterActivity
import com.eveningoutpost.dexdrip.nocturne.NocturneConnectHelper
import com.eveningoutpost.dexdrip.tidepool.AuthFlowOut
import com.eveningoutpost.dexdrip.tidepool.TidepoolUploader
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.AndroidBarcode
import com.eveningoutpost.dexdrip.utils.DisplayQRCode
import com.eveningoutpost.dexdrip.utils.QrCodeFromFile

/**
 * S4 — Data Sync (`pref_data_sync.xml`).
 *
 * Auto configure, cloud upload (REST/Mongo/Influx/dexcom share/Tidepool/NightLite/Nocturne/Web
 * deposit) and glucose meters. Pref keys/types unchanged; QR actions and the Tidepool/Nocturne
 * listeners are reproduced as callbacks.
 */
@Composable
internal fun DataSyncScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    SettingsCategory(context.getString(R.string.data_sync)) {
        SettingsActionRow(
            title = context.getString(R.string.auto_configure_title),
            subtitle = context.getString(R.string.prefs_auto_config_summary),
            icon = Icons.Outlined.QrCodeScanner,
            onClick = { onNavigate(SettingsScreen.AutoConfig) },
            modifier = Modifier.testTag("setting_auto_config"),
        )
        SettingsActionRow(
            title = context.getString(R.string.cloud_upload),
            subtitle = context.getString(R.string.options_for_upload),
            icon = Icons.Outlined.CloudUpload,
            onClick = { onNavigate(SettingsScreen.CloudUpload) },
            modifier = Modifier.testTag("setting_cloud_upload"),
        )
    }
}

@Composable
internal fun AutoConfigScreen() {
    val context = LocalContext.current
    SettingsCategory(context.getString(R.string.auto_configure_title)) {
        SettingsActionRow(
            title = context.getString(R.string.auto_config_cam_title),
            subtitle = context.getString(R.string.auto_config_cam_summary),
            onClick = { (context as? Activity)?.let { AndroidBarcode(it).scan() } },
            modifier = Modifier.testTag("setting_auto_configure"),
        )
        SettingsActionRow(
            title = context.getString(R.string.auto_config_image_title),
            subtitle = context.getString(R.string.auto_config_image_summary),
            onClick = { (context as? Activity)?.let { QrCodeFromFile(it).scanFile() } },
            modifier = Modifier.testTag("setting_qr_from_file"),
        )
    }
}

@Composable
internal fun CloudUploadScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.cloud_upload)) {
        SettingsActionRow(title = context.getString(R.string.pref_title_api), subtitle = context.getString(R.string.pref_summary_api_enabled), onClick = { onNavigate(SettingsScreen.RestApi) })
        SettingsActionRow(title = context.getString(R.string.pref_title_mongodb), subtitle = context.getString(R.string.pref_summary_mongodb_enabled), onClick = { onNavigate(SettingsScreen.Mongo) })
        SettingsActionRow(title = context.getString(R.string.pref_title_influxdb), subtitle = context.getString(R.string.pref_summary_influxdb_enabled), onClick = { onNavigate(SettingsScreen.Influx) })
        SettingsActionRow(title = context.getString(R.string.dexcom_share_server_upload), subtitle = context.getString(R.string.upload_data_to_dex_servers), onClick = { onNavigate(SettingsScreen.DexcomUpload) })
        SettingsActionRow(title = context.getString(R.string.title_tidepool), subtitle = context.getString(R.string.summary_tidepool_upload_screen), onClick = { onNavigate(SettingsScreen.Tidepool) })
        if (SettingsVisibility.isEngineeringMode(state)) {
            SettingsActionRow(title = "Web Deposit", subtitle = "Simple batch API currently used for diagnostics", onClick = { onNavigate(SettingsScreen.WebDeposit) })
        }
        SettingsActionRow(title = "NightLite", subtitle = "Lightweight Nightscout compatible service intended for followers", onClick = { onNavigate(SettingsScreen.NightLite) })
        SettingsActionRow(title = context.getString(R.string.nocturne), subtitle = context.getString(R.string.nocturne_settings_summary), onClick = { onNavigate(SettingsScreen.Nocturne) })
    }
}

@Composable
internal fun RestApiScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("cloud_storage_api_enable", false)
    SettingsCategory(context.getString(R.string.pref_title_api)) {
        SwitchPref(state, "cloud_storage_api_enable", context.getString(R.string.pref_title_api_enabled), default = false, subtitle = context.getString(R.string.pref_summary_api_enabled))
        SwitchPref(state, "cloud_storage_api_use_mobile", context.getString(R.string.use_mobile_data), default = true, subtitle = context.getString(R.string.upload_even_when_using_mobile_data), enabled = enabled)
        SwitchPref(state, "cloud_storage_api_use_best_glucose", context.getString(R.string.send_display_glucose), default = false, subtitle = context.getString(R.string.use_plugins_for_brodcast), enabled = enabled)
        EditPref(state, "cloud_storage_api_base", context.getString(R.string.pref_title_api_url), default = context.getString(R.string.pref_default_api_url), subtitle = context.getString(R.string.pref_summary_api_url), enabled = enabled)
        SettingsActionRow(title = context.getString(R.string.title_cloud_storage_api_download_enable), onClick = { onNavigate(SettingsScreen.RestApiDownload) }, enabled = enabled)
        SwitchPref(state, "bluetooth_meter_for_calibrations_auto", context.getString(R.string.title_bluetooth_meter_for_calibrations_auto), default = false, subtitle = context.getString(R.string.summary_bluetooth_meter_for_calibrations_auto), enabled = state.bool("cloud_storage_api_download_enable", true))
        SettingsActionRow(title = context.getString(R.string.title_rest_api_extra_options), onClick = { onNavigate(SettingsScreen.RestApiExtra) })
    }
}

@Composable
internal fun RestApiDownloadScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.title_cloud_storage_api_download_enable)) {
        SwitchPref(state, "cloud_storage_api_download_enable", context.getString(R.string.title_cloud_storage_api_download_enable), default = true, subtitle = context.getString(R.string.summary_cloud_storage_api_download_enable), enabled = state.bool("cloud_storage_api_enable", false))
        SwitchPref(state, "cloud_storage_api_skip_download_from_xdrip", context.getString(R.string.title_cloud_storage_api_download_from_xdrip), default = true, subtitle = context.getString(R.string.summary_cloud_storage_api_download_from_xdrip), enabled = state.dependentEnabled("cloud_storage_api_download_enable", true))
    }
}

@Composable
internal fun RestApiExtraScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory(context.getString(R.string.title_rest_api_extra_options)) {
        SettingsActionRow(
            title = context.getString(R.string.title_send_sync_show_qr),
            subtitle = context.getString(R.string.summary_send_sync_show_qr),
            onClick = { startActivityWithAction(context, DisplayQRCode::class.java, "xdrip_nightscout_qr") },
            modifier = Modifier.testTag("setting_nightscout_qr"),
        )
        SwitchPref(state, "skip_lan_uploads_when_no_lan", context.getString(R.string.skip_lan_uploads), default = true, subtitle = context.getString(R.string.skip_local_lan))
        SwitchPref(state, "send_bridge_battery_to_nightscout", context.getString(R.string.title_send_bridge_battery_to_nightscout), default = true, subtitle = context.getString(R.string.summary_send_bridge_battery_to_nightscout))
        SwitchPref(state, "send_ob1dex_tx_battery_to_nightscout", context.getString(R.string.title_send_dexcom_transmitter_battery_to_nightscout), default = false, subtitle = context.getString(R.string.summary_send_dexcom_transmitter_battery_to_nightscout))
        SwitchPref(state, "send_treatments_to_nightscout", context.getString(R.string.title_send_treatments_to_nightscout), default = true, subtitle = context.getString(R.string.summary_send_treatments_to_nightscout))
        SwitchPref(state, "warn_nightscout_failures", context.getString(R.string.title_warn_nightscout_failures), default = true, subtitle = context.getString(R.string.summary_warn_nightscout_failures))
        SwitchPref(state, "nightscout_device_append_source_info", context.getString(R.string.title_nightscout_device_append_source_info), default = false, subtitle = context.getString(R.string.summary_nightscout_device_append_source_info))
        SwitchPref(state, "warn_nightscout_multi_site_upload_failure", context.getString(R.string.title_warn_nightscout_multi_site_upload_failure), default = true, subtitle = context.getString(R.string.summary_warn_nightscout_multi_site_upload_failure))
        SettingsActionRow(
            title = context.getString(R.string.title_back_fill_data),
            subtitle = context.getString(R.string.summary_tap_to_send_historical_data),
            onClick = { startActivityWithAction(context, NightscoutBackfillActivity::class.java, Intent.ACTION_MAIN) },
        )
    }
}

@Composable
internal fun MongoScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("cloud_storage_mongodb_enable", false)
    SettingsCategory(context.getString(R.string.pref_title_mongodb)) {
        SwitchPref(state, "cloud_storage_mongodb_enable", context.getString(R.string.enable_mongo_sync), default = false, subtitle = context.getString(R.string.pref_summary_mongodb_enabled))
        EditPref(state, "cloud_storage_mongodb_uri", context.getString(R.string.mongo_db_uri), default = context.getString(R.string.pref_default_mongodb_uri), enabled = enabled)
        EditPref(state, "cloud_storage_mongodb_collection", context.getString(R.string.pref_title_mongodb_collection), default = context.getString(R.string.pref_default_mongodb_collection), enabled = enabled)
        EditPref(state, "cloud_storage_mongodb_device_status_collection", context.getString(R.string.pref_title_mongodb_device_status_collection), default = context.getString(R.string.pref_default_mongodb_device_status_collection), enabled = enabled)
        SwitchPref(state, "skip_lan_uploads_when_no_lan", context.getString(R.string.skip_lan_uploads), default = true, subtitle = context.getString(R.string.skip_local_lan))
        SwitchPref(state, "mongo_load_transmitter_data", context.getString(R.string.mongo_load_transmitter_data_title), default = false, subtitle = context.getString(R.string.mongo_load_transmitter_data_summary))
    }
}

@Composable
internal fun InfluxScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("cloud_storage_influxdb_enable", false)
    SettingsCategory(context.getString(R.string.pref_title_influxdb)) {
        SwitchPref(state, "cloud_storage_influxdb_enable", context.getString(R.string.influxdb_sync), default = false, subtitle = context.getString(R.string.pref_summary_influxdb_enabled))
        EditPref(state, "cloud_storage_influxdb_uri", context.getString(R.string.influxdb_uri), default = context.getString(R.string.pref_default_influxdb_uri), enabled = enabled)
        EditPref(state, "cloud_storage_influxdb_database", context.getString(R.string.influxdb_database_name), default = context.getString(R.string.pref_default_influxdb_database), enabled = enabled)
        EditPref(state, "cloud_storage_influxdb_username", context.getString(R.string.user), default = context.getString(R.string.pref_default_influxdb_username), enabled = enabled)
        EditPref(state, "cloud_storage_influxdb_password", context.getString(R.string.password), default = context.getString(R.string.pref_default_influxdb_password), enabled = enabled)
        SwitchPref(state, "skip_lan_uploads_when_no_lan", context.getString(R.string.skip_lan_uploads), default = true, subtitle = context.getString(R.string.skip_local_lan))
    }
}

@Composable
internal fun DexcomUploadScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("share_upload", false)
    SettingsCategory(context.getString(R.string.dexcom_share_server_upload)) {
        SwitchPref(state, "share_upload", context.getString(R.string.upload_to_dexcom_share), default = false, subtitle = context.getString(R.string.upload_to_dexcom))
        SwitchPref(
            state,
            "dex_share_us_acct",
            context.getString(R.string.usa_based_account),
            default = true,
            subtitle = if (state.bool("dex_share_us_acct", true)) context.getString(R.string.accounts_inside_usa) else context.getString(R.string.accounts_outside_usa),
        )
        EditPref(state, "dexcom_account_name", context.getString(R.string.dexcom_user), default = "", subtitle = context.getString(R.string.dexcom_login), enabled = enabled)
        EditPref(state, "dexcom_account_password", context.getString(R.string.dexcom_password), default = "", subtitle = context.getString(R.string.dexcom_login_password), enabled = enabled)
    }
}

@Composable
internal fun TidepoolScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("cloud_storage_tidepool_enable", false)
    SettingsCategory(context.getString(R.string.title_tidepool)) {
        SwitchPref(state, "cloud_storage_tidepool_enable", context.getString(R.string.title_sync_to_tidepool), default = false, subtitle = context.getString(R.string.summary_tidepool_upload_screen))
        EditPref(
            state,
            "tidepool_username",
            context.getString(R.string.title_tidepool_username),
            default = "",
            subtitle = context.getString(R.string.summary_tidepool_username),
            onValueChange = { newValue ->
                TidepoolUploader.resetInstance()
                if (newValue != Pref.getString("tidepool_username", "")) {
                    Pref.setString("tidepool_username", newValue)
                    AuthFlowOut.doTidePoolInitialLogin(true)
                }
            },
        )
        EditPref(
            state,
            "tidepool_password",
            context.getString(R.string.title_tidepool_password),
            default = "",
            subtitle = context.getString(R.string.summary_tidepool_password),
            onValueChange = {
                Pref.setString("tidepool_password", it)
                TidepoolUploader.resetInstance()
                AuthFlowOut.doTidePoolInitialLogin(true)
            },
        )
        SettingsActionRow(
            title = context.getString(R.string.title_tidepool_test_login),
            onClick = { AuthFlowOut.doTidePoolInitialLogin() },
            modifier = Modifier.testTag("setting_tidepool_test_login"),
        )
        SettingsSliderRow(
            title = context.getString(R.string.title_tidepool_window_latency),
            value = state.int("tidepool_window_latency", 0),
            valueRange = 0..300,
            enabled = enabled,
            onValueChange = { state.setInt("tidepool_window_latency", it) },
        )
        SwitchPref(state, "tidepool_dev_servers", context.getString(R.string.title_tidepool_dev_servers), default = false, subtitle = context.getString(R.string.summary_tidepool_dev_servers), onCheckedChange = { TidepoolUploader.resetInstance(); state.setBool("tidepool_dev_servers", it) })
        SwitchPref(state, "tidepool_only_while_charging", context.getString(R.string.title_tidepool_upload_when_chargeonly), default = false, subtitle = context.getString(R.string.summary_tidepool_upload_when_chargeonly))
        SwitchPref(state, "tidepool_only_while_unmetered", context.getString(R.string.title_tidepool_upload_only_wifi), default = false, subtitle = context.getString(R.string.summary_tidepool_upload_only_wifi))
        SwitchPref(state, "tidepool_no_treatments", context.getString(R.string.title_tidepool_upload_no_treatment), default = false, subtitle = context.getString(R.string.summary_tidepool_upload_no_treatment))
    }
}

@Composable
internal fun WebDepositScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory("Web Deposit") {
        EditPref(state, "web_deposit_url", "Web Deposit URL", default = "http://192.168.0.5:2080/")
        EditPref(state, "web_deposit_serial", "Serial Information", default = "ABC123")
        SettingsActionRow(
            title = "Start Web Deposit",
            onClick = { startActivityWithAction(context, DepositActivity::class.java, Intent.ACTION_MAIN) },
        )
    }
}

@Composable
internal fun NightLiteScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory("NightLite") {
        SwitchPref(state, "nightlite-enabled", "Upload to NightLite", default = false, subtitle = "Whether to upload to the remote service")
        EditPref(state, "nightlite-api-url", "NightLite URL", default = "")
        SettingsActionRow(
            title = context.getString(R.string.title_send_sync_show_qr),
            subtitle = context.getString(R.string.summary_send_sync_show_qr),
            onClick = { startActivityWithAction(context, DisplayQRCode::class.java, "xdrip_nightlite_qr") },
            enabled = state.bool("nightlite-enabled", false),
        )
    }
}

@Composable
internal fun NocturneScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val enabled = state.bool("nocturne_upload_enable", false)
    SettingsCategory(context.getString(R.string.nocturne)) {
        SwitchPref(state, "nocturne_upload_enable", context.getString(R.string.title_nocturne_upload_enable), default = false, subtitle = context.getString(R.string.nocturne_settings_summary))
        EditPref(state, "nocturne_instance_url", context.getString(R.string.title_nocturne_instance_url), default = "", enabled = enabled)
        SettingsActionRow(
            title = context.getString(R.string.nocturne_connect_activity_label),
            subtitle = context.getString(R.string.nocturne_connect_pref_summary),
            onClick = { (context as? Activity)?.let { NocturneConnectHelper.startConnectFlow(it) } },
            enabled = enabled,
            modifier = Modifier.testTag("setting_nocturne_connect"),
        )
        SwitchPref(state, "nocturne_upload_sgv", context.getString(R.string.title_nocturne_upload_sgv), default = true, subtitle = context.getString(R.string.summary_nocturne_upload_sgv), enabled = enabled)
        SwitchPref(state, "nocturne_upload_treatments", context.getString(R.string.title_nocturne_upload_treatments), default = false, subtitle = context.getString(R.string.summary_nocturne_upload_treatments), enabled = enabled)
        SwitchPref(state, "nocturne_upload_calibrations", context.getString(R.string.title_nocturne_upload_calibrations), default = false, subtitle = context.getString(R.string.summary_nocturne_upload_calibrations), enabled = enabled)
        SwitchPref(state, "nocturne_upload_bloodtests", context.getString(R.string.title_nocturne_upload_bloodtests), default = false, subtitle = context.getString(R.string.summary_nocturne_upload_bloodtests), enabled = enabled)
        SwitchPref(state, "nocturne_upload_heartrate", context.getString(R.string.title_nocturne_upload_heartrate), default = false, subtitle = context.getString(R.string.summary_nocturne_upload_heartrate), enabled = enabled)
        SwitchPref(state, "nocturne_upload_stepcount", context.getString(R.string.title_nocturne_upload_stepcount), default = false, subtitle = context.getString(R.string.summary_nocturne_upload_stepcount), enabled = enabled)
        SwitchPref(state, "nocturne_upload_devicestatus", context.getString(R.string.title_nocturne_upload_devicestatus), default = false, subtitle = context.getString(R.string.summary_nocturne_upload_devicestatus), enabled = enabled)
        SwitchPref(state, "nocturne_upload_motion", context.getString(R.string.title_nocturne_upload_motion), default = false, subtitle = context.getString(R.string.summary_nocturne_upload_motion), enabled = enabled)
        SwitchPref(state, "nocturne_use_mobile", context.getString(R.string.title_nocturne_use_mobile), default = true, subtitle = context.getString(R.string.summary_nocturne_use_mobile), enabled = enabled)
    }
}

@Composable
internal fun GlucoseMetersScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val btEnabled = state.bool("bluetooth_meter_enabled", false)
    SettingsCategory(context.getString(R.string.glucose_meters)) {
        SwitchPref(state, "nfc_meter_enabled", context.getString(R.string.title_use_nfc_meter), default = false, subtitle = context.getString(R.string.summary_use_nfc_meter))
        SwitchPref(state, "bluetooth_meter_enabled", context.getString(R.string.use_bluetooth_meter), default = false, subtitle = context.getString(R.string.auto_connect_to_meter))
        SettingsActionRow(
            title = context.getString(R.string.scan_for_bluetooth_meter),
            subtitle = Pref.getString("selected_bluetooth_meter_info", ""),
            onClick = { startActivityWithAction(context, BTGlucoseMeterActivity::class.java, Intent.ACTION_MAIN) },
        )
        SwitchPref(state, "bluetooth_meter_play_sounds", context.getString(R.string.meter_connect_sound_effect), default = true, subtitle = context.getString(R.string.meter_connect_sound))
        SwitchPref(state, "bluetooth_meter_for_calibrations", context.getString(R.string.use_meter_for_calibrations), default = false, subtitle = context.getString(R.string.ask_for_calibration), enabled = btEnabled)
        SwitchPref(state, "bluetooth_meter_for_calibrations_auto", context.getString(R.string.automatic_calibration), default = false, subtitle = context.getString(R.string.auto_calibration_good_conditions), enabled = btEnabled)
    }
}

private fun startActivityWithAction(context: Context, clazz: Class<*>, action: String?) {
    context.startActivity(Intent(action).setClass(context, clazz))
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun DataSyncScreenPreview() {
    XdripPreview { DataSyncScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AutoConfigScreenPreview() {
    XdripPreview { AutoConfigScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun CloudUploadScreenPreview() {
    XdripPreview { CloudUploadScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun RestApiScreenPreview() {
    XdripPreview { RestApiScreen(onNavigate = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun RestApiDownloadScreenPreview() {
    XdripPreview { RestApiDownloadScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun RestApiExtraScreenPreview() {
    XdripPreview { RestApiExtraScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MongoScreenPreview() {
    XdripPreview { MongoScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun InfluxScreenPreview() {
    XdripPreview { InfluxScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun DexcomUploadScreenPreview() {
    XdripPreview { DexcomUploadScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun TidepoolScreenPreview() {
    XdripPreview { TidepoolScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun WebDepositScreenPreview() {
    XdripPreview { WebDepositScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun NightLiteScreenPreview() {
    XdripPreview { NightLiteScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun NocturneScreenPreview() {
    XdripPreview { NocturneScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun GlucoseMetersScreenPreview() {
    XdripPreview { GlucoseMetersScreen() }
}

// endregion
