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
 * A pump-reported predicted blood-glucose sample (iLet real-time stream), in mg/dL,
 * stamped in Unix millis with the pump clock offset already applied. This is the pump's
 * own forecast, distinct from xDrip's trend/IOB prediction.
 */
@Table(name = "IletPredictedBg", id = BaseColumns._ID)
public class IletPredictedBg extends Model {

    private static boolean patched = false;

    @Column(name = "timestamp", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long timestamp;

    @Column(name = "predicted")
    public double predicted;

    /** Store one sample; ignores duplicate timestamps and out-of-range values. */
    public static synchronized void store(long timestamp, double predictedMgdl) {
        if (predictedMgdl <= 0 || predictedMgdl > 600) return;
        final IletPredictedBg reading = new IletPredictedBg();
        reading.timestamp = timestamp;
        reading.predicted = predictedMgdl;
        fixUpTable();
        reading.save();
    }

    public static List<IletPredictedBg> latestForGraph(int number, long startTime, long endTime) {
        try {
            return new Select()
                    .from(IletPredictedBg.class)
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
                    .from(IletPredictedBg.class)
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
                "CREATE TABLE IletPredictedBg (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE IletPredictedBg ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE IletPredictedBg ADD COLUMN predicted REAL;",
                "CREATE UNIQUE INDEX index_IletPredictedBg_timestamp on IletPredictedBg(timestamp);"};
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
