package com.eveningoutpost.dexdrip.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.R
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Small, hand-rolled settings components used by the Compose settings screens.
 *
 * These read/write preferences through [com.eveningoutpost.dexdrip.utilitymodels.Pref] (same keys
 * as the legacy settings) so no user data or backups change, and draw chrome from the app theme
 * ([com.eveningoutpost.dexdrip.ui.theme.XdripTheme] / `MaterialTheme`) plus user data colors from
 * [LocalXdripColors]. Each row takes an optional [modifier] so callers can attach a `testTag`,
 * and an [enabled] flag so callers can reproduce the legacy `android:dependency` behaviour.
 */

private val DISABLED_ALPHA = 0.38f

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
    Column(Modifier.fillMaxWidth(), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = trailing?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, onClick = onClick),
    )
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
    )
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsEditTextRow(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    numeric: Boolean = false,
    enabled: Boolean = true,
    valueColor: Color = Color.Unspecified,
) {
    var showDialog by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor) },
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled) { showDialog = true },
    )
    HorizontalDivider()
    if (showDialog) {
        EditTextDialog(
            title = title,
            initial = value,
            numeric = numeric,
            onDismiss = { showDialog = false },
            onConfirm = { onValueChange(it); showDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsListRow(
    title: String,
    entries: List<String>,
    values: List<String>,
    selectedValue: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }
    val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Text(entries.getOrElse(selectedIndex) { "" }, style = MaterialTheme.typography.bodyMedium) },
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled) { showDialog = true },
    )
    HorizontalDivider()
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 480.dp),
                ) {
                    entries.forEachIndexed { index, label ->
                        Text(
                            text = label,
                            color = if (values[index] == selectedValue) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(values[index])
                                    showDialog = false
                                }
                                .padding(vertical = 14.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}

/**
 * Integer slider row, replacing the legacy `SeekBarPreference` (e.g. Tidepool window latency).
 */
@Composable
fun SettingsSliderRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: IntRange = 0..100,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            enabled = enabled,
        )
    }
    HorizontalDivider()
}

/**
 * Sound picker row. Uses the system ringtone picker (`ACTION_RINGTONE_PICKER`), exactly like the
 * legacy `RingtonePreference`, and stores the picked ringtone [Uri] as a string (empty = silent).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRingtoneRow(
    title: String,
    value: String?,
    onPicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            onPicked(uri?.toString() ?: "")
        }
    }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Text(ringtoneTitle(context, value), style = MaterialTheme.typography.bodyMedium) },
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled) {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    putExtra(
                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        if (value.isNullOrEmpty()) null else Uri.parse(value),
                    )
                }
                launcher.launch(intent)
            },
    )
    HorizontalDivider()
}

private fun ringtoneTitle(context: Context, value: String?): String {
    if (value.isNullOrEmpty()) return context.getString(R.string.pref_ringtone_silent)
    return try {
        RingtoneManager.getRingtone(context, Uri.parse(value))?.getTitle(context) ?: value
    } catch (e: Exception) {
        value
    }
}

/**
 * Time-of-day row, replacing the legacy [com.eveningoutpost.dexdrip.utils.TimePreference]
 * (stores milliseconds since the epoch).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTimeRow(
    title: String,
    valueMillis: Long,
    onTimeChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Text(
                text = DateFormat.getTimeFormat(context).format(valueMillis),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled) {
                val calendar = Calendar.getInstance().apply { timeInMillis = valueMillis }
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val picked = Calendar.getInstance().apply {
                            timeInMillis = valueMillis
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                        }
                        onTimeChanged(picked.timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(context),
                ).show()
            },
    )
    HorizontalDivider()
}

/**
 * Color row, replacing the `colorpicker` AAR preference dialog. Shows a swatch of the current
 * data color and opens a simple HSV picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsColorRow(
    title: String,
    color: Int,
    onColorChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    onReset: (() -> Unit)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(color), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled) { showDialog = true },
    )
    HorizontalDivider()
    if (showDialog) {
        ColorPickerDialog(
            title = title,
            initial = color,
            onDismiss = { showDialog = false },
            onColorPicked = { onColorChanged(it); showDialog = false },
            onReset = onReset?.let { r -> { r(); showDialog = false } },
        )
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initial: Int,
    onDismiss: () -> Unit,
    onColorPicked: (Int) -> Unit,
    onReset: (() -> Unit)? = null,
) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(color)),
                )
                ColorSlider("Hue", hue, 0f..360f) { hue = it }
                ColorSlider("Saturation", saturation, 0f..1f) { saturation = it }
                ColorSlider("Brightness", value, 0f..1f) { value = it }
            }
        },
        confirmButton = { TextButton(onClick = { onColorPicked(color) }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = {
            Row {
                if (onReset != null) {
                    TextButton(onClick = onReset) { Text(stringResource(R.string.theme_reset_default)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun ColorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    Slider(value = value, onValueChange = onChange, valueRange = range)
}

@Composable
private fun EditTextDialog(
    title: String,
    initial: String,
    numeric: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}
