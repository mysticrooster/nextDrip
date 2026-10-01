package com.eveningoutpost.dexdrip.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.PumpIobReadingDao;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.List;

/**
 * A pump-reported IOB / reservoir / battery sample, timestamped on the pump
 * clock converted to Unix millis. Distinct from xDrip's treatment-derived IOB:
 * this is the iLet's own estimate from its real-time stream.
 *
 * @author xDrip iLet integration
 */
@Entity(tableName = "PumpIobReading",
        indices = {
                @Index(value = "timestamp", unique = true)
        })
public class PumpIobReading {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "iob")
    public double iob;

    @ColumnInfo(name = "reservoir")
    public double reservoir;

    @ColumnInfo(name = "battery")
    public double battery;

    public PumpIobReading() {
    }

    public PumpIobReading(long timestamp, double iob, double reservoir, double battery) {
        this.timestamp = timestamp;
        this.iob = iob;
        this.reservoir = reservoir;
        this.battery = battery;
    }

    public static synchronized void store(long timestamp, double iob, double reservoir, double battery) {
        final PumpIobReading reading = new PumpIobReading(timestamp, iob, reservoir, battery);
        AppDatabase.getInstance(xdrip.getAppContext()).pumpIobReadingDao().insert(reading);
    }

    public static PumpIobReading last() {
        return AppDatabase.getInstance(xdrip.getAppContext()).pumpIobReadingDao().last();
    }

    public static List<PumpIobReading> latestForGraph(long start, long end, int limit) {
        return AppDatabase.getInstance(xdrip.getAppContext()).pumpIobReadingDao().latestForGraph(start, end, limit);
    }

    public static PumpIobReadingDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).pumpIobReadingDao();
    }

    public static void cleanup() {
        final long cutoff = System.currentTimeMillis() - (Constants.DAY_IN_MS * 45);
        AppDatabase.getInstance(xdrip.getAppContext()).pumpIobReadingDao().cleanup(cutoff);
    }
}
