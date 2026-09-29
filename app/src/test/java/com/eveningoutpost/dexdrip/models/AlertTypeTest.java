package com.eveningoutpost.dexdrip.models;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Characterization tests for {@link AlertType}, covering each method that reads from
 * {@code PreferenceManager.getDefaultSharedPreferences()}, so the android.preference →
 * androidx.preference import swap is proven behaviour-preserving.
 *
 * <p>Also covers the Room-backed storage introduced by the ActiveAndroid → Room migration.
 *
 * @author Asbjørn Aarrestad
 */
public class AlertTypeTest extends RobolectricTestWithConfig {

    private AppDatabase database;
    private Context context;

    // --- Setup ---

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        context = RuntimeEnvironment.getApplication();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        AppDatabase.setInstanceForTesting(database);
    }

    @After
    public void tearDownDatabase() {
        database.close();
        AppDatabase.setInstanceForTesting(null);
    }

    // --- get_highest_active_alert ---

    /** Characterization: returns null while alerts are snoozed via alerts_disabled_until. */
    @Test
    public void get_highest_active_alert_returnsNullWhenAlertsDisabled() {
        // :: Setup
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        prefs.edit().putLong("alerts_disabled_until", new Date().getTime() + 60_000).commit();

        // :: Act
        AlertType highestActiveAlert = AlertType.get_highest_active_alert(xdrip.getAppContext(), 100);

        // :: Verify
        assertThat(highestActiveAlert).isNull();
    }

    // --- toSettings ---

    /** Characterization: toSettings serialises the alert table into the saved_alerts preference. */
    @Test
    public void toSettings_writesSavedAlertsPreference() {
        // :: Act
        boolean result = AlertType.toSettings(xdrip.getAppContext());

        // :: Verify
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        assertThat(result).isTrue();
        assertThat(prefs.contains("saved_alerts")).isTrue();
    }

    // --- fromSettings ---

    /** Characterization: fromSettings returns true (no-op) when no saved_alerts string is present. */
    @Test
    public void fromSettings_returnsTrueWhenNothingSaved() {
        // :: Setup
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        prefs.edit().putString("saved_alerts", "").commit();

        // :: Act
        boolean result = AlertType.fromSettings(xdrip.getAppContext());

        // :: Verify
        assertThat(result).isTrue();
    }

    // --- Room-backed storage ---

    private static void addHigh(String uuid, String name, double threshold) {
        AlertType.add_alert(uuid, name, true, threshold, true, 10, null, 0, 0, true, true, 20, true, true);
    }

    private static void addLow(String uuid, String name, double threshold) {
        AlertType.add_alert(uuid, name, false, threshold, true, 10, null, 0, 0, true, true, 20, true, true);
    }

    @Test
    public void addAndGetAlert() {
        addHigh("uuid-1", "high", 180);

        final AlertType at = AlertType.get_alert("uuid-1");
        assertWithMessage("alert found").that(at).isNotNull();
        assertWithMessage("threshold stored").that(at.threshold).isEqualTo(180);
        assertWithMessage("direction stored").that(at.above).isTrue();
    }

    @Test
    public void getAllSplitsByDirectionAndOrders() {
        addHigh("h1", "high1", 180);
        addHigh("h2", "high2", 220);
        addLow("l1", "low1", 80);
        addLow("l2", "low2", 60);

        final List<AlertType> highs = AlertType.getAll(true);
        assertWithMessage("two high alerts").that(highs).hasSize(2);
        assertWithMessage("highs ascending").that(highs.get(0).threshold).isEqualTo(180);

        final List<AlertType> lows = AlertType.getAll(false);
        assertWithMessage("two low alerts").that(lows).hasSize(2);
        assertWithMessage("lows descending").that(lows.get(0).threshold).isEqualTo(80);
    }

    @Test
    public void updateAlertChangesFields() {
        addHigh("uuid-1", "before", 180);
        AlertType.update_alert("uuid-1", "after", true, 200, true, 10, null, 0, 0, true, true, 20, true, true);

        final AlertType at = AlertType.get_alert("uuid-1");
        assertWithMessage("name updated").that(at.name).isEqualTo("after");
        assertWithMessage("threshold updated").that(at.threshold).isEqualTo(200);
    }

    @Test
    public void removeAlertDeletesIt() {
        addHigh("uuid-1", "high", 180);
        AlertType.remove_alert("uuid-1");

        assertWithMessage("alert removed").that(AlertType.get_alert("uuid-1")).isNull();
    }

    @Test
    public void removeAllClearsAlerts() {
        addHigh("h1", "high1", 180);
        addLow("l1", "low1", 80);
        AlertType.remove_all();

        assertWithMessage("all alerts cleared").that(AlertType.getAll(true)).isEmpty();
        assertWithMessage("no active bg alert").that(ActiveBgAlert.getOnly()).isNull();
    }

    @Test
    public void toSettingsThenFromSettingsRoundTrips() {
        addHigh("uuid-1", "high", 180);

        AlertType.toSettings(context);
        AlertType.remove_all();
        assertWithMessage("cleared before restore").that(AlertType.get_alert("uuid-1")).isNull();

        AlertType.fromSettings(context);
        assertWithMessage("restored from settings").that(AlertType.get_alert("uuid-1")).isNotNull();
        assertWithMessage("threshold preserved").that(AlertType.get_alert("uuid-1").threshold).isEqualTo(180);
    }
}
