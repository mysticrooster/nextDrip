package com.eveningoutpost.dexdrip.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.eveningoutpost.dexdrip.db.dao.ActiveBgAlertDao;
import com.eveningoutpost.dexdrip.db.dao.ActiveBluetoothDeviceDao;
import com.eveningoutpost.dexdrip.db.dao.AlertTypeDao;
import com.eveningoutpost.dexdrip.db.dao.APStatusDao;
import com.eveningoutpost.dexdrip.db.dao.AccuracyDao;
import com.eveningoutpost.dexdrip.db.dao.CalibrationRequestDao;
import com.eveningoutpost.dexdrip.db.dao.HeartRateDao;
import com.eveningoutpost.dexdrip.db.dao.Libre2RawValueDao;
import com.eveningoutpost.dexdrip.db.dao.Libre2SensorDao;
import com.eveningoutpost.dexdrip.db.dao.MetaDao;
import com.eveningoutpost.dexdrip.db.dao.PenDataDao;
import com.eveningoutpost.dexdrip.db.dao.PredictionDao;
import com.eveningoutpost.dexdrip.db.dao.ReminderDao;
import com.eveningoutpost.dexdrip.db.dao.StepCounterDao;
import com.eveningoutpost.dexdrip.db.dao.TransmitterDataDao;
import com.eveningoutpost.dexdrip.db.dao.UserNotificationDao;
import com.eveningoutpost.dexdrip.models.APStatus;
import com.eveningoutpost.dexdrip.models.Accuracy;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.CalibrationRequest;
import com.eveningoutpost.dexdrip.models.HeartRate;
import com.eveningoutpost.dexdrip.models.Libre2RawValue;
import com.eveningoutpost.dexdrip.models.Libre2Sensor;
import com.eveningoutpost.dexdrip.models.LibreData;
import com.eveningoutpost.dexdrip.models.PenData;
import com.eveningoutpost.dexdrip.models.Prediction;
import com.eveningoutpost.dexdrip.models.Reminder;
import com.eveningoutpost.dexdrip.models.StepCounter;
import com.eveningoutpost.dexdrip.models.TransmitterData;
import com.eveningoutpost.dexdrip.models.UserNotification;
import com.eveningoutpost.dexdrip.sharemodels.models.ShareGlucose;

@Database(entities = {CalibrationRequest.class, ActiveBgAlert.class, PenData.class, AlertType.class, HeartRate.class, StepCounter.class, TransmitterData.class, ActiveBluetoothDevice.class, Reminder.class, ShareGlucose.class, UserNotification.class, Prediction.class, APStatus.class, Accuracy.class, LibreData.class, Libre2RawValue.class}, views = {Libre2Sensor.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "xdrip-room.db";

    private static volatile AppDatabase INSTANCE;

    public abstract CalibrationRequestDao calibrationRequestDao();

    public abstract ActiveBgAlertDao activeBgAlertDao();

    public abstract PenDataDao penDataDao();

    public abstract AlertTypeDao alertTypeDao();

    public abstract HeartRateDao heartRateDao();

    public abstract StepCounterDao stepCounterDao();

    public abstract TransmitterDataDao transmitterDataDao();

    public abstract ActiveBluetoothDeviceDao activeBluetoothDeviceDao();

    public abstract ReminderDao reminderDao();

    public abstract UserNotificationDao userNotificationDao();

    public abstract PredictionDao predictionDao();

    public abstract APStatusDao apStatusDao();

    public abstract AccuracyDao accuracyDao();

    public abstract Libre2RawValueDao libre2RawValueDao();

    public abstract Libre2SensorDao libre2SensorDao();

    public abstract MetaDao metaDao();

    public static AppDatabase getInstance(Context context) {
        // Wait for the one-time legacy import so a migrated façade can never read
        // (and then race) a table that is still being copied from ActiveAndroid.
        LegacyDataImporter.awaitImportComplete();
        return buildInstance(context);
    }

    /**
     * Builds/returns the singleton without waiting for the legacy import.
     * Used by {@link LegacyDataImporter} itself (which must not wait on its own import).
     */
    static AppDatabase buildInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void setInstanceForTesting(AppDatabase database) {
        INSTANCE = database;
    }

    public static synchronized void resetForTesting() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
