@file:JvmName("SdcardImportExportScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

/**
 * Track V (Data & admin) — `SdcardImportExport`: save/load/delete of the settings file. The activity
 * keeps all file/storage permission handling and the public static helpers used elsewhere.
 * Java entry point: [installSdcardImportExport].
 */
fun installSdcardImportExport(activity: com.eveningoutpost.dexdrip.utils.SdcardImportExport) {
    activity.setContent {
        SdcardImportExportScreen(
            onSave = { activity.savePreferencesToSD(null) },
            onLoad = { activity.loadPreferencesToSD(null) },
            onDelete = { activity.deletePreferencesOnSD(null) },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun SdcardImportExportScreen(
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    SecondaryScreen(title = "Import/Export Settings", onBack = onBack) {
        Text(
            text = context.getString(R.string.here_you_can_save_the_settings_to_the_external_storage_sdcard),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("sdcard_warning"),
        )
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("sdcard_save"),
        ) {
            Text(context.getString(R.string.save_all_settings_to_sdcard))
        }
        Button(
            onClick = onLoad,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("sdcard_load"),
        ) {
            Text(context.getString(R.string.load_all_settings_from_sdcard))
        }
        Button(
            onClick = onDelete,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("sdcard_delete"),
        ) {
            Text(context.getString(R.string.delete_any_settings_on_sdcard))
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SdcardImportExportScreenPreview() {
    XdripPreview {
        SdcardImportExportScreen(onSave = {}, onLoad = {}, onDelete = {}, onBack = {})
    }
}

// endregion
