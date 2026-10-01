package com.eveningoutpost.dexdrip.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.eveningoutpost.dexdrip.cloud.jamcm.Pusher
import com.eveningoutpost.dexdrip.services.ActivityRecognizedService
import com.eveningoutpost.dexdrip.ui.LockScreenWallPaper
import com.eveningoutpost.dexdrip.ui.theme.XdripTheme
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.Preferences
import com.eveningoutpost.dexdrip.watch.lefun.LeFunEntry
import com.eveningoutpost.dexdrip.watch.miband.MiBandEntry
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry

/**
 * Compose settings host.
 *
 * This is the settings migration (Phase 4). It renders the migrated categories as a lightweight
 * in-Compose screen stack (sealed [SettingsScreen] state + `BackHandler`) and links to the legacy
 * [Preferences] activity for everything not yet migrated. Preference keys are unchanged so user
 * data/backups are unaffected.
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XdripTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SettingsRoot(
                        onOpenClassic = {
                            startActivity(Intent(this, Preferences::class.java))
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsRoot(onOpenClassic: () -> Unit) {
    val stack = remember { mutableStateListOf<SettingsScreen>(SettingsScreen.Root) }
    val scrollState = rememberScrollState()
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    // The legacy settings activity registers these listeners for its lifetime; reproduce that here
    // so changes made in the Compose host reach the services. Watch listeners refresh services /
    // restart the collector; the number-wall listener refreshes the lockscreen wallpaper; the motion
    // listener enforces the remote/master mutual exclusion and (re)starts the recogniser; the cloud
    // listener reconnects the pusher.
    DisposableEffect(Unit) {
        val prefs = Pref.getInstance()
        val numberWallListener = LockScreenWallPaper.PrefListener().prefListener
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
        prefs.registerOnSharedPreferenceChangeListener(numberWallListener)
        prefs.registerOnSharedPreferenceChangeListener(cloudListener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(MiBandEntry.prefListener)
            prefs.unregisterOnSharedPreferenceChangeListener(LeFunEntry.prefListener)
            prefs.unregisterOnSharedPreferenceChangeListener(BlueJayEntry.prefListener)
            prefs.unregisterOnSharedPreferenceChangeListener(ActivityRecognizedService.prefListener)
            prefs.unregisterOnSharedPreferenceChangeListener(numberWallListener)
            prefs.unregisterOnSharedPreferenceChangeListener(cloudListener)
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
                onOpenClassic = onOpenClassic,
            )
        }
    }
}
