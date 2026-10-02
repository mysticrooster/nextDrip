@file:JvmName("AlertListScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.AlertList
import com.eveningoutpost.dexdrip.AlertList.AlertRow
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

/**
 * Track V (Alerts) — `AlertList`: level alert profiles with add buttons and long-press to edit.
 * The activity keeps the units read, tone-path permission and edit/result flow; the screen renders
 * the rows. Java entry point: [installAlertList].
 */
fun installAlertList(activity: AlertList) {
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

        AlertListScreen(
            lowRows = remember(tick) { activity.lowRowsSnapshot },
            highRows = remember(tick) { activity.highRowsSnapshot },
            onAddLow = { activity.addLowAlert() },
            onAddHigh = { activity.addHighAlert() },
            onEdit = { activity.editAlert(it) },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun AlertListScreen(
    lowRows: List<AlertRow>,
    highRows: List<AlertRow>,
    onAddLow: () -> Unit,
    onAddHigh: () -> Unit,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    SecondaryScreenList(title = context.getString(R.string.level_alerts), onBack = onBack) {
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            item {
                Button(
                    onClick = onAddLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("alert_create_low"),
                ) {
                    Text(context.getString(R.string.create_low_alert))
                }
            }
            items(lowRows, key = { it.uuid ?: it.name }) { row ->
                AlertRowView(row) { onEdit(row.uuid) }
            }
            item {
                Button(
                    onClick = onAddHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("alert_create_high"),
                ) {
                    Text(context.getString(R.string.create_high_alert))
                }
            }
            items(highRows, key = { it.uuid ?: it.name }) { row ->
                AlertRowView(row) { onEdit(row.uuid) }
            }
            item {
                Text(
                    text = context.getString(R.string.long_press_an_existing_alert_to_edit),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .testTag("alert_long_press_hint"),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlertRowView(row: AlertRow, onLongClick: () -> Unit) {
    val decoration = if (row.active) TextDecoration.None else TextDecoration.LineThrough
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("alert_row_${row.uuid}"),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.titleSmall,
                textDecoration = decoration,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = row.threshold,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = decoration,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = row.time,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = decoration,
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = row.overrideSilentMode,
                style = MaterialTheme.typography.labelSmall,
                textDecoration = decoration,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = row.mp3File,
                style = MaterialTheme.typography.labelSmall,
                textDecoration = decoration,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AlertListScreenPreview() {
    XdripPreview {
        AlertListScreen(
            lowRows = emptyList(),
            highRows = emptyList(),
            onAddLow = {},
            onAddHigh = {},
            onEdit = {},
            onBack = {},
        )
    }
}

// endregion
