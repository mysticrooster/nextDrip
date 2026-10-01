package com.eveningoutpost.dexdrip.ui.settings

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 * IA redesign guard: the root is 9 category buttons opening submenus, the retired umbrellas are gone
 * and the former Home overflow actions are reachable as settings rows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class SettingsIaTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<SettingsActivity>()

    private fun category(tag: String) = composeRule.onNodeWithTag(tag).performScrollTo().performClick()

    @Test
    fun rootShowsTheNineCategoryButtons() {
        listOf(
            "setting_category_general",
            "setting_category_alarms",
            "setting_category_data",
            "setting_category_profile",
            "setting_category_devices",
            "setting_category_appearance",
            "setting_category_accessibility",
            "setting_category_advanced",
            "setting_category_about",
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).performScrollTo().assertExists()
        }
    }

    @Test
    fun categoriesOpenSubmenusWithTheirRows() {
        composeRule.onNodeWithTag("setting_search").assertExists()

        category("setting_category_general")
        composeRule.onNodeWithTag("setting_glucose_units").assertExists()
        composeRule.onNodeWithTag("setting_search").assertDoesNotExist()
    }

    @Test
    fun retiredUmbrellasAreGone() {
        composeRule.onNodeWithText("Extra settings").assertDoesNotExist()
        composeRule.onNodeWithText("Other Settings").assertDoesNotExist()
        composeRule.onNodeWithText("Less common settings").assertDoesNotExist()
    }

    @Test
    fun formerOverflowActionsAreReachable() {
        category("setting_category_alarms")
        composeRule.onNodeWithTag("setting_reminders").performScrollTo().assertExists()
        composeRule.onNodeWithTag("setting_emergency_messages").performScrollTo().assertExists()
    }

    @Test
    fun backupsScreenListsExportRows() {
        category("setting_category_data")
        composeRule.onNodeWithTag("setting_backups").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_cloud_backup").performScrollTo().assertExists()
        composeRule.onNodeWithTag("setting_export_database").performScrollTo().assertExists()
        composeRule.onNodeWithTag("setting_import_database").performScrollTo().assertExists()
        composeRule.onNodeWithTag("setting_export_csv").performScrollTo().assertExists()
        composeRule.onNodeWithTag("setting_settings_sd").performScrollTo().assertExists()
    }

    @Test
    fun aboutScreenReachableWithVersion() {
        category("setting_category_about")
        composeRule.onNodeWithTag("setting_version").performScrollTo().assertExists()
        composeRule.onNodeWithTag("setting_check_update").assertExists()
        composeRule.onNodeWithTag("setting_help").assertExists()

        composeRule.onNodeWithTag("setting_version").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_version_name").assertExists()
    }

    @Test
    fun deleteAllBgRequiresConfirmation() {
        category("setting_category_advanced")
        composeRule.onNodeWithTag("setting_delete_all_bg").performScrollTo().performClick()

        // The destructive action must not run until the dialog is confirmed.
        composeRule.onNodeWithTag("setting_delete_all_bg_confirm").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithTag("setting_delete_all_bg_confirm").assertDoesNotExist()
    }

    @Test
    fun homeScreenShelfToggleWritesPref() {
        Pref.setBoolean("home-shelf-chart_preview", false)

        category("setting_category_appearance")
        composeRule.onNodeWithTag("setting_home_screen").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_home_chart_preview").performScrollTo().performClick()

        assertThat(Pref.getBoolean("home-shelf-chart_preview", false)).isTrue()
    }
}
