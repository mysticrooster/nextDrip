package com.eveningoutpost.dexdrip.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.CalibrationRequestDao;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by Emma Black on 12/9/14.
 */

@Entity(tableName = "CalibrationRequest")
public class CalibrationRequest {
    private static final int max = 250;
    private static final int min = 70;
    private static final String TAG = CalibrationRequest.class.getSimpleName();

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @ColumnInfo(name = "requestIfAbove")
    public double requestIfAbove;

    @ColumnInfo(name = "requestIfBelow")
    public double requestIfBelow;

    public static void createRange(double low, double high) {
        CalibrationRequest calibrationRequest = new CalibrationRequest();
        calibrationRequest.requestIfAbove = low;
        calibrationRequest.requestIfBelow = high;
        dao().insert(calibrationRequest);
    }
    static void createOffset(double center, double distance) {
        CalibrationRequest calibrationRequest = new CalibrationRequest();
        calibrationRequest.requestIfAbove = center + distance;
        calibrationRequest.requestIfBelow = max;
        dao().insert(calibrationRequest);

        calibrationRequest = new CalibrationRequest();
        calibrationRequest.requestIfAbove = min;
        calibrationRequest.requestIfBelow = center - distance;
        dao().insert(calibrationRequest);
    }

    static void clearAll(){
        dao().deleteAll();
    }

    public static boolean shouldRequestCalibration(BgReading bgReading) {
        CalibrationRequest calibrationRequest = dao().findMatching(bgReading.calculated_value);
        return (calibrationRequest != null && isSlopeFlatEnough(bgReading, 1));
    }

    public static boolean isSlopeFlatEnough() {
        BgReading bgReading = BgReading.last(true);
        if (bgReading == null) return false;
        if (JoH.msSince(bgReading.timestamp) > Home.stale_data_millis()) {
            UserError.Log.d(TAG, "Slope cannot be flat enough as data is stale");
            return false;
        }
        // TODO check if stale, check previous slope also, check that reading parameters also
        return isSlopeFlatEnough(bgReading);
    }

    public static boolean isSlopeFlatEnough(BgReading bgReading) {
        return isSlopeFlatEnough(bgReading, 1);
    }

    public static boolean isSlopeFlatEnough(BgReading bgReading, double limit) {
        if (bgReading == null) return false;
        // TODO use BestGlucose
        return Math.abs(bgReading.calculated_value_slope * 60000) < limit;
    }

    private static CalibrationRequestDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).calibrationRequestDao();
    }
}
