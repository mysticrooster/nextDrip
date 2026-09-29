package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class BloodTestTest extends RobolectricTestWithConfig {

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
    public void createStoresAndLookupsWork() {
        final BloodTest bt = BloodTest.create(JoH.tsl(), 100, "test");

        assertThat(bt).isNotNull();
        assertWithMessage("state valid").that(bt.state).isEqualTo(BloodTest.STATE_VALID);
        assertWithMessage("last").that(BloodTest.last().mgdl).isEqualTo(100);
        assertWithMessage("by uuid").that(BloodTest.byUUID(bt.uuid)).isNotNull();
        assertWithMessage("by id").that(BloodTest.byid(bt._id)).isNotNull();
    }

    @Test
    public void createRejectsDuplicateTimestamp() {
        final long now = JoH.tsl();
        BloodTest.create(now, 100, "test");

        assertWithMessage("duplicate rejected").that(BloodTest.create(now, 100, "test")).isNull();
    }

    @Test
    public void addAndRemoveStatePersist() {
        final BloodTest bt = BloodTest.create(JoH.tsl(), 100, "test");

        bt.removeState(BloodTest.STATE_VALID);
        assertWithMessage("state removed").that(BloodTest.byid(bt._id).state & BloodTest.STATE_VALID).isEqualTo(0);

        bt.addState(BloodTest.STATE_VALID);
        assertWithMessage("state restored").that(BloodTest.byid(bt._id).state & BloodTest.STATE_VALID).isNotEqualTo(0);
    }

    @Test
    public void lastValidFiltersByState() {
        final BloodTest bt = BloodTest.create(JoH.tsl(), 100, "test");
        bt.removeState(BloodTest.STATE_VALID);

        assertWithMessage("no valid rows").that(BloodTest.lastValid()).isNull();
    }
}
