@file:JvmName("MtpConfigureScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.MtpConfigure
import com.eveningoutpost.dexdrip.utilitymodels.NanoStatus
import com.eveningoutpost.dexdrip.utils.usb.UsbTools
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Track V pass 3 (Medium) — `MtpConfigureActivity`: USB-OTG MTP configuration status. Polls
 * [NanoStatus] while resumed (replacing the legacy `NanoStatus` background thread + dovetail) and
 * attempts the USB connect on the same cadence (the call is internally rate-limited). Java entry
 * point: [installMtpConfigure].
 */
fun installMtpConfigure(activity: ComponentActivity) {
    activity.setContent {
        MtpConfigureScreen(onBack = { activity.finish() })
    }
}

@Composable
internal fun MtpConfigureScreen(
    onBack: () -> Unit,
    statusProvider: () -> String = { NanoStatus.nanoStatus("mtp-configure") ?: "" },
    colorProvider: () -> Int? = { firstForegroundColor(NanoStatus.nanoStatusColor("mtp-configure")) },
    usbAttempt: () -> Unit = { MtpConfigure.handleConnect(UsbTools.getUsbDevice(0x0bb4, 0x0c02, "oFi")) },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember { mutableStateOf(statusProvider()) }
    var color by remember { mutableStateOf(colorProvider()) }
    var resumed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumed) {
        while (resumed && isActive) {
            status = statusProvider()
            color = colorProvider()
            usbAttempt()
            delay(300)
        }
    }

    SecondaryScreen(title = "USB programmer", onBack = onBack) {
        Text(
            text = context.getString(R.string.connect_device_usb_otg),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
        )
        Text(
            text = context.getString(R.string.connection_status),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        Text(
            text = status,
            style = MaterialTheme.typography.titleMedium,
            color = color?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .testTag("mtp_status"),
        )
    }
}

private fun firstForegroundColor(spannable: SpannableString?): Int? {
    if (spannable == null) return null
    return spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)
        .firstOrNull()
        ?.foregroundColor
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MtpConfigureScreenPreview() {
    XdripPreview {
        MtpConfigureScreen(onBack = {}, statusProvider = { "" }, colorProvider = { null }, usbAttempt = {})
    }
}

// endregion
