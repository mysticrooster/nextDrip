package com.eveningoutpost.dexdrip.db;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * One-time import of rows from the legacy ActiveAndroid database ("DexDrip.db")
 * into the Room database ("xdrip-room.db").
 *
 * The Room migration uses a separate database file, so each migrated table starts
 * empty. To avoid losing user data, this importer copies the rows for migrated
 * tables out of the legacy database.
 *
 * The copy runs entirely inside SQLite: it {@code ATTACH}es the legacy database and
 * issues a single {@code INSERT ... SELECT} per table over the intersection of the
 * two tables' columns. That makes it:
 * <ul>
 *   <li>immune to column ordering differences and to legacy-only/room-only columns;</li>
 *   <li>atomic per table (a single statement), so a failure cannot half-copy a table;</li>
 *   <li>fast (no per-row Java).</li>
 * </ul>
 *
 * Two details make it robust enough to be re-run safely:
 * <ul>
 *   <li>Room makes primitive columns {@code NOT NULL} while the legacy schema is
 *       nullable (e.g. columns added via {@code ALTER TABLE}). A plain
 *       {@code INSERT OR IGNORE ... SELECT} would silently drop those rows, so any
 *       shared column that is {@code NOT NULL} in Room is copied through
 *       {@code COALESCE(col, 0)}.</li>
 *   <li>Rows are only copied when their {@code _id} is not already present, so a
 *       re-run backfills missing rows without duplicating tables that have no unique
 *       constraint.</li>
 * </ul>
 *
 * Each table tracks the {@link #CURRENT_IMPORT_GENERATION} it was imported at
 * (per-table {@link PersistentStore} keys). Bumping {@code CURRENT_IMPORT_GENERATION}
 * forces every table to be re-imported (idempotently) on the next launch, which is how
 * a fix to the copy logic can recover rows a previous version dropped. Adding a new
 * table to {@link #MIGRATED_TABLES} also imports it automatically (its stored
 * generation starts at 0).
 *
 * The import is started on a background thread via {@link #importAll(Context)} (called
 * at startup) so it never blocks app startup. Any Room access obtained through
 * {@link AppDatabase#getInstance(Context)} first calls {@link #awaitImportComplete()},
 * so a migrated façade can never read (and then race) a table that is still being
 * copied.
 */
public final class LegacyDataImporter {

    private static final String TAG = "LegacyDataImporter";

    /**
     * The legacy ActiveAndroid database file name. This must match {@code AA_DB_NAME} in the
     * manifest (ActiveAndroid does not use its "Application.db" default here).
     */
    public static final String LEGACY_DB_NAME = "DexDrip.db";
    public static final String GENERATION_KEY_PREFIX = "room_legacy_import_gen_";

    /**
     * Bump this whenever the copy logic changes in a way that should re-run the import
     * (idempotently) to recover previously dropped rows.
     */
    public static final long CURRENT_IMPORT_GENERATION = 2;

    private static final String LEGACY_SCHEMA = "legacy";
    private static final int AWAIT_TIMEOUT_SECONDS = 30;

    /**
     * Table names of every entity registered in {@link AppDatabase}.
     * Keep this in sync when adding an entity; a unit test enforces it.
     */
    public static final Set<String> MIGRATED_TABLES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "CalibrationRequest",
                    "ActiveBgAlert",
                    "PenData",
                    "AlertType",
                    "HeartRate",
                    "PebbleMovement",
                    "TransmitterData",
                    "ActiveBluetoothDevice",
                    "Reminder",
                    "ShareGlucose",
                    "Notifications",
                    "Prediction",
                    "APStatus",
                    "Accuracy",
                    "LibreData",
                    "Libre2RawValue2",
                    "BloodTest",
                    "Treatments",
                    "LibreBlock",
                    "DesertSync",
                    "Sensors",
                    "Calibration",
                    "BgReadings",
                    "SensorSendQueue",
                    "CalibrationSendQueue",
                    "BgSendQueue",
                    "UploaderQueue",
                    "UserErrors",
                    "PumpIobReading")));

    private static final Object stateLock = new Object();
    private static boolean started;
    private static boolean running;
    private static CountDownLatch latch = new CountDownLatch(1);

    private LegacyDataImporter() {
    }

    /**
     * Starts the one-time import on a background thread. Safe to call more than once;
     * only the first call does anything.
     */
    public static void importAll(Context context) {
        if (!markStarted()) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        final Thread thread = new Thread(() -> {
            try {
                runImport(appContext);
            } catch (Throwable t) {
                Log.e(TAG, "Legacy import failed", t);
            } finally {
                finish();
            }
        }, "legacy-data-import");
        thread.start();
    }

    /**
     * Blocks until a running import has finished. No-op if the import was never started
     * or has already completed. Always returns (bounded by {@value #AWAIT_TIMEOUT_SECONDS}
     * seconds) so a stuck import can never hang the app forever.
     */
    public static void awaitImportComplete() {
        final CountDownLatch current = latchIfRunning();
        if (current == null) {
            return;
        }
        try {
            if (!current.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                Log.w(TAG, "Timed out after " + AWAIT_TIMEOUT_SECONDS + "s waiting for legacy import");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean markStarted() {
        synchronized (stateLock) {
            if (started) {
                return false;
            }
            started = true;
            running = true;
            latch = new CountDownLatch(1);
            return true;
        }
    }

    private static void finish() {
        synchronized (stateLock) {
            running = false;
            latch.countDown();
        }
    }

    private static CountDownLatch latchIfRunning() {
        synchronized (stateLock) {
            return running ? latch : null;
        }
    }

    private static void runImport(Context context) {
        final File legacyFile = context.getDatabasePath(LEGACY_DB_NAME);
        if (legacyFile == null || !legacyFile.exists()) {
            // Fresh install (or the legacy DB is already gone): nothing to preserve.
            markAllTablesImported();
            return;
        }

        // Ensure the Room schema exists on disk before we open the file directly.
        // Building the Room database does not open it, so run a trivial query to
        // force Room to create the file and schema. (Use the non-gating accessor:
        // we are the import, so we must not wait on ourselves.)
        final AppDatabase database = AppDatabase.buildInstance(context);
        try (Cursor ignored = database.query("SELECT 1", null)) {
            // opening the database is the point of this query
        }
        final File roomFile = context.getDatabasePath(AppDatabase.DATABASE_NAME);

        SQLiteDatabase room = null;
        try {
            room = SQLiteDatabase.openDatabase(roomFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
            room.execSQL("ATTACH DATABASE ? AS " + LEGACY_SCHEMA, new Object[]{legacyFile.getAbsolutePath()});
            try {
                for (final String table : MIGRATED_TABLES) {
                    importTable(room, table);
                }
            } finally {
                room.execSQL("DETACH DATABASE " + LEGACY_SCHEMA);
            }
        } catch (Exception e) {
            Log.e(TAG, "Legacy import failed: " + e);
        } finally {
            if (room != null) {
                room.close();
            }
        }
    }

    private static void importTable(SQLiteDatabase room, String table) {
        final String generationKey = GENERATION_KEY_PREFIX + table;
        if (PersistentStore.getLong(generationKey) >= CURRENT_IMPORT_GENERATION) {
            return; // already imported at the current generation
        }
        try {
            if (!tableExists(room, table) || !legacyTableExists(room, table)) {
                markDone(generationKey, table, "table missing");
                return;
            }
            if (rowCount(room, LEGACY_SCHEMA, table) == 0) {
                markDone(generationKey, table, "no legacy rows");
                return;
            }
            final Set<String> columns = sharedColumns(room, table);
            if (columns.isEmpty()) {
                Log.e(TAG, "No shared columns for " + table + " - skipping to avoid corrupting it");
                return;
            }
            final Set<String> notNull = notNullColumns(room, table);
            final StringBuilder columnList = new StringBuilder();
            final StringBuilder selectList = new StringBuilder();
            for (final String column : columns) {
                if (columnList.length() > 0) {
                    columnList.append(", ");
                    selectList.append(", ");
                }
                columnList.append(quote(column));
                if (notNull.contains(column) && !"_id".equalsIgnoreCase(column)) {
                    // Room is stricter (NOT NULL) than the legacy schema, which may hold NULL
                    // (e.g. columns added via ALTER TABLE). Coerce to 0 so the row is not dropped.
                    selectList.append("COALESCE(").append(quote(column)).append(", 0)");
                } else {
                    selectList.append(quote(column));
                }
            }
            final long before = rowCount(room, "main", table);
            room.beginTransaction();
            try {
                room.execSQL("INSERT OR IGNORE INTO main." + quote(table)
                        + " (" + columnList + ") SELECT " + selectList
                        + " FROM " + LEGACY_SCHEMA + "." + quote(table)
                        + " WHERE " + quote("_id") + " NOT IN (SELECT " + quote("_id")
                        + " FROM main." + quote(table) + ")");
                room.setTransactionSuccessful();
            } finally {
                room.endTransaction();
            }
            final long after = rowCount(room, "main", table);
            markDone(generationKey, table, "imported " + (after - before) + " row(s), now " + after);
        } catch (Exception e) {
            Log.e(TAG, "Failed importing " + table + ": " + e);
        }
    }

    private static void markAllTablesImported() {
        for (final String table : MIGRATED_TABLES) {
            PersistentStore.setLong(GENERATION_KEY_PREFIX + table, CURRENT_IMPORT_GENERATION);
        }
    }

    private static void markDone(String generationKey, String table, String reason) {
        PersistentStore.setLong(generationKey, CURRENT_IMPORT_GENERATION);
        Log.d(TAG, "Legacy import for " + table + " complete (" + reason + ")");
    }

    private static boolean tableExists(SQLiteDatabase db, String table) {
        return DatabaseUtils.longForQuery(db,
                "SELECT COUNT(*) FROM main.sqlite_master WHERE type='table' AND name=?",
                new String[]{table}) > 0;
    }

    private static boolean legacyTableExists(SQLiteDatabase db, String table) {
        return DatabaseUtils.longForQuery(db,
                "SELECT COUNT(*) FROM " + LEGACY_SCHEMA + ".sqlite_master WHERE type='table' AND name=?",
                new String[]{table}) > 0;
    }

    private static long rowCount(SQLiteDatabase db, String schema, String table) {
        return DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + schema + "." + quote(table), null);
    }

    private static Set<String> sharedColumns(SQLiteDatabase db, String table) {
        final Set<String> columns = columnNames(db, "main." + quote(table));
        columns.retainAll(columnNames(db, LEGACY_SCHEMA + "." + quote(table)));
        return columns;
    }

    private static Set<String> notNullColumns(SQLiteDatabase db, String table) {
        final Set<String> names = new LinkedHashSet<>();
        try (Cursor cursor = db.rawQuery("PRAGMA main.table_info(" + quote(table) + ")", null)) {
            final int nameIndex = cursor.getColumnIndex("name");
            final int notNullIndex = cursor.getColumnIndex("notnull");
            while (cursor.moveToNext()) {
                if (cursor.getInt(notNullIndex) != 0) {
                    names.add(cursor.getString(nameIndex));
                }
            }
        }
        return names;
    }

    private static Set<String> columnNames(SQLiteDatabase db, String qualifiedTable) {
        final Set<String> names = new LinkedHashSet<>();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + qualifiedTable + " LIMIT 0", null)) {
            Collections.addAll(names, cursor.getColumnNames());
        }
        return names;
    }

    private static String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * Runs the copy synchronously on the calling thread. Only for tests that want a
     * deterministic (non-threaded) import.
     */
    static void importSynchronouslyForTesting(Context context) {
        runImport(context.getApplicationContext());
    }

    /**
     * Clears the per-table import generations so the next {@link #importAll(Context)} re-copies
     * every migrated table. Called when Room recreates its database (destructive migration), and
     * by tests.
     */
    public static void clearImportState() {
        for (final String table : MIGRATED_TABLES) {
            PersistentStore.removeItem(GENERATION_KEY_PREFIX + table);
        }
    }

    /**
     * Resets the import thread state so tests can start a clean import. Only for use from tests.
     */
    static void resetForTesting() {
        synchronized (stateLock) {
            started = false;
            running = false;
            latch = new CountDownLatch(1);
        }
        clearImportState();
    }
}
