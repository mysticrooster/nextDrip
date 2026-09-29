package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.eveningoutpost.dexdrip.utilitymodels.SensorSendQueue;

import java.util.List;

@Dao
public interface SensorSendQueueDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(SensorSendQueue sensorSendQueue);

    @Query("SELECT * FROM SensorSendQueue WHERE success = 0 ORDER BY _id DESC LIMIT 1")
    SensorSendQueue nextUnsuccessful();

    @Query("SELECT * FROM SensorSendQueue WHERE success = 0 ORDER BY _id DESC")
    List<SensorSendQueue> queue();

    @Query("DELETE FROM SensorSendQueue")
    void deleteAll();
}
