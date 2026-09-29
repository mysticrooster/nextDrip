package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.utilitymodels.UploaderQueue;

import java.util.List;

@Dao
public interface UploaderQueueDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(UploaderQueue uploaderQueue);

    @Update
    void update(UploaderQueue uploaderQueue);

    @Query("DELETE FROM UploaderQueue")
    void deleteAll();

    @Query("SELECT * FROM UploaderQueue WHERE otype = :type AND (bitfield_wanted & :bitfield) = :bitfield "
            + "AND (bitfield_complete & :bitfield) != :bitfield ORDER BY timestamp ASC, _id ASC LIMIT :limit")
    List<UploaderQueue> getPendingByType(String type, long bitfield, int limit);

    @Query("SELECT COUNT(*) FROM UploaderQueue WHERE otype = :type AND (bitfield_wanted & :bitfield) = :bitfield "
            + "AND (bitfield_complete & :bitfield) = :bitfield")
    int countCompletedByType(String type, long bitfield);

    @Query("SELECT COUNT(*) FROM UploaderQueue WHERE otype = :type AND (bitfield_wanted & :bitfield) = :bitfield "
            + "AND (bitfield_complete & :bitfield) != :bitfield")
    int countPendingByType(String type, long bitfield);

    @Query("SELECT DISTINCT otype FROM UploaderQueue")
    List<String> distinctTypes();

    @Query("DELETE FROM UploaderQueue WHERE timestamp < :cutoff AND bitfield_wanted = bitfield_complete")
    int deleteCompletedOlderThan(long cutoff);

    @Query("DELETE FROM UploaderQueue WHERE timestamp < :cutoff")
    int deleteOlderThan(long cutoff);
}
