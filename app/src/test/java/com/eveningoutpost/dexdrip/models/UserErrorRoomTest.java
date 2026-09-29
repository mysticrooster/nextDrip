package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class UserErrorRoomTest extends RobolectricTestWithConfig {

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
    public void createQueryAndIds() {
        final UserError high = new UserError(3, "tag", "high");
        new UserError(1, "tag", "low");

        assertWithMessage("stored").that(UserError.all()).hasSize(2);
        assertWithMessage("id assigned").that(high.getId()).isGreaterThan(0);
        assertWithMessage("newest by severity").that(UserError.newestBySeverity(3).message).isEqualTo("high");
        assertWithMessage("by severity 3").that(UserError.bySeverity(new Integer[]{3})).hasSize(1);
        assertWithMessage("by severity 1 or 3").that(UserError.bySeverity(new Integer[]{1, 3})).hasSize(2);
    }

    @Test
    public void newerAndOlderThanId() {
        final UserError a = new UserError(1, "t", "a");
        final UserError b = new UserError(1, "t", "b");
        final UserError c = new UserError(1, "t", "c");

        final long[] ids = {a.getId(), b.getId(), c.getId()};
        Arrays.sort(ids);
        final long middle = ids[1];

        assertWithMessage("one newer").that(UserError.newerThanID(middle, 10)).hasSize(1);
        assertWithMessage("one older").that(UserError.olderThanID(middle, 10)).hasSize(1);
    }

    @Test
    public void getForTimestamp() {
        final UserError error = new UserError(1, "tag", "message");

        assertThat(UserError.getForTimestamp(error)).isNotNull();
    }

    @Test
    public void cleanupRawDeletesByAge() {
        final UserError old = new UserError(1, "tag", "old");
        old.timestamp = JoH.tsl() - Constants.DAY_IN_MS * 2;
        old.save();

        UserError.cleanupRaw();

        assertWithMessage("old low-severity error removed").that(UserError.all()).isEmpty();
    }
}
