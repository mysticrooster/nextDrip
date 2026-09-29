package com.eveningoutpost.dexdrip.ui.settings

import android.content.Intent
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.eveningoutpost.dexdrip.ui.theme.XdripTheme
import com.eveningoutpost.dexdrip.utils.Preferences

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
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(stack.last())) },
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
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsScreenContent(
                screen = stack.last(),
                onNavigate = { stack.add(it) },
                onOpenClassic = onOpenClassic,
            )
        }
    }
}
