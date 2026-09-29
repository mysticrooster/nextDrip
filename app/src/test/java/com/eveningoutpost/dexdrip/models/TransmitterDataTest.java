package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class TransmitterDataTest extends RobolectricTestWithConfig {

    private AppDatabase database;

    @Before
    public void setUpDatabase() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        AppDatabase.setInstanceForTesting(database);
    }

    @After
    public void tearDownDatabase() {
        database.close();
        AppDatabase.setInstanceForTesting(null);
    }

    @Test
    public void createStoresAndLookupsWork() {
        final long now = new Date().getTime();
        final TransmitterData td = TransmitterData.create(123, 45, 88, now);

        assertThat(td).isNotNull();
        assertThat(TransmitterData.last().raw_data).isEqualTo(123);
        assertThat(TransmitterData.byid(td._id).uuid).isEqualTo(td.uuid);
        assertThat(TransmitterData.findByUuid(td.uuid)).isNotNull();
    }

    @Test
    public void createRejectsDuplicateWithinTwoMinutes() {
        final long now = new Date().getTime();

        assertWithMessage("first accepted").that(TransmitterData.create(123, 45, 88, now)).isNotNull();
        assertWithMessage("duplicate rejected").that(TransmitterData.create(123, 45, 88, now)).isNull();
    }

    @Test
    public void lastNReturnsMostRecentFirst() {
        final long now = new Date().getTime();
        TransmitterData.create(1, 1, 80, now - 10_000);
        TransmitterData.create(2, 2, 80, now - 5_000);

        final List<TransmitterData> list = TransmitterData.last(2);
        assertWithMessage("two records").that(list).hasSize(2);
        assertWithMessage("most recent first").that(list.get(0).raw_data).isEqualTo(2);
    }

    @Test
    public void lastByTimestampOrdersByTimestamp() {
        final long now = new Date().getTime();
        TransmitterData.create(1, 1, 80, now - 10_000);
        TransmitterData.create(2, 2, 80, now - 5_000);

        assertThat(TransmitterData.lastByTimestamp().raw_data).isEqualTo(2);
    }

    @Test
    public void updateTransmitterBatteryFromSyncStoresBattery() {
        TransmitterData.updateTransmitterBatteryFromSync(77);

        assertThat(TransmitterData.last().sensor_battery_level).isEqualTo(77);
    }
}
