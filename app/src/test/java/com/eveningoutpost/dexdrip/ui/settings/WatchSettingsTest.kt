package com.eveningoutpost.dexdrip.ui.settings

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S5a watches: navigation, key round-trips, dependency/visibility gating and the model/hardware
 * branches that can be seeded in Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class WatchSettingsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<SettingsActivity>()

    private fun openWatches() {
        composeRule.onNodeWithTag("setting_category_devices").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_smart_watch").performScrollTo().performClick()
    }

    @Test
    fun watchHubListsAllPlatforms() {
        openWatches()

        composeRule.onNodeWithTag("setting_watch_wear").assertExists()
        composeRule.onNodeWithTag("setting_watch_pebble").assertExists()
        composeRule.onNodeWithTag("setting_watch_amazfit").assertExists()
        composeRule.onNodeWithTag("setting_watch_bluejay").assertExists()
        composeRule.onNodeWithTag("setting_watch_lefun").assertExists()
        composeRule.onNodeWithTag("setting_watch_miband").assertExists()
        composeRule.onNodeWithTag("setting_watch_sensors").assertExists()
    }

    @Test
    fun wearSyncTogglesPref() {
        Pref.setBoolean("wear_sync", false)

        openWatches()
        composeRule.onNodeWithTag("setting_watch_wear").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_wear_sync").performScrollTo().performClick()

        assertThat(Pref.getBoolean("wear_sync", false)).isTrue()
    }

    @Test
    fun wearLogsPrefixDisabledUntilSyncEnabled() {
        Pref.setBoolean("wear_sync", true)
        Pref.setBoolean("sync_wear_logs", false)

        openWatches()
        composeRule.onNodeWithTag("setting_watch_wear").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_wear_logs_prefix").performScrollTo().performClick()
        composeRule.onNodeWithText("OK").assertDoesNotExist()

        composeRule.onNodeWithTag("setting_wear_sync_logs").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_wear_logs_prefix").performScrollTo().performClick()
        composeRule.onNodeWithText("OK").assertExists()
    }

    @Test
    fun pebbleTrendRowsVisibleForTrendTypes() {
        Pref.setString("broadcast_to_pebble_type", "3")

        openWatches()
        composeRule.onNodeWithTag("setting_watch_pebble").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_pebble_trend").performScrollTo().assertExists()
        composeRule.onNodeWithTag("setting_pebble_special_value").assertExists()
    }

    @Test
    fun pebbleStandardTypeHidesTrendRows() {
        Pref.setString("broadcast_to_pebble_type", "2")

        openWatches()
        composeRule.onNodeWithTag("setting_watch_pebble").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_pebble_trend").assertDoesNotExist()
        composeRule.onNodeWithTag("setting_pebble_special_value").assertExists()
    }

    @Test
    fun pebbleDisabledTypeHidesSpecialRows() {
        Pref.setString("broadcast_to_pebble_type", "1")

        openWatches()
        composeRule.onNodeWithTag("setting_watch_pebble").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_pebble_special_value").assertDoesNotExist()
    }

    @Test
    fun miBand4ShowsModelSpecificRows() {
        val mac = "AA:BB:CC:DD:EE:FF"
        Pref.setString("miband_data_mac", mac)
        PersistentStore.setString("miband_model_$mac", "Mi Smart Band 4")

        openWatches()
        composeRule.onNodeWithTag("setting_watch_miband").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_miband_authkey").performScrollTo().assertExists()
    }

    @Test
    fun unknownMiBandHidesModelSpecificRows() {
        Pref.setString("miband_data_mac", "")

        openWatches()
        composeRule.onNodeWithTag("setting_watch_miband").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_miband_authkey").assertDoesNotExist()
    }

    @Test
    fun miBandDebugSectionHiddenWithoutEngineeringMode() {
        Pref.setBoolean("miband_enabled", true)
        Pref.setBoolean("engineering_mode", false)

        openWatches()
        composeRule.onNodeWithTag("setting_watch_miband").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_miband_subscreen").performScrollTo().performClick()
        composeRule.onNodeWithText("Experimental").assertDoesNotExist()
    }

    @Test
    fun miBandDebugSectionVisibleWithEngineeringMode() {
        Pref.setBoolean("miband_enabled", true)
        Pref.setBoolean("engineering_mode", true)

        openWatches()
        composeRule.onNodeWithTag("setting_watch_miband").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_miband_subscreen").performScrollTo().performClick()
        composeRule.onNodeWithText("Experimental").assertExists()
    }

    @Test
    fun leFunFeaturesReachable() {
        openWatches()
        composeRule.onNodeWithTag("setting_watch_lefun").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_lefun_features").performScrollTo().performClick()

        composeRule.onNodeWithText("Features").assertExists()
    }

    @Test
    fun blueJayAdvancedReachable() {
        openWatches()
        composeRule.onNodeWithTag("setting_watch_bluejay").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_bluejay_advanced").performScrollTo().performClick()

        composeRule.onNodeWithTag("setting_bluejay_mac").assertExists()
    }

    @Test
    fun blueJayPhoneCollectorVetoRejectsChange() {
        Pref.setBoolean("bluejay_enabled", true)
        Pref.setBoolean("engineering_mode", false)
        Pref.setBoolean("bluejay_run_phone_collector", false)
        Pref.setBoolean("bluejay_run_as_phone_collector", true)

        openWatches()
        composeRule.onNodeWithTag("setting_watch_bluejay").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_bluejay_advanced").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_bluejay_run_phone_collector").performScrollTo().performClick()

        assertThat(Pref.getBoolean("bluejay_run_phone_collector", true)).isFalse()
    }

    @Test
    fun amazfitEnableTogglesPref() {
        Pref.setBoolean("pref_amazfit_enable_key", false)

        openWatches()
        composeRule.onNodeWithTag("setting_watch_amazfit").performScrollTo().performClick()
        composeRule.onNodeWithTag("setting_amazfit_enable").performScrollTo().performClick()

        assertThat(Pref.getBoolean("pref_amazfit_enable_key", false)).isTrue()
    }
}
