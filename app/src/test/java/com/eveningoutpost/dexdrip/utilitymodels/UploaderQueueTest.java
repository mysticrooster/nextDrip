package com.eveningoutpost.dexdrip.utilitymodels;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.models.JoH;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class UploaderQueueTest extends RobolectricTestWithConfig {

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

    private static UploaderQueue newQueued(String type, long wanted, long complete) {
        final UploaderQueue q = new UploaderQueue();
        q.timestamp = JoH.tsl();
        q.action = "insert";
        q.type = type;
        q.reference_id = 1;
        q.reference_uuid = "uuid-1";
        q.bitfield_wanted = wanted;
        q.bitfield_complete = complete;
        q.saveit();
        return q;
    }

    @Test
    public void pendingAndCompletedCounts() {
        newQueued("BgReading", 1, 0);

        assertWithMessage("pending").that(UploaderQueue.getQueueSizeByType("BgReading", 1, false)).isEqualTo(1);
        assertWithMessage("not completed").that(UploaderQueue.getQueueSizeByType("BgReading", 1, true)).isEqualTo(0);
        assertWithMessage("pending list").that(UploaderQueue.getPendingbyType("BgReading", 1)).hasSize(1);
    }

    @Test
    public void completedMovesOutOfPending() {
        final UploaderQueue q = newQueued("BgReading", 1, 0);
        q.completed(1);

        assertWithMessage("no longer pending").that(UploaderQueue.getQueueSizeByType("BgReading", 1, false)).isEqualTo(0);
        assertWithMessage("completed").that(UploaderQueue.getQueueSizeByType("BgReading", 1, true)).isEqualTo(1);
        assertWithMessage("pending list empty").that(UploaderQueue.getPendingbyType("BgReading", 1)).isEmpty();
    }

    @Test
    public void distinctTypesAndEmpty() {
        newQueued("BgReading", 1, 0);
        newQueued("Treatments", 1, 0);

        assertThat(database.uploaderQueueDao().distinctTypes()).containsExactly("BgReading", "Treatments");

        UploaderQueue.emptyQueue();
        assertThat(database.uploaderQueueDao().distinctTypes()).isEmpty();
    }

    @Test
    public void cleanQueueDeletesOldRows() {
        // Completed and older than 24h -> deleted by the 24h rule.
        final UploaderQueue oldCompleted = newQueued("BgReading", 1, 1);
        oldCompleted.timestamp = JoH.tsl() - 2 * 86400000L;
        oldCompleted.saveit();

        // Pending and older than 7 days -> deleted by the 7 day rule.
        final UploaderQueue oldPending = newQueued("Treatments", 1, 0);
        oldPending.timestamp = JoH.tsl() - 8 * 86400000L;
        oldPending.saveit();

        // Recent completed -> kept.
        newQueued("HeartRate", 1, 1);

        UploaderQueue.cleanQueue();

        assertThat(database.uploaderQueueDao().distinctTypes()).containsExactly("HeartRate");
    }
}
