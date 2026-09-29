package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class PredictionTest extends RobolectricTestWithConfig {

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
    public void createSaveAndLast() {
        Prediction.create(1000L, 120, "test").save();

        final Prediction last = Prediction.last();
        assertThat(last).isNotNull();
        assertThat(last.glucose).isEqualTo(120);
        assertThat(last.source).isEqualTo("test");
    }

    @Test
    public void addNoteIsStored() {
        Prediction.create(1000L, 120, "test").addNote("hello").save();

        assertThat(Prediction.last().note).isEqualTo("hello");
    }

    @Test
    public void latestForGraphFiltersAndOrders() {
        Prediction.create(1000L, 1, "a").save();
        Prediction.create(3000L, 3, "a").save();
        Prediction.create(2000L, 2, "a").save();

        final List<Prediction> list = Prediction.latestForGraph(10, 0L, Long.MAX_VALUE);
        assertWithMessage("all three").that(list).hasSize(3);
        assertWithMessage("ascending by timestamp").that(list.get(0).glucose).isEqualTo(1);
        assertWithMessage("ascending by timestamp").that(list.get(1).glucose).isEqualTo(2);
    }
}
