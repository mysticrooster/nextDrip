package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

public class ActiveBluetoothDeviceTest extends RobolectricTestWithConfig {

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

    private static ActiveBluetoothDevice create(String name, String address) {
        final ActiveBluetoothDevice device = new ActiveBluetoothDevice();
        device.name = name;
        device.address = address;
        device.save();
        return device;
    }

    @Test
    public void saveThenFirstAndLast() {
        create("dev", "AA:BB");

        assertWithMessage("first address").that(ActiveBluetoothDevice.first().address).isEqualTo("AA:BB");
        assertWithMessage("last address").that(ActiveBluetoothDevice.last().address).isEqualTo("AA:BB");
        assertWithMessage("address helper").that(ActiveBluetoothDevice.btDeviceAddresses()).isEqualTo("AA:BB");
    }

    @Test
    public void connectedReflectsState() {
        create("dev", "AA:BB");

        ActiveBluetoothDevice.connected();
        assertWithMessage("connected").that(ActiveBluetoothDevice.is_connected()).isTrue();

        ActiveBluetoothDevice.disconnected();
        assertWithMessage("disconnected").that(ActiveBluetoothDevice.is_connected()).isFalse();
    }

    @Test
    public void forgetRemovesDevice() {
        create("dev", "AA:BB");
        ActiveBluetoothDevice.forget();

        assertWithMessage("removed").that(ActiveBluetoothDevice.first()).isNull();
    }

    @Test
    public void setDeviceCreatesThenUpdatesSingleRow() {
        ActiveBluetoothDevice.setDevice("dev1", "AA:BB");
        assertWithMessage("created").that(ActiveBluetoothDevice.first().name).isEqualTo("dev1");

        ActiveBluetoothDevice.setDevice("dev2", "CC:DD");
        assertWithMessage("still one row").that(database.activeBluetoothDeviceDao().count()).isEqualTo(1);
        assertWithMessage("updated in place").that(ActiveBluetoothDevice.first().name).isEqualTo("dev2");
    }
}
