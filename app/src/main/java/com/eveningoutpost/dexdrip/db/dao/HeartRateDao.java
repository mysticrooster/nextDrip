package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.HeartRate;

import java.util.List;

@Dao
public interface HeartRateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnoringConflicts(HeartRate heartRate);

    @Update
    void update(HeartRate heartRate);

    @Query("SELECT * FROM HeartRate WHERE timestamp >= :since ORDER BY timestamp DESC LIMIT 1")
    HeartRate last(long since);

    @Query("SELECT * FROM HeartRate WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC LIMIT :limit")
    List<HeartRate> latestForGraph(long start, long end, int limit);

    @Query("DELETE FROM HeartRate WHERE timestamp < :cutoff")
    int cleanup(long cutoff);
}
