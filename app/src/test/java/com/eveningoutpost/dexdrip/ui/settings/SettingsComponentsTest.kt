package com.eveningoutpost.dexdrip.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.ui.theme.XdripTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class SettingsComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun colorRowOpensPickerAndReturnsColor() {
        var picked = -1
        // Translucent and not full-brightness, so the picker must preserve value/alpha.
        val initial = 0x8033B5E6.toInt()
        composeRule.setContent {
            XdripTheme {
                SettingsColorRow(
                    title = "Chart color",
                    color = initial,
                    onColorChanged = { picked = it },
                )
            }
        }

        composeRule.onNodeWithText("Chart color").performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertThat(picked).isNotEqualTo(-1)
        // Alpha survives the round-trip (not forced opaque).
        assertThat(picked ushr 24).isEqualTo(0x80)
        // Brightness is not forced to maximum (input blue channel is 0xE6, not 0xFF).
        assertThat(picked and 0xFF).isLessThan(0xF0)
    }

    @Test
    fun timeAndRingtoneRowsRender() {
        composeRule.setContent {
            XdripTheme {
                SettingsTimeRow(title = "Alert time", valueMillis = 0L, onTimeChanged = {})
                SettingsRingtoneRow(title = "Alert sound", value = "default", onPicked = {})
            }
        }

        composeRule.onNodeWithText("Alert time").assertExists()
        composeRule.onNodeWithText("Alert sound").assertExists()
    }
}
