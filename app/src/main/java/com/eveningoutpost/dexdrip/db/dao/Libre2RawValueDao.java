package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.Libre2RawValue;

import java.util.List;

@Dao
public interface Libre2RawValueDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Libre2RawValue value);

    @Update
    void update(Libre2RawValue value);

    @Query("SELECT * FROM Libre2RawValue2 WHERE ts >= :from ORDER BY ts ASC")
    List<Libre2RawValue> weightedAverageInterval(long from);

    @Query("SELECT * FROM Libre2RawValue2 WHERE ts >= :start AND ts <= :end AND glucose != 0 ORDER BY ts DESC LIMIT :limit")
    List<Libre2RawValue> latestForGraph(long start, long end, int limit);

    @Query("DELETE FROM Libre2RawValue2 WHERE ts < :cutoff")
    int cleanup(long cutoff);
}
