package com.eveningoutpost.dexdrip.models;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.Libre2RawValueDao;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Date;
import java.util.List;

@Entity(tableName = "Libre2RawValue2",
        indices = {
                @Index("serial"),
                @Index("ts")
        })
public class Libre2RawValue {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @ColumnInfo(name = "serial")
    public String serial;

    @ColumnInfo(name = "ts")
    public long timestamp;

    @ColumnInfo(name = "glucose")
    public double glucose;

    public static List<Libre2RawValue> weightedAverageInterval(long min) {
        double timestamp = (new Date().getTime()) - (60000 * min);
        return dao().weightedAverageInterval((long) timestamp);
    }

    public static List<Libre2RawValue> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<Libre2RawValue> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<Libre2RawValue> latestForGraph(int number, long startTime, long endTime) {
        return dao().latestForGraph(Math.max(startTime, 0), endTime, number);
    }

    public static List<Libre2RawValue> cleanup(final int retention_days) {
        dao().cleanup(JoH.tsl() - (retention_days * Constants.DAY_IN_MS));
        return new java.util.ArrayList<>();
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

    private static Libre2RawValueDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).libre2RawValueDao();
    }
}
