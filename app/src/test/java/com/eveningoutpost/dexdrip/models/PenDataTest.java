package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

public class PenDataTest extends RobolectricTestWithConfig {

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
    public void createAndSaveStoresRecord() {
        PenData.create("AA:BB", "inpen", 5, 2.0, 1000L, 20.0, new byte[]{1, 2}).save();

        assertWithMessage("highest index read back").that(PenData.getHighestIndex("AA:BB")).isEqualTo(5);
        assertWithMessage("one row stored").that(database.penDataDao().count()).isEqualTo(1);
    }

    @Test
    public void duplicateTimestampIsIgnored() {
        PenData.create("AA:BB", "inpen", 1, 1.0, 1000L, 20.0, null).save();
        PenData.create("AA:BB", "inpen", 2, 1.0, 1000L, 20.0, null).save();

        assertWithMessage("unique timestamp conflict ignored").that(database.penDataDao().count()).isEqualTo(1);
    }

    @Test
    public void saveUpdatesExistingRow() {
        PenData.create("AA:BB", "inpen", 5, 2.0, 1000L, 20.0, null).save();

        final PenData loaded = database.penDataDao().highestIndex("AA:BB");
        loaded.units = 3.0;
        loaded.save();

        assertWithMessage("updated in place").that(database.penDataDao().count()).isEqualTo(1);
        assertWithMessage("value updated").that(database.penDataDao().highestIndex("AA:BB").units).isEqualTo(3.0);
    }

    @Test
    public void getMissingIndexDetectsGap() {
        PenData.create("AA:BB", "inpen", 10, 1.0, JoH.tsl() - 1000, 20.0, null).save();
        PenData.create("AA:BB", "inpen", 9, 1.0, JoH.tsl() - 2000, 20.0, null).save();
        PenData.create("AA:BB", "inpen", 7, 1.0, JoH.tsl() - 3000, 20.0, null).save();

        assertWithMessage("gap of 8 detected").that(PenData.getMissingIndex("AA:BB")).isEqualTo(8);
    }

    @Test
    public void getAllRecordsBetweenFiltersByTimestamp() {
        PenData.create("AA:BB", "inpen", 1, 1.0, 1000L, 20.0, null).save();
        PenData.create("AA:BB", "inpen", 2, 1.0, 5000L, 20.0, null).save();

        assertWithMessage("only the in-range record returned")
                .that(PenData.getAllRecordsBetween(1000L, 3000L)).hasSize(1);
    }
}
