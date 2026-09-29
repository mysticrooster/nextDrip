package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class AccuracyTest extends RobolectricTestWithConfig {

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

    private static BloodTest bloodTest(long timestamp, double mgdl) {
        final BloodTest bt = new BloodTest();
        bt.timestamp = timestamp;
        bt.mgdl = mgdl;
        bt.source = "test";
        return bt;
    }

    private static BgReading bgReading(long timestamp, double value) {
        final BgReading bg = new BgReading();
        bg.timestamp = timestamp;
        bg.calculated_value = value;
        return bg;
    }

    @Test
    public void createStoresAndIsFoundByTimestamp() {
        final Accuracy created = Accuracy.create(bloodTest(1000L, 100), bgReading(1000L, 110), "plugin1");

        assertThat(created).isNotNull();
        assertThat(created.difference).isEqualTo(10.0);
        assertWithMessage("found by precise timestamp")
                .that(Accuracy.getForPreciseTimestamp(1000L, Constants.MINUTE_IN_MS, "plugin1")).isNotNull();
    }

    @Test
    public void createRejectsDuplicateTimestamp() {
        Accuracy.create(bloodTest(1000L, 100), bgReading(1000L, 110), "plugin1");

        assertWithMessage("duplicate rejected")
                .that(Accuracy.create(bloodTest(1000L, 100), bgReading(1000L, 110), "plugin1")).isNull();
    }

    @Test
    public void latestForGraphOrdersByTimestampDesc() {
        Accuracy.create(bloodTest(1000L, 100), bgReading(1000L, 110), "p");
        Accuracy.create(bloodTest(70000L, 100), bgReading(70000L, 130), "p");

        final java.util.List<Accuracy> list = Accuracy.latestForGraph(10, 0L, Long.MAX_VALUE);
        assertWithMessage("two rows").that(list).hasSize(2);
        assertWithMessage("descending").that(list.get(0).timestamp).isEqualTo(70000L);
    }
}
