package com.eveningoutpost.dexdrip.ui.secondary

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.SnoozeActivity
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.utilitymodels.Constants
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Replaces the removed view-based `SnoozeActivityPreferencesTest`: the disabled-alert deadlines are
 * read into the screen state and rendered as the status line.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class SnoozeActivityPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        prefs().edit().clear().commit() // the preference file leaks between test methods
    }

    @Test
    fun storedAllAlertsDeadline_isReportedAsDisabled() {
        disableUntil(SnoozeActivity.SnoozeType.ALL_ALERTS, JoH.tsl() + Constants.HOUR_IN_MS)

        val status = snoozeUiState(context, prefs()).status

        assertThat(status).contains(string(R.string.all_alerts_disabled_until))
    }

    @Test
    fun storedLowAlertsDeadline_isReportedAsDisabled() {
        disableUntil(SnoozeActivity.SnoozeType.LOW_ALERTS, JoH.tsl() + Constants.HOUR_IN_MS)

        val status = snoozeUiState(context, prefs()).status

        assertThat(status).contains(string(R.string.low_alerts_disabled_until))
        assertThat(status).doesNotContain(string(R.string.high_alerts_disabled_until))
    }

    @Test
    fun storedHighAlertsDeadline_isReportedAsDisabled() {
        disableUntil(SnoozeActivity.SnoozeType.HIGH_ALERTS, JoH.tsl() + Constants.HOUR_IN_MS)

        val status = snoozeUiState(context, prefs()).status

        assertThat(status).contains(string(R.string.high_alerts_disabled_until))
        assertThat(status).doesNotContain(string(R.string.low_alerts_disabled_until))
    }

    @Test
    fun expiredDeadline_isNotReportedAsDisabled() {
        disableUntil(SnoozeActivity.SnoozeType.ALL_ALERTS, JoH.tsl() - Constants.HOUR_IN_MS)

        val status = snoozeUiState(context, prefs()).status

        assertThat(status).doesNotContain(string(R.string.all_alerts_disabled_until))
    }

    @Test
    fun nothingStored_reportsNoDisabledAlerts() {
        val status = snoozeUiState(context, prefs()).status

        assertThat(status).doesNotContain(string(R.string.all_alerts_disabled_until))
        assertThat(status).doesNotContain(string(R.string.low_alerts_disabled_until))
        assertThat(status).doesNotContain(string(R.string.high_alerts_disabled_until))
    }

    private fun disableUntil(type: SnoozeActivity.SnoozeType, until: Long) {
        prefs().edit().putLong(type.getPrefKey(), until).commit()
    }

    private fun string(resourceId: Int): String = context.getString(resourceId)

    private fun prefs(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
}
