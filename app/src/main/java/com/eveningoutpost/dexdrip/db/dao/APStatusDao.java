package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.APStatus;

import java.util.List;

@Dao
public interface APStatusDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(APStatus apStatus);

    @Update
    void update(APStatus apStatus);

    @Query("SELECT * FROM APStatus ORDER BY timestamp DESC LIMIT 1")
    APStatus last();

    @Query("SELECT * FROM APStatus WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC LIMIT :limit")
    List<APStatus> latestForGraph(long start, long end, int limit);

    @Query("DELETE FROM APStatus WHERE timestamp < :cutoff")
    int cleanup(long cutoff);
}
