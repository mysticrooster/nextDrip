package com.eveningoutpost.dexdrip.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.StepCounterDao;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

import lombok.val;

/**
 * Created by jamorham on 01/11/2016.
 */


@Entity(tableName = "PebbleMovement",
        indices = {
                @Index("source"),
                @Index(value = "timestamp", unique = true)
        })
public class StepCounter {

    private final static String TAG = "StepCounter";
    private final static boolean d = false;

    private static final int ABSOLUTE_MASK = 1;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @Expose
    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @Expose
    @ColumnInfo(name = "metric")
    public int metric;

    @Expose
    @ColumnInfo(name = "source")
    public int source;


    // patches and saves
    public Long saveit() {
        try {
            if (_id != 0) {
                dao().update(this);
            } else {
                final long id = dao().insertIgnoringConflicts(this);
                if (id > 0) {
                    _id = id;
                }
            }
            return _id;
        } catch (Exception e) {
            return null;
        }
    }

    public String toS() {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(this);
    }

    public boolean isAbsolute() {
        return ((source & ABSOLUTE_MASK) != 0);
    }

    // static methods

    public static StepCounter getForTimestamp(final long timestamp) {
        return dao().getForTimestamp(timestamp);
    }


    public static synchronized StepCounter createUniqueRecord(final long timestamp_ms, final int data, final boolean absolute) {
        if (getForTimestamp(timestamp_ms) == null) {
            val pm = new StepCounter();
            pm.timestamp = timestamp_ms;
            pm.metric = data;
            if (absolute) {
                pm.source |= ABSOLUTE_MASK;
            }
            pm.saveit();
            UserError.Log.d(TAG, "Created new record: " + pm.toS() + " " + JoH.dateTimeText(pm.timestamp));
            return pm;
        }
        return null;
    }

    public static StepCounter createEfficientRecord(long timestamp_ms, int data) {
        StepCounter pm = last();
        if ((pm == null) || (data < pm.metric) || ((timestamp_ms - pm.timestamp) > (1000 * 30 * 5))) {
            pm = new StepCounter();
            pm.timestamp = timestamp_ms;
            if (d)
                UserError.Log.d(TAG, "Creating new record for timestamp: " + JoH.dateTimeText(timestamp_ms));
        } else {
            if (d)
                UserError.Log.d(TAG, "Merging pebble movement record: " + JoH.dateTimeText(timestamp_ms) + " vs old " + JoH.dateTimeText(pm.timestamp));
        }

        pm.metric = (int) (long) data;
        if (d) UserError.Log.d(TAG, "Saving Movement: " + pm.toS());
        pm.saveit();
        return pm;
    }

    public static StepCounter last() {
        return dao().last();
    }

    public static int getDailyTotal() {
        int accumulator = 0;
        val list = latestForGraph(5000, JoH.tsl() - Constants.DAY_IN_MS, JoH.tsl()); // TODO since midnight vs 24 hours?
        for (val item : list) {
            if (item.isAbsolute()) {
                accumulator += item.metric;
            }
        }
        if (accumulator == 0) {
            val last = last();
            if (last != null) {
                return last.metric;
            } else {
                return 0;
            }
        } else {
            return accumulator; // total from absolutes
        }
    }

    public static List<StepCounter> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<StepCounter> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<StepCounter> latestForGraph(int number, long startTime, long endTime) {
        return dao().latestForGraph(Math.max(startTime, 0), endTime, number);
    }

    // expects pre-sorted in asc order?
    public static List<StepCounter> deltaListFromMovementList(List<StepCounter> mList) {
        int last_metric = -1;
        int temp_metric = -1;
        for (StepCounter pm : mList) {
            if (pm.isAbsolute()) continue;
            // first item in list
            if (last_metric == -1) {
                last_metric = pm.metric;
                pm.metric = 0;
            } else {
                // normal incrementing calculate delta
                if (pm.metric >= last_metric) {
                    temp_metric = pm.metric - last_metric;
                    last_metric = pm.metric;
                    pm.metric = temp_metric;
                } else {
                    last_metric = pm.metric;
                }
            }
        }
        return mList;
    }

    public static List<StepCounter> cleanup(int retention_days) {
        final int deleted = dao().cleanup(JoH.tsl() - (retention_days * 86400000L));
        UserError.Log.d(TAG, "StepCounter cleanup removed " + deleted + " record(s)");
        return new ArrayList<>();
    }

    private static StepCounterDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).stepCounterDao();
    }
}



