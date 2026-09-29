package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.Prediction;

import java.util.List;

@Dao
public interface PredictionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Prediction prediction);

    @Update
    void update(Prediction prediction);

    @Query("SELECT * FROM Prediction ORDER BY timestamp DESC LIMIT 1")
    Prediction last();

    @Query("SELECT * FROM Prediction WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC LIMIT :limit")
    List<Prediction> latestForGraph(long start, long end, int limit);

    @Query("DELETE FROM Prediction WHERE timestamp < :cutoff")
    int cleanup(long cutoff);
}
