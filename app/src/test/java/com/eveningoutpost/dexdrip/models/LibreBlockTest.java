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

public class LibreBlockTest extends RobolectricTestWithConfig {

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

    private static LibreBlock saveTrendBlock(long timestamp) {
        final LibreBlock lb = LibreBlock.create("ref", timestamp, new byte[Constants.LIBRE_1_2_FRAM_SIZE], 0, null, null);
        lb.save();
        return lb;
    }

    @Test
    public void createSaveAndLookup() {
        final LibreBlock lb = saveTrendBlock(1000L);

        assertWithMessage("by uuid").that(LibreBlock.findByUuid(lb.uuid)).isNotNull();
        assertWithMessage("by id").that(LibreBlock.byid(lb._id)).isNotNull();
        assertWithMessage("by timestamp window").that(LibreBlock.getForTimestamp(1000L)).isNotNull();
    }

    @Test
    public void getForTrendFiltersByFramSize() {
        saveTrendBlock(1000L);
        // Wrong frame size, should be filtered out.
        LibreBlock.create("ref", 2000L, new byte[5], 0, null, null).save();

        assertWithMessage("only the valid frame returned").that(LibreBlock.getForTrend(0L, 5000L)).hasSize(1);
    }

    @Test
    public void getLatestForTrendReturnsNewest() {
        saveTrendBlock(1000L);
        saveTrendBlock(2000L);

        assertThat(LibreBlock.getLatestForTrend(0L, 5000L).timestamp).isEqualTo(2000L);
    }
}
