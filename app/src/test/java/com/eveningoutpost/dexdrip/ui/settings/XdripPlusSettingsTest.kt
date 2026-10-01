package com.eveningoutpost.dexdrip.ui.settings

import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.ui.activities.NumberWallPreview
import com.eveningoutpost.dexdrip.ui.activities.TimePickerPrefActivity
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.DisplayQRCode
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * S5b — xDrip+ Extra Settings: navigation, key round-trips, gating, intents and the theme editor's
 * legacy group parity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class XdripPlusSettingsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<SettingsActivity>()

    private fun click(tag: String) = composeRule.onNodeWithTag(tag).performScrollTo().performClick()

    private fun openCategory(tag: String) = composeRule.onNodeWithTag(tag).performScrollTo().performClick()

    private fun nextStartedActivity(): Intent? = Shadows.shadowOf(composeRule.activity).nextStartedActivity

    /**
     * Root rows are gated while the activity composes (at rule start), so a raw pref write in the
     * test body does not recompose them. Commit the value and recreate the host, then navigate.
     */
    private fun setPrefThenRecreate(key: String, value: Boolean) {
        Pref.getInstance().edit().putBoolean(key, value).commit()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    @Test
    fun extraSettingsRootListsScreens() {
        // The former "extra settings" rows now live inside their IA category buttons.
        composeRule.onNodeWithTag("setting_category_general").performScrollTo().assertExists()
        composeRule.onNodeWithTag("setting_category_alarms").assertExists()
        composeRule.onNodeWithTag("setting_category_data").assertExists()
        composeRule.onNodeWithTag("setting_category_profile").assertExists()
        composeRule.onNodeWithTag("setting_category_devices").assertExists()
        composeRule.onNodeWithTag("setting_category_appearance").assertExists()
        composeRule.onNodeWithTag("setting_category_accessibility").assertExists()
        composeRule.onNodeWithTag("setting_category_advanced").assertExists()
        composeRule.onNodeWithTag("setting_category_about").assertExists()
    }

    @Test
    fun copyingLaunchesQrCodeIntent() {
        openCategory("setting_category_data")
        click("setting_xdrip_copying")
        click("setting_show_qr_codes")

        assertThat(nextStartedActivity()?.component?.className).isEqualTo(DisplayQRCode::class.java.name)
    }

    @Test
    fun updateChannelHidesNightlyWithoutEngineeringMode() {
        Pref.setBoolean("engineering_mode", false)
        Pref.setString("update_channel", "beta")

        openCategory("setting_category_advanced")
        click("setting_xdrip_update")
        click("setting_update_channel")

        composeRule.onNodeWithText("Nightly - untested raw code - experts only!").assertDoesNotExist()
    }

    @Test
    fun updateChannelShowsNightlyInEngineeringMode() {
        Pref.setBoolean("engineering_mode", true)
        Pref.setString("update_channel", "beta")

        openCategory("setting_category_advanced")
        click("setting_xdrip_update")
        click("setting_update_channel")

        composeRule.onNodeWithText("Nightly - untested raw code - experts only!").assertExists()
    }

    @Test
    fun telemetryDisabledUntilCrashlyticsEnabled() {
        Pref.setBoolean("engineering_mode", false)
        Pref.setBoolean("enable_crashlytics", false)
        Pref.setBoolean("enable_telemetry", false)

        openCategory("setting_category_advanced")
        click("setting_xdrip_update")
        composeRule.onNodeWithTag("setting_enable_telemetry").performScrollTo().performClick()

        assertThat(Pref.getBoolean("enable_telemetry", false)).isFalse()
    }

    @Test
    fun motionRowsDisabledUntilMotionEnabled() {
        Pref.setBoolean("motion_tracking_enabled", false)
        Pref.setBoolean("plot_motion", false)

        openCategory("setting_category_advanced")
        click("setting_xdrip_motion")
        composeRule.onNodeWithTag("setting_plot_motion").performScrollTo().performClick()

        assertThat(Pref.getBoolean("plot_motion", false)).isFalse()
    }

    @Test
    fun pensNavigateToNovopen() {
        openCategory("setting_category_devices")
        click("setting_xdrip_pens")
        click("setting_pens_novopen")

        composeRule.onNodeWithTag("setting_opennov_enabled").assertExists()
    }

    @Test
    fun inpenResetLaunchesHomeIntent() {
        Pref.setBoolean("engineering_mode", false)

        openCategory("setting_category_devices")
        click("setting_xdrip_pens")
        click("setting_pens_inpen")
        click("setting_inpen_reset")

        val intent = nextStartedActivity()
        assertThat(intent?.component?.className).isEqualTo("com.eveningoutpost.dexdrip.Home")
        assertThat(intent?.getStringExtra("inpen-reset")).isEqualTo("inpen-reset")
    }

    @Test
    fun predictionGatedByIUnderstand() {
        Pref.setBoolean("I_understand", false)
        setPrefThenRecreate("I_understand", false)

        openCategory("setting_category_profile")
        click("setting_xdrip_prediction")

        // The gated row is disabled, so its sub-screen does not open.
        composeRule.onNodeWithTag("setting_simulations_enabled").assertDoesNotExist()
    }

    @Test
    fun predictionEditorLaunchesProfileEditor() {
        Pref.setBoolean("I_understand", true)
        setPrefThenRecreate("I_understand", true)

        openCategory("setting_category_profile")
        click("setting_profile_carb_ratio")

        assertThat(nextStartedActivity()?.component?.className)
            .isEqualTo("com.eveningoutpost.dexdrip.profileeditor.ProfileEditor")
    }

    @Test
    fun syncGeneratesCustomKeyWhenEmpty() {
        Pref.setString("custom_sync_key", "")
        setPrefThenRecreate("I_understand", true)

        openCategory("setting_category_data")
        click("setting_xdrip_sync")
        composeRule.waitForIdle()

        assertThat(Pref.getString("custom_sync_key", "")).isNotEmpty()
    }

    @Test
    fun desertSyncMasterIpHiddenWhenMaster() {
        Pref.setString("dex_collection_method", "BluetoothWixel")
        Pref.setBoolean("plus_follow_master", true)
        setPrefThenRecreate("I_understand", true)

        openCategory("setting_category_data")
        click("setting_xdrip_sync")
        click("setting_desert_sync")

        composeRule.onNodeWithTag("setting_desert_sync_master_ip").assertDoesNotExist()
    }

    @Test
    fun noiseUltrasensitiveHiddenWithoutEngineeringMode() {
        Pref.setBoolean("engineering_mode", false)

        openCategory("setting_category_appearance")
        click("setting_xdrip_display")

        composeRule.onNodeWithTag("setting_bg_compensate_noise_ultrasensitive").assertDoesNotExist()
    }

    @Test
    fun languageListWritesPref() {
        Pref.setBoolean("force_english", false)
        Pref.setString("forced_language", "en")
        Pref.setBoolean("engineering_mode", false)

        openCategory("setting_category_appearance")
        click("setting_language")
        click("setting_forced_language")
        composeRule.onNodeWithText("Deutsch").performClick()

        assertThat(Pref.getString("forced_language", "en")).isEqualTo("de")
    }

    @Test
    fun numberWallTimePickerCarriesPrefName() {
        openCategory("setting_category_appearance")
        click("setting_number_wall")
        click("setting_pick_numberwall_start")

        val intent = nextStartedActivity()
        assertThat(intent?.component?.className).isEqualTo(TimePickerPrefActivity::class.java.name)
        assertThat(intent?.getStringExtra("pref-name")).isEqualTo("number_wall_start_time")
    }

    @Test
    fun numberWallConfigLaunchesPreview() {
        openCategory("setting_category_appearance")
        click("setting_number_wall")
        click("setting_number_wall_config")

        assertThat(nextStartedActivity()?.component?.className).isEqualTo(NumberWallPreview::class.java.name)
    }

    @Test
    fun themeEditorRendersLegacyGroupsAndFlairSwitch() {
        openCategory("setting_category_appearance")
        click("setting_theme")

        composeRule.onNodeWithText("Reading dots").assertExists()
        composeRule.onNodeWithTag("setting_theme_HIGH_VALUES").assertExists()
        composeRule.onNodeWithTag("setting_theme_use_flair_colors").performScrollTo().assertExists()
        // Number-wall colours moved to the Number Wall screen.
        composeRule.onNodeWithTag("setting_theme_NUMBER_WALL").assertDoesNotExist()
    }

    @Test
    fun themeFlairSwitchRoundTrips() {
        Pref.setBoolean("use_flair_colors", false)

        openCategory("setting_category_appearance")
        click("setting_theme")
        composeRule.onNodeWithTag("setting_theme_use_flair_colors").performScrollTo().performClick()

        assertThat(Pref.getBoolean("use_flair_colors", false)).isTrue()
    }
}
