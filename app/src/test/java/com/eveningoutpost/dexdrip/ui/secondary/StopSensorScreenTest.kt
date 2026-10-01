package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
class StopSensorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun stopConfirmsAndDispatches() {
        var stopped = false
        composeRule.setContent {
            StopSensorScreen(
                resettableCals = true,
                stopConfirmMessage = "You will not be able to simply restart the sensor.",
                onConfirmStop = { stopped = true },
                onConfirmResetCalibrations = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("stop_sensor_stop").performScrollTo().performClick()
        composeRule.onNodeWithText("You will not be able to simply restart the sensor.").assertExists()
        composeRule.onNodeWithText("Yes").performClick()

        assertThat(stopped).isTrue()
    }

    @Test
    fun stopCancelDoesNotDispatch() {
        var stopped = false
        composeRule.setContent {
            StopSensorScreen(
                resettableCals = true,
                stopConfirmMessage = "You will not be able to simply restart the sensor.",
                onConfirmStop = { stopped = true },
                onConfirmResetCalibrations = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("stop_sensor_stop").performScrollTo().performClick()
        composeRule.onNodeWithText("You will not be able to simply restart the sensor.").assertExists()
        composeRule.onNodeWithText("No").performClick()

        assertThat(stopped).isFalse()
    }

    @Test
    fun resetButtonHiddenWhenNotResettable() {
        composeRule.setContent {
            StopSensorScreen(
                resettableCals = false,
                stopConfirmMessage = "Are you sure?",
                onConfirmStop = {},
                onConfirmResetCalibrations = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("stop_sensor_reset").assertDoesNotExist()
    }

    @Test
    fun resetConfirmsAndDispatches() {
        var reset = false
        composeRule.setContent {
            StopSensorScreen(
                resettableCals = true,
                stopConfirmMessage = "Are you sure?",
                onConfirmStop = {},
                onConfirmResetCalibrations = { reset = true },
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("stop_sensor_reset").performScrollTo().performClick()
        composeRule.onNodeWithText("Do you want to delete and reset the calibrations for this sensor?").assertExists()
        composeRule.onNodeWithText("Yes").performClick()

        assertThat(reset).isTrue()
    }
}
