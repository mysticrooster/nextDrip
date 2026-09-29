package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.eveningoutpost.dexdrip.models.Libre2Sensor;

import java.util.List;

@Dao
public interface Libre2SensorDao {

    @Query("SELECT * FROM Libre2Sensors")
    List<Libre2Sensor> all();
}
