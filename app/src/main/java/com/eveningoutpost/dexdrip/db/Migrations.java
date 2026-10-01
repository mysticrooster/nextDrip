package com.eveningoutpost.dexdrip.db;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room schema migrations.
 *
 * <p>The Room schema is exported to {@code app/schemas} (via the {@code room.schemaLocation}
 * annotation-processor argument in {@code app/build.gradle}). Whenever an entity, column or
 * index changes, bump {@link AppDatabase}'s {@code version} <b>and</b> add a {@link Migration}
 * here. Room validates the migrated schema against the exported schema, so keeping the two in
 * sync means upgrades never need a destructive migration and user data is preserved.
 *
 * <p>Example:
 * <pre>{@code
 * static final Migration MIGRATION_9_10 = new Migration(9, 10) {
 *     @Override
 *     public void migrate(SupportSQLiteDatabase db) {
 *         db.execSQL("ALTER TABLE BgReadings ADD COLUMN new_column REAL NOT NULL DEFAULT 0");
 *     }
 * };
 * }</pre>
 */
public final class Migrations {

    /** Adds the PumpIobReading table for the iLet pump-IOB graph line. */
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `PumpIobReading` "
                    + "(`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`timestamp` INTEGER NOT NULL, "
                    + "`iob` REAL NOT NULL, "
                    + "`reservoir` REAL NOT NULL, "
                    + "`battery` REAL NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_PumpIobReading_timestamp` "
                    + "ON `PumpIobReading` (`timestamp`)");
        }
    };

    public static final Migration[] ALL = {
            MIGRATION_9_10,
    };

    private Migrations() {
    }
}
