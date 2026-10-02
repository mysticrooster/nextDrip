@file:JvmName("EventLogScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.EventLogActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.models.UserError
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import java.text.DateFormat
import java.util.Date

/**
 * Track V (Logs) — `EventLogActivity`: streaming severity/search-filtered event log. The activity
 * keeps the refresh thread, filters, log packing and wear log sync; the retained [EventLogActivity.ViewModel]
 * observable lists are bridged into this screen. Java entry point: [installEventLog].
 */
fun installEventLog(activity: EventLogActivity) {
    activity.setContent {
        val model = activity.model
        val listState = rememberLazyListState()

        var tick by remember { mutableStateOf(activity.tick.get() ?: 0) }
        var loading by remember { mutableStateOf(model.showLoading.get()) }
        var scrollReq by remember { mutableStateOf(activity.scrollToTopRequest.get() ?: 0) }
        var query by remember { mutableStateOf(model.currentFilter) }
        var lastTitleFilter by remember { mutableStateOf("") }
        var severities by remember {
            mutableStateOf(
                (1..6).associateWith { model.severity(it) },
            )
        }

        DisposableEffect(activity) {
            val tickCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    tick = activity.tick.get() ?: 0
                }
            }
            activity.tick.addOnPropertyChangedCallback(tickCallback)

            val loadingCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    loading = model.showLoading.get()
                }
            }
            model.showLoading.addOnPropertyChangedCallback(loadingCallback)

            val scrollCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    scrollReq = activity.scrollToTopRequest.get() ?: 0
                }
            }
            activity.scrollToTopRequest.addOnPropertyChangedCallback(scrollCallback)

            onDispose {
                activity.tick.removeOnPropertyChangedCallback(tickCallback)
                model.showLoading.removeOnPropertyChangedCallback(loadingCallback)
                activity.scrollToTopRequest.removeOnPropertyChangedCallback(scrollCallback)
            }
        }

        LaunchedEffect(scrollReq) {
            if (scrollReq > 0) listState.animateScrollToItem(0)
        }

        LaunchedEffect(listState.firstVisibleItemIndex) {
            activity.listAtTop.set(listState.firstVisibleItemIndex <= 1)
        }

        val view = LocalView.current
        DisposableEffect(view) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }

        EventLogScreen(
            query = query,
            severities = severities,
            loading = loading,
            visible = remember(tick) { model.visible.toList() },
            showScrollToTop = listState.firstVisibleItemIndex > 1,
            showThisTitle = { model.showThisTitle(it) },
            listState = listState,
            onQueryChange = {
                query = it
                model.filterChanged(it)
            },
            onSeverityChange = { severity, value ->
                severities = severities + (severity to value)
                model.setSeverity(severity, value)
            },
            onTitleLongClick = { title ->
                // long-pressing the same title again clears the filter (matches legacy behaviour)
                val next = if (title == lastTitleFilter) "" else title
                lastTitleFilter = next
                query = next
                model.filterChanged(next)
            },
            onUpload = { activity.uploadEventLogs() },
            onSave = { activity.saveEventLog() },
            onTop = { activity.requestScrollToTop() },
            onBack = { activity.finish() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EventLogScreen(
    query: String,
    severities: Map<Int, Boolean>,
    loading: Boolean,
    visible: List<UserError>,
    showScrollToTop: Boolean,
    showThisTitle: (UserError) -> Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onQueryChange: (String) -> Unit,
    onSeverityChange: (Int, Boolean) -> Unit,
    onTitleLongClick: (String) -> Unit,
    onUpload: () -> Unit,
    onSave: () -> Unit,
    onTop: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val dateFormat = remember { DateFormat.getDateTimeInstance() }
    SecondaryScreenList(title = "Event Log", onBack = onBack) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            label = { Text(context.getString(R.string.search)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag("event_log_search"),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            severityLabels.forEach { (severity, label) ->
                SeverityFilter(severity, label, severities, onSeverityChange)
            }
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(8.dp)
                    .testTag("event_log_loading"),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(visible) { error ->
                val background = severityContainerColor(error.severity)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(background)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("event_log_row_${error.getId()}"),
                ) {
                    if (showThisTitle(error)) {
                        Text(
                            text = error.shortError.orEmpty(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = severityTitleColor(error.severity, background),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("event_log_title")
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onTitleLongClick(error.shortError.orEmpty()) },
                                ),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateFormat.format(Date(error.timestamp.toLong())),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = error.message.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Button(
                onClick = onUpload,
                modifier = Modifier
                    .weight(1f)
                    .testTag("event_log_upload"),
            ) {
                Text(context.getString(R.string.upload_logs))
            }
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .testTag("event_log_save"),
            ) {
                Text("Save Logs")
            }
            if (showScrollToTop) {
                Button(
                    onClick = onTop,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .testTag("event_log_top"),
                ) {
                    Text(context.getString(R.string.top))
                }
            }
        }
    }
}

@Composable
private fun SeverityFilter(
    severity: Int,
    label: String,
    severities: Map<Int, Boolean>,
    onSeverityChange: (Int, Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
        Checkbox(
            checked = severities[severity] ?: false,
            onCheckedChange = { onSeverityChange(severity, it) },
            modifier = Modifier.testTag("event_log_severity_$severity"),
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun EventLogScreenPreview() {
    XdripPreview {
        EventLogScreen(
            query = "",
            severities = mapOf(1 to true, 2 to true, 3 to true, 5 to true, 6 to true),
            loading = false,
            visible = emptyList(),
            showScrollToTop = false,
            showThisTitle = { true },
            listState = rememberLazyListState(),
            onQueryChange = {},
            onSeverityChange = { _, _ -> },
            onTitleLongClick = {},
            onUpload = {},
            onSave = {},
            onTop = {},
            onBack = {},
        )
    }
}

// endregion
