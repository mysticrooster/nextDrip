package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.StepCounter;

import java.util.List;

@Dao
public interface StepCounterDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnoringConflicts(StepCounter stepCounter);

    @Update
    void update(StepCounter stepCounter);

    @Query("SELECT * FROM PebbleMovement WHERE timestamp = :timestamp LIMIT 1")
    StepCounter getForTimestamp(long timestamp);

    @Query("SELECT * FROM PebbleMovement ORDER BY timestamp DESC LIMIT 1")
    StepCounter last();

    @Query("SELECT * FROM PebbleMovement WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC LIMIT :limit")
    List<StepCounter> latestForGraph(long start, long end, int limit);

    @Query("DELETE FROM PebbleMovement WHERE timestamp < :cutoff")
    int cleanup(long cutoff);

    @Query("SELECT sum(t.metric) FROM PebbleMovement t "
            + "INNER JOIN ("
            + "  SELECT metric, max(timestamp) AS MaxDate FROM PebbleMovement "
            + "  GROUP BY date(timestamp/1000,'unixepoch','localtime')"
            + ") tm ON t.metric = tm.metric AND t.timestamp = tm.MaxDate "
            + "WHERE t.timestamp >= :from AND t.timestamp <= :to")
    Integer totalStepsBetween(long from, long to);
}
