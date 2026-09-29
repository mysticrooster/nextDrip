package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

public class ActiveBgAlertTest extends RobolectricTestWithConfig {

    private AppDatabase database;

    @Before
    public void setUpDatabase() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        AppDatabase.setInstanceForTesting(database);
    }

    @After
    public void tearDownDatabase() {
        database.close();
        AppDatabase.setInstanceForTesting(null);
    }

    @Test
    public void createInsertsRow() {
        ActiveBgAlert.Create("uuid-1", false, JoH.tsl() + 60000);

        assertWithMessage("one row created").that(database.activeBgAlertDao().count()).isEqualTo(1);
        assertWithMessage("read back uuid").that(ActiveBgAlert.getOnly().alert_uuid).isEqualTo("uuid-1");
    }

    @Test
    public void createReusesExistingRow() {
        ActiveBgAlert.Create("uuid-1", false, JoH.tsl() + 60000);
        ActiveBgAlert.Create("uuid-2", false, JoH.tsl() + 60000);

        assertWithMessage("still a single row").that(database.activeBgAlertDao().count()).isEqualTo(1);
        assertWithMessage("row updated in place").that(ActiveBgAlert.getOnly().alert_uuid).isEqualTo("uuid-2");
    }

    @Test
    public void clearDataRemovesRow() {
        ActiveBgAlert.Create("uuid-1", false, JoH.tsl() + 60000);
        ActiveBgAlert.ClearData();

        assertWithMessage("no rows remain").that(ActiveBgAlert.getOnly()).isNull();
    }

    @Test
    public void snoozePersistsState() {
        ActiveBgAlert.Create("uuid-1", false, JoH.tsl() + 60000);
        ActiveBgAlert.getOnly().snooze(5);

        final ActiveBgAlert reloaded = ActiveBgAlert.getOnly();
        assertWithMessage("snoozed flag persisted").that(reloaded.is_snoozed).isTrue();
        assertWithMessage("next alert in the future").that(reloaded.next_alert_at).isGreaterThan(JoH.tsl());
    }

    @Test
    public void currentlyAlertingTracksSnooze() {
        ActiveBgAlert.Create("uuid-1", false, JoH.tsl() + 60000);
        assertWithMessage("alerting when not snoozed").that(ActiveBgAlert.currentlyAlerting()).isTrue();

        ActiveBgAlert.getOnly().snooze(5);
        assertWithMessage("not alerting while snoozed").that(ActiveBgAlert.currentlyAlerting()).isFalse();
    }
}
