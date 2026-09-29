package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.eveningoutpost.dexdrip.Agreement
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
 * Replaces the removed view-based [com.eveningoutpost.dexdrip.AgreementTest]: the stored
 * `warning_agreed_to` preference drives the checkbox and Save reports the current state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class AgreementScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun checkboxCheckedWhenStoredTrue() {
        Pref.setBoolean(Agreement.prefmarker, true)

        composeRule.setContent { AgreementScreen(onBack = {}, onAgree = {}) }

        composeRule.onNodeWithTag("agreement_checkbox").assertIsOn()
    }

    @Test
    fun checkboxUncheckedWhenStoredFalse() {
        Pref.setBoolean(Agreement.prefmarker, false)

        composeRule.setContent { AgreementScreen(onBack = {}, onAgree = {}) }

        composeRule.onNodeWithTag("agreement_checkbox").assertIsOff()
    }

    @Test
    fun checkboxUncheckedWhenPreferenceUnset() {
        Pref.getInstance().edit().remove(Agreement.prefmarker).commit()

        composeRule.setContent { AgreementScreen(onBack = {}, onAgree = {}) }

        composeRule.onNodeWithTag("agreement_checkbox").assertIsOff()
    }

    @Test
    fun agreeReportsTickedState() {
        Pref.setBoolean(Agreement.prefmarker, false)
        var reported: Boolean? = null

        composeRule.setContent { AgreementScreen(onBack = {}, onAgree = { reported = it }) }
        composeRule.onNodeWithTag("agreement_checkbox").performScrollTo().performClick()
        composeRule.onNodeWithTag("agreement_save").performScrollTo().performClick()

        assertThat(reported).isTrue()
    }
}
