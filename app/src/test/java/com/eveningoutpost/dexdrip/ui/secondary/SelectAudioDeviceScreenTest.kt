package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.ui.activities.SelectAudioDevice
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class SelectAudioDeviceScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsConnectedDeviceAndSaves() {
        PersistentStore.setString("bluetooth-last-audio-connected-mac", "AA:BB:CC:DD:EE:FF")
        PersistentStore.setString("bluetooth-last-audio-connected-name", "Car Kit")
        var savedMac = ""

        composeRule.setContent {
            SelectAudioDeviceScreen(onSave = { mac, _ -> savedMac = mac }, onCancel = {})
        }

        composeRule.onNodeWithTag("audio_name").assertTextEquals("Car Kit")
        composeRule.onNodeWithTag("audio_save").performClick()

        assertThat(savedMac).isEqualTo("AA:BB:CC:DD:EE:FF")
    }

    @Test
    fun staticHelperStoresMac() {
        SelectAudioDevice.setAudioMac("11:22:33:44:55:66")

        assertThat(SelectAudioDevice.getAudioMac()).isEqualTo("11:22:33:44:55:66")
    }

    @Test
    fun finishesWhenNoDeviceConnected() {
        PersistentStore.setString("bluetooth-last-audio-connected-mac", "")

        val activity = Robolectric.buildActivity(SelectAudioDevice::class.java).create().get()

        assertThat(activity.isFinishing).isTrue()
    }
}
