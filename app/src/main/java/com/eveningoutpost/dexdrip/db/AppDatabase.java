package com.eveningoutpost.dexdrip.db;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;

import com.eveningoutpost.dexdrip.db.dao.ActiveBgAlertDao;
import com.eveningoutpost.dexdrip.db.dao.ActiveBluetoothDeviceDao;
import com.eveningoutpost.dexdrip.db.dao.AlertTypeDao;
import com.eveningoutpost.dexdrip.db.dao.APStatusDao;
import com.eveningoutpost.dexdrip.db.dao.AccuracyDao;
import com.eveningoutpost.dexdrip.db.dao.BgReadingDao;
import com.eveningoutpost.dexdrip.db.dao.BgSendQueueDao;
import com.eveningoutpost.dexdrip.db.dao.CalibrationDao;
import com.eveningoutpost.dexdrip.db.dao.CalibrationSendQueueDao;
import com.eveningoutpost.dexdrip.db.dao.BloodTestDao;
import com.eveningoutpost.dexdrip.db.dao.CalibrationRequestDao;
import com.eveningoutpost.dexdrip.db.dao.DesertSyncDao;
import com.eveningoutpost.dexdrip.db.dao.HeartRateDao;
import com.eveningoutpost.dexdrip.db.dao.Libre2RawValueDao;
import com.eveningoutpost.dexdrip.db.dao.Libre2SensorDao;
import com.eveningoutpost.dexdrip.db.dao.LibreBlockDao;
import com.eveningoutpost.dexdrip.db.dao.MetaDao;
import com.eveningoutpost.dexdrip.db.dao.PenDataDao;
import com.eveningoutpost.dexdrip.db.dao.PredictionDao;
import com.eveningoutpost.dexdrip.db.dao.SensorDao;
import com.eveningoutpost.dexdrip.db.dao.SensorSendQueueDao;
import com.eveningoutpost.dexdrip.db.dao.ReminderDao;
import com.eveningoutpost.dexdrip.db.dao.StepCounterDao;
import com.eveningoutpost.dexdrip.db.dao.TreatmentsDao;
import com.eveningoutpost.dexdrip.db.dao.TransmitterDataDao;
import com.eveningoutpost.dexdrip.db.dao.UserNotificationDao;
import com.eveningoutpost.dexdrip.models.APStatus;
import com.eveningoutpost.dexdrip.models.Accuracy;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.CalibrationRequest;
import com.eveningoutpost.dexdrip.models.DesertSync;
import com.eveningoutpost.dexdrip.models.HeartRate;
import com.eveningoutpost.dexdrip.models.Libre2RawValue;
import com.eveningoutpost.dexdrip.models.Libre2Sensor;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.LibreData;
import com.eveningoutpost.dexdrip.models.PenData;
import com.eveningoutpost.dexdrip.models.Prediction;
import com.eveningoutpost.dexdrip.models.Reminder;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.StepCounter;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.TransmitterData;
import com.eveningoutpost.dexdrip.models.UserNotification;
import com.eveningoutpost.dexdrip.sharemodels.models.ShareGlucose;
import com.eveningoutpost.dexdrip.utilitymodels.BgSendQueue;
import com.eveningoutpost.dexdrip.utilitymodels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.utilitymodels.SensorSendQueue;

@Database(entities = {CalibrationRequest.class, ActiveBgAlert.class, PenData.class, AlertType.class, HeartRate.class, StepCounter.class, TransmitterData.class, ActiveBluetoothDevice.class, Reminder.class, ShareGlucose.class, UserNotification.class, Prediction.class, APStatus.class, Accuracy.class, LibreData.class, Libre2RawValue.class, BloodTest.class, Treatments.class, LibreBlock.class, DesertSync.class, Sensor.class, Calibration.class, BgReading.class, SensorSendQueue.class, CalibrationSendQueue.class, BgSendQueue.class}, views = {Libre2Sensor.class}, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "xdrip-room.db";
    private static final String TAG = "AppDatabase";

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

    public abstract DesertSyncDao desertSyncDao();

    public abstract BloodTestDao bloodTestDao();

    public abstract TreatmentsDao treatmentsDao();

    public abstract Libre2RawValueDao libre2RawValueDao();

    public abstract Libre2SensorDao libre2SensorDao();

    public abstract LibreBlockDao libreBlockDao();

    public abstract SensorDao sensorDao();

    public abstract CalibrationDao calibrationDao();

    public abstract BgReadingDao bgReadingDao();

    public abstract SensorSendQueueDao sensorSendQueueDao();

    public abstract CalibrationSendQueueDao calibrationSendQueueDao();

    public abstract BgSendQueueDao bgSendQueueDao();

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
                    INSTANCE = open(context);
                }
            }
        }
        return INSTANCE;
    }

    private static AppDatabase open(Context context) {
        try {
            return openOnce(context);
        } catch (IllegalStateException e) {
            // Room refused to open because the schema identity hash changed. During the
            // ActiveAndroid transition the Room DB only mirrors the legacy data, so wipe and
            // rebuild it, then let the legacy importer refill it. This makes dev builds with a
            // changed entity set self-heal instead of crashing (see the version-bump rule too).
            Log.w(TAG, "Recreating Room database after schema mismatch: " + e.getMessage());
            deleteDatabaseFiles(context);
            LegacyDataImporter.clearImportFlags();
            return openOnce(context);
        }
    }

    private static AppDatabase openOnce(Context context) {
        final AppDatabase db = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME)
                .allowMainThreadQueries()
                // During the ActiveAndroid -> Room transition the Room DB is only ever a
                // copy of the legacy data, so it is safe to recreate it whenever the
                // schema changes. (Bump @Database version whenever an entity is added.)
                .fallbackToDestructiveMigration(true)
                .addCallback(new Callback() {
                    @Override
                    public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                        // Room recreated the DB; let the legacy importer refill the
                        // migrated tables from ActiveAndroid's Application.db again.
                        LegacyDataImporter.clearImportFlags();
                    }
                })
                .build();
        // Force the open so schema/identity errors surface here, where we can recover.
        try (Cursor ignored = db.query("SELECT 1", null)) {
            return db;
        } catch (RuntimeException e) {
            db.close();
            throw e;
        }
    }

    private static void deleteDatabaseFiles(Context context) {
        context.deleteDatabase(DATABASE_NAME);
        final String base = context.getDatabasePath(DATABASE_NAME).getPath();
        new File(base + "-wal").delete();
        new File(base + "-shm").delete();
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
