@file:JvmName("DatabaseAdminScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.ui.activities.DatabaseAdmin
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

/**
 * Track V pass 3 (Medium) — `DatabaseAdmin`: console output + maintenance actions. The activity keeps
 * the SQL processors and its `console` [ObservableField]; this screen observes it. Java entry point:
 * [installDatabaseAdmin].
 */
fun installDatabaseAdmin(activity: DatabaseAdmin) {
    activity.setContent {
        var console by remember { mutableStateOf(activity.console.get() ?: "") }
        DisposableEffect(activity) {
            val callback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    console = activity.console.get() ?: ""
                }
            }
            activity.console.addOnPropertyChangedCallback(callback)
            onDispose { activity.console.removeOnPropertyChangedCallback(callback) }
        }
        DatabaseAdminScreen(
            console = console,
            onQuickCheck = { activity.quickCheck() },
            onLongCheck = { activity.longCheck() },
            onStatistics = { activity.statistics() },
            onCompact = { activity.compact() },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun DatabaseAdminScreen(
    console: String,
    onQuickCheck: () -> Unit,
    onLongCheck: () -> Unit,
    onStatistics: () -> Unit,
    onCompact: () -> Unit,
    onBack: () -> Unit,
) {
    SecondaryScreen(title = "Database Admin", onBack = onBack) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onQuickCheck,
                modifier = Modifier.weight(1f).testTag("db_quick"),
            ) {
                Text("Check DB quick")
            }
            Button(
                onClick = onLongCheck,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .testTag("db_long"),
            ) {
                Text("Check DB long")
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Button(
                onClick = onStatistics,
                modifier = Modifier.weight(1f).testTag("db_statistics"),
            ) {
                Text("Statistics")
            }
            Button(
                onClick = onCompact,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .testTag("db_compact"),
            ) {
                Text("Compact")
            }
        }
        Text(
            text = console,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .testTag("db_console"),
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun DatabaseAdminScreenPreview() {
    XdripPreview {
        DatabaseAdminScreen(
            console = "Sample database console output",
            onQuickCheck = {},
            onLongCheck = {},
            onStatistics = {},
            onCompact = {},
            onBack = {},
        )
    }
}

// endregion
