package com.eveningoutpost.dexdrip.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.ActiveBluetoothDeviceDao;
import com.eveningoutpost.dexdrip.utilitymodels.Blukon;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by Emma Black on 11/3/14.
 */
@Entity(tableName = "ActiveBluetoothDevice")
public class ActiveBluetoothDevice {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "address")
    public String address;

    @ColumnInfo(name = "connected")
    public boolean connected;


    public static final Object table_lock = new Object();

    public static synchronized ActiveBluetoothDevice first() {
        return dao().first();
    }

    public static ActiveBluetoothDevice last() {
        return dao().last();
    }

    public static String btDeviceAddresses() {
        final ActiveBluetoothDevice btDevice = ActiveBluetoothDevice.first();
        if (btDevice == null || btDevice.address == null) {
            return "";
        }
        return btDevice.address;
    }

    public static synchronized  void forget() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if (activeBluetoothDevice != null) {
            dao().delete(activeBluetoothDevice);
        }
    }

    public static synchronized  void connected() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if(activeBluetoothDevice != null) {
            activeBluetoothDevice.connected = true;
            dao().update(activeBluetoothDevice);
        }
    }

    public static synchronized  void disconnected() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if(activeBluetoothDevice != null) {
            activeBluetoothDevice.connected = false;
            dao().update(activeBluetoothDevice);
        }
    }

    public static synchronized boolean is_connected() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        return (activeBluetoothDevice != null && activeBluetoothDevice.connected);
    }

    public static void deleteAll() {
        dao().deleteAll();
    }

    public static synchronized void setDevice(String name, String address) {
        ActiveBluetoothDevice btDevice;
        synchronized (ActiveBluetoothDevice.table_lock) {
             btDevice = dao().last();
        }
        Pref.setString("last_connected_device_address", address);
        Blukon.clearPin();
        if (btDevice == null) {
            ActiveBluetoothDevice newBtDevice = new ActiveBluetoothDevice();
            newBtDevice.name = name;
            newBtDevice.address = address;
            newBtDevice.save();
        } else {
            btDevice.name = name;
            btDevice.address = address;
            btDevice.save();
        }
    }

    /**
     * Insert-or-update, mirroring the ActiveAndroid Model.save() used by callers
     * (e.g. BluetoothScan) before the Room migration.
     */
    public Long save() {
        if (_id != 0) {
            dao().update(this);
        } else {
            final long id = dao().insert(this);
            if (id > 0) {
                _id = id;
            }
        }
        return _id;
    }

    private static ActiveBluetoothDeviceDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).activeBluetoothDeviceDao();
    }
}
