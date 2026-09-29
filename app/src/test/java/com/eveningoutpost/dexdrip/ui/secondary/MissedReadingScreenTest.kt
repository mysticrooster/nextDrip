package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Replaces the removed view-based [com.eveningoutpost.dexdrip.MissedReadingActivityTest]: stored
 * settings drive the form and edits are written straight to the preference store (the legacy
 * activity wrote on destroy; the Compose form writes on change).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class MissedReadingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @androidx.compose.runtime.Composable
    private fun screen() = MissedReadingScreen(onDone = {})

    @Test
    fun enableSwitchStoredTrueTogglesOff() {
        Pref.setBoolean("bg_missed_alerts", true)

        composeRule.setContent { screen() }
        composeRule.onNodeWithTag("mra_enable").performClick()

        assertThat(Pref.getBoolean("bg_missed_alerts", true)).isFalse()
    }

    @Test
    fun enableSwitchStoredFalseTogglesOn() {
        Pref.setBoolean("bg_missed_alerts", false)

        composeRule.setContent { screen() }
        composeRule.onNodeWithTag("mra_enable").performClick()

        assertThat(Pref.getBoolean("bg_missed_alerts", false)).isTrue()
    }

    @Test
    fun allDaySwitchStoredTrueTogglesOff() {
        Pref.setBoolean("bg_missed_alerts", true)
        Pref.setBoolean("missed_readings_all_day", true)

        composeRule.setContent { screen() }
        composeRule.onNodeWithTag("mra_all_day").performClick()

        assertThat(Pref.getBoolean("missed_readings_all_day", true)).isFalse()
    }

    @Test
    fun reraiseSwitchStoredTrueTogglesOff() {
        Pref.setBoolean("bg_missed_alerts", true)
        Pref.setBoolean("bg_missed_alerts_enable_alerts_reraise", true)

        composeRule.setContent { screen() }
        composeRule.onNodeWithTag("mra_reraise").performScrollTo().performClick()

        assertThat(Pref.getBoolean("bg_missed_alerts_enable_alerts_reraise", true)).isFalse()
    }

    @Test
    fun overrideSilentSwitchStoredTrueTogglesOff() {
        Pref.setBoolean("bg_missed_alerts", true)
        Pref.setBoolean("bg_missed_alerts_override_silent", true)

        composeRule.setContent { screen() }
        composeRule.onNodeWithTag("mra_override_silent").performScrollTo().performClick()

        assertThat(Pref.getBoolean("bg_missed_alerts_override_silent", true)).isFalse()
    }

    @Test
    fun togglingStoresImmediately() {
        Pref.setBoolean("bg_missed_alerts", false)

        composeRule.setContent { screen() }
        composeRule.onNodeWithTag("mra_enable").performClick()

        assertThat(Pref.getBoolean("bg_missed_alerts", false)).isTrue()
    }

    @Test
    fun minutesFieldFollowsStoredValueAndWritesEdit() {
        Pref.setBoolean("bg_missed_alerts", true)
        Pref.setString("bg_missed_minutes", "17")

        composeRule.setContent { screen() }
        composeRule.onNodeWithTag("mra_minutes").performScrollTo().performClick()
        composeRule.onNode(hasSetTextAction() and hasText("17")).performTextReplacement("42")
        composeRule.onNodeWithText("OK").performClick()

        assertThat(Pref.getString("bg_missed_minutes", "")).isEqualTo("42")
    }

    @Test
    fun allDayHidesTimeRows() {
        Pref.setBoolean("bg_missed_alerts", true)
        Pref.setBoolean("missed_readings_all_day", true)

        composeRule.setContent { screen() }

        composeRule.onNodeWithTag("mra_start").assertDoesNotExist()
        composeRule.onNodeWithTag("mra_end").assertDoesNotExist()
    }

    @Test
    fun timeRowsVisibleWhenNotAllDay() {
        Pref.setBoolean("bg_missed_alerts", true)
        Pref.setBoolean("missed_readings_all_day", false)

        composeRule.setContent { screen() }

        composeRule.onNodeWithTag("mra_start").assertExists()
        composeRule.onNodeWithTag("mra_end").assertExists()
    }

    @Test
    fun alertWindowStoredAsMinutesOfDaySurvivesRender() {
        Pref.setBoolean("bg_missed_alerts", true)
        Pref.setBoolean("missed_readings_all_day", false)
        Pref.setInt("missed_readings_start", 8 * 60 + 15) // 08:15
        Pref.setInt("missed_readings_end", 22 * 60 + 45)  // 22:45

        composeRule.setContent { screen() }

        assertThat(Pref.getInt("missed_readings_start", -1)).isEqualTo(8 * 60 + 15)
        assertThat(Pref.getInt("missed_readings_end", -1)).isEqualTo(22 * 60 + 45)
    }
}
