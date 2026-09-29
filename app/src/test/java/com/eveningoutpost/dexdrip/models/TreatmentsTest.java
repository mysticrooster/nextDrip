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

public class TreatmentsTest extends RobolectricTestWithConfig {

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

    private static Treatments save(long timestamp, String uuid, double carbs) {
        final Treatments t = new Treatments();
        t.timestamp = timestamp;
        t.uuid = uuid;
        t.enteredBy = Treatments.XDRIP_TAG;
        t.eventType = "<none>";
        t.carbs = carbs;
        t.save();
        return t;
    }

    @Test
    public void saveAndLookupsWork() {
        final Treatments t = save(1000L, "uuid-1", 10);

        assertWithMessage("last").that(Treatments.last().uuid).isEqualTo("uuid-1");
        assertWithMessage("by uuid").that(Treatments.byuuid("uuid-1")).isNotNull();
        assertWithMessage("by id").that(Treatments.byid(t._id)).isNotNull();
        assertWithMessage("latest").that(Treatments.latest(5)).hasSize(1);
        assertWithMessage("list by timestamp").that(Treatments.listByTimestamp(1000L)).hasSize(1);
    }

    @Test
    public void byTimestampFindsWithinWindow() {
        save(1000L, "uuid-1", 10);

        assertWithMessage("within window").that(Treatments.byTimestamp(1200L, 500)).isNotNull();
        assertWithMessage("outside window").that(Treatments.byTimestamp(5000L, 500)).isNull();
    }

    @Test
    public void saveUpdatesExistingRow() {
        final Treatments t = save(1000L, "uuid-1", 10);
        t.carbs = 25;
        t.save();

        assertWithMessage("updated in place").that(Treatments.byuuid("uuid-1").carbs).isEqualTo(25);
        assertWithMessage("single row").that(Treatments.latest(5)).hasSize(1);
    }

    @Test
    public void deleteRemovesRow() {
        final Treatments t = save(1000L, "uuid-1", 10);
        t.delete();

        assertThat(Treatments.byuuid("uuid-1")).isNull();
    }

    @Test
    public void latestForGraphFiltersByTimestamp() {
        save(1000L, "uuid-1", 10);
        save(5000L, "uuid-2", 20);

        assertWithMessage("only in-range row").that(Treatments.latestForGraph(10, 0L, 3000L)).hasSize(1);
    }
}
