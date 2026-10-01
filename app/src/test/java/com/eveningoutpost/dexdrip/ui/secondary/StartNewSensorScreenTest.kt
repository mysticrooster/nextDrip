package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class StartNewSensorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun instructionsAreShown() {
        composeRule.setContent { StartNewSensorScreen(onStart = {}, onBack = {}) }

        composeRule.onNodeWithText("Start Sensor").assertExists()
    }

    @Test
    fun startDispatches() {
        var started = false
        composeRule.setContent { StartNewSensorScreen(onStart = { started = true }, onBack = {}) }

        composeRule.onNodeWithTag("start_sensor_button").performClick()

        assertThat(started).isTrue()
    }

    @Test
    fun backDispatches() {
        var back = false
        composeRule.setContent { StartNewSensorScreen(onStart = {}, onBack = { back = true }) }

        composeRule.onNodeWithTag("start_sensor_button").assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertThat(back).isTrue()
    }
}
