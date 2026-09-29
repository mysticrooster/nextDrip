package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.eveningoutpost.dexdrip.models.CalibrationRequest;

import java.util.List;

@Dao
public interface CalibrationRequestDao {

    @Insert
    long insert(CalibrationRequest calibrationRequest);

    @Query("DELETE FROM CalibrationRequest")
    void deleteAll();

    @Query("SELECT * FROM CalibrationRequest")
    List<CalibrationRequest> getAll();

    @Query("SELECT * FROM CalibrationRequest WHERE requestIfAbove < :value AND requestIfBelow > :value LIMIT 1")
    CalibrationRequest findMatching(double value);
}
