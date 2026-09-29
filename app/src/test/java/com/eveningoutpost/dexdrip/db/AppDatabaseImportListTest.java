package com.eveningoutpost.dexdrip.db;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.HashSet;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Guards {@link LegacyDataImporter#MIGRATED_TABLES}: every entity registered in
 * {@link AppDatabase} must be listed there, otherwise its data would silently not be
 * preserved when it moves from ActiveAndroid to Room.
 */
public class AppDatabaseImportListTest extends RobolectricTestWithConfig {

    @Test
    public void everyEntityIsListedForLegacyImport() {
        final AppDatabase database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        try {
            assertWithMessage("AppDatabase entities and LegacyDataImporter.MIGRATED_TABLES must match")
                    .that(new HashSet<>(database.metaDao().tableNames()))
                    .isEqualTo(new HashSet<>(LegacyDataImporter.MIGRATED_TABLES));
        } finally {
            database.close();
        }
    }
}
