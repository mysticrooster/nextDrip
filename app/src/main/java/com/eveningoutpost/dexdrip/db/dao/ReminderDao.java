package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.Reminder;

import java.util.List;

@Dao
public interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Reminder reminder);

    @Update
    void update(Reminder reminder);

    @Delete
    void delete(Reminder reminder);

    @Query("SELECT * FROM Reminder WHERE enabled = 1 AND next_due < :now AND snoozed_till < :now "
            + "ORDER BY enabled DESC, next_due ASC")
    List<Reminder> activeReminders(long now);

    @Query("SELECT * FROM Reminder WHERE enabled = 1 AND next_due < :now AND snoozed_till < :now "
            + "AND last_fired < (:now - (600000 * alerted_times)) AND homeonly > -1 "
            + "ORDER BY enabled DESC, priority DESC, next_due ASC LIMIT 1")
    Reminder nextActiveReminderAny(long now);

    @Query("SELECT * FROM Reminder WHERE enabled = 1 AND next_due < :now AND snoozed_till < :now "
            + "AND last_fired < (:now - (600000 * alerted_times)) AND homeonly = 0 "
            + "ORDER BY enabled DESC, priority DESC, next_due ASC LIMIT 1")
    Reminder nextActiveReminderNotHomeOnly(long now);

    @Query("SELECT * FROM Reminder ORDER BY enabled DESC, priority DESC, next_due ASC")
    List<Reminder> allReminders();

    @Query("SELECT * FROM Reminder WHERE _id = :id LIMIT 1")
    Reminder byid(long id);
}
