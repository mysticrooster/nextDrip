@file:JvmName("NumberWallPreviewScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.activities.NumberWallPreview
import com.eveningoutpost.dexdrip.ui.settings.ColorPickerDialog
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utilitymodels.PrefsViewString
import kotlin.math.roundToInt

private const val PREF_X = "numberwall_x_param"
private const val PREF_Y = "numberwall_y_param"
private const val PREF_S = "numberwall_s_param"
private const val PREF_BACKGROUND = "numberwall_background"
private const val PREF_MULTI = "numberwall_multi_param"

/**
 * Track V V5 — `NumberWallPreview`: full-screen lock-screen number-wall preview with the size
 * sliders and text/shadow colour, background-image and multi-number toggles. Reuses the legacy
 * `ViewModel` (bitmap rendering, SAF pick) and `PrefsViewStringSnapDefaultsRefresh` (min-value
 * snapping); a refresh tick drives recomposition. Java entry point: [installNumberWallPreview].
 */
fun installNumberWallPreview(activity: NumberWallPreview) {
    activity.setContent {
        val context = LocalContext.current
        val vm = activity.viewModel
        val prefs = activity.prefs
        val sprefs = activity.sprefs

        var tick by remember { mutableStateOf(activity.refreshTick.get() ?: 0) }
        DisposableEffect(activity) {
            val callback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    tick = activity.refreshTick.get() ?: 0
                }
            }
            activity.refreshTick.addOnPropertyChangedCallback(callback)
            onDispose { activity.refreshTick.removeOnPropertyChangedCallback(callback) }
        }

        val backgroundBitmap = remember(tick) { activity.backgroundBitmap() }
        val backgroundSet = remember(tick) { Pref.getString(PREF_BACKGROUND, null) != null }
        val multi = remember(tick) { prefs.getbool(PREF_MULTI) }

        var x by remember { mutableStateOf(readSnapped(sprefs, PREF_X, 30)) }
        var y by remember { mutableStateOf(readSnapped(sprefs, PREF_Y, 30)) }
        var spacer by remember { mutableStateOf(readSnapped(sprefs, PREF_S, 10)) }

        val textColorKey = ColorCache.X.color_number_wall.internalName
        val shadowColorKey = ColorCache.X.color_number_wall_shadow.internalName
        var pickerTarget by remember { mutableStateOf<String?>(null) }

        NumberWallPreviewScreen(
            bitmap = backgroundBitmap,
            backgroundSet = backgroundSet,
            multi = multi,
            width = x,
            height = y,
            spacer = spacer,
            onWidthChange = { x = writeSnapped(sprefs, PREF_X, it) },
            onHeightChange = { y = writeSnapped(sprefs, PREF_Y, it) },
            onSpacerChange = { spacer = writeSnapped(sprefs, PREF_S, it) },
            onPaletteClick = { pickerTarget = textColorKey },
            onPaletteLongClick = { pickerTarget = shadowColorKey },
            onFolderClick = { vm.folderImageButtonClick() },
            onMultiClick = { prefs.togglebool(PREF_MULTI) },
            onBack = { activity.finish() },
            title = context.getString(R.string.number_wall_config),
        )

        pickerTarget?.let { key ->
            val isShadow = key == shadowColorKey
            ColorPickerDialog(
                title = if (isShadow) "Shadow Color" else "Text Color",
                initialArgb = ColorCache.getCol(if (isShadow) ColorCache.X.color_number_wall_shadow else ColorCache.X.color_number_wall),
                onDismiss = { pickerTarget = null },
                onColorPicked = {
                    Pref.setInt(key, it)
                    ColorCache.invalidateCache()
                    vm.refreshBitmap()
                    pickerTarget = null
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NumberWallPreviewScreen(
    bitmap: Bitmap?,
    backgroundSet: Boolean,
    multi: Boolean,
    width: Int,
    height: Int,
    spacer: Int,
    onWidthChange: (Int) -> Unit,
    onHeightChange: (Int) -> Unit,
    onSpacerChange: (Int) -> Unit,
    onPaletteClick: () -> Unit,
    onPaletteLongClick: () -> Unit,
    onFolderClick: () -> Unit,
    onMultiClick: () -> Unit,
    onBack: () -> Unit,
    title: String = "Number wall",
) {
    SecondaryScreenFill(title = title, onBack = onBack) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                SliderRow(
                    iconRes = R.drawable.expand_text_icon,
                    value = width,
                    range = 0f..360f,
                    onValueChange = onWidthChange,
                    tag = "nwp_width",
                )
                SliderRow(
                    iconRes = R.drawable.expand_vertical_icon,
                    value = height,
                    range = 0f..360f,
                    onValueChange = onHeightChange,
                    tag = "nwp_height",
                )
                SliderRow(
                    iconRes = R.drawable.expand_horizontal_icon,
                    value = spacer,
                    range = 0f..90f,
                    onValueChange = onSpacerChange,
                    tag = "nwp_spacer",
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .combinedClickable(onClick = onPaletteClick, onLongClick = onPaletteLongClick)
                            .testTag("nwp_palette"),
                    ) {
                        Icon(painter = painterResource(R.drawable.palette), contentDescription = null)
                    }
                    IconButton(onClick = onFolderClick, modifier = Modifier.testTag("nwp_folder")) {
                        Icon(
                            painter = painterResource(if (backgroundSet) R.drawable.image_cancel else R.drawable.folder_image),
                            contentDescription = null,
                        )
                    }
                    IconButton(onClick = onMultiClick, modifier = Modifier.testTag("nwp_multi")) {
                        Icon(
                            painter = painterResource(if (multi) R.drawable.multi_numbers else R.drawable.numbers),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    iconRes: Int,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    tag: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(iconRes), contentDescription = null)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .testTag(tag),
        )
    }
}

private fun readSnapped(sprefs: PrefsViewString, key: String, minimum: Int): Int =
    sprefs.get(key)?.toIntOrNull() ?: minimum

private fun writeSnapped(sprefs: PrefsViewString, key: String, value: Int): Int {
    sprefs.put(key, value.toString())
    return sprefs.get(key)?.toIntOrNull() ?: value
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun NumberWallPreviewScreenPreview() {
    XdripPreview {
        NumberWallPreviewScreen(
            bitmap = null,
            backgroundSet = false,
            multi = false,
            width = 100,
            height = 100,
            spacer = 10,
            onWidthChange = {},
            onHeightChange = {},
            onSpacerChange = {},
            onPaletteClick = {},
            onPaletteLongClick = {},
            onFolderClick = {},
            onMultiClick = {},
            onBack = {},
        )
    }
}

// endregion
