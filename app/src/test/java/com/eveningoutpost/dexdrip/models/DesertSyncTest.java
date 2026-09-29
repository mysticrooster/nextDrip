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

public class DesertSyncTest extends RobolectricTestWithConfig {

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

    private static DesertSync save(long timestamp, String topic) {
        final DesertSync d = new DesertSync();
        d.timestamp = timestamp;
        d.topic = topic;
        d.sender = "sender";
        d.payload = "payload";
        d.processed = "processed";
        d.save();
        return d;
    }

    @Test
    public void saveAndSince() {
        save(1000L, "topicA");

        assertWithMessage("all topics").that(DesertSync.since(0L, null)).hasSize(1);
        assertWithMessage("matching topic").that(DesertSync.since(0L, "topicA")).hasSize(1);
        assertWithMessage("other topic").that(DesertSync.since(0L, "topicB")).isEmpty();
        assertWithMessage("position filter").that(DesertSync.since(2000L, null)).isEmpty();
    }

    @Test
    public void deleteAllRemovesEverything() {
        save(1000L, "topicA");
        DesertSync.deleteAll();

        assertThat(DesertSync.since(0L, null)).isEmpty();
    }
}
