package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.LibreBlock;

import java.util.List;

@Dao
public interface LibreBlockDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(LibreBlock libreBlock);

    @Update
    void update(LibreBlock libreBlock);

    @Query("SELECT * FROM LibreBlock WHERE bytestart = 0 AND (byteend = :fram OR byteend = 44) "
            + "AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC LIMIT 1")
    LibreBlock latestForTrend(long start, long end, long fram);

    @Query("SELECT * FROM LibreBlock WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC")
    List<LibreBlock> forTrend(long start, long end);

    @Query("SELECT * FROM LibreBlock WHERE timestamp >= :lower AND timestamp <= :upper LIMIT 1")
    LibreBlock forTimestamp(long lower, long upper);

    @Query("SELECT * FROM LibreBlock WHERE uuid = :uuid LIMIT 1")
    LibreBlock findByUuid(String uuid);

    @Query("SELECT * FROM LibreBlock WHERE _id = :id LIMIT 1")
    LibreBlock byid(long id);
}
