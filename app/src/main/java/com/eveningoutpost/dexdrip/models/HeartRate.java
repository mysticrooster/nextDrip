package com.eveningoutpost.dexdrip.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.HeartRateDao;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jamorham on 01/11/2016.
 */


@Entity(tableName = "HeartRate",
        indices = {@Index(value = "timestamp", unique = true)})
public class HeartRate {

    private final static String TAG = "HeartRate";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @Expose
    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @Expose
    @ColumnInfo(name = "bpm")
    public int bpm;

    @Expose
    @ColumnInfo(name = "accuracy")
    public int accuracy;

    public static HeartRate last() {
        return dao().last(JoH.tsl() - Constants.DAY_IN_MS);
    }

    public static void create(long timestamp, int bpm, int accuracy) {
        final HeartRate hr = new HeartRate();
        hr.timestamp = timestamp;
        hr.bpm = bpm;
        hr.accuracy = accuracy;
        hr.saveit();
    }

    public static List<HeartRate> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<HeartRate> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    // TODO efficient record creation?

    public static List<HeartRate> latestForGraph(int number, long startTime, long endTime) {
        return dao().latestForGraph(Math.max(startTime, 0), endTime, number);
    }

    public static List<HeartRate> cleanup(int retention_days) {
        final int deleted = dao().cleanup(JoH.tsl() - (retention_days * 86400000L));
        UserError.Log.d(TAG, "HeartRate cleanup removed " + deleted + " record(s)");
        return new ArrayList<>();
    }

    // patches and saves
    public Long saveit() {
        if (_id != 0) {
            dao().update(this);
        } else {
            final long id = dao().insertIgnoringConflicts(this);
            if (id > 0) {
                _id = id;
            }
        }
        return _id;
    }

    // TODO cache gson statically
    public String toS() {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(this);
    }

    private static HeartRateDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).heartRateDao();
    }
}
