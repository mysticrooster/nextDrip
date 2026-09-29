package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.BloodTest;

import java.util.List;

@Dao
public interface BloodTestDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(BloodTest bloodTest);

    @Update
    void update(BloodTest bloodTest);

    @Delete
    void delete(BloodTest bloodTest);

    @Query("SELECT * FROM BloodTest ORDER BY timestamp DESC LIMIT :num")
    List<BloodTest> lastN(int num);

    @Query("SELECT * FROM BloodTest WHERE source LIKE :match ORDER BY timestamp DESC LIMIT :num")
    List<BloodTest> lastMatching(int num, String match);

    @Query("SELECT * FROM BloodTest WHERE (state & :flag) != 0 ORDER BY timestamp DESC LIMIT :num")
    List<BloodTest> lastValidN(int num, long flag);

    @Query("SELECT * FROM BloodTest WHERE uuid = :uuid LIMIT 1")
    BloodTest byUUID(String uuid);

    @Query("SELECT * FROM BloodTest WHERE _id = :id LIMIT 1")
    BloodTest byid(long id);

    @Query("SELECT * FROM BloodTest WHERE timestamp <= :upper AND timestamp >= :lower "
            + "ORDER BY abs(timestamp - :timestamp) ASC LIMIT 1")
    BloodTest getForPreciseTimestamp(long lower, long upper, long timestamp);

    @Query("SELECT * FROM BloodTest WHERE (state & :flag) != 0 AND timestamp >= :start AND timestamp <= :end "
            + "ORDER BY timestamp ASC LIMIT :limit")
    List<BloodTest> latestForGraph(long start, long end, int limit, long flag);

    @Query("DELETE FROM BloodTest WHERE timestamp < :cutoff")
    int cleanup(long cutoff);
}
