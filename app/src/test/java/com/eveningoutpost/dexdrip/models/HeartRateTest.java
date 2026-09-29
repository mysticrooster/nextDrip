package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

public class HeartRateTest extends RobolectricTestWithConfig {

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
    public void createThenLastReturnsMostRecent() {
        HeartRate.create(JoH.tsl() - 1000, 60, 1);
        HeartRate.create(JoH.tsl() - 500, 70, 1);

        final HeartRate last = HeartRate.last();
        assertWithMessage("most recent returned").that(last.bpm).isEqualTo(70);
    }

    @Test
    public void lastIgnoresOldData() {
        HeartRate.create(JoH.tsl() - Constants.DAY_IN_MS * 2, 60, 1);

        assertWithMessage("stale record ignored").that(HeartRate.last()).isNull();
    }

    @Test
    public void duplicateTimestampIsIgnored() {
        HeartRate.create(1000L, 60, 1);
        HeartRate.create(1000L, 99, 1);

        assertWithMessage("unique timestamp enforced")
                .that(HeartRate.latestForGraph(10, 0L)).hasSize(1);
    }

    @Test
    public void latestForGraphOrdersAscendingAndAppliesLimit() {
        HeartRate.create(1000L, 1, 1);
        HeartRate.create(3000L, 3, 1);
        HeartRate.create(2000L, 2, 1);

        final java.util.List<HeartRate> list = HeartRate.latestForGraph(2, 0L, Long.MAX_VALUE);
        assertWithMessage("limited to 2").that(list).hasSize(2);
        assertWithMessage("ascending order").that(list.get(0).bpm).isEqualTo(1);
        assertWithMessage("ascending order").that(list.get(1).bpm).isEqualTo(2);
    }

    @Test
    public void cleanupRemovesOnlyOldRows() {
        HeartRate.create(JoH.tsl() - Constants.DAY_IN_MS * 10, 1, 1);
        HeartRate.create(JoH.tsl(), 2, 1);

        HeartRate.cleanup(1);

        assertWithMessage("only recent row remains")
                .that(HeartRate.latestForGraph(10, 0L, Long.MAX_VALUE)).hasSize(1);
    }
}
