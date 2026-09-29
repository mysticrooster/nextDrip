package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.DesertSync;

import java.util.List;

@Dao
public interface DesertSyncDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(DesertSync desertSync);

    @Update
    void update(DesertSync desertSync);

    @Query("SELECT * FROM DesertSync WHERE timestamp > :position ORDER BY timestamp ASC LIMIT :limit")
    List<DesertSync> sinceAll(long position, int limit);

    @Query("SELECT * FROM DesertSync WHERE topic = :topic AND timestamp > :position ORDER BY timestamp ASC LIMIT :limit")
    List<DesertSync> sinceTopic(String topic, long position, int limit);

    @Query("SELECT * FROM DesertSync WHERE topic = :topic AND processed = :processed LIMIT 1")
    DesertSync alreadyInDatabase(String topic, String processed);

    @Query("SELECT * FROM DesertSync WHERE topic = :topic ORDER BY timestamp DESC LIMIT 1")
    DesertSync last(String topic);

    @Query("DELETE FROM DesertSync WHERE timestamp < :cutoff")
    int cleanup(long cutoff);

    @Query("DELETE FROM DesertSync")
    void deleteAll();
}
