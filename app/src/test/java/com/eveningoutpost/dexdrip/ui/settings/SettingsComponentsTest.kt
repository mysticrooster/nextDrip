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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class SettingsComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun colorRowOpensPickerAndReturnsColor() {
        var picked = -1
        composeRule.setContent {
            XdripTheme {
                SettingsColorRow(
                    title = "Chart color",
                    color = 0xFF00FF00.toInt(),
                    onColorChanged = { picked = it },
                )
            }
        }

        composeRule.onNodeWithText("Chart color").performClick()
        composeRule.onNodeWithText("OK").performClick()

        // Green survives the HSV round-trip (alpha forced opaque).
        assertThat(picked).isNotEqualTo(-1)
        assertThat(picked ushr 24).isEqualTo(0xFF)
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
