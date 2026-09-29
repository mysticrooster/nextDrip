package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.CalibrationSendQueueDao;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emma Black on 11/7/14.
 */
@Deprecated
@Entity(tableName = "CalibrationSendQueue",
        indices = {
                @Index("calibration"),
                @Index("success"),
                @Index("mongo_success")
        })
public class CalibrationSendQueue {
    private final static String TAG = CalibrationSendQueue.class.getSimpleName();

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @ColumnInfo(name = "calibration")
    public long calibration_id;

    @ColumnInfo(name = "success")
    public boolean success;

    @ColumnInfo(name = "mongo_success")
    public boolean mongo_success;

    /*
    public static List<CalibrationSendQueue> queue() {
        return new Select()
                .from(CalibrationSendQueue.class)
                .where("success = ?", false)
                .orderBy("_ID asc")
                .execute();
    }
    */
    public static List<CalibrationSendQueue> mongoQueue() {
        return dao().mongoQueue();
    }

    @Deprecated
    public static List<CalibrationSendQueue> cleanQueue() {
        dao().cleanQueue();
        return new ArrayList<>();
    }

    public static int countBySuccess(boolean success) {
        return dao().countBySuccess(success);
    }

    public static int countByMongoSuccess(boolean success) {
        return dao().countByMongoSuccess(success);
    }

    public static void addToQueue(Calibration calibration, Context context) {

        // TODO support for various insert/update/delete functions

        //  CalibrationSendQueue calibrationSendQueue = new CalibrationSendQueue();
        //  calibrationSendQueue.calibration = calibration;
        //  calibrationSendQueue.success = false;
        //  calibrationSendQueue.mongo_success = false;
        //  calibrationSendQueue.save();
        UploaderQueue.newEntry("create", calibration);
        Log.i(TAG, "calling SensorSendQueue.SendToFollower");
        SensorSendQueue.SendToFollower(Sensor.getByUuid(calibration.sensor_uuid));
    }

    public void markMongoSuccess() {
        mongo_success = true;
        dao().update(this);
    }

    private static CalibrationSendQueueDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).calibrationSendQueueDao();
    }
}
