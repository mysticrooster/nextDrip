package com.eveningoutpost.dexdrip.ui.settings

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.ui.theme.ThemeColor
import com.eveningoutpost.dexdrip.ui.theme.ThemeColorStore
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class SettingsActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<SettingsActivity>()

    @Test
    fun unitsSubScreenWritesPref() {
        Pref.setString("units", "mgdl")

        // Root -> General category -> units sub-screen -> units list dialog.
        composeRule.onNodeWithTag("setting_category_general").performClick()
        composeRule.onNodeWithTag("setting_glucose_units").performClick()
        composeRule.onNodeWithTag("setting_units").performClick()
        composeRule.onNodeWithText("mmol/L").performClick()

        assertThat(Pref.getString("units", "mgdl")).isEqualTo("mmol")
    }

    @Test
    fun editTextRowOpensDialog() {
        composeRule.onNodeWithTag("setting_category_general").performClick()
        composeRule.onNodeWithTag("setting_glucose_units").performClick()
        composeRule.onNodeWithTag("setting_highValue").performClick()

        // The edit dialog (with its OK button) is shown.
        composeRule.onNodeWithText("OK").assertExists()
    }

    @Test
    fun notificationSwitchWritesPref() {
        Pref.setBoolean("smart_snoozing", true)

        composeRule.onNodeWithTag("setting_category_alarms").performClick()
        composeRule.onNodeWithTag("setting_bg_alerts").performScrollTo().performClick()
        composeRule.onNodeWithText("Smart Snoozing").performScrollTo().performClick()

        assertThat(Pref.getBoolean("smart_snoozing", true)).isFalse()
    }

    @Test
    fun dependentRowDisabledUntilMasterEnabled() {
        Pref.setBoolean("disable_alerts_stale_data", false)

        composeRule.onNodeWithTag("setting_category_alarms").performClick()
        composeRule.onNodeWithTag("setting_bg_alerts").performScrollTo().performClick()
        composeRule.onNodeWithText("Suppress Alerts if missed readings").performScrollTo().performClick()

        // Master off -> dependent numeric row does not react.
        composeRule.onNodeWithTag("setting_stale_minutes").performScrollTo().performClick()
        composeRule.onNodeWithText("OK").assertDoesNotExist()

        // Master on -> dependent row opens its dialog.
        composeRule.onNodeWithTag("setting_stale_enabled").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_stale_minutes").performScrollTo().performClick()
        composeRule.onNodeWithText("OK").assertExists()
    }

    @Test
    fun searchNavigatesToMigratedSetting() {
        composeRule.onNodeWithTag("setting_search").performTextInput("calibration")
        composeRule.onNodeWithText("Calibration Alerts").performClick()

        composeRule.onNodeWithTag("setting_calibration_notifications").assertExists()
    }

    @Test
    fun searchNavigatesToNewlyCoveredDestination() {
        composeRule.onNodeWithTag("setting_search").performTextInput("health")
        composeRule.onNodeWithText("Google Health Connect").performClick()

        composeRule.onNodeWithText("Use Health Connect").assertExists()
    }

    @Test
    fun themeEditorSetsAndResetsOverride() {
        ThemeColorStore.clearAll()

        composeRule.onNodeWithTag("setting_category_appearance").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_theme").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_theme_PRIMARY").performScrollTo().performClick()
        composeRule.onNodeWithText("OK").performClick()
        assertThat(ThemeColorStore.isOverridden(ThemeColor.PRIMARY)).isTrue()

        // Now that it is custom, the picker offers a reset back to Material You.
        composeRule.onNodeWithTag("setting_theme_PRIMARY").performScrollTo().performClick()
        composeRule.onNodeWithText("Use Material You default").performClick()
        assertThat(ThemeColorStore.isOverridden(ThemeColor.PRIMARY)).isFalse()
    }

    @Test
    fun themeGlobalResetClearsOverrides() {
        ThemeColorStore.setOverride(ThemeColor.PRIMARY, 0xFF123456.toInt())

        composeRule.onNodeWithTag("setting_category_appearance").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_theme").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_theme_reset_all").performClick()

        assertThat(ThemeColorStore.isOverridden(ThemeColor.PRIMARY)).isFalse()
    }

    @Test
    fun dataSourceWebFollowGateHiddenByDefault() {
        Pref.setString("dex_collection_method", "BluetoothWixel")

        composeRule.onNodeWithTag("setting_category_devices").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_data_source").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_web_follow").assertDoesNotExist()
        composeRule.onNodeWithTag("setting_share_key").assertDoesNotExist()
    }

    @Test
    fun dataSourceWebFollowGateVisibleForWebFollow() {
        Pref.setString("dex_collection_method", "WebFollower")

        composeRule.onNodeWithTag("setting_category_devices").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_data_source").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_web_follow").assertExists()
    }

    @Test
    fun dataSourceNfcGateVisibleForLibre() {
        Pref.setString("dex_collection_method", "LimiTTer")

        composeRule.onNodeWithTag("setting_category_devices").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_data_source").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_libre_device").performScrollTo().assertExists().performClick()

        composeRule.onNodeWithTag("setting_nfc").assertExists()
    }

    @Test
    fun dataSyncAutoConfigReachable() {
        composeRule.onNodeWithTag("setting_category_data").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_cloud_sync").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_auto_config").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_auto_configure").assertExists()
    }

    @Test
    fun webDepositHiddenWithoutEngineeringMode() {
        Pref.setBoolean("engineering_mode", false)

        composeRule.onNodeWithTag("setting_category_data").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_cloud_sync").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_cloud_upload").performScrollTo().performClick()

        composeRule.onNodeWithText("Web Deposit").assertDoesNotExist()
    }

    @Test
    fun advancedExtraStatusLineToggleWritesPref() {
        Pref.setBoolean("extra_status_line", false)

        composeRule.onNodeWithTag("setting_category_appearance").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_nav_extra_status_line").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_extra_status_line").performScrollTo().performClick()

        assertThat(Pref.getBoolean("extra_status_line", false)).isTrue()
    }

    @Test
    fun advancedCalibrationPluginRowPresent() {
        composeRule.onNodeWithTag("setting_category_advanced").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_calibration").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_calibration_plugin").assertExists()
    }

    @Test
    fun libreOptionsRowVisibleForLibreCollectionMethod() {
        Pref.setString("dex_collection_method", "LimiTTer")

        composeRule.onNodeWithTag("setting_category_devices").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_libre_options").performScrollTo().assertExists()
    }

    @Test
    fun libreOptionsRowHiddenForNonLibreCollectionMethod() {
        Pref.setString("dex_collection_method", "BluetoothWixel")

        composeRule.onNodeWithTag("setting_category_devices").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_libre_options").assertDoesNotExist()
    }

    @Test
    fun libreOptionsShowsOnlyLimiTTerRows() {
        Pref.setString("dex_collection_method", "LimiTTer")

        composeRule.onNodeWithTag("setting_category_devices").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_libre_options").performScrollTo().performClick()

        listOf(
            "setting_external_blukon_algorithm",
            "setting_retrieve_blukon_history",
            "setting_libre_sn_changes",
            "setting_non_fixed_li_parameters",
        ).forEach { composeRule.onNodeWithTag(it).performScrollTo().assertExists() }
        listOf(
            "setting_libre_smoothed_data",
            "setting_libre_one_minute",
            "setting_libre2_show_raw_graph",
            "setting_calibrate_libre_algorithm",
        ).forEach { composeRule.onNodeWithTag(it).assertDoesNotExist() }
    }

    @Test
    fun libreOptionsShowsOnlyLibreReceiverRows() {
        Pref.setString("dex_collection_method", "LibreReceiver")

        composeRule.onNodeWithTag("setting_category_devices").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_libre_options").performScrollTo().performClick()

        listOf(
            "setting_external_blukon_algorithm",
            "setting_retrieve_blukon_history",
            "setting_calibrate_libre_algorithm",
            "setting_libre_one_minute",
            "setting_libre2_show_raw_graph",
            "setting_libre2_show_sensors",
        ).forEach { composeRule.onNodeWithTag(it).performScrollTo().assertExists() }
        composeRule.onNodeWithTag("setting_non_fixed_li_parameters").assertDoesNotExist()
        composeRule.onNodeWithTag("setting_libre_sn_changes").assertDoesNotExist()
    }

    @Test
    fun libreOptionsShowsOnlyLibreAlarmRows() {
        Pref.setString("dex_collection_method", "LibreAlarm")

        composeRule.onNodeWithTag("setting_category_devices").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_libre_options").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_libre_smoothed_data").performScrollTo().assertExists()
        composeRule.onNodeWithTag("setting_libre_sn_changes").assertDoesNotExist()
        composeRule.onNodeWithTag("setting_libre_one_minute").assertDoesNotExist()
        composeRule.onNodeWithTag("setting_external_blukon_algorithm").assertDoesNotExist()
    }

    @Test
    fun libreOneMinuteToggleWritesPref() {
        Pref.setString("dex_collection_method", "LibreReceiver")
        Pref.setBoolean("libre_one_minute", false)

        composeRule.onNodeWithTag("setting_category_devices").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_libre_options").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_libre_one_minute").performScrollTo().performClick()

        assertThat(Pref.getBoolean("libre_one_minute", false)).isTrue()
    }

    @Test
    fun retrieveBlukonHistoryDisabledWhileExternalAlgorithmEnabled() {
        Pref.setString("dex_collection_method", "LimiTTer")
        Pref.setBoolean("external_blukon_algorithm", true)

        composeRule.onNodeWithTag("setting_category_devices").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_libre_options").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_retrieve_blukon_history").performScrollTo()
            .assertIsNotEnabled()
    }
}
