package com.eveningoutpost.dexdrip.models;

import android.os.AsyncTask;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.UserErrorDao;
import com.eveningoutpost.dexdrip.receiver.InfoContentProvider;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * Created by Emma Black on 8/3/15.
 */

@Entity(tableName = "UserErrors",
        indices = {
                @Index("severity"),
                @Index("timestamp")
        })
public class UserError {

    private final static String TAG = UserError.class.getSimpleName();

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @Expose
    @ColumnInfo(name = "shortError")
    public String shortError; // Short error message to be displayed on table

    @Expose
    @ColumnInfo(name = "message")
    public String message; // Additional text when error is expanded

    @Expose
    @ColumnInfo(name = "severity")
    public int severity; // int between 1 and 3, 3 being most severe

    // 5 = internal lower level user events
    // 6 = higher granularity user events

    @Expose
    @ColumnInfo(name = "timestamp")
    public long timestamp; // Time the error was raised

    //todo: rather than include multiples of the same error, should we have a "Count" and just increase that on duplicates?
    //or rather, perhaps we should group up the errors

    public String toString() {
        return severity + " ^ " + JoH.dateTimeText((long) timestamp) + " ^ " + shortError + " ^ " + message;
    }

    public UserError() {
    }

    @androidx.room.Ignore
    public UserError(int severity, String shortError, String message) {
        this.severity = severity;
        this.shortError = shortError;
        this.message = message;
        this.timestamp = new Date().getTime();
        this.save();
       /* if (xdrip.useBF) {
            switch (severity) {
                case 2:
                case 3:
                    Bugfender.e(shortError, message);
                    break;
                case 5:
                case 6:
                    Bugfender.w(shortError, message);
                    break;
                default:
                    Bugfender.d(shortError, message);
                    break;
            }
        }*/
    }

    @androidx.room.Ignore
    public UserError(String shortError, String message) {
        this(2, shortError, message);
    }

    public static UserError UserErrorHigh(String shortError, String message) {
        return new UserError(3, shortError, message);
    }

    public static UserError UserErrorLow(String shortError, String message) {
        return new UserError(1, shortError, message);
    }

    public static UserError UserEventLow(String shortError, String message) {
        return new UserError(5, shortError, message);
    }

    public static UserError UserEventHigh(String shortError, String message) {
        return new UserError(6, shortError, message);
    }

    // TODO move time calc stuff to JOH, wrap it here with our timestamp
    public String bestTime() {
        final long since = JoH.msSince(timestamp);
        if (since < Constants.DAY_IN_MS) {
            return JoH.hourMinuteString(timestamp);
        } else {
            return JoH.dateTimeText(timestamp);
        }
    }


    public static void cleanup() {
        new Cleanup().execute(deletable());
    }

    // used in unit testing
    public static void cleanup(long timestamp) {
        final List<UserError> userErrors = dao().olderThan(timestamp);
        if (userErrors != null) Log.d(TAG, "cleanup UserError size=" + userErrors.size());
        new Cleanup().execute(userErrors);
    }

    public synchronized static void cleanupRaw() {
        final long timestamp = JoH.tsl();
        dao().deleteLow(timestamp - Constants.DAY_IN_MS);        // severity < 3
        dao().deleteHigh(timestamp - Constants.DAY_IN_MS * 3);   // severity = 3
        dao().deleteEvents(timestamp - Constants.DAY_IN_MS * 7); // severity > 3
    }


    public static List<UserError> all() {
        return dao().all();
    }

    public static List<UserError> deletable() {
        final long now = new Date().getTime();
        final List<UserError> userErrors = new ArrayList<>(dao().deletableLow(now - 1000 * 60 * 60 * 24));
        userErrors.addAll(dao().deletableHigh(now - 1000 * 60 * 60 * 24 * 3));
        userErrors.addAll(dao().deletableEvents(now - 1000 * 60 * 60 * 24 * 7));
        return userErrors;
    }

    public static List<UserError> bySeverity(Integer[] levels) {
        return dao().bySeverity(Arrays.asList(levels));
    }

    public static List<UserError> bySeverityNewerThanID(long id, Integer[] levels, int limit) {
        return dao().bySeverityNewerThanID(id, Arrays.asList(levels), limit);
    }

    public static List<UserError> newerThanID(long id, int limit) {
        return dao().newerThanID(id, limit);
    }

    public static List<UserError> olderThanID(long id, int limit) {
        return dao().olderThanID(id, limit);
    }

    public static List<UserError> bySeverityOlderThanID(long id, Integer[] levels, int limit) {
        return dao().bySeverityOlderThanID(id, Arrays.asList(levels), limit);
    }


    public static UserError newestBySeverity(int level) {
        return dao().newestBySeverity(level);
    }

    public static UserError getForTimestamp(UserError error) {
        try {
            return dao().getForTimestamp(error.timestamp, error.shortError, error.message);
        } catch (Exception e) {
            Log.e(TAG, "getForTimestamp() Got exception on Select : " + e.toString());
            return null;
        }
    }

    /**
     * Best-effort insert-or-update. Logging must never crash or block the caller, so failures are
     * swallowed (reported to logcat only).
     */
    public Long save() {
        try {
            if (_id != 0) {
                dao().update(this);
            } else {
                final long id = dao().insert(this);
                if (id > 0) {
                    _id = id;
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to persist UserError: " + e);
        }
        return _id;
    }

    public void delete() {
        try {
            dao().delete(this);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to delete UserError: " + e);
        }
    }

    /** Mirrors the ActiveAndroid Model.getId() used by callers. */
    public Long getId() {
        return _id;
    }

    private static UserErrorDao dao() {
        // Non-gating: logging must never wait on (or block) the legacy import.
        return AppDatabase.getInstanceWithoutImportWait(xdrip.getAppContext()).userErrorDao();
    }

    private static class Cleanup extends AsyncTask<List<UserError>, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(List<UserError>... errors) {
            try {
                for (UserError userError : errors[0]) {
                    userError.delete();
                    //userError.save();
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static List<UserError> bySeverity(int level) {
        return bySeverity(new Integer[]{level});
    }

    public static List<UserError> bySeverity(int level, int level2) {
        return bySeverity(new Integer[]{level, level2});
    }

    public static List<UserError> bySeverity(int level, int level2, int level3) {
        return bySeverity(new Integer[]{level, level2, level3});
    }


    public static class Log {
        public static void e(String a, String b) {
            android.util.Log.e(a, b);
            new UserError(a, b);
        }

        public static void e(String tag, String b, Exception e) {
            android.util.Log.e(tag, b, e);
            StringBuilder sb = new StringBuilder();
            sb.append(b);
            sb.append("\n");
            sb.append(e.toString());
            sb.append("\n");
            StackTraceElement[] ste = e.getStackTrace();
            for (StackTraceElement ee : ste) {
                sb.append("    " + ee.toString() + "\n");
            }
            new UserError(tag, sb.toString());
        }

        public static void w(String tag, String b) {
            android.util.Log.w(tag, b);
            UserError.UserErrorLow(tag, b);
        }

        public static void w(String tag, String b, Exception e) {
            android.util.Log.w(tag, b, e);
            UserError.UserErrorLow(tag, b + "\n" + e.toString());
        }

        public static void wtf(String tag, String b) {
            android.util.Log.wtf(tag, b);
            UserError.UserErrorHigh(tag, b);
        }

        public static void wtf(String tag, String b, Exception e) {
            android.util.Log.wtf(tag, b, e);
            UserError.UserErrorHigh(tag, b + "\n" + e.toString());
        }

        public static void wtf(String tag, Exception e) {
            android.util.Log.wtf(tag, e);
            UserError.UserErrorHigh(tag, e.toString());
        }

        public static void uel(String tag, String b) {
            android.util.Log.i(tag, b);
            UserError.UserEventLow(tag, b);
        }

        public static void ueh(String tag, String b) {
            android.util.Log.i(tag, b);
            UserError.UserEventHigh(tag, b);
            InfoContentProvider.ping("info");
        }

        public static void d(String tag, String b) {
            android.util.Log.d(tag, b);
            if (ExtraLogTags.shouldLogTag(tag, android.util.Log.DEBUG)) {
                UserErrorLow(tag, b);
            }
        }

        public static void v(String tag, String b) {
            android.util.Log.v(tag, b);
            if (ExtraLogTags.shouldLogTag(tag, android.util.Log.VERBOSE)) {
                UserErrorLow(tag, b);
            }
        }

        public static void i(String tag, String b) {
            android.util.Log.i(tag, b);
            if (ExtraLogTags.shouldLogTag(tag, android.util.Log.INFO)) {
                UserErrorLow(tag, b);
            }
        }

    }

    public static class ExtraLogTags {

        private static final Hashtable<String, Integer> extraTags = new Hashtable<>();

        static {
            init();
        }

        private static void init() {
            try {
                extraTags.clear();
                readPreference(Pref.getStringDefaultBlank("extra_tags_for_logging"));
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Error with extra log tags: " + e);
            }
        }

        ExtraLogTags() {
            init();
        }

        /*
         * This function reads a string representing tags that the user wants to log
         * Format of string is tag1:level1,tag2,level2
         * Example of string is Alerts:i,BG:W
         *
         */
        public static void readPreference(String extraLogs) {
            extraLogs = extraLogs.trim();
            if (extraLogs.length() > 0) UserErrorLow(TAG, "called with string " + extraLogs);
            extraTags.clear();

            // allow splitting to work with a single entry and no delimiter zzz
            if ((extraLogs.length() > 1) && (!extraLogs.contains(","))) {
                extraLogs += ",";
            }
            String[] tags = extraLogs.split(",");
            if (tags.length == 0) {
                return;
            }

            // go over all tags and parse them
            for (String tag : tags) {
                if (tag.length() > 0) parseTag(tag);
            }
        }

        static void parseTag(String tag) {
            // Format is tag:level for example  Alerts:i
            String[] tagAndLevel = tag.trim().split(":");
            if (tagAndLevel.length != 2) {
                Log.e(TAG, "Failed to parse " + tag);
                return;
            }
            String level = tagAndLevel[1];
            String tagName = tagAndLevel[0].toLowerCase(); // TODO I would like to make this case sensitive for performance reasons
            if (level.compareTo("d") == 0) {
                extraTags.put(tagName, android.util.Log.DEBUG);
                UserErrorLow(TAG, "Adding tag with DEBUG " + tagAndLevel[0]);
                return;
            }
            if (level.compareTo("v") == 0) {
                extraTags.put(tagName, android.util.Log.VERBOSE);
                UserErrorLow(TAG, "Adding tag with VERBOSE " + tagAndLevel[0]);
                return;
            }
            if (level.compareTo("i") == 0) {
                extraTags.put(tagName, android.util.Log.INFO);
                UserErrorLow(TAG, "Adding tag with info " + tagAndLevel[0]);
                return;
            }
            Log.e(TAG, "Unknown level for tag " + tag + " please use d v or i");
        }

        public static boolean shouldLogTag(final String tag, final int level) {
            final Integer levelForTag = extraTags.get(tag != null ? tag.toLowerCase() : ""); // TODO I would like to make this case sensitive for performance reasons
            return levelForTag != null && level >= levelForTag;
        }

    }
}
