package com.eveningoutpost.dexdrip.ui.settings

import android.content.Context
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Covers the destination-level search index: completeness, ranking, diacritics and gating. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class SettingsSearchTest {

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    private fun index() = buildSettingsSearchIndex(context)

    private fun results(query: String, state: SettingsState = SettingsState()): List<SettingsScreen> =
        searchSettings(index(), query, state).map { it.screen }

    @Test
    fun indexCoversEveryDestinationExceptRoot() {
        val indexed = index().map { it.screen }.toSet()
        assertThat(indexed).isEqualTo(SettingsScreen.entries.toSet() - SettingsScreen.Root)
    }

    @Test
    fun everyDestinationCarriesItsOwnTitleMetadata() {
        SettingsScreen.entries.filter { it != SettingsScreen.Root }.forEach { screen ->
            assertWithMessage(screen.name)
                .that(screen.titleRes != 0 || screen.titleLiteral != null)
                .isTrue()
            assertWithMessage(screen.name).that(screen.title(context)).isNotEmpty()
        }
        assertThat(SettingsScreen.Root.title(context)).isEqualTo("Settings")
    }

    @Test
    fun previouslyMissingDestinationsAreFindable() {
        assertThat(results("mongo")).contains(SettingsScreen.Mongo)
        assertThat(results("health")).contains(SettingsScreen.HealthConnect)
        assertThat(results("general")).contains(SettingsScreen.GeneralCategory)
        assertThat(results("tidepool")).contains(SettingsScreen.Tidepool)
    }

    @Test
    fun retiredCuratedTitlesStillMatch() {
        assertThat(results("advanced settings")).contains(SettingsScreen.OtherMiscSettings)
        assertThat(results("cloud sync")).contains(SettingsScreen.DataSync)
        assertThat(results("display settings")).contains(SettingsScreen.XdripPlusDisplay)
        assertThat(results("language settings")).contains(SettingsScreen.XdripPlusLanguage)
        assertThat(results("graph display settings")).contains(SettingsScreen.XdripPlusGraphDisplay)
        assertThat(results("y axis range")).contains(SettingsScreen.XdripPlusYAxis)
        assertThat(results("prediction settings")).contains(SettingsScreen.XdripPlusPrediction)
        assertThat(results("low prediction values")).contains(SettingsScreen.XdripPlusAdvPredict)
        assertThat(results("sync settings")).contains(SettingsScreen.XdripPlusSync)
    }

    @Test
    fun titlePrefixOutranksContainedTitleAndKeyword() {
        val found = results("cal")
        assertThat(found).containsAtLeast(SettingsScreen.CalibrationAlerts, SettingsScreen.CalibrationSettings)
        assertThat(found.indexOf(SettingsScreen.CalibrationAlerts))
            .isLessThan(found.indexOf(SettingsScreen.CalibrationSettings))
    }

    @Test
    fun accentedQueryMatchesLikeUnaccented() {
        assertThat(results("théme")).isEqualTo(results("theme"))
        assertThat(results("théme")).contains(SettingsScreen.Theme)
    }

    @Test
    fun libreOptionsHiddenUnlessLibreCollectionMethod() {
        Pref.setString("dex_collection_method", "BluetoothWixel")
        assertThat(results("libre options")).doesNotContain(SettingsScreen.LibreOptions)

        Pref.setString("dex_collection_method", "LimiTTer")
        assertThat(results("libre options")).contains(SettingsScreen.LibreOptions)
    }

    @Test
    fun webDepositHiddenUntilEngineeringMode() {
        Pref.setBoolean("engineering_mode", false)
        assertThat(results("web deposit")).doesNotContain(SettingsScreen.WebDeposit)

        Pref.setBoolean("engineering_mode", true)
        assertThat(results("web deposit")).contains(SettingsScreen.WebDeposit)
    }
}
