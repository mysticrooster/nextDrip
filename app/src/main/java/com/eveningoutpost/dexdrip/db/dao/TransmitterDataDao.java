package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.TransmitterData;

import java.util.List;

@Dao
public interface TransmitterDataDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(TransmitterData transmitterData);

    @Update
    void update(TransmitterData transmitterData);

    @Query("SELECT * FROM TransmitterData ORDER BY _id DESC LIMIT 1")
    TransmitterData last();

    @Query("SELECT * FROM TransmitterData ORDER BY _id DESC LIMIT :count")
    List<TransmitterData> lastN(int count);

    @Query("SELECT * FROM TransmitterData ORDER BY timestamp DESC LIMIT 1")
    TransmitterData lastByTimestamp();

    @Query("SELECT * FROM TransmitterData WHERE timestamp <= :timestamp ORDER BY timestamp DESC LIMIT 1")
    TransmitterData getForTimestamp(double timestamp);

    @Query("SELECT * FROM TransmitterData WHERE uuid = :uuid LIMIT 1")
    TransmitterData findByUuid(String uuid);

    @Query("SELECT * FROM TransmitterData WHERE _id = :id LIMIT 1")
    TransmitterData byid(long id);

    @Query("DELETE FROM TransmitterData")
    void deleteAll();
}
