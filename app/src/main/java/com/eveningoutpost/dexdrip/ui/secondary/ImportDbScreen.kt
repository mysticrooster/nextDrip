@file:JvmName("ImportDbScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.ImportDatabaseActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

/**
 * Track V (Data & admin) — `ImportDatabaseActivity`: restore-instructions gate, found-database list
 * and import result. The activity keeps permission handling, database discovery and the async
 * import/restore flow; the screen renders the state it observes through [ImportDatabaseActivity.tick].
 * Java entry point: [installImportDb].
 */
fun installImportDb(activity: ImportDatabaseActivity) {
    activity.setContent {
        var tick by remember { mutableStateOf(activity.tick.get() ?: 0) }
        DisposableEffect(activity) {
            val callback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    tick = activity.tick.get() ?: 0
                }
            }
            activity.tick.addOnPropertyChangedCallback(callback)
            onDispose { activity.tick.removeOnPropertyChangedCallback(callback) }
        }

        val names = remember(tick) { activity.databaseNamesSnapshot }
        val showWarning = remember(tick) { activity.showWarning.get() ?: true }
        val result = remember(tick) { activity.resultMessage.get() }

        ImportDbScreen(
            databaseNames = names,
            showWarning = showWarning,
            resultMessage = result,
            databaseNameAt = { activity.databaseNameAt(it) },
            onWarningOk = { activity.generateDBGui() },
            onImport = { position -> activity.importDB(position) },
            onResultOk = { activity.returnToHome() },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun ImportDbScreen(
    databaseNames: List<String>,
    showWarning: Boolean,
    resultMessage: String?,
    databaseNameAt: (Int) -> String,
    onWarningOk: () -> Unit,
    onImport: (Int) -> Unit,
    onResultOk: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var confirmPosition by remember { mutableStateOf<Int?>(null) }

    SecondaryScreen(title = ImportDatabaseActivity.menu_name, onBack = onBack) {
        when {
            showWarning -> {
                Text(
                    text = context.getString(R.string.import_db_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("import_db_warning"),
                )
                Button(
                    onClick = onWarningOk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("import_db_warning_ok"),
                ) {
                    Text(context.getString(R.string.ok))
                }
            }

            else -> databaseNames.forEachIndexed { index, name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { confirmPosition = index }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("import_db_item_$index"),
                ) {
                    Text(text = name, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider()
            }
        }
    }

    confirmPosition?.let { position ->
        AlertDialog(
            onDismissRequest = { confirmPosition = null },
            title = { Text("Confirm Import") },
            text = {
                Text(
                    "Do you really want to import '" + databaseNameAt(position) +
                        "'?\n This may negatively affect the data integrity of your system!",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmPosition = null
                        onImport(position)
                    },
                ) {
                    Text(context.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmPosition = null }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }

    resultMessage?.let {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Import Result") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = onResultOk) {
                    Text(context.getString(R.string.ok))
                }
            },
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ImportDbScreenPreview() {
    XdripPreview {
        ImportDbScreen(
            databaseNames = listOf("backup-2024.sqlite", "backup-2023.zip"),
            showWarning = false,
            resultMessage = null,
            databaseNameAt = { "backup-2024.sqlite" },
            onWarningOk = {},
            onImport = {},
            onResultOk = {},
            onBack = {},
        )
    }
}

// endregion
