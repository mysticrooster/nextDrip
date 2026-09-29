package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.eveningoutpost.dexdrip.utilitymodels.BgSendQueue;

import java.util.List;

@Dao
public interface BgSendQueueDao {

    @Query("SELECT * FROM BgSendQueue WHERE mongo_success = 0 AND operation_type = 'create' ORDER BY _id DESC LIMIT 30")
    List<BgSendQueue> mongoQueue();

    @Query("DELETE FROM BgSendQueue WHERE mongo_success = 1 AND operation_type = 'create'")
    int cleanQueue();

    @Query("DELETE FROM BgSendQueue")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM BgSendQueue WHERE success = :success")
    int countBySuccess(boolean success);

    @Query("SELECT COUNT(*) FROM BgSendQueue WHERE mongo_success = :success")
    int countByMongoSuccess(boolean success);
}
