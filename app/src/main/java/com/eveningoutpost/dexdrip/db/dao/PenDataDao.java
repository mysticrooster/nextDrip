package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.PenData;

import java.util.List;

@Dao
public interface PenDataDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnoringConflicts(PenData penData);

    @Update
    void update(PenData penData);

    @Query("SELECT COUNT(*) FROM PenData")
    int count();

    @Query("SELECT * FROM PenData WHERE pen_mac = :mac ORDER BY idx DESC LIMIT 1")
    PenData highestIndex(String mac);

    @Query("SELECT * FROM PenData WHERE pen_mac = :mac AND timestamp > :since ORDER BY idx DESC")
    List<PenData> recentByMac(String mac, long since);

    @Query("SELECT * FROM PenData WHERE timestamp >= :start AND timestamp <= :end ORDER BY typ ASC, pen_mac ASC, timestamp ASC")
    List<PenData> allRecordsBetween(long start, long end);
}
