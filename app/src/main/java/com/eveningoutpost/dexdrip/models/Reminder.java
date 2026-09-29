package com.eveningoutpost.dexdrip.models;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.Reminders;
import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.ReminderDao;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.HomeWifi;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.annotations.Expose;

import java.util.Calendar;
import java.util.List;


/**
 * Created by jamorham on 01/02/2017.
 */

@Entity(tableName = "Reminder",
        indices = {
                @Index("next_due"),
                @Index("enabled"),
                @Index("weekdays"),
                @Index("weekends"),
                @Index("homeonly"),
                @Index("priority"),
                @Index("snoozed_till"),
                @Index("last_fired")
        })
public class Reminder {


    private static final String TAG = "Reminder";
    public static final String REMINDERS_ALL_DISABLED = "reminders-all-disabled";
    public static final String REMINDERS_NIGHT_DISABLED = "reminders-at-night-disabled";
    public static final String REMINDERS_RESTART_TOMORROW = "reminders-restart-tomorrow";
    public static final String REMINDERS_ADVANCED_MODE = "reminders-advanced-mode";
    public static final String REMINDERS_CANCEL_DEFAULT = "reminders-cancel-default";
    public static final String REMINDERS_GRAPH_ICONS = "reminders-graph-icons";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @Expose
    @ColumnInfo(name = "title")
    public String title;

    @Expose
    @ColumnInfo(name = "alt_title")
    public String alternate_title;

    @Expose
    @ColumnInfo(name = "next_due")
    public long next_due;

    @Expose
    @ColumnInfo(name = "period")
    public long period;

    @Expose
    @ColumnInfo(name = "sound_uri")
    public String sound_uri;

    @Expose
    @ColumnInfo(name = "enabled")
    public boolean enabled;

    @Expose
    @ColumnInfo(name = "weekdays")
    public boolean weekdays;

    @Expose
    @ColumnInfo(name = "weekends")
    public boolean weekends;

    @Expose
    @ColumnInfo(name = "homeonly")
    public boolean homeonly;

    @Expose
    @ColumnInfo(name = "speak")
    public boolean speak;

    @Expose
    @ColumnInfo(name = "graphicon")
    public boolean graphicon;

    @Expose
    @ColumnInfo(name = "repeating")
    public boolean repeating;

    @Expose
    @ColumnInfo(name = "chime")
    public boolean chime_only;

    @Expose
    @ColumnInfo(name = "alternating")
    public boolean alternating;

    @Expose
    @ColumnInfo(name = "alternate")
    public boolean alternate;

    @Expose
    @ColumnInfo(name = "snoozed_till")
    public long snoozed_till;

    @Expose
    @ColumnInfo(name = "last_fired")
    public long last_fired;

    @Expose
    @ColumnInfo(name = "last_snoozed_for")
    public long last_snoozed_for;

    @Expose
    @ColumnInfo(name = "fired_times")
    public long fired_times;

    @Expose
    @ColumnInfo(name = "alerted_times")
    public long alerted_times;

    @Expose
    @ColumnInfo(name = "priority")
    public long priority;

    @Expose
    @ColumnInfo(name = "ideal_time")
    public String ideal_time;


    public boolean isAlternate() {
        if (alternating && (alternate_title != null)) {
            return alternate;
        } else {
            return false;
        }
    }

    public String getTitle() {
        return isAlternate() ? alternate_title : title;
    }

    public String getAlternateTitle() {
        return alternate_title != null ? alternate_title : title + " alternate";
    }

    public void updateTitle(String new_title) {
        if (isAlternate()) {
            alternate_title = new_title;
        } else {
            title = new_title;
        }
        save();
    }

    public boolean isHoursPeriod() {
        return (period >= Constants.HOUR_IN_MS) && (period < Constants.DAY_IN_MS);
    }

    public boolean isDaysPeriod() {
        return (period >= Constants.DAY_IN_MS) && (period < (Constants.WEEK_IN_MS * 2));
    }

    public boolean isWeeksPeriod() {
        return (period >= (2 * Constants.WEEK_IN_MS));
    }

    public long periodInUnits() {
        if (isDaysPeriod()) return period / Constants.DAY_IN_MS;
        if (isHoursPeriod()) return period / Constants.HOUR_IN_MS;
        if (isWeeksPeriod()) return period / Constants.WEEK_IN_MS;
        return -1; // ERROR
    }

    public boolean isDue() {
        return (enabled) && (next_due <= JoH.tsl());
    }

    public boolean isOverdueBy(long ms) {
       return isDue() && next_due <= (JoH.tsl() - ms);
    }

    public boolean isOverdue() {
        return isOverdueBy(Constants.DAY_IN_MS);
    }

    public boolean isSnoozed() {
        if (snoozed_till > JoH.tsl()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean shouldNotify() {
        return enabled && isDue() && !isSnoozed();
    }

    public synchronized void notified() {
        if (last_fired < next_due) fired_times++;
        alerted_times++;
        last_fired = JoH.tsl();
        if (chime_only) {
            if (repeating) {
                UserError.Log.d(TAG, "Rescheduling next");
                schedule_next();
            } else {
                enabled = false;
            }
        }
        save();
    }

    public synchronized void reminder_alert() {
        Reminders.doAlert(this);
    }

    public synchronized long getPotentialNextSchedule() {
        long now = JoH.tsl();
        long next = this.next_due + this.period;
        // check it is actually in the future
        while (next < now) {
            next += this.period;
        }
        return next;
    }

    public synchronized void schedule_next(long when) {
        this.next_due = when;
        UserError.Log.uel(TAG, "Scheduling next for: " + this.title + " to " + JoH.dateTimeText(this.next_due));
        if (alternating) alternate = !alternate;
        alerted_times = 0; // reset counter
        save();
    }

    public synchronized void schedule_next() {
        schedule_next(getPotentialNextSchedule());
    }

    public static Reminder create(String title, long period) {
        Reminder reminder = new Reminder();
        reminder.title = title;
        reminder.alternate_title = title + " alternate";
        reminder.period = period;
        reminder.next_due = JoH.tsl() + period;
        reminder.enabled = true;
        reminder.snoozed_till = 0;
        reminder.last_snoozed_for = 0;
        reminder.last_fired = 0;
        reminder.fired_times = 0;
        reminder.repeating = true;
        reminder.alternating = false;
        reminder.alternate = false;
        reminder.chime_only = false;
        reminder.homeonly = false;
        reminder.ideal_time = JoH.hourMinuteString();
        reminder.priority = 5; // default
        reminder.save();
        return reminder;
    }

    public static List<Reminder> getActiveReminders() {
        return dao().activeReminders(JoH.tsl());
    }

    private static boolean isNight() {
        final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour < 9; // midnight to 9am we say is night
    }

    public static synchronized void processAnyDueReminders() {
        if (JoH.quietratelimit("reminder_due_check", 10)) {
            if (!Pref.getBooleanDefaultFalse(REMINDERS_ALL_DISABLED)
                    && (!Pref.getBooleanDefaultFalse(REMINDERS_NIGHT_DISABLED) || !isNight())) {
                final Reminder due_reminder = getNextActiveReminder();
                if (due_reminder != null) {
                    UserError.Log.d(TAG, "Found due reminder! " + due_reminder.title);
                    due_reminder.reminder_alert();
                }
            } else {
                // reminders are disabled - should we re-enable them?
                if (Pref.getBooleanDefaultFalse(REMINDERS_RESTART_TOMORROW)) {
                    // temporary testing logic
                    final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    if (hour == 10) {
                        if (JoH.pratelimit("restart-reminders", 7200)) {
                            UserError.Log.d(TAG, "Re-enabling reminders as its morning time");
                            Pref.setBoolean(REMINDERS_ALL_DISABLED, false);
                        }
                    }
                }
            }
        }
    }

    public static Reminder getNextActiveReminder() {
        final boolean onHomeWifi = !HomeWifi.isSet() || HomeWifi.isConnected();
        final long now = JoH.tsl();
        return onHomeWifi ? dao().nextActiveReminderAny(now) : dao().nextActiveReminderNotHomeOnly(now);
    }

    public static List<Reminder> getAllReminders() {
        return dao().allReminders();
    }

    public synchronized static void firstInit(Context context) {
      /*  Inevitable.task("reminders-first-init", 2000, new Runnable() {
            @Override
            public void run() {
                try {
                    final Reminder reminder = new Select()
                            .from(Reminder.class)
                            .where("enabled = ?", true)
                            .executeSingle();
                    if (reminder != null) {
                        // PendingIntent serviceIntent = PendingIntent.getService(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), MissedReadingService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                        // PendingIntent serviceIntent = WakeLockTrampoline.getPendingIntent(MissedReadingService.class);
                        //  JoH.wakeUpIntent(xdrip.getAppContext(), Constants.MINUTE_IN_MS, serviceIntent);
                        //  UserError.Log.ueh(TAG, "Starting missed readings service");
                    }
                } catch (NullPointerException e) {
                    UserError.Log.wtf(TAG, "Got nasty initial concurrency exception: " + e);
                }
            }
        });
        */
    }

    public static Reminder byid(long id) {
        return dao().byid(id);
    }

    /**
     * Insert-or-update, mirroring the ActiveAndroid Model.save() used before the Room migration.
     */
    public Long save() {
        if (_id != 0) {
            dao().update(this);
        } else {
            final long id = dao().insert(this);
            if (id > 0) {
                _id = id;
            }
        }
        return _id;
    }

    /** Mirrors the ActiveAndroid Model.getId() used by callers. */
    public Long getId() {
        return _id;
    }

    public void delete() {
        dao().delete(this);
    }

    private static ReminderDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).reminderDao();
    }
}
