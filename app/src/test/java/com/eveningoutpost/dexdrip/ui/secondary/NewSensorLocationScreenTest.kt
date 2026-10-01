package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
class NewSensorLocationScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun privateOptionSelectedByDefaultAndSaved() {
        var saved: String? = null
        composeRule.setContent { NewSensorLocationScreen(onSave = { saved = it }, onCancel = {}) }

        composeRule.onNodeWithTag("nsl_radio_$LOCATION_PRIVATE_ID").assertIsSelected()
        composeRule.onNodeWithTag("nsl_radio_1").assertIsNotSelected()
        composeRule.onNodeWithTag("nsl_save").performScrollTo().performClick()

        assertThat(saved).isEqualTo("I don't wish to share")
    }

    @Test
    fun presetOptionIsSaved() {
        var saved: String? = null
        composeRule.setContent { NewSensorLocationScreen(onSave = { saved = it }, onCancel = {}) }

        composeRule.onNodeWithTag("nsl_radio_1").performClick()
        composeRule.onNodeWithTag("nsl_save").performScrollTo().performClick()

        assertThat(saved).isEqualTo("Upper arm")
    }

    @Test
    fun otherTextFieldDisabledUntilOtherSelected() {
        composeRule.setContent { NewSensorLocationScreen(onSave = {}, onCancel = {}) }

        composeRule.onNodeWithTag("nsl_other").assertIsNotEnabled()
        composeRule.onNodeWithTag("nsl_radio_$LOCATION_OTHER_ID").performClick()
        composeRule.onNodeWithTag("nsl_other").performTextInput("Left calf")
        composeRule.onNodeWithTag("nsl_radio_$LOCATION_OTHER_ID").assertIsSelected()
    }

    @Test
    fun otherTypedLocationIsSaved() {
        var saved: String? = null
        composeRule.setContent { NewSensorLocationScreen(onSave = { saved = it }, onCancel = {}) }

        composeRule.onNodeWithTag("nsl_radio_$LOCATION_OTHER_ID").performClick()
        composeRule.onNodeWithTag("nsl_other").performTextInput("Left calf")
        composeRule.onNodeWithTag("nsl_save").performScrollTo().performClick()

        assertThat(saved).isEqualTo("Left calf")
    }

    @Test
    fun cancelDispatches() {
        var cancelled = false
        composeRule.setContent { NewSensorLocationScreen(onSave = {}, onCancel = { cancelled = true }) }

        composeRule.onNodeWithTag("nsl_cancel").performScrollTo().performClick()

        assertThat(cancelled).isTrue()
    }
}
