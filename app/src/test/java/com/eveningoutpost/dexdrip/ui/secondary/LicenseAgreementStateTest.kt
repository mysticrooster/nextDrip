package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
 * Preserves the contract of the removed view-based [com.eveningoutpost.dexdrip.LicenseAgreementActivityTest]:
 * the stored `I_understand` preference drives the checkbox, and Save writes it back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class LicenseAgreementStateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun checkboxCheckedWhenStoredTrue() {
        Pref.setBoolean("I_understand", true)

        composeRule.setContent { Screen() }

        composeRule.onNodeWithTag("license_checkbox").assertIsOn()
    }

    @Test
    fun checkboxUncheckedWhenStoredFalse() {
        Pref.setBoolean("I_understand", false)

        composeRule.setContent { Screen() }

        composeRule.onNodeWithTag("license_checkbox").assertIsOff()
    }

    @Test
    fun checkboxUncheckedWhenPreferenceUnset() {
        Pref.getInstance().edit().remove("I_understand").commit()

        composeRule.setContent { Screen() }

        composeRule.onNodeWithTag("license_checkbox").assertIsOff()
    }

    @Test
    fun saveStoresFalseWhenCheckboxUnticked() {
        Pref.setBoolean("I_understand", true)

        composeRule.setContent { Screen() }
        composeRule.onNodeWithTag("license_checkbox").performClick()
        composeRule.onNodeWithTag("license_save").performClick()

        assertThat(Pref.getBoolean("I_understand", true)).isFalse()
    }

    @androidx.compose.runtime.Composable
    private fun Screen() {
        LicenseAgreementScreen(
            onBack = {},
            onSave = { Pref.setBoolean("I_understand", it) },
            onGoogleLicenses = {},
            onWarning = {},
        )
    }
}
