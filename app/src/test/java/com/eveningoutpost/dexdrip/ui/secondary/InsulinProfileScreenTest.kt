package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.insulin.InsulinManager
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class InsulinProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun toggleAndSavePersistsDisabledProfile() {
        Pref.setString("saved_disabled_insulinprofiles_json", "[]")
        InsulinManager.LoadDisabledProfilesFromPrefs()
        val profiles = InsulinManager.getAllProfiles()!!
        val first = profiles[0]
        val second = profiles[1]
        assertThat(InsulinManager.isProfileEnabled(first)).isTrue()

        composeRule.setContent { InsulinProfileScreen(onSave = {}, onCancel = {}) }
        composeRule.onNodeWithTag("insulin_${first.displayName}").assertIsOn()
        composeRule.onNodeWithTag("insulin_${first.displayName}").performClick()
        composeRule.onNodeWithTag("insulin_${first.displayName}").assertIsOff()

        assertThat(InsulinManager.isProfileEnabled(first)).isFalse()

        composeRule.onNodeWithTag("insulin_save").performScrollTo().performClick()

        // Reload from prefs: the disabled profile must have been persisted.
        InsulinManager.LoadDisabledProfilesFromPrefs()
        assertThat(InsulinManager.isProfileEnabled(first)).isFalse()
        assertThat(InsulinManager.isProfileEnabled(second)).isTrue()
    }

    @Test
    fun managerDisableWorks() {
        Pref.setString("saved_disabled_insulinprofiles_json", "[]")
        InsulinManager.LoadDisabledProfilesFromPrefs()
        val first = InsulinManager.getAllProfiles()!![0]
        InsulinManager.disableProfile(first)
        assertThat(InsulinManager.isProfileEnabled(first)).isFalse()
    }

    @Test
    fun basalAndBolusRowsRender() {
        InsulinManager.LoadDisabledProfilesFromPrefs()

        composeRule.setContent { InsulinProfileScreen(onSave = {}, onCancel = {}) }

        composeRule.onNodeWithTag("insulin_basal").assertExists()
        composeRule.onNodeWithTag("insulin_bolus").assertExists()
    }
}
