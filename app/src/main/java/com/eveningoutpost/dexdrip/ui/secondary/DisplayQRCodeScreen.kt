@file:JvmName("DisplayQRCodeScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.DisplayQRCode

/**
 * Track V V5 — `DisplayQRCode`: settings-transfer QR codes and the settings-source buttons.
 * Bridges the legacy `ViewModel` observables (QR bitmap, narrative, visibility) into Compose;
 * the QR/network logic stays in the activity. Java entry point: [installDisplayQRCode].
 */
fun installDisplayQRCode(activity: DisplayQRCode) {
    activity.setContent {
        val context = LocalContext.current
        val vm = activity.viewModel

        var showQr by remember { mutableStateOf(vm.showQr.get()) }
        var qrBitmap by remember { mutableStateOf((vm.qrbitmap.get() as? BitmapDrawable)?.bitmap) }
        var showGkey by remember { mutableStateOf(vm.showGkey.get()) }
        var narrative by remember { mutableStateOf(vm.narrative.get() ?: "Hello World") }
        var desertSync by remember { mutableStateOf(Pref.getBooleanDefaultFalse("desert_sync_enabled")) }

        DisposableEffect(activity) {
            val showQrCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    showQr = vm.showQr.get()
                }
            }
            vm.showQr.addOnPropertyChangedCallback(showQrCallback)

            val bitmapCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    qrBitmap = (vm.qrbitmap.get() as? BitmapDrawable)?.bitmap
                }
            }
            vm.qrbitmap.addOnPropertyChangedCallback(bitmapCallback)

            val showGkeyCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    showGkey = vm.showGkey.get()
                }
            }
            vm.showGkey.addOnPropertyChangedCallback(showGkeyCallback)

            val narrativeCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    narrative = vm.narrative.get() ?: "Hello World"
                }
            }
            vm.narrative.addOnPropertyChangedCallback(narrativeCallback)

            onDispose {
                vm.showQr.removeOnPropertyChangedCallback(showQrCallback)
                vm.qrbitmap.removeOnPropertyChangedCallback(bitmapCallback)
                vm.showGkey.removeOnPropertyChangedCallback(showGkeyCallback)
                vm.narrative.removeOnPropertyChangedCallback(narrativeCallback)
            }
        }

        // Re-read the desert-sync gate whenever the QR is refreshed (it is set from settings).
        if (showQr) {
            desertSync = Pref.getBooleanDefaultFalse("desert_sync_enabled")
        }

        DisplayQRCodeScreen(
            showQr = showQr,
            qrBitmap = qrBitmap,
            showGkey = showGkey,
            desertSync = desertSync,
            narrative = narrative,
            onSyncSettings = { activity.xdripPlusSyncSettings() },
            onDesertSync = { activity.desertSyncSettings() },
            onGkey = { activity.showGKey() },
            onConnectionSettings = { activity.connectionSettings() },
            onAllSettings = { activity.allSettings() },
            onClose = { activity.closeNow() },
            title = context.getString(R.string.title_activity_display_qrcode),
        )
    }
}

@Composable
internal fun DisplayQRCodeScreen(
    showQr: Boolean,
    qrBitmap: Bitmap?,
    showGkey: Boolean,
    desertSync: Boolean,
    narrative: String,
    onSyncSettings: () -> Unit,
    onDesertSync: () -> Unit,
    onGkey: () -> Unit,
    onConnectionSettings: () -> Unit,
    onAllSettings: () -> Unit,
    onClose: () -> Unit,
    title: String = "Share Settings via QR code",
) {
    val context = LocalContext.current
    SecondaryScreen(title = title, onBack = onClose) {
        if (showQr) {
            qrBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_image"),
                )
            }
            Text(
                text = narrative,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("qr_narrative"),
            )
        } else {
            Text(
                text = context.getString(R.string.use_qr_codes_to_transfer_settings_using_settings_auto_configure_feature),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(
                onClick = onSyncSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("qr_sync"),
            ) {
                Text(context.getString(R.string.xdrip_plus_security_key_settings_only))
            }
            if (desertSync) {
                Button(
                    onClick = onDesertSync,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("qr_desert"),
                ) {
                    Text(context.getString(R.string.set_up_desert_sync_follower))
                }
            }
            if (showGkey) {
                Button(
                    onClick = onGkey,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("qr_gkey"),
                ) {
                    Text("Export KEKS key to another phone")
                }
            }
            Button(
                onClick = onConnectionSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("qr_connection"),
            ) {
                Text(context.getString(R.string.show_general_and_collection_settings))
            }
            Button(
                onClick = onAllSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("qr_all"),
            ) {
                Text(context.getString(R.string.copy_all_settings))
            }
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun DisplayQRCodeScreenPreview() {
    XdripPreview {
        DisplayQRCodeScreen(
            showQr = true,
            qrBitmap = null,
            showGkey = false,
            desertSync = false,
            narrative = "Scan this code to share your settings",
            onSyncSettings = {},
            onDesertSync = {},
            onGkey = {},
            onConnectionSettings = {},
            onAllSettings = {},
            onClose = {},
        )
    }
}

// endregion
