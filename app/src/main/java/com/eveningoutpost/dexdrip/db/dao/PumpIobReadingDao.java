package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.eveningoutpost.dexdrip.models.PumpIobReading;

import java.util.List;

@Dao
public interface PumpIobReadingDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(PumpIobReading reading);

    @Query("SELECT * FROM PumpIobReading ORDER BY timestamp DESC LIMIT 1")
    PumpIobReading last();

    @Query("SELECT * FROM PumpIobReading WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC LIMIT :limit")
    List<PumpIobReading> latestForGraph(long start, long end, int limit);

    @Query("DELETE FROM PumpIobReading WHERE timestamp < :cutoff")
    int cleanup(long cutoff);
}
