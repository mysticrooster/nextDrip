package com.eveningoutpost.dexdrip.db;

import androidx.room.migration.Migration;

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

    public static final Migration[] ALL = {
            // Add migrations here as the schema evolves.
    };

    private Migrations() {
    }
}
