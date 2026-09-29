package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.Upsert;

import com.eveningoutpost.dexdrip.models.ActiveBgAlert;

@Dao
public interface ActiveBgAlertDao {

    @Query("SELECT * FROM ActiveBgAlert ORDER BY _id ASC LIMIT 1")
    ActiveBgAlert getOnly();

    @Query("SELECT COUNT(*) FROM ActiveBgAlert")
    int count();

    @Upsert
    void upsert(ActiveBgAlert activeBgAlert);

    @Delete
    void delete(ActiveBgAlert activeBgAlert);
}
