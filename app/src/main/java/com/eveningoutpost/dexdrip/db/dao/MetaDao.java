package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MetaDao {

    @Query("SELECT name FROM sqlite_master WHERE type='table' "
            + "AND name NOT LIKE 'sqlite_%' "
            + "AND name != 'room_master_table' "
            + "AND name != 'android_metadata'")
    List<String> tableNames();
}
