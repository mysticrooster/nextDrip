package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.Sensor;

import java.util.List;

@Dao
public interface SensorDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Sensor sensor);

    @Update
    void update(Sensor sensor);

    @Delete
    void delete(Sensor sensor);

    @Query("DELETE FROM Sensors")
    void deleteAll();

    @Query("SELECT * FROM Sensors WHERE started_at != 0 AND stopped_at != 0 ORDER BY _id DESC LIMIT 1")
    Sensor lastStopped();

    @Query("SELECT * FROM Sensors WHERE started_at != 0 ORDER BY _id DESC LIMIT 1")
    Sensor currentSensor();

    @Query("SELECT * FROM Sensors WHERE started_at = :started_at LIMIT 1")
    Sensor getByTimestamp(long started_at);

    @Query("SELECT * FROM Sensors WHERE uuid = :uuid LIMIT 1")
    Sensor getByUuid(String uuid);

    @Query("SELECT * FROM Sensors")
    List<Sensor> all();

    @Query("SELECT * FROM Sensors ORDER BY _id DESC")
    android.database.Cursor allCursor();
}
