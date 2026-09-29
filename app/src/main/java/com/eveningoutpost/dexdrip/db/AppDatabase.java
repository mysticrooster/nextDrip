package com.eveningoutpost.dexdrip.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.eveningoutpost.dexdrip.db.dao.ActiveBgAlertDao;
import com.eveningoutpost.dexdrip.db.dao.CalibrationRequestDao;
import com.eveningoutpost.dexdrip.db.dao.MetaDao;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.CalibrationRequest;

@Database(entities = {CalibrationRequest.class, ActiveBgAlert.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "xdrip-room.db";

    private static volatile AppDatabase INSTANCE;

    public abstract CalibrationRequestDao calibrationRequestDao();

    public abstract ActiveBgAlertDao activeBgAlertDao();

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
