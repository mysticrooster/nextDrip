package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Track V pass 2 — trivial secondary screens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class TrivialScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calibrationCheckInInvokesCallback() {
        var clicked = false

        composeRule.setContent { CalibrationCheckInScreen(onBack = {}, onCheckIn = { clicked = true }) }
        composeRule.onNodeWithTag("check_in_calibrations").performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun calibrationOverrideShowsSubmitError() {
        composeRule.setContent {
            CalibrationOverrideScreen(onBack = {}, onSubmit = { "Calibration Can Not be blank" })
        }
        composeRule.onNodeWithTag("override_save").performClick()

        composeRule.onNodeWithText("Calibration Can Not be blank").assertExists()
    }

    @Test
    fun calibrationOverridePassesEnteredText() {
        var submitted: String? = null

        composeRule.setContent {
            CalibrationOverrideScreen(onBack = {}, onSubmit = { submitted = it; null })
        }
        composeRule.onNodeWithTag("override_bg_value").performTextInput("123")
        composeRule.onNodeWithTag("override_save").performClick()

        assertThat(submitted).isEqualTo("123")
    }

    @Test
    fun doubleCalibrationSecondFieldHiddenWhenNotEnabled() {
        Pref.setBoolean("use_double_calibrations", false)

        composeRule.setContent { DoubleCalibrationScreen(onBack = {}, onSubmit = { _, _ -> null }) }

        composeRule.onNodeWithTag("double_bg_value_2").assertDoesNotExist()
    }

    @Test
    fun doubleCalibrationSecondFieldShownWhenEnabled() {
        Pref.setBoolean("use_double_calibrations", true)

        composeRule.setContent { DoubleCalibrationScreen(onBack = {}, onSubmit = { _, _ -> null }) }

        composeRule.onNodeWithTag("double_bg_value_2").assertExists()
    }

    @Test
    fun dreamSwitchWritesPreference() {
        Pref.setBoolean("daydream_use_gravity_sensor", false)

        composeRule.setContent { DreamSettingsScreen(onBack = {}) }
        composeRule.onNodeWithTag("dream_gravity_switch").performClick()

        assertThat(Pref.getBoolean("daydream_use_gravity_sensor", false)).isTrue()
    }

    @Test
    fun healthPrivacyCloseInvokesCallback() {
        var closed = false

        composeRule.setContent { HealthPrivacyScreen(onClose = { closed = true }) }
        composeRule.onNodeWithTag("health_privacy_close").performClick()

        assertThat(closed).isTrue()
    }

    @Test
    fun fakeNumbersReportsInvalidInput() {
        composeRule.setContent {
            FakeNumbersScreen(onBack = {}, onLog = { "bad" }, onStartTest = {}, onStartTestAlerts = {})
        }
        composeRule.onNodeWithTag("fake_log").performClick()

        composeRule.onNodeWithText("bad").assertExists()
    }

    @Test
    fun fakeNumbersStartTestInvokesCallback() {
        var started = false

        composeRule.setContent {
            FakeNumbersScreen(onBack = {}, onLog = { null }, onStartTest = { started = true }, onStartTestAlerts = {})
        }
        composeRule.onNodeWithTag("fake_start_test").performClick()

        assertThat(started).isTrue()
    }
}
