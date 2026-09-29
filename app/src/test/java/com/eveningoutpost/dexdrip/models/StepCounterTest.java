package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

public class StepCounterTest extends RobolectricTestWithConfig {

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
    public void createUniqueRecordStoresAndRejectsDuplicateTimestamp() {
        final StepCounter first = StepCounter.createUniqueRecord(1000L, 50, false);

        assertWithMessage("record created").that(first).isNotNull();
        assertWithMessage("value stored").that(StepCounter.getForTimestamp(1000L).metric).isEqualTo(50);
        assertWithMessage("duplicate rejected").that(StepCounter.createUniqueRecord(1000L, 60, false)).isNull();
    }

    @Test
    public void createEfficientRecordMergesInRange() {
        StepCounter.createEfficientRecord(1000L, 10);
        final StepCounter merged = StepCounter.createEfficientRecord(2000L, 20);

        assertWithMessage("same record").that(merged.timestamp).isEqualTo(1000L);
        assertWithMessage("metric updated").that(merged.metric).isEqualTo(20);
        assertWithMessage("single row").that(StepCounter.latestForGraph(10, 0L)).hasSize(1);
    }

    @Test
    public void createEfficientRecordStartsNewRecordWhenDataDrops() {
        StepCounter.createEfficientRecord(1000L, 100);
        StepCounter.createEfficientRecord(2000L, 5);

        assertWithMessage("new row created").that(StepCounter.latestForGraph(10, 0L)).hasSize(2);
    }

    @Test
    public void getDailyTotalSumsAbsoluteRecords() {
        StepCounter.createUniqueRecord(JoH.tsl() - 5000, 100, true);
        StepCounter.createUniqueRecord(JoH.tsl() - 4000, 50, true);

        assertWithMessage("absolute totals summed").that(StepCounter.getDailyTotal()).isEqualTo(150);
    }

    @Test
    public void totalStepsBetweenSumsRecords() {
        StepCounter.createUniqueRecord(JoH.tsl() - 5000, 42, false);

        assertWithMessage("stats query sums the metric")
                .that(database.stepCounterDao().totalStepsBetween(JoH.tsl() - 10000, JoH.tsl())).isEqualTo(42);
    }
}
