package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * A pump-reported IOB / reservoir / battery sample (iLet real-time stream), stamped
 * in Unix millis. Distinct from xDrip's treatment-derived IOB.
 */
@Table(name = "PumpIobReading", id = BaseColumns._ID)
public class PumpIobReading extends Model {

    private static boolean patched = false;

    @Column(name = "timestamp", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long timestamp;

    @Column(name = "iob")
    public double iob;

    @Column(name = "reservoir")
    public double reservoir;

    @Column(name = "battery")
    public double battery;

    public static synchronized void store(long timestamp, double iob, double reservoir, double battery) {
        final PumpIobReading reading = new PumpIobReading();
        reading.timestamp = timestamp;
        reading.iob = iob;
        reading.reservoir = reservoir;
        reading.battery = battery;
        fixUpTable();
        reading.save();
    }

    public static List<PumpIobReading> latestForGraph(int number, long startTime, long endTime) {
        try {
            return new Select()
                    .from(PumpIobReading.class)
                    .where("timestamp >= " + Math.max(startTime, 0))
                    .where("timestamp <= " + endTime)
                    .orderBy("timestamp asc")
                    .limit(number)
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return new ArrayList<>();
        }
    }

    public static void cleanup(int retention_days) {
        try {
            new Delete()
                    .from(PumpIobReading.class)
                    .where("timestamp < ?", JoH.tsl() - (retention_days * Constants.DAY_IN_MS))
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
        }
    }

    // create the table ourselves in case the schema upgrade did not
    private static void fixUpTable() {
        if (patched) return;
        final String[] patchup = {
                "CREATE TABLE PumpIobReading (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE PumpIobReading ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE PumpIobReading ADD COLUMN iob REAL;",
                "ALTER TABLE PumpIobReading ADD COLUMN reservoir REAL;",
                "ALTER TABLE PumpIobReading ADD COLUMN battery REAL;",
                "CREATE UNIQUE INDEX index_PumpIobReading_timestamp on PumpIobReading(timestamp);"};
        for (String patch : patchup) {
            try {
                SQLiteUtils.execSql(patch);
            } catch (Exception e) {
                // expected when the table/column already exists
            }
        }
        patched = true;
    }
}
