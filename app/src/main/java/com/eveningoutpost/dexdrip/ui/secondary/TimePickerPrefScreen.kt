@file:JvmName("TimePickerPrefScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.ui.settings.TimeOfDayDialog
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import java.util.Locale

/**
 * Track V — `TimePickerPrefActivity`: a Material 3 time picker bound to a `pref-name` string
 * preference that stores seconds-of-day (0..86399). Java entry point: [install].
 */
fun installTimePickerPref(activity: ComponentActivity, prefName: String) {
    activity.setContent {
        TimePickerPrefScreen(prefName = prefName, onDone = { activity.finish() })
    }
}

@Composable
internal fun TimePickerPrefScreen(prefName: String, onDone: () -> Unit) {
    val initialMinutes = remember { JoH.tolerantParseInt(Pref.getString(prefName, "0"), 0) / 60 }
    Surface(modifier = Modifier.fillMaxSize()) {
        TimeOfDayDialog(
            initialMinutes = initialMinutes,
            onDismiss = onDone,
            onConfirm = { minutes ->
                Pref.setString(prefName, String.format(Locale.US, "%d", minutes * 60))
                onDone()
            },
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun TimePickerPrefScreenPreview() {
    XdripPreview {
        TimePickerPrefScreen(prefName = "preview_time", onDone = {})
    }
}

// endregion
