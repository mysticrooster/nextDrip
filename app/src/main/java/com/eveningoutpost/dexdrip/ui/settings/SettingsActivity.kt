package com.eveningoutpost.dexdrip.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.eveningoutpost.dexdrip.alert.Registry
import com.eveningoutpost.dexdrip.cgm.webfollow.Cpref
import com.eveningoutpost.dexdrip.cloud.jamcm.Pusher
import com.eveningoutpost.dexdrip.healthconnect.HealthConnectEntry
import com.eveningoutpost.dexdrip.healthconnect.HealthGamut
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.services.ActivityRecognizedService
import com.eveningoutpost.dexdrip.services.UiBasedCollector
import com.eveningoutpost.dexdrip.services.broadcastservice.BroadcastService
import com.eveningoutpost.dexdrip.ui.LockScreenWallPaper
import com.eveningoutpost.dexdrip.ui.theme.XdripTheme
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter
import com.eveningoutpost.dexdrip.utilitymodels.Constants
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utilitymodels.WholeHouse
import com.eveningoutpost.dexdrip.utils.DexCollectionType
import com.eveningoutpost.dexdrip.utils.LocationHelper
import com.eveningoutpost.dexdrip.utils.QrScanProcessor
import com.eveningoutpost.dexdrip.watch.lefun.LeFunEntry
import com.eveningoutpost.dexdrip.watch.miband.MiBandEntry
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import java.io.FileNotFoundException

/**
 * Compose settings host.
 *
 * This is the settings migration (Phase 4). It renders the migrated categories as a lightweight
 * in-Compose screen stack (sealed [SettingsScreen] state + `BackHandler`). S6 retired the legacy
 * `android.preference` activity; this host now also owns the scan-result handling and the legacy
 * preference-change listeners. Preference keys are unchanged so user data/backups are unaffected.
 */
class SettingsActivity : ComponentActivity() {

    private val deepLink = mutableStateOf<SettingsScreen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLink.value = screenFromIntent(intent)
        UiBasedCollector.onEnableCheckPermission(this)
        setContent {
            XdripTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SettingsRoot(deepLink.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink.value = screenFromIntent(intent)
    }

    // Scans started from the Compose settings screens (Auto Configure, Data Source) return here.
    // Ported from the retired legacy settings activity so the same payloads are still handled.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var scanFormat: String? = null
        var scanContents: String? = null
        var scanRawBytes: ByteArray? = null

        if (requestCode == Constants.HEALTH_CONNECT_RESPONSE_ID) {
            // Permit/install flow returned; re-init so Health Connect picks up the new state.
            if (HealthConnectEntry.enabled() && JoH.ratelimit("health-connect-bump", 2)) {
                HealthGamut.init(this)
            }
            return
        } else if (requestCode == Constants.ZXING_FILE_REQ_CODE) {
            if (data == null || data.data == null) {
                Log.e("TAG", "No file was selected")
                return
            }
            val uri: Uri = data.data!!
            try {
                val inputStream = contentResolver.openInputStream(uri)
                var bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) {
                    Log.e("TAG", "uri is not a bitmap, $uri")
                    return
                }
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                bitmap.recycle()
                bitmap = null
                val source = RGBLuminanceSource(width, height, pixels)
                val bBitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader()
                try {
                    val result = reader.decode(bBitmap)
                    scanFormat = result.barcodeFormat.toString()
                    scanContents = result.text
                    scanRawBytes = result.rawBytes
                } catch (e: NotFoundException) {
                    Log.e("TAG", "decode exception", e)
                }
            } catch (e: FileNotFoundException) {
                Log.e("TAG", "can not open file $uri", e)
            }
        } else if (requestCode == Constants.ZXING_CAM_REQ_CODE) {
            val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            scanFormat = scanResult.formatName
            scanContents = scanResult.contents
            scanRawBytes = scanResult.rawBytes
        } else {
            return
        }

        if (scanContents != null) {
            QrScanProcessor.handle(this, scanFormat, scanContents, scanRawBytes)
        }
    }

    companion object {
        const val EXTRA_SETTINGS_SCREEN = "settings_screen"
        const val ACTION_BLUEJAY_PREFERENCE_SCREEN = "bluejay_preference_screen"

        /** Maps legacy deep-link actions / extras to a Compose settings destination. */
        internal fun screenFromIntent(intent: Intent?): SettingsScreen? {
            if (intent == null) return null
            if (intent.action == ACTION_BLUEJAY_PREFERENCE_SCREEN) return SettingsScreen.BlueJaySettings
            val name = intent.getStringExtra(EXTRA_SETTINGS_SCREEN) ?: return null
            return SettingsScreen.entries.firstOrNull { it.name == name }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsRoot(initialScreen: SettingsScreen? = null) {
    val stack = remember { mutableStateListOf<SettingsScreen>(SettingsScreen.Root) }
    LaunchedEffect(initialScreen) {
        if (initialScreen != null && initialScreen != SettingsScreen.Root && stack.last() != initialScreen) {
            stack.add(initialScreen)
        }
    }
    val scrollState = rememberScrollState()
    val activity = LocalContext.current as? Activity
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    // The legacy settings activity registered these listeners for its lifetime; reproduce that here
    // so changes made in the Compose host reach the services. Watch listeners refresh services /
    // restart the collector; the number-wall listener refreshes the lockscreen wallpaper; the motion
    // listener enforces the remote/master mutual exclusion and (re)starts the recogniser; the cloud,
    // webfollow, broadcast and alert listeners reconnect / re-enable their services.
    DisposableEffect(Unit) {
        val prefs = Pref.getInstance()
        val numberWallListener = LockScreenWallPaper.PrefListener().prefListener
        val uiPrefListener = activity?.let { UiBasedCollector.getListener(it) }
        val cloudListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "use_xdrip_cloud_sync") {
                Pusher.requestReconnect()
                CollectionServiceStarter.restartCollectionServiceBackground()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(MiBandEntry.prefListener)
        prefs.registerOnSharedPreferenceChangeListener(LeFunEntry.prefListener)
        prefs.registerOnSharedPreferenceChangeListener(BlueJayEntry.prefListener)
        prefs.registerOnSharedPreferenceChangeListener(ActivityRecognizedService.prefListener)
        prefs.registerOnSharedPreferenceChangeListener(Cpref.prefListener)
        prefs.registerOnSharedPreferenceChangeListener(BroadcastService.prefListener)
        prefs.registerOnSharedPreferenceChangeListener(Registry.prefListener)
        prefs.registerOnSharedPreferenceChangeListener(numberWallListener)
        prefs.registerOnSharedPreferenceChangeListener(cloudListener)
        uiPrefListener?.let { prefs.registerOnSharedPreferenceChangeListener(it) }
        if (DexCollectionType.hasBluetooth() && !WholeHouse.isRpi()) {
            activity?.let { LocationHelper.requestLocationForBluetooth(it) }
        }
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(MiBandEntry.prefListener)
            prefs.unregisterOnSharedPreferenceChangeListener(LeFunEntry.prefListener)
            prefs.unregisterOnSharedPreferenceChangeListener(BlueJayEntry.prefListener)
            prefs.unregisterOnSharedPreferenceChangeListener(ActivityRecognizedService.prefListener)
            prefs.unregisterOnSharedPreferenceChangeListener(Cpref.prefListener)
            prefs.unregisterOnSharedPreferenceChangeListener(BroadcastService.prefListener)
            prefs.unregisterOnSharedPreferenceChangeListener(Registry.prefListener)
            prefs.unregisterOnSharedPreferenceChangeListener(numberWallListener)
            prefs.unregisterOnSharedPreferenceChangeListener(cloudListener)
            uiPrefListener?.let { prefs.unregisterOnSharedPreferenceChangeListener(it) }
        }
    }

    // Reset the scroll position when navigating between screens.
    LaunchedEffect(stack.last()) { scrollState.scrollTo(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(LocalContext.current, stack.last())) },
                navigationIcon = {
                    if (stack.size > 1) {
                        IconButton(onClick = { stack.removeAt(stack.lastIndex) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
        ) {
            SettingsScreenContent(
                screen = stack.last(),
                onNavigate = { stack.add(it) },
            )
        }
    }
}
