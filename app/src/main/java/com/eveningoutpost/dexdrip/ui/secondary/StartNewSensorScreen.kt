@file:JvmName("StartNewSensorScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.StartNewSensor

/**
 * Track V pass 5 — `StartNewSensor`: single start action; the activity keeps the permission,
 * insertion-time/date prompts and collector-specific start flows. Java entry point:
 * [installStartNewSensor].
 */
fun installStartNewSensor(activity: StartNewSensor) {
    activity.setContent {
        StartNewSensorScreen(
            onStart = { activity.startSensor() },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun StartNewSensorScreen(
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    SecondaryScreen(title = context.getString(R.string.title_activity_start_new_sensor), onBack = onBack) {
        Text(
            text = context.getString(R.string.do_not_hit_start_long),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
        Button(
            onClick = onStart,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
                .testTag("start_sensor_button"),
        ) {
            Text(context.getString(R.string.start_sensor))
        }
    }
}
