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

public class ReminderTest extends RobolectricTestWithConfig {

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
    public void createStoresReminder() {
        final Reminder reminder = Reminder.create("test", Constants.HOUR_IN_MS);

        assertThat(Reminder.getAllReminders()).hasSize(1);
        assertThat(reminder.getTitle()).isEqualTo("test");
        assertThat(reminder.enabled).isTrue();
        assertThat(reminder.next_due).isGreaterThan(JoH.tsl());
    }

    @Test
    public void byidAndDelete() {
        final Reminder reminder = Reminder.create("test", Constants.HOUR_IN_MS);
        assertWithMessage("found by id").that(Reminder.byid(reminder.getId())).isNotNull();

        reminder.delete();
        assertWithMessage("deleted").that(Reminder.byid(reminder.getId())).isNull();
    }

    @Test
    public void notifiedUpdatesCounters() {
        final Reminder reminder = Reminder.create("test", Constants.HOUR_IN_MS);
        reminder.next_due = JoH.tsl() - 1000;
        reminder.last_fired = 0;
        reminder.save();

        reminder.notified();

        final Reminder reloaded = Reminder.byid(reminder.getId());
        assertWithMessage("alerted count incremented").that(reloaded.alerted_times).isEqualTo(1);
        assertWithMessage("last fired set").that(reloaded.last_fired).isGreaterThan(0);
    }

    @Test
    public void scheduleNextUpdatesDue() {
        final Reminder reminder = Reminder.create("test", Constants.HOUR_IN_MS);
        final long when = JoH.tsl() + 12_345;
        reminder.schedule_next(when);

        assertThat(Reminder.byid(reminder.getId()).next_due).isEqualTo(when);
    }

    @Test
    public void getNextActiveReminderReturnsDueReminder() {
        final Reminder reminder = Reminder.create("test", Constants.HOUR_IN_MS);
        reminder.next_due = JoH.tsl() - 1000;
        reminder.snoozed_till = 0;
        reminder.last_fired = 0;
        reminder.save();

        assertThat(Reminder.getNextActiveReminder()).isNotNull();
        assertThat(Reminder.getActiveReminders()).hasSize(1);
    }
}
