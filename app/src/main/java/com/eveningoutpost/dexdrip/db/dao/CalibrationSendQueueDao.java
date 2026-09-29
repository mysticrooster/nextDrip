package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.utilitymodels.CalibrationSendQueue;

import java.util.List;

@Dao
public interface CalibrationSendQueueDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CalibrationSendQueue calibrationSendQueue);

    @Update
    void update(CalibrationSendQueue calibrationSendQueue);

    @Query("SELECT * FROM CalibrationSendQueue WHERE mongo_success = 0 ORDER BY _id DESC LIMIT 20")
    List<CalibrationSendQueue> mongoQueue();

    @Query("DELETE FROM CalibrationSendQueue WHERE mongo_success = 1")
    int cleanQueue();

    @Query("SELECT COUNT(*) FROM CalibrationSendQueue WHERE success = :success")
    int countBySuccess(boolean success);

    @Query("SELECT COUNT(*) FROM CalibrationSendQueue WHERE mongo_success = :success")
    int countByMongoSuccess(boolean success);
}
