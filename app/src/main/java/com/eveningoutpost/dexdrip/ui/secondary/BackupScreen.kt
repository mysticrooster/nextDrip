@file:JvmName("BackupScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import androidx.databinding.ObservableMap
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.cloud.backup.Backup
import com.eveningoutpost.dexdrip.cloud.backup.BackupActivity
import com.eveningoutpost.dexdrip.ui.settings.SettingsSwitchRow
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

private val META_KEYS = listOf("selectedLocation", "stext", "lastBackupTime", "lastAgoTime", "lastDevice")

/**
 * Track V (rich Medium) — `BackupActivity`: SAF/Google Drive backup picker. Keeps the activity's
 * `ViewModel` (also the `BackupStatus` sink) and bridges its `ObservableField`s and metadata
 * `ObservableArrayMap` into Compose; the SAF/Drive dialogs and result flows stay in Java.
 * Java entry point: [installBackup].
 */
fun installBackup(activity: BackupActivity) {
    activity.setContent {
        val vm = activity.viewModel
        val prefs = activity.prefs

        var status by remember { mutableStateOf(vm.status.get() ?: "Status") }
        var idle by remember { mutableStateOf(vm.idle.get() ?: true) }
        var showAuto by remember { mutableStateOf(vm.showAuto.get() ?: false) }
        var metaData by remember { mutableStateOf(vm.map.toMap()) }
        var automaticEnabled by remember { mutableStateOf(prefs.getbool(Backup.PREF_AUTO_BACKUP)) }
        var automaticMobile by remember { mutableStateOf(prefs.getbool(Backup.PREF_AUTO_BACKUP_MOBILE + ":true")) }

        DisposableEffect(activity) {
            val statusCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    status = vm.status.get() ?: "Status"
                }
            }
            vm.status.addOnPropertyChangedCallback(statusCallback)

            val idleCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    idle = vm.idle.get() ?: true
                }
            }
            vm.idle.addOnPropertyChangedCallback(idleCallback)

            val showAutoCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    showAuto = vm.showAuto.get() ?: false
                }
            }
            vm.showAuto.addOnPropertyChangedCallback(showAutoCallback)

            val mapCallback = object : ObservableMap.OnMapChangedCallback<ObservableMap<String, String>, String, String>() {
                override fun onMapChanged(sender: ObservableMap<String, String>?, key: String?) {
                    metaData = vm.map.toMap()
                }
            }
            vm.map.addOnMapChangedCallback(mapCallback)

            val prefsCallback = object : ObservableMap.OnMapChangedCallback<ObservableMap<String, Boolean>, String, Boolean>() {
                override fun onMapChanged(sender: ObservableMap<String, Boolean>?, key: String?) {
                    when (key) {
                        Backup.PREF_AUTO_BACKUP -> automaticEnabled = prefs.getbool(Backup.PREF_AUTO_BACKUP)
                        Backup.PREF_AUTO_BACKUP_MOBILE -> automaticMobile = prefs.getbool(Backup.PREF_AUTO_BACKUP_MOBILE + ":true")
                    }
                }
            }
            prefs.addOnMapChangedCallback(prefsCallback)

            onDispose {
                vm.status.removeOnPropertyChangedCallback(statusCallback)
                vm.idle.removeOnPropertyChangedCallback(idleCallback)
                vm.showAuto.removeOnPropertyChangedCallback(showAutoCallback)
                vm.map.removeOnMapChangedCallback(mapCallback)
                prefs.removeOnMapChangedCallback(prefsCallback)
            }
        }

        BackupScreen(
            status = status,
            idle = idle,
            showAuto = showAuto,
            metaData = metaData,
            automaticEnabled = automaticEnabled,
            automaticMobile = automaticMobile,
            onSelectFile = { vm.selectFile() },
            onBackupNow = { vm.backupNow() },
            onRestoreNow = { vm.restoreNow() },
            onAutomaticEnabledChange = { prefs.setbool(Backup.PREF_AUTO_BACKUP, it) },
            onAutomaticMobileChange = { prefs.setbool(Backup.PREF_AUTO_BACKUP_MOBILE + ":true", it) },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun BackupScreen(
    status: String,
    idle: Boolean,
    showAuto: Boolean,
    metaData: Map<String, String>,
    automaticEnabled: Boolean,
    automaticMobile: Boolean,
    onSelectFile: () -> Unit,
    onBackupNow: () -> Unit,
    onRestoreNow: () -> Unit,
    onAutomaticEnabledChange: (Boolean) -> Unit,
    onAutomaticMobileChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    SecondaryScreen(title = context.getString(R.string.cloud_backup), onBack = onBack) {
        Text(
            text = context.getString(R.string.you_can_use_google_drive_for_backup),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        )
        META_KEYS.forEach { key ->
            val value = metaData[key].orEmpty()
            if (value.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .testTag("backup_row_$key"),
                ) {
                    Text(
                        text = metaData[key + "String"].orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 5.dp),
                    )
                    Text(":", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 5.dp),
                    )
                }
                HorizontalDivider()
            }
        }
        Text(
            text = status,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .testTag("backup_status"),
        )
        if (!idle) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("backup_progress"),
            )
        }
        Button(
            onClick = onSelectFile,
            enabled = idle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("backup_select"),
        ) {
            Text(context.getString(R.string.select_backup_location))
        }
        Button(
            onClick = onBackupNow,
            enabled = idle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("backup_now"),
        ) {
            Text(context.getString(R.string.do_backup_now))
        }
        if (showAuto) {
            SettingsSwitchRow(
                title = context.getString(R.string.automatic_daily_backup),
                checked = automaticEnabled,
                onCheckedChange = onAutomaticEnabledChange,
                modifier = Modifier.testTag("backup_auto"),
            )
            SettingsSwitchRow(
                title = context.getString(R.string.daily_backup_even_mobile),
                checked = automaticMobile,
                onCheckedChange = onAutomaticMobileChange,
                enabled = automaticEnabled,
                modifier = Modifier.testTag("backup_auto_mobile"),
            )
        }
        Button(
            onClick = onRestoreNow,
            enabled = idle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("backup_restore"),
        ) {
            Text(context.getString(R.string.restore_from_backup))
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun BackupScreenPreview() {
    XdripPreview {
        BackupScreen(
            status = "Idle",
            idle = true,
            showAuto = true,
            metaData = mapOf("Last backup" to "never"),
            automaticEnabled = false,
            automaticMobile = false,
            onSelectFile = {},
            onBackupNow = {},
            onRestoreNow = {},
            onAutomaticEnabledChange = {},
            onAutomaticMobileChange = {},
            onBack = {},
        )
    }
}

// endregion
