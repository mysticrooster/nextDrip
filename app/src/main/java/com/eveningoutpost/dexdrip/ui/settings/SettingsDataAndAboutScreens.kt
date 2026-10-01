package com.eveningoutpost.dexdrip.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.HelpActivity
import com.eveningoutpost.dexdrip.ImportDatabaseActivity
import com.eveningoutpost.dexdrip.LicenseAgreementActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.cloud.backup.BackupActivity
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utilitymodels.SendFeedBack
import com.eveningoutpost.dexdrip.utilitymodels.UpdateActivity
import com.eveningoutpost.dexdrip.utils.DatabaseUtil
import com.eveningoutpost.dexdrip.utils.DisplayQRCode
import com.eveningoutpost.dexdrip.utils.SdcardImportExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * IA redesign — the new `Your Data → Backups`, `Appearance → Home screen` and `About` screens.
 *
 * Preference keys/types are unchanged; the backup/export actions reproduce the legacy Home
 * overflow handlers (permission request, background database export, feedback intents).
 */

@Composable
internal fun BackupsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) pending?.invoke()
        pending = null
    }
    val withStoragePermission: (() -> Unit) -> Unit = { action ->
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            pending = action
            permissionLauncher.launch(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
            )
        }
    }

    SettingsCategory("Backups") {
        SettingsActionRow(
            title = context.getString(R.string.cloud_backup),
            subtitle = "Back up your data to the cloud",
            icon = Icons.Outlined.CloudDone,
            onClick = { JoH.startActivity(BackupActivity::class.java) },
            modifier = Modifier.testTag("setting_cloud_backup"),
        )
        SettingsActionRow(
            title = context.getString(R.string.share_config_via_qr_code),
            icon = Icons.Outlined.QrCode,
            onClick = { context.startActivity(Intent(context, DisplayQRCode::class.java)) },
            modifier = Modifier.testTag("setting_share_config"),
        )
        SettingsActionRow(
            title = context.getString(R.string.menu_export_database),
            subtitle = "Export the database to a zip file",
            icon = Icons.Outlined.Download,
            onClick = {
                withStoragePermission {
                    scope.launch(Dispatchers.IO) {
                        val filename = DatabaseUtil.saveSql(context)
                        withContext(Dispatchers.Main) {
                            if (filename != null) {
                                JoH.static_toast_long(context.getString(R.string.exported_to) + filename)
                                // Match the legacy handler: also back the settings up to the SD card.
                                context.startActivity(
                                    Intent(context, SdcardImportExport::class.java)
                                        .putExtra("backup", "now")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            } else {
                                JoH.static_toast_long(context.getString(R.string.could_not_export_database))
                            }
                        }
                    }
                }
            },
            modifier = Modifier.testTag("setting_export_database"),
        )
        SettingsActionRow(
            title = context.getString(R.string.menu_import_db),
            subtitle = "Restore the database from a previously exported file",
            icon = Icons.Outlined.Upload,
            onClick = { context.startActivity(Intent(context, ImportDatabaseActivity::class.java)) },
            modifier = Modifier.testTag("setting_import_database"),
        )
        SettingsActionRow(
            title = context.getString(R.string.menu_export_csv_sidiary),
            subtitle = "Export readings as CSV for SiDiary",
            icon = Icons.Outlined.Description,
            onClick = {
                withStoragePermission {
                    scope.launch(Dispatchers.IO) {
                        val from = Pref.getLong("sidiary_last_exportdate", 0)
                        val filename = DatabaseUtil.saveCSV(context, from)
                        if (filename != null) Pref.setLong("sidiary_last_exportdate", System.currentTimeMillis())
                        withContext(Dispatchers.Main) {
                            val message = if (filename != null) {
                                context.getString(R.string.exported_to) + filename
                            } else {
                                context.getString(R.string.could_not_export_csv_)
                            }
                            JoH.static_toast_long(message)
                        }
                    }
                }
            },
            modifier = Modifier.testTag("setting_export_csv"),
        )
        SettingsActionRow(
            title = context.getString(R.string.load_save_settings_on_sdcard),
            subtitle = "Load or save settings to the SD card",
            icon = Icons.Outlined.FolderOpen,
            onClick = { context.startActivity(Intent(context, SdcardImportExport::class.java)) },
            modifier = Modifier.testTag("setting_settings_sd"),
        )
    }
}

@Composable
internal fun AboutScreen(onNavigate: (SettingsScreen) -> Unit, onOpenClassic: () -> Unit) {
    val context = LocalContext.current
    SettingsCategory("About") {
        SettingsActionRow(
            title = "Version",
            subtitle = "App version and build information",
            icon = Icons.Outlined.Numbers,
            onClick = { onNavigate(SettingsScreen.Version) },
            modifier = Modifier.testTag("setting_version"),
        )
        SettingsActionRow(
            title = context.getString(R.string.check_for_updated_version),
            icon = Icons.Outlined.SystemUpdate,
            onClick = {
                (context as? Activity)?.let {
                    UpdateActivity.last_check_time = -1
                    UpdateActivity.checkForAnUpdate(it, true)
                }
            },
            modifier = Modifier.testTag("setting_check_update"),
        )
        SettingsActionRow(
            title = context.getString(R.string.send_feedback_to_developer),
            icon = Icons.Outlined.Email,
            onClick = { context.startActivity(Intent(context, SendFeedBack::class.java)) },
            modifier = Modifier.testTag("setting_send_feedback"),
        )
        SettingsActionRow(
            title = context.getString(R.string.show_help),
            icon = Icons.Outlined.HelpOutline,
            onClick = { context.startActivity(Intent(context, HelpActivity::class.java)) },
            modifier = Modifier.testTag("setting_help"),
        )
        SettingsActionRow(
            title = context.getString(R.string.crowd_sourced_translation),
            icon = Icons.Outlined.Translate,
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://crowdin.com/project/xdrip")))
                }.onFailure {
                    JoH.static_toast_long("Unable to open web browser for crowdin.com/project/xdrip")
                }
            },
            modifier = Modifier.testTag("setting_crowd_translate"),
        )
        SettingsActionRow(
            title = context.getString(R.string.end_user_license_agreement),
            subtitle = context.getString(R.string.not_for_medical_use),
            icon = Icons.Outlined.Description,
            onClick = { context.startActivity(Intent(context, LicenseAgreementActivity::class.java)) },
            modifier = Modifier.testTag("setting_license"),
        )
        SettingsActionRow(
            title = "Classic settings",
            subtitle = "Screens not yet migrated to the new UI",
            icon = Icons.Outlined.Settings,
            onClick = onOpenClassic,
            modifier = Modifier.testTag("setting_classic"),
        )
    }
}

@Composable
internal fun VersionScreen() {
    val context = LocalContext.current
    SettingsCategory("Version") {
        SettingsInfoRow(title = "Version name", value = BuildConfig.VERSION_NAME, modifier = Modifier.testTag("setting_version_name"))
        SettingsInfoRow(title = "Version code", value = BuildConfig.VERSION_CODE.toString())
        SettingsInfoRow(title = "Build type", value = BuildConfig.BUILD_TYPE)
        SettingsInfoRow(title = "Flavour", value = BuildConfig.FLAVOR)
        SettingsInfoRow(title = "Target SDK", value = BuildConfig.targetSDK.toString())
        SettingsActionRow(
            title = "Classic settings",
            subtitle = "Screens not yet migrated to the new UI",
            icon = Icons.Outlined.Settings,
            onClick = { context.startActivity(Intent(context, com.eveningoutpost.dexdrip.utils.Preferences::class.java)) },
            modifier = Modifier.testTag("setting_version_classic"),
        )
    }
}

@Composable
internal fun HomeScreenSettingsScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()
    SettingsCategory("Home screen") {
        SwitchPref(state, "home-shelf-time_buttons", "Show time buttons", default = false, tag = "setting_home_time_buttons")
        SwitchPref(state, "home-shelf-time_locked_always", "Locked time period always used", default = false)
        SwitchPref(state, "home-shelf-chart_preview", "Show chart preview", default = true, tag = "setting_home_chart_preview")
        SwitchPref(state, "home-shelf-source_wizard", "Source wizard button", default = false)
        SwitchPref(state, "home-shelf-collector_nano_status", "Show collector status", default = true)
        SwitchPref(state, "home-shelf-sensor_expiry", "Show sensor expiry", default = true)
        SwitchPref(state, "home-shelf-graphic_trend_arrow", "Show graphical trend arrow", default = false)
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun BackupsScreenPreview() {
    XdripPreview { BackupsScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AboutScreenPreview() {
    XdripPreview { AboutScreen(onNavigate = {}, onOpenClassic = {}) }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun VersionScreenPreview() {
    XdripPreview { VersionScreen() }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun HomeScreenSettingsScreenPreview() {
    XdripPreview { HomeScreenSettingsScreen() }
}

// endregion
