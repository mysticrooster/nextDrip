package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;

@Dao
public interface ActiveBluetoothDeviceDao {

    @Query("SELECT * FROM ActiveBluetoothDevice ORDER BY _id ASC LIMIT 1")
    ActiveBluetoothDevice first();

    @Query("SELECT COUNT(*) FROM ActiveBluetoothDevice")
    int count();

    @Query("SELECT * FROM ActiveBluetoothDevice ORDER BY _id DESC LIMIT 1")
    ActiveBluetoothDevice last();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ActiveBluetoothDevice device);

    @Update
    void update(ActiveBluetoothDevice device);

    @Delete
    void delete(ActiveBluetoothDevice device);

    @Query("DELETE FROM ActiveBluetoothDevice")
    void deleteAll();
}
