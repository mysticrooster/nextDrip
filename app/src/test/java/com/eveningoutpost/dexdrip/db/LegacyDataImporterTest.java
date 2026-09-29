package com.eveningoutpost.dexdrip.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.CalibrationRequest;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.SQLiteMode;

import java.io.File;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Verifies that existing ActiveAndroid data is copied into Room exactly once, without
 * overwriting data the Room side already holds.
 */
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class LegacyDataImporterTest extends RobolectricTestWithConfig {

    private static final String CALIBRATION_TABLE_SQL =
            "CREATE TABLE CalibrationRequest (_id INTEGER PRIMARY KEY AUTOINCREMENT, requestIfAbove REAL, requestIfBelow REAL)";
    private static final String ACTIVE_ALERT_TABLE_SQL =
            "CREATE TABLE ActiveBgAlert (_id INTEGER PRIMARY KEY AUTOINCREMENT, alert_uuid TEXT, is_snoozed INTEGER, "
                    + "last_alerted_at INTEGER, next_alert_at INTEGER, alert_started_at INTEGER)";

    private Context context;

    @Before
    public void setUpImporter() {
        context = RuntimeEnvironment.getApplication();
        reset();
    }

    @After
    public void tearDownImporter() {
        reset();
    }

    private void reset() {
        AppDatabase.resetForTesting();
        LegacyDataImporter.resetForTesting();
        context.deleteDatabase(LegacyDataImporter.LEGACY_DB_NAME);
        context.deleteDatabase(AppDatabase.DATABASE_NAME);
    }

    private SQLiteDatabase createLegacyDatabase() {
        final File file = context.getDatabasePath(LegacyDataImporter.LEGACY_DB_NAME);
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        return SQLiteDatabase.openOrCreateDatabase(file, null);
    }

    @Test
    public void importsLegacyRowsIntoRoom() {
        try (SQLiteDatabase legacy = createLegacyDatabase()) {
            legacy.execSQL(CALIBRATION_TABLE_SQL);
            legacy.execSQL("INSERT INTO CalibrationRequest (requestIfAbove, requestIfBelow) VALUES (140.0, 160.0)");
            legacy.execSQL(ACTIVE_ALERT_TABLE_SQL);
            legacy.execSQL("INSERT INTO ActiveBgAlert (alert_uuid, is_snoozed, last_alerted_at, next_alert_at, alert_started_at) "
                    + "VALUES ('legacy-uuid', 0, 0, 1000, 500)");
        }

        LegacyDataImporter.importSynchronouslyForTesting(context);

        final AppDatabase db = AppDatabase.getInstance(context);
        assertWithMessage("calibration request preserved")
                .that(db.calibrationRequestDao().getAll()).hasSize(1);
        assertWithMessage("calibration values preserved")
                .that(db.calibrationRequestDao().getAll().get(0).requestIfAbove).isEqualTo(140.0);
        assertWithMessage("active bg alert preserved")
                .that(db.activeBgAlertDao().getOnly().alert_uuid).isEqualTo("legacy-uuid");
    }

    @Test
    public void backgroundImportPreservesRowsAndIsAwaitable() {
        try (SQLiteDatabase legacy = createLegacyDatabase()) {
            legacy.execSQL(CALIBRATION_TABLE_SQL);
            legacy.execSQL("INSERT INTO CalibrationRequest (requestIfAbove, requestIfBelow) VALUES (140.0, 160.0)");
        }

        LegacyDataImporter.importAll(context);
        LegacyDataImporter.awaitImportComplete();

        assertWithMessage("row imported by the background import")
                .that(AppDatabase.getInstance(context).calibrationRequestDao().getAll()).hasSize(1);
    }

    @Test
    public void importIsIdempotent() {
        try (SQLiteDatabase legacy = createLegacyDatabase()) {
            legacy.execSQL(CALIBRATION_TABLE_SQL);
            legacy.execSQL("INSERT INTO CalibrationRequest (requestIfAbove, requestIfBelow) VALUES (140.0, 160.0)");
        }

        LegacyDataImporter.importSynchronouslyForTesting(context);
        LegacyDataImporter.importSynchronouslyForTesting(context);

        assertWithMessage("no duplicate rows after re-running")
                .that(AppDatabase.getInstance(context).calibrationRequestDao().getAll()).hasSize(1);
    }

    @Test
    public void doesNotOverwriteExistingRoomData() {
        final AppDatabase db = AppDatabase.getInstance(context);
        final CalibrationRequest existing = new CalibrationRequest();
        existing.requestIfAbove = 10.0;
        existing.requestIfBelow = 20.0;
        db.calibrationRequestDao().insert(existing);

        try (SQLiteDatabase legacy = createLegacyDatabase()) {
            legacy.execSQL(CALIBRATION_TABLE_SQL);
            legacy.execSQL("INSERT INTO CalibrationRequest (requestIfAbove, requestIfBelow) VALUES (140.0, 160.0)");
        }

        LegacyDataImporter.importSynchronouslyForTesting(context);

        assertWithMessage("existing Room data untouched")
                .that(db.calibrationRequestDao().getAll()).hasSize(1);
        assertWithMessage("legacy row not imported over existing data")
                .that(db.calibrationRequestDao().getAll().get(0).requestIfAbove).isEqualTo(10.0);
    }

    @Test
    public void toleratesMissingLegacyDatabase() {
        LegacyDataImporter.importSynchronouslyForTesting(context);

        assertWithMessage("Room still usable with no legacy database")
                .that(AppDatabase.getInstance(context).calibrationRequestDao().getAll()).isEmpty();
    }

    @Test
    public void ignoresLegacyOnlyColumns() {
        try (SQLiteDatabase legacy = createLegacyDatabase()) {
            legacy.execSQL("CREATE TABLE CalibrationRequest (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "requestIfAbove REAL, requestIfBelow REAL, legacy_only_column TEXT)");
            legacy.execSQL("INSERT INTO CalibrationRequest (requestIfAbove, requestIfBelow, legacy_only_column) "
                    + "VALUES (140.0, 160.0, 'ignored')");
        }

        LegacyDataImporter.importSynchronouslyForTesting(context);

        assertWithMessage("shared columns copied despite a legacy-only column")
                .that(AppDatabase.getInstance(context).calibrationRequestDao().getAll()).hasSize(1);
    }

    @Test
    public void destructiveMigrationClearsImportFlagSoDataIsReimported() {
        // Legacy data present and already imported once.
        try (SQLiteDatabase legacy = createLegacyDatabase()) {
            legacy.execSQL(CALIBRATION_TABLE_SQL);
            legacy.execSQL("INSERT INTO CalibrationRequest (requestIfAbove, requestIfBelow) VALUES (140.0, 160.0)");
        }
        LegacyDataImporter.importSynchronouslyForTesting(context);
        final String flag = LegacyDataImporter.IMPORTED_FLAG_PREFIX + "CalibrationRequest";
        assertWithMessage("flag set after import").that(PersistentStore.getBoolean(flag)).isTrue();

        // Simulate an older schema version so Room recreates the DB on the next open.
        final File roomFile = context.getDatabasePath(AppDatabase.DATABASE_NAME);
        try (SQLiteDatabase raw = SQLiteDatabase.openDatabase(roomFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE)) {
            raw.execSQL("PRAGMA user_version = 1");
        }
        AppDatabase.resetForTesting();

        // Re-open: destructive migration runs and clears the import flags.
        AppDatabase.getInstance(context).query("SELECT 1", null).close();

        assertWithMessage("flag cleared by destructive migration").that(PersistentStore.getBoolean(flag)).isFalse();
        assertWithMessage("room table was recreated empty")
                .that(AppDatabase.getInstance(context).calibrationRequestDao().getAll()).isEmpty();
    }

    @Test
    public void sameVersionSchemaMismatchSelfHeals() {
        // Create the Room DB, then corrupt the stored schema identity hash so Room refuses to open.
        AppDatabase.getInstance(context).query("SELECT 1", null).close();
        final File roomFile = context.getDatabasePath(AppDatabase.DATABASE_NAME);
        try (SQLiteDatabase raw = SQLiteDatabase.openDatabase(roomFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE)) {
            raw.execSQL("UPDATE room_master_table SET identity_hash = 'deadbeef'");
        }
        AppDatabase.resetForTesting();

        final String flag = LegacyDataImporter.IMPORTED_FLAG_PREFIX + "CalibrationRequest";
        PersistentStore.setBoolean(flag, true);

        // Re-open must self-heal (no crash) and clear the import flags.
        AppDatabase.getInstance(context).query("SELECT 1", null).close();

        assertWithMessage("flag cleared after self-heal").that(PersistentStore.getBoolean(flag)).isFalse();
    }
}
