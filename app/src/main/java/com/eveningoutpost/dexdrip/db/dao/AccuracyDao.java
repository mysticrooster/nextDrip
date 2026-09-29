package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.Accuracy;

import java.util.List;

@Dao
public interface AccuracyDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Accuracy accuracy);

    @Update
    void update(Accuracy accuracy);

    @Query("SELECT * FROM Accuracy WHERE timestamp <= :upper AND timestamp >= :lower AND plugin = :plugin "
            + "ORDER BY abs(timestamp - :timestamp) ASC LIMIT 1")
    Accuracy getForPreciseTimestamp(double lower, double upper, double timestamp, String plugin);

    @Query("SELECT * FROM Accuracy WHERE timestamp >= :start AND timestamp <= :end "
            + "ORDER BY timestamp DESC, _id ASC LIMIT :limit")
    List<Accuracy> latestForGraph(long start, long end, int limit);
}
