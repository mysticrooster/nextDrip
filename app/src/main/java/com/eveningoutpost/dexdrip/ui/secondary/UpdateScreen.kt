@file:JvmName("UpdateScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.UpdateActivity

/**
 * Track V (Data & admin) — `UpdateActivity`: update channel/version details, download button,
 * auto-update and internal-downloader switches and the download progress. The activity keeps the
 * HTTP check/download, APK install and notification `PendingIntent` contract; the screen observes
 * [UpdateActivity.tick]. Java entry point: [installUpdate].
 */
fun installUpdate(activity: UpdateActivity) {
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

        UpdateScreen(
            channel = remember(tick) { activity.channelText.get().orEmpty() },
            detail = remember(tick) { activity.detailText.get().orEmpty() },
            message = remember(tick) { activity.messageText.get().orEmpty() },
            progressLabel = remember(tick) { activity.progressLabel.get().orEmpty() },
            progressValue = remember(tick) { activity.progressValue.get() ?: 0 },
            progressMax = remember(tick) { activity.progressMax.get() ?: 0 },
            progressVisible = remember(tick) { activity.progressVisible.get() ?: false },
            autoUpdate = remember(tick) { activity.autoUpdate.get() ?: true },
            internalDownloader = remember(tick) { activity.internalDownloader.get() ?: true },
            onDownload = { activity.downloadNow() },
            onAutoUpdateChange = { activity.setAutoUpdate(it) },
            onInternalDownloaderChange = { activity.setInternalDownloader(it) },
            onBack = { activity.closeActivity() },
        )
    }
}

@Composable
internal fun UpdateScreen(
    channel: String,
    detail: String,
    message: String,
    progressLabel: String,
    progressValue: Int,
    progressMax: Int,
    progressVisible: Boolean,
    autoUpdate: Boolean,
    internalDownloader: Boolean,
    onDownload: () -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
    onInternalDownloaderChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    SecondaryScreen(title = "Update", onBack = onBack) {
        Text(
            text = channel,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("update_channel"),
        )
        Text(
            text = context.getString(R.string.a_new_version_is_available),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("update_available"),
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .testTag("update_detail"),
        )
        Text(
            text = context.getString(R.string.version_details),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("update_message"),
        )
        Button(
            onClick = onDownload,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("update_download"),
        ) {
            Text(context.getString(R.string.download_now))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(
                text = context.getString(R.string.automatically_check_for_updates),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = autoUpdate,
                onCheckedChange = onAutoUpdateChange,
                modifier = Modifier.testTag("update_auto"),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(
                text = context.getString(R.string.use_internal_downloader),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = internalDownloader,
                onCheckedChange = onInternalDownloaderChange,
                modifier = Modifier.testTag("update_internal_downloader"),
            )
        }
        if (progressVisible) {
            LinearProgressIndicator(
                progress = { if (progressMax > 0) progressValue.toFloat() / progressMax else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("update_progress"),
            )
            Text(
                text = progressLabel,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("update_progress_label"),
            )
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun UpdateScreenPreview() {
    XdripPreview {
        UpdateScreen(
            channel = "Update channel: Beta",
            detail = "New version: 123\nOld version: 120",
            message = "Highlights",
            progressLabel = "512 KB",
            progressValue = 30,
            progressMax = 100,
            progressVisible = true,
            autoUpdate = true,
            internalDownloader = true,
            onDownload = {},
            onAutoUpdateChange = {},
            onInternalDownloaderChange = {},
            onBack = {},
        )
    }
}

// endregion
