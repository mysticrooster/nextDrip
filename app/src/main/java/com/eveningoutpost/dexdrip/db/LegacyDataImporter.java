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
 * One-time import of rows from the legacy ActiveAndroid database ("Application.db")
 * into the Room database ("xdrip-room.db").
 *
 * The Room migration uses a separate database file, so each migrated table starts
 * empty. To avoid losing user data, this importer copies the rows for migrated
 * tables the first time the app runs after a table has moved to Room.
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
 * The import is started on a background thread via {@link #importAll(Context)} (called
 * at startup) so it never blocks app startup. Any Room access obtained through
 * {@link AppDatabase#getInstance(Context)} first calls {@link #awaitImportComplete()},
 * so a migrated façade can never read (and then race) a table that is still being
 * copied.
 *
 * Each table is copied at most once, guarded by a {@link PersistentStore} flag, so
 * deleting rows later never re-imports them.
 */
public final class LegacyDataImporter {

    private static final String TAG = "LegacyDataImporter";

    public static final String LEGACY_DB_NAME = "Application.db";
    public static final String IMPORTED_FLAG_PREFIX = "room_legacy_import_done_";

    private static final String LEGACY_SCHEMA = "legacy";
    private static final int AWAIT_TIMEOUT_SECONDS = 30;

    /**
     * Table names of every entity registered in {@link AppDatabase}.
     * Keep this in sync when adding an entity; a unit test enforces it.
     */
    public static final Set<String> MIGRATED_TABLES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "CalibrationRequest",
                    "ActiveBgAlert")));

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
            return; // fresh install: nothing to preserve
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
        final String flag = IMPORTED_FLAG_PREFIX + table;
        if (PersistentStore.getBoolean(flag)) {
            return;
        }
        try {
            if (!tableExists(room, table) || !legacyTableExists(room, table)) {
                markDone(flag, table, "table missing");
                return;
            }
            if (rowCount(room, "main", table) > 0) {
                markDone(flag, table, "room table already populated");
                return;
            }
            if (rowCount(room, LEGACY_SCHEMA, table) == 0) {
                markDone(flag, table, "no legacy rows");
                return;
            }
            final Set<String> columns = sharedColumns(room, table);
            if (columns.isEmpty()) {
                Log.e(TAG, "No shared columns for " + table + " - skipping to avoid corrupting it");
                return;
            }
            final String columnList = joinQuoted(columns);
            room.beginTransaction();
            try {
                room.execSQL("INSERT OR IGNORE INTO main." + quote(table)
                        + " (" + columnList + ") SELECT " + columnList
                        + " FROM " + LEGACY_SCHEMA + "." + quote(table));
                room.setTransactionSuccessful();
            } finally {
                room.endTransaction();
            }
            markDone(flag, table, "imported " + rowCount(room, "main", table) + " row(s)");
        } catch (Exception e) {
            Log.e(TAG, "Failed importing " + table + ": " + e);
        }
    }

    private static void markDone(String flag, String table, String reason) {
        PersistentStore.setBoolean(flag, true);
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

    private static Set<String> columnNames(SQLiteDatabase db, String qualifiedTable) {
        final Set<String> names = new LinkedHashSet<>();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + qualifiedTable + " LIMIT 0", null)) {
            Collections.addAll(names, cursor.getColumnNames());
        }
        return names;
    }

    private static String joinQuoted(Set<String> columns) {
        final StringBuilder sb = new StringBuilder();
        for (final String column : columns) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(quote(column));
        }
        return sb.toString();
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
     * Resets the import state (started/running latch + per-table flags) and clears the
     * flags. Only for use from tests that need a clean slate.
     */
    static void resetForTesting() {
        synchronized (stateLock) {
            started = false;
            running = false;
            latch = new CountDownLatch(1);
        }
        for (final String table : MIGRATED_TABLES) {
            PersistentStore.removeItem(IMPORTED_FLAG_PREFIX + table);
        }
    }
}
